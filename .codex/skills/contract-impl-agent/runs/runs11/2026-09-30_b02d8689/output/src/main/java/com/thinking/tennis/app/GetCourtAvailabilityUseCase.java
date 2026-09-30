package com.thinking.tennis.app;

import com.thinking.tennis.domain.Court;
import com.thinking.tennis.port.CourtAvailabilityPort;
import com.thinking.tennis.port.CourtAvailabilityPort.CheckFailed;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtNotSupportedException;
import com.thinking.tennis.port.CourtAvailabilityPort.SupportedCourt;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 코트 하나의 하루치 예약 상태를 돌려준다.
 *
 * <p>확인 간격과 중복 조회 억제가 이 자리의 몫이다. 포트는 부르면 그때 확인하므로 얼마나 자주 부를지와
 * 같은 코트·날짜로 몰린 요청을 어떻게 한 번으로 묶을지는 여기서 정한다.
 *
 * <p>묶는 방식은 코트·날짜마다 잠금 하나다. 앞선 요청이 확인하는 동안 들어온 요청은 예약처를 따로
 * 조회하지 않고 잠금 앞에서 기다리고, 깨어나면 방금 기록된 시도를 보고 같은 결과를 받는다. 성공이면
 * 같은 내용을, 실패면 같은 실패를 받는다. 그래서 피크에 200건이 몰려도 예약처로 나가는 조회는 확인
 * 간격마다 한 번이다.
 */
@Service
public class GetCourtAvailabilityUseCase {

    private final CourtAvailabilityPort courtAvailability;
    private final AvailabilityStore store;
    private final Clock clock;

    public GetCourtAvailabilityUseCase(CourtAvailabilityPort courtAvailability,
                                       AvailabilityStore store,
                                       Clock clock) {
        this.courtAvailability = courtAvailability;
        this.store = store;
        this.clock = clock;
    }

    /**
     * 서비스가 지원하는 코트인지 확인한다.
     *
     * @param origin 지원하지 않을 때 그 사실을 어디서 받았는지. 계약이 경로와 본문에 다른 상태 코드를
     *               붙이므로 판단하는 자리가 아니라 요청을 받은 자리가 정한다
     */
    public Court supportedCourt(String courtId, AlertOperationFailure.Origin origin) {
        return courtAvailability.findCourt(courtId)
                .map(GetCourtAvailabilityUseCase::toCourt)
                .orElseThrow(() -> new AlertOperationFailure.UnsupportedCourt(courtId, origin));
    }

    public AvailabilityView availability(String courtId, LocalDate date) {
        supportedCourt(courtId, AlertOperationFailure.Origin.PATH);

        Optional<AvailabilityStore.Entry> cached = store.find(courtId, date);
        if (cached.isPresent() && withinCheckInterval(cached.get())) {
            return view(cached.get());
        }

        synchronized (store.checkLock(courtId, date)) {
            /* 기다리는 동안 앞선 요청이 확인을 끝냈으면 그 결과를 그대로 받는다. */
            Optional<AvailabilityStore.Entry> refreshed = store.find(courtId, date);
            if (refreshed.isPresent() && withinCheckInterval(refreshed.get())) {
                return view(refreshed.get());
            }
            return view(check(courtId, date));
        }
    }

    private AvailabilityStore.Entry check(String courtId, LocalDate date) {
        Instant attemptedAt = clock.instant();
        try {
            return store.recordSuccess(courtAvailability.checkAvailability(courtId, date), attemptedAt);
        } catch (CheckFailed failed) {
            /* 실패도 기록한다. 기록하지 않으면 뒤따라 들어온 요청마다 예약처로 조회가 나간다. */
            return store.recordFailure(courtId, date, attemptedAt, failed.reason());
        } catch (CourtNotSupportedException notSupported) {
            throw new AlertOperationFailure.UnsupportedCourt(
                    notSupported.courtId(), AlertOperationFailure.Origin.PATH);
        }
    }

    /**
     * 기록 하나를 돌려줄 모양으로 바꾼다. 마지막 시도가 실패했고 저장된 성공 결과도 없을 때만 실패로
     * 나간다. 확인 실패를 빈자리 없음으로 다루지 않으려는 것이다.
     */
    private AvailabilityView view(AvailabilityStore.Entry entry) {
        if (entry.lastFailure() == null) {
            return new AvailabilityView(entry.lastSuccess(), false, null);
        }
        if (entry.lastSuccess() != null) {
            return new AvailabilityView(entry.lastSuccess(), true, entry.lastFailure());
        }
        throw new AlertOperationFailure.AvailabilityUnavailable(entry.lastFailure());
    }

    private boolean withinCheckInterval(AvailabilityStore.Entry entry) {
        return clock.instant().isBefore(entry.lastAttemptAt().plus(AvailabilityPolicy.CHECK_INTERVAL));
    }

    private static Court toCourt(SupportedCourt court) {
        return new Court(court.courtId(), court.courtName(), court.reservationUrl());
    }
}
