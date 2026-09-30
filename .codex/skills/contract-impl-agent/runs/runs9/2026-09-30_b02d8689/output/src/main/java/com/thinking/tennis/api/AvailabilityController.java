package com.thinking.tennis.api;

import com.thinking.tennis.app.AvailabilityService;
import com.thinking.tennis.app.TennisAlertConstants;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping(name = "getCourtAvailability", path = "/courts/{courtId}/availability", produces = "application/json")
    public ResponseEntity<ApiDtos.CourtAvailabilityDto> getCourtAvailability(@PathVariable String courtId,
                                                                             @RequestParam LocalDate date) {
        validateCourtId(courtId);
        return ResponseEntity.ok(ApiDtos.CourtAvailabilityDto.from(availabilityService.getAvailability(courtId, date)));
    }

    private void validateCourtId(String courtId) {
        if (courtId == null || !TennisAlertConstants.COURT_ID_PATTERN.matcher(courtId).matches()) {
            throw new ApiValidationException("courtId: 형식이 올바르지 않습니다");
        }
    }
}
