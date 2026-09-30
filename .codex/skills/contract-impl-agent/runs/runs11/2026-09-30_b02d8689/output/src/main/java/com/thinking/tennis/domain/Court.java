package com.thinking.tennis.domain;

/**
 * 서비스가 지원하는 코트 하나다.
 *
 * <p>포트의 {@code SupportedCourt} 와 같은 것을 담지만 안쪽의 타입으로 따로 둔다. 도메인이 아웃바운드
 * 포트의 타입을 그대로 들고 다니면 예약처를 가져오는 방식이 신청의 모양을 정하게 된다.
 *
 * @param courtId        코트 식별자
 * @param courtName      사람이 읽는 코트 이름
 * @param reservationUrl 외부 예약 사이트의 이 코트 예약 화면
 */
public record Court(String courtId, String courtName, String reservationUrl) {
}
