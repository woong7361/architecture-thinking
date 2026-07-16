package com.thinking.ticket;

/**
 * 결제사에게 기대하는 역할.
 *
 * <p>변경 축: 결제사는 정책이 통제하지 않는 바깥 세계이고, 갈아끼워지는 것을 전제로 하는 자리다.
 * 어느 결제사를 쓰든 예매 정책은 그대로여야 한다.
 *
 * <p>거절이 곧 예매 실패라는 판단은 여기서 하지 않는다. 결제사는 승인 여부만 답하고,
 * 그 답을 어떻게 볼지는 예매 정책이 정한다.
 */
public interface Payments {

    /** 승인이면 true, 거절이면 false. */
    boolean charge(String paymentInfo, int amount);
}
