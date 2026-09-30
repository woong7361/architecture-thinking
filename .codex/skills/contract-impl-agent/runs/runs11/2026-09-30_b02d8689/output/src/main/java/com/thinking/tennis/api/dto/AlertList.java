package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 계약의 {@code AlertList} 다.
 *
 * <p>페이지를 나누지 않는다. 감시 중인 신청은 1인당 두세 건이라고 NFR-1이 보았고, 끝난 신청은 계속
 * 쌓이지만 보관 기간이 정해지기 전까지 응답 크기의 상한이 없다.
 */
public record AlertList(

        @Schema(description = "본인의 신청 전부. 최근에 신청한 것이 앞에 온다.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<Alert> items) {
}
