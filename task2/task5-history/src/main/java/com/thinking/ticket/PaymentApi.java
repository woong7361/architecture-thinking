package com.thinking.ticket;

/* 외부 결제 API 경계(외부 의존). 특성화 테스트에서 Mock으로 격리한다. */
public interface PaymentApi {

    boolean charge(String paymentInfo, int amount);
}
