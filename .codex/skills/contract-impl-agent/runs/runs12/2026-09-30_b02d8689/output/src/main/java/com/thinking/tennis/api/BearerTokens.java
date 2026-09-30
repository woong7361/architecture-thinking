package com.thinking.tennis.api;

import java.util.Locale;

/**
 * Bearer 토큰에서 사용자를 식별한다.
 *
 * <p>인증 방식 자체는 계약이 정하지 않는다. 남의 신청을 조회·해제할 수 없어야 해서 사용자 식별이
 * 필요하고, 그 수단이 Bearer 토큰이라는 것까지가 계약이 말한 것이다. 토큰의 내부 형식과 검증 방법은
 * 계약도 명세도 정하지 않았으므로 이 구현은 토큰 문자열 자체를 사용자 식별자로 쓴다.
 */
final class BearerTokens {

    private static final String SCHEME = "bearer ";

    static String userId(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new UnauthenticatedException();
        }
        String header = authorizationHeader.trim();
        if (!header.toLowerCase(Locale.ROOT).startsWith(SCHEME)) {
            throw new UnauthenticatedException();
        }
        String token = header.substring(SCHEME.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthenticatedException();
        }
        return token;
    }

    private BearerTokens() {
    }
}
