package com.thinking.tennis.app;

public final class ConcurrentUpdateConflictException extends AlertApplicationException {

    public ConcurrentUpdateConflictException() {
        super("같은 요청이 아직 처리 중입니다");
    }
}
