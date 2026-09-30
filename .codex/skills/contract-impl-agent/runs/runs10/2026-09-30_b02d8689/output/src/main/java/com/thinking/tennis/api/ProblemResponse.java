package com.thinking.tennis.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Problem", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record ProblemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
        String type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
        @Schema(nullable = true)
        String detail,
        @Schema(nullable = true, format = "uri-reference")
        String instance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean retryable,
        @Schema(nullable = true, minimum = "1")
        Integer retryAfterSeconds,
        @Schema(nullable = true)
        String traceId) {
}
