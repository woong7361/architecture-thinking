package com.thinking.ticket;

/**
 * 티켓 값을 받아내는 방법.
 *
 * <p>예매 흐름은 "값을 받는다"까지만 알고, 그것이 카드 청구인지 포인트 차감인지는 이쪽이 안다.
 * 그래서 결제 수단이 하나 늘어도 예매 흐름은 그대로다.
 */
interface Payment {

    /**
     * 값을 받아낸다. 받아내지 못하면 수단에 맞는 예외를 던진다 — 흐름이 예매를 중단할 수 있도록.
     */
    void pay(long userId, int amount);
}
