package com.thinking.tennis.api;

import com.thinking.tennis.domain.UserId;

final class ApiAuthentication {

    private static final String BEARER_PREFIX = "Bearer ";

    private ApiAuthentication() {
    }

    static UserId userFromAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthenticatedException();
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new UnauthenticatedException();
        }
        return new UserId(token);
    }

    static class UnauthenticatedException extends RuntimeException {
    }
}
