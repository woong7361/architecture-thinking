package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplicationService;
import com.thinking.tennis.app.AvailabilityApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courts")
public class AvailabilityController {
    private final AvailabilityApplicationService availability;
    private final AlertApplicationService alerts;

    public AvailabilityController(AvailabilityApplicationService availability, AlertApplicationService alerts) {
        this.availability = availability;
        this.alerts = alerts;
    }

    @GetMapping("/{courtId}/availability")
    @Operation(operationId = "getCourtAvailability")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability result.",
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[]")
                    })),
            @ApiResponse(responseCode = "400", description = "Validation failed.",
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]")
                    })),
            @ApiResponse(responseCode = "404", description = "Court not supported.",
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[\"COURT_NOT_SUPPORTED\"]")
                    })),
            @ApiResponse(responseCode = "500", description = "Internal error.",
                headers = @Header(name = "Retry-After", ref = "#/components/headers/RetryAfter"),
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]")
                    })),
            @ApiResponse(responseCode = "502", description = "Upstream response unreadable.",
                headers = @Header(name = "Retry-After", ref = "#/components/headers/RetryAfter"),
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[\"UPSTREAM_RESPONSE_UNREADABLE\"]")
                    })),
            @ApiResponse(responseCode = "503", description = "Availability unavailable.",
                headers = @Header(name = "Retry-After", ref = "#/components/headers/RetryAfter"),
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[\"UPSTREAM_UNAVAILABLE\",\"STORAGE_TIMEOUT\"]")
                    })),
            @ApiResponse(responseCode = "504", description = "Upstream timeout.",
                headers = @Header(name = "Retry-After", ref = "#/components/headers/RetryAfter"),
                    extensions = @Extension(properties = {
                            @ExtensionProperty(name = "x-error-codes", value = "[\"UPSTREAM_TIMEOUT\"]")
                    }))
    })
    public ResponseEntity<ApiModels.AvailabilityResponse> get(
            @PathVariable String courtId,
            @RequestParam(name = "date") String date
    ) {
        var snapshot = availability.get(courtId, ApiMapper.date(date));
        alerts.applyAvailability(snapshot);
        return ResponseEntity.ok(ApiMapper.availability(snapshot));
    }
}
