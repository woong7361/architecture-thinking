package com.thinking.tennis.app;

public final class StorageTimeoutException extends AlertApplicationException {

    public StorageTimeoutException() {
        super("저장소가 제때 응답하지 않았습니다");
    }
}
