package com.thinking.tennis.kata;

import java.time.LocalDate;
import java.util.List;

// 갱신 경로다. 빈자리락만 선언해 창구에 넘기고, 조회 결과로 알게 된 신청락을 작업 도중에 추가한다.
public final class AvailabilityRefreshService {
    private final OrderedLockFacade facade;
    private final LockPathSteps steps;

    public AvailabilityRefreshService(OrderedLockFacade facade, LockPathSteps steps) {
        this.facade = facade;
        this.steps = steps;
    }

    public LockedOutcome refresh(long courtId, LocalDate playDate, String foundTimeSlot)
            throws InterruptedException {
        LockRequest availability = LockRequest.availability(courtId, playDate);
        LockRequest alert = LockRequest.alert(new SubscribeCommand(0L, courtId, playDate, foundTimeSlot));

        return facade.inOrder(List.of(availability), session -> {
            steps.afterFirstLocked();
            if (session.escalate(alert) == EscalateResult.ACQUIRED) {
                steps.afterBothLocked();
            }
        });
    }
}
