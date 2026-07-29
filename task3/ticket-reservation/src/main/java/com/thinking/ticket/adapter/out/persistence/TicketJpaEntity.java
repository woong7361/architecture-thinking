package com.thinking.ticket.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/* 영속 모델(JPA @Entity). 순수 도메인 Ticket과 분리한다(C-3 방침) — Core는 이 타입을 모른다.
 * 도메인<->엔티티 변환은 TicketPersistenceAdapter가 담당(규모가 작아 별도 Mapper 클래스는 미추출, YAGNI). */
@Entity
@Table(name = "tickets")
public class TicketJpaEntity {

    @Id
    private long id; // 좌석/티켓 번호를 그대로 PK로 쓴다(자동 생성 아님).

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean reserved;

    @Column(nullable = false)
    private boolean suspended;

    @Column(name = "user_id", nullable = false)
    private long userId;

    protected TicketJpaEntity() {
        // JPA 요구 기본 생성자
    }

    public TicketJpaEntity(long id, int price, boolean reserved, boolean suspended, long userId) {
        this.id = id;
        this.price = price;
        this.reserved = reserved;
        this.suspended = suspended;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    public boolean isReserved() {
        return reserved;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public long getUserId() {
        return userId;
    }
}
