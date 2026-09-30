package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.AlertListResponse;
import com.thinking.tennis.api.dto.AlertResponse;
import com.thinking.tennis.api.dto.AlertStatusValue;
import com.thinking.tennis.api.dto.CreateAlertRequest;
import com.thinking.tennis.api.dto.ProblemResponse;
import com.thinking.tennis.app.AlertCommandService;
import com.thinking.tennis.app.AlertCondition;
import com.thinking.tennis.app.AlertQueryService;
import com.thinking.tennis.app.AlertView;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 알림 신청을 다루는 인바운드 어댑터다.
 *
 * <p>사용자 식별을 이 자리에서 한다. 남의 신청을 조회·해제할 수 없어야 해서 유스케이스는 언제나 누구의
 * 요청인지를 받아야 하고, 그 값을 토큰에서 꺼내는 일은 HTTP 관심사다.
 */
@RestController
@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
public class AlertController {

    private final AlertCommandService commands;
    private final AlertQueryService queries;

    public AlertController(AlertCommandService commands, AlertQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    /*
     * 받아들이는 미디어 타입을 매핑 조건으로 걸지 않는다. 조건으로 걸면 Content-Type 이 다른 요청이
     * 이 자리에 닿기 전에 스프링이 계약에 없는 상태 코드로 되돌려, 계약이 정한 실패 모양을 거치지 않는다.
     * 본문의 미디어 타입 선언은 요청 본문 쪽이 하고, 다룰 수 없는 타입은 실패 처리기가 계약이 정한
     * 400 VALIDATION_FAILED 로 옮긴다.
     */
    @PostMapping(path = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "createAlert",
            summary = "빈자리 알림 신청",
            description = """
                    코트 하나, 날짜 하나, 시간대 하나로 알림을 신청한다. 신청은 감시 중으로 시작한다.

                    지원하는 시간대인지는 그 코트·날짜의 저장된 확인 결과로 판정하며, 저장된 결과가 확인 간격보다 오래됐거나 없으면
                    이 요청이 확인을 일으킨다. 그 확인이 실패하고 저장된 마지막 성공 결과도 없으면 조건을 판정할 수 없으므로,
                    예약 상태 조회와 같은 조건으로 실패한 이유에 따라 502·503·504 중 하나를 돌려준다.
                    확인 실패는 빈자리 없음이 아니므로 지원하지 않는 시간대로 거절하지 않는다.

                    예약처가 그 날짜를 아직 열지 않아 판정에 쓴 결과의 `slots`가 비어 있으면 시간대 판정을 미루고 신청을 받는다.
                    예약이 열리기 전에 미리 걸어 두는 것이 이 서비스의 주된 쓰임이라, 아직 열리지 않은 날짜를 운영하지 않는 시간대와 같이 다루지 않는다.
                    이후 확인에서 운영하지 않는 시간대로 드러나면 그 신청은 감시만 계속되고 알림이 나가지 않는다.

                    판정에 쓴 확인 결과에 그 시간대가 비어 있으면 응답의 `delivery`는 곧바로 `PENDING`이다.
                    비어 있지 않거나 운영 시간대를 아직 판정하지 못했으면 null이고, 이후 확인에서 빈자리가 드러날 때 대기에 오른다.

                    이 오퍼레이션은 재시도해도 안전하다. `Idempotency-Key` 헤더가 필수이며, 같은 키로 같은 요청을 다시 보내면
                    서버는 새 신청을 만들지 않고 처음 돌려준 응답을 그대로 다시 돌려준다. 그래서 응답을 받지 못했거나 5xx를 받았으면
                    같은 키로 그대로 다시 보내면 된다. 4xx는 요청을 고쳐야 풀리므로 기록하지 않고, 같은 키로 다시 보내면 조건을 다시 판정한다.

                    같은 키의 앞선 요청이 아직 처리 중이면 서버는 503 `CONCURRENT_UPDATE_CONFLICT`로 물러난다.
                    `Retry-After`만큼 기다렸다 같은 키로 다시 보내면 그 요청의 결과를 재생으로 받는다.

                    재생되는 응답은 처음 처리한 시점의 내용이다. 그 뒤로 신청의 상태가 바뀌었어도 재생 응답은 바뀌지 않으므로,
                    지금 상태가 필요하면 신청 조회로 다시 읽는다. 재생인지 여부는 `Idempotency-Replayed`가 알린다.

                    같은 사용자의 감시 중인 신청과 같은 코트·날짜·시간대로 다시 신청하면, 키가 달라도 새 신청을 만들지 않고 그 신청을 200으로 돌려준다.
                    남이 같은 조건을 감시 중인 것은 이 판정에 들지 않는다. 남의 신청은 조회·해제할 수 없으므로 돌려줄 수도 없다.
                    같은 사용자의 이미 끝난 신청과 같은 조건이면 새 신청을 만든다. 끝난 신청은 되살아나지 않지만 같은 조건을 다시 감시할 수는 있다.
                    """,
            tags = "alerts",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-1\",\"V-1\"]", parseValue = true)))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = """
                            신청이 만들어졌다. 이 요청이 확인을 일으켰으면 `lastCheckedAt`은 그 확인이 성공한 시각이고,
                            확인 간격 안의 저장된 결과를 썼으면 그 결과의 시각이다. 한 번도 확인에 성공하지 못한 코트·날짜에서만 null이다.
                            판정에 쓴 확인 결과에 신청한 시간대가 비어 있었으면 `delivery`는 곧바로 `PENDING`이고, 비어 있지 않았으면 null이다.
                            """,
                    headers = {
                            @Header(name = "Location",
                                    description = "만들어졌거나 이미 있던 신청의 위치",
                                    schema = @Schema(type = "string", format = "uri-reference")),
                            @Header(name = "Idempotency-Replayed",
                                    description = """
                                            true이면 이 응답은 같은 키로 앞서 처리한 요청의 응답을 그대로 다시 돌려준 것이고, 이번 요청은 아무것도 바꾸지 않았다.
                                            false이면 이번 요청이 처리된 결과다. 두 값 중 하나를 언제나 함께 보낸다.
                                            같은 조건을 감시 중인 신청이 이미 있어 그 신청을 돌려주는 200은 이번 요청이 판정한 결과이므로 false다.
                                            true는 같은 키로 앞서 보관한 응답을 그대로 돌려줄 때만이다.
                                            """,
                                    required = true,
                                    schema = @Schema(type = "boolean"))
                    },
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(
                    responseCode = "200",
                    description = "같은 조건을 감시 중인 신청이 이미 있어 그 신청을 돌려준다. 새 신청은 만들어지지 않았다.",
                    headers = {
                            @Header(name = "Location",
                                    description = "만들어졌거나 이미 있던 신청의 위치",
                                    schema = @Schema(type = "string", format = "uri-reference")),
                            @Header(name = "Idempotency-Replayed",
                                    description = """
                                            true이면 이 응답은 같은 키로 앞서 처리한 요청의 응답을 그대로 다시 돌려준 것이고, 이번 요청은 아무것도 바꾸지 않았다.
                                            false이면 이번 요청이 처리된 결과다. 두 값 중 하나를 언제나 함께 보낸다.
                                            같은 조건을 감시 중인 신청이 이미 있어 그 신청을 돌려주는 200은 이번 요청이 판정한 결과이므로 false다.
                                            true는 같은 키로 앞서 보관한 응답을 그대로 돌려줄 때만이다.
                                            """,
                                    required = true,
                                    schema = @Schema(type = "boolean"))
                    },
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청이 스키마를 만족하지 않는다. 본문의 형식 위반과 필수 멱등 키 헤더의 누락이 함께 여기로 온다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "409",
                    description = "멱등 키가 앞선 요청의 내용과 어긋난다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"IDEMPOTENCY_KEY_REUSED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "422",
                    description = "요청 형식은 맞지만 본문이 가리킨 조건을 감시할 수 없다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes",
                            value = "[\"COURT_NOT_SUPPORTED\",\"SLOT_NOT_SUPPORTED\",\"ALERT_WINDOW_CLOSED\"]",
                            parseValue = true))),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부에서 처리하지 못했다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "502",
                    description = "외부 예약처의 응답을 읽지 못해 운영 시간대를 확인할 수 없고 저장된 마지막 성공 결과도 없다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"UPSTREAM_RESPONSE_UNREADABLE\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "503",
                    description = """
                            신청을 처리할 수 없다. 예약처에 닿지 못해 운영 시간대를 확인하지 못했고 저장된 결과도 없거나,
                            저장소가 제때 응답하지 않았거나, 같은 자원을 고치려는 요청끼리 경합에 밀렸다.
                            """,
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes",
                            value = "[\"UPSTREAM_UNAVAILABLE\",\"STORAGE_TIMEOUT\",\"CONCURRENT_UPDATE_CONFLICT\"]",
                            parseValue = true))),
            @ApiResponse(
                    responseCode = "504",
                    description = "외부 예약처가 제때 응답하지 않아 운영 시간대를 확인할 수 없고 저장된 마지막 성공 결과도 없다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"UPSTREAM_TIMEOUT\"]", parseValue = true)))
    })
    public ResponseEntity<AlertResponse> createAlert(
            @Parameter(hidden = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,

            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true,
                    description = """
                            클라이언트가 신청 하나를 만들 때마다 새로 만드는 UUID다. 같은 요청을 재시도할 때는 처음 보낸 값을 그대로 다시 보낸다.
                            서버는 이 키와 2xx로 끝난 응답을 사용자와 오퍼레이션 단위로 24시간 보관한다.
                            보관 기간이 지난 뒤 같은 키로 다시 보내면 재생이 아니라 새 요청으로 처리한다.
                            """,
                    schema = @Schema(type = "string", format = "uuid"),
                    example = "3f0c5b8a-1d24-4e6f-8b19-7c2a9e5d4f61")
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateAlertRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "courtId": "seoul-yangjae-1",
                                      "date": "2026-10-04",
                                      "slot": { "startTime": "06:00", "endTime": "08:00" }
                                    }
                                    """)))
            @RequestBody CreateAlertRequest request) {

        String userId = BearerTokens.userId(authorization);
        String validKey = RequestValues.idempotencyKey(idempotencyKey);
        AlertCondition condition = condition(request);

        AlertCommandService.CreateResult result = commands.create(userId, validKey, condition);
        return ResponseEntity.status(result.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Location", ApiPaths.alertLocation(result.view().alert().alertId()))
                .header("Idempotency-Replayed", String.valueOf(result.replayed()))
                .body(AlertResponse.of(result.view()));
    }

    @GetMapping(path = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "listAlerts",
            summary = "내 알림 신청 목록 조회",
            description = """
                    요청한 사용자 본인의 신청만 돌려준다. 남의 신청은 어떤 방법으로도 이 목록에 나오지 않는다.

                    `status`를 지정하지 않으면 끝난 신청까지 모두 돌려준다. 거르는 기준은 저장된 `status`이므로,
                    만료 시각이 지났지만 아직 감시 중으로 저장된 신청은 `status=WATCHING`으로 걸러도 함께 온다.

                    감시 중인 신청은 1인당 두세 건이라고 NFR-1이 보았기에 페이지를 나누지 않는다.
                    끝난 신청은 계속 쌓이므로 보관 기간이 정해지기 전까지 응답 크기의 상한은 없다.
                    """,
            tags = "alerts",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-2\",\"NFR-1\"]", parseValue = true)))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "본인의 신청 목록",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertListResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부에서 처리하지 못했다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "503",
                    description = "저장소가 제때 응답하지 않아 처리하지 못했다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"STORAGE_TIMEOUT\"]", parseValue = true)))
    })
    public ResponseEntity<AlertListResponse> listAlerts(
            @Parameter(hidden = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,

            @Parameter(name = "status", in = ParameterIn.QUERY, required = false,
                    description = "지정하면 그 상태로 저장된 신청만 돌려준다.",
                    schema = @Schema(implementation = AlertStatusValue.class))
            @RequestParam(name = "status", required = false) String status) {

        String userId = BearerTokens.userId(authorization);
        AlertStatus filter = RequestValues.alertStatus("status", status);

        List<AlertView> views = queries.list(userId, filter);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(AlertListResponse.of(views));
    }

    @GetMapping(path = "/alerts/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "getAlert",
            summary = "알림 신청 단건 조회",
            description = """
                    본인의 신청 하나를 돌려준다. 남의 신청을 요청하면 존재 여부를 드러내지 않기 위해 없는 것과 같은 404를 돌려준다.
                    """,
            tags = "alerts",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-2\"]", parseValue = true)))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "신청 하나",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "404",
                    description = "없는 신청과 남의 신청을 구분해 알리지 않는다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"ALERT_NOT_FOUND\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부에서 처리하지 못했다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "503",
                    description = "저장소가 제때 응답하지 않아 처리하지 못했다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"STORAGE_TIMEOUT\"]", parseValue = true)))
    })
    public ResponseEntity<AlertResponse> getAlert(
            @Parameter(hidden = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,

            @Parameter(name = "alertId", in = ParameterIn.PATH, required = true,
                    description = "알림 신청의 식별자. UUID가 아니면 400으로 거절한다.",
                    schema = @Schema(type = "string", format = "uuid",
                            description = "알림 신청의 식별자. 서버가 신청을 만들 때 발급한다."),
                    example = "6f5b2f7e-9c1a-4f0e-9a4c-2b7d5e1a8c30")
            @PathVariable("alertId") String alertId) {

        String userId = BearerTokens.userId(authorization);
        String validAlertId = RequestValues.alertId("alertId", alertId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(AlertResponse.of(queries.get(userId, validAlertId)));
    }

    @DeleteMapping(path = "/alerts/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "cancelAlert",
            summary = "알림 신청 해제",
            description = """
                    감시와 발송을 멈춘다. 해제된 신청은 되살아나지 않고, 아직 보내지 않은 발송 대기가 있으면 그 대기는 버려진다.
                    이미 나간 알림은 취소되지 않는다.

                    이 오퍼레이션은 여러 번 불러도 결과가 같다. 이미 끝난 신청에 다시 요청하면 상태를 바꾸지 않고 현재 상태를 돌려준다.
                    해제한 신청도 기록은 남으므로 조회에서는 계속 보인다.
                    """,
            tags = "alerts",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-2\",\"V-8\"]", parseValue = true)))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "해제 요청을 처리한 뒤의 신청 상태",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "401",
                    description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "404",
                    description = "없는 신청과 남의 신청을 구분해 알리지 않는다.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"ALERT_NOT_FOUND\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부에서 처리하지 못했다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true))),
            @ApiResponse(
                    responseCode = "503",
                    description = "저장소가 제때 응답하지 않았거나 같은 자원을 고치려는 요청끼리 경합에 밀렸다.",
                    headers = @Header(name = "Retry-After",
                            description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                            schema = @Schema(type = "integer", minimum = "1")),
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)),
                    extensions = @Extension(properties = @ExtensionProperty(
                            name = "x-error-codes",
                            value = "[\"STORAGE_TIMEOUT\",\"CONCURRENT_UPDATE_CONFLICT\"]",
                            parseValue = true)))
    })
    public ResponseEntity<AlertResponse> cancelAlert(
            @Parameter(hidden = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,

            @Parameter(name = "alertId", in = ParameterIn.PATH, required = true,
                    description = "알림 신청의 식별자. UUID가 아니면 400으로 거절한다.",
                    schema = @Schema(type = "string", format = "uuid",
                            description = "알림 신청의 식별자. 서버가 신청을 만들 때 발급한다."),
                    example = "6f5b2f7e-9c1a-4f0e-9a4c-2b7d5e1a8c30")
            @PathVariable("alertId") String alertId) {

        String userId = BearerTokens.userId(authorization);
        String validAlertId = RequestValues.alertId("alertId", alertId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(AlertResponse.of(commands.cancel(userId, validAlertId)));
    }

    /*
     * 본문을 안쪽이 쓰는 값으로 옮긴다. 형식 검사를 여기서 마치므로 유스케이스는 형식이 맞는 조건만 본다.
     */
    private static AlertCondition condition(CreateAlertRequest request) {
        if (request == null) {
            throw new RequestValidationException("body: 필수 값입니다");
        }
        String courtId = RequestValues.courtId("courtId", request.courtId());
        LocalDate date = RequestValues.date("date", request.date());
        if (request.slot() == null) {
            throw new RequestValidationException("slot: 필수 값입니다");
        }
        TimeSlot slot = new TimeSlot(
                RequestValues.hourMinute("slot.startTime", request.slot().startTime()),
                RequestValues.hourMinute("slot.endTime", request.slot().endTime()));
        return new AlertCondition(courtId, date, slot);
    }
}
