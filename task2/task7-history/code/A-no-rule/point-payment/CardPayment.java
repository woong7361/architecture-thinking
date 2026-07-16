package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;

/** 카드로 값을 받는다. 결제사가 거절하면 청구는 일어나지 않은 것으로 본다. */
final class CardPayment implements Payment {

    private final PaymentApi payments;
    private final String paymentInfo;

    CardPayment(PaymentApi payments, String paymentInfo) {
        this.payments = payments;
        this.paymentInfo = paymentInfo;
    }

    @Override
    public void pay(long userId, int amount) {
        if (!payments.charge(paymentInfo, amount)) {
            throw new PaymentFailedException("청구가 거절되었습니다: " + amount + "원");
        }
    }
}
