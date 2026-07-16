package com.thinking.ticket;

/**
 * 이미 예약된 티켓을 예매하려 했다.
 *
 * <p>{@link Ticket#reserve(long)}가 자기 불변식을 지키며 스스로 던진다.
 */
public class TicketAlreadyReservedException extends RuntimeException {

    public TicketAlreadyReservedException(long ticketId) {
        super("이미 예약된 티켓입니다: " + ticketId);
    }
}
