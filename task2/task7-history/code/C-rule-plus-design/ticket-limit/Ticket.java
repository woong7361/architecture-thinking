package com.thinking.ticket;

import com.thinking.ticket.provided.TicketRecord;

/**
 * 예약 불변식의 주인.
 *
 * <p>{@link TicketRecord}는 setter를 다 연 저장용 레코드라 "이미 예약된 티켓은 다시 예약될 수 없다"는
 * 규칙을 스스로 지키지 못한다. 그 규칙을 예약 상태를 소유한 이 객체 안으로 들여, 상태는
 * {@link #reserve(long)}라는 의미 있는 행위를 통해서만 바뀌게 한다.
 *
 * <p>I/O는 모른다. 저장소를 부르는 일은 {@link TicketService}에 남아 있어, 이 객체는 Mock 없이 검증된다.
 */
final class Ticket {

    private final long id;
    private final int price;
    private boolean reserved;
    private Long userId;

    private Ticket(long id, int price, boolean reserved, Long userId) {
        this.id = id;
        this.price = price;
        this.reserved = reserved;
        this.userId = userId;
    }

    static Ticket from(TicketRecord record) {
        return new Ticket(record.getId(), record.getPrice(), record.isReserved(), record.getUserId());
    }

    /**
     * 이 티켓을 회원의 것으로 확정한다.
     *
     * @throws TicketAlreadyReservedException 이미 다른 사람이 예약한 티켓이면
     */
    void reserve(long userId) {
        if (reserved) {
            throw new TicketAlreadyReservedException(id);
        }
        this.reserved = true;
        this.userId = userId;
    }

    int price() {
        return price;
    }

    /**
     * 저장소에 넘길 레코드를 만든다.
     *
     * <p>기존 레코드를 제자리에서 고치지 않고 새로 만든다. 저장소는 조회해 준 인스턴스를 그대로 들고
     * 있어서, 제자리에서 고치면 {@code save} 전에 이미 저장소가 바뀌어 버린다 — 청구가 거절돼
     * 저장에 이르지 못한 티켓이 예약된 것으로 남게 된다.
     */
    TicketRecord toRecord() {
        TicketRecord record = new TicketRecord(id, price);
        record.setReserved(reserved);
        record.setUserId(userId);
        return record;
    }
}
