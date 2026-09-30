package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code AlertStatus} 다. 바깥으로 나가는 값의 집합과 그 설명을 이 자리가 소유한다.
 *
 * <p>도메인에 같은 이름의 열거형이 따로 있는 것은 중복이 아니라 경계다. 도메인은 직렬화 기술과 문서화
 * 어휘를 모르고, 응답에 무엇이 어떤 이름으로 실리는지는 인바운드 어댑터가 정한다.
 */
@Schema(
        description = """
                신청의 상태. 감시 중으로 시작해 나머지 셋 중 하나로 끝나며, 끝난 신청은 되살아나지 않는다.

                - `WATCHING`: 감시 중이다. 서버가 주기적으로 예약 상태를 확인한다.
                - `NOTIFIED`: 빈자리를 알리는 알림이 발송됐다. 서버가 발송에 성공한 시점에 스스로 옮긴다.
                - `CANCELED`: 사용자가 해제했다. 확인과 발송을 멈춘다.
                - `EXPIRED`: 만료 시각이 지났다. 서버가 시간이 되면 스스로 옮긴다.""",
        extensions = @Extension(properties = @ExtensionProperty(
                name = "x-requirement", value = "[\"FR-4\", \"FR-5\"]", parseValue = true)))
public enum AlertStatus {

    WATCHING,
    NOTIFIED,
    CANCELED,
    EXPIRED;

    public static AlertStatus of(com.thinking.tennis.domain.AlertStatus status) {
        return valueOf(status.name());
    }

    public com.thinking.tennis.domain.AlertStatus toDomain() {
        return com.thinking.tennis.domain.AlertStatus.valueOf(name());
    }
}
