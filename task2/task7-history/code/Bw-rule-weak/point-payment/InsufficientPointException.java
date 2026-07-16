package com.thinking.ticket;

/** 포인트 잔액이 티켓 가격보다 적다. */
public class InsufficientPointException extends RuntimeException {

    public InsufficientPointException(long userId, int amount) {
        super("포인트 잔액이 부족합니다. 회원 " + userId + ", " + amount + "점");
    }
}
