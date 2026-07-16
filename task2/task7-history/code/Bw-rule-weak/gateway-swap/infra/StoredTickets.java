package com.thinking.ticket.infra;

import com.thinking.ticket.Ticket;
import com.thinking.ticket.Tickets;
import com.thinking.ticket.provided.TicketRecord;
import com.thinking.ticket.provided.TicketStore;

/**
 * 사내 티켓 저장소를 티켓 보관소 역할에 맞춘다.
 *
 * <p>저장 레코드와 도메인 티켓 사이의 번역이 여기 갇힌다. 레코드가 예약 여부와 회원을
 * 따로 들고 있다는 사실은 이 클래스 밖으로 새지 않는다.
 */
public final class StoredTickets implements Tickets {

    private final TicketStore store;

    public StoredTickets(TicketStore store) {
        this.store = store;
    }

    @Override
    public Ticket byId(long ticketId) {
        TicketRecord record = store.findById(ticketId);
        return new Ticket(record.getId(), record.getPrice(),
                record.isReserved() ? record.getUserId() : null);
    }

    @Override
    public void save(Ticket ticket) {
        TicketRecord record = new TicketRecord(ticket.id(), ticket.price());
        record.setReserved(ticket.reservedBy() != null);
        record.setUserId(ticket.reservedBy());
        store.save(record);
    }
}
