package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;

/**
 * 카드로 청구하는 결제 담당.
 *
 * <p>벤더 API({@link PaymentApi})를 {@link ChargePort} 뒤로 감춘다(DIP) — 정책인
 * {@link TicketService}가 내가 통제하지 못하는 결제사 타입에 직접 매달리지 않게.
 *
 * <p>{@code boolean}으로 오는 거절을 도메인의 실패({@link PaymentFailedException})로 옮기는 것도
 * 이 자리의 일이다. 거절을 아는 것은 결제사와 통신하는 이쪽이고, 서비스가 되물어 판단하면
 * 그 분기가 결제수단마다 서비스 안으로 번진다.
 */
final class CardPaymentAdapter implements ChargePort {

    private final PaymentApi payments;
    private final String paymentInfo;

    CardPaymentAdapter(PaymentApi payments, String paymentInfo) {
        this.payments = payments;
        this.paymentInfo = paymentInfo;
    }

    @Override
    public void charge(int amount) {
        if (!payments.charge(paymentInfo, amount)) {
            throw new PaymentFailedException(amount);
        }
    }
}
