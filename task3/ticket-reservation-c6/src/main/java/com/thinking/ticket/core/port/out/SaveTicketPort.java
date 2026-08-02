package com.thinking.ticket.core.port.out;

import com.thinking.ticket.core.domain.Ticket;

/* Outbound Port: Core가 바깥에 요구하는 티켓 저장 계약.
 * 저장 기술의 upsert, dirty checking, 조건부 update 같은 세부는 adapter 안에 숨긴다. */
public interface SaveTicketPort {

    void save(Ticket ticket);
}
