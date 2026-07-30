package com.thinking.ticket.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.port.out.UserRepository;

/* Outbound Adapter: Core가 요구한 회원 조회 계약(UserRepository)을 JPA + MySQL로 구현한다. */
@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User findById(long userId) {
        /* 없으면 null. 자격 없는 요청을 어떻게 다룰지는 유스케이스가 정한다. */
        return userJpaRepository.findById(userId)
                .map(this::toDomain)
                .orElse(null);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getName());
    }
}
