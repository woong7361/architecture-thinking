package com.thinking.payment;

public interface PgClient {

    PgCancelResult cancelPayment(String paymentUuid, int amount);
}
