# 계약 적합성 run 리포트 — 2026-09-30_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T16:15:41+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

**기계 판정이 깨끗한 iteration이 있다: 001.** 그 iteration에서 G0와 G1과 판본 대조가 모두 판정되었고 위반이 없었다. v0의 합격선은 최종 판정 PASS가 아니라 이것이다.

판정은 **PASS**이고 iteration 001까지 왔다. 위반 0건 가운데 0건을 코드로 닫고 0건을 계약으로 닫았으며 0건이 아직 열려 있다. 서 있는 결정은 14건, 계약 변경 기록은 0건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **low** 수준이다. 올릴 근거는 없다.

합격선은 두 문장이 함께 참인 상태다. **판정했다**와 **그리고 위반이 없다**를 같은 칸에 넣지 않는다. 뒤의 문장만 적으면 게이트를 부수는 것이 게이트를 통과하는 가장 쉬운 길이 된다.

| iteration | 판정 | 판정했다 | 위반 없다 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | PASS | 예 | 예 | PASS | PASS | PASS | 0 | 31 | 4.2 |

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

신고된 결정은 14건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_bearer_token_identity | 계약이 인증 방식을 정하지 않았는데 Bearer 토큰에서 사용자 식별자를 어떻게 얻는가 | 토큰 문자열을 사용자 식별자로 쓰고, 헤더가 없거나 Bearer 형식이 아니거나 토큰이 비면 401 UNAUTHENTICATED | spec_implication | medium | BearerUserResolver 한 파일을 고친다. 유스케이스는 사용자 식별자를 문자열로만 받으므로 바뀌지 않는다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, getAlert:404, cancelAlert:404, listAlerts:200 | standing |
| d_version_prefix_in_references | 오퍼레이션 경로에 주 버전 접두사를 넣지 않는데 Location과 instance에는 접두사를 붙이는가 | 매핑 경로에는 접두사를 넣지 않고 Location과 instance에만 /v1을 붙인다 | contract_analogy | medium | ApiUris의 상수 하나를 비운다. | createAlert:201, createAlert:200, getCourtAvailability:400, getCourtAvailability:404, createAlert:422, getAlert:404, cancelAlert:404 | standing |
| d_framework_request_failures | 프레임워크가 요청 자체를 거절한 실패를 어느 코드로 옮기는가 | 헤더·매개변수 누락, 타입 불일치, 읽을 수 없는 본문, 지원하지 않는 미디어 타입과 그 밖의 프레임워크 4xx를 400 VALIDATION_FAILED로 옮긴다 | contract_analogy | medium | ApiErrorHandler의 처리기 몇 개를 나눈다. | getCourtAvailability:400, createAlert:400, listAlerts:400, getAlert:400, cancelAlert:400 | standing |
| d_expiry_sweep_interval | 만료로 옮기는 서버 작업을 얼마나 자주 도는가 | 1초 간격으로 돌며 만료 시각이 지난 감시 중 신청을 만료로 옮긴다 | spec_implication | medium | @Scheduled의 값 하나를 고친다. | listAlerts:200, getAlert:200, cancelAlert:200, createAlert:201 | standing |
| d_slots_undetermined_internal_error | 신청 시점에 운영 시간대를 확인하지 못하면 무엇으로 나가는가 | 500 INTERNAL_ERROR에 retryAfterSeconds 5와 traceId를 실어 낸다 | least_harm | medium | AvailabilityUndetermined를 내는 자리와 그 처리기 한 쌍을 고친다. | createAlert:500, createAlert:422, createAlert:INTERNAL_ERROR, createAlert:SLOT_NOT_SUPPORTED | standing |
| d_idempotency_fingerprint | 같은 키로 온 요청이 같은 요청인지 무엇으로 가르는가 | 코트, 날짜, 시간대의 시작·끝을 이어 붙인 지문으로 비교한다 | spec_implication | medium | AlertController.fingerprint 한 메서드를 고친다. | createAlert:409, createAlert:200, createAlert:201 | standing |
| d_problem_optional_keys | 전역 직렬화 설정이 널을 지우지 않는데 traceId를 5xx에만 싣는 약속을 어떻게 지키는가 | Problem.toBody()가 계약 순서대로 실을 키만 담은 LinkedHashMap을 만들고 그 표를 응답 본문으로 낸다. 스펙의 Problem 스키마는 레코드 선언이 그대로 낸다. | contract_analogy | high | Problem.toBody() 하나를 고친다. | getCourtAvailability:400, getCourtAvailability:404, getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:400, createAlert:401, createAlert:409, createAlert:422, createAlert:500, createAlert:503, listAlerts:400, getAlert:404, cancelAlert:503 | standing |
| d_trace_id_format | traceId에 무엇을 담는가 | 무작위 UUID 문자열을 담고 같은 값을 서버 로그에 함께 남긴다 | convention | high | newTraceId 한 메서드를 고친다. | getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, createAlert:503, listAlerts:500, getAlert:500, cancelAlert:500, cancelAlert:503 | standing |
| d_expires_at_stays_at_slot_start | 예약 마감 시각이 더 이르면 만료를 앞당기라고 했는데 그 값을 어디서 얻는가 | Asia/Seoul 기준 이용 시작 시각을 만료 시각으로 두고 앞당기지 않는다 | contract_analogy | high | 만료 시각 계산 한 메서드와 그 값을 읽는 자리를 고친다. 포트가 마감 시각을 드러내야 한다. | createAlert:201, createAlert:200, createAlert:422, getAlert:200, listAlerts:200, createAlert:ALERT_WINDOW_CLOSED | standing |
| d_failed_check_fills_interval | 실패한 확인 시도도 확인 간격을 채운 것으로 세는가 | 마지막 시도 시각을 따로 기록해 실패한 시도도 확인 간격을 채운 것으로 센다 | spec_implication | high | AvailabilityStore의 Entry와 신선도 판정 한 줄을 고친다. | getCourtAvailability:200, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:422, createAlert:500 | standing |
| d_idempotency_conflict_precedence | 같은 키의 앞선 요청이 처리 중인데 이번 요청의 내용이 다르면 409와 503 중 무엇인가 | 지문 불일치를 먼저 판정해 409 IDEMPOTENCY_KEY_REUSED를 낸다 | contract_analogy | high | beginOrReplay의 두 검사 순서를 바꾼다. | createAlert:409, createAlert:503, createAlert:IDEMPOTENCY_KEY_REUSED, createAlert:CONCURRENT_UPDATE_CONFLICT | standing |
| d_unprocessable_precedence | 422로 갈 세 조건이 동시에 걸릴 때 어느 코드로 나가는가 | 코트 지원 여부, 운영 시간대, 만료 시각 순으로 판정하고 처음 걸린 것으로 거절한다 | contract_analogy | high | create의 판정 순서 세 줄을 옮긴다. | createAlert:422, createAlert:COURT_NOT_SUPPORTED, createAlert:SLOT_NOT_SUPPORTED, createAlert:ALERT_WINDOW_CLOSED | standing |
| d_auth_before_body_validation | 토큰도 없고 본문도 틀린 요청은 401과 400 중 무엇으로 나가는가 | 사용자 식별을 먼저 하고 그다음 본문을 검사한다 | convention | high | createAlert 본문의 두 줄 순서를 바꾼다. | createAlert:400, createAlert:401 | standing |
| d_list_order_tie_break | 신청 시각이 같은 두 신청 사이의 순서를 무엇으로 가르는가 | 신청마다 증가하는 번호를 붙이고 그 역순으로 내며 그 번호는 응답에 싣지 않는다 | convention | high | Alert의 필드 하나와 정렬 한 줄을 뺀다. | listAlerts:200, createAlert:200 | standing |

`d_bearer_token_identity` — FR-2가 남의 신청을 조회·해제할 수 없게 하라고 요구하므로 요청마다 사용자 식별자가 필요하다. 계약은 인증 방식 자체를 정하지 않겠다고 적었으므로 검증 방식은 구현이 고르는 자리이고, 토큰을 식별자로 쓰면 사용자 분리를 지키면서 계약에 없는 토큰 구조를 지어내지 않는다.

`d_version_prefix_in_references` — 계약의 servers가 /v1을 들고 있어 오퍼레이션 경로에 다시 넣으면 두 번 붙는다. 반면 Location과 instance의 예시는 /v1까지 붙은 모양이고 이 둘은 클라이언트가 그대로 따라갈 참조라 배포된 표면과 같아야 한다.

`d_framework_request_failures` — 계약은 요청의 형식이나 값이 스키마를 만족하지 않는 경우를 VALIDATION_FAILED 하나로 모으고 멱등 키 헤더의 누락도 여기에 든다고 적었다. 계약이 선언하지 않은 상태 코드를 새로 내보내지 않는 쪽을 택한다.

`d_expiry_sweep_interval` — 계약은 만료를 서버 작업이 옮긴다고 했고 그 사이 상태가 잠시 감시 중으로 보일 수 있다고 적었지만 얼마나 늦을 수 있는지는 정하지 않았다. 명세가 발송 대기를 1초 주기로 꺼낸다고 한 것과 같은 결로 두어 뒤늦게 보이는 창을 그 주기에 맞춘다.

`d_slots_undetermined_internal_error` — 계약은 이 오퍼레이션에 예약처 실패를 알리는 상태 코드를 두지 않았고 INTERNAL_ERROR를 그 밖의 서버 내부 실패로 정의했다. 422로 내면 일시적 실패를 영구 거절로 말하고, 검사를 건너뛰면 계약이 거절하라고 한 조건을 받는다. 재시도 가능한 500이 잘못 말하는 것이 가장 적다.

`d_idempotency_fingerprint` — 계약은 같은 요청을 재시도하면 재생된다고 했고 신청은 코트 하나, 날짜 하나, 시간대 하나라고 적었다. 그 셋이 같으면 같은 신청이므로 표현의 차이로 재시도를 막지 않는다.

`d_problem_optional_keys` — 계약은 traceId를 5xx에만, retryAfterSeconds를 재시도 가능한 실패에만 실으라고 적었고, 전역 설정은 널을 허용하는 필수 필드의 키를 지키려고 널을 남기는 쪽으로 고정돼 있다. 두 약속이 부딪히는 자리는 실패 본문뿐이므로 그 본문만 키를 골라 담는다.

`d_trace_id_format` — 계약은 traceId를 형식 없는 문자열로 두고 서버 로그에서 이 요청을 찾는 식별자라고만 적었다. 찾을 수 있으면 되는 값이므로 형식을 지어내지 않고 겹치지 않는 값을 쓴다.

`d_expires_at_stays_at_slot_start` — 계약은 만료 시각을 이용 시작 시각이라고 정하고 더 이른 마감 시각이 확인되면 그 시각으로 앞당기라고 적었다. 확인 결과에는 마감 시각이 없고 포트도 그것을 드러내지 않으므로 확인되는 일이 없다. 값을 지어내는 대신 계약이 정한 기본값만 쓴다.

`d_failed_check_fills_interval` — 계약은 확인이 실패하면 기다리던 요청이 모두 같은 실패를 받는다고 했고 예약처로 나가는 조회는 간격마다 한 번이라고 했다. 성공만 세면 실패하는 동안 그 상한이 사라진다. 늦게 아는 값은 간격을 넘지 않는다.

`d_idempotency_conflict_precedence` — 계약은 409를 요청을 고쳐야 풀리는 실패로, 503을 기다리면 풀리는 실패로 적었다. 내용이 다른 키는 기다려도 풀리지 않으므로 재시도 가능으로 말하면 안 된다.

`d_unprocessable_precedence` — 코트를 모르면 그 코트의 운영 시간대를 물을 수 없고 운영하지 않는 시간대에는 이용 시작 시각도 없다. 계약이 422 예시를 적은 순서와도 같다.

`d_auth_before_body_validation` — 계약은 두 실패의 순서를 적지 않았다. 인증을 먼저 보는 것이 인증 뒤의 판정 결과가 인증 없이 새어 나가지 않게 한다.

`d_list_order_tie_break` — 계약은 최근에 신청한 것이 앞에 온다고만 적었다. 같은 시각의 두 신청도 만들어진 순서는 있으므로 그 순서를 기록해야 목록이 호출마다 같은 답을 낸다.

## 5. 고친 것

게이트가 잡은 위반이 없다. 판정 장치가 다 돌았다면 이 run은 위반 없이 왔다는 뜻이다.

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 3건이다.

### 축 판정의 어긋남 (3건)

Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다. 방향을 함께 적는다 — 누가 더 엄한지가 읽는 사람에게 필요하다. 게이트까지 셋이 갈린 자리가 자기 평가와 계약 테스트 결과가 어긋난 지점의 후보다.

| iteration | axis | critique_id | gate_verdict | eval_score | direction | 관찰 |
| --- | --- | --- | --- | --- | --- | --- |
| 001 | state_continuity | w_create_triggers_and_writes_check | PASS | 4 | Critique가 더 엄하다 | 게이트는 위반을 찾지 못했고 Eval은 4를 줬는데 Critique만 high로 지적했다. 셋이 갈렸다. |
| 001 | judgement_disclosure | w_create_check_not_disclosed | PASS | 4 | Critique가 더 엄하다 | 게이트는 위반을 찾지 못했고 Eval은 4를 줬는데 Critique만 high로 지적했다. 셋이 갈렸다. |
| 001 | request_tolerance | w_unopened_date_rejected | PASS | 5 | Critique가 더 엄하다 | 게이트는 위반을 찾지 못했고 Eval은 5를 줬는데 Critique만 high로 지적했다. 셋이 갈렸다. |

게이트가 세는 자리를 비평이 다시 말한 곳은 없다. 같은 좌표를 가리키지 않으면 겹침이 아니므로 축 이름이나 규칙이 하나뿐인 것을 근거로 짝짓지 않는다. 겹치지 않았다고 적는 것이 지어낸 겹침보다 낫다.

### 게이트가 남긴 기록 (31건)

게이트가 보고 위반으로 세지 않은 차이다. 계약이 말하지 않은 자리가 자란 것과 응답의 값을 계약보다 좁게 선언한 것이 여기 온다. REJECT 사유가 아니고 뜻은 Critique가 붙인다.

| 규칙 | 가족 | 라벨 | 판정 지점 | 좌표 | iteration | 관찰 |
| --- | --- | --- | --- | --- | --- | --- |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:400 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:404 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:500 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:502 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:503 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:504 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:200 | AlertList.items[].delivery | 001 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:400 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:401 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:500 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:503 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:201 | Alert.delivery | 001 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:200 | Alert.delivery | 001 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:400 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:401 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:409 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:422 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:500 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:503 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:200 | Alert.delivery | 001 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:400 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:401 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:404 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:500 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:503 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:200 | Alert.delivery | 001 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:400 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:401 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:404 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:500 | Problem | 001 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:503 | Problem | 001 | 형식: 2곳에서 좁혔다 |

기록에 붙은 라벨의 뜻이다. 손해가 없는 차이는 위반이 아니라 여기 온다 — 판정은 그대로 남기고 리포트에서 뒤로 보낼 뿐이다.

| 라벨 | 뜻 |
| --- | --- |
| harmless_to_client | 쓰는 쪽에 손해가 없는 차이다. 응답의 값의 범위나 형식이 계약보다 좁은 자리다. 계약대로 읽는 쪽은 받을 수 있다고 믿은 값의 부분집합을 받는다. 리포트에서 뒤로 온다. |
