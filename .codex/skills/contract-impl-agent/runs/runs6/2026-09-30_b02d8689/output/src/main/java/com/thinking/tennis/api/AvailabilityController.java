package com.thinking.tennis.api;

import com.thinking.tennis.app.AvailabilityService;
import com.thinking.tennis.port.CourtAvailabilityPort.SlotAvailability;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courts")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @Operation(
            operationId = "getCourtAvailability",
            summary = "코트·날짜의 예약 상태 조회",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예약 상태",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiModels.CourtAvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않습니다",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "404", description = "지원하지 않는 코트입니다",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "502", description = "예약처 응답을 읽지 못했습니다",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "503", description = "예약처에 연결하지 못했습니다",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class))),
            @ApiResponse(responseCode = "504", description = "예약처가 제때 응답하지 않았습니다",
                    headers = @Header(name = "Retry-After", required = true),
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiModels.Problem.class)))
    })
    @GetMapping(value = "/{courtId}/availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiModels.CourtAvailabilityResponse getCourtAvailability(
            @Parameter(in = ParameterIn.PATH, required = true) @PathVariable String courtId,
            @Parameter(required = true, schema = @Schema(type = "string", format = "date"))
            @RequestParam LocalDate date) {
        AvailabilityService.CachedResult result = availabilityService.getAvailability(courtId, date);
        var check = result.check();
        return new ApiModels.CourtAvailabilityResponse(
                check.court().courtId(),
                check.court().courtName(),
                check.date(),
                check.confirmedAt(),
                result.stale(),
                result.staleReason(),
                check.court().reservationUrl(),
                check.slots().stream().map(this::toSlot).toList());
    }

    private ApiModels.AvailabilitySlotResponse toSlot(SlotAvailability slot) {
        return new ApiModels.AvailabilitySlotResponse(
                slot.startTime().toString(), slot.endTime().toString(), slot.available());
    }
}
