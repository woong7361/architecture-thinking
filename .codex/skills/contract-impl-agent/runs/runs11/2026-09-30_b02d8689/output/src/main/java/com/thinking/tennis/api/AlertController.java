package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.Alert;
import com.thinking.tennis.api.dto.AlertList;
import com.thinking.tennis.api.dto.AlertRequest;
import com.thinking.tennis.api.dto.AlertStatus;
import com.thinking.tennis.api.dto.Problem;
import com.thinking.tennis.app.AlertView;
import com.thinking.tennis.app.CancelAlertUseCase;
import com.thinking.tennis.app.CreateAlertUseCase;
import com.thinking.tennis.app.QueryAlertsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 빈자리 알림 신청의 생성, 조회, 해제를 받는 자리다.
 *
 * <p>사용자 식별을 위한 {@code Authorization} 헤더는 스펙에 매개변수로 드러내지 않는다. 계약은 그것을
 * 보안 스키마로 적었고, 같은 것을 매개변수로도 적으면 한 요구가 두 자리에 서로 다른 모양으로 남는다.
 *
 * <p>이 오퍼레이션들의 보안 요구도 여기에 적지 않는다. 계약은 그것을 문서 전체의 요구로 한 번 적고
 * 인증을 요구하지 않는 오퍼레이션에서만 벗겼다. 두 선언을 {@code OpenApiConfig} 가 그대로 낸다.
 * 여기에 오퍼레이션마다 다시 적으면 같은 요구가 계약에 없는 자리에 한 번 더 생긴다.
 */
@RestController
public class AlertController {

    /** 멱등 키의 보관 단위가 되는 오퍼레이션 식별자다. 계약이 선언한 것과 같은 값을 쓴다. */
    private static final String CREATE_ALERT = "createAlert";

    private static final String IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";

    private final CreateAlertUseCase createAlert;
    private final QueryAlertsUseCase queryAlerts;
    private final CancelAlertUseCase cancelAlert;
    private final BearerUserResolver users;
    private final IdempotencyRegistry idempotency;
    private final AlertPayloadMapper mapper;

    public AlertController(CreateAlertUseCase createAlert,
                           QueryAlertsUseCase queryAlerts,
                           CancelAlertUseCase cancelAlert,
                           BearerUserResolver users,
                           IdempotencyRegistry idempotency,
                           AlertPayloadMapper mapper) {
        this.createAlert = createAlert;
        this.queryAlerts = queryAlerts;
        this.cancelAlert = cancelAlert;
        this.users = users;
        this.idempotency = idempotency;
        this.mapper = mapper;
    }

    @Operation(
            operationId = "createAlert",
            tags = "alerts",
            summary = "빈자리 알림 신청",
            description = """
                    코트 하나, 날짜 하나, 시간대 하나로 알림을 신청한다. 신청은 감시 중으로 시작한다.

                    이 오퍼레이션은 재시도해도 안전하다. `Idempotency-Key` 헤더가 필수이며, 같은 키로 같은 요청을 다시 보내면
                    서버는 새 신청을 만들지 않고 처음 돌려준 응답을 그대로 다시 돌려준다. 그래서 응답을 받지 못했거나 5xx를 받았으면
                    같은 키로 그대로 다시 보내면 된다. 4xx는 요청을 고쳐야 풀리므로 기록하지 않고, 같은 키로 다시 보내면 조건을 다시 판정한다.

                    같은 키의 앞선 요청이 아직 처리 중이면 서버는 503 `CONCURRENT_UPDATE_CONFLICT`로 물러난다.
                    `Retry-After`만큼 기다렸다 같은 키로 다시 보내면 그 요청의 결과를 재생으로 받는다.

                    재생되는 응답은 처음 처리한 시점의 내용이다. 그 뒤로 신청의 상태가 바뀌었어도 재생 응답은 바뀌지 않으므로,
                    지금 상태가 필요하면 신청 조회로 다시 읽는다. 재생인지 여부는 `Idempotency-Replayed`가 알린다.

                    감시 중인 신청과 같은 코트·날짜·시간대로 다시 신청하면, 키가 달라도 새 신청을 만들지 않고 그 신청을 200으로 돌려준다.
                    이미 끝난 신청과 같은 조건이면 새 신청을 만든다. 끝난 신청은 되살아나지 않지만 같은 조건을 다시 감시할 수는 있다.""",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-1\", \"V-1\"]", parseValue = true)))
    @ApiResponse(responseCode = "201",
            description = "신청이 만들어졌다.",
            headers = {
                    @Header(name = HttpHeaders.LOCATION,
                            description = "만들어졌거나 이미 있던 신청의 위치",
                            schema = @Schema(type = "string", format = "uri-reference")),
                    @Header(name = IDEMPOTENCY_REPLAYED, required = true,
                            description = """
                                    true이면 이 응답은 같은 키로 앞서 처리한 요청의 응답을 그대로 다시 돌려준 것이고, 이번 요청은 아무것도 바꾸지 않았다.
                                    false이면 이번 요청이 처리된 결과다. 두 값 중 하나를 언제나 함께 보낸다.""",
                            schema = @Schema(type = "boolean"))
            },
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Alert.class)))
    @ApiResponse(responseCode = "200",
            description = "같은 조건을 감시 중인 신청이 이미 있어 그 신청을 돌려준다. 새 신청은 만들어지지 않았다.",
            headers = {
                    @Header(name = HttpHeaders.LOCATION,
                            description = "만들어졌거나 이미 있던 신청의 위치",
                            schema = @Schema(type = "string", format = "uri-reference")),
                    @Header(name = IDEMPOTENCY_REPLAYED, required = true,
                            description = """
                                    true이면 이 응답은 같은 키로 앞서 처리한 요청의 응답을 그대로 다시 돌려준 것이고, 이번 요청은 아무것도 바꾸지 않았다.
                                    false이면 이번 요청이 처리된 결과다. 두 값 중 하나를 언제나 함께 보낸다.""",
                            schema = @Schema(type = "boolean"))
            },
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Alert.class)))
    @ApiResponse(responseCode = "400",
            description = "요청이 스키마를 만족하지 않는다. 본문의 형식 위반과 필수 멱등 키 헤더의 누락이 함께 여기로 온다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true)))
    @ApiResponse(responseCode = "401",
            description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true)))
    @ApiResponse(responseCode = "409",
            description = "멱등 키가 앞선 요청의 내용과 어긋난다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"IDEMPOTENCY_KEY_REUSED\"]", parseValue = true)))
    @ApiResponse(responseCode = "422",
            description = "요청 형식은 맞지만 본문이 가리킨 조건을 감시할 수 없다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes",
                    value = "[\"COURT_NOT_SUPPORTED\", \"SLOT_NOT_SUPPORTED\", \"ALERT_WINDOW_CLOSED\"]",
                    parseValue = true)))
    @ApiResponse(responseCode = "500",
            description = "서버 내부에서 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true)))
    @ApiResponse(responseCode = "503",
            description = "저장소가 제때 응답하지 않았거나 같은 자원을 고치려는 요청끼리 경합에 밀렸다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes",
                    value = "[\"STORAGE_TIMEOUT\", \"CONCURRENT_UPDATE_CONFLICT\"]", parseValue = true)))
    @PostMapping(path = "/alerts",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Alert> createAlert(

            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,

            @Parameter(description = """
                    클라이언트가 신청 하나를 만들 때마다 새로 만드는 UUID다. 같은 요청을 재시도할 때는 처음 보낸 값을 그대로 다시 보낸다.
                    서버는 이 키와 2xx로 끝난 응답을 사용자와 오퍼레이션 단위로 24시간 보관한다.
                    보관 기간이 지난 뒤 같은 키로 다시 보내면 재생이 아니라 새 요청으로 처리한다.""",
                    required = true,
                    example = "3f0c5b8a-1d24-4e6f-8b19-7c2a9e5d4f61")
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,

            @RequestBody AlertRequest body) {

        String userId = users.requireUser(authorization);
        CreateAlertUseCase.Command command = RequestValidation.toCommand(userId, body);
        String key = idempotencyKey.toString();
        String fingerprint = fingerprint(command);

        Optional<IdempotencyRegistry.Replay> replayed =
                idempotency.beginOrReplay(userId, CREATE_ALERT, key, fingerprint);
        if (replayed.isPresent()) {
            IdempotencyRegistry.Replay replay = replayed.get();
            return respond(replay.status(), (Alert) replay.body(), replay.location(), true);
        }

        boolean stored = false;
        try {
            CreateAlertUseCase.Result result = createAlert.create(command);
            Alert payload = mapper.toAlert(result.alert());
            String location = ApiUris.alertLocation(result.alert().alert().alertId());
            int status = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();

            idempotency.complete(userId, CREATE_ALERT, key, fingerprint,
                    new IdempotencyRegistry.Replay(status, payload, location));
            stored = true;
            return respond(status, payload, location, false);
        } finally {
            /* 2xx로 끝나지 않았으면 이 키에 아무것도 남기지 않는다. 같은 키로 다시 보내면 다시 판정한다. */
            if (!stored) {
                idempotency.abandon(userId, CREATE_ALERT, key);
            }
        }
    }

    @Operation(
            operationId = "listAlerts",
            tags = "alerts",
            summary = "내 알림 신청 목록 조회",
            description = """
                    요청한 사용자 본인의 신청만 돌려준다. 남의 신청은 어떤 방법으로도 이 목록에 나오지 않는다.

                    `status`를 지정하지 않으면 끝난 신청까지 모두 돌려준다. 거르는 기준은 저장된 `status`이므로,
                    만료 시각이 지났지만 아직 감시 중으로 저장된 신청은 `status=WATCHING`으로 걸러도 함께 온다.

                    감시 중인 신청은 1인당 두세 건이라고 NFR-1이 보았기에 페이지를 나누지 않는다.
                    끝난 신청은 계속 쌓이므로 보관 기간이 정해지기 전까지 응답 크기의 상한은 없다.""",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-2\", \"NFR-1\"]", parseValue = true)))
    @ApiResponse(responseCode = "200",
            description = "본인의 신청 목록",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AlertList.class)))
    @ApiResponse(responseCode = "400",
            description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true)))
    @ApiResponse(responseCode = "401",
            description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true)))
    @ApiResponse(responseCode = "500",
            description = "서버 내부에서 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true)))
    @ApiResponse(responseCode = "503",
            description = "저장소가 제때 응답하지 않아 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"STORAGE_TIMEOUT\"]", parseValue = true)))
    @GetMapping(path = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertList listAlerts(

            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,

            @Parameter(description = "지정하면 그 상태로 저장된 신청만 돌려준다.")
            @RequestParam(required = false) AlertStatus status) {

        String userId = users.requireUser(authorization);
        List<AlertView> alerts = queryAlerts.list(userId, status == null ? null : status.toDomain());
        return mapper.toAlertList(alerts);
    }

    @Operation(
            operationId = "getAlert",
            tags = "alerts",
            summary = "알림 신청 단건 조회",
            description = """
                    본인의 신청 하나를 돌려준다. 남의 신청을 요청하면 존재 여부를 드러내지 않기 위해 없는 것과 같은 404를 돌려준다.""",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-2\"]", parseValue = true)))
    @ApiResponse(responseCode = "200",
            description = "신청 하나",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Alert.class)))
    @ApiResponse(responseCode = "400",
            description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true)))
    @ApiResponse(responseCode = "401",
            description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true)))
    @ApiResponse(responseCode = "404",
            description = "없는 신청과 남의 신청을 구분해 알리지 않는다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"ALERT_NOT_FOUND\"]", parseValue = true)))
    @ApiResponse(responseCode = "500",
            description = "서버 내부에서 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true)))
    @ApiResponse(responseCode = "503",
            description = "저장소가 제때 응답하지 않아 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"STORAGE_TIMEOUT\"]", parseValue = true)))
    @GetMapping(path = "/alerts/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Alert getAlert(

            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,

            @Parameter(description = "알림 신청의 식별자. UUID가 아니면 400으로 거절한다.",
                    required = true,
                    example = "6f5b2f7e-9c1a-4f0e-9a4c-2b7d5e1a8c30")
            @PathVariable UUID alertId) {

        String userId = users.requireUser(authorization);
        return mapper.toAlert(queryAlerts.get(userId, alertId));
    }

    @Operation(
            operationId = "cancelAlert",
            tags = "alerts",
            summary = "알림 신청 해제",
            description = """
                    감시와 발송을 멈춘다. 해제된 신청은 되살아나지 않고, 아직 보내지 않은 발송 대기가 있으면 그 대기는 버려진다.
                    이미 나간 알림은 취소되지 않는다.

                    이 오퍼레이션은 여러 번 불러도 결과가 같다. 이미 끝난 신청에 다시 요청하면 상태를 바꾸지 않고 현재 상태를 돌려준다.
                    해제한 신청도 기록은 남으므로 조회에서는 계속 보인다.""",
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-requirement", value = "[\"FR-2\", \"V-8\"]", parseValue = true)))
    @ApiResponse(responseCode = "200",
            description = "해제 요청을 처리한 뒤의 신청 상태",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Alert.class)))
    @ApiResponse(responseCode = "400",
            description = "요청 자체가 스키마를 만족하지 않아 처리하지 않았다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"VALIDATION_FAILED\"]", parseValue = true)))
    @ApiResponse(responseCode = "401",
            description = "사용자를 식별하지 못해 본인 것인지 판단할 수 없다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"UNAUTHENTICATED\"]", parseValue = true)))
    @ApiResponse(responseCode = "404",
            description = "없는 신청과 남의 신청을 구분해 알리지 않는다.",
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"ALERT_NOT_FOUND\"]", parseValue = true)))
    @ApiResponse(responseCode = "500",
            description = "서버 내부에서 처리하지 못했다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes", value = "[\"INTERNAL_ERROR\"]", parseValue = true)))
    @ApiResponse(responseCode = "503",
            description = "저장소가 제때 응답하지 않았거나 같은 자원을 고치려는 요청끼리 경합에 밀렸다.",
            headers = @Header(name = "Retry-After",
                    description = "다시 시도하기까지 기다릴 초. 본문의 `retryAfterSeconds`와 같은 값이다.",
                    schema = @Schema(type = "integer", minimum = "1")),
            content = @Content(mediaType = ApiMediaTypes.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)),
            extensions = @Extension(properties = @ExtensionProperty(
                    name = "x-error-codes",
                    value = "[\"STORAGE_TIMEOUT\", \"CONCURRENT_UPDATE_CONFLICT\"]", parseValue = true)))
    @DeleteMapping(path = "/alerts/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Alert cancelAlert(

            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,

            @Parameter(description = "알림 신청의 식별자. UUID가 아니면 400으로 거절한다.",
                    required = true,
                    example = "6f5b2f7e-9c1a-4f0e-9a4c-2b7d5e1a8c30")
            @PathVariable UUID alertId) {

        String userId = users.requireUser(authorization);
        return mapper.toAlert(cancelAlert.cancel(userId, alertId));
    }

    private static ResponseEntity<Alert> respond(int status, Alert payload, String location, boolean replayed) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.LOCATION, location)
                .header(IDEMPOTENCY_REPLAYED, Boolean.toString(replayed))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
    }

    /**
     * 같은 키로 들어온 요청이 같은 내용인지 가르는 값이다. 키는 같지만 내용이 다른 요청을 재생으로
     * 돌려주면 클라이언트가 보낸 적 없는 조건의 신청을 자기 것으로 받는다.
     */
    private static String fingerprint(CreateAlertUseCase.Command command) {
        return command.courtId() + "|" + command.date()
                + "|" + command.slot().startTime() + "|" + command.slot().endTime();
    }
}
