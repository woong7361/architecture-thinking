package com.thinking.ticket.infra;

import com.thinking.ticket.Payments;
import com.thinking.ticket.provided.PaymentApi;

/** 외부 결제사 API를 결제사 역할에 맞춘다. */
public final class CardPayments implements Payments {

    private final PaymentApi api;

    public CardPayments(PaymentApi api) {
        this.api = api;
    }

    @Override
    public boolean charge(String paymentInfo, int amount) {
        return api.charge(paymentInfo, amount);
    }
}
