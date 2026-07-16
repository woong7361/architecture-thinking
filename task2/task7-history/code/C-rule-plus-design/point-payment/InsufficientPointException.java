package com.thinking.ticket;

/**
 * 포인트 잔액이 티켓 가격보다 적다.
 *
 * <p>차감은 일어나지 않았고, 저장도 아직이므로 티켓은 아무에게도 확정되지 않은 채로 남는다.
 */
public class InsufficientPointException extends RuntimeException {

    public InsufficientPointException(long userId, int amount) {
        super("포인트가 부족합니다. 회원: " + userId + ", 필요 금액: " + amount);
    }
}
