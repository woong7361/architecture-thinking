package com.thinking.ticket;

/** 결제사가 청구를 거절했다. */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(long ticketId, int amount) {
        super("결제사가 청구를 거절했습니다. 티켓 " + ticketId + ", " + amount + "원");
    }
}
