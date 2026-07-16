package com.thinking.ticket;

/** 결제사가 청구를 거절했다. */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
