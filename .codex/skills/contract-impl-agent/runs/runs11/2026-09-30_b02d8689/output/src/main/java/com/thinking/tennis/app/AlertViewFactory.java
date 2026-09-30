package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 저장된 신청에 확인 기록에서 나오는 값을 붙인다.
 *
 * <p>확인 지연은 마지막 성공 확인이 확인 간격의 세 배를 넘긴 상태다. 한 주기만 걸러도 지연으로 보면
 * 작업이 조금 밀린 것까지 지연으로 표시되어 신호가 무뎌진다. 한 번도 확인에 성공하지 못한 신청은
 * 기준으로 삼을 확인 시각이 없으므로 신청한 시각에 같은 규칙을 적용한다.
 */
@Component
public class AlertViewFactory {

    private final AvailabilityStore availability;
    private final Clock clock;

    public AlertViewFactory(AvailabilityStore availability, Clock clock) {
        this.availability = availability;
        this.clock = clock;
    }

    public AlertView of(Alert alert) {
        Instant lastCheckedAt = availability
                .lastConfirmedAt(alert.court().courtId(), alert.date())
                .orElse(null);
        Instant since = lastCheckedAt != null ? lastCheckedAt : alert.createdAt();
        boolean delayed = clock.instant().isAfter(since.plus(AvailabilityPolicy.CHECK_DELAY_THRESHOLD));
        return new AlertView(alert, lastCheckedAt, delayed);
    }

    public List<AlertView> of(List<Alert> alerts) {
        return alerts.stream().map(this::of).toList();
    }
}
