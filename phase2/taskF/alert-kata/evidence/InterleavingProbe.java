import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 같은 사용자의 두 신청 요청이 "조회 → 없으면 저장"을 어떤 순서로 실행하느냐에 따라
 * 격리 수준이 결과를 바꾸는지 관찰한다.
 *
 * 재현 테스트는 두 요청을 겹쳐 보내므로 두 조회가 모두 저장보다 먼저 끝나는 순서만 만든다.
 * 나머지 두 순서는 타이밍에 맡기면 만들어지지 않으므로 여기서 직접 고정한다.
 *
 * 조회 조건과 저장하는 행은 서비스와 같다.
 * 두 세션 모두 innodb_lock_wait_timeout = 1 이므로, 잠금에 막히면 1초 뒤 오류로 돌아온다.
 */
public class InterleavingProbe {
    static final String URL = System.getenv().getOrDefault("KATA_PROBE_URL", "jdbc:mysql://127.0.0.1:13399/alert_kata");
    static final String USER = System.getenv().getOrDefault("KATA_PROBE_USER", "root");
    static final String PASSWORD = requiredPassword();
    static final int ROUNDS = 20;
    static final String PREDICATE = """
            SELECT COUNT(*) FROM alert_subscriptions
            WHERE user_id = 1 AND court_id = 10 AND play_date = '2026-10-10'
              AND time_slot = '09:00-10:00' AND status = 'WATCHING'
            """;
    static final String INSERT = """
            INSERT INTO alert_subscriptions (user_id, court_id, play_date, time_slot, status)
            VALUES (1, 10, '2026-10-10', '09:00-10:00', 'WATCHING')
            """;

    /** 두 요청이 조회와 저장을 실행하는 순서. */
    enum Order {
        /** 조회(1) → 조회(2) → 저장(1) → 저장(2). 재현 테스트가 만드는 순서다. */
        BOTH_READ_FIRST,
        /** 조회(1) → 저장(1) → 조회(2) → 저장(2). 2번 조회가 1번의 커밋 전에 일어난다. */
        READ_BEFORE_COMMIT,
        /** 조회(1) → 저장(1) → 커밋(1) → 조회(2) → 저장(2). 2번 조회가 1번의 커밋 뒤에 일어난다. */
        READ_AFTER_COMMIT
    }

    static final Map<String, Integer> LEVELS = new LinkedHashMap<>();
    static {
        LEVELS.put("READ UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
        LEVELS.put("READ COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
        LEVELS.put("REPEATABLE READ", Connection.TRANSACTION_REPEATABLE_READ);
        LEVELS.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
    }

    public static void main(String[] args) throws Exception {
        try (Connection first = open(); Connection second = open()) {
            System.out.printf("ENV java=%s database=%s driver=%s rounds=%d%n",
                    System.getProperty("java.version"), scalar(first, "SELECT VERSION()"),
                    first.getMetaData().getDriverVersion(), ROUNDS);
            first.rollback();
            for (var level : LEVELS.entrySet()) {
                reset(first, second);
                first.setTransactionIsolation(level.getValue());
                second.setTransactionIsolation(level.getValue());
                System.out.printf("LEVEL requested=%s session1=%s session2=%s%n", level.getKey(),
                        scalar(first, "SELECT @@session.transaction_isolation"),
                        scalar(second, "SELECT @@session.transaction_isolation"));
                first.rollback();
                for (Order order : Order.values()) {
                    Map<String, Integer> outcomes = new LinkedHashMap<>();
                    int duplicates = 0;
                    for (int round = 1; round <= ROUNDS; round++) {
                        Result result = runRound(order, level.getKey(), round, first, second);
                        tally(outcomes, result.outcome());
                        if (result.rows() > 1) {
                            duplicates++;
                        }
                    }
                    System.out.printf("SUMMARY level=%s order=%s rounds=%d duplicateRounds=%d outcomes=%s%n",
                            level.getKey(), order, ROUNDS, duplicates, outcomes);
                }
            }
            reset(first, second);
        }
    }

    /** 두 요청이 서비스와 같은 판단("없으면 만든다")을 하도록 두고, 실행 순서만 바꾼다. */
    static Result runRound(Order order, String level, int round, Connection first, Connection second)
            throws SQLException {
        reset(first, second);

        int read1 = count(first);
        String read2;
        String insert1;
        String insert2;

        if (order == Order.BOTH_READ_FIRST) {
            // 조회(1) → 조회(2) → 저장(1) → 저장(2)
            Integer seen = null;
            try {
                seen = count(second);
                read2 = String.valueOf(seen);
            } catch (SQLException blocked) {
                read2 = "blocked";
            }
            insert1 = read1 == 0 ? attempt(() -> exec(first, INSERT)) : "skipped";
            insert2 = seen == null ? "notReached"
                    : (seen == 0 ? attempt(() -> exec(second, INSERT)) : "skipped");
        } else {
            // 조회(1) → 저장(1) → [커밋(1)] → 조회(2) → 저장(2)
            insert1 = read1 == 0 ? attempt(() -> exec(first, INSERT)) : "skipped";
            if (order == Order.READ_AFTER_COMMIT) {
                first.commit();
            }
            try {
                int seen = count(second);
                read2 = String.valueOf(seen);
                insert2 = seen == 0 ? attempt(() -> exec(second, INSERT)) : "skipped";
            } catch (SQLException blocked) {
                read2 = "blocked";
                insert2 = "notReached";
            }
        }

        if (order != Order.READ_AFTER_COMMIT) {
            commitOrRollback(first);
        }
        commitOrRollback(second);

        int rows = Integer.parseInt(scalar(first, "SELECT COUNT(*) FROM alert_subscriptions"));
        first.commit();

        String outcome = describe(read2, rows);
        System.out.printf("ROUND level=%s order=%s round=%d read1=%d insert1=%s read2=%s insert2=%s"
                        + " rows=%d outcome=%s%n",
                level, order, round, read1, insert1, read2, insert2, rows, outcome);
        return new Result(outcome, rows);
    }

    /** 2번 요청이 무엇을 보고 무엇이 남았는지로 회차를 분류한다. */
    static String describe(String read2, int rows) {
        if (read2.equals("blocked")) {
            return "READ_BLOCKED/rows=" + rows;
        }
        if (rows > 1) {
            return "DUPLICATE/rows=" + rows;
        }
        if (rows == 0) {
            return "WRITE_BLOCKED/rows=0";
        }
        if (read2.equals("0")) {
            return "ONE_WRITE_BLOCKED/rows=1";
        }
        return "EXISTING_RETURNED/rows=1";
    }

    static void commitOrRollback(Connection connection) {
        try {
            connection.commit();
        } catch (SQLException failed) {
            rollbackQuietly(connection);
        }
    }

    static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 다음 회차의 reset이 다시 정리한다.
        }
    }

    static void reset(Connection first, Connection second) throws SQLException {
        rollbackQuietly(first);
        rollbackQuietly(second);
        exec(first, "DELETE FROM alert_subscriptions");
        first.commit();
    }

    /** 잠금에 막히면 오류 메시지를, 통과하면 null을 돌려준다. */
    static String attempt(Work work) {
        try {
            work.run();
            return null;
        } catch (SQLException blocked) {
            return firstLine(blocked);
        }
    }

    static String firstLine(SQLException error) {
        return error.getMessage().split("\\R")[0];
    }

    static int count(Connection connection) throws SQLException {
        return Integer.parseInt(scalar(connection, PREDICATE));
    }

    static String scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getString(1);
        }
    }

    static void exec(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    static void tally(Map<String, Integer> counts, String outcome) {
        counts.merge(outcome, 1, Integer::sum);
    }

    static String requiredPassword() {
        String password = System.getenv("KATA_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set KATA_DB_PASSWORD to your chosen disposable probe password");
        }
        return password;
    }

    static Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        connection.setAutoCommit(false);
        exec(connection, "SET SESSION innodb_lock_wait_timeout = 1");
        connection.commit();
        return connection;
    }

    record Result(String outcome, int rows) {
    }

    interface Work {
        void run() throws SQLException;
    }
}
