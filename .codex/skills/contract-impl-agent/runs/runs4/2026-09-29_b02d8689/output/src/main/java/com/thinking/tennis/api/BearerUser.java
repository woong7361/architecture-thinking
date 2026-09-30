package com.thinking.tennis.api;

import com.thinking.tennis.app.UseCaseException;

public final class BearerUser {

    private BearerUser() {
    }

    public static String resolveSubject(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw unauthenticated();
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (!token.matches("[A-Za-z0-9._~\\-]{1,256}")) {
            throw unauthenticated();
        }
        /*
         * The authentication provider owns cryptographic verification. This boundary only
         * accepts the provider's opaque subject representation and never treats a blank or
         * malformed bearer value as an authenticated subject.
         */
        return token;
    }

    private static UseCaseException unauthenticated() {
        return new UseCaseException(
                UseCaseException.Code.UNAUTHENTICATED,
                "인증이 필요합니다");
    }
}
