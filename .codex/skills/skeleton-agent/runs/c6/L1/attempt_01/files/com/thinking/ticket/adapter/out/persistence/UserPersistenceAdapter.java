package com.thinking.ticket.adapter.out.persistence;

import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.port.out.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/* 아웃바운드 포트 UserRepository의 JPA 구현. */
@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(long userId) {
        /* 없음 판정은 유스케이스가 한다. */
        return userJpaRepository.findById(userId)
                .map(UserPersistenceMapper::toDomain)
                .orElse(null);
    }
}
