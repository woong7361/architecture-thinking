package com.thinking.tennis.api;

import com.thinking.tennis.api.ApiDtos.AvailabilityResponse;
import com.thinking.tennis.app.AvailabilityResult;
import com.thinking.tennis.app.AvailabilityService;
import com.thinking.tennis.app.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping
public class AvailabilityController {
    private final AvailabilityService availabilityService;
    private final AlertService alertService;

    public AvailabilityController(AvailabilityService availabilityService, AlertService alertService) {
        this.availabilityService = availabilityService;
        this.alertService = alertService;
    }

    @GetMapping("/courts/{courtId}/availability")
    @OperationId("getCourtAvailability")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AvailabilityResponse> getCourtAvailability(
            @PathVariable String courtId,
            @RequestParam String date
    ) {
        String validatedCourtId = RequestValidation.courtId(courtId, "courtId");
        LocalDate validatedDate = RequestValidation.date(date, "date");
        AvailabilityResult result = availabilityService.get(validatedCourtId, validatedDate);
        if (!result.stale()) {
            alertService.recordAvailabilityCheck(
                    validatedCourtId,
                    validatedDate,
                    result.snapshot().confirmedAt()
            );
        }
        return ResponseEntity.ok(AvailabilityResponse.from(
                result.snapshot(),
                result.stale(),
                result.staleReason()
        ));
    }
}
