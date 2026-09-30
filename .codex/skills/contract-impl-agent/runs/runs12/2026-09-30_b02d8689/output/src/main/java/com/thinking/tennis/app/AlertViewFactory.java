package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.CourtDayAvailability;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * 신청에 확인 상태를 붙여 읽을 모양으로 만든다.
 *
 * <p>마지막 확인 시각은 그 신청의 코트·날짜를 마지막으로 확인하는 데 성공한 시각이다. 신청마다 따로
 * 확인하지 않고 코트·날짜 단위로 확인하므로, 저장된 확인 결과에서 가져온다.
 *
 * <p>코트 이름과 예약 화면 주소는 신청에 보관된 값이 아니라 지금 그 코트의 값이다. 그 규칙은
 * {@link CourtIdentity} 가 한자리에서 정하고 예약 상태 조회도 같은 자리를 지난다. 두 표면이 같은 코트를
 * 다른 시점의 이름과 링크로 말하면 어느 쪽이 참인지 고를 근거가 없기 때문이다.
 */
@Component
public class AlertViewFactory {

    private final AvailabilityRepository availability;
    private final CourtAvailabilityPort courts;
    private final Clock clock;

    public AlertViewFactory(AvailabilityRepository availability,
                            CourtAvailabilityPort courts,
                            Clock clock) {
        this.availability = availability;
        this.courts = courts;
        this.clock = clock;
    }

    public AlertView of(Alert alert) {
        Instant lastCheckedAt = availability.findLastSuccess(alert.courtId(), alert.date())
                .map(CourtDayAvailability::confirmedAt)
                .orElse(null);
        CourtIdentity court = CourtIdentity.read(courts, alert.courtId(),
                alert.courtName(), alert.reservationUrl());
        return new AlertView(alert,
                court.courtName(),
                court.reservationUrl(),
                lastCheckedAt,
                isCheckDelayed(alert, lastCheckedAt));
    }

    /*
     * 마지막 성공 확인이 확인 간격의 세 배를 넘기면 확인이 밀리고 있는 것으로 본다. 한 번도 확인에
     * 성공하지 못한 신청은 신청 시각을 기준으로 같은 규칙을 적용한다.
     */
    private boolean isCheckDelayed(Alert alert, Instant lastCheckedAt) {
        Instant since = Optional.ofNullable(lastCheckedAt).orElse(alert.createdAt());
        return clock.instant().isAfter(since.plus(WatchPolicy.CHECK_DELAY_THRESHOLD));
    }
}
