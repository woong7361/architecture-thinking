package com.thinking.ticket.core.port.out;

import com.thinking.ticket.core.domain.User;

/* Outbound Port: 회원 조회 계약. JPA Adapter / in-memory fake가 구현한다. */
public interface UserRepository {

    User findById(long userId);
}
