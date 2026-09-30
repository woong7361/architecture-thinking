# 계약 적합성 run 리포트 — 2026-09-30_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T14:42:03+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

**기계 판정이 깨끗한 iteration은 없다.** 다만 001은 판정까지는 갔고 위반이 남아 걸렸다. iteration 001은 여기서 걸렸다: G1이 REJECT다 — 위반이 있다.

판정은 **REJECT**이고 iteration 002까지 왔다. 위반 55건 가운데 54건을 코드로 닫고 0건을 계약으로 닫았으며 1건이 아직 열려 있다. 서 있는 결정은 6건, 계약 변경 기록은 0건이다.

위반은 지점 단위로 센 것이고 그 뒤의 원인은 22개다. 고칠 일의 개수는 원인 쪽이므로 5절을 원인으로 묶어 함께 낸다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **low** 수준이다. 올릴 근거는 없다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:g0.compile_failed@None: 컴파일 오류 2건
src/main/java/com/thinking/tennis/domain/AlertDelivery.java:22:12 recursive constructor invocation
src/main/java/com/thinking/tennis/app/AlertApplicationService.java:201:82 incompatible types: long cannot be converted to java.lang.Integer

판정되지 않은 검사가 있다: g1. 건너뛴 단계의 이유는 g0_reject다. 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.

합격선은 두 문장이 함께 참인 상태다. **판정했다**와 **그리고 위반이 없다**를 같은 칸에 넣지 않는다. 뒤의 문장만 적으면 게이트를 부수는 것이 게이트를 통과하는 가장 쉬운 길이 된다.

| iteration | 판정 | 판정했다 | 위반 없다 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | 예 | 아니다 | PASS | REJECT | PASS | 94 | 7 | 2.25 |
| 002 | REJECT | 아니다 | 아니다 | REJECT | SKIPPED | PASS | 1 | 0 | — |

합격선을 넘지 못한 자리마다 무엇에 걸렸는지 적는다.

| iteration | 무엇에 걸렸는가 |
| --- | --- |
| 001 | G1이 REJECT다 — 위반이 있다 |
| 002 | G1이 SKIPPED이다 — 판정되지 않았다 |
| 002 | 추출 스펙이 나오지 않았다 |
| 002 | Eval이 돌지 않아 축을 쟀는지 말할 수 없다 |
| 002 | G0이 REJECT다 — 위반이 있다 |
| 002 | G1이 판정되지 않아 위반 0건이라고 말할 수 없다 |

## 2. 계약을 이렇게 고쳤다

계약 판본은 이 run에서 움직이지 않았다. 고칠 자리를 찾지 못했거나 고칠 필요가 없었다는 뜻이다.

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 0곳이 달라지고 깨는 변경 0건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 0곳이 달라지고 깨는 변경 0건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 대조하지 못한 자리 | 사라진 판정 지점 |
| --- | --- | --- | --- | --- |
| 앞 판본 | 0 | 0 | 0 | — |
| 이 run의 기준선 | 0 | 0 | 0 | — |
| 사람이 확정한 원본 | 0 | 0 | 0 | — |

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 6건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_bearer_identity | How should the HTTP layer identify a user when the contract specifies Bearer authentication but provides no verifier port or token service? | Treat a syntactically valid non-empty bearer token as the owner identifier; reject only missing or malformed bearer credentials. | least_harm | medium | Replace the controller identity extraction with the eventual authentication adapter and update the service calls to use its principal identifier. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_lazy_expiry | How should EXPIRED transitions occur in an in-memory implementation without a scheduler port? | Lazily transition due WATCHING alerts to EXPIRED on create, list, get, and cancel operations. | least_harm | medium | Move the same transition into the future expiration worker while retaining the read-time guard for race safety. | createAlert:200, createAlert:201, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_create_slot_cache | How can createAlert validate an exact operating slot without causing an upstream availability check? | Use the stored court-date availability snapshot; return SLOT_NOT_SUPPORTED when no snapshot or matching slot exists. | spec_implication | medium | Introduce a separate schedule port or change the creation workflow once the contract exposes one. | createAlert:200, createAlert:201, createAlert:422 | standing |
| d_failure_window | How should repeated availability requests behave immediately after an upstream failure with no successful snapshot? | Cache the failure reason for 20 seconds and reuse it until the interval expires. | contract_analogy | medium | Replace the failure marker with the eventual persistent refresh-state record if storage semantics are added. | getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504 | standing |
| d_trace_id_format | What concrete string format should be used for the contract's unconstrained 5xx traceId? | Generate a UUID string for every 5xx problem response. | convention | high | Replace the generator with the application's logging correlation-id provider. | getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, listAlerts:500, listAlerts:503, getAlert:500, getAlert:503, cancelAlert:500, cancelAlert:503 | standing |
| d_idempotency_snapshot | How should a replay preserve the original response after the alert changes state? | Store an immutable-by-copy alert snapshot, the original 200/201 outcome, request fingerprint, and timestamp for 24 hours. | spec_implication | high | Replace the in-memory response record with the durable idempotency representation while preserving the same replay contract. | createAlert:200, createAlert:201, createAlert:409 | standing |

`d_bearer_identity` — The implementation contract exposes no authentication port and forbids creating new external clients. Token-to-owner mapping is therefore kept local and deterministic.

`d_lazy_expiry` — The contract requires server-side expiration, while this iteration provides no scheduler port. Request-time reconciliation preserves terminal behavior without adding an undeclared endpoint or dependency.

`d_create_slot_cache` — The contract explicitly makes lookup the only operation that triggers confirmation, while also requiring exact slot validation. The provided port has no separate schedule operation, so the last stored result is the available source of truth.

`d_failure_window` — The contract defines the refresh interval and requires concurrent callers to observe the same result; retaining the failure marker extends that rule to requests arriving just after the in-flight attempt completes.

`d_trace_id_format` — The schema constrains traceId only to a string, and a UUID is a standard local correlation identifier compatible with the in-memory implementation.

`d_idempotency_snapshot` — The contract explicitly requires replaying the first response byte-for-byte in meaning, including its original status outcome, rather than returning current state.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 55건 중 54건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

라벨은 그 차이가 쓰는 사람에게 어떤 뜻인지다. 판정을 가르지 않고 이 절의 순서만 정한다.

| 라벨 | 뜻 |
| --- | --- |
| withholds_promised_response | 계약이 주겠다고 한 것을 구현이 주지 않게 만드는 차이다. 읽는 쪽이 있다고 믿은 것이 없다. |
| undeclared_surface | 계약이 말하지 않은 표면이 자라는 차이다. 문서 밖의 것을 쓰는 사람이 의지하게 된다. |
| harmless_to_client | 쓰는 쪽에 손해가 없는 차이다. 응답의 값이 계약보다 좁거나 요청을 계약보다 넓게 받는 자리다. 차이는 차이이므로 판정은 그대로이고, 리포트에서 뒤로 온다. |

그 55건은 원인 22개에서 나왔다. 그중 5개가 여러 판정 지점에 걸쳐 38건을 만들었다. 계약이 같은 컴포넌트를 여러 응답에서 참조하면 결함 하나가 지점 수만큼 세어지므로, 고칠 일의 개수는 위반의 개수가 아니라 원인의 개수다.

| 원인 | 규칙 | family | 좌표 | 라벨 | 닿는 지점 | 상태 | 닫은 방법 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| v_579dfe | g0.compile_failed | — | mvnw test | — | 0 | open | — |
| v_035a89, v_061225, v_213a44 외 24건 | response.status_missing | missing | responses | withholds_promised_response | 27 | closed | code |
| v_1de197, v_4589ee, v_66d51e | response.field_differs | differs | Alert.slot | withholds_promised_response | 3 | closed | code |
| v_4b32e2, v_c9214b, v_d6c9f5 | response.field_differs | differs | Alert | withholds_promised_response | 3 | closed | code |
| v_03c067 | response.field_differs | differs | AlertList | withholds_promised_response | 1 | closed | code |
| v_3ad4dd | response.field_differs | differs | AlertList.items[] | withholds_promised_response | 1 | closed | code |
| v_430fb8 | response.field_differs | differs | CourtAvailability | withholds_promised_response | 1 | closed | code |
| v_aa9ec9 | response.field_differs | differs | AlertList.items[].slot | withholds_promised_response | 1 | closed | code |
| v_e4df80 | response.field_differs | differs | CourtAvailability.slots[] | withholds_promised_response | 1 | closed | code |
| v_5bdad4 | response.header_missing | missing | headers.Location | withholds_promised_response | 1 | closed | code |
| v_9c7d67 | response.header_missing | missing | headers.Idempotency-Replayed | withholds_promised_response | 1 | closed | code |
| v_42778f | extract.blind | — | x-error-codes | undeclared_surface | 0 | closed | code |
| v_f5534b | extract.blind | — | security | undeclared_surface | 0 | closed | code |
| v_a87fd1, v_ab56e3, v_c1ea6e | response.field_differs | differs | Alert.delivery | harmless_to_client | 3 | closed | code |
| v_3b3037, v_c74ea5 | request.input_differs | differs | parameters.path.alertId | harmless_to_client | 2 | closed | code |
| v_0baf83 | request.input_differs | differs | AlertRequest.slot | harmless_to_client | 1 | closed | code |
| v_116805 | request.input_differs | differs | parameters.path.courtId | harmless_to_client | 1 | closed | code |
| v_2e68f6 | request.input_differs | differs | parameters.header.Idempotency-Key | harmless_to_client | 1 | closed | code |
| v_376dae | request.input_differs | differs | AlertRequest | harmless_to_client | 1 | closed | code |
| v_988f51 | request.input_differs | differs | parameters.query.status | harmless_to_client | 1 | closed | code |
| v_f88b25 | request.input_differs | differs | parameters.query.date | harmless_to_client | 1 | closed | code |
| v_960ca3 | response.field_differs | differs | AlertList.items[].delivery | harmless_to_client | 1 | closed | code |

접힌 잎이 있는 원인은 16개이고 잎은 모두 21개다. 게이트가 한 자리를 통째로 다르다고 보고 아래를 접은 것이므로, 대조기가 보지 못한 자리와 다르다. 보지 못한 자리는 3절에 따로 있다.

### `g0.compile_failed` @ `mvnw test`

컴파일 오류 2건
src/main/java/com/thinking/tennis/domain/AlertDelivery.java:22:12 recursive constructor invocation
src/main/java/com/thinking/tennis/app/AlertApplicationService.java:201:82 incompatible types: long cannot be converted to java.lang.Integer. 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 컴파일 오류 2건
src/main/java/com/thinking/tennis/domain/AlertDelivery.java:22:12 recursive constructor invocation
src/main/java/com/thinking/tennis/app/AlertApplicationService.java:201:82 incompatible types: long cannot be converted to java.lang.Integer

주장한 수정은 없다. 확인된 수정은 없다.

### `response.status_missing` @ `responses`

계약이 선언한 상태 코드다. 닿는 판정 지점은 27곳이다: `createAlert:400`, `getAlert:404`, `listAlerts:500`, `getAlert:503`, `createAlert:422`, `getCourtAvailability:404`, `createAlert:401`, `getAlert:500` 외 19곳.

왜 문제인가 — 계약이 선언한 응답 상태 코드가 구현에 없다. 그 아래의 본문과 헤더와 에러 코드는 이 한 건이 설명한다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`이다.

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -5,4 +5,18 @@
 import com.thinking.tennis.domain.AlertStatus;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.ParameterIn;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.security.SecurityRequirements;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
@@ -26,2 +40,3 @@
 import java.util.UUID;
+import java.util.regex.Pattern;
 
@@ -29,3 +44,8 @@
 @RequestMapping
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
+        in = SecuritySchemeIn.HEADER)
 public class TennisAlertController {
+    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
+    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
+    private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
 
@@ -37,9 +57,33 @@
 
-    @GetMapping(name = "getCourtAvailability", path = "/courts/{courtId}/availability")
+    @GetMapping("/courts/{courtId}/availability")
+    @Operation(operationId = "getCourtAvailability",
... (336줄 더)
```

### `response.field_differs` @ `Alert.slot`

필수: 2곳에서 다르다. 닿는 판정 지점은 3곳이다: `getAlert:200`, `createAlert:200`, `cancelAlert:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AlertDelivery.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 2개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 필수: endTime: 계약이 항상 담겠다고 보장했는데 구현은 선택이다
- 필수: startTime: 계약이 항상 담겠다고 보장했는데 구현은 선택이다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `response.field_differs` @ `Alert`

형식: reservationUrl: 계약 uri → 구현 None. 닿는 판정 지점은 3곳이다: `createAlert:200`, `cancelAlert:200`, `getAlert:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/ProblemResponse.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AlertDelivery.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: reservationUrl: 계약 uri → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `response.field_differs` @ `AlertList`

필수: items: 계약이 항상 담겠다고 보장했는데 구현은 선택이다. 닿는 판정 지점은 1곳이다: `listAlerts:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 필수: items: 계약이 항상 담겠다고 보장했는데 구현은 선택이다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

### `response.field_differs` @ `AlertList.items[]`

형식: reservationUrl: 계약 uri → 구현 None. 닿는 판정 지점은 1곳이다: `listAlerts:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/ProblemResponse.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: reservationUrl: 계약 uri → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

### `response.field_differs` @ `CourtAvailability`

형식: reservationUrl: 계약 uri → 구현 None. 닿는 판정 지점은 1곳이다: `getCourtAvailability:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/ProblemResponse.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: reservationUrl: 계약 uri → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

### `response.field_differs` @ `AlertList.items[].slot`

필수: 2곳에서 다르다. 닿는 판정 지점은 1곳이다: `listAlerts:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 2개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 필수: endTime: 계약이 항상 담겠다고 보장했는데 구현은 선택이다
- 필수: startTime: 계약이 항상 담겠다고 보장했는데 구현은 선택이다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `response.field_differs` @ `CourtAvailability.slots[]`

필수: 3곳에서 다르다. 닿는 판정 지점은 1곳이다: `getCourtAvailability:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 3개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 필수: available: 계약이 항상 담겠다고 보장했는데 구현은 선택이다
- 필수: endTime: 계약이 항상 담겠다고 보장했는데 구현은 선택이다
- 필수: startTime: 계약이 항상 담겠다고 보장했는데 구현은 선택이다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `response.header_missing` @ `headers.Location`

계약이 싣겠다고 선언한 헤더다. 닿는 판정 지점은 1곳이다: `createAlert:200`.

왜 문제인가 — 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`이다.

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -5,4 +5,18 @@
 import com.thinking.tennis.domain.AlertStatus;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.ParameterIn;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.security.SecurityRequirements;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
@@ -26,2 +40,3 @@
 import java.util.UUID;
+import java.util.regex.Pattern;
 
@@ -29,3 +44,8 @@
 @RequestMapping
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
+        in = SecuritySchemeIn.HEADER)
 public class TennisAlertController {
+    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
+    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
+    private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
 
@@ -37,9 +57,33 @@
 
-    @GetMapping(name = "getCourtAvailability", path = "/courts/{courtId}/availability")
+    @GetMapping("/courts/{courtId}/availability")
+    @Operation(operationId = "getCourtAvailability",
... (336줄 더)
```

### `response.header_missing` @ `headers.Idempotency-Replayed`

계약이 싣겠다고 선언한 헤더다. 닿는 판정 지점은 1곳이다: `createAlert:200`.

왜 문제인가 — 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`이다.

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -5,4 +5,18 @@
 import com.thinking.tennis.domain.AlertStatus;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.ParameterIn;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.security.SecurityRequirements;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
@@ -26,2 +40,3 @@
 import java.util.UUID;
+import java.util.regex.Pattern;
 
@@ -29,3 +44,8 @@
 @RequestMapping
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
+        in = SecuritySchemeIn.HEADER)
 public class TennisAlertController {
+    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
+    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
+    private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
 
@@ -37,9 +57,33 @@
 
-    @GetMapping(name = "getCourtAvailability", path = "/courts/{courtId}/availability")
+    @GetMapping("/courts/{courtId}/availability")
+    @Operation(operationId = "getCourtAvailability",
... (336줄 더)
```

### `extract.blind` @ `x-error-codes`

계약은 (상태 코드, 에러 코드) 쌍을 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다. 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`이다.

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -5,4 +5,18 @@
 import com.thinking.tennis.domain.AlertStatus;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.ParameterIn;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.security.SecurityRequirements;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
@@ -26,2 +40,3 @@
 import java.util.UUID;
+import java.util.regex.Pattern;
 
@@ -29,3 +44,8 @@
 @RequestMapping
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
+        in = SecuritySchemeIn.HEADER)
 public class TennisAlertController {
+    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
+    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
+    private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
 
@@ -37,9 +57,33 @@
 
-    @GetMapping(name = "getCourtAvailability", path = "/courts/{courtId}/availability")
+    @GetMapping("/courts/{courtId}/availability")
+    @Operation(operationId = "getCourtAvailability",
... (336줄 더)
```

### `extract.blind` @ `security`

계약은 인증 요구를 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다. 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/TennisAlertController.java`이다.

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -5,4 +5,18 @@
 import com.thinking.tennis.domain.AlertStatus;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.ParameterIn;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.security.SecurityRequirements;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
@@ -26,2 +40,3 @@
 import java.util.UUID;
+import java.util.regex.Pattern;
 
@@ -29,3 +44,8 @@
 @RequestMapping
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
+        in = SecuritySchemeIn.HEADER)
 public class TennisAlertController {
+    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
+    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
+    private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
 
@@ -37,9 +57,33 @@
 
-    @GetMapping(name = "getCourtAvailability", path = "/courts/{courtId}/availability")
+    @GetMapping("/courts/{courtId}/availability")
+    @Operation(operationId = "getCourtAvailability",
... (336줄 더)
```

### `response.field_differs` @ `Alert.delivery`

형식: attemptCount: 계약 None → 구현 int32. 닿는 판정 지점은 3곳이다: `createAlert:200`, `cancelAlert:200`, `getAlert:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AlertDelivery.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: attemptCount: 계약 None → 구현 int32

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `request.input_differs` @ `parameters.path.alertId`

형식: 계약 uuid → 구현 None. 닿는 판정 지점은 2곳이다: `getAlert`, `cancelAlert`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: 계약 uuid → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

### `request.input_differs` @ `AlertRequest.slot`

모양: 2곳에서 다르다. 닿는 판정 지점은 1곳이다: `createAlert`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 2개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 모양: endTime: 계약 ^([01][0-9]|2[0-3]):[0-5][0-9]$ → 구현 None
- 모양: startTime: 계약 ^([01][0-9]|2[0-3]):[0-5][0-9]$ → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `request.input_differs` @ `parameters.path.courtId`

모양: 계약 ^[a-z0-9][a-z0-9-]*$ → 구현 None. 닿는 판정 지점은 1곳이다: `getCourtAvailability`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 모양: 계약 ^[a-z0-9][a-z0-9-]*$ → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `request.input_differs` @ `parameters.header.Idempotency-Key`

형식: 계약 uuid → 구현 None. 닿는 판정 지점은 1곳이다: `createAlert`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: 계약 uuid → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

### `request.input_differs` @ `AlertRequest`

형식: date: 계약 date → 구현 None. 닿는 판정 지점은 1곳이다: `createAlert`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: date: 계약 date → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `request.input_differs` @ `parameters.query.status`

열거형: 계약은 ['WATCHING', 'NOTIFIED', 'CANCELED', 'EXPIRED']로 열거했는데 구현은 열거하지 않는다. 닿는 판정 지점은 1곳이다: `listAlerts`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AlertDelivery.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 열거형: 계약은 ['WATCHING', 'NOTIFIED', 'CANCELED', 'EXPIRED']로 열거했는데 구현은 열거하지 않는다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

### `request.input_differs` @ `parameters.query.date`

형식: 계약 date → 구현 None. 닿는 판정 지점은 1곳이다: `getCourtAvailability`.

왜 문제인가 — 계약이 그 입력에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/AlertRequestPayload.java`, `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java`, `src/main/java/com/thinking/tennis/app/AlertApplicationService.java`, `src/main/java/com/thinking/tennis/app/UseCaseException.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: 계약 date → 구현 None

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequestPayload.java
@@ -2,19 +2,24 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.util.Objects;
+@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+public record AlertRequestPayload(
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                minLength = 1,
+                maxLength = 64,
+                pattern = "^[a-z0-9][a-z0-9-]*$")
+        String courtId,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+        String date,
+        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+        TimeSlotPayload slot) {
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequestPayload(
-        String courtId,
-        String date,
-        TimeSlotPayload slot
-) {
-    public boolean hasAllFields() {
-        return courtId != null && date != null && slot != null
-                && slot.startTime() != null && slot.endTime() != null;
-    }
-
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotPayload(String startTime, String endTime) {
+    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
+    public record TimeSlotPayload(
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
+            String startTime,
+            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
+                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
... (2줄 더)
```

### `response.field_differs` @ `AlertList.items[].delivery`

형식: attemptCount: 계약 None → 구현 int32. 닿는 판정 지점은 1곳이다: `listAlerts:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/ApiResponses.java`, `src/main/java/com/thinking/tennis/api/TennisAlertController.java`, `src/main/java/com/thinking/tennis/domain/Alert.java`, `src/main/java/com/thinking/tennis/domain/AlertDelivery.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 형식: attemptCount: 계약 None → 구현 int32

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiResponses.java
+++ b/src/main/java/com/thinking/tennis/api/ApiResponses.java
@@ -4,3 +4,6 @@
 import com.thinking.tennis.domain.AlertDelivery;
+import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
+import com.thinking.tennis.domain.DeliveryStatus;
+import io.swagger.v3.oas.annotations.media.Schema;
 
@@ -10,6 +13,4 @@
 import java.util.List;
-import java.util.UUID;
 
 public final class ApiResponses {
-
     private ApiResponses() {
@@ -17,27 +18,12 @@
 
-    public static AlertResponse alert(Alert alert, boolean checkDelayed) {
-        TimeSlotResponse slot = new TimeSlotResponse(alert.startTime().toString(), alert.endTime().toString());
-        AlertDelivery delivery = alert.delivery();
-        DeliveryResponse deliveryResponse = delivery == null
-                ? null
-                : new DeliveryResponse(
-                delivery.status().name(),
-                delivery.attemptCount(),
-                delivery.lastAttemptAt(),
-                delivery.failureReason()
-        );
-        return new AlertResponse(
-                alert.alertId(),
-                alert.courtId(),
-                alert.courtName(),
-                alert.reservationUrl(),
-                alert.date(),
-                slot,
-                alert.status().name(),
-                alert.createdAt(),
-                alert.expiresAt(),
-                alert.lastCheckedAt(),
... (165줄 더)
```

판정은 지점 단위로 한다. 어느 지점이 아직 열려 있는지는 지점으로만 말할 수 있다.

| id | 규칙 | 판정 지점 | 좌표 | 라벨 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_579dfe | g0.compile_failed | — | mvnw test | — | open | — | 0 | 002 | — |
| v_035a89 | response.status_missing | createAlert:400 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_03c067 | response.field_differs | listAlerts:200 | AlertList | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_061225 | response.status_missing | getAlert:404 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_1de197 | response.field_differs | getAlert:200 | Alert.slot | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_213a44 | response.status_missing | listAlerts:500 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_36e659 | response.status_missing | getAlert:503 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_3ad4dd | response.field_differs | listAlerts:200 | AlertList.items[] | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_430fb8 | response.field_differs | getCourtAvailability:200 | CourtAvailability | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_4589ee | response.field_differs | createAlert:200 | Alert.slot | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_499bdd | response.status_missing | createAlert:422 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_4b32e2 | response.field_differs | createAlert:200 | Alert | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_4fac05 | response.status_missing | getCourtAvailability:404 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_52b438 | response.status_missing | createAlert:401 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_552a8e | response.status_missing | getAlert:500 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_57240f | response.status_missing | cancelAlert:401 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_5b8320 | response.status_missing | createAlert:500 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_5bdad4 | response.header_missing | createAlert:200 | headers.Location | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_5c0d30 | response.status_missing | listAlerts:503 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_66d51e | response.field_differs | cancelAlert:200 | Alert.slot | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_7e4938 | response.status_missing | cancelAlert:500 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_817689 | response.status_missing | getCourtAvailability:400 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_81cde5 | response.status_missing | cancelAlert:400 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_8298d6 | response.status_missing | cancelAlert:404 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_851e03 | response.status_missing | listAlerts:400 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_8ad7ff | response.status_missing | getCourtAvailability:502 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_96b99a | response.status_missing | getCourtAvailability:503 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_9c5b4a | response.status_missing | getAlert:401 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_9c7d67 | response.header_missing | createAlert:200 | headers.Idempotency-Replayed | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_a6ff3e | response.status_missing | createAlert:503 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_aa9ec9 | response.field_differs | listAlerts:200 | AlertList.items[].slot | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_aad0b3 | response.status_missing | createAlert:201 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_ab307f | response.status_missing | getCourtAvailability:504 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_c9214b | response.field_differs | cancelAlert:200 | Alert | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_c9ec52 | response.status_missing | getAlert:400 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_d6c9f5 | response.field_differs | getAlert:200 | Alert | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_e4df80 | response.field_differs | getCourtAvailability:200 | CourtAvailability.slots[] | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_e85568 | response.status_missing | cancelAlert:503 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_f31579 | response.status_missing | listAlerts:401 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_f3725a | response.status_missing | getCourtAvailability:500 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_f47ecb | response.status_missing | createAlert:409 | responses | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_42778f | extract.blind | — | x-error-codes | undeclared_surface | closed | code | 0 | 001 | 002 |
| v_f5534b | extract.blind | — | security | undeclared_surface | closed | code | 0 | 001 | 002 |
| v_0baf83 | request.input_differs | createAlert | AlertRequest.slot | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_116805 | request.input_differs | getCourtAvailability | parameters.path.courtId | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_2e68f6 | request.input_differs | createAlert | parameters.header.Idempotency-Key | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_376dae | request.input_differs | createAlert | AlertRequest | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_3b3037 | request.input_differs | getAlert | parameters.path.alertId | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_960ca3 | response.field_differs | listAlerts:200 | AlertList.items[].delivery | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_988f51 | request.input_differs | listAlerts | parameters.query.status | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_a87fd1 | response.field_differs | createAlert:200 | Alert.delivery | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_ab56e3 | response.field_differs | cancelAlert:200 | Alert.delivery | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_c1ea6e | response.field_differs | getAlert:200 | Alert.delivery | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_c74ea5 | request.input_differs | cancelAlert | parameters.path.alertId | harmless_to_client | closed | code | 0 | 001 | 002 |
| v_f88b25 | request.input_differs | getCourtAvailability | parameters.query.date | harmless_to_client | closed | code | 0 | 001 | 002 |

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/app/ClockConfiguration.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 70건이다.

### 충족 주장과 게이트 판정 (64건)

주장은 판정이 아니다. 어긋난 자리만 남긴다.

| iteration | point | 관찰 |
| --- | --- | --- |
| 001 | getCourtAvailability:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:400 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:404 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:502 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:504 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:400 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:401 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:201 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:400 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:401 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:409 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:422 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:400 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:401 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:404 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:400 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:401 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:404 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 002 | getCourtAvailability:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:502 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:201 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:409 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:422 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |

### 축 판정의 어긋남 (1건)

Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다.

| iteration | axis | critique_id | eval_score | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | judgement_disclosure | w_slot_cache_rejection_needs_contract_surface | 4 | Critique는 이 축을 high로 지적했는데 Eval은 4를 줬다 |

### 게이트가 세는 것을 다시 말한 지적 (5건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다.

| iteration | axis | critique_id | rule | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | state_continuity | w_alerts_not_linked_to_checks | request.input_differs | 게이트가 request.input_differs로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | request_tolerance | w_time_parsing_too_permissive_and_too_narrow | response.field_differs | 게이트가 response.field_differs로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | response_fidelity | w_check_delayed_false_for_terminal_alerts | response.field_differs | 게이트가 response.field_differs로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | failure_faithfulness | w_idempotency_concurrent_failure_after_expired_record | request.input_differs | 게이트가 request.input_differs로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | judgement_disclosure | w_slot_cache_rejection_needs_contract_surface | request.input_differs | 게이트가 request.input_differs로 이미 세는 자리를 비평이 다시 말했다 |

### 게이트가 남긴 기록 (7건)

계약이 말하지 않은 자리의 초과다. REJECT 사유가 아니고 뜻은 Critique가 붙인다.

| 규칙 | 판정 지점 | 좌표 | iteration | 관찰 |
| --- | --- | --- | --- | --- |
| gate:request.input_extra | listAlerts | parameters.header.Authorization | 001 | 계약이 말하지 않은 파라미터다 |
| gate:request.input_extra | createAlert | parameters.header.Authorization | 001 | 계약이 말하지 않은 파라미터다 |
| gate:request.input_extra | getAlert | parameters.header.Authorization | 001 | 계약이 말하지 않은 파라미터다 |
| gate:request.input_extra | cancelAlert | parameters.header.Authorization | 001 | 계약이 말하지 않은 파라미터다 |
| gate:decision.unknown_point | Bearer token owner identification | src/main/java/com/thinking/tennis/api/TennisAlertController.java:authenticate | 001 | 계약의 판정 지점이 아니다 (결정 d_bearer_identity) |
| gate:decision.unknown_point | Alert lifecycle expiration | src/main/java/com/thinking/tennis/app/AlertApplicationService.java:expireDueAlerts | 001 | 계약의 판정 지점이 아니다 (결정 d_lazy_expiry) |
| gate:decision.unknown_point | Problem:traceId | src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java:response | 001 | 계약의 판정 지점이 아니다 (결정 d_trace_id_format) |
