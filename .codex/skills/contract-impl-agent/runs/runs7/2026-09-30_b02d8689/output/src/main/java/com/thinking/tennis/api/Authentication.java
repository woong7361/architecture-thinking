package com.thinking.tennis.api;

import com.thinking.tennis.app.ApplicationFailure;
import com.thinking.tennis.app.FailureCode;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class Authentication {
    private Authentication() {
    }

    public static String requireUser(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            throw unauthenticated();
        }
        int separator = authorization.indexOf(' ');
        if (separator <= 0
                || !"bearer".equals(authorization.substring(0, separator).toLowerCase(Locale.ROOT))) {
            throw unauthenticated();
        }
        String credential = authorization.substring(separator + 1).trim();
        if (credential.isEmpty()) {
            throw unauthenticated();
        }
        return credential;
    }

    private static ApplicationFailure unauthenticated() {
        return ApplicationFailure.of(
                FailureCode.UNAUTHENTICATED,
                401,
                false,
                null,
                "인증이 필요합니다"
        );
    }
}
