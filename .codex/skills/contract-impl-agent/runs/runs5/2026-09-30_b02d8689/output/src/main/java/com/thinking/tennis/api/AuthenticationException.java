package com.thinking.tennis.api;

public final class AuthenticationException extends RuntimeException {

    public AuthenticationException() {
        super("인증이 필요합니다");
    }
}
