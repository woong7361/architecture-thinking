# 계약 적합성 run 리포트 — 2026-09-29_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T01:29:53+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 003까지 왔다. 위반 114건 가운데 57건을 코드로 닫고 0건을 계약으로 닫았으며 57건이 아직 열려 있다. 서 있는 결정은 8건, 계약 변경 기록은 0건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api.yaml-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api.yaml-v1.yaml`이다.

`decision_risk`는 **low** 수준이다. 올릴 근거는 없다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:error.code_pair_missing@getCourtAvailability:VALIDATION_FAILED: 계약이 선언한 쌍이다 (400, VALIDATION_FAILED)
- gate:error.code_undeclared@getCourtAvailability:values: 계약의 선언 밖이다 (400, values)
- gate:error.code_pair_missing@getCourtAvailability:COURT_NOT_SUPPORTED: 계약이 선언한 쌍이다 (404, COURT_NOT_SUPPORTED)
- gate:error.code_undeclared@getCourtAvailability:values: 계약의 선언 밖이다 (404, values)
- gate:error.code_pair_missing@getCourtAvailability:INTERNAL_ERROR: 계약이 선언한 쌍이다 (500, INTERNAL_ERROR)
- gate:error.code_undeclared@getCourtAvailability:values: 계약의 선언 밖이다 (500, values)
- gate:error.code_pair_missing@getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE: 계약이 선언한 쌍이다 (502, UPSTREAM_RESPONSE_UNREADABLE)
- gate:error.code_undeclared@getCourtAvailability:values: 계약의 선언 밖이다 (502, values)
- gate:error.code_pair_missing@getCourtAvailability:UPSTREAM_UNAVAILABLE: 계약이 선언한 쌍이다 (503, UPSTREAM_UNAVAILABLE)
- gate:error.code_pair_missing@getCourtAvailability:STORAGE_TIMEOUT: 계약이 선언한 쌍이다 (503, STORAGE_TIMEOUT)
- gate:error.code_undeclared@getCourtAvailability:values: 계약의 선언 밖이다 (503, values)
- gate:error.code_pair_missing@getCourtAvailability:UPSTREAM_TIMEOUT: 계약이 선언한 쌍이다 (504, UPSTREAM_TIMEOUT)
- gate:error.code_undeclared@getCourtAvailability:values: 계약의 선언 밖이다 (504, values)
- gate:error.code_pair_missing@listAlerts:VALIDATION_FAILED: 계약이 선언한 쌍이다 (400, VALIDATION_FAILED)
- gate:error.code_undeclared@listAlerts:values: 계약의 선언 밖이다 (400, values)
- gate:error.code_pair_missing@listAlerts:UNAUTHENTICATED: 계약이 선언한 쌍이다 (401, UNAUTHENTICATED)
- gate:error.code_undeclared@listAlerts:values: 계약의 선언 밖이다 (401, values)
- gate:error.code_pair_missing@listAlerts:INTERNAL_ERROR: 계약이 선언한 쌍이다 (500, INTERNAL_ERROR)
- gate:error.code_undeclared@listAlerts:values: 계약의 선언 밖이다 (500, values)
- gate:error.code_pair_missing@listAlerts:STORAGE_TIMEOUT: 계약이 선언한 쌍이다 (503, STORAGE_TIMEOUT)
- gate:error.code_undeclared@listAlerts:values: 계약의 선언 밖이다 (503, values)
- gate:error.code_pair_missing@createAlert:VALIDATION_FAILED: 계약이 선언한 쌍이다 (400, VALIDATION_FAILED)
- gate:error.code_undeclared@createAlert:values: 계약의 선언 밖이다 (400, values)
- gate:error.code_pair_missing@createAlert:UNAUTHENTICATED: 계약이 선언한 쌍이다 (401, UNAUTHENTICATED)
- gate:error.code_undeclared@createAlert:values: 계약의 선언 밖이다 (401, values)
- gate:error.code_pair_missing@createAlert:IDEMPOTENCY_KEY_REUSED: 계약이 선언한 쌍이다 (409, IDEMPOTENCY_KEY_REUSED)
- gate:error.code_undeclared@createAlert:values: 계약의 선언 밖이다 (409, values)
- gate:error.code_pair_missing@createAlert:COURT_NOT_SUPPORTED: 계약이 선언한 쌍이다 (422, COURT_NOT_SUPPORTED)
- gate:error.code_pair_missing@createAlert:SLOT_NOT_SUPPORTED: 계약이 선언한 쌍이다 (422, SLOT_NOT_SUPPORTED)
- gate:error.code_pair_missing@createAlert:ALERT_WINDOW_CLOSED: 계약이 선언한 쌍이다 (422, ALERT_WINDOW_CLOSED)
- gate:error.code_undeclared@createAlert:values: 계약의 선언 밖이다 (422, values)
- gate:error.code_pair_missing@createAlert:INTERNAL_ERROR: 계약이 선언한 쌍이다 (500, INTERNAL_ERROR)
- gate:error.code_undeclared@createAlert:values: 계약의 선언 밖이다 (500, values)
- gate:error.code_pair_missing@createAlert:STORAGE_TIMEOUT: 계약이 선언한 쌍이다 (503, STORAGE_TIMEOUT)
- gate:error.code_pair_missing@createAlert:CONCURRENT_UPDATE_CONFLICT: 계약이 선언한 쌍이다 (503, CONCURRENT_UPDATE_CONFLICT)
- gate:error.code_undeclared@createAlert:values: 계약의 선언 밖이다 (503, values)
- gate:error.code_pair_missing@getAlert:VALIDATION_FAILED: 계약이 선언한 쌍이다 (400, VALIDATION_FAILED)
- gate:error.code_undeclared@getAlert:values: 계약의 선언 밖이다 (400, values)
- gate:error.code_pair_missing@getAlert:UNAUTHENTICATED: 계약이 선언한 쌍이다 (401, UNAUTHENTICATED)
- gate:error.code_undeclared@getAlert:values: 계약의 선언 밖이다 (401, values)
- gate:error.code_pair_missing@getAlert:ALERT_NOT_FOUND: 계약이 선언한 쌍이다 (404, ALERT_NOT_FOUND)
- gate:error.code_undeclared@getAlert:values: 계약의 선언 밖이다 (404, values)
- gate:error.code_pair_missing@getAlert:INTERNAL_ERROR: 계약이 선언한 쌍이다 (500, INTERNAL_ERROR)
- gate:error.code_undeclared@getAlert:values: 계약의 선언 밖이다 (500, values)
- gate:error.code_pair_missing@getAlert:STORAGE_TIMEOUT: 계약이 선언한 쌍이다 (503, STORAGE_TIMEOUT)
- gate:error.code_undeclared@getAlert:values: 계약의 선언 밖이다 (503, values)
- gate:error.code_pair_missing@cancelAlert:VALIDATION_FAILED: 계약이 선언한 쌍이다 (400, VALIDATION_FAILED)
- gate:error.code_undeclared@cancelAlert:values: 계약의 선언 밖이다 (400, values)
- gate:error.code_pair_missing@cancelAlert:UNAUTHENTICATED: 계약이 선언한 쌍이다 (401, UNAUTHENTICATED)
- gate:error.code_undeclared@cancelAlert:values: 계약의 선언 밖이다 (401, values)
- gate:error.code_pair_missing@cancelAlert:ALERT_NOT_FOUND: 계약이 선언한 쌍이다 (404, ALERT_NOT_FOUND)
- gate:error.code_undeclared@cancelAlert:values: 계약의 선언 밖이다 (404, values)
- gate:error.code_pair_missing@cancelAlert:INTERNAL_ERROR: 계약이 선언한 쌍이다 (500, INTERNAL_ERROR)
- gate:error.code_undeclared@cancelAlert:values: 계약의 선언 밖이다 (500, values)
- gate:error.code_pair_missing@cancelAlert:STORAGE_TIMEOUT: 계약이 선언한 쌍이다 (503, STORAGE_TIMEOUT)
- gate:error.code_pair_missing@cancelAlert:CONCURRENT_UPDATE_CONFLICT: 계약이 선언한 쌍이다 (503, CONCURRENT_UPDATE_CONFLICT)
- gate:error.code_undeclared@cancelAlert:values: 계약의 선언 밖이다 (503, values)

루브릭:
- min_total: 1.45 < 4.2
- min_axis.request_tolerance: 1 < 4.0
- min_axis.response_fidelity: 1 < 4.0
- min_axis.failure_faithfulness: 1 < 4.0
- min_axis.state_continuity: 1 < 4.0

색인과 실물이 어긋난 경로가 18곳이다. 게이트는 색인이 선언한 것을 판정하므로 선언 밖에 놓인 파일은 판정되지 않은 표면이 된다.

| 경로 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/AlertRequest.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AlertResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/BearerUser.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ProblemResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidation.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/TennisAlertController.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertExpiryJob.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityService.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/SchedulingConfig.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/UseCaseException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertDelivery.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertStatus.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DeliveryStatus.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |

판정되지 않은 검사가 있다: changes, g1. 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | PASS | REJECT | PASS | 56 | 51 | 4.6 |
| 002 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |
| 003 | REJECT | PASS | REJECT | PASS | 57 | 14 | 1.45 |

## 2. 계약을 이렇게 고쳤다

계약 판본은 이 run에서 움직이지 않았다. 고칠 자리를 찾지 못했거나 고칠 필요가 없었다는 뜻이다.

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 123곳이 달라지고 깨는 변경 0건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 123곳이 달라지고 깨는 변경 0건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 사라진 판정 지점 |
| --- | --- | --- | --- |
| 앞 판본 | 0 | 0 | — |
| 이 run의 기준선 | 123 | 0 | — |
| 사람이 확정한 원본 | 123 | 0 | — |

기준선 대비 달라진 좌표는 이렇다.

- `/tags`
- `/info/x-out-of-scope`
- `/info/description`
- `/paths/~1courts~1{courtId}~1availability/get/tags`
- `/paths/~1courts~1{courtId}~1availability/get/summary`
- `/paths/~1courts~1{courtId}~1availability/get/description`
- `/paths/~1courts~1{courtId}~1availability/get/x-requirement`
- `/paths/~1courts~1{courtId}~1availability/get/parameters/1/description`
- `/paths/~1courts~1{courtId}~1availability/get/parameters/1/example`
- `/paths/~1courts~1{courtId}~1availability/get/responses/200/description`
- `/paths/~1courts~1{courtId}~1availability/get/responses/200/content/application~1json/examples`
- `/paths/~1alerts/post/tags`
- `/paths/~1alerts/post/summary`
- `/paths/~1alerts/post/description`
- `/paths/~1alerts/post/x-requirement`
- `/paths/~1alerts/post/requestBody/content/application~1json/example`
- `/paths/~1alerts/post/responses/201/description`
- `/paths/~1alerts/post/responses/201/content/application~1json/example`
- `/paths/~1alerts/post/responses/200/description`
- `/paths/~1alerts/post/responses/200/content/application~1json/example`
- `/paths/~1alerts/get/tags`
- `/paths/~1alerts/get/summary`
- `/paths/~1alerts/get/description`
- `/paths/~1alerts/get/x-requirement`
- `/paths/~1alerts/get/parameters/0/description`
- `/paths/~1alerts/get/responses/200/description`
- `/paths/~1alerts/get/responses/200/content/application~1json/example`
- `/paths/~1alerts~1{alertId}/get/tags`
- `/paths/~1alerts~1{alertId}/get/summary`
- `/paths/~1alerts~1{alertId}/get/description`
- 그 밖에 93곳

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 10건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_problem_representation | Problem의 자유 형식 문자열과 traceId 형식을 어떤 규칙으로 만들 것인가? | 코드 kebab-case URI, 한국어 제목, UUID traceId를 사용한다. | contract_analogy | medium | ApiExceptionHandler의 중앙 매핑만 수정하면 된다. | getCourtAvailability:400, getCourtAvailability:404, getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:400, createAlert:401, createAlert:409, createAlert:422, createAlert:500, createAlert:502, createAlert:503, createAlert:504, listAlerts:400, listAlerts:401, listAlerts:500, listAlerts:503, getAlert:400, getAlert:401, getAlert:404, getAlert:500, getAlert:503, cancelAlert:400, cancelAlert:401, cancelAlert:404, cancelAlert:500, cancelAlert:503 | standing |
| d_bearer_subject_boundary | 계약이 토큰 검증자와 토큰 내용 형식을 정하지 않은 상태에서 API 경계가 어떤 값을 애플리케이션 사용자 키로 넘길 것인가? | Bearer 형식과 비어 있지 않은 허용 문자 집합을 먼저 검증한 뒤 불투명 subject를 전달한다. | least_harm | medium | BearerUser의 subject 추출부에 실제 인증 provider 검증기를 주입하면 된다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401 | standing |
| d_expiry_scheduled_transition | 조회 요청과 분리된 만료 상태 전이를 어떤 서버 작업으로 수행할 것인가? | 1초 고정 지연의 서버 작업이 WATCHING만 EXPIRED로 원자 전이한다. | spec_implication | medium | 스케줄러를 외부 작업 실행기나 만료 큐로 교체하면 된다. | listAlerts:200, getAlert:200, createAlert:200, cancelAlert:200 | standing |
| d_stored_expiry_projection | 만료 시각이 지났지만 서버 만료 작업이 아직 상태를 저장하지 않은 신청을 조회할 때 어떻게 할 것인가? | HTTP 조회에서는 저장된 status를 유지한다. | contract_analogy | medium | 만료 작업을 연결하고 저장소 갱신 정책을 추가해야 한다. | listAlerts:200, getAlert:200, cancelAlert:200, createAlert:422 | standing |
| d_create_slot_check_failure | createAlert 계약에 upstream 실패 응답이 선언되지 않은 상태에서 시간대 확인이 실패하면 어떻게 응답할 것인가? | INTERNAL_ERROR로 변환한다. | least_harm | medium | createAlert에 upstream 실패 응답을 연결하거나 별도 조건 확인 포트를 추가해야 한다. | createAlert:500, createAlert:422 | superseded |
| d_bearer_subject_mapping | 계약이 토큰 검증자와 토큰 내용 형식을 정하지 않은 상태에서 Bearer 값을 어떻게 사용자 키로 사용할 것인가? | 토큰 문자열 자체를 불투명 사용자 키로 사용한다. | least_harm | high | BearerUser를 실제 인증 검증 어댑터로 교체하고 서비스 호출부의 ownerId 공급만 변경하면 된다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, listAlerts:200, getAlert:200, cancelAlert:200 | superseded |
| d_system_utc_clock | 계약이 UTC 타임스탬프를 요구하지만 시계 주입 방식을 정하지 않은 경우 어떤 시계를 사용할 것인가? | Clock.systemUTC()를 사용한다. | spec_implication | high | 서비스의 Clock 생성 방식을 주입형으로 바꾸면 된다. | createAlert:201, createAlert:200, listAlerts:200, getAlert:200, cancelAlert:200, getCourtAvailability:200 | standing |
| d_expiry_boundary | 더 이른 예약 마감 시각을 제공하는 포트가 없을 때 expiresAt을 어떻게 계산할 것인가? | 이용 시작 시각을 expiresAt으로 사용한다. | spec_implication | high | 예약처 포트에 마감 시각을 추가하지 않고 별도 정책 공급원을 연결하면 된다. | createAlert:201, createAlert:200, getAlert:200, listAlerts:200, cancelAlert:200 | standing |
| d_create_upstream_failure_surface | 신청 조건 검증 중 외부 확인이 실패하면 어떤 계약상 의미를 보존할 것인가? | UPSTREAM_TIMEOUT, UPSTREAM_UNAVAILABLE, UPSTREAM_RESPONSE_UNREADABLE을 원래 의미와 Retry-After로 유지한다. | spec_implication | high | 오퍼레이션별 upstream 응답 표면을 다시 좁히려면 핸들러와 API 선언을 함께 변경해야 한다. | createAlert:500, createAlert:502, createAlert:503, createAlert:504 | standing |
| d_idempotency_cleanup | 인메모리 저장소에서 24시간 보관이 지난 멱등 기록을 언제 제거할 것인가? | 멱등 기록 조회 시작 시 지연 정리한다. | least_harm | high | 저장소에 스케줄러 또는 만료 큐를 추가하면 된다. | createAlert:200, createAlert:201, createAlert:409, createAlert:503 | standing |

`d_problem_representation` — 계약 예시가 problem URI와 사람이 읽는 제목을 보여 주고 traceId에는 형식 제한이 없으므로 일관된 관례를 적용했다.

`d_bearer_subject_boundary` — 실제 토큰 검증기는 계약 범위 밖이지만 malformed bearer 값이 인증 사용자로 통과하지 않도록 어댑터 경계를 명시했다.

`d_expiry_scheduled_transition` — FR-5의 서버 소유 전이를 조회 투영과 분리하고, 저장소의 동기화 경계 안에서 terminal 전이를 수행한다.

`d_stored_expiry_projection` — 목록 계약이 저장된 status를 필터 기준으로 명시하고 만료 이동은 별도 서버 작업으로 설명한다.

`d_create_slot_check_failure` — 계약이 createAlert에 외부 예약처 실패 응답을 선언하지 않았으므로 선언된 500 표면을 유지한다.

`d_bearer_subject_mapping` — 인증 방식 자체가 계약 범위 밖이고 사용자 식별만 필요하므로, 추가 형식과 외부 의존성을 만들지 않는다.

`d_system_utc_clock` — 타임스탬프는 UTC이고 지역 시간은 만료 계산에만 사용되므로 시각 공급을 UTC로 고정했다.

`d_expiry_boundary` — 계약은 만료 시각을 이용 시작 시각으로 정의하고 더 이른 마감이 확인될 때만 앞당기도록 했으며 제공 포트에는 마감 정보가 없다.

`d_create_upstream_failure_surface` — 계약이 이미 외부 실패 원인과 재시도 규칙을 정의하므로 알려진 upstream 실패를 내부 오류로 숨기지 않는다.

`d_idempotency_cleanup` — 계약은 보관 기간만 정하고 정리 방식은 정하지 않았으며 인메모리 구현의 의존성을 최소화했다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 114건 중 57건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_06cbfe | error.code_pair_missing | createAlert:ALERT_WINDOW_CLOSED | responses.422 | open | — | 0 | 003 | — |
| v_0b825c | error.code_pair_missing | getAlert:VALIDATION_FAILED | responses.400 | open | — | 0 | 003 | — |
| v_110d3a | error.code_undeclared | getCourtAvailability:values | responses.400 | open | — | 0 | 003 | — |
| v_18034e | error.code_pair_missing | cancelAlert:STORAGE_TIMEOUT | responses.503 | open | — | 0 | 003 | — |
| v_1c4295 | error.code_pair_missing | getAlert:ALERT_NOT_FOUND | responses.404 | open | — | 0 | 003 | — |
| v_1ffe37 | error.code_pair_missing | createAlert:UNAUTHENTICATED | responses.401 | open | — | 0 | 003 | — |
| v_2a124a | error.code_pair_missing | cancelAlert:INTERNAL_ERROR | responses.500 | open | — | 0 | 003 | — |
| v_2a2799 | error.code_pair_missing | cancelAlert:ALERT_NOT_FOUND | responses.404 | open | — | 0 | 003 | — |
| v_2bcbda | error.code_pair_missing | cancelAlert:CONCURRENT_UPDATE_CONFLICT | responses.503 | open | — | 0 | 003 | — |
| v_2d3007 | error.code_pair_missing | getAlert:STORAGE_TIMEOUT | responses.503 | open | — | 0 | 003 | — |
| v_43c7bd | error.code_pair_missing | getAlert:UNAUTHENTICATED | responses.401 | open | — | 0 | 003 | — |
| v_46e972 | error.code_undeclared | createAlert:values | responses.400 | open | — | 0 | 003 | — |
| v_4b5f89 | error.code_pair_missing | createAlert:INTERNAL_ERROR | responses.500 | open | — | 0 | 003 | — |
| v_574543 | error.code_undeclared | getCourtAvailability:values | responses.503 | open | — | 0 | 003 | — |
| v_5e3c7a | error.code_undeclared | listAlerts:values | responses.500 | open | — | 0 | 003 | — |
| v_60cb5d | error.code_pair_missing | getCourtAvailability:STORAGE_TIMEOUT | responses.503 | open | — | 0 | 003 | — |
| v_66a77c | error.code_undeclared | getCourtAvailability:values | responses.502 | open | — | 0 | 003 | — |
| v_6763fb | error.code_undeclared | createAlert:values | responses.422 | open | — | 0 | 003 | — |
| v_7715ca | error.code_pair_missing | listAlerts:INTERNAL_ERROR | responses.500 | open | — | 0 | 003 | — |
| v_77fc7e | error.code_pair_missing | getAlert:INTERNAL_ERROR | responses.500 | open | — | 0 | 003 | — |
| v_79c0a0 | error.code_undeclared | getAlert:values | responses.401 | open | — | 0 | 003 | — |
| v_89e5ba | error.code_pair_missing | listAlerts:STORAGE_TIMEOUT | responses.503 | open | — | 0 | 003 | — |
| v_906a24 | error.code_pair_missing | getCourtAvailability:UPSTREAM_UNAVAILABLE | responses.503 | open | — | 0 | 003 | — |
| v_90ee2e | error.code_pair_missing | createAlert:SLOT_NOT_SUPPORTED | responses.422 | open | — | 0 | 003 | — |
| v_949568 | error.code_undeclared | createAlert:values | responses.500 | open | — | 0 | 003 | — |
| v_95a13d | error.code_pair_missing | getCourtAvailability:COURT_NOT_SUPPORTED | responses.404 | open | — | 0 | 003 | — |
| v_9800af | error.code_undeclared | getAlert:values | responses.404 | open | — | 0 | 003 | — |
| v_995678 | error.code_pair_missing | cancelAlert:VALIDATION_FAILED | responses.400 | open | — | 0 | 003 | — |
| v_9a795f | error.code_undeclared | getAlert:values | responses.503 | open | — | 0 | 003 | — |
| v_9e4ea2 | error.code_undeclared | getAlert:values | responses.400 | open | — | 0 | 003 | — |
| v_a2b5ac | error.code_pair_missing | getCourtAvailability:UPSTREAM_TIMEOUT | responses.504 | open | — | 0 | 003 | — |
| v_aea0bc | error.code_undeclared | getCourtAvailability:values | responses.404 | open | — | 0 | 003 | — |
| v_aee578 | error.code_pair_missing | cancelAlert:UNAUTHENTICATED | responses.401 | open | — | 0 | 003 | — |
| v_b4cd40 | error.code_undeclared | createAlert:values | responses.401 | open | — | 0 | 003 | — |
| v_b520df | error.code_pair_missing | createAlert:CONCURRENT_UPDATE_CONFLICT | responses.503 | open | — | 0 | 003 | — |
| v_b9da18 | error.code_pair_missing | listAlerts:UNAUTHENTICATED | responses.401 | open | — | 0 | 003 | — |
| v_bd8d05 | error.code_pair_missing | listAlerts:VALIDATION_FAILED | responses.400 | open | — | 0 | 003 | — |
| v_be032b | error.code_pair_missing | createAlert:VALIDATION_FAILED | responses.400 | open | — | 0 | 003 | — |
| v_c14d64 | error.code_undeclared | getAlert:values | responses.500 | open | — | 0 | 003 | — |
| v_c5f1ef | error.code_pair_missing | getCourtAvailability:INTERNAL_ERROR | responses.500 | open | — | 0 | 003 | — |
| v_c99204 | error.code_undeclared | cancelAlert:values | responses.404 | open | — | 0 | 003 | — |
| v_da6fef | error.code_pair_missing | createAlert:STORAGE_TIMEOUT | responses.503 | open | — | 0 | 003 | — |
| v_de167e | error.code_pair_missing | createAlert:COURT_NOT_SUPPORTED | responses.422 | open | — | 0 | 003 | — |
| v_df80a5 | error.code_undeclared | getCourtAvailability:values | responses.504 | open | — | 0 | 003 | — |
| v_dfb1ad | error.code_pair_missing | getCourtAvailability:VALIDATION_FAILED | responses.400 | open | — | 0 | 003 | — |
| v_e02fd6 | error.code_undeclared | cancelAlert:values | responses.503 | open | — | 0 | 003 | — |
| v_e18c23 | error.code_undeclared | cancelAlert:values | responses.500 | open | — | 0 | 003 | — |
| v_e1a650 | error.code_undeclared | cancelAlert:values | responses.401 | open | — | 0 | 003 | — |
| v_e40801 | error.code_undeclared | listAlerts:values | responses.400 | open | — | 0 | 003 | — |
| v_ed22a4 | error.code_undeclared | getCourtAvailability:values | responses.500 | open | — | 0 | 003 | — |
| v_f011e9 | error.code_undeclared | createAlert:values | responses.409 | open | — | 0 | 003 | — |
| v_f2e18b | error.code_undeclared | listAlerts:values | responses.401 | open | — | 0 | 003 | — |
| v_f3d8b4 | error.code_undeclared | createAlert:values | responses.503 | open | — | 0 | 003 | — |
| v_f7239c | error.code_undeclared | cancelAlert:values | responses.400 | open | — | 0 | 003 | — |
| v_f92c29 | error.code_pair_missing | getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE | responses.502 | open | — | 0 | 003 | — |
| v_fabd90 | error.code_undeclared | listAlerts:values | responses.503 | open | — | 0 | 003 | — |
| v_fc2823 | error.code_pair_missing | createAlert:IDEMPOTENCY_KEY_REUSED | responses.409 | open | — | 0 | 003 | — |
| v_02467d | response.type_changed | listAlerts:200 | AlertList.items[].slot.endTime | closed | code | 0 | 001 | 002 |
| v_035a89 | response.status_missing | createAlert:400 | responses | closed | code | 0 | 001 | 002 |
| v_047bda | response.required_weakened | cancelAlert:200 | Alert | closed | code | 0 | 001 | 002 |
| v_061225 | response.status_missing | getAlert:404 | responses | closed | code | 0 | 001 | 002 |
| v_06155f | response.required_weakened | cancelAlert:200 | Alert.delivery | closed | code | 0 | 001 | 002 |
| v_15798d | response.required_weakened | getAlert:200 | Alert.delivery | closed | code | 0 | 001 | 002 |
| v_20931f | response.type_changed | getCourtAvailability:200 | CourtAvailability.slots[].endTime | closed | code | 0 | 001 | 002 |
| v_213a44 | response.status_missing | listAlerts:500 | responses | closed | code | 0 | 001 | 002 |
| v_2bd4ca | response.type_changed | getAlert:200 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_2fd24a | response.required_weakened | createAlert:200 | Alert.slot | closed | code | 0 | 001 | 002 |
| v_310e16 | response.required_weakened | listAlerts:200 | AlertList.items[].slot | closed | code | 0 | 001 | 002 |
| v_36e659 | response.status_missing | getAlert:503 | responses | closed | code | 0 | 001 | 002 |
| v_40839c | response.required_weakened | cancelAlert:200 | Alert.slot | closed | code | 0 | 001 | 002 |
| v_42778f | extract.blind | — | x-error-codes | closed | code | 0 | 001 | 002 |
| v_43c02f | response.type_changed | cancelAlert:200 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |
| v_499bdd | response.status_missing | createAlert:422 | responses | closed | code | 0 | 001 | 002 |
| v_4fac05 | response.status_missing | getCourtAvailability:404 | responses | closed | code | 0 | 001 | 002 |
| v_52b438 | response.status_missing | createAlert:401 | responses | closed | code | 0 | 001 | 002 |
| v_552a8e | response.status_missing | getAlert:500 | responses | closed | code | 0 | 001 | 002 |
| v_57240f | response.status_missing | cancelAlert:401 | responses | closed | code | 0 | 001 | 002 |
| v_5b8320 | response.status_missing | createAlert:500 | responses | closed | code | 0 | 001 | 002 |
| v_5bdad4 | response.header_missing | createAlert:200 | headers.Location | closed | code | 0 | 001 | 002 |
| v_5c0d30 | response.status_missing | listAlerts:503 | responses | closed | code | 0 | 001 | 002 |
| v_662884 | response.required_weakened | listAlerts:200 | AlertList.items[].delivery | closed | code | 0 | 001 | 002 |
| v_6884fd | response.type_changed | getCourtAvailability:200 | CourtAvailability.slots[].startTime | closed | code | 0 | 001 | 002 |
| v_7709b5 | response.type_changed | createAlert:200 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_7acd13 | response.required_weakened | createAlert:200 | Alert | closed | code | 0 | 001 | 002 |
| v_7e4938 | response.status_missing | cancelAlert:500 | responses | closed | code | 0 | 001 | 002 |
| v_817689 | response.status_missing | getCourtAvailability:400 | responses | closed | code | 0 | 001 | 002 |
| v_81cde5 | response.status_missing | cancelAlert:400 | responses | closed | code | 0 | 001 | 002 |
| v_8298d6 | response.status_missing | cancelAlert:404 | responses | closed | code | 0 | 001 | 002 |
| v_851e03 | response.status_missing | listAlerts:400 | responses | closed | code | 0 | 001 | 002 |
| v_8ad7ff | response.status_missing | getCourtAvailability:502 | responses | closed | code | 0 | 001 | 002 |
| v_932149 | response.required_weakened | getAlert:200 | Alert.slot | closed | code | 0 | 001 | 002 |
| v_96b99a | response.status_missing | getCourtAvailability:503 | responses | closed | code | 0 | 001 | 002 |
| v_98b6a8 | response.required_weakened | listAlerts:200 | AlertList.items[] | closed | code | 0 | 001 | 002 |
| v_9b0716 | response.type_changed | getAlert:200 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |
| v_9c5b4a | response.status_missing | getAlert:401 | responses | closed | code | 0 | 001 | 002 |
| v_9c7d67 | response.header_missing | createAlert:200 | headers.Idempotency-Replayed | closed | code | 0 | 001 | 002 |
| v_a6ff3e | response.status_missing | createAlert:503 | responses | closed | code | 0 | 001 | 002 |
| v_aad0b3 | response.status_missing | createAlert:201 | responses | closed | code | 0 | 001 | 002 |
| v_ab307f | response.status_missing | getCourtAvailability:504 | responses | closed | code | 0 | 001 | 002 |
| v_bf1a9b | response.required_weakened | listAlerts:200 | AlertList | closed | code | 0 | 001 | 002 |
| v_bf20cf | response.type_changed | listAlerts:200 | AlertList.items[].slot.startTime | closed | code | 0 | 001 | 002 |
| v_bfc8b4 | response.required_weakened | getAlert:200 | Alert | closed | code | 0 | 001 | 002 |
| v_c9ec52 | response.status_missing | getAlert:400 | responses | closed | code | 0 | 001 | 002 |
| v_cf5ae1 | response.required_weakened | getCourtAvailability:200 | CourtAvailability | closed | code | 0 | 001 | 002 |
| v_cff1b6 | g0.extract_failed | — | dump_api_docs.py | closed | code | 0 | 002 | 003 |
| v_d44d72 | response.required_weakened | createAlert:200 | Alert.delivery | closed | code | 0 | 001 | 002 |
| v_da98ea | response.type_changed | cancelAlert:200 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_e374a3 | response.required_weakened | getCourtAvailability:200 | CourtAvailability.slots[] | closed | code | 0 | 001 | 002 |
| v_e85568 | response.status_missing | cancelAlert:503 | responses | closed | code | 0 | 001 | 002 |
| v_f31579 | response.status_missing | listAlerts:401 | responses | closed | code | 0 | 001 | 002 |
| v_f3725a | response.status_missing | getCourtAvailability:500 | responses | closed | code | 0 | 001 | 002 |
| v_f47ecb | response.status_missing | createAlert:409 | responses | closed | code | 0 | 001 | 002 |
| v_f5534b | extract.blind | — | security | closed | code | 0 | 001 | 002 |
| v_fe64f8 | response.type_changed | createAlert:200 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_06cbfe | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_0b825c | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_110d3a | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_18034e | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_1c4295 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_1ffe37 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_2a124a | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_2a2799 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_2bcbda | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_2d3007 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_43c7bd | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_46e972 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_4b5f89 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_574543 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_5e3c7a | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_60cb5d | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_66a77c | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_6763fb | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_7715ca | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_77fc7e | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_79c0a0 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_89e5ba | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_906a24 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_90ee2e | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_949568 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_95a13d | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_9800af | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_995678 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_9a795f | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_9e4ea2 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_a2b5ac | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_aea0bc | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_aee578 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_b4cd40 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_b520df | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_b9da18 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_bd8d05 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_be032b | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_c14d64 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_c5f1ef | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_c99204 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_da6fef | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_de167e | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_df80a5 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_dfb1ad | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_e02fd6 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_e18c23 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_e1a650 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_e40801 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_ed22a4 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_f011e9 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_f2e18b | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_f3d8b4 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_f7239c | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_f92c29 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_fabd90 | 구현이 계약의 선언 밖에 있는 에러 코드를 쓴다. 읽는 쪽이 분기할 근거가 계약에 없다. | — | — |
| v_fc2823 | 계약이 선언한 (상태 코드, 에러 코드) 쌍이 구현에 없다. 같은 코드가 상태에 따라 다른 자리를 뜻하므로 쌍으로 센다. | — | — |
| v_02467d | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_035a89 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_047bda | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java |
| v_061225 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_06155f | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java |
| v_15798d | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java |
| v_20931f | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_213a44 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_2bd4ca | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_2fd24a | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_310e16 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_36e659 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_40839c | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_42778f | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_43c02f | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_499bdd | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_4fac05 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_52b438 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_552a8e | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_57240f | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_5b8320 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_5bdad4 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_5c0d30 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_662884 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java |
| v_6884fd | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_7709b5 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_7acd13 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java |
| v_7e4938 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_817689 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_81cde5 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_8298d6 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_851e03 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_8ad7ff | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_932149 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_96b99a | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_98b6a8 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_9b0716 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_9c5b4a | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_9c7d67 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java |
| v_a6ff3e | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_aad0b3 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_ab307f | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_bf1a9b | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_bf20cf | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_bfc8b4 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java |
| v_c9ec52 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_cf5ae1 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_cff1b6 | 부팅이 실패했거나 제한 시간 안에 포트를 열지 못했다 원인: java.lang.NoSuchMethodException: com.thinking.tennis.app.AlertApplicationService.<init>()   ← org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.thinking.tennis.app.AlertApplicationService]: No default constructor found   ← org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'alertApplicationService' defined in AlertApplicationService.class: Failed to instantiate [com.thinking.tennis.app.AlertApplicationService]: No default constructor found org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'tennisAlertController' defined in TennisAlertController.class: Unsatisfied dependency expressed through constructor parameter 1: Error creating bean with name 'alertApplicationService' defined in AlertApplicationService.class: Failed to instantiate [com.thinking.tennis.app.AlertApplicationService]: No default constructor found 추출기: 애플리케이션이 포트를 열기 전에 종료했다. exit 1 | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_d44d72 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java |
| v_da98ea | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_e374a3 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_e85568 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_f31579 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_f3725a | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java |
| v_f47ecb | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_f5534b | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java |
| v_fe64f8 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertRequest.java, src/main/java/com/thinking/tennis/api/AlertResponse.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityResponse.java, src/main/java/com/thinking/tennis/api/RequestValidation.java, src/main/java/com/thinking/tennis/api/TennisAlertController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AlertExpiryJob.java, src/main/java/com/thinking/tennis/app/AvailabilityService.java, src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertDelivery.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |

`v_02467d`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_035a89`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_047bda`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_061225`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AlertResponse.java
@@ -2,30 +2,74 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
 import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertDelivery;
-import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.DeliveryStatus;
-
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
-import java.util.UUID;
-
-public record AlertResponse(
-        UUID alertId,
-        String courtId,
-        String courtName,
-        String reservationUrl,
-        LocalDate date,
-        TimeSlotResponse slot,
-        AlertStatus status,
-        Instant createdAt,
-        Instant expiresAt,
-        Instant lastCheckedAt,
-        boolean checkDelayed,
-        DeliveryResponse delivery
-) {
+import io.swagger.v3.oas.annotations.media.Schema;
+
+import java.time.ZoneOffset;
+import java.time.format.DateTimeFormatter;
+
+@Schema(name = "Alert", requiredProperties = {
+        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
+        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
+})
+public class AlertResponse {
+
... (223줄 더)
```

`v_06155f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_15798d`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_20931f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_213a44`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_2bd4ca`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_2fd24a`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_310e16`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_36e659`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AlertResponse.java
@@ -2,30 +2,74 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
 import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertDelivery;
-import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.DeliveryStatus;
-
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
-import java.util.UUID;
-
-public record AlertResponse(
-        UUID alertId,
-        String courtId,
-        String courtName,
-        String reservationUrl,
-        LocalDate date,
-        TimeSlotResponse slot,
-        AlertStatus status,
-        Instant createdAt,
-        Instant expiresAt,
-        Instant lastCheckedAt,
-        boolean checkDelayed,
-        DeliveryResponse delivery
-) {
+import io.swagger.v3.oas.annotations.media.Schema;
+
+import java.time.ZoneOffset;
+import java.time.format.DateTimeFormatter;
+
+@Schema(name = "Alert", requiredProperties = {
+        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
+        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
+})
+public class AlertResponse {
+
... (223줄 더)
```

`v_40839c`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_42778f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_43c02f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_499bdd`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_4fac05`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_52b438`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_552a8e`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AlertResponse.java
@@ -2,30 +2,74 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
 import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertDelivery;
-import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.DeliveryStatus;
-
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
-import java.util.UUID;
-
-public record AlertResponse(
-        UUID alertId,
-        String courtId,
-        String courtName,
-        String reservationUrl,
-        LocalDate date,
-        TimeSlotResponse slot,
-        AlertStatus status,
-        Instant createdAt,
-        Instant expiresAt,
-        Instant lastCheckedAt,
-        boolean checkDelayed,
-        DeliveryResponse delivery
-) {
+import io.swagger.v3.oas.annotations.media.Schema;
+
+import java.time.ZoneOffset;
+import java.time.format.DateTimeFormatter;
+
+@Schema(name = "Alert", requiredProperties = {
+        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
+        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
+})
+public class AlertResponse {
+
... (223줄 더)
```

`v_57240f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_5b8320`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_5bdad4`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java
+++ b/src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java
@@ -8,4 +8,2 @@
 import org.springframework.http.ResponseEntity;
-import org.springframework.http.converter.HttpMessageNotReadableException;
-import org.springframework.web.bind.MissingServletRequestParameterException;
 import org.springframework.web.bind.annotation.ExceptionHandler;
@@ -13,2 +11,3 @@
 
+import java.util.Locale;
 import java.util.UUID;
@@ -17,2 +16,7 @@
 public class ApiExceptionHandler {
+
+    private static final int DEFAULT_INTERNAL_RETRY_AFTER_SECONDS = 5;
+    private static final int STORAGE_RETRY_AFTER_SECONDS = 1;
+    private static final int UPSTREAM_RETRY_AFTER_SECONDS = 20;
+
     @ExceptionHandler(UseCaseException.class)
@@ -20,23 +24,16 @@
             UseCaseException exception,
-            HttpServletRequest request
-    ) {
+            HttpServletRequest request) {
         HttpStatus status = statusFor(exception.code(), request);
-        return problem(exception, status, request);
+        Integer retryAfterSeconds = retryAfterSeconds(exception, status);
+        ProblemResponse body = body(exception.code(), exception.getMessage(), status, request, retryAfterSeconds);
+        return response(status, retryAfterSeconds, body);
     }
 
-    @ExceptionHandler({
-            HttpMessageNotReadableException.class,
-            MissingServletRequestParameterException.class
-    })
-    public ResponseEntity<ProblemResponse> handleMalformedRequest(
-            Exception exception,
-            HttpServletRequest request
-    ) {
-        UseCaseException useCaseException = new UseCaseException(
... (144줄 더)
```

`v_5c0d30`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_662884`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AlertResponse.java
@@ -2,30 +2,74 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
 import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertDelivery;
-import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.DeliveryStatus;
-
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
-import java.util.UUID;
-
-public record AlertResponse(
-        UUID alertId,
-        String courtId,
-        String courtName,
-        String reservationUrl,
-        LocalDate date,
-        TimeSlotResponse slot,
-        AlertStatus status,
-        Instant createdAt,
-        Instant expiresAt,
-        Instant lastCheckedAt,
-        boolean checkDelayed,
-        DeliveryResponse delivery
-) {
+import io.swagger.v3.oas.annotations.media.Schema;
+
+import java.time.ZoneOffset;
+import java.time.format.DateTimeFormatter;
+
+@Schema(name = "Alert", requiredProperties = {
+        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
+        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
+})
+public class AlertResponse {
+
... (223줄 더)
```

`v_6884fd`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_7709b5`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_7acd13`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_7e4938`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_817689`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_81cde5`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_8298d6`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_851e03`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_8ad7ff`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_932149`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_96b99a`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_98b6a8`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_9b0716`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_9c5b4a`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AlertResponse.java
@@ -2,30 +2,74 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
 import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertDelivery;
-import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.DeliveryStatus;
-
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
-import java.util.UUID;
-
-public record AlertResponse(
-        UUID alertId,
-        String courtId,
-        String courtName,
-        String reservationUrl,
-        LocalDate date,
-        TimeSlotResponse slot,
-        AlertStatus status,
-        Instant createdAt,
-        Instant expiresAt,
-        Instant lastCheckedAt,
-        boolean checkDelayed,
-        DeliveryResponse delivery
-) {
+import io.swagger.v3.oas.annotations.media.Schema;
+
+import java.time.ZoneOffset;
+import java.time.format.DateTimeFormatter;
+
+@Schema(name = "Alert", requiredProperties = {
+        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
+        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
+})
+public class AlertResponse {
+
... (223줄 더)
```

`v_9c7d67`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java
+++ b/src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java
@@ -8,4 +8,2 @@
 import org.springframework.http.ResponseEntity;
-import org.springframework.http.converter.HttpMessageNotReadableException;
-import org.springframework.web.bind.MissingServletRequestParameterException;
 import org.springframework.web.bind.annotation.ExceptionHandler;
@@ -13,2 +11,3 @@
 
+import java.util.Locale;
 import java.util.UUID;
@@ -17,2 +16,7 @@
 public class ApiExceptionHandler {
+
+    private static final int DEFAULT_INTERNAL_RETRY_AFTER_SECONDS = 5;
+    private static final int STORAGE_RETRY_AFTER_SECONDS = 1;
+    private static final int UPSTREAM_RETRY_AFTER_SECONDS = 20;
+
     @ExceptionHandler(UseCaseException.class)
@@ -20,23 +24,16 @@
             UseCaseException exception,
-            HttpServletRequest request
-    ) {
+            HttpServletRequest request) {
         HttpStatus status = statusFor(exception.code(), request);
-        return problem(exception, status, request);
+        Integer retryAfterSeconds = retryAfterSeconds(exception, status);
+        ProblemResponse body = body(exception.code(), exception.getMessage(), status, request, retryAfterSeconds);
+        return response(status, retryAfterSeconds, body);
     }
 
-    @ExceptionHandler({
-            HttpMessageNotReadableException.class,
-            MissingServletRequestParameterException.class
-    })
-    public ResponseEntity<ProblemResponse> handleMalformedRequest(
-            Exception exception,
-            HttpServletRequest request
-    ) {
-        UseCaseException useCaseException = new UseCaseException(
... (144줄 더)
```

`v_a6ff3e`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_aad0b3`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_ab307f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_bf1a9b`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_bf20cf`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_bfc8b4`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_c9ec52`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AlertResponse.java
@@ -2,30 +2,74 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
 import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertDelivery;
-import com.thinking.tennis.domain.AlertStatus;
 import com.thinking.tennis.domain.DeliveryStatus;
-
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
-import java.util.UUID;
-
-public record AlertResponse(
-        UUID alertId,
-        String courtId,
-        String courtName,
-        String reservationUrl,
-        LocalDate date,
-        TimeSlotResponse slot,
-        AlertStatus status,
-        Instant createdAt,
-        Instant expiresAt,
-        Instant lastCheckedAt,
-        boolean checkDelayed,
-        DeliveryResponse delivery
-) {
+import io.swagger.v3.oas.annotations.media.Schema;
+
+import java.time.ZoneOffset;
+import java.time.format.DateTimeFormatter;
+
+@Schema(name = "Alert", requiredProperties = {
+        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
+        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
+})
+public class AlertResponse {
+
... (223줄 더)
```

`v_cf5ae1`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AvailabilityResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AvailabilityResponse.java
@@ -2,49 +2,131 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
-import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
 import com.thinking.tennis.port.CourtAvailabilityPort;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
+import java.time.format.DateTimeFormatter;
 import java.util.List;
 
-public record AvailabilityResponse(
-        String courtId,
-        String courtName,
-        LocalDate date,
-        Instant confirmedAt,
-        boolean stale,
-        String staleReason,
-        String reservationUrl,
-        List<AvailabilitySlotResponse> slots
-) {
-    public static AvailabilityResponse from(AvailabilityService.ReadResult result) {
-        AvailabilitySnapshot snapshot = result.snapshot();
-        return new AvailabilityResponse(
-                snapshot.courtId(),
-                snapshot.courtName(),
-                snapshot.date(),
-                snapshot.confirmedAt(),
-                result.stale(),
-                result.staleReason() == null ? null : result.staleReason().name(),
-                snapshot.reservationUrl(),
-                snapshot.slots().stream().map(AvailabilitySlotResponse::from).toList()
-        );
+@Schema(name = "CourtAvailability", requiredProperties = {
+        "courtId", "courtName", "date", "confirmedAt", "stale",
... (133줄 더)
```

`v_d44d72`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_da98ea`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

`v_e374a3`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AvailabilityResponse.java
+++ b/src/main/java/com/thinking/tennis/api/AvailabilityResponse.java
@@ -2,49 +2,131 @@
 
-import com.fasterxml.jackson.annotation.JsonFormat;
-import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.domain.AvailabilitySnapshot;
 import com.thinking.tennis.port.CourtAvailabilityPort;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-import java.time.Instant;
-import java.time.LocalDate;
-import java.time.LocalTime;
+import java.time.format.DateTimeFormatter;
 import java.util.List;
 
-public record AvailabilityResponse(
-        String courtId,
-        String courtName,
-        LocalDate date,
-        Instant confirmedAt,
-        boolean stale,
-        String staleReason,
-        String reservationUrl,
-        List<AvailabilitySlotResponse> slots
-) {
-    public static AvailabilityResponse from(AvailabilityService.ReadResult result) {
-        AvailabilitySnapshot snapshot = result.snapshot();
-        return new AvailabilityResponse(
-                snapshot.courtId(),
-                snapshot.courtName(),
-                snapshot.date(),
-                snapshot.confirmedAt(),
-                result.stale(),
-                result.staleReason() == null ? null : result.staleReason().name(),
-                snapshot.reservationUrl(),
-                snapshot.slots().stream().map(AvailabilitySlotResponse::from).toList()
-        );
+@Schema(name = "CourtAvailability", requiredProperties = {
+        "courtId", "courtName", "date", "confirmedAt", "stale",
... (133줄 더)
```

`v_e85568`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_f31579`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_f3725a`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_f47ecb`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_f5534b`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/TennisAlertController.java
+++ b/src/main/java/com/thinking/tennis/api/TennisAlertController.java
@@ -3,7 +3,22 @@
 import com.thinking.tennis.app.AlertApplicationService;
+import com.thinking.tennis.app.AvailabilityService;
 import com.thinking.tennis.app.UseCaseException;
+import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
+import com.thinking.tennis.domain.AvailabilitySnapshot;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.Parameter;
+import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
+import io.swagger.v3.oas.annotations.media.ArraySchema;
+import io.swagger.v3.oas.annotations.media.Content;
+import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.responses.ApiResponse;
+import io.swagger.v3.oas.annotations.security.SecurityRequirement;
+import io.swagger.v3.oas.annotations.security.SecurityScheme;
+import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
-import org.springframework.http.MediaType;
 import org.springframework.http.ResponseEntity;
@@ -20,2 +35,3 @@
 import java.time.LocalDate;
+import java.time.LocalTime;
 import java.util.List;
@@ -24,95 +40,278 @@
 @RestController
-@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
+@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
 public class TennisAlertController {
-    private final AlertApplicationService service;
-
-    public TennisAlertController(AlertApplicationService service) {
-        this.service = service;
-    }
... (392줄 더)
```

`v_fe64f8`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertRequest.java
+++ b/src/main/java/com/thinking/tennis/api/AlertRequest.java
@@ -2,15 +2,67 @@
 
-import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
+import io.swagger.v3.oas.annotations.media.Schema;
 
-@JsonIgnoreProperties(ignoreUnknown = false)
-public record AlertRequest(
-        String courtId,
-        String date,
-        TimeSlotRequest slot
-) {
-    @JsonIgnoreProperties(ignoreUnknown = false)
-    public record TimeSlotRequest(
-            String startTime,
-            String endTime
-    ) {
+@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
+public class AlertRequest {
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
+            pattern = "^[a-z0-9][a-z0-9-]*$")
+    private String courtId;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
+    private String date;
+
+    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
+    private TimeSlotRequest slot;
+
+    public String getCourtId() {
+        return courtId;
+    }
+
+    public void setCourtId(String courtId) {
+        this.courtId = courtId;
+    }
+
+    public String getDate() {
... (42줄 더)
```

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/BearerUser.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/ProblemResponse.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/SchedulingConfig.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/UseCaseException.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 50건이다.

### 충족 주장과 게이트 판정 (39건)

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
| 002 | getCourtAvailability:400..504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:200/201 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:400..504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:200, cancelAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | state_continuity:expiry | G1이 미판정이라 이 주장을 대조할 수 없다 |

### 축 판정의 어긋남 (3건)

Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다.

| iteration | axis | critique_id | eval_score | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | response_fidelity | w_response_time_projection | 5 | Critique는 이 축을 high로 지적했는데 Eval은 5를 줬다 |
| 001 | failure_faithfulness | w_create_upstream_error_mapping | 5 | Critique는 이 축을 high로 지적했는데 Eval은 5를 줬다 |
| 001 | failure_faithfulness | w_bearer_identity_without_validation | 5 | Critique는 이 축을 high로 지적했는데 Eval은 5를 줬다 |

### 게이트가 세는 것을 다시 말한 지적 (8건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다.

| iteration | axis | critique_id | rule | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | response_fidelity | w_response_time_projection | response.type_changed | 게이트가 response.type_changed로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | state_continuity | w_expiry_transition_missing | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | failure_faithfulness | w_create_upstream_error_mapping | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | failure_faithfulness | w_bearer_identity_without_validation | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 003 | request_tolerance | w_bearer_value_narrowing | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 003 | failure_faithfulness | w_create_upstream_surface | error.code_pair_missing | 게이트가 error.code_pair_missing로 이미 세는 자리를 비평이 다시 말했다 |
| 003 | state_continuity | w_expiry_scheduler_window | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 003 | judgement_disclosure | w_idempotency_order_undisclosed | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |

### 게이트가 남긴 기록 (65건)

계약이 말하지 않은 자리의 초과다. REJECT 사유가 아니고 뜻은 Critique가 붙인다.

| 규칙 | 판정 지점 | 좌표 | iteration | 관찰 |
| --- | --- | --- | --- | --- |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].startTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].startTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].startTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].startTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].endTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].endTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].endTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getCourtAvailability:200 | CourtAvailability.slots[].endTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:request.field_extra | listAlerts | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.startTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.startTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.startTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.startTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.endTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.endTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.endTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | listAlerts:200 | AlertList.items[].slot.endTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:request.field_extra | createAlert | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.startTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.startTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.startTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.startTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.endTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.endTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.endTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:200 | Alert.slot.endTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:request.field_extra | getAlert | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.startTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.startTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.startTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.startTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.endTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.endTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.endTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | getAlert:200 | Alert.slot.endTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:request.field_extra | cancelAlert | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.startTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.startTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.startTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.startTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.endTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.endTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.endTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | cancelAlert:200 | Alert.slot.endTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:decision.unknown_point | 인증 토큰의 사용자 식별 | src/main/java/com/thinking/tennis/api/BearerUser.java | 001 | 계약의 판정 지점이 아니다 (결정 d_bearer_subject_mapping) |
| gate:decision.unknown_point | 서버 시각 공급 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 001 | 계약의 판정 지점이 아니다 (결정 d_system_utc_clock) |
| gate:decision.unknown_point | expiresAt 계산 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 001 | 계약의 판정 지점이 아니다 (결정 d_expiry_boundary) |
| gate:decision.unknown_point | 신청 중 시간대 확인 실패 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 001 | 계약의 판정 지점이 아니다 (결정 d_create_slot_check_failure) |
| gate:decision.unknown_point | 문제 응답의 type/title/traceId 값 | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 001 | 계약의 판정 지점이 아니다 (결정 d_problem_representation) |
| gate:decision.unknown_point | 24시간 멱등 기록 정리 | src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java | 001 | 계약의 판정 지점이 아니다 (결정 d_idempotency_cleanup) |
| gate:decision.unknown_point | 만료 상태의 HTTP 투영 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 001 | 계약의 판정 지점이 아니다 (결정 d_stored_expiry_projection) |
| gate:request.field_extra | listAlerts | parameters.header.Authorization | 003 | 계약에 없는 파라미터다 |
| gate:request.field_extra | createAlert | parameters.header.Authorization | 003 | 계약에 없는 파라미터다 |
| gate:surface.status_extra | createAlert:502 | responses | 003 | 계약이 선언하지 않은 상태 코드다 |
| gate:surface.status_extra | createAlert:504 | responses | 003 | 계약이 선언하지 않은 상태 코드다 |
| gate:request.field_extra | getAlert | parameters.header.Authorization | 003 | 계약에 없는 파라미터다 |
| gate:request.field_extra | cancelAlert | parameters.header.Authorization | 003 | 계약에 없는 파라미터다 |
| gate:decision.unknown_point | 인증 토큰의 사용자 식별 | src/main/java/com/thinking/tennis/api/BearerUser.java | 003 | 계약의 판정 지점이 아니다 (결정 d_bearer_subject_boundary) |
| gate:decision.unknown_point | 서버 시각 공급 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 003 | 계약의 판정 지점이 아니다 (결정 d_system_utc_clock) |
| gate:decision.unknown_point | expiresAt 계산 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 003 | 계약의 판정 지점이 아니다 (결정 d_expiry_boundary) |
| gate:decision.unknown_point | 신청 중 시간대 확인 실패 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java; src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 003 | 계약의 판정 지점이 아니다 (결정 d_create_upstream_failure_surface) |
| gate:decision.unknown_point | 문제 응답의 type/title/traceId 값 | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 003 | 계약의 판정 지점이 아니다 (결정 d_problem_representation) |
| gate:decision.unknown_point | 24시간 멱등 기록 정리 | src/main/java/com/thinking/tennis/app/InMemoryAlertStore.java | 003 | 계약의 판정 지점이 아니다 (결정 d_idempotency_cleanup) |
| gate:decision.unknown_point | 만료 상태의 HTTP 투영 | src/main/java/com/thinking/tennis/app/AlertApplicationService.java | 003 | 계약의 판정 지점이 아니다 (결정 d_stored_expiry_projection) |
| gate:decision.unknown_point | 서버 소유 만료 전이 실행 | src/main/java/com/thinking/tennis/app/AlertExpiryJob.java | 003 | 계약의 판정 지점이 아니다 (결정 d_expiry_scheduled_transition) |
