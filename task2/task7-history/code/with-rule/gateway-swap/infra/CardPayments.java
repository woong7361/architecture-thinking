package com.thinking.ticket.infra;

import com.thinking.ticket.Payments;
import com.thinking.ticket.provided.PaymentGateway;

/**
 * 새 결제사 게이트웨이를 결제사 역할에 맞춘다.
 *
 * <p>게이트웨이의 관례 — 인자가 (금액, 카드토큰) 순이고, 결과가 승인번호이며 거절이 null인 것 —
 * 은 전부 이 클래스 안에서 번역되어 밖으로 새지 않는다. 정책은 승인 여부만 본다.
 */
public final class CardPayments implements Payments {

    private final PaymentGateway gateway;

    public CardPayments(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean charge(String paymentInfo, int amount) {
        return gateway.authorize(amount, paymentInfo) != null;
    }
}
