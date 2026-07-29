package com.thinking.ticket.jpa;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/* 조합 B의 Spring/Testcontainers 부트스트랩.
 * 실제 MySQL 컨테이너를 싱글턴으로 한 번 띄우고(JVM 수명 동안 재사용), 그 접속 정보를
 * spring.datasource.* 로 주입한다. 결제(외부 의존)는 TestPaymentConfig의 통제 가능한 더블로 대체.
 * "test" 프로파일이라 운영용 DataSeeder(@Profile("!test"))는 로드되지 않는다 — 데이터는 시나리오가 만든다. */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@Import(TestPaymentConfig.class)
public class CucumberSpringConfiguration {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

    static {
        MYSQL.start(); // 싱글턴 컨테이너: @Testcontainers(JUnit 확장) 없이 Cucumber에서 안전하게 재사용
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
