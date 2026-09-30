package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;

import java.time.Instant;

/**
 * 신청 하나를 읽은 결과다. 신청 자체와, 그 신청이 보는 코트·날짜의 확인 상태를 함께 담는다.
 *
 * <p>마지막 확인 시각과 확인 지연 여부를 신청에 저장하지 않고 읽는 시점에 계산해 이 모양으로 담는
 * 이유는, 그 두 값이 신청의 상태가 아니라 코트·날짜의 확인 결과에서 나오기 때문이다.
 *
 * <p>이 기록은 불변이다. 멱등 재생이 처음 처리한 시점의 내용을 그대로 돌려줄 수 있어야 해서, 재생에
 * 쓸 응답으로 이 값을 그대로 보관한다.
 *
 * @param courtName      지금 그 코트의 이름. 코트가 더 이상 지원되지 않으면 신청에 보관된 이름이다
 * @param reservationUrl 지금 그 코트의 예약 화면 주소. 알림에 담기는 링크와 같아야 하므로 신청 시점의
 *                       값이 아니라 읽는 시점의 값이다
 * @param lastCheckedAt  이 신청의 코트·날짜를 마지막으로 확인하는 데 성공한 시각. 한 번도 없으면 null이다
 * @param checkDelayed   마지막 성공 확인이 확인 간격의 세 배를 넘겼는지 여부
 */
public record AlertView(Alert alert,
                        String courtName,
                        String reservationUrl,
                        Instant lastCheckedAt,
                        boolean checkDelayed) {
}
