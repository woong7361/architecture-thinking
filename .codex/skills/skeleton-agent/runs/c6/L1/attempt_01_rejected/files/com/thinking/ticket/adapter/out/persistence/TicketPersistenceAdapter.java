package com.thinking.ticket.adapter.out.persistence;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.port.out.TicketRepository;

/* Outbound Adapter: Core가 요구한 저장 계약(TicketRepository)을 JPA + MySQL로 구현한다.
 * 도메인 ↔ 영속 모델 변환도 이 안에서 끝낸다 — 변환이 밖으로 새면 영속 모델이 Core까지 흘러간다.
 * 스프링 빈으로 등록해 두어야 조립 지점이 이 구현을 포트 자리에 꽂을 수 있다. */
@Repository
public class TicketPersistenceAdapter implements TicketRepository {

    private final TicketJpaRepository ticketJpaRepository;

    public TicketPersistenceAdapter(TicketJpaRepository ticketJpaRepository) {
        this.ticketJpaRepository = ticketJpaRepository;
    }

    @Override
    public Ticket findById(long ticketId) {
        /* 없으면 null을 준다. '없음'을 어떤 예외로 볼지는 유스케이스의 판단이라 저장 기술이 정하지 않는다. */
        return ticketJpaRepository.findById(ticketId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional
    public void save(Ticket ticket) {
        if (!ticket.isReserved()) {
            ticketJpaRepository.save(toEntity(ticket));
            return;
        }

        /* 예약 확정만은 조건부 갱신으로 쓴다. 상태 전체를 그대로 덮어쓰면
         * 다른 인스턴스가 먼저 확정한 예약이 소리 없이 지워진다. */
        int updated = ticketJpaRepository.reserveIfNotReserved(ticket.getId(), ticket.getUserId());
        if (updated == 1) {
            return;
        }

        if (!ticketJpaRepository.existsById(ticket.getId())) {
            /* 아직 저장된 적 없는 예약 상태 티켓 — 갱신할 행이 없어서 0이었을 뿐이므로 그대로 넣는다. */
            ticketJpaRepository.save(toEntity(ticket));
            return;
        }

        /* 행은 있는데 갱신되지 않았다 = 그 사이 다른 예약이 먼저 확정됐다.
         * 스프링 표준 낙관적 잠금 예외로 알린다 — 진입 어댑터가 프로토콜 응답으로 번역할 수 있는 신호다. */
        throw new OptimisticLockingFailureException("이미 예약된 티켓이다: ticketId=" + ticket.getId());
    }

    private Ticket toDomain(TicketJpaEntity entity) {
        return Ticket.rehydrate(entity.getId(), entity.getPrice(), entity.isReserved(),
                entity.isSuspended(), entity.getUserId());
    }

    private TicketJpaEntity toEntity(Ticket ticket) {
        return new TicketJpaEntity(ticket.getId(), ticket.getPrice(), ticket.isReserved(),
                ticket.isSuspended(), ticket.getUserId());
    }
}
