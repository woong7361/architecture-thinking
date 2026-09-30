package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 계약의 {@code DeliveryStatus} 다. */
@Schema(description = """
        알림 한 건의 발송 결과.

        - `PENDING`: 빈자리를 확인해 발송 대기에 올랐고 아직 보내지 않았다.
        - `SENT`: 전송 서비스가 알림을 접수했다. 단말에 도착했는지와 사용자가 봤는지는 이 경로로 알 수 없다.
        - `FAILED`: 재시도를 모두 쓰고도 실패해 더 보내지 않는다.""")
public enum DeliveryStatus {

    PENDING,
    SENT,
    FAILED;

    public static DeliveryStatus of(com.thinking.tennis.domain.DeliveryStatus status) {
        return valueOf(status.name());
    }
}
