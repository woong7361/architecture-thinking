package com.thinking.tennis.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class AlertController {

    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    private static final String DEFAULT_COURT_ID = "seoul-yangjae-1";
    private static final String DEFAULT_COURT_NAME = "양재 시민의 숲 테니스장 1번";
    private static final String DEFAULT_RESERVATION_URL =
            "https://yeyak.seoul.go.kr/reservation/yangjae-1";

    private final Map<UUID, StoredAlert> alerts = new ConcurrentHashMap<>();
    private final Map<String, IdempotentResponse> idempotency = new ConcurrentHashMap<>();

    @PostMapping("/alerts")
    public ResponseEntity<?> createAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AlertRequest request) {
        String userId = userId(authorization);
        if (userId == null) {
            return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", false);
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", false);
        }
        if (!isValid(request)) {
            return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", false);
        }
        if (!DEFAULT_COURT_ID.equals(request.courtId())) {
            return problem(HttpStatus.UNPROCESSABLE_ENTITY, "COURT_NOT_SUPPORTED", false);
        }

        String fingerprint = userId + "|" + request;
        IdempotentResponse previous = idempotency.get(idempotencyKey);
        if (previous != null) {
            if (!previous.fingerprint().equals(fingerprint)) {
                return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", false);
            }
            return ResponseEntity.status(previous.status()).body(previous.body());
        }

        Instant now = Instant.now();
        Instant expiresAt = LocalDateTime.of(request.date(), request.slot().startTime())
                .atZone(COURT_ZONE)
                .toInstant();
        if (!expiresAt.isAfter(now)) {
            return problem(HttpStatus.UNPROCESSABLE_ENTITY, "ALERT_WINDOW_CLOSED", false);
        }

        StoredAlert duplicate = alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> alert.status() == AlertStatus.WATCHING)
                .filter(alert -> alert.matches(request))
                .findFirst()
                .orElse(null);
        if (duplicate != null) {
            IdempotentResponse response = new IdempotentResponse(
                    fingerprint, HttpStatus.OK.value(), duplicate.toResponse());
            idempotency.put(idempotencyKey, response);
            return ResponseEntity.ok(duplicate.toResponse());
        }

        StoredAlert created = new StoredAlert(
                UUID.randomUUID(),
                userId,
                request.courtId(),
                DEFAULT_COURT_NAME,
                DEFAULT_RESERVATION_URL,
                request.date(),
                request.slot(),
                AlertStatus.WATCHING,
                now,
                expiresAt,
                null,
                false,
                null);
        alerts.put(created.alertId(), created);
        AlertResponse body = created.toResponse();
        idempotency.put(idempotencyKey, new IdempotentResponse(fingerprint, HttpStatus.CREATED.value(), body));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> listAlerts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "status", required = false) AlertStatus status) {
        String userId = userId(authorization);
        if (userId == null) {
            return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", false);
        }

        List<AlertResponse> items = alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> status == null || alert.status() == status)
                .sorted(Comparator.comparing(StoredAlert::createdAt).reversed())
                .map(StoredAlert::toResponse)
                .toList();
        return ResponseEntity.ok(new AlertList(items));
    }

    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<?> getAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("alertId") UUID alertId) {
        String userId = userId(authorization);
        if (userId == null) {
            return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", false);
        }
        StoredAlert alert = alerts.get(alertId);
        if (alert == null || !alert.userId().equals(userId)) {
            return problem(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", false);
        }
        return ResponseEntity.ok(alert.toResponse());
    }

    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<?> cancelAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("alertId") UUID alertId) {
        String userId = userId(authorization);
        if (userId == null) {
            return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", false);
        }
        StoredAlert current = alerts.get(alertId);
        if (current == null || !current.userId().equals(userId)) {
            return problem(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", false);
        }
        if (current.status() == AlertStatus.WATCHING) {
            StoredAlert canceled = current.withStatus(AlertStatus.CANCELED);
            alerts.put(alertId, canceled);
            current = canceled;
        }
        return ResponseEntity.ok(current.toResponse());
    }

    private static boolean isValid(AlertRequest request) {
        return request != null
                && request.courtId() != null
                && request.date() != null
                && request.slot() != null
                && request.slot().startTime() != null
                && request.slot().endTime() != null
                && request.slot().startTime().isBefore(request.slot().endTime());
    }

    private static String userId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static ResponseEntity<Problem> problem(HttpStatus status, String code, boolean retryable) {
        return ResponseEntity.status(status)
                .body(new Problem("about:blank", status.getReasonPhrase(), status.value(), code, retryable, null));
    }

    public record AlertRequest(String courtId, LocalDate date, TimeSlot slot) {
    }

    public record TimeSlot(LocalTime startTime, LocalTime endTime) {
    }

    public record AlertList(List<AlertResponse> items) {
    }

    public record AlertResponse(
            UUID alertId,
            String courtId,
            String courtName,
            String reservationUrl,
            LocalDate date,
            TimeSlot slot,
            AlertStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant lastCheckedAt,
            boolean checkDelayed,
            AlertDelivery delivery) {
    }

    public record AlertDelivery(
            DeliveryStatus status,
            int attemptCount,
            Instant lastAttemptAt,
            String failureReason) {
    }

    public record Problem(
            String type,
            String title,
            int status,
            String code,
            boolean retryable,
            String traceId) {
    }

    public enum AlertStatus {
        WATCHING,
        NOTIFIED,
        CANCELED,
        EXPIRED
    }

    public enum DeliveryStatus {
        PENDING,
        SENT,
        FAILED
    }

    private record IdempotentResponse(String fingerprint, int status, AlertResponse body) {
    }

    private record StoredAlert(
            UUID alertId,
            String userId,
            String courtId,
            String courtName,
            String reservationUrl,
            LocalDate date,
            TimeSlot slot,
            AlertStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant lastCheckedAt,
            boolean checkDelayed,
            AlertDelivery delivery) {

        private boolean matches(AlertRequest request) {
            return courtId.equals(request.courtId())
                    && date.equals(request.date())
                    && slot.equals(request.slot());
        }

        private StoredAlert withStatus(AlertStatus newStatus) {
            return new StoredAlert(
                    alertId, userId, courtId, courtName, reservationUrl, date, slot, newStatus,
                    createdAt, expiresAt, lastCheckedAt, checkDelayed, delivery);
        }

        private AlertResponse toResponse() {
            return new AlertResponse(
                    alertId, courtId, courtName, reservationUrl, date, slot, status,
                    createdAt, expiresAt, lastCheckedAt, checkDelayed, delivery);
        }
    }
}
