package com.thinking.ticket.support;

import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.port.out.UserRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * DB 경계의 in-memory fake. Mock이 아니라 진짜 저장소처럼 동작해,
 * net이 "save가 불렸나(상호작용)"가 아니라 "저장소 상태가 어떤가(상태)"를 검증하게 한다.
 * 없는 id는 null을 반환한다 — 원본 코드의 null 반환 동작을 그대로 재현.
 */
public final class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new HashMap<>();

    public void save(User user) {
        users.put(user.getId(), user);
    }

    @Override
    public User findById(long userId) {
        return users.get(userId);
    }
}
