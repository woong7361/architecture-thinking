package com.thinking.ticket;

import com.thinking.ticket.provided.TicketRecord;

/**
 * 예매 규칙을 스스로 지키는 티켓.
 *
 * <p>{@link TicketRecord}는 setter가 전부 열린 저장용 행이라 "이미 예약된 티켓은 다시 예약할 수 없다"는
 * 불변식을 지킬 수 없다. 그 불변식은 예약 상태를 가진 이 객체 안에서 강제한다.
 *
 * <p>저장소에서 읽어온 레코드를 그대로 고치지 않고 값을 복원해 다룬다. 예약이 확정되기 전에 실패하면
 * 저장소의 행은 손대지 않은 채로 남는다.
 */
final class Ticket {

    private final long id;
    private final int price;
    private boolean reserved;
    private Long ownerId;

    private Ticket(long id, int price, boolean reserved, Long ownerId) {
        this.id = id;
        this.price = price;
        this.reserved = reserved;
        this.ownerId = ownerId;
    }

    static Ticket from(TicketRecord record) {
        return new Ticket(record.getId(), record.getPrice(), record.isReserved(), record.getUserId());
    }

    int price() {
        return price;
    }

    void reserveFor(long userId) {
        if (reserved) {
            throw new TicketAlreadyReservedException("이미 예약된 티켓이다: " + id);
        }
        this.reserved = true;
        this.ownerId = userId;
    }

    TicketRecord toRecord() {
        TicketRecord record = new TicketRecord(id, price);
        record.setReserved(reserved);
        record.setUserId(ownerId);
        return record;
    }
}
