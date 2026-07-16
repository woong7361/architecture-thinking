package com.thinking.ticket;

import com.thinking.ticket.provided.UserRecord;

/**
 * 몇 장까지 가질 수 있는지 아는 회원.
 *
 * <p>{@link UserRecord}는 저장소가 다루는 행이라 상태만 갖는다. 한 장 더 가져도 되는지 판단하는
 * 일은 이쪽이 맡는다. {@link Ticket}이 티켓 쪽 규칙을 맡는 것과 같은 자리다.
 *
 * <p>가진 티켓 수는 만들 때 한 번 받아 온 값이다. 판단은 청구 전에 끝나므로 그 사이 수가 변할
 * 일은 이번 범위 밖이다.
 */
final class Buyer {

    /** 한 회원이 가질 수 있는 티켓 수. */
    private static final int TICKET_LIMIT = 1;

    private final long id;
    private final int ticketsHeld;

    private Buyer(long id, int ticketsHeld) {
        this.id = id;
        this.ticketsHeld = ticketsHeld;
    }

    static Buyer of(UserRecord record, int ticketsHeld) {
        return new Buyer(record.getId(), ticketsHeld);
    }

    /** 한 장 더 가질 수 없는 회원이면 거부한다. 청구 전에 물어보라고 있는 것이다. */
    void requireCanHoldOneMore() {
        if (ticketsHeld >= TICKET_LIMIT) {
            throw new TicketLimitExceededException(
                    "이미 티켓을 가진 회원입니다: " + id + " (보유 " + ticketsHeld + "장, 한도 " + TICKET_LIMIT + "장)");
        }
    }
}
