package com.thinking.tennis.kata;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * claim-10 이 기대고 있는 전제를 측정한다.
 *
 * "유일 인덱스가 NULL 을 서로 다른 값으로 보는 DB 에서 감시 중인 신청에만 걸리는 부분 유일 제약이 된다."
 *
 * 단언하지 않고 숫자와 오류 코드만 찍는다. 이 테스트는 판정 장치가 아니라 관찰 도구다.
 */
@SpringBootTest
@Testcontainers
class NullDistinctProbeTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 10, 10);

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_null_probe")
            .withUsername("kata")
            .withPassword(requiredPassword());

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private static String requiredPassword() {
        String password = System.getenv("KATA_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set KATA_DB_PASSWORD to your chosen disposable test password");
        }
        return password;
    }

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void resetFixtures() {
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0)");
    }

    private String insert(String status) {
        try {
            jdbc.update("""
                    INSERT INTO alert_subscriptions (user_id, court_id, play_date, time_slot, status)
                    VALUES (?, ?, ?, ?, ?)
                    """, 1, 10, PLAY_DATE, "09:00-10:00", status);
            return "OK";
        } catch (RuntimeException failure) {
            Throwable cause = failure;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String message = cause.getMessage();
            return cause.getClass().getSimpleName() + " / "
                    + (message == null ? "" : message.replace('\n', ' ').trim());
        }
    }

    @Test
    void nullsAreDistinctInUniqueIndex() {
        System.out.println("NULLPROBE --- 같은 조건의 WATCHING 둘 ---");
        System.out.println("NULLPROBE watching#1 " + insert("WATCHING"));
        System.out.println("NULLPROBE watching#2 " + insert("WATCHING"));

        System.out.println("NULLPROBE --- 같은 조건의 ENDED 셋 ---");
        System.out.println("NULLPROBE ended#1 " + insert("ENDED"));
        System.out.println("NULLPROBE ended#2 " + insert("ENDED"));
        System.out.println("NULLPROBE ended#3 " + insert("ENDED"));

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT status, watching_marker, COUNT(*) AS n FROM alert_subscriptions "
                        + "GROUP BY status, watching_marker ORDER BY status");
        for (Map<String, Object> row : rows) {
            System.out.printf("NULLPROBE rows status=%s marker=%s count=%s%n",
                    row.get("status"), row.get("watching_marker"), row.get("n"));
        }

        System.out.println("NULLPROBE --- 유일 인덱스 구성 ---");
        for (Map<String, Object> row : jdbc.queryForList(
                "SHOW INDEX FROM alert_subscriptions WHERE Key_name = 'uq_watching_subscription'")) {
            System.out.printf("NULLPROBE index seq=%s column=%s nullable=%s%n",
                    row.get("Seq_in_index"), row.get("Column_name"), row.get("Null"));
        }

        System.out.println("NULLPROBE --- 상태를 ENDED 로 바꾼 뒤 같은 조건 재신청 ---");
        jdbc.update("UPDATE alert_subscriptions SET status = 'ENDED' WHERE status = 'WATCHING'");
        System.out.println("NULLPROBE afterEnded watching#3 " + insert("WATCHING"));
        Integer watching = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alert_subscriptions WHERE status = 'WATCHING'", Integer.class);
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alert_subscriptions", Integer.class);
        System.out.printf("NULLPROBE final watching=%s total=%s%n", watching, total);
    }
}
