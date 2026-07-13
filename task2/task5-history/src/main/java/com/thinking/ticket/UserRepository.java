package com.thinking.ticket;

/* DB 경계(외부 의존). 특성화 테스트에서 Mock으로 격리한다. */
public interface UserRepository {

    User findById(long userId);
}
