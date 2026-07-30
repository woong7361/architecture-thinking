package com.thinking.ticket.jpa;

import com.thinking.ticket.support.SharedMySql;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/* 조합 B의 Spring/Testcontainers 부트스트랩.
 * 실제 MySQL 컨테이너를 싱글턴으로 한 번 띄우고(JVM 수명 동안 재사용), 그 접속 정보를
 * spring.datasource.* 로 주입한다. 결제(외부 의존)는 TestPaymentConfig의 통제 가능한 더블로 대체.
 * "test" 프로파일이라 운영용 DataSeeder(@Profile("!test"))는 로드되지 않는다 — 데이터는 시나리오가 만든다. */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@Import({TestPaymentConfig.class, com.thinking.ticket.support.StubCompositionRoot.class})
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        // 컨테이너는 HTTP 구성과 공유한다(SharedMySql) — 구성마다 따로 띄우지 않는다.
        registry.add("spring.datasource.url", SharedMySql.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySql.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySql.INSTANCE::getPassword);
    }
}
