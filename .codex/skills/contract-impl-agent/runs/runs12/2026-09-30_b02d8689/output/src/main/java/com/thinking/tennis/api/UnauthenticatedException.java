package com.thinking.tennis.api;

/**
 * 토큰이 없거나 유효하지 않아 사용자를 식별할 수 없다.
 */
class UnauthenticatedException extends RuntimeException {

    UnauthenticatedException() {
        super("사용자를 식별하지 못했습니다");
    }
}
