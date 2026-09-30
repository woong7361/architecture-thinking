package com.thinking.tennis.api;

import com.thinking.tennis.app.AvailabilityService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping(path = "/courts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping(name = "getCourtAvailability", path = "/{courtId}/availability")
    public ResponseEntity<ApiModels.AvailabilityResponse> getCourtAvailability(
            @PathVariable("courtId") String courtId,
            @RequestParam("date") String date
    ) {
        validateCourtId(courtId);
        LocalDate parsedDate = parseDate(date);
        return ResponseEntity.ok(ApiModels.availability(availabilityService.getCourtAvailability(courtId, parsedDate)));
    }

    private static void validateCourtId(String courtId) {
        if (courtId == null || courtId.length() > 64 || !courtId.matches("^[a-z0-9][a-z0-9-]*$")) {
            throw new RequestValidationException("courtId: 값이 올바르지 않습니다");
        }
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new RequestValidationException("date: 날짜 형식이어야 합니다");
        }
    }
}
