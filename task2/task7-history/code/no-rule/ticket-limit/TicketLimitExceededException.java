package com.thinking.ticket;

/** 이미 티켓을 가진 회원이 추가로 예매하려 했다. */
public class TicketLimitExceededException extends RuntimeException {

    public TicketLimitExceededException(String message) {
        super(message);
    }
}
