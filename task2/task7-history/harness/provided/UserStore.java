package com.thinking.ticket.provided;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원 저장소. 기존 인프라이며 수정할 수 없다.
 *
 * <p>실제 운영에서는 DB를 읽지만, 이 실험에서는 메모리로 동작하는 시뮬레이터다.
 * 등록되지 않은 회원이면 {@link #findById}는 null을 반환한다.
 */
public final class UserStore {

    private final Map<Long, UserRecord> users = new HashMap<>();

    /** 테스트가 초기 상태를 심는 통로. 운영 코드에서 쓰라고 있는 것이 아니다. */
    public void seed(UserRecord user) {
        users.put(user.getId(), user);
    }

    public UserRecord findById(long userId) {
        return users.get(userId);
    }
}
