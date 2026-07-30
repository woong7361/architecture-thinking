package com.thinking.ticket.support;

import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.port.out.TicketRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * DB 경계(Outbound Port)의 in-memory fake. save는 실제로 맵에 반영하므로,
 * net은 "예약 후 저장소의 티켓 상태가 reserved인가"를 상태로 단언할 수 있다(verify 대신).
 * 없는 id는 null을 반환한다 — 도메인이 TicketNotFoundException으로 거부한다.
 */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<Long, Ticket> tickets = new HashMap<>();

    public void seed(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
    }

    @Override
    public Ticket findById(long ticketId) {
        return tickets.get(ticketId);
    }

    @Override
    public void save(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
    }
}
