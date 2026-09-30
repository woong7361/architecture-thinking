package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.CourtAvailability;
import com.thinking.tennis.api.dto.Patterns;
import com.thinking.tennis.api.dto.Problem;
import com.thinking.tennis.app.GetCourtAvailabilityUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 외부 예약처에서 가져온 예약 상태를 돌려주는 자리다.
 *
 * <p>경로에 주 버전 접두사를 넣지 않는다. 그것은 계약의 서버 주소가 들고 있는 것이라 오퍼레이션 경로에
 * 다시 넣으면 두 번 붙는다.
 *
 * <p>이 오퍼레이션만 인증을 요구하지 않는다. 전역 보안 요구를 벗기는 선언은 {@code OpenApiConfig} 가
 * 한다. 애노테이션으로는 "요구가 없다"와 "적지 않았다"를 가릴 수 없어 빈 목록이 스펙에 남지 않는다.
 */
@RestController
public class CourtAvailabilityController {

    private final GetCourtAvailabilityUseCase getCourtAvailability;
    private final AlertPayloadMapper mapper;

    public CourtAvailabilityController(GetCourtAvailabilityUseCase getCourtAvailability,
                                       AlertPayloadMapper mapper) {
        this.getCourtAvailability = getCourtAvailability;
        this.mapper = mapper;
    }

    @Operation(
            operationId = "getCourtAvailability",
            tags = "availability",
            summary = "코트·날짜의 예약 상태 조회",
            description = """
                    코트 하나의 하루치 예약 상태를 돌려준다. 서버는 코트·날짜 단위로 확인 결과를 저장해 두고,
                    저장된 결과가 확인 간격보다 오래됐거나 한 번도 없으면 이 요청이 외부 예약처 확인을 일으킨다.
                    확인 간격은 요구사항 NFR-3이 정하고, 확인 실패로 나가는 응답의 `retryAfterSeconds`도 그 간격을 따른다.

                    확인이 일어나는 동안 같은 코트·날짜로 들어온 다른 요청은 예약처를 따로 조회하지 않고 그 결과를 기다린다.
                    기다리던 요청은 모두 같은 내용을 받고, 확인이 실패하면 모두 같은 실패를 받는다.
                    그래서 피크 초당 200건(NFR-1)이 몰려도 예약처로 나가는 조회는 확인 간격마다 한 번이다.

                    확인에 실패해도 저장된 마지막 성공 결과가 있으면 그 결과를 200으로 돌려주고 `stale`을 true로 두며
                    `staleReason`에 무엇이 실패했는지를 담는다. 처리 자체는 성공했고 달라진 것은 데이터의 신선도라서 200이다.
                    저장된 성공 결과가 한 번도 없을 때만 5xx로 실패를 알린다.

                    이 오퍼레이션만 인증을 요구하지 않는다. 신청하기 전에 조건을 고르려면 먼저 볼 수 있어야 하고,
                    돌려주는 값에 사용자를 가리키는 것이 없다.""",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement",
                    value = "[\"FR-7\", \"FR-3\", \"NFR-1\", \"EF-1\", \"EF-2\", \"NFR-3\", \"V-4\"]",
                    parseValue = true)))
    @ApiResponse(responseCode = "200",
            description = "저장된 마지막 확인 결과. `stale`이 true이면 그 결과가 최신 확인에 실패해 오래된 것이다.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CourtAvailability.class)))
    @ApiResponse(responseCode = "400",
            description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true)))
    @ApiResponse(responseCode = "404",
            description = "경로가 가리킨 코트를 서비스가 다루지 않는다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"COURT_NOT_SUPPORTED\"]", parseValue = true)))
    @ApiResponse(responseCode = "500",
            description = "서버 내부에서 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true)))
    @ApiResponse(responseCode = "502",
            description = "외부 예약처의 응답을 읽지 못했고 저장된 마지막 성공 결과도 없다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"UPSTREAM_RESPONSE_UNREADABLE\"]", parseValue = true)))
    @ApiResponse(responseCode = "503",
            description = "예약 상태를 돌려줄 수 없다. 예약처에 닿지 못했고 저장된 결과도 없거나, 저장소가 제때 응답하지 않았다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes",
                    value = "[\"UPSTREAM_UNAVAILABLE\", \"STORAGE_TIMEOUT\"]", parseValue = true)))
    @ApiResponse(responseCode = "504",
            description = "외부 예약처가 제때 응답하지 않았고 저장된 마지막 성공 결과도 없다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"UPSTREAM_TIMEOUT\"]", parseValue = true)))
    @GetMapping(path = "/courts/{courtId}/availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public CourtAvailability getCourtAvailability(

            @Parameter(description = "서비스가 지원하는 코트의 식별자. 검색 기능이 제공한다.",
                    required = true,
                    example = "seoul-yangjae-1",
                    schema = @Schema(type = "string", minLength = 1,
                            maxLength = Patterns.COURT_ID_MAX_LENGTH, pattern = Patterns.COURT_ID))
            @PathVariable String courtId,

            @Parameter(description = """
                    조회할 날짜다. 코트가 있는 지역의 날짜를 쓴다.
                    예약처가 아직 열지 않은 날짜를 물으면 확인은 성공하고 `slots`가 빈 배열로 온다.""",
                    required = true,
                    example = "2026-10-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        RequestValidation.requireCourtIdFormat(courtId);
        return mapper.toCourtAvailability(getCourtAvailability.availability(courtId, date));
    }
}
