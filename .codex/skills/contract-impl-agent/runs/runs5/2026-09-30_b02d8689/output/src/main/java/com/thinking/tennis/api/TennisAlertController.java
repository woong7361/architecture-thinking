package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplication;
import com.thinking.tennis.app.AvailabilityResult;
import com.thinking.tennis.app.CreateAlertResult;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.port.CourtAvailabilityPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
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

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.thinking.tennis.api.ApiModels.AlertListResponse;
import static com.thinking.tennis.api.ApiModels.AlertRequest;
import static com.thinking.tennis.api.ApiModels.AlertResponse;
import static com.thinking.tennis.api.ApiModels.AlertDeliveryResponse;
import static com.thinking.tennis.api.ApiModels.AvailabilitySlotResponse;
import static com.thinking.tennis.api.ApiModels.CourtAvailabilityResponse;
import static com.thinking.tennis.api.ApiModels.TimeSlotRequest;
import static com.thinking.tennis.api.ApiModels.TimeSlotResponse;

@RestController
@RequestMapping
public final class TennisAlertController {

    private static final Pattern COURT_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final AlertApplication application;

    public TennisAlertController(AlertApplication application) {
        this.application = application;
    }

    @GetMapping("/courts/{courtId}/availability")
    @OperationId("getCourtAvailability")
    public ResponseEntity<CourtAvailabilityResponse> getCourtAvailability(
            @PathVariable String courtId,
            @RequestParam String date
    ) {
        LocalDate parsedDate = parseDate(date, "date");
        validateCourtId(courtId);
        AvailabilityResult result = application.getCourtAvailability(courtId, parsedDate);
        return ResponseEntity.ok(toAvailabilityResponse(result));
    }

    @PostMapping("/alerts")
    @OperationId("createAlert")
    public ResponseEntity<AlertResponse> createAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) AlertRequest request
    ) {
        String ownerId = requireUser(authorization);
        UUID key = parseRequiredUuid(idempotencyKey, "Idempotency-Key");
        validateAlertRequest(request);
        LocalTime start = parseTime(request.slot().startTime(), "slot.startTime");
        LocalTime end = parseTime(request.slot().endTime(), "slot.endTime");
        validateCourtId(request.courtId());

        CreateAlertResult result = application.createAlert(
                ownerId,
                key,
                request.courtId(),
                request.date(),
                start,
                end
        );
        AlertResponse body = toAlertResponse(result.alert());
        String location = "/v1/alerts/" + result.alert().alertId();
        return ResponseEntity.status(result.status())
                .header(HttpHeaders.LOCATION, location)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(body);
    }

    @GetMapping("/alerts")
    @OperationId("listAlerts")
    public ResponseEntity<AlertListResponse> listAlerts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String status
    ) {
        String ownerId = requireUser(authorization);
        AlertStatus parsedStatus = parseStatus(status);
        List<AlertResponse> items = application.listAlerts(ownerId, parsedStatus).stream()
                .map(this::toAlertResponse)
                .toList();
        return ResponseEntity.ok(new AlertListResponse(items));
    }

    @GetMapping("/alerts/{alertId}")
    @OperationId("getAlert")
    public ResponseEntity<AlertResponse> getAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String alertId
    ) {
        String ownerId = requireUser(authorization);
        UUID parsedId = parseAlertId(alertId);
        return ResponseEntity.ok(toAlertResponse(application.getAlert(ownerId, parsedId)));
    }

    @DeleteMapping("/alerts/{alertId}")
    @OperationId("cancelAlert")
    public ResponseEntity<AlertResponse> cancelAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String alertId
    ) {
        String ownerId = requireUser(authorization);
        UUID parsedId = parseAlertId(alertId);
        return ResponseEntity.ok(toAlertResponse(application.cancelAlert(ownerId, parsedId)));
    }

    private String requireUser(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            throw new AuthenticationException();
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new AuthenticationException();
        }
        return token;
    }

    private void validateAlertRequest(AlertRequest request) {
        if (request == null || request.courtId() == null || request.date() == null || request.slot() == null) {
            throw new RequestValidationException("요청 본문의 필수 필드가 없습니다");
        }
        if (request.slot().startTime() == null || request.slot().endTime() == null) {
            throw new RequestValidationException("slot: 필수 필드입니다");
        }
    }

    private void validateCourtId(String courtId) {
        if (courtId == null || courtId.length() < 1 || courtId.length() > 64
                || !COURT_ID_PATTERN.matcher(courtId).matches()) {
            throw new RequestValidationException("courtId: 값이 올바르지 않습니다");
        }
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null) {
            throw new RequestValidationException(field + ": 필수 값입니다");
        }
        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (DateTimeParseException exception) {
            throw new RequestValidationException(field + ": 날짜 형식이어야 합니다");
        }
    }

    private LocalTime parseTime(String value, String field) {
        if (value == null || !TIME_PATTERN.matcher(value).matches()) {
            throw new RequestValidationException(field + ": HH:mm 형식이어야 합니다");
        }
        return LocalTime.parse(value);
    }

    private UUID parseRequiredUuid(String value, String field) {
        if (value == null) {
            throw new RequestValidationException(field + ": 필수 헤더입니다. UUID 형식의 값을 요구합니다");
        }
        try {
            if (!UUID_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException();
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException(field + ": UUID 형식이어야 합니다");
        }
    }

    private UUID parseAlertId(String value) {
        if (value == null) {
            throw new RequestValidationException("alertId: UUID 형식이어야 합니다");
        }
        try {
            if (!UUID_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException();
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException("alertId: UUID 형식이어야 합니다");
        }
    }

    private AlertStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AlertStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException("status: 값이 올바르지 않습니다");
        }
    }

    private AlertResponse toAlertResponse(Alert alert) {
        AlertDelivery delivery = alert.delivery();
        AlertDeliveryResponse deliveryResponse = delivery == null ? null : new AlertDeliveryResponse(
                delivery.status(),
                delivery.attemptCount(),
                delivery.lastAttemptAt(),
                delivery.failureReason()
        );
        return new AlertResponse(
                alert.alertId(),
                alert.courtId(),
                alert.courtName(),
                alert.reservationUrl(),
                alert.date(),
                new TimeSlotResponse(
                        formatTime(alert.startTime()),
                        formatTime(alert.endTime())
                ),
                alert.status(),
                alert.createdAt(),
                alert.expiresAt(),
                alert.lastCheckedAt(),
                alert.checkDelayed(),
                deliveryResponse
        );
    }

    private CourtAvailabilityResponse toAvailabilityResponse(AvailabilityResult result) {
        return new CourtAvailabilityResponse(
                result.snapshot().court().courtId(),
                result.snapshot().court().courtName(),
                result.snapshot().date(),
                result.snapshot().confirmedAt(),
                result.stale(),
                result.staleReason() == null ? null : result.staleReason().name(),
                result.snapshot().court().reservationUrl(),
                result.snapshot().slots().stream()
                        .map(slot -> new AvailabilitySlotResponse(
                                formatTime(slot.startTime()),
                                formatTime(slot.endTime()),
                                slot.available()
                        ))
                        .toList()
        );
    }

    private String formatTime(LocalTime time) {
        return HH_MM.format(time);
    }
}
