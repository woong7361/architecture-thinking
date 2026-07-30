package com.thinking.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/* 회원 조회만 필요하므로 기본 제공 메서드 외에 추가하지 않는다. */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
}
