package com.thinking.tennis.api;

/**
 * 계약이 적은 미디어 타입이다.
 *
 * <p>애노테이션의 값은 컴파일 시점 상수여야 해서 스프링의 {@code MediaType} 상수를 그대로 쓸 수 없는
 * 자리가 있다. 실패 응답의 타입을 자리마다 문자열로 적으면 한 자리만 달라도 그 응답의 본문 형식이
 * 계약과 어긋난다.
 */
public final class ApiMediaTypes {

    /** RFC 9457 실패 응답의 미디어 타입이다. */
    public static final String PROBLEM_JSON = "application/problem+json";

    private ApiMediaTypes() {
    }
}
