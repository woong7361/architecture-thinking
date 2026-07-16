package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;

/** 카드로 받아낸다. 결제사가 거절하면 {@link PaymentFailedException}. */
final class CardPayment implements PaymentMethod {

    private final PaymentApi payments;
    private final String paymentInfo;

    CardPayment(PaymentApi payments, String paymentInfo) {
        this.payments = payments;
        this.paymentInfo = paymentInfo;
    }

    @Override
    public void pay(int amount) {
        if (!payments.charge(paymentInfo, amount)) {
            throw new PaymentFailedException("청구가 거절되었다. 금액: " + amount);
        }
    }
}
