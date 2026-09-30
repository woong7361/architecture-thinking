package com.thinking.tennis.api.dto;

/**
 * 계약이 적은 문자열 형식이다.
 *
 * <p>스키마 선언과 요청 검사가 같은 문자열을 보게 하려고 한 자리에 둔다. 선언과 검사가 각자의 정규식을
 * 들면 한쪽만 고쳐졌을 때 스펙은 좁게 말하고 코드는 넓게 받는다.
 */
public final class Patterns {

    /** 24시간제 시각. 계약의 {@code TimeSlot} 과 {@code AvailabilitySlot} 이 쓴다. */
    public static final String TIME = "^([01][0-9]|2[0-3]):[0-5][0-9]$";

    /** 코트 식별자. 계약의 {@code CourtId} 가 쓴다. */
    public static final String COURT_ID = "^[a-z0-9][a-z0-9-]*$";

    /** 코트 식별자의 길이 상한. 계약의 {@code CourtId} 가 정한다. */
    public static final int COURT_ID_MAX_LENGTH = 64;

    private Patterns() {
    }
}
