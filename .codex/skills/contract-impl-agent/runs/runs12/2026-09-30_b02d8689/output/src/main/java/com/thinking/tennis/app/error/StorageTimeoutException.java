package com.thinking.tennis.app.error;

/**
 * 저장소가 정해진 시간 안에 응답하지 않았다.
 *
 * <p>저장이 인메모리라 이 iteration에서 이 실패는 일어나지 않는다. 계약이 선언한 실패이므로 옮기는
 * 자리를 비워 두지 않고, 저장소가 바뀌었을 때 던질 자리를 여기 둔다.
 */
public class StorageTimeoutException extends RuntimeException {

    public StorageTimeoutException(String message) {
        super(message);
    }
}
