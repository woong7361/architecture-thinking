package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason;

/**
 * 조회가 돌려주는 예약 상태다.
 *
 * <p>{@code stale} 은 요청 처리의 실패가 아니라 돌려준 데이터가 얼마나 최신인지를 뜻한다. 확인에
 * 실패해도 저장된 마지막 성공 결과가 있으면 그 결과를 이 모양으로 돌려주고, 저장된 성공 결과가 없을
 * 때만 실패로 나간다.
 *
 * @param check       돌려줄 확인 결과. 오래된 결과일 수 있다
 * @param stale       가장 최근 확인이 실패해 그 전에 성공한 결과를 돌려주는지 여부
 * @param staleReason 오래된 결과를 돌려주는 이유. {@code stale} 이 false면 null이다
 */
public record AvailabilityView(CourtAvailabilityCheck check,
                               boolean stale,
                               UpstreamFailureReason staleReason) {
}
