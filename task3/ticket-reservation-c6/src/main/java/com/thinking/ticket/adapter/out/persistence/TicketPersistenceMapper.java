package com.thinking.ticket.adapter.out.persistence;

import com.thinking.ticket.core.domain.Ticket;

/* 도메인 모델 ↔ 영속 모델 변환. 분리를 택한 대가를 이 어댑터 안에서 치른다 —
 * 변환이 Core로 새면 Core가 영속 모델을 알게 된다.
 *
 * 상태 복원에는 rehydrate를 쓴다. assignTo/suspend는 '전이'라서 이미 예약된 행을 되살릴 때
 * 불변식에 걸리고, 애초에 저장된 상태를 다시 판단하는 것 자체가 틀린 일이다. */
final class TicketPersistenceMapper {

    private TicketPersistenceMapper() {
    }

    static Ticket toDomain(TicketJpaEntity entity) {
        return Ticket.rehydrate(entity.getId(), entity.getPrice(), entity.isReserved(),
                entity.isSuspended(), entity.getUserId());
    }

    static TicketJpaEntity toEntity(Ticket ticket) {
        return new TicketJpaEntity(ticket.getId(), ticket.getPrice(), ticket.isReserved(),
                ticket.isSuspended(), ticket.getUserId());
    }
}
