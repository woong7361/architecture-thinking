package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertService;
import com.thinking.tennis.app.ApplicationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(operationId = "createAlert", summary = "빈자리 알림 신청")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신청이 만들어졌다.",
                    headers = {
                            @Header(name = "Location", required = true),
                            @Header(name = "Idempotency-Replayed", required = true)
                    },
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiModels.AlertResponse.class))),
            @ApiResponse(responseCode = "200", description = "이미 감시 중인 신청",
                    headers = {
                            @Header(name = "Location", required = true),
                            @Header(name = "Idempotency-Replayed", required = true)
                    },
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiModels.AlertResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청이 올바르지 않음",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "401", description = "인증이 필요함",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "409", description = "멱등 키 재사용",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "422", description = "신청 조건을 받아들일 수 없음",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "502", description = "예약처 응답을 읽지 못함",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "503", description = "일시적으로 처리할 수 없음",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "504", description = "예약처 시간 초과",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiModels.AlertResponse> createAlert(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ApiModels.AlertRequest request) {
        String subject = authenticate(authorization);
        validateIdempotencyKey(idempotencyKey);
        validateRequest(request);

        AlertService.CreateResult result = alertService.createAlert(subject, idempotencyKey, request);
        UUID id = UUID.fromString(result.body().alertId());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/v1/alerts/" + id));
        headers.set("Idempotency-Replayed", Boolean.toString(result.replayed()));
        return new ResponseEntity<>(result.body(), headers,
                org.springframework.http.HttpStatus.valueOf(result.status()));
    }

    @Operation(operationId = "listAlerts", summary = "내 알림 신청 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiModels.AlertListResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "401", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "500", headers = @Header(name = "Retry-After", required = true),
                    content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", required = true),
                    content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiModels.AlertListResponse listAlerts(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "status", required = false) String status) {
        String subject = authenticate(authorization);
        return new ApiModels.AlertListResponse(alertService.listAlerts(subject, parseStatus(status)));
    }

    @Operation(operationId = "getAlert", summary = "알림 신청 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiModels.AlertResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "401", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "404", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "500", headers = @Header(name = "Retry-After", required = true),
                    content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", required = true),
                    content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class)))
    })
    @GetMapping(value = "/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiModels.AlertResponse getAlert(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String alertId) {
        return alertService.getAlert(authenticate(authorization), parseAlertId(alertId));
    }

    @Operation(operationId = "cancelAlert", summary = "알림 신청 해제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiModels.AlertResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "401", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "404", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "500", headers = @Header(name = "Retry-After", required = true),
                    content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", required = true),
                    content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiModels.Problem.class)))
    })
    @DeleteMapping(value = "/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiModels.AlertResponse cancelAlert(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String alertId) {
        return alertService.cancelAlert(authenticate(authorization), parseAlertId(alertId));
    }

    private String authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ApplicationException.unauthenticated();
        }
        String subject = authorization.substring("Bearer ".length()).trim();
        if (subject.isEmpty()) {
            throw ApplicationException.unauthenticated();
        }
        return subject;
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            throw ApplicationException.validation("Idempotency-Key: 필수 헤더입니다");
        }
        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw ApplicationException.validation("Idempotency-Key: UUID 형식이어야 합니다");
        }
    }

    private void validateRequest(ApiModels.AlertRequest request) {
        if (request == null || request.courtId() == null || request.courtId().isBlank()
                || request.date() == null || request.slot() == null
                || request.slot().startTime() == null || request.slot().endTime() == null) {
            throw ApplicationException.validation("요청 본문이 올바르지 않습니다");
        }
        boolean validCourt = request.courtId().length() <= 64
                && request.courtId().matches("^[a-z0-9][a-z0-9-]*$");
        boolean validStart = request.slot().startTime()
                .matches("^([01][0-9]|2[0-3]):[0-5][0-9]$");
        boolean validEnd = request.slot().endTime()
                .matches("^([01][0-9]|2[0-3]):[0-5][0-9]$");
        if (!validCourt || !validStart || !validEnd) {
            throw ApplicationException.validation("요청 본문이 올바르지 않습니다");
        }
    }

    private String parseStatus(String status) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw ApplicationException.validation("status: 빈 값은 허용되지 않습니다");
        }
        List<String> allowed = List.of("WATCHING", "NOTIFIED", "CANCELED", "EXPIRED");
        if (!allowed.contains(status)) {
            throw ApplicationException.validation("status: 알 수 없는 상태입니다");
        }
        return status;
    }

    private UUID parseAlertId(String alertId) {
        try {
            return UUID.fromString(alertId);
        } catch (IllegalArgumentException exception) {
            throw ApplicationException.validation("alertId: UUID 형식이어야 합니다");
        }
    }
}
