package com.thinking.ticket.core.port.out;

/* Outbound Port: 청구(결제) 계약. Core는 벤더 SDK가 아니라 이 역할에 의존한다.
 * 기동 환경에선 외부 Mock PG 서버를 호출하는 HTTP Adapter가, 인수테스트에선 in-process
 * test double이 이 계약을 구현한다 — '외부 PG로 교체해도 Core 무수정'의 실증 지점. */
public interface ChargePort {

    boolean charge(String paymentInfo, int amount);
}
