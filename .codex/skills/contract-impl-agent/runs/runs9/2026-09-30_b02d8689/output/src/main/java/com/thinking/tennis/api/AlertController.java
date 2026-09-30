package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertService;
import com.thinking.tennis.app.AlertService.CreateAlertCommand;
import com.thinking.tennis.app.TennisAlertConstants;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
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

@RestController
@RequestMapping
public class AlertController {

    private final AlertService alertService;
    private final CurrentUser currentUser;
    private final DtoMapper dtoMapper;

    public AlertController(AlertService alertService, CurrentUser currentUser, DtoMapper dtoMapper) {
        this.alertService = alertService;
        this.currentUser = currentUser;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping(name = "createAlert", path = "/alerts", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiDtos.AlertDto> createAlert(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                        @RequestBody(required = false) ApiDtos.AlertRequestDto request,
                                                        HttpServletRequest servletRequest) {
        String userId = currentUser.require(servletRequest);
        UUID parsedKey = parseUuid(idempotencyKey, "Idempotency-Key");
        CreateAlertCommand command = toCommand(request);
        var result = alertService.createAlert(userId, parsedKey, command);
        ApiDtos.AlertDto body = result.replayed()
                ? ApiDtos.AlertDto.from(result.alert(), result.checkDelayed())
                : dtoMapper.toAlertDto(result.alert());
        return ResponseEntity.status(result.statusCode())
                .location(URI.create("/v1/alerts/" + result.alert().alertId()))
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(body);
    }

    @GetMapping(name = "listAlerts", path = "/alerts", produces = "application/json")
    public ResponseEntity<ApiDtos.AlertListDto> listAlerts(@RequestParam(required = false) String status,
                                                           HttpServletRequest request) {
        String userId = currentUser.require(request);
        AlertStatus parsedStatus = parseStatus(status);
        var items = alertService.listAlerts(userId, parsedStatus).stream()
                .map(dtoMapper::toAlertDto)
                .toList();
        return ResponseEntity.ok(new ApiDtos.AlertListDto(items));
    }

    @GetMapping(name = "getAlert", path = "/alerts/{alertId}", produces = "application/json")
    public ResponseEntity<ApiDtos.AlertDto> getAlert(@PathVariable String alertId, HttpServletRequest request) {
        String userId = currentUser.require(request);
        return ResponseEntity.ok(dtoMapper.toAlertDto(alertService.getAlert(userId, parseUuid(alertId, "alertId"))));
    }

    @DeleteMapping(name = "cancelAlert", path = "/alerts/{alertId}", produces = "application/json")
    public ResponseEntity<ApiDtos.AlertDto> cancelAlert(@PathVariable String alertId, HttpServletRequest request) {
        String userId = currentUser.require(request);
        return ResponseEntity.ok(dtoMapper.toAlertDto(alertService.cancelAlert(userId, parseUuid(alertId, "alertId"))));
    }

    private CreateAlertCommand toCommand(ApiDtos.AlertRequestDto request) {
        if (request == null || request.courtId() == null || request.date() == null || request.slot() == null) {
            throw new ApiValidationException("courtId, date, slot은 필수입니다");
        }
        if (!TennisAlertConstants.COURT_ID_PATTERN.matcher(request.courtId()).matches()) {
            throw new ApiValidationException("courtId: 형식이 올바르지 않습니다");
        }
        TimeSlot slot = dtoMapper.toTimeSlot(request.slot());
        return new CreateAlertCommand(request.courtId(), request.date(), slot);
    }

    private UUID parseUuid(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ApiValidationException(name + ": 필수 값입니다");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiValidationException(name + ": UUID 형식이어야 합니다");
        }
    }

    private AlertStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AlertStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new ApiValidationException("status: 지원하지 않는 상태입니다");
        }
    }
}
