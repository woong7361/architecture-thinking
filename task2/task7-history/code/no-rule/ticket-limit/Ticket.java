package com.thinking.ticket;

import com.thinking.ticket.provided.TicketRecord;

/**
 * 예매 규칙을 아는 티켓.
 *
 * <p>{@link TicketRecord}는 저장소가 다루는 행이라 상태만 갖는다. 예매 가능 여부를 판단하고
 * 주인을 확정하는 일은 이쪽이 맡는다.
 *
 * <p>레코드를 참조하지 않고 값을 복사해 온다. 그래서 예매가 도중에 실패하면 저장소에 심어진
 * 레코드는 손대지 않은 채로 남고, {@link #toRecord()}로 저장할 때만 변경이 밖으로 나간다.
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

    int price() {
        return price;
    }

    /** 예매할 수 있는 티켓이 아니면 거부한다. 청구 전에 물어보라고 있는 것이다. */
    void requireReservable() {
        if (reserved) {
            throw new TicketAlreadyReservedException("이미 예매된 티켓입니다: " + id);
        }
    }

    void reserveFor(long userId) {
        requireReservable();
        this.reserved = true;
        this.userId = userId;
    }

    TicketRecord toRecord() {
        TicketRecord record = new TicketRecord(id, price);
        record.setReserved(reserved);
        record.setUserId(userId);
        return record;
    }
}
