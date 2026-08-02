package com.thinking.ticket.adapter.out.persistence;

import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.domain.TicketAlreadyReservedException;
import com.thinking.ticket.core.port.out.LoadTicketPort;
import com.thinking.ticket.core.port.out.SaveTicketPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/* 아웃바운드 포트 LoadTicketPort / SaveTicketPort를 JPA + MySQL로 구현한다.
 * 포트는 Core가 소유한 계약이고, 이 클래스는 그 계약을 특정 기술로 갚는 쪽이다.
 * 스프링 빈으로 등록해 두어야 조립 지점이 포트 자리에 꽂을 수 있다. */
@Repository
public class TicketPersistenceAdapter implements LoadTicketPort, SaveTicketPort {

    private final TicketJpaRepository ticketJpaRepository;

    public TicketPersistenceAdapter(TicketJpaRepository ticketJpaRepository) {
        this.ticketJpaRepository = ticketJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket findById(long ticketId) {
        /* 없으면 null만 알린다 — '없음'을 예외로 번역하는 판단은 유스케이스의 몫이다. */
        return ticketJpaRepository.findById(ticketId)
                .map(TicketPersistenceMapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional
    public void save(Ticket ticket) {
        if (!ticket.isReserved()) {
            /* 예약 이전 상태(등록·판매 중지 등)는 읽어온 행 전체를 그대로 기록한다. */
            ticketJpaRepository.save(TicketPersistenceMapper.toEntity(ticket));
            return;
        }

        /* 예약 확정만은 조건부 UPDATE로 넘긴다. 전체 저장으로 처리하면 동시에 들어온 두 예약 중
         * 나중 것이 앞선 예약을 덮어써 같은 티켓이 두 번 팔린다. */
        int updated = ticketJpaRepository.reserveIfNotReserved(ticket.getId(), ticket.getUserId());
        if (updated == 0) {
            /* 한 행도 못 바꿨다 = 그사이 다른 인스턴스가 먼저 예약을 확정했다.
             * 여기서 성공한 척하면 결제만 남고 예약은 남의 것이 된다. 도메인 예외로 올려 보낸다.
             * 프로토콜 응답으로의 번역은 인바운드 어댑터가 한다. */
            throw new TicketAlreadyReservedException();
        }
    }
}
