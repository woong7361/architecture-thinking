package com.thinking.ticket.infra;

import com.thinking.ticket.Members;
import com.thinking.ticket.provided.UserStore;

/**
 * 사내 회원 저장소를 회원 명부 역할에 맞춘다.
 *
 * <p>"없으면 null"이라는 저장소의 관례를 여기서 등록 여부로 번역해, 정책이 null을 다루지 않게 한다.
 */
public final class RegisteredMembers implements Members {

    private final UserStore store;

    public RegisteredMembers(UserStore store) {
        this.store = store;
    }

    @Override
    public boolean isRegistered(long userId) {
        return store.findById(userId) != null;
    }
}
