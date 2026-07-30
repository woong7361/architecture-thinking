package com.thinking.ticket.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/* 회원의 영속 모델. 스키마 계약은 users(id, name)이다.
 * 티켓과 같은 이유로 도메인 User와 분리한다 — Core에는 순수 도메인 객체만 오간다. */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    /* JPA 전용 생성자. */
    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
