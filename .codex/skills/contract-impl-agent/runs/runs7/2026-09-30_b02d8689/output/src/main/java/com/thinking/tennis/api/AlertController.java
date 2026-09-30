package com.thinking.tennis.api;

import com.thinking.tennis.api.ApiDtos.AlertListResponse;
import com.thinking.tennis.api.ApiDtos.AlertRequest;
import com.thinking.tennis.api.ApiDtos.AlertResponse;
import com.thinking.tennis.app.AlertCommand;
import com.thinking.tennis.app.AlertService;
import com.thinking.tennis.app.CreateAlertResult;
import com.thinking.tennis.domain.AlertStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/alerts")
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @OperationId("createAlert")
    public ResponseEntity<AlertResponse> createAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) AlertRequest request
    ) {
        String ownerId = Authentication.requireUser(authorization);
        String key = RequestValidation.uuid(idempotencyKey, "Idempotency-Key");
        AlertCommand command = RequestValidation.alertCommand(request);
        CreateAlertResult result = alertService.create(ownerId, key, command);
        AlertResponse response = AlertResponse.from(result.alert());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Location", "/v1/alerts/" + result.alert().alertId());
        headers.set("Idempotency-Replayed", Boolean.toString(result.replayed()));
        if (result.status() == 201) {
            return ResponseEntity.status(201).headers(headers).body(response);
        }
        return ResponseEntity.status(200).headers(headers).body(response);
    }

    @GetMapping
    @OperationId("listAlerts")
    @ResponseStatus(org.springframework.http.HttpStatus.OK)
    public ResponseEntity<AlertListResponse> listAlerts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "status", required = false) String status
    ) {
        String ownerId = Authentication.requireUser(authorization);
        AlertStatus requestedStatus = parseStatus(status);
        return ResponseEntity.ok(new AlertListResponse(
                alertService.list(ownerId, requestedStatus).stream()
                        .map(AlertResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/{alertId}")
    @OperationId("getAlert")
    @ResponseStatus(org.springframework.http.HttpStatus.OK)
    public ResponseEntity<AlertResponse> getAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String alertId
    ) {
        String ownerId = Authentication.requireUser(authorization);
        String validatedAlertId = RequestValidation.uuid(alertId, "alertId");
        return ResponseEntity.ok(AlertResponse.from(alertService.get(ownerId, validatedAlertId)));
    }

    @DeleteMapping("/{alertId}")
    @OperationId("cancelAlert")
    @ResponseStatus(org.springframework.http.HttpStatus.OK)
    public ResponseEntity<AlertResponse> cancelAlert(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String alertId
    ) {
        String ownerId = Authentication.requireUser(authorization);
        String validatedAlertId = RequestValidation.uuid(alertId, "alertId");
        return ResponseEntity.ok(AlertResponse.from(alertService.cancel(ownerId, validatedAlertId)));
    }

    private AlertStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return AlertStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw com.thinking.tennis.app.ApplicationFailure.of(
                    com.thinking.tennis.app.FailureCode.VALIDATION_FAILED,
                    400,
                    false,
                    null,
                    "status: 지원하지 않는 상태입니다"
            );
        }
    }
}
