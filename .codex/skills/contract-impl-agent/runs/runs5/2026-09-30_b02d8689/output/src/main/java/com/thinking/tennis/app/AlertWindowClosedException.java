package com.thinking.tennis.app;

public final class AlertWindowClosedException extends AlertApplicationException {

    public AlertWindowClosedException() {
        super("이용 시작 시각이 지나 감시할 수 없습니다");
    }
}
