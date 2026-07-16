package com.thinking.ticket;

/** 이미 티켓을 가진 회원이 추가로 예매하려 했다. 한 회원은 티켓을 한 장만 가질 수 있다. */
public class TicketLimitExceededException extends RuntimeException {

    public TicketLimitExceededException(String message) {
        super(message);
    }
}
