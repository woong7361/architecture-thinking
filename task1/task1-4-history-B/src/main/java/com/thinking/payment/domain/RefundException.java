package com.thinking.payment.domain;

public class RefundException extends RuntimeException {
    public RefundException(String message) {
        super(message);
    }
}
