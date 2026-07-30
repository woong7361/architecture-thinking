package com.thinking.ticket.adapter.out.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thinking.ticket.core.port.out.ChargePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/* 아웃바운드 포트 ChargePort를 외부 결제사 HTTP 호출로 구현한다.
 * Core는 결제사 SDK가 아니라 '청구'라는 역할에만 의존하므로, 결제사를 갈아끼워도
 * 바뀌는 것은 이 클래스뿐이다.
 *
 * 기반 URL은 pg.base-url에서 주입받는다. 기본값을 두지 않은 것은 의도적이다 —
 * 설정이 빠졌으면 엉뚱한 주소로 조용히 붙는 것보다 기동 시점에 실패하는 편이 낫다. */
@Component
public class PgChargeAdapter implements ChargePort {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PgChargeAdapter(RestTemplateBuilder restTemplateBuilder, @Value("${pg.base-url}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean charge(String paymentInfo, int amount) {
        try {
            ChargeResponse response = restTemplate.postForObject(
                    baseUrl + "/charge", new ChargeRequest(paymentInfo, amount), ChargeResponse.class);

            /* 승인 여부는 상태 코드가 아니라 본문이 정한다 — 거절도 200으로 온다.
             * 상태 코드로 판단하면 거절이 승인으로 뒤집혀 결제 없이 예약이 확정된다. */
            return response != null && response.approved();
        } catch (RestClientException e) {
            /* 통신이 실패했으면 승인받지 못한 것이다. 청구가 실제로 걸렸는지 알 수 없는 상태에서
             * 예약을 확정하는 쪽이 훨씬 비싼 오류다. */
            return false;
        }
    }

    /* 결제사 계약의 요청·응답 표현. 이 어댑터 밖으로 새어 나가면 안 되는 기술 세부라 중첩해 둔다. */
    record ChargeRequest(String paymentInfo, int amount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChargeResponse(boolean approved) {
    }
}
