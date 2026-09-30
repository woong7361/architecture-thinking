package com.thinking.tennis.app;

import com.thinking.tennis.domain.CourtDayAvailability;
import com.thinking.tennis.domain.UpstreamFailureReason;

/**
 * 예약 상태를 읽은 결과다. 돌려줄 확인 결과와 그 결과가 오래된 것인지를 함께 담는다.
 *
 * <p>확인에 실패해도 저장된 마지막 성공 결과가 있으면 그 결과를 돌려주고 {@code stale} 을 세운다.
 * 처리 자체는 성공했고 달라진 것은 데이터의 신선도라서, 실패를 상태 코드로 옮기는 자리가 아니다.
 *
 * <p>코트의 신원은 확인 결과가 아니라 이 자리에 담는다. 확인 결과는 오래될 수 있지만 이름과 예약 화면
 * 주소는 읽는 시점의 값이어야 하고, 그 규칙은 신청 조회와 같다.
 *
 * @param court       지금 그 코트의 이름과 예약 화면 주소
 * @param stale       가장 최근 확인이 실패해 그 전에 성공한 결과를 돌려주는지 여부
 * @param staleReason 오래된 결과를 돌려주는 이유. {@code stale} 이 false이면 null이다
 */
public record AvailabilityView(CourtDayAvailability availability,
                               CourtIdentity court,
                               boolean stale,
                               UpstreamFailureReason staleReason) {

    public static AvailabilityView fresh(CourtDayAvailability availability, CourtIdentity court) {
        return new AvailabilityView(availability, court, false, null);
    }

    public static AvailabilityView stale(CourtDayAvailability availability,
                                         CourtIdentity court,
                                         UpstreamFailureReason reason) {
        return new AvailabilityView(availability, court, true, reason);
    }
}
