package com.thinking.ticket.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/* 영속 모델. 도메인 Ticket과 다른 클래스로 둔다 — 도메인은 JPA를 모르고 이 클래스는 예약 불변식을 모른다.
 * 하나로 겸용하면 저장 기술이 도메인의 모양을 규정하게 된다.
 *
 * 컬럼 이름을 @Column으로 못박는 이유: 스키마가 ddl-auto=update로 이 매핑에서 만들어지기 때문에,
 * 이름이 어긋나면 실패가 아니라 '다른 컬럼'이 조용히 하나 더 생긴다.
 * 식별자는 도메인이 이미 정해서 넘겨주므로 생성 전략을 두지 않는다. */
@Entity
@Table(name = "tickets")
public class TicketJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "price")
    private int price;

    @Column(name = "reserved")
    private boolean reserved;

    @Column(name = "suspended")
    private boolean suspended;

    @Column(name = "user_id")
    private long userId;

    /* JPA가 리플렉션으로 인스턴스를 만들 때만 쓰는 생성자. 코드에서 직접 부르지 않는다. */
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
