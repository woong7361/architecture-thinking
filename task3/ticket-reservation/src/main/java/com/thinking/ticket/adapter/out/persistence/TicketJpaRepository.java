package com.thinking.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* Spring Data JPA 저장소(어댑터 내부 기술 세부). Core는 이 타입을 모른다. */
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, Long> {

    /* 수평 확장(scale-out) 안전 장치: 예약을 단일 원자 UPDATE로 확정한다.
     * WHERE reserved=false 조건이 '이중 예약'을 DB에서 막는다 —
     * 무상태 앱 인스턴스가 N개 떠 동시에 같은 티켓을 예약해도, 실제로 행을 바꾸는 건 딱 하나뿐이다.
     * 영향 행 수(0/1)로 성공 여부를 판정한다. */
    @Modifying
    @Query("update TicketJpaEntity t set t.reserved = true, t.userId = :userId "
            + "where t.id = :id and t.reserved = false and t.suspended = false")
    int reserveIfFree(@Param("id") long id, @Param("userId") long userId);
}
