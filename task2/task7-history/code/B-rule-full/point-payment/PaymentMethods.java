package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.PointApi;

/**
 * 어떤 결제 수단이 있고 요청이 그중 무엇을 가리키는지 아는 쪽.
 *
 * <p>수단 이름을 실제 수단으로 옮기는 자리는 여기 하나뿐이다. 수단이 늘어도 {@link TicketService}의
 * 예매 흐름은 열리지 않는다 — 여기에 갈래가 하나 늘고 구현이 하나 늘 뿐이다.
 */
final class PaymentMethods {

    static final String CARD = "CARD";
    static final String POINT = "POINT";

    private final PaymentApi payments;
    private final PointApi points;

    PaymentMethods(PaymentApi payments, PointApi points) {
        this.payments = payments;
        this.points = points;
    }

    /** 요청이 가리키는 수단을, 누가 무엇으로 내는지까지 정해진 채로 돌려준다. */
    PaymentMethod select(String method, long userId, String paymentInfo) {
        return switch (method) {
            case CARD -> new CardPayment(payments, paymentInfo);
            case POINT -> new PointPayment(points, userId);
            default -> throw new IllegalArgumentException("알 수 없는 결제 수단이다: " + method);
        };
    }
}
