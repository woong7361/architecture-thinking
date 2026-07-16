package com.thinking.ticket;

/**
 * 회원 명부에게 기대하는 역할.
 *
 * <p>변경 축: 회원 확인은 정책이 통제하지 않는 남의 것(사내 회원 저장소)에 있고,
 * 저장소가 DB든 회원 서비스 호출이든 예매 정책은 그대로여야 한다. 그 방향을 뒤집으려고 둔다.
 *
 * <p>너비: 정책은 회원의 이름도 그 밖의 무엇도 쓰지 않는다. 등록됐는지만 묻는다.
 */
public interface Members {

    boolean isRegistered(long userId);
}
