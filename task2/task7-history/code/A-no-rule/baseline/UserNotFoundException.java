package com.thinking.ticket;

/** 등록되지 않은 회원이 예매를 시도했다. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
