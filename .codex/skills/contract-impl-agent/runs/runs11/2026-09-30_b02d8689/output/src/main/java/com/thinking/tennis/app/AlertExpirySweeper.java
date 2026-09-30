package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 만료 시각이 지난 신청을 만료로 옮긴다.
 *
 * <p>만료는 엔드포인트가 없고 서버가 스스로 수행하는 일이다. 읽을 때 옮기지 않는 것은 계약이 저장된
 * 상태로 거른다고 적었기 때문이다. 만료 시각이 지났지만 아직 감시 중으로 저장된 신청은 감시 중으로
 * 걸러도 함께 와야 하는데, 읽을 때 옮기면 그 약속이 깨진다.
 *
 * <p>그래서 이 시각이 지났는데도 상태가 잠시 감시 중으로 보일 수 있고, 그 사이에는 응답의
 * {@code expiresAt} 을 기준으로 만료를 표시하라고 계약이 적었다.
 */
@Component
public class AlertExpirySweeper {

    private final AlertStore alerts;
    private final Clock clock;

    public AlertExpirySweeper(AlertStore alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000L)
    public void sweep() {
        Instant now = clock.instant();
        for (Alert alert : alerts.watching()) {
            if (alert.expiresAt().isAfter(now)) {
                continue;
            }
            ReentrantLock lock = alerts.updateLock(alert.alertId());
            if (!lock.tryLock()) {
                /* 사용자의 해제가 같은 신청을 고치는 중이면 이번 주기는 건너뛴다. 다음 주기에 다시 본다. */
                continue;
            }
            try {
                alerts.findOwned(alert.alertId(), alert.userId())
                        .map(Alert::expired)
                        .ifPresent(alerts::save);
            } finally {
                lock.unlock();
            }
        }
    }
}
