package com.thinking.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* 저장 기술 쪽 인터페이스. Core의 TicketRepository(Port)와 이름이 비슷하지만 성격이 다르다 —
 * 이쪽은 어댑터 내부의 세부이고, 바깥으로 드러나는 계약은 Port 하나뿐이다.
 * 그래서 이 타입은 어댑터 밖(Core·다른 어댑터)에서 쓰지 않는다. */
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, Long> {

    /* 예약 확정을 '아직 예약되지 않은 행'에만 적용하는 조건부 갱신.
     * 인스턴스가 여러 개면 두 요청이 같은 티켓을 각자 읽어 둘 다 예약 가능으로 판단할 수 있다.
     * 그때 읽은 상태를 통째로 쓰면 나중 쓰기가 먼저 확정된 예약을 덮어쓰므로,
     * 조건을 DB의 UPDATE 문 안으로 옮겨 그 창을 닫는다.
     * 갱신된 행 수가 판정 근거다 — 0이면 대상 행이 없거나 이미 예약됐다는 뜻이다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TicketJpaEntity t set t.reserved = true, t.userId = :userId "
            + "where t.id = :id and t.reserved = false")
    int reserveIfNotReserved(@Param("id") long id, @Param("userId") long userId);
}
