package com.thinking.tennis.app;

public final class AlertNotFoundException extends AlertApplicationException {

    public AlertNotFoundException() {
        super("신청을 찾을 수 없습니다");
    }
}
