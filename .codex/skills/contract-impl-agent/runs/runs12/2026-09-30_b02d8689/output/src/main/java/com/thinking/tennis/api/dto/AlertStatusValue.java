package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code AlertStatus} 다.
 *
 * <p>도메인의 같은 이름 열거형을 그대로 쓰지 않고 표현용 열거형을 둔 이유는, 응답의 모양을 설명하는
 * 문장과 확장 키가 계약의 것이고 그것을 도메인에 얹으면 도메인이 표현 형식을 알게 되기 때문이다.
 */
@Schema(name = "AlertStatus",
        description = """
                신청의 상태. 감시 중으로 시작해 나머지 셋 중 하나로 끝나며, 끝난 신청은 되살아나지 않는다.

                - `WATCHING`: 감시 중이다. 예약 상태 조회가 일으킨 확인 결과로 빈자리를 판정한다. 그 코트·날짜를 아무도 조회하지 않으면 확인도 일어나지 않으며, 그 상태는 `checkDelayed`로 드러난다.
                - `NOTIFIED`: 빈자리를 알리는 알림이 발송됐다. 서버가 발송에 성공한 시점에 스스로 옮긴다.
                - `CANCELED`: 사용자가 해제했다. 확인과 발송을 멈춘다.
                - `EXPIRED`: 만료 시각이 지났다. 서버가 시간이 되면 스스로 옮긴다.
                """,
        extensions = @Extension(properties = @ExtensionProperty(
                name = "x-requirement", value = "[\"FR-4\",\"FR-5\"]", parseValue = true)))
public enum AlertStatusValue {

    WATCHING,
    NOTIFIED,
    CANCELED,
    EXPIRED;

    public static AlertStatusValue of(com.thinking.tennis.domain.AlertStatus status) {
        return valueOf(status.name());
    }

    public com.thinking.tennis.domain.AlertStatus toDomain() {
        return com.thinking.tennis.domain.AlertStatus.valueOf(name());
    }
}
