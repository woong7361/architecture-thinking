package com.thinking.tennis.app;

public final class IdempotencyKeyReusedException extends AlertApplicationException {

    public IdempotencyKeyReusedException() {
        super("이미 다른 요청에 쓰인 멱등 키입니다");
    }
}
