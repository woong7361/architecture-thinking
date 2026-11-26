package com.thinking.tennis.kata;

import java.util.List;

// 발송 경로다. 신청을 받으면 두 락의 키를 모두 알 수 있으므로 집합을 선언해 창구에 넘긴다.
public final class AlertDispatchService {
    private final OrderedLockFacade facade;
    private final LockPathSteps steps;

    public AlertDispatchService(OrderedLockFacade facade, LockPathSteps steps) {
        this.facade = facade;
        this.steps = steps;
    }

    public LockedOutcome dispatch(SubscribeCommand subscription) throws InterruptedException {
        LockRequest alert = LockRequest.alert(subscription);
        LockRequest availability =
                LockRequest.availability(subscription.courtId(), subscription.playDate());

        return facade.inOrder(List.of(alert, availability), session -> steps.afterBothLocked());
    }
}
