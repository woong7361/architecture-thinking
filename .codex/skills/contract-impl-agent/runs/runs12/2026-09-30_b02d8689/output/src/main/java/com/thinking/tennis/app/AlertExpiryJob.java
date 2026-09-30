package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * 만료 시각이 지난 신청을 만료로 옮긴다. 만료되면 확인과 발송을 멈춘다.
 *
 * <p>계약에 이 전이를 일으키는 엔드포인트가 없는 이유는 서버가 스스로 하는 일이기 때문이다. 그래서
 * 조회가 이 전이를 앞당기지 않고, 이 작업이 아직 돌지 않은 사이에는 만료 시각이 지난 신청이 잠시
 * 감시 중으로 보인다. 계약이 그 틈을 인정하고 그 사이에는 만료 시각을 기준으로 판단하라고 적었다.
 */
@Component
public class AlertExpiryJob {

    private final AlertRepository alerts;
    private final Clock clock;

    public AlertExpiryJob(AlertRepository alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1S")
    public void expireDueAlerts() {
        Instant now = clock.instant();
        for (Alert alert : alerts.findAllWatching()) {
            if (alert.isDue(now)) {
                /*
                 * 경합에 밀렸으면 다른 요청이 그 신청을 이미 끝냈다는 뜻이다. 끝난 신청은 되살아나지
                 * 않으므로 다시 시도하지 않고 넘어간다.
                 */
                alerts.compareAndSet(alert, alert.expired());
            }
        }
    }
}
