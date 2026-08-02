package com.thinking.ticket.core.port.out;

import com.thinking.ticket.core.domain.User;

/* Outbound Port: 회원 조회 계약. JPA Adapter / in-memory adapter가 구현한다. */
public interface LoadUserPort {

    User findById(long userId);
}
