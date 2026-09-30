package com.thinking.tennis.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Problem", requiredProperties = {"type", "title", "status", "code", "retryable"})
public class ProblemResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
    private String type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int status;
    private String detail;
    @Schema(format = "uri-reference")
    private String instance;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean retryable;
    @Schema(minimum = "1")
    private Integer retryAfterSeconds;
    private String traceId;

    public ProblemResponse(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code,
            boolean retryable,
            Integer retryAfterSeconds,
            String traceId) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.code = code;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
        this.traceId = traceId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public String getInstance() {
        return instance;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getTraceId() {
        return traceId;
    }
}
