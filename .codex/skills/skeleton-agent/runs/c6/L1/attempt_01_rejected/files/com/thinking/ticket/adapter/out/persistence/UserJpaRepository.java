package com.thinking.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/* 회원 조회용 저장 기술 인터페이스. 어댑터 내부 세부이므로 이 패키지 밖에서 쓰지 않는다. */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
}
