package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplicationService;
import com.thinking.tennis.app.AvailabilityService;
import com.thinking.tennis.app.UseCaseException;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class TennisAlertController {

    private final AvailabilityService availabilityService;
    private final AlertApplicationService alertApplicationService;

    public TennisAlertController(
            AvailabilityService availabilityService,
            AlertApplicationService alertApplicationService) {
        this.availabilityService = availabilityService;
        this.alertApplicationService = alertApplicationService;
    }

    @Tag(name = "availability")
    @Operation(operationId = "getCourtAvailability", summary = "코트·날짜의 예약 상태 조회", security = {})
    @ApiResponse(responseCode = "200", description = "저장된 마지막 확인 결과",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AvailabilityResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "VALIDATION_FAILED")))
    @ApiResponse(responseCode = "404", description = "경로가 가리킨 코트를 서비스가 다루지 않는다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "COURT_NOT_SUPPORTED")))
    @ApiResponse(responseCode = "500", description = "서버 내부에서 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "INTERNAL_ERROR")))
    @ApiResponse(responseCode = "502", description = "외부 예약처의 응답을 읽지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UPSTREAM_RESPONSE_UNREADABLE")))
    @ApiResponse(responseCode = "503", description = "예약 상태를 돌려줄 수 없다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UPSTREAM_UNAVAILABLE,STORAGE_TIMEOUT")))
    @ApiResponse(responseCode = "504", description = "외부 예약처가 제때 응답하지 않았다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UPSTREAM_TIMEOUT")))
    @GetMapping("/courts/{courtId}/availability")
    public AvailabilityResponse getCourtAvailability(
            @PathVariable String courtId,
            @RequestParam String date) {
        RequestValidation.requireText(courtId, "courtId");
        LocalDate parsedDate = RequestValidation.date(date);
        AvailabilitySnapshot snapshot =
                availabilityService.get(parsedDate, courtId, java.time.Clock.systemUTC().instant());
        return AvailabilityResponse.from(snapshot);
    }

    @Tag(name = "alerts")
    @Operation(operationId = "listAlerts", summary = "내 알림 신청 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "본인의 신청 목록",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AlertListResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "VALIDATION_FAILED")))
    @ApiResponse(responseCode = "401", description = "사용자를 식별하지 못했다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UNAUTHENTICATED")))
    @ApiResponse(responseCode = "500", description = "서버 내부에서 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "INTERNAL_ERROR")))
    @ApiResponse(responseCode = "503", description = "저장소가 제때 응답하지 않아 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "STORAGE_TIMEOUT")))
    @GetMapping("/alerts")
    public AlertListResponse listAlerts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String status) {
        String ownerId = BearerUser.resolveSubject(authorization);
        AlertStatus parsedStatus = parseStatus(status);
        List<AlertResponse> items = alertApplicationService.list(ownerId, parsedStatus)
                .stream()
                .map(AlertResponse::from)
                .toList();
        return new AlertListResponse(items);
    }

    @Tag(name = "alerts")
    @Operation(operationId = "createAlert", summary = "빈자리 알림 신청",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "신청이 만들어졌다",
            headers = {
                    @Header(name = HttpHeaders.LOCATION,
                            schema = @Schema(type = "string", format = "uri-reference")),
                    @Header(name = "Idempotency-Replayed", schema = @Schema(type = "boolean"))
            },
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AlertResponse.class)))
    @ApiResponse(responseCode = "200", description = "같은 조건을 감시 중인 신청이 이미 있다",
            headers = {
                    @Header(name = HttpHeaders.LOCATION,
                            schema = @Schema(type = "string", format = "uri-reference")),
                    @Header(name = "Idempotency-Replayed", schema = @Schema(type = "boolean"))
            },
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AlertResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청이 스키마를 만족하지 않는다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "VALIDATION_FAILED")))
    @ApiResponse(responseCode = "401", description = "사용자를 식별하지 못했다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UNAUTHENTICATED")))
    @ApiResponse(responseCode = "409", description = "멱등 키가 앞선 요청의 내용과 어긋난다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "IDEMPOTENCY_KEY_REUSED")))
    @ApiResponse(responseCode = "422", description = "요청 형식은 맞지만 조건을 감시할 수 없다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values",
                            value = "COURT_NOT_SUPPORTED,SLOT_NOT_SUPPORTED,ALERT_WINDOW_CLOSED")))
    @ApiResponse(responseCode = "500", description = "서버 내부에서 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "INTERNAL_ERROR")))
    @ApiResponse(responseCode = "502", description = "외부 예약처의 응답을 읽지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UPSTREAM_RESPONSE_UNREADABLE")))
    @ApiResponse(responseCode = "503", description = "저장소, 경합 또는 예약처 연결 실패",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values",
                            value = "STORAGE_TIMEOUT,CONCURRENT_UPDATE_CONFLICT,UPSTREAM_UNAVAILABLE")))
    @ApiResponse(responseCode = "504", description = "외부 예약처가 제때 응답하지 않았다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UPSTREAM_TIMEOUT")))
    @PostMapping("/alerts")
    public ResponseEntity<AlertResponse> createAlert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Parameter(required = true, schema = @Schema(type = "string", format = "uuid"))
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AlertRequest request) {
        String ownerId = BearerUser.resolveSubject(authorization);
        UUID.fromString(idempotencyKey == null ? "" : idempotencyKey);
        LocalDate date = RequestValidation.date(request.getDate());
        LocalTime startTime = RequestValidation.time(request.getSlot().getStartTime(), "slot.startTime");
        LocalTime endTime = RequestValidation.time(request.getSlot().getEndTime(), "slot.endTime");
        RequestValidation.requireText(request.getCourtId(), "courtId");
        AlertApplicationService.CreateResult result =
                alertApplicationService.create(ownerId, idempotencyKey, request.getCourtId(), date, startTime, endTime);
        AlertResponse body = AlertResponse.from(result.alert());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .header(HttpHeaders.LOCATION, "/v1/alerts/" + result.alert().alertId())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(body);
    }

    @Tag(name = "alerts")
    @Operation(operationId = "getAlert", summary = "알림 신청 단건 조회",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "신청 하나",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AlertResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "VALIDATION_FAILED")))
    @ApiResponse(responseCode = "401", description = "사용자를 식별하지 못했다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UNAUTHENTICATED")))
    @ApiResponse(responseCode = "404", description = "없는 신청과 남의 신청을 구분해 알리지 않는다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "ALERT_NOT_FOUND")))
    @ApiResponse(responseCode = "500", description = "서버 내부에서 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "INTERNAL_ERROR")))
    @ApiResponse(responseCode = "503", description = "저장소가 제때 응답하지 않아 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "STORAGE_TIMEOUT")))
    @GetMapping("/alerts/{alertId}")
    public AlertResponse getAlert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String alertId) {
        String ownerId = BearerUser.resolveSubject(authorization);
        return AlertResponse.from(alertApplicationService.get(ownerId, RequestValidation.alertId(alertId)));
    }

    @Tag(name = "alerts")
    @Operation(operationId = "cancelAlert", summary = "알림 신청 해제",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "해제 요청을 처리한 뒤의 신청 상태",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AlertResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "VALIDATION_FAILED")))
    @ApiResponse(responseCode = "401", description = "사용자를 식별하지 못했다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "UNAUTHENTICATED")))
    @ApiResponse(responseCode = "404", description = "없는 신청과 남의 신청을 구분해 알리지 않는다",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "ALERT_NOT_FOUND")))
    @ApiResponse(responseCode = "500", description = "서버 내부에서 처리하지 못했다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values", value = "INTERNAL_ERROR")))
    @ApiResponse(responseCode = "503", description = "저장소가 제때 응답하지 않았거나 경합에 밀렸다",
            headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemResponse.class)),
            extensions = @Extension(name = "x-error-codes",
                    properties = @ExtensionProperty(name = "values",
                            value = "STORAGE_TIMEOUT,CONCURRENT_UPDATE_CONFLICT")))
    @DeleteMapping("/alerts/{alertId}")
    public AlertResponse cancelAlert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String alertId) {
        String ownerId = BearerUser.resolveSubject(authorization);
        return AlertResponse.from(alertApplicationService.cancel(ownerId, RequestValidation.alertId(alertId)));
    }

    private AlertStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AlertStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new UseCaseException(
                    UseCaseException.Code.VALIDATION_FAILED,
                    "status: 요청 값이 올바르지 않습니다");
        }
    }

    @Schema(name = "AlertList", requiredProperties = {"items"})
    public static class AlertListResponse {
        @ArraySchema(schema = @Schema(implementation = AlertResponse.class))
        private final List<AlertResponse> items;

        public AlertListResponse(List<AlertResponse> items) {
            this.items = items;
        }

        public List<AlertResponse> getItems() {
            return items;
        }
    }
}
