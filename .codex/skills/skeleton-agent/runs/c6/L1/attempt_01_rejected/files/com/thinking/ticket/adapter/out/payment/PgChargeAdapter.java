package com.thinking.ticket.adapter.out.payment;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.thinking.ticket.core.port.out.ChargePort;

/* Outbound Adapter: Core가 요구한 청구 계약(ChargePort)을 외부 결제사 HTTP 호출로 구현한다.
 * 결제사 주소가 환경마다 다르다는 사실은 이 층의 사정이므로 설정값으로 받아 여기서만 안다 —
 * Core는 '청구해 달라'까지만 안다.
 *
 * 스프링 빈으로 등록해 두어야 조립 지점이 이 구현을 포트 자리에 꽂을 수 있다. */
@Component
public class PgChargeAdapter implements ChargePort {

    private final RestTemplate restTemplate;
    private final URI chargeUri;

    public PgChargeAdapter(@Value("${pg.base-url}") String baseUrl) {
        /* HTTP 클라이언트는 이 어댑터의 내부 세부다. 교체 축이 없어 인터페이스로 뽑지 않고,
         * 생성자 시그니처에도 드러내지 않는다. */
        this.restTemplate = new RestTemplate();
        this.chargeUri = URI.create(stripTrailingSlash(baseUrl) + "/payments");
    }

    @Override
    public boolean charge(String paymentInfo, int amount) {
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    chargeUri, new PgChargeRequest(paymentInfo, amount), Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            /* 통신 실패와 승인 거절을 구분해 알릴 자리가 계약(boolean)에 없다. 여기서 임의의 예외를
             * 만들어 던지면 Core가 결제 기술의 실패 모양을 알게 되므로, '청구되지 않았다'로만 답한다. */
            return false;
        }
    }

    /* 설정값이 슬래시로 끝나도 경로가 이중 슬래시가 되지 않게 맞춘다. */
    private static String stripTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
