package com.thinking.ticket.adapter.out.persistence;

import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.port.out.UserRepository;

import org.springframework.stereotype.Component;

/* Outbound Adapter: UserRepository(Outbound Port)를 JPA로 구현한다. */
@Component
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserPersistenceAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User findById(long userId) {
        // 없는 회원은 null 반환 — 원본 동작 보존(TicketService가 UserNotFoundException으로 변환).
        return jpa.findById(userId)
                .map(e -> new User(e.getId(), e.getName()))
                .orElse(null);
    }
}
