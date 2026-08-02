package com.thinking.ticket.core.port.out;

import com.thinking.ticket.core.domain.Ticket;

/* Outbound Port: Core가 바깥에 요구하는 티켓 조회 계약.
 * JPA Persistence Adapter가, 인수테스트에선 in-memory adapter가 이 계약을 구현한다.
 * Core는 이 인터페이스만 알고 JPA를 모른다(의존성 역전). */
public interface LoadTicketPort {

    Ticket findById(long ticketId);
}
