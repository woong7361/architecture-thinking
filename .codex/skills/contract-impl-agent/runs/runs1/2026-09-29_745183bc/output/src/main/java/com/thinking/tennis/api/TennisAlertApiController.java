package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplicationService;
import com.thinking.tennis.app.AvailabilityApplicationService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TennisAlertApiController {

    private final AvailabilityApplicationService availabilityService;
    private final AlertApplicationService alertService;

    public TennisAlertApiController(AvailabilityApplicationService availabilityService,
                                    AlertApplicationService alertService) {
        this.availabilityService = availabilityService;
        this.alertService = alertService;
    }

    @GetMapping(value = "/courts/{courtId}/availability", name = "getCourtAvailability")
    public AvailabilityApplicationService.CourtAvailabilityView getCourtAvailability(
            @PathVariable String courtId,
            @RequestParam String date) {
        return availabilityService.getAvailability(courtId, date);
    }

    @PostMapping(value = "/alerts", name = "createAlert")
    public ResponseEntity<AlertApplicationService.AlertView> createAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) CreateAlertRequest request) {
        String userId = authenticate(authorization);
        UUID key = parseUuid(idempotencyKey);
        if (request == null) {
            throw AlertApplicationService.AppFailure.of(AlertApplicationService.FailureCode.VALIDATION_FAILED);
        }
        AlertApplicationService.CreateAlertResult result = alertService.createAlert(userId, key,
                new AlertApplicationService.CreateAlertCommand(request.courtId(), request.date(),
                        request.slot() == null ? null
                                : new AlertApplicationService.TimeSlotInput(request.slot().startTime(), request.slot().endTime())));
        return ResponseEntity.status(result.status())
                .location(URI.create("/v1/alerts/" + result.alert().alertId()))
                .header("Idempotency-Replayed", Boolean.toString(result.idempotencyReplayed()))
                .body(result.alert());
    }

    @GetMapping(value = "/alerts", name = "listAlerts")
    public AlertApplicationService.AlertList listAlerts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String status) {
        String userId = authenticate(authorization);
        Optional<AlertApplicationService.AlertStatus> parsedStatus = Optional.empty();
        if (status != null) {
            try {
                parsedStatus = Optional.of(AlertApplicationService.AlertStatus.valueOf(status));
            } catch (IllegalArgumentException failure) {
                throw AlertApplicationService.AppFailure.of(AlertApplicationService.FailureCode.VALIDATION_FAILED);
            }
        }
        return alertService.listAlerts(userId, parsedStatus);
    }

    @GetMapping(value = "/alerts/{alertId}", name = "getAlert")
    public AlertApplicationService.AlertView getAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String alertId) {
        return alertService.getAlert(authenticate(authorization), parseUuid(alertId));
    }

    @DeleteMapping(value = "/alerts/{alertId}", name = "cancelAlert")
    public AlertApplicationService.AlertView cancelAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String alertId) {
        return alertService.cancelAlert(authenticate(authorization), parseUuid(alertId));
    }

    private String authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            throw AlertApplicationService.AppFailure.of(AlertApplicationService.FailureCode.UNAUTHENTICATED);
        }
        return authorization.substring(7);
    }

    private UUID parseUuid(String value) {
        if (value == null) {
            throw AlertApplicationService.AppFailure.of(AlertApplicationService.FailureCode.VALIDATION_FAILED);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw AlertApplicationService.AppFailure.of(AlertApplicationService.FailureCode.VALIDATION_FAILED);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record CreateAlertRequest(String courtId, String date, TimeSlotRequest slot) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record TimeSlotRequest(String startTime, String endTime) {
    }
}
