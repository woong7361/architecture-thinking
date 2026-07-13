package com.thinking.ticket.support;

import com.thinking.ticket.Ticket;
import com.thinking.ticket.TicketRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * DB 경계의 in-memory fake. save는 실제로 맵에 반영하므로,
 * net은 "예약 후 저장소의 티켓 상태가 reserved인가"를 상태로 단언할 수 있다(verify 대신).
 *
 * <p>없는 id는 null을 반환한다 — 원본 코드가 null 체크를 안 해 NPE가 나는 quirk 재현용.
 * <p>{@link #failSaveWith}로 저장 실패를 주입할 수 있다 — 원자성 quirk(결제 후 저장 실패, 보상 없음) 재현용.
 * happy한 fake로는 save 실패를 낼 수 없어, 인스턴스 교체·재시딩 없이 플래그로 실패를 켠다.
 */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<Long, Ticket> tickets = new HashMap<>();
    private RuntimeException saveError; // null이면 정상 저장

    public void seed(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
    }

    public void failSaveWith(RuntimeException error) {
        this.saveError = error;
    }

    @Override
    public Ticket findById(long ticketId) {
        return tickets.get(ticketId);
    }

    @Override
    public void save(Ticket ticket) {
        if (saveError != null) {
            throw saveError;
        }
        tickets.put(ticket.getId(), ticket);
    }
}
