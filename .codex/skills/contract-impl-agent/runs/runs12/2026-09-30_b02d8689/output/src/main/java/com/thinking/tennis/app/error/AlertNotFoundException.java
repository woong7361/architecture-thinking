package com.thinking.tennis.app.error;

/**
 * 그런 신청이 없거나 요청한 사용자의 것이 아니다.
 *
 * <p>두 경우를 하나로 둔 것은 의도다. 남의 신청을 요청했을 때 없는 것과 다른 응답을 돌려주면 그 신청이
 * 있다는 사실이 드러난다.
 */
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(String alertId) {
        super("신청을 찾을 수 없습니다: " + alertId);
    }
}
