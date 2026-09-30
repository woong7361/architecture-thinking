package com.thinking.tennis.api;

/**
 * 요청의 형식이나 값이 스키마를 만족하지 않는다.
 *
 * <p>본문의 형식 위반과 필수 멱등 키 헤더의 누락이 함께 여기로 온다. 클라이언트의 대응이 같기 때문이다.
 */
class RequestValidationException extends RuntimeException {

    RequestValidationException(String detail) {
        super(detail);
    }
}
