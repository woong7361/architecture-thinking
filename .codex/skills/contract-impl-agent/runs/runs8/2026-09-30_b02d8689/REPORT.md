# 계약 적합성 run 리포트 — 2026-09-30_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T09:02:12+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 002까지 왔다. 위반 32건 가운데 31건을 코드로 닫고 0건을 계약으로 닫았으며 1건이 아직 열려 있다. 서 있는 결정은 8건, 계약 변경 기록은 8건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v2.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **high** 수준이다. 근거는 low confidence decisions: ['d_expiry_source_semantics']다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:g0.compile_failed@None: 컴파일 오류 2건
src/main/java/com/thinking/tennis/api/AlertController.java:233:86 illegal start of type
src/main/java/com/thinking/tennis/api/AlertController.java:233:87 reached end of file while parsing

판정되지 않은 검사가 있다: changes, g1. 건너뛴 단계의 이유는 g0_reject다. 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | PASS | REJECT | PASS | 31 | 65 | 2.6 |
| 002 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |

## 2. 계약을 이렇게 고쳤다

계약이 어느 좌표에서 무엇 때문에 움직였는지 적는다. 깨는 변경이 위로 온다.

변경은 8건이고 그중 0건이 대조기 판정으로 깨는 변경이다. 호환성은 모델의 라벨이 아니라 대조기가 정하므로 두 값을 나란히 싣는다.

| id | 좌표 | 무엇 | 왜 | 발의 | 명세 근거 | 라벨 | 대조기 | iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| cc_restore_alert_state_invariants | /components/schemas/Alert/description | status와 delivery의 허용 조합, PENDING 취소, terminal 상태의 비가역성을 복원했다. | 클라이언트가 상태 조합을 해석할 수 있는 핵심 불변식이 사라져 있었다. | critique:cr_restore_alert_state_invariants | slot_cec09639 | compatible | compatible | 002 |
| cc_restore_availability_behavior | /paths/~1courts~1{courtId}~1availability/get/description | 20초 갱신, 코트·날짜별 단일 조회, 진행 중 요청의 동일 결과, stale 200, 인증 예외를 복원했다. | 동시 갱신과 stale 실패 의미가 계약에서 보이지 않았다. | critique:cr_restore_availability_behavior | slot_218b9620 | compatible | compatible | 002 |
| cc_restore_create_idempotency_behavior | /paths/~1alerts/post/description | 4xx 미기록, 2xx 재생, 진행 중 동일 키의 503, 재생 시점 고정, 동일 조건 중복 규칙을 복원했다. | 클라이언트 재시도와 중복 신청의 의미를 계약에 고정해야 한다. | critique:cr_restore_create_idempotency_behavior | slot_7afbe3b4 | compatible | compatible | 002 |
| cc_restore_error_code_meaning | /components/schemas/ErrorCode/description | 에러 코드가 클라이언트 대응이 갈리는 지점에만 존재한다는 원칙과 코드 집합의 의미를 복원했다. | 오류 코드 선택과 재시도 해석의 기준이 축약 판본에서 사라져 있었다. | critique:cr_restore_error_code_meaning | slot_58100c30 | compatible | compatible | 002 |
| cc_restore_expiry_semantics | /components/schemas/Alert/properties/expiresAt/description | 이용 시작 시각과 더 이른 예약 마감 시각의 최소값, 재조회 필요성, 전이 지연 중 표시 규칙을 복원했다. | expiresAt의 실제 의미가 축약 판본에서 사라져 있었다. | critique:cr_restore_expiry_semantics | slot_c14121c8 | compatible | compatible | 002 |
| cc_restore_list_state_filtering | /paths/~1alerts/get/description | 저장 상태 기준 필터, 기본 조회 범위, 정렬과 비페이지 전제를 복원했다. | 목록 응답의 범위와 필터 semantics가 축약되어 있었다. | critique:cr_restore_list_state_filtering | slot_85c85829 | compatible | compatible | 002 |
| cc_restore_problem_conditionals | /components/schemas/Problem/description | retryable과 retryAfterSeconds·Retry-After의 동반 규칙 및 5xx traceId 조건을 복원했다. | 재시도 메타데이터의 조건부 제공이 계약에 드러나야 한다. | critique:cr_restore_problem_conditionals | slot_47c11736 | compatible | compatible | 002 |
| cc_restore_scope_time_compatibility | /info/description | 범위 밖 항목, Asia/Seoul 날짜·시각 규칙, /v1 호환성 규칙을 복원했다. | HTTP 계약의 전제와 호환성 판단 기준이 축약 판본에서 사라져 있었다. | critique:cr_restore_scope_time_compatibility | slot_fd7db9b0 | compatible | compatible | 002 |

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 112곳이 달라지고 깨는 변경 0건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 112곳이 달라지고 깨는 변경 0건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 대조하지 못한 자리 | 사라진 판정 지점 |
| --- | --- | --- | --- | --- |
| 앞 판본 | 60 | 0 | 0 | — |
| 이 run의 기준선 | 112 | 0 | 0 | — |
| 사람이 확정한 원본 | 112 | 0 | 0 | — |

기준선 대비 달라진 좌표는 이렇다.

- `/info/description`
- `/info/x-out-of-scope/0/reason`
- `/tags/0/description`
- `/tags/1/description`
- `/paths/~1courts~1{courtId}~1availability/get/summary`
- `/paths/~1courts~1{courtId}~1availability/get/description`
- `/paths/~1courts~1{courtId}~1availability/get/parameters/1/description`
- `/paths/~1courts~1{courtId}~1availability/get/parameters/1/example`
- `/paths/~1courts~1{courtId}~1availability/get/responses/200/description`
- `/paths/~1courts~1{courtId}~1availability/get/responses/200/content/application~1json/examples`
- `/paths/~1alerts/post/summary`
- `/paths/~1alerts/post/description`
- `/paths/~1alerts/post/requestBody/content/application~1json/example`
- `/paths/~1alerts/post/responses/201/description`
- `/paths/~1alerts/post/responses/201/content/application~1json/example`
- `/paths/~1alerts/post/responses/200/description`
- `/paths/~1alerts/post/responses/200/content/application~1json/example`
- `/paths/~1alerts/get/summary`
- `/paths/~1alerts/get/description`
- `/paths/~1alerts/get/parameters/0/description`
- `/paths/~1alerts/get/responses/200/description`
- `/paths/~1alerts/get/responses/200/content/application~1json/example`
- `/paths/~1alerts~1{alertId}/get/summary`
- `/paths/~1alerts~1{alertId}/get/description`
- `/paths/~1alerts~1{alertId}/get/responses/200/description`
- `/paths/~1alerts~1{alertId}/get/responses/200/content/application~1json/example`
- `/paths/~1alerts~1{alertId}/delete/summary`
- `/paths/~1alerts~1{alertId}/delete/description`
- `/paths/~1alerts~1{alertId}/delete/responses/200/description`
- `/paths/~1alerts~1{alertId}/delete/responses/200/content/application~1json/example`
- 그 밖에 82곳

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 9건이고 그중 1건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_expiry_source_semantics | 더 이른 예약 마감 시각을 제공된 API가 노출하지 않을 때 expiresAt을 어떻게 계산할 것인가 | 제공된 포트가 보장하는 이용 시작 시각을 사용하고 더 이른 외부 마감은 해당 포트가 확장될 때 반영한다 | least_harm | low | 포트에 예약 마감 시각을 추가할 수 있게 된 뒤 최소값 계산과 상태 전이를 함께 변경해야 한다. | createAlert:201, createAlert:422, Alert.expiresAt, AlertStatus:EXPIRED | standing |
| d_trace_id_format | traceId의 형식이 계약에 없을 때 어떤 식별자를 생성할 것인가 | UUID 문자열을 생성한다 | convention | medium | Problem 응답 생성기의 traceId 생성부와 로그 상관관계 형식을 함께 변경해야 한다. | getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, createAlert:503, listAlerts:500, getAlert:500, cancelAlert:500 | standing |
| d_user_identity | 토큰 검증 방식이 계약에 정의되지 않은 상태에서 Bearer 토큰을 어떻게 사용자 식별자로 사용할 것인가 | 비어 있지 않은 Bearer 토큰의 값 자체를 사용자 식별자로 사용한다 | convention | medium | ApiMapper.userId를 인증 포트 호출로 교체하고 사용자 식별자 전달 경계를 갱신해야 한다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, listAlerts:200, getAlert:404, cancelAlert:404 | standing |
| d_expiry_scheduler_cadence | 만료를 서버 작업으로 수행해야 하지만 실행 주기가 계약에 없을 때 어떻게 실행할 것인가 | 1초 고정 지연의 인메모리 만료 작업을 사용한다 | least_harm | medium | 스케줄러 주기 또는 외부 작업 트리거를 교체하고 인메모리 상태 전이 동기화를 다시 검토해야 한다. | listAlerts:200, getAlert:200, cancelAlert:200, createAlert:200, createAlert:201 | standing |
| d_refresh_wait_deadline | 동일 코트·날짜의 진행 중 확인을 기다리는 최대 시간이 계약에 없을 때 얼마를 기다릴 것인가 | 20초 동안 진행 중 확인을 기다린다 | contract_analogy | medium | AvailabilityApplicationService의 대기 제한과 동시 확인 실패 매핑을 함께 변경해야 한다. | getCourtAvailability:200, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504 | superseded |
| d_delivery_surface | 메시지 발송 경로가 out_of_scope일 때 HTTP 조회 표면에서 발송 결과를 어디까지 만들 것인가 | 빈자리 발견 시 PENDING까지만 만들고 전송 결과는 생성하지 않는다 | least_harm | high | AsyncAPI 발송 어댑터와 상태 전이 작업을 추가하고 Alert의 SENT/FAILED 전이를 연결해야 한다. | createAlert:201, createAlert:200, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_inmemory_alert_store | 저장소 장애 주입이 범위 밖이고 구현 계약이 인메모리 저장을 요구할 때 알림 상태를 어디에 둘 것인가 | ConcurrentHashMap 기반 인메모리 저장을 사용한다 | spec_implication | high | 저장소 포트와 원자적 조건 키 연산을 추가하고 멱등성 보관을 이식해야 한다. | createAlert, listAlerts, getAlert, cancelAlert, AlertStatus | standing |
| d_create_slot_validation | 본문의 시간대 지원 여부를 제공된 포트로 어떻게 판정할 것인가 | 제공된 availability 포트의 코트·날짜 결과에서 시간대가 정확히 일치하는지 확인한다 | spec_implication | high | AlertApplicationService의 신청 검증을 별도 지원 코트 카탈로그 포트로 옮기고 제공 API 의존을 재조정해야 한다. | createAlert:200, createAlert:201, createAlert:422, createAlert:500 | standing |
| d_refresh_shared_completion | 동일 코트·날짜의 진행 중 확인을 기다리는 요청이 서로 다른 결과를 받지 않게 하려면 어떻게 완료를 공유할 것인가 | 리더 Future의 단일 완료 결과를 모든 대기 요청이 기다리게 하고 별도 대기자 타임아웃을 두지 않는다 | spec_implication | high | 리더 완료 상태와 모든 대기자의 실패 전파를 다시 설계해야 한다. | getCourtAvailability:200, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504 | standing |

`d_expiry_source_semantics` — immutable 제공 API에는 예약 마감 시각 필드가 없다. 임의의 마감 규칙을 발명하지 않고 현재 입력으로 증명 가능한 경계만 사용한다.

`d_trace_id_format` — 계약은 traceId를 로그 검색용 문자열로만 정의하고 형식을 제한하지 않는다. Java 표준 UUID가 가장 좁은 구현 선택이다.

`d_user_identity` — 계약은 Bearer를 사용한다고만 정하고 검증 방식이나 제공 API를 정하지 않았다. 인메모리 초안에서 소유권 구분을 유지하는 최소 선택이다.

`d_expiry_scheduler_cadence` — 계약은 만료가 서버 스스로 일어나야 한다고 정하지만 주기를 정하지 않았다. 저장소와 컨테이너가 금지된 초안에서 최소한의 독립 실행 방식이다.

`d_refresh_wait_deadline` — 확인 간격과 재시도 대기 값이 계약에서 20초로 고정되어 있어 같은 시간 창을 공유하는 선택이다.

`d_delivery_surface` — 알림 발송 경로는 명시적으로 out_of_scope이고 제공된 발송 포트도 없다. 계약이 허용한 null/PENDING 상태만 사용한다.

`d_inmemory_alert_store` — 구현 계약이 저장은 인메모리라고 정하고 저장소 장애 주입도 out_of_scope으로 명시한다.

`d_create_slot_validation` — 계약은 여기에 없는 시간대로 신청하면 SLOT_NOT_SUPPORTED라고 정하고, 제공 API에서 시간대 목록을 얻을 수 있는 유일한 경로가 availability 확인이다.

`d_refresh_shared_completion` — 계약이 동일 코트·날짜의 진행 중 요청 모두에 같은 결과를 요구하므로, 이전의 독립적인 20초 대기 제한 결정을 폐기했다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 32건 중 31건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_579dfe | g0.compile_failed | — | mvnw test | open | — | 0 | 002 | — |
| v_02467d | response.type_changed | listAlerts:200 | AlertList.items[].slot.endTime | closed | code | 0 | 001 | 002 |
| v_08da55 | response.type_changed | createAlert:201 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |
| v_1757b8 | response.header_missing | listAlerts:503 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_20931f | response.type_changed | getCourtAvailability:200 | CourtAvailability.slots[].endTime | closed | code | 0 | 001 | 002 |
| v_228ec7 | request.type_narrowed | createAlert | AlertRequest.slot.endTime | closed | code | 0 | 001 | 002 |
| v_2bd4ca | response.type_changed | getAlert:200 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_2cecbf | response.header_missing | getCourtAvailability:504 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_336018 | response.header_missing | getAlert:503 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_3b2c4c | response.header_missing | createAlert:503 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_3f44cf | response.header_missing | createAlert:201 | headers.Location | closed | code | 0 | 001 | 002 |
| v_42778f | extract.blind | — | x-error-codes | closed | code | 0 | 001 | 002 |
| v_43c02f | response.type_changed | cancelAlert:200 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |
| v_4fcb76 | response.header_missing | cancelAlert:503 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_5bdad4 | response.header_missing | createAlert:200 | headers.Location | closed | code | 0 | 001 | 002 |
| v_66c368 | response.header_missing | getCourtAvailability:500 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_6884fd | response.type_changed | getCourtAvailability:200 | CourtAvailability.slots[].startTime | closed | code | 0 | 001 | 002 |
| v_7709b5 | response.type_changed | createAlert:200 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_7de05d | response.header_missing | listAlerts:500 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_9800b4 | response.header_missing | getAlert:500 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_9b0716 | response.type_changed | getAlert:200 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |
| v_9c7d67 | response.header_missing | createAlert:200 | headers.Idempotency-Replayed | closed | code | 0 | 001 | 002 |
| v_ac43d1 | response.header_missing | getCourtAvailability:502 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_bada47 | response.type_changed | createAlert:201 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_bbde5e | response.header_missing | cancelAlert:500 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_bd1a4d | response.header_missing | createAlert:500 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_bf20cf | response.type_changed | listAlerts:200 | AlertList.items[].slot.startTime | closed | code | 0 | 001 | 002 |
| v_da98ea | response.type_changed | cancelAlert:200 | Alert.slot.startTime | closed | code | 0 | 001 | 002 |
| v_f677a5 | response.header_missing | getCourtAvailability:503 | headers.Retry-After | closed | code | 0 | 001 | 002 |
| v_f7441a | request.type_narrowed | createAlert | AlertRequest.slot.startTime | closed | code | 0 | 001 | 002 |
| v_faff5b | response.header_missing | createAlert:201 | headers.Idempotency-Replayed | closed | code | 0 | 001 | 002 |
| v_fe64f8 | response.type_changed | createAlert:200 | Alert.slot.endTime | closed | code | 0 | 001 | 002 |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_579dfe | 컴파일 오류 2건 src/main/java/com/thinking/tennis/api/AlertController.java:233:86 illegal start of type src/main/java/com/thinking/tennis/api/AlertController.java:233:87 reached end of file while parsing | — | — |
| v_02467d | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_08da55 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_1757b8 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_20931f | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_228ec7 | 구현이 받는 타입이 계약이 받겠다고 한 타입보다 좁다. 계약상 적법한 값이 거절된다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_2bd4ca | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_2cecbf | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_336018 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_3b2c4c | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_3f44cf | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_42778f | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java |
| v_43c02f | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_4fcb76 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_5bdad4 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_66c368 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_6884fd | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_7709b5 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_7de05d | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_9800b4 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_9b0716 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_9c7d67 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_ac43d1 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_bada47 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_bbde5e | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_bd1a4d | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_bf20cf | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_da98ea | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_f677a5 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_f7441a | 구현이 받는 타입이 계약이 받겠다고 한 타입보다 좁다. 계약상 적법한 값이 거절된다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |
| v_faff5b | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/io/swagger/v3/oas/annotations/headers/Header.java |
| v_fe64f8 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | src/main/java/com/thinking/tennis/api/AlertController.java, src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java, src/main/java/com/thinking/tennis/api/ApiMapper.java, src/main/java/com/thinking/tennis/api/ApiModels.java, src/main/java/com/thinking/tennis/api/AvailabilityController.java, src/main/java/com/thinking/tennis/api/OpenApiContractConfiguration.java, src/main/java/com/thinking/tennis/app/AlertApplicationService.java, src/main/java/com/thinking/tennis/app/ApiFailure.java, src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java, src/main/java/com/thinking/tennis/domain/Alert.java, src/main/java/com/thinking/tennis/domain/AlertSnapshot.java, src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java |

`v_02467d`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_08da55`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_1757b8`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_20931f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_228ec7`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_2bd4ca`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_2cecbf`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_336018`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_3b2c4c`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_3f44cf`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_42778f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_43c02f`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_4fcb76`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_5bdad4`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_66c368`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_6884fd`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_7709b5`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_7de05d`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_9800b4`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_9b0716`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_9c7d67`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_ac43d1`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_bada47`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_bbde5e`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_bd1a4d`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_bf20cf`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_da98ea`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_f677a5`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_f7441a`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_faff5b`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

`v_fe64f8`을 닫을 때 작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/AlertController.java
+++ b/src/main/java/com/thinking/tennis/api/AlertController.java
@@ -5,8 +5,7 @@
 import com.thinking.tennis.app.ValidationFailure;
-import com.thinking.tennis.domain.Alert;
 import com.thinking.tennis.domain.AlertStatus;
 import io.swagger.v3.oas.annotations.Operation;
-import io.swagger.v3.oas.annotations.Parameter;
-import io.swagger.v3.oas.annotations.media.Content;
-import io.swagger.v3.oas.annotations.media.Schema;
+import io.swagger.v3.oas.annotations.extensions.Extension;
+import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
+import io.swagger.v3.oas.annotations.headers.Header;
 import io.swagger.v3.oas.annotations.responses.ApiResponse;
@@ -14,4 +13,2 @@
 import io.swagger.v3.oas.annotations.security.SecurityRequirement;
-import io.swagger.v3.oas.annotations.tags.Tag;
-import java.util.UUID;
 import org.springframework.http.HttpHeaders;
@@ -28,2 +25,5 @@
 
+import java.net.URI;
+import java.util.List;
+
 @RestController
@@ -31,160 +31,203 @@
 @SecurityRequirement(name = "bearerAuth")
-@Tag(name = "alerts", description = "빈자리 알림 신청의 생성, 조회, 해제")
-public final class AlertController {
-    private final AlertApplicationService alertService;
-
-    public AlertController(AlertApplicationService alertService) {
-        this.alertService = alertService;
-    }
-
-    @Operation(
-            operationId = "createAlert",
-            summary = "빈자리 알림 신청"
-    )
-    @ApiResponses({
... (342줄 더)
```

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/app/ValidationFailure.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 27건이다.

### 충족 주장과 게이트 판정 (21건)

주장은 판정이 아니다. 어긋난 자리만 남긴다.

| iteration | point | 관찰 |
| --- | --- | --- |
| 001 | getCourtAvailability:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:502 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getCourtAvailability:504 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:201 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:500 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | createAlert:503 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | getAlert:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | cancelAlert:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 002 | getCourtAvailability:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:500/502/503/504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:200/201 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:200/201/422 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | Alert:required-fields | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | Alert.expiresAt | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | contract:v2 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | verification | G1이 미판정이라 이 주장을 대조할 수 없다 |

### 게이트가 세는 것을 다시 말한 지적 (6건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다.

| iteration | axis | critique_id | rule | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | failure_faithfulness | w_missing_parameter_failure_mapping | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | state_continuity | w_expiry_transition_guard | response.type_changed | 게이트가 response.type_changed로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | state_continuity | w_condition_dedup_not_atomic | response.type_changed | 게이트가 response.type_changed로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | response_fidelity | w_expiry_source_semantics | response.type_changed | 게이트가 response.type_changed로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | state_continuity | w_refresh_waiter_divergence | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | judgement_disclosure | w_contract_semantics_unreported | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |

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
| gate:request.field_extra | createAlert | AlertRequest.slot.startTime.hour | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.startTime.minute | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.startTime.second | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.startTime.nano | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.endTime.hour | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.endTime.minute | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.endTime.second | 001 | 계약에 없는 요청 필드다 |
| gate:request.field_extra | createAlert | AlertRequest.slot.endTime.nano | 001 | 계약에 없는 요청 필드다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.startTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.startTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.startTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.startTime.nano | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.endTime.hour | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.endTime.minute | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.endTime.second | 001 | 계약이 열어 둔 객체다 |
| gate:response.field_extra | createAlert:201 | Alert.slot.endTime.nano | 001 | 계약이 열어 둔 객체다 |
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
| gate:decision.unknown_point | authentication | src/main/java/com/thinking/tennis/api/ApiMapper.java | 001 | 계약의 판정 지점이 아니다 (결정 d_user_identity) |
| gate:decision.unknown_point | availability-refresh-wait | src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java | 001 | 계약의 판정 지점이 아니다 (결정 d_refresh_wait_deadline) |
| gate:decision.unknown_point | 5xx-trace-id | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 001 | 계약의 판정 지점이 아니다 (결정 d_trace_id_format) |
| gate:decision.unknown_point | AlertStatus:EXPIRED | src/main/java/com/thinking/tennis/config/SchedulingConfig.java | 001 | 계약의 판정 지점이 아니다 (결정 d_expiry_scheduler_cadence) |
| gate:decision.unknown_point | Alert.delivery | src/main/java/com/thinking/tennis/app/AvailabilityApplicationService.java | 001 | 계약의 판정 지점이 아니다 (결정 d_delivery_surface) |
