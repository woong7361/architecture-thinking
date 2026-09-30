package com.thinking.tennis.app.error;

/**
 * 같은 멱등 키가 다른 내용의 요청에 이미 쓰였다.
 */
public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException() {
        super("같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다");
    }
}
