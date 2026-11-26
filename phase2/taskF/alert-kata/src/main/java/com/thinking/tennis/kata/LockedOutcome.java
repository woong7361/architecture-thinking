package com.thinking.tennis.kata;

import java.util.List;
import java.util.Optional;

// 창구를 한 번 지난 결과다. 정렬 때문에 위치가 경로마다 달라지므로 요청으로 조회한다.
public record LockedOutcome(Status status, List<LockStep> steps) {
    public enum Status {
        COMPLETED,
        NOT_ACQUIRED,
        ORDER_VIOLATION
    }

    public Optional<LockStep> step(LockRequest request) {
        return steps.stream().filter(step -> step.request().equals(request)).findFirst();
    }
}
