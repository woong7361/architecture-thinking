package com.thinking.ticket;

/* 청구 포트(내가 통제하는 결제 추상). 서비스가 벤더 API가 아니라 이 역할에 의존한다.
 * 특성화 테스트에서 test double로 격리한다. */
public interface ChargePort {

    boolean charge(String paymentInfo, int amount);
}
