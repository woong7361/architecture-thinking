package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplicationService;
import com.thinking.tennis.app.UseCaseException;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class TennisAlertController {
    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

    private final AlertApplicationService service;

    public TennisAlertController(AlertApplicationService service) {
        this.service = service;
    }

    @GetMapping("/courts/{courtId}/availability")
    @Operation(operationId = "getCourtAvailability",
            summary = "Get court availability",
            tags = {"availability"},
            security = {})
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @ApiResponse(responseCode = "200",
                    content = @Content(schema = @Schema(implementation = ApiResponses.AvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"COURT_NOT_SUPPORTED\"]", parseValue = true))),
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(responseCode = "502", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"UPSTREAM_RESPONSE_UNREADABLE\"]", parseValue = true))),
            @ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"UPSTREAM_UNAVAILABLE\",\"STORAGE_TIMEOUT\"]", parseValue = true))),
            @ApiResponse(responseCode = "504", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"UPSTREAM_TIMEOUT\"]", parseValue = true)))
    })
    public ResponseEntity<ApiResponses.AvailabilityResponse> getCourtAvailability(
            @Parameter(name = "courtId", in = ParameterIn.PATH, required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 64,
                            pattern = "^[a-z0-9][a-z0-9-]*$"))
            @PathVariable("courtId") String courtId,
            @Parameter(name = "date", in = ParameterIn.QUERY, required = true,
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam("date") String date) {
        AvailabilitySnapshot snapshot = service.getCourtAvailability(parseCourtId(courtId), parseDate(date));
        return ResponseEntity.ok(ApiResponses.availability(snapshot));
    }

    @PostMapping("/alerts")
    @Operation(operationId = "createAlert",
            summary = "Create an alert",
            tags = {"alerts"},
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @ApiResponse(responseCode = "201", headers = {
                    @Header(name = "Location", schema = @Schema(type = "string", format = "uri-reference")),
                    @Header(name = "Idempotency-Replayed", required = true, schema = @Schema(type = "boolean"))
            }, content = @Content(schema = @Schema(implementation = ApiResponses.AlertResponse.class))),
            @ApiResponse(responseCode = "200", headers = {
                    @Header(name = "Location", schema = @Schema(type = "string", format = "uri-reference")),
                    @Header(name = "Idempotency-Replayed", required = true, schema = @Schema(type = "boolean"))
            }, content = @Content(schema = @Schema(implementation = ApiResponses.AlertResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"IDEMPOTENCY_KEY_REUSED\"]", parseValue = true))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"COURT_NOT_SUPPORTED\",\"SLOT_NOT_SUPPORTED\",\"ALERT_WINDOW_CLOSED\"]", parseValue = true))),
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"STORAGE_TIMEOUT\",\"CONCURRENT_UPDATE_CONFLICT\"]", parseValue = true)))
    })
    public ResponseEntity<ApiResponses.AlertResponse> createAlert(
            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", format = "uuid"))
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = AlertRequestPayload.class)))
            AlertRequestPayload payload) {
        String ownerId = authenticate(authorization);
        UUID key = parseUuid(idempotencyKey, "Idempotency-Key");
        if (payload == null || payload.slot() == null) {
            throw validation("request body and slot are required");
        }
        AlertApplicationService.CreateResult result = service.createAlert(ownerId, parseCourtId(payload.courtId()),
                parseDate(payload.date()), parseTime(payload.slot().startTime()), parseTime(payload.slot().endTime()),
                key.toString());
        return ResponseEntity.status(result.statusCode())
                .header(HttpHeaders.LOCATION, "/v1/alerts/" + result.alert().alertId())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(ApiResponses.alert(result.alert()));
    }

    @GetMapping("/alerts")
    @Operation(operationId = "listAlerts",
            summary = "List my alerts",
            tags = {"alerts"},
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ApiResponses.AlertListResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(name = "x-error-codes", properties = @ExtensionProperty(name = "values", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(name = "x-error-codes", properties = @ExtensionProperty(name = "values", value = "[\"STORAGE_TIMEOUT\"]", parseValue = true)))
    })
    public ResponseEntity<ApiResponses.AlertListResponse> listAlerts(
            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Parameter(name = "status", in = ParameterIn.QUERY,
                    schema = @Schema(type = "string", allowableValues = {"WATCHING", "NOTIFIED", "CANCELED", "EXPIRED"}))
            @RequestParam(value = "status", required = false) String status) {
        AlertStatus parsedStatus = status == null ? null : parseStatus(status);
        return ResponseEntity.ok(ApiResponses.alertList(service.listAlerts(authenticate(authorization), parsedStatus)));
    }

    @GetMapping("/alerts/{alertId}")
    @Operation(operationId = "getAlert",
            summary = "Get an alert",
            tags = {"alerts"},
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ApiResponses.AlertResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(name = "x-error-codes", properties = @ExtensionProperty(name = "values", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(name = "x-error-codes", properties = @ExtensionProperty(name = "values", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"ALERT_NOT_FOUND\"]", parseValue = true))),
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"STORAGE_TIMEOUT\"]", parseValue = true)))
    })
    public ResponseEntity<ApiResponses.AlertResponse> getAlert(
            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Parameter(name = "alertId", in = ParameterIn.PATH, required = true,
                    schema = @Schema(type = "string", format = "uuid"))
            @PathVariable("alertId") String alertId) {
        return ResponseEntity.ok(ApiResponses.alert(service.getAlert(authenticate(authorization),
                parseUuid(alertId, "alertId"))));
    }

    @DeleteMapping("/alerts/{alertId}")
    @Operation(operationId = "cancelAlert",
            summary = "Cancel an alert",
            tags = {"alerts"},
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ApiResponses.AlertResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"ALERT_NOT_FOUND\"]", parseValue = true))),
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(responseCode = "503", headers = @Header(name = "Retry-After", schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(name = "error-codes", value = "[\"STORAGE_TIMEOUT\",\"CONCURRENT_UPDATE_CONFLICT\"]", parseValue = true)))
    })
    public ResponseEntity<ApiResponses.AlertResponse> cancelAlert(
            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Parameter(name = "alertId", in = ParameterIn.PATH, required = true,
                    schema = @Schema(type = "string", format = "uuid"))
            @PathVariable("alertId") String alertId) {
        return ResponseEntity.ok(ApiResponses.alert(service.cancelAlert(authenticate(authorization),
                parseUuid(alertId, "alertId"))));
    }

    private static String authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UseCaseException(401, "UNAUTHENTICATED", "A bearer token is required.", false, null);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new UseCaseException(401, "UNAUTHENTICATED", "A bearer token is required.", false, null);
        }
        return token;
    }

    private static String parseCourtId(String value) {
        if (value == null || value.length() < 1 || value.length() > 64 || !COURT_ID.matcher(value).matches()) {
            throw validation("courtId must match the contract pattern");
        }
        return value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || !DATE.matcher(value).matches()) {
            throw validation("date must use yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException exception) {
            throw validation("date must be a valid calendar date");
        }
    }

    private static LocalTime parseTime(String value) {
        if (value == null || !TIME.matcher(value).matches()) {
            throw validation("time must use HH:mm");
        }
        return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw validation(field + " must be a UUID");
        }
    }

    private static AlertStatus parseStatus(String value) {
        try {
            return AlertStatus.valueOf(value);
        } catch (RuntimeException exception) {
            throw validation("status must be one of WATCHING, NOTIFIED, CANCELED, EXPIRED");
        }
    }

    private static UseCaseException validation(String detail) {
        return new UseCaseException(400, "VALIDATION_FAILED", detail, false, null);
    }
}
