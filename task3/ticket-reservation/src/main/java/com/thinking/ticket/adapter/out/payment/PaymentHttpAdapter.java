package com.thinking.ticket.adapter.out.payment;

import com.thinking.ticket.core.port.out.ChargePort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/* Outbound Adapter: ChargePort(Outbound Port)를 외부 결제사(PG) HTTP 호출로 구현한다.
 * 기동 환경에선 compose의 mock-pg(WireMock) 컨테이너를 호출한다(PG_BASE_URL, .env로 주입).
 * 진짜 PG로 교체해도 이 어댑터만 바뀌고 Core는 무수정 — 헥사고날의 실증 지점. */
@Component
public class PaymentHttpAdapter implements ChargePort {

    private final RestClient restClient;

    public PaymentHttpAdapter(RestClient.Builder builder, @Value("${pg.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public boolean charge(String paymentInfo, int amount) {
        try {
            ChargeResponse response = restClient.post()
                    .uri("/charge")
                    .body(new ChargeRequest(paymentInfo, amount))
                    .retrieve()
                    .body(ChargeResponse.class);
            return response != null && response.approved();
        } catch (RuntimeException e) {
            // 네트워크/서버 오류는 '승인 실패'로 다룬다(결제 미확정). 상세 오류 정책은 C-5.
            return false;
        }
    }

    public record ChargeRequest(String paymentInfo, int amount) {
    }

    public record ChargeResponse(boolean approved) {
    }
}
