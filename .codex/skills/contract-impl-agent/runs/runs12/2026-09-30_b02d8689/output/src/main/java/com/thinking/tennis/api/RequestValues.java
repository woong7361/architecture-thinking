package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.AlertStatusValue;
import com.thinking.tennis.domain.AlertStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 요청에서 받은 값이 계약이 정한 형식인지 보고 안쪽이 쓰는 값으로 옮긴다.
 *
 * <p>형식을 만족하지 않으면 처리하지 않고 거절한다. 계약보다 넓게 받는 것은 허용이지만 계약이 정한
 * 형식을 어긴 값을 그대로 안으로 들이면, 그 값이 어디서 어떤 실패로 터질지 알 수 없다.
 */
final class RequestValues {

    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
    private static final int COURT_ID_MAX_LENGTH = 64;
    private static final Pattern HOUR_MINUTE = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
    private static final DateTimeFormatter HOUR_MINUTE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern UUID_VALUE =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    static String courtId(String field, String raw) {
        requirePresent(field, raw);
        if (raw.length() > COURT_ID_MAX_LENGTH || !COURT_ID.matcher(raw).matches()) {
            throw invalid(field, "소문자와 숫자와 붙임표로 이뤄진 64자 이내의 코트 식별자여야 합니다");
        }
        return raw;
    }

    static LocalDate date(String field, String raw) {
        requirePresent(field, raw);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw invalid(field, "날짜 형식이어야 합니다");
        }
    }

    static LocalTime hourMinute(String field, String raw) {
        requirePresent(field, raw);
        if (!HOUR_MINUTE.matcher(raw).matches()) {
            throw invalid(field, "HH:mm 형식이어야 합니다");
        }
        return LocalTime.parse(raw, HOUR_MINUTE_FORMAT);
    }

    static String alertId(String field, String raw) {
        requirePresent(field, raw);
        if (!UUID_VALUE.matcher(raw).matches()) {
            throw invalid(field, "UUID 형식이어야 합니다");
        }
        return raw;
    }

    /**
     * 멱등 키다. 없는 것과 UUID가 아닌 것을 같은 실패로 다룬다.
     *
     * <p>둘 다 요청을 고쳐야 풀리고 고치는 방법도 같아서, 계약은 두 경우에 같은 코드를 쓰라고 한다.
     */
    static String idempotencyKey(String raw) {
        if (raw == null || raw.isBlank() || !UUID_VALUE.matcher(raw.trim()).matches()) {
            throw invalid("Idempotency-Key",
                    "필수 헤더입니다. 재시도 안전을 보장하기 위해 UUID 형식의 값을 요구합니다");
        }
        return raw.trim();
    }

    /**
     * 신청 목록을 거르는 상태다. 지정하지 않으면 끝난 신청까지 모두 돌려주므로 null이다.
     */
    static AlertStatus alertStatus(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (AlertStatusValue candidate : AlertStatusValue.values()) {
            if (candidate.name().equals(raw)) {
                return candidate.toDomain();
            }
        }
        throw invalid(field, "신청 상태여야 합니다");
    }

    private static void requirePresent(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid(field, "필수 값입니다");
        }
    }

    private static RequestValidationException invalid(String field, String message) {
        return new RequestValidationException(field + ": " + message);
    }

    private RequestValues() {
    }
}
