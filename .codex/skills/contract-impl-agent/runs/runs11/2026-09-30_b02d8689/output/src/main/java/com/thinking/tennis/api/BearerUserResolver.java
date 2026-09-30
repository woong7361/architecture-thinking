package com.thinking.tennis.api;

import org.springframework.stereotype.Component;

/**
 * 요청을 보낸 사용자를 가려낸다.
 *
 * <p>계약은 인증 방식 자체를 정하지 않는다. 남의 신청을 조회·해제할 수 없다는 요구 때문에 사용자 식별이
 * 필요하고 그 수단으로 Bearer 토큰을 쓴다고만 적었다. 그래서 토큰을 검증하는 방식은 이 초안이 고른 것이고,
 * 토큰 값을 그대로 사용자 식별자로 쓴다. 토큰이 없거나 Bearer 형식이 아니면 사용자를 식별할 수 없다.
 */
@Component
public class BearerUserResolver {

    private static final String BEARER = "Bearer ";

    /**
     * @param authorizationHeader {@code Authorization} 헤더의 값. 없으면 null이다
     * @return 사용자 식별자
     */
    public String requireUser(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new ApiFailure.Unauthenticated();
        }
        String value = authorizationHeader.trim();
        if (value.length() <= BEARER.length()
                || !value.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw new ApiFailure.Unauthenticated();
        }
        String token = value.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            throw new ApiFailure.Unauthenticated();
        }
        return token;
    }
}
