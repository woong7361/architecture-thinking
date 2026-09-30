package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.AlertRequest;
import com.thinking.tennis.api.dto.Patterns;
import com.thinking.tennis.api.dto.TimeSlot;
import com.thinking.tennis.app.CreateAlertUseCase;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 요청이 계약의 스키마를 만족하는지 본다.
 *
 * <p>손으로 보는 이유는 스켈레톤의 의존에 빈 검증 구현이 없기 때문이다. 검증 선언만 붙여 두면 아무것도
 * 검사하지 않는 채로 통과하고, 계약이 400으로 거절하라고 한 요청이 저장까지 내려간다. 빌드 파일은
 * 고칠 수 없는 경로라서 구현을 끌어올 수도 없다.
 *
 * <p>스키마 선언과 같은 정규식을 쓴다. 둘이 갈라지면 스펙은 좁게 말하고 코드는 넓게 받는다.
 */
final class RequestValidation {

    private static final Pattern COURT_ID = Pattern.compile(Patterns.COURT_ID);
    private static final Pattern TIME = Pattern.compile(Patterns.TIME);

    /** 경로에서 받은 코트 식별자의 형식이다. 형식이 틀린 것은 지원 여부를 묻기 전에 거절한다. */
    static void requireCourtIdFormat(String courtId) {
        if (courtId == null || courtId.isEmpty()) {
            throw new ApiFailure.ValidationFailed("courtId: 코트 식별자가 필요합니다");
        }
        if (courtId.length() > Patterns.COURT_ID_MAX_LENGTH) {
            throw new ApiFailure.ValidationFailed(
                    "courtId: " + Patterns.COURT_ID_MAX_LENGTH + "자를 넘을 수 없습니다");
        }
        if (!COURT_ID.matcher(courtId).matches()) {
            throw new ApiFailure.ValidationFailed("courtId: " + Patterns.COURT_ID + " 형식이어야 합니다");
        }
    }

    /** 신청 본문을 유스케이스가 받는 조건으로 바꾼다. */
    static CreateAlertUseCase.Command toCommand(String userId, AlertRequest body) {
        if (body == null) {
            throw new ApiFailure.ValidationFailed("본문이 필요합니다");
        }
        requireCourtIdFormat(body.courtId());
        if (body.date() == null) {
            throw new ApiFailure.ValidationFailed("date: 날짜 형식이어야 합니다");
        }
        if (body.slot() == null) {
            throw new ApiFailure.ValidationFailed("slot: 시간대가 필요합니다");
        }
        return new CreateAlertUseCase.Command(userId, body.courtId(), body.date(), toDomain(body.slot()));
    }

    private static com.thinking.tennis.domain.TimeSlot toDomain(TimeSlot slot) {
        return new com.thinking.tennis.domain.TimeSlot(
                time("slot.startTime", slot.startTime()),
                time("slot.endTime", slot.endTime()));
    }

    private static LocalTime time(String field, String value) {
        if (value == null || !TIME.matcher(value).matches()) {
            throw new ApiFailure.ValidationFailed(field + ": HH:mm 형식이어야 합니다");
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException invalid) {
            throw new ApiFailure.ValidationFailed(field + ": HH:mm 형식이어야 합니다");
        }
    }

    private RequestValidation() {
    }
}
