package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 감시와 발송을 멈춘다.
 *
 * <p>여러 번 불러도 결과가 같다. 이미 끝난 신청에 다시 요청하면 상태를 바꾸지 않고 현재 상태를
 * 돌려준다. 해제한 신청도 기록은 남으므로 조회에서는 계속 보인다.
 *
 * <p>같은 신청을 동시에 고치려는 요청은 한쪽만 들인다. 밀린 쪽은 경합 실패로 물러나고, 같은 요청을
 * 그대로 다시 보내면 된다. 해제는 여러 번 불러도 결과가 같으므로 다시 보내는 것이 안전하다.
 */
@Service
public class CancelAlertUseCase {

    private final AlertStore alerts;
    private final AlertViewFactory views;

    public CancelAlertUseCase(AlertStore alerts, AlertViewFactory views) {
        this.alerts = alerts;
        this.views = views;
    }

    public AlertView cancel(String userId, UUID alertId) {
        ReentrantLock lock = alerts.updateLock(alertId);
        if (!lock.tryLock()) {
            throw new AlertOperationFailure.ConcurrentUpdate(
                    "같은 신청을 동시에 고치려는 요청이 있었습니다");
        }
        try {
            Alert alert = alerts.findOwned(alertId, userId)
                    .orElseThrow(AlertOperationFailure.AlertNotFound::new);
            Alert canceled = alert.canceled();
            if (canceled != alert) {
                alerts.save(canceled);
            }
            return views.of(canceled);
        } finally {
            lock.unlock();
        }
    }
}
