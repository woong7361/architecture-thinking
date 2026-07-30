package com.thinking.ticket.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/* 회원의 영속 모델. 도메인 User가 지금은 데이터 그릇에 가깝지만 여기서도 분리를 유지한다 —
 * 한쪽만 겸용으로 두면 경계가 반쪽이 되고, 나중에 회원에 규칙이 생겼을 때 되돌리기 어렵다. */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    /* JPA가 리플렉션으로 쓰는 기본 생성자. */
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
