package com.thinking.ticket.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/* 회원 영속 모델. 도메인 User와 분리하되 매핑이 사소하므로 변환은 어댑터 내부에서 처리한다. */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private long id;

    @Column(nullable = false)
    private String name;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
