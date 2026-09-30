package com.thinking.tennis.app;

import com.thinking.tennis.domain.CourtDayAvailability;
import com.thinking.tennis.domain.UpstreamFailureReason;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 코트·날짜 단위로 확인 결과를 보관하는 포트다.
 *
 * <p>성공과 실패를 따로 보관한다. 실패가 성공을 덮어쓰면 확인 실패가 빈자리 없음으로 보이게 되고,
 * 그것은 EF-1이 금지한 것이다. 마지막 성공 결과는 남겨 두고 실패는 신선도를 낮추는 사실로만 쓴다.
 */
public interface AvailabilityRepository {

    Optional<CourtDayAvailability> findLastSuccess(String courtId, LocalDate date);

    void storeSuccess(CourtDayAvailability availability);

    /** 마지막 확인 실패다. 확인 간격 안에 들어온 다른 요청이 예약처를 다시 조회하지 않게 한다. */
    Optional<FailedCheck> findLastFailure(String courtId, LocalDate date);

    void storeFailure(String courtId, LocalDate date, UpstreamFailureReason reason, Instant failedAt);

    void clearFailure(String courtId, LocalDate date);

    /**
     * 확인이 실패한 사실이다.
     *
     * @param failedAt 실패한 시각. 이 시각을 확인 간격과 견주어 다시 조회할지 정한다
     */
    record FailedCheck(UpstreamFailureReason reason, Instant failedAt) {
    }
}
