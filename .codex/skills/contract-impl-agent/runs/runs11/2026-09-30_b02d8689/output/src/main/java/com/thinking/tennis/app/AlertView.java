package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;

import java.time.Instant;

/**
 * 조회가 돌려주는 신청 하나다. 저장된 신청에 확인 기록에서 나오는 두 값을 붙였다.
 *
 * @param alert         저장된 신청
 * @param lastCheckedAt 이 신청의 코트·날짜를 마지막으로 확인하는 데 성공한 시각. 한 번도 없으면 null이다
 * @param checkDelayed  마지막 성공 확인이 확인 간격의 세 배를 넘겼는지 여부
 */
public record AlertView(Alert alert, Instant lastCheckedAt, boolean checkDelayed) {
}
