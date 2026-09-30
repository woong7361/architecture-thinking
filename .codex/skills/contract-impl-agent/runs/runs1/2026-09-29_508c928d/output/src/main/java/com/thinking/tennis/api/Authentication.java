package com.thinking.tennis.api;

import com.thinking.tennis.app.FailureCode;
import com.thinking.tennis.app.FailureContext;
import com.thinking.tennis.app.UseCaseException;

final class Authentication {
    private Authentication() {
    }

    static String userId(String authorization) {
        if (authorization == null) {
            throw new UseCaseException(
                    FailureCode.UNAUTHENTICATED,
                    FailureContext.GENERIC,
                    "인증이 필요합니다.",
                    false,
                null
            );
        }
        int separator = authorization.indexOf(' ');
        if (separator <= 0 || !"Bearer".equalsIgnoreCase(authorization.substring(0, separator))) {
            throw new UseCaseException(
                    FailureCode.UNAUTHENTICATED,
                    FailureContext.GENERIC,
                    "인증이 필요합니다.",
                    false,
                    null
            );
        }
        String token = authorization.substring(separator + 1).trim();
        if (token.isEmpty()) {
            throw new UseCaseException(
                    FailureCode.UNAUTHENTICATED,
                    FailureContext.GENERIC,
                    "인증이 필요합니다.",
                    false,
                    null
            );
        }
        return token;
    }
}
