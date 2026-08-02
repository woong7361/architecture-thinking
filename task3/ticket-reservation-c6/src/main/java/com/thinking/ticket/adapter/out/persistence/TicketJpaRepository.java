package com.thinking.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* Spring Data가 구현을 만들어 주는 기술 세부. Core는 이 타입을 모르고
 * LoadTicketPort / SaveTicketPort(아웃바운드 포트)만 안다. */
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, Long> {

    /* 예약 확정을 '아직 예약되지 않은 행'에만 적용한다.
     *
     * 조회하고 판단하고 저장하는 사이에 다른 인스턴스가 먼저 예약을 확정할 수 있다.
     * 그때 읽어둔 상태를 그대로 덮어쓰면 뒤늦은 예약이 앞선 예약을 지워 이중 판매가 된다.
     * 조건을 UPDATE 문 안으로 넣어 그 틈을 DB의 원자적 연산 한 번으로 닫는다.
     * (version 컬럼을 둔 낙관적 잠금이 아니라 이 방식을 쓴 이유는 스키마 계약에 version이 없기 때문이다.)
     *
     * clearAutomatically: 벌크 UPDATE는 영속성 컨텍스트를 우회하므로, 남아 있는 1차 캐시를
     * 비워 뒤이은 조회가 낡은 상태를 보지 않게 한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TicketJpaEntity t set t.reserved = true, t.userId = :userId "
            + "where t.id = :ticketId and t.reserved = false")
    int reserveIfNotReserved(@Param("ticketId") long ticketId, @Param("userId") long userId);
}
