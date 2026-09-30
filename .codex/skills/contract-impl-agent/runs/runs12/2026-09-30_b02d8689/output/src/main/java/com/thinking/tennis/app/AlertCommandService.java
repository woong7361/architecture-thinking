package com.thinking.tennis.app;

import com.thinking.tennis.app.error.AlertNotFoundException;
import com.thinking.tennis.app.error.AlertWindowClosedException;
import com.thinking.tennis.app.error.AvailabilityUnavailableException;
import com.thinking.tennis.app.error.ConcurrentUpdateException;
import com.thinking.tennis.app.error.IdempotencyKeyReusedException;
import com.thinking.tennis.app.error.ScheduleUnknownException;
import com.thinking.tennis.app.error.SlotNotSupportedException;
import com.thinking.tennis.app.error.UnsupportedCourtException;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.CourtDayAvailability;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 신청을 만들고 해제하는 유스케이스다.
 *
 * <p>상태를 바꾸는 오퍼레이션 중 신청 생성만 멱등 키를 요구한다. 해제는 여러 번 불러도 결과가 같으므로
 * 키가 없어도 재시도가 안전하다.
 */
@Service
public class AlertCommandService {

    /** 멱등 키를 오퍼레이션 단위로 보관하므로 계약이 선언한 식별자를 그대로 쓴다. */
    private static final String CREATE_OPERATION = "createAlert";

    private final AlertRepository alerts;
    private final IdempotencyStore idempotency;
    private final AvailabilityQueryService availability;
    private final AlertViewFactory views;
    private final CourtAvailabilityPort courts;
    private final Clock clock;

    public AlertCommandService(AlertRepository alerts,
                              IdempotencyStore idempotency,
                              AvailabilityQueryService availability,
                              AlertViewFactory views,
                              CourtAvailabilityPort courts,
                              Clock clock) {
        this.alerts = alerts;
        this.idempotency = idempotency;
        this.availability = availability;
        this.views = views;
        this.courts = courts;
        this.clock = clock;
    }

    /**
     * 알림을 신청한다. 신청은 감시 중으로 시작한다.
     *
     * <p>같은 키로 같은 요청을 다시 보내면 새 신청을 만들지 않고 처음 돌려준 응답을 그대로 다시 돌려준다.
     * 재생되는 응답은 처음 처리한 시점의 내용이므로 그 뒤로 상태가 바뀌었어도 달라지지 않는다.
     *
     * <p>2xx로 끝난 응답만 키에 붙여 보관한다. 4xx는 요청을 고쳐야 풀리므로 기록하지 않고, 5xx는 같은
     * 키로 그대로 다시 보내 처리되게 해야 하므로 기록하지 않는다.
     */
    public CreateResult create(String userId, String idempotencyKey, AlertCondition condition) {
        IdempotencyStore.Claim claim = idempotency.claim(userId, CREATE_OPERATION, idempotencyKey,
                condition.fingerprint(), clock.instant());
        switch (claim.outcome()) {
            case IN_PROGRESS -> throw new ConcurrentUpdateException(
                    "같은 Idempotency-Key의 앞선 요청이 아직 처리 중입니다. 같은 요청을 그대로 다시 보내면 됩니다.");
            /*
             * 처리 중인 자리에서 내용이 다른 요청에는 재시도가 성공한다고 약속하지 않는다. 앞선 요청이
             * 2xx로 끝나면 같은 키로 다시 보낸 이 요청은 409를 받는다.
             */
            case IN_PROGRESS_OTHER_REQUEST -> throw new ConcurrentUpdateException(
                    "같은 Idempotency-Key의 앞선 요청이 아직 처리 중이고 그 요청의 내용이 이번 요청과 다릅니다. "
                            + "기다렸다 다시 보내면 앞선 요청이 어떻게 끝났는지에 따라 판정됩니다.");
            case FINGERPRINT_MISMATCH -> throw new IdempotencyKeyReusedException();
            case REPLAY -> {
                return new CreateResult(claim.view(), claim.statusCode(), true);
            }
            case STARTED -> {
                /* 이 요청이 처리를 시작한다. */
            }
            default -> throw new IllegalStateException("알 수 없는 멱등 판정: " + claim.outcome());
        }

        boolean recorded = false;
        try {
            CreateResult result = process(userId, condition);
            idempotency.complete(userId, CREATE_OPERATION, idempotencyKey,
                    result.statusCode(), result.view(), clock.instant());
            recorded = true;
            return result;
        } finally {
            if (!recorded) {
                idempotency.release(userId, CREATE_OPERATION, idempotencyKey);
            }
        }
    }

    /**
     * 신청을 해제한다. 감시와 발송을 멈추고 아직 보내지 않은 발송 대기는 버린다.
     *
     * <p>이미 끝난 신청에 다시 요청하면 상태를 바꾸지 않고 현재 상태를 돌려준다.
     */
    public AlertView cancel(String userId, String alertId) {
        Alert alert = alerts.findById(alertId)
                .filter(found -> found.ownedBy(userId))
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        if (alert.status().isTerminal()) {
            return views.of(alert);
        }
        Alert canceled = alert.canceled();
        if (!alerts.compareAndSet(alert, canceled)) {
            throw new ConcurrentUpdateException(
                    "같은 신청을 동시에 고치려는 요청이 있었습니다. 같은 요청을 그대로 다시 보내면 됩니다.");
        }
        return views.of(canceled);
    }

    private CreateResult process(String userId, AlertCondition condition) {
        CourtAvailabilityPort.SupportedCourt court = courts.findCourt(condition.courtId())
                .orElseThrow(() -> new UnsupportedCourtException(condition.courtId(),
                        UnsupportedCourtException.Source.BODY));

        CourtDayAvailability schedule = operatingSchedule(court, condition);
        if (schedule.operatingSlotsKnown() && !schedule.operates(condition.slot())) {
            throw new SlotNotSupportedException(condition.courtId(), condition.date(), condition.slot(),
                    schedule.operatingSlots());
        }

        Instant expiresAt = expiresAt(condition);
        if (!expiresAt.isAfter(clock.instant())) {
            throw new AlertWindowClosedException();
        }

        Alert candidate = Alert.watching(UUID.randomUUID().toString(), userId, court.courtId(),
                court.courtName(), court.reservationUrl(), condition.date(), condition.slot(),
                clock.instant(), expiresAt);
        Alert stored = alerts.insertIfNotWatching(candidate);
        boolean created = stored.alertId().equals(candidate.alertId());
        Alert current = created ? applyCheckedSchedule(stored, schedule) : stored;
        return new CreateResult(views.of(current), created ? 201 : 200, false);
    }

    /*
     * 신청을 판정하는 데 쓴 확인 결과를 그 신청 하나에도 적용한다.
     *
     * 확인 결과가 그 시간대를 빈자리로 말하는데 대기를 올리지 않으면, 이미 비어 있는 자리를 보고 신청한
     * 사용자는 누군가 그 코트·날짜를 확인 간격이 지난 뒤 다시 조회할 때까지 기다린다. 아무도 조회하지
     * 않으면 그 기회는 오지 않는다. 확인 결과를 손에 쥔 자리가 그 결과를 버리지 않는다.
     *
     * 이미 감시 중이던 신청을 돌려주는 경로에서는 하지 않는다. 그 신청의 대기는 앞선 확인이 이미 정했고,
     * 여기서 다시 손대면 확인 없이 대기를 되살리게 된다.
     *
     * 경합에 밀리면 넘어간다. 그 사이에 다른 요청이 이 신청을 해제했거나 만료시켰다는 뜻이다.
     */
    private Alert applyCheckedSchedule(Alert stored, CourtDayAvailability schedule) {
        if (!schedule.isAvailable(stored.slot())) {
            return stored;
        }
        Alert pending = stored.deliveryPending();
        if (pending == stored) {
            return stored;
        }
        return alerts.compareAndSet(stored, pending) ? pending : stored;
    }

    /*
     * 조건을 판정할 운영 시간대를 얻는다. 그 코트·날짜의 확인 결과가 유일한 출처이고, 확인은 실패할 수
     * 있다. 실패를 삼키면 운영하지 않는 시간대로 거절하거나 원인을 서버 결함으로 바꿔 말하게 되므로,
     * 무엇이 실패했는지를 지닌 예외로 옮겨 그대로 올려보낸다.
     *
     * 코트 식별자는 본문에서 받았다. 확인이 지원하지 않는 코트라고 답하면 경로에서 받았을 때와 다른
     * 상태 코드로 나가야 해서 받은 자리를 바꿔 다시 던진다.
     */
    private CourtDayAvailability operatingSchedule(CourtAvailabilityPort.SupportedCourt court,
                                                   AlertCondition condition) {
        try {
            return availability.knownSchedule(court, condition.date());
        } catch (AvailabilityUnavailableException e) {
            throw new ScheduleUnknownException(condition.courtId(), condition.date(), e.reason());
        } catch (UnsupportedCourtException e) {
            throw new UnsupportedCourtException(condition.courtId(), UnsupportedCourtException.Source.BODY);
        }
    }

    /*
     * 만료 시각은 이용 시작 시각이다. 날짜와 시작 시각은 코트가 있는 지역 기준이라 그 지역의 시간대로
     * 읽어 시점으로 옮긴다. 더 이른 예약 마감 시각은 포트가 드러내지 않으므로 이 자리에서는 알 수 없다.
     */
    private Instant expiresAt(AlertCondition condition) {
        return condition.date()
                .atTime(condition.slot().startTime())
                .atZone(WatchPolicy.COURT_ZONE)
                .toInstant();
    }

    /**
     * 신청 요청을 처리한 결과다.
     *
     * @param statusCode 201이면 신청을 만들었고 200이면 같은 조건을 감시 중인 신청을 돌려준다
     * @param replayed   같은 키로 앞서 처리한 응답을 그대로 다시 돌려준 것인지 여부
     */
    public record CreateResult(AlertView view, int statusCode, boolean replayed) {
    }
}
