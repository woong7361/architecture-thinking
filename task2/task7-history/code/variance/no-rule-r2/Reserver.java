package com.thinking.ticket;

import com.thinking.ticket.provided.UserRecord;

/**
 * 티켓을 예매하는 회원.
 *
 * <p>{@link UserRecord}는 저장소가 다루는 행이라 상태만 갖는다. 한 회원이 티켓을 몇 장까지
 * 가질 수 있는지 판단하는 일은 이쪽이 맡는다.
 *
 * <p>{@link Ticket}과 마찬가지로 값을 복사해 온다. 이미 가진 티켓 수는 예매를 시작한 시점의
 * 것이다.
 */
final class Reserver {

    /** 한 회원이 가질 수 있는 티켓 수. */
    private static final int TICKET_LIMIT = 1;

    private final long id;
    private final int heldTickets;

    private Reserver(long id, int heldTickets) {
        this.id = id;
        this.heldTickets = heldTickets;
    }

    static Reserver of(UserRecord record, int heldTickets) {
        return new Reserver(record.getId(), heldTickets);
    }

    /** 티켓을 더 가질 수 없으면 거부한다. 청구 전에 물어보라고 있는 것이다. */
    void requireCanHoldMore() {
        if (heldTickets >= TICKET_LIMIT) {
            throw new TicketLimitExceededException(
                    "회원당 티켓은 " + TICKET_LIMIT + "장까지입니다: 회원 " + id);
        }
    }
}
