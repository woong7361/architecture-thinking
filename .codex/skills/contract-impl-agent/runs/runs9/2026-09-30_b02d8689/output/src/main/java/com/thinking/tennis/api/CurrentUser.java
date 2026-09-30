package com.thinking.tennis.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
class CurrentUser {

    private static final String BEARER_PREFIX = "Bearer ";

    String require(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX) || authorization.substring(BEARER_PREFIX.length()).isBlank()) {
            throw new UnauthenticatedException();
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
