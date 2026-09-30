package com.thinking.tennis.app.error;

/**
 * 같은 자원을 동시에 고치려다 경합에 밀렸다.
 *
 * <p>같은 멱등 키의 앞선 요청이 아직 처리 중이라 물러난 경우도 여기에 든다. 클라이언트의 대응이 같기
 * 때문이다. 잠시 기다렸다 같은 요청을 그대로 다시 보내면 된다.
 */
public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
