package com.thinking.ticket.e2e;

import com.thinking.ticket.jpa.TestPaymentConfig;
import com.thinking.ticket.support.SharedMySql;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/* HTTP 관통 구성의 부트스트랩. 실제 서블릿 컨테이너를 임의 포트로 띄워(RANDOM_PORT)
 * 인바운드 진입을 대역(포트 직접 호출)이 아니라 실물 HTTP로 바꾼다.
 * 저장은 실제 MySQL, 결제만 통제 가능한 더블로 남는다 — 이 구성이 walking skeleton의 판정자다. */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestPaymentConfig.class, com.thinking.ticket.support.StubCompositionRoot.class})
public class E2eCucumberSpringConfiguration {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySql.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySql.INSTANCE::getPassword);
    }
}
