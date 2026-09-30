package com.thinking.tennis.app;

import com.thinking.tennis.app.error.AvailabilityUnavailableException;
import com.thinking.tennis.app.error.UnsupportedCourtException;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.CourtDayAvailability;
import com.thinking.tennis.domain.TimeSlot;
import com.thinking.tennis.domain.UpstreamFailureReason;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 코트·날짜의 예약 상태를 읽는 유스케이스다.
 *
 * <p>확인 간격과 중복 조회 억제를 여기서 정한다. 포트는 부르면 그때 확인하므로, 얼마나 자주 부를지와
 * 같은 코트·날짜로 몰린 요청을 어떻게 한 번으로 묶을지는 이 자리의 몫이다.
 *
 * <p>확인을 일으키는 것은 조회뿐이다. 감시 중인 신청만 보고 예약처를 도는 주기 작업을 두지 않아, 아무도
 * 조회하지 않는 코트·날짜는 확인도 일어나지 않는다. 요구사항이 그렇게 정했다.
 */
@Service
public class AvailabilityQueryService {

    private final CourtAvailabilityPort courts;
    private final AvailabilityRepository repository;
    private final AlertRepository alerts;
    private final Clock clock;

    /*
     * 코트·날짜마다 확인을 하나만 통과시키는 자리다. 기다리던 요청은 잠금을 얻은 뒤 저장된 결과를 다시
     * 보므로 예약처를 따로 조회하지 않고 같은 내용을 받는다. 피크 초당 200건이 몰려도 예약처로 나가는
     * 조회는 확인 간격마다 한 번이다.
     *
     * 이 표는 코트·날짜마다 항목 하나가 생기고 지워지지 않는다. 지나간 날짜의 항목도 남는다는 뜻이지만,
     * 자물쇠 하나가 차지하는 것이 작고 비우는 시점을 정하려면 확인이 끝났는지를 따로 알아야 해서
     * 이 run에서는 비우지 않는다.
     */
    private final Map<String, Object> checkLocks = new ConcurrentHashMap<>();

    public AvailabilityQueryService(CourtAvailabilityPort courts,
                                    AvailabilityRepository repository,
                                    AlertRepository alerts,
                                    Clock clock) {
        this.courts = courts;
        this.repository = repository;
        this.alerts = alerts;
        this.clock = clock;
    }

    /**
     * 코트·날짜의 예약 상태를 돌려준다. 저장된 결과가 없거나 확인 간격보다 오래됐으면 이 호출이 확인을
     * 일으킨다.
     *
     * @throws UnsupportedCourtException        지원하지 않는 코트다. 경로에서 받은 식별자다
     * @throws AvailabilityUnavailableException 확인이 실패했고 저장된 성공 결과도 없다
     */
    public AvailabilityView availability(String courtId, LocalDate date) {
        CourtAvailabilityPort.SupportedCourt court = courts.findCourt(courtId)
                .orElseThrow(() -> new UnsupportedCourtException(courtId, UnsupportedCourtException.Source.PATH));
        return resolve(court, date);
    }

    /**
     * 그 코트가 그 날짜에 운영하는 시간대를 돌려준다. 알림 신청이 조건을 판정할 때 쓴다.
     *
     * <p>조회와 같은 경로를 쓴다. 저장된 결과가 확인 간격 안이면 그것을 쓰고, 오래됐거나 한 번도 없으면
     * 이 호출이 확인을 일으킨다.
     *
     * @throws AvailabilityUnavailableException 확인이 실패했고 저장된 성공 결과도 없다. 운영 시간대를 모르는
     *                                         상태에서 지원하지 않는 시간대로 거절하면 확인 실패를 빈자리
     *                                         없음처럼 다루는 것이 되므로, 실패를 삼키지 않고 올려보낸다
     */
    public CourtDayAvailability knownSchedule(CourtAvailabilityPort.SupportedCourt court, LocalDate date) {
        return resolve(court, date).availability();
    }

    private AvailabilityView resolve(CourtAvailabilityPort.SupportedCourt court, LocalDate date) {
        CourtIdentity identity = CourtIdentity.of(court);
        Optional<CourtDayAvailability> stored = repository.findLastSuccess(court.courtId(), date);
        if (stored.isPresent() && isFresh(stored.get(), clock.instant())) {
            return AvailabilityView.fresh(stored.get(), identity);
        }
        synchronized (lockFor(court.courtId(), date)) {
            return checkUnderLock(court, identity, date);
        }
    }

    private AvailabilityView checkUnderLock(CourtAvailabilityPort.SupportedCourt court,
                                            CourtIdentity identity,
                                            LocalDate date) {
        Instant now = clock.instant();
        Optional<CourtDayAvailability> stored = repository.findLastSuccess(court.courtId(), date);
        if (stored.isPresent() && isFresh(stored.get(), now)) {
            /* 기다리는 동안 다른 요청이 확인을 끝냈다. 예약처를 다시 부르지 않고 그 결과를 함께 쓴다. */
            return AvailabilityView.fresh(stored.get(), identity);
        }
        Optional<AvailabilityRepository.FailedCheck> failure = repository.findLastFailure(court.courtId(), date);
        if (failure.isPresent() && isWithinFailureWindow(failure.get().failedAt(), now)) {
            /* 확인 간격 안에 이미 실패했다. 기다리던 요청은 모두 같은 실패를 받는다. */
            return failed(stored, identity, failure.get().reason());
        }
        try {
            CourtAvailabilityPort.CourtAvailabilityCheck check = courts.checkAvailability(court.courtId(), date);
            CourtDayAvailability confirmed = toDomain(check);
            repository.storeSuccess(confirmed);
            repository.clearFailure(court.courtId(), date);
            applyToWatchingAlerts(confirmed);
            return AvailabilityView.fresh(confirmed, identity);
        } catch (CourtAvailabilityPort.CheckFailed e) {
            UpstreamFailureReason reason = UpstreamFailureReason.valueOf(e.reason().name());
            repository.storeFailure(court.courtId(), date, reason, clock.instant());
            return failed(repository.findLastSuccess(court.courtId(), date), identity, reason);
        } catch (CourtAvailabilityPort.CourtNotSupportedException e) {
            throw new UnsupportedCourtException(e.courtId(), UnsupportedCourtException.Source.PATH);
        }
    }

    /*
     * 확인이 성공했으면 그 코트·날짜를 감시 중인 신청의 발송 대기를 이 결과에 맞춘다. 빈자리를 확인한
     * 신청은 대기에 올리고, 최신 결과에 빈자리가 없는 신청은 아직 보내지 않은 대기를 버린다.
     *
     * 보내는 일은 하지 않는다. 발송은 별도 작업의 몫이고 그 경로는 이 계약의 범위 밖이라, 여기서 오르는
     * 대기는 PENDING 에서 멈춘다. 그래서 감시 중인 신청의 발송 결과는 없거나 PENDING 이고, 계약이 적어 둔
     * 범위와 같다.
     *
     * 경합에 밀린 신청은 넘어간다. 다른 요청이 그 사이에 그 신청을 해제했거나 만료시켰다는 뜻이고,
     * 끝난 신청에 대기를 올릴 일은 없다. 다음 확인이 다시 이 자리를 지난다.
     *
     * 이 자리는 예약처 확인에 새로 성공한 직후에만 지난다. 저장된 결과가 확인 간격 안이어서 예약처를
     * 부르지 않은 경로는 여기 오지 않으므로, 그때 만들어진 신청은 자기 조건을 판정한 그 결과를 스스로
     * 적용한다. 신청을 만드는 자리가 같은 규칙으로 대기를 올린다.
     */
    private void applyToWatchingAlerts(CourtDayAvailability confirmed) {
        for (Alert alert : alerts.findWatchingByCourtDay(confirmed.courtId(), confirmed.date())) {
            Alert updated = confirmed.isAvailable(alert.slot())
                    ? alert.deliveryPending()
                    : alert.pendingDeliveryDiscarded();
            if (updated != alert) {
                alerts.compareAndSet(alert, updated);
            }
        }
    }

    /*
     * 확인에 실패해도 저장된 마지막 성공 결과가 있으면 그 결과를 돌려주고 오래된 것으로 표시한다.
     * 저장된 성공 결과가 한 번도 없을 때만 실패로 알린다.
     */
    private AvailabilityView failed(Optional<CourtDayAvailability> stored,
                                    CourtIdentity identity,
                                    UpstreamFailureReason reason) {
        return stored.map(availability -> AvailabilityView.stale(availability, identity, reason))
                .orElseThrow(() -> new AvailabilityUnavailableException(reason));
    }

    private boolean isFresh(CourtDayAvailability availability, Instant now) {
        return !now.isAfter(availability.confirmedAt().plus(WatchPolicy.CHECK_INTERVAL));
    }

    /*
     * 확인이 실패한 뒤 다음 확인을 언제 허용하는가. 창의 길이는 확인 간격과 같게 두되 경계는 배타로 둔다.
     *
     * 실패 응답은 클라이언트에게 확인 간격만큼 기다렸다 다시 보내라고 안내한다. 경계를 포함으로 두면
     * 안내한 그 시각에 도착한 요청이 새 확인을 일으키지 못하고 저장된 실패를 그대로 받아, 계약이
     * "그 초만큼 기다리면 결과가 달라질 수 있다"고 한 약속이 참이 아니게 된다.
     */
    private boolean isWithinFailureWindow(Instant failedAt, Instant now) {
        return now.isBefore(failedAt.plus(WatchPolicy.CHECK_INTERVAL));
    }

    private Object lockFor(String courtId, LocalDate date) {
        return checkLocks.computeIfAbsent(courtId + "@" + date, key -> new Object());
    }

    private static CourtDayAvailability toDomain(CourtAvailabilityPort.CourtAvailabilityCheck check) {
        List<CourtDayAvailability.SlotStatus> slots = new ArrayList<>(check.slots().size());
        for (CourtAvailabilityPort.SlotAvailability slot : check.slots()) {
            slots.add(new CourtDayAvailability.SlotStatus(
                    new TimeSlot(slot.startTime(), slot.endTime()), slot.available()));
        }
        return new CourtDayAvailability(check.court().courtId(), check.date(), check.confirmedAt(), slots);
    }
}
