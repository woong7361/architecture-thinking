import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 더티 리드와 팬텀 리드를 격리 수준별로 직접 관찰한다.
 * 조회 조건은 F-2의 알림 신청 조회와 같다.
 * 두 세션 모두 innodb_lock_wait_timeout = 1 이므로, 잠금에 막히면 1초 뒤 오류로 돌아온다.
 */
public class IsolationAnomalyProbe {
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

    static final Map<String, Integer> LEVELS = new LinkedHashMap<>();
    static {
        LEVELS.put("READ UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
        LEVELS.put("READ COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
        LEVELS.put("REPEATABLE READ", Connection.TRANSACTION_REPEATABLE_READ);
        LEVELS.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
    }

    public static void main(String[] args) throws Exception {
        try (Connection reader = open(); Connection writer = open()) {
            System.out.printf("ENV java=%s database=%s driver=%s%n",
                    System.getProperty("java.version"), scalar(reader, "SELECT VERSION()"),
                    reader.getMetaData().getDriverVersion());
            reader.rollback();
            for (var level : LEVELS.entrySet()) {
                reset(reader, writer);
                reader.setTransactionIsolation(level.getValue());
                writer.setTransactionIsolation(level.getValue());
                System.out.printf("LEVEL requested=%s readerSession=%s writerSession=%s%n", level.getKey(),
                        scalar(reader, "SELECT @@session.transaction_isolation"),
                        scalar(writer, "SELECT @@session.transaction_isolation"));
                reader.rollback();
                Map<String, Integer> dirty = new LinkedHashMap<>();
                Map<String, Integer> phantom = new LinkedHashMap<>();
                for (int round = 1; round <= ROUNDS; round++) {
                    tally(dirty, dirtyReadProbe(level.getKey(), round, reader, writer));
                    tally(phantom, phantomProbe(level.getKey(), round, reader, writer));
                }
                System.out.printf("SUMMARY level=%s rounds=%d dirtyRead=%s phantom=%s%n",
                        level.getKey(), ROUNDS, dirty, phantom);
            }
            reset(reader, writer);
        }
    }

    /** 커밋하지 않은 다른 트랜잭션의 INSERT가 조회에 보이는가. */
    static String dirtyReadProbe(String level, int round, Connection reader, Connection writer)
            throws SQLException {
        reset(reader, writer);
        String outcome;
        String writeNote = attempt(() -> exec(writer, INSERT));
        if (writeNote != null) {
            outcome = "WRITE_BLOCKED";
        } else {
            try {
                outcome = count(reader) > 0 ? "VISIBLE" : "NOT_VISIBLE";
            } catch (SQLException blocked) {
                outcome = "READ_BLOCKED:" + firstLine(blocked);
            }
        }
        reader.rollback();
        writer.rollback();
        System.out.printf("ROUND probe=dirtyRead level=%s round=%d outcome=%s writeNote=%s%n",
                level, round, outcome, writeNote);
        return outcome;
    }

    /** 같은 트랜잭션에서 두 번 조회할 때, 그 사이에 커밋된 행이 보이는가. */
    static String phantomProbe(String level, int round, Connection reader, Connection writer)
            throws SQLException {
        reset(reader, writer);
        int first = count(reader);
        String writeNote = attempt(() -> {
            exec(writer, INSERT);
            writer.commit();
        });
        if (writeNote != null) {
            writer.rollback();
        }
        int second = count(reader);
        String readerInsertNote = null;
        if (second == 0) {
            readerInsertNote = attempt(() -> exec(reader, INSERT));
        }
        if (readerInsertNote == null) {
            reader.commit();
        } else {
            reader.rollback();
        }
        int total = Integer.parseInt(scalar(writer, "SELECT COUNT(*) FROM alert_subscriptions"));
        writer.rollback();
        String outcome = writeNote != null ? "WRITE_BLOCKED" : (second > first ? "VISIBLE" : "NOT_VISIBLE");
        System.out.printf("ROUND probe=phantom level=%s round=%d outcome=%s firstRead=%d secondRead=%d"
                        + " rowsAfterReaderInsert=%d writeNote=%s readerInsertNote=%s%n",
                level, round, outcome, first, second, total, writeNote, readerInsertNote);
        return outcome + "/rows=" + total;
    }

    static void reset(Connection reader, Connection writer) throws SQLException {
        reader.rollback();
        writer.rollback();
        exec(writer, "DELETE FROM alert_subscriptions");
        writer.commit();
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

    interface Work {
        void run() throws SQLException;
    }
}
