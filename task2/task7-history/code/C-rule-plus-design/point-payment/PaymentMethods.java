package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.PointApi;

/**
 * 결제수단의 목록을 아는 자. 요청이 말한 수단에 맞는 결제 담당을 골라 준다.
 *
 * <p><b>왜 서비스가 아니라 이 객체인가.</b> 고르려면 "어떤 수단이 있고 각각 누가 맡으며 무엇을 매어
 * 줘야 하는지"를 알아야 한다. 그것은 협력의 순서를 아는 {@link TicketService}의 관심사가 아니다.
 * 두 객체는 바뀌는 이유가 다르다(SRP) — 세 번째 수단이 생기면 여기와 어댑터 한 장만 열리고,
 * 예매 흐름은 닫힌 채로 남는다.
 *
 * <p>수단 이름을 아는 곳도 여기 하나뿐이다. 3-인자 진입점이 쓰는 {@link #CARD}까지 이 자리에 모아 둔
 * 것은, 이름이 흩어지면 수단이 늘 때마다 흩어진 자리를 모두 찾아야 하기 때문이다.
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

    /**
     * 결제수단 이름에 맞는 결제 담당을 고른다. 그 담당에게는 자기가 청구에 쓸 정보만 매어 준다 —
     * 포인트에는 회원, 카드에는 카드정보. 계약이 정한 이름은 {@link #CARD}와 {@link #POINT} 둘뿐이다.
     */
    ChargePort select(String paymentMethod, long userId, String paymentInfo) {
        if (POINT.equals(paymentMethod)) {
            return new PointPaymentAdapter(points, userId);
        }
        return new CardPaymentAdapter(payments, paymentInfo);
    }
}
