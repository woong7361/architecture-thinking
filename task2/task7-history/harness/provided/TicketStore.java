package com.thinking.ticket.provided;

import java.util.HashMap;
import java.util.Map;

/**
 * 티켓 저장소. 기존 인프라이며 수정할 수 없다.
 *
 * <p>실제 운영에서는 DB를 읽고 쓰지만, 이 실험에서는 메모리로 동작하는 시뮬레이터다.
 * 그래서 {@link #seed}처럼 테스트가 초기 상태를 심는 통로가 함께 열려 있다.
 * 저장된 티켓이 없으면 {@link #findById}는 null을 반환한다.
 */
public final class TicketStore {

    private final Map<Long, TicketRecord> tickets = new HashMap<>();

    /** 테스트가 초기 상태를 심는 통로. 운영 코드에서 쓰라고 있는 것이 아니다. */
    public void seed(TicketRecord ticket) {
        tickets.put(ticket.getId(), ticket);
    }

    public TicketRecord findById(long ticketId) {
        return tickets.get(ticketId);
    }

    public void save(TicketRecord ticket) {
        tickets.put(ticket.getId(), ticket);
    }
}
