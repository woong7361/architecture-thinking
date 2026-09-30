package com.thinking.tennis.app.error;

/**
 * 만료 시각이 이미 지나 감시할 수 없는 조건이다. 만료 시각은 이용 시작 시각이다.
 */
public class AlertWindowClosedException extends RuntimeException {

    public AlertWindowClosedException() {
        super("이용 시작 시각이 지나 감시할 수 없습니다");
    }
}
