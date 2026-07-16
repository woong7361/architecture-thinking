package com.thinking.ticket;

/** 이미 예매된 티켓을 예매하려 했다. */
public class TicketAlreadyReservedException extends RuntimeException {

    public TicketAlreadyReservedException(String message) {
        super(message);
    }
}
