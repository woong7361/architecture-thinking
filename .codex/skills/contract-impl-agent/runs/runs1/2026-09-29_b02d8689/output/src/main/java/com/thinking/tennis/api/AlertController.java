package com.thinking.tennis.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.thinking.tennis.app.AlertService;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.TimeWindow;
import com.thinking.tennis.domain.UserId;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping(path = "/alerts")
public class AlertController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> ALERT_FIELDS = Set.of("courtId", "date", "slot");
    private static final Set<String> SLOT_FIELDS = Set.of("startTime", "endTime");

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping(
            name = "createAlert",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiModels.AlertResponse> createAlert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) JsonNode body
    ) {
        UserId owner = ApiAuthentication.userFromAuthorization(authorization);
        UUID key = parseRequiredUuid(idempotencyKey, "Idempotency-Key");
        AlertService.CreateAlertCommand command = parseCreateCommand(body);
        AlertService.CreateAlertResult result = alertService.createAlert(owner, key, command);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create("/v1/alerts/" + result.alert().alertId()));
        headers.add("Idempotency-Replayed", Boolean.toString(result.replayed()));
        return ResponseEntity.status(result.created() ? 201 : 200)
                .headers(headers)
                .body(ApiModels.alert(result.alert()));
    }

    @GetMapping(name = "listAlerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiModels.AlertListResponse> listAlerts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "status", required = false) String status
    ) {
        UserId owner = ApiAuthentication.userFromAuthorization(authorization);
        Optional<Alert.AlertStatus> parsedStatus = parseStatus(status);
        return ResponseEntity.ok(ApiModels.alertList(alertService.listAlerts(owner, parsedStatus)));
    }

    @GetMapping(name = "getAlert", path = "/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiModels.AlertResponse> getAlert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("alertId") String alertId
    ) {
        UserId owner = ApiAuthentication.userFromAuthorization(authorization);
        UUID parsedId = parseAlertId(alertId);
        return ResponseEntity.ok(ApiModels.alert(alertService.getAlert(owner, parsedId)));
    }

    @DeleteMapping(name = "cancelAlert", path = "/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiModels.AlertResponse> cancelAlert(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("alertId") String alertId
    ) {
        UserId owner = ApiAuthentication.userFromAuthorization(authorization);
        UUID parsedId = parseAlertId(alertId);
        return ResponseEntity.ok(ApiModels.alert(alertService.cancelAlert(owner, parsedId)));
    }

    private static AlertService.CreateAlertCommand parseCreateCommand(JsonNode body) {
        requireObject(body, "body: JSON 객체여야 합니다");
        rejectUnknown(body, ALERT_FIELDS, "body");

        String courtId = requiredText(body, "courtId");
        if (courtId.length() > 64 || !courtId.matches("^[a-z0-9][a-z0-9-]*$")) {
            throw new RequestValidationException("courtId: 값이 올바르지 않습니다");
        }

        String dateText = requiredText(body, "date");
        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException exception) {
            throw new RequestValidationException("date: 날짜 형식이어야 합니다");
        }

        JsonNode slot = body.get("slot");
        requireObject(slot, "slot: 객체여야 합니다");
        rejectUnknown(slot, SLOT_FIELDS, "slot");
        LocalTime startTime = parseTime(requiredText(slot, "startTime"), "slot.startTime");
        LocalTime endTime = parseTime(requiredText(slot, "endTime"), "slot.endTime");
        return new AlertService.CreateAlertCommand(courtId, date, new TimeWindow(startTime, endTime));
    }

    private static Optional<Alert.AlertStatus> parseStatus(String status) {
        if (status == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Alert.AlertStatus.valueOf(status));
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException("status: 값이 올바르지 않습니다");
        }
    }

    private static UUID parseAlertId(String value) {
        return parseRequiredUuid(value, "alertId");
    }

    private static UUID parseRequiredUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RequestValidationException(field + ": UUID 형식이어야 합니다");
        }
        if (!value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            throw new RequestValidationException(field + ": UUID 형식이어야 합니다");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException(field + ": UUID 형식이어야 합니다");
        }
    }

    private static LocalTime parseTime(String value, String field) {
        if (!value.matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new RequestValidationException(field + ": HH:mm 형식이어야 합니다");
        }
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new RequestValidationException(field + ": HH:mm 형식이어야 합니다");
        }
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new RequestValidationException(field + ": 필수 문자열입니다");
        }
        return value.textValue();
    }

    private static void requireObject(JsonNode node, String message) {
        if (node == null || !node.isObject()) {
            throw new RequestValidationException(message);
        }
    }

    private static void rejectUnknown(JsonNode object, Set<String> allowed, String field) {
        Iterator<String> names = object.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed.contains(name)) {
                throw new RequestValidationException(field + "." + name + ": 허용되지 않는 필드입니다");
            }
        }
    }
}
