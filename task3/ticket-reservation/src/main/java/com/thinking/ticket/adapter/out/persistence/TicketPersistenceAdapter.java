package com.thinking.ticket.adapter.out.persistence;

import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.port.out.TicketRepository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/* Outbound Adapter: TicketRepository(Outbound Port)를 JPA로 구현한다.
 * 엔티티<->도메인 변환을 여기서 처리하고, Core에는 순수 Ticket만 오간다. */
@Component
public class TicketPersistenceAdapter implements TicketRepository {

    private final TicketJpaRepository jpa;

    public TicketPersistenceAdapter(TicketJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Ticket findById(long ticketId) {
        // 없는 티켓은 null을 반환한다 — 원본 코드의 null 미검사 동작(quirk)을 그대로 보존.
        return jpa.findById(ticketId)
                .map(TicketPersistenceAdapter::toDomain)
                .orElse(null);
    }

    /* 예약 확정을 단일 원자 UPDATE로 저장한다(수평 확장 안전 지점).
     * 영향 행이 0이면 다른 인스턴스가 먼저 예약한 것 -> 동시성 충돌로 알린다.
     * (충돌 시 이미 청구된 결제의 보상은 이 skeleton 범위 밖 quirk로 남긴다 — C-5.) */
    @Override
    @Transactional
    public void save(Ticket ticket) {
        int updated = jpa.reserveIfFree(ticket.getId(), ticket.getUserId());
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "ticket " + ticket.getId() + " was already reserved by another instance");
        }
    }

    private static Ticket toDomain(TicketJpaEntity e) {
        return Ticket.rehydrate(e.getId(), e.getPrice(), e.isReserved(), e.isSuspended(), e.getUserId());
    }
}
