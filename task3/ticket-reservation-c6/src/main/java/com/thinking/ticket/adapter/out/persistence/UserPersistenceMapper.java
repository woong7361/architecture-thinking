package com.thinking.ticket.adapter.out.persistence;

import com.thinking.ticket.core.domain.User;

/* 회원은 조회만 하는 자리라 영속 모델 → 도메인 방향만 만든다.
 * 쓰지 않을 반대 방향까지 미리 만들면 아무도 검증하지 않는 코드가 남는다. */
final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    static User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getName());
    }
}
