package com.thinking.ticket.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/* 티켓의 영속 모델. 도메인 Ticket과 일부러 다른 클래스로 둔다 — 도메인에 JPA 애노테이션이 붙는 순간
 * Core가 영속 기술을 알게 되고, 저장 스키마의 사정이 업무 규칙의 모양을 규정하게 된다.
 *
 * 물리 컬럼 이름을 @Column으로 못박는다. 스키마가 이 클래스로부터 만들어지는 구성이라,
 * 네이밍 전략 설정 하나만 바뀌어도 컬럼 이름이 조용히 달라져 저장 계약이 깨지기 때문이다. */
@Entity
@Table(name = "tickets")
public class TicketJpaEntity {

    /* 식별자는 도메인이 이미 정해 넘겨준다 — 여기서 생성 전략을 두면 저장 기술이 식별자를 소유하게 된다. */
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "reserved", nullable = false)
    private boolean reserved;

    @Column(name = "suspended", nullable = false)
    private boolean suspended;

    @Column(name = "user_id")
    private long userId;

    /* JPA가 리플렉션으로 쓰는 기본 생성자. 업무 코드가 빈 상태의 엔티티를 만들지 못하게 protected로 둔다. */
    protected TicketJpaEntity() {
    }

    public TicketJpaEntity(Long id, int price, boolean reserved, boolean suspended, long userId) {
        this.id = id;
        this.price = price;
        this.reserved = reserved;
        this.suspended = suspended;
        this.userId = userId;
    }

    public Long getId() {
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
