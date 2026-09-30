# 계약 적합성 run 리포트 — 2026-09-29_745183bc

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-29T22:43:49+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 001까지 왔다. 위반 46건 가운데 0건을 코드로 닫고 0건을 계약으로 닫았으며 46건이 아직 열려 있다. 서 있는 결정은 6건, 계약 변경 기록은 0건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **low** 수준이다. 올릴 근거는 없다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:extract.blind@None: 계약은 (상태 코드, 에러 코드) 쌍을 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다
- gate:extract.blind@None: 계약은 인증 요구를 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다
- gate:response.required_weakened@getCourtAvailability:200: 계약이 항상 담겠다고 보장한 필드 ['confirmedAt', 'courtId', 'courtName', 'date', 'reservationUrl', 'slots', 'stale', 'staleReason']
- gate:response.required_weakened@getCourtAvailability:200: 계약이 항상 담겠다고 보장한 필드 ['available', 'endTime', 'startTime']
- gate:response.status_missing@getCourtAvailability:400: 계약이 선언한 상태 코드다
- gate:response.status_missing@getCourtAvailability:404: 계약이 선언한 상태 코드다
- gate:response.status_missing@getCourtAvailability:500: 계약이 선언한 상태 코드다
- gate:response.status_missing@getCourtAvailability:502: 계약이 선언한 상태 코드다
- gate:response.status_missing@getCourtAvailability:503: 계약이 선언한 상태 코드다
- gate:response.status_missing@getCourtAvailability:504: 계약이 선언한 상태 코드다
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['items']
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.status_missing@listAlerts:400: 계약이 선언한 상태 코드다
- gate:response.status_missing@listAlerts:401: 계약이 선언한 상태 코드다
- gate:response.status_missing@listAlerts:500: 계약이 선언한 상태 코드다
- gate:response.status_missing@listAlerts:503: 계약이 선언한 상태 코드다
- gate:response.status_missing@createAlert:201: 계약이 선언한 상태 코드다
- gate:response.required_weakened@createAlert:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@createAlert:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@createAlert:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.header_missing@createAlert:200: 계약이 싣겠다고 선언한 헤더다
- gate:response.header_missing@createAlert:200: 계약이 싣겠다고 선언한 헤더다
- gate:response.status_missing@createAlert:400: 계약이 선언한 상태 코드다
- gate:response.status_missing@createAlert:401: 계약이 선언한 상태 코드다
- gate:response.status_missing@createAlert:409: 계약이 선언한 상태 코드다
- gate:response.status_missing@createAlert:422: 계약이 선언한 상태 코드다
- gate:response.status_missing@createAlert:500: 계약이 선언한 상태 코드다
- gate:response.status_missing@createAlert:503: 계약이 선언한 상태 코드다
- gate:response.required_weakened@getAlert:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@getAlert:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@getAlert:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.status_missing@getAlert:400: 계약이 선언한 상태 코드다
- gate:response.status_missing@getAlert:401: 계약이 선언한 상태 코드다
- gate:response.status_missing@getAlert:404: 계약이 선언한 상태 코드다
- gate:response.status_missing@getAlert:500: 계약이 선언한 상태 코드다
- gate:response.status_missing@getAlert:503: 계약이 선언한 상태 코드다
- gate:response.required_weakened@cancelAlert:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@cancelAlert:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@cancelAlert:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.status_missing@cancelAlert:400: 계약이 선언한 상태 코드다
- gate:response.status_missing@cancelAlert:401: 계약이 선언한 상태 코드다
- gate:response.status_missing@cancelAlert:404: 계약이 선언한 상태 코드다
- gate:response.status_missing@cancelAlert:500: 계약이 선언한 상태 코드다
- gate:response.status_missing@cancelAlert:503: 계약이 선언한 상태 코드다

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | PASS | REJECT | PASS | 46 | 4 | 4.2 |

## 2. 계약을 이렇게 고쳤다

계약 판본은 이 run에서 움직이지 않았다. 고칠 자리를 찾지 못했거나 고칠 필요가 없었다는 뜻이다.

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 0곳이 달라지고 깨는 변경 0건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 0곳이 달라지고 깨는 변경 0건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 사라진 판정 지점 |
| --- | --- | --- | --- |
| 앞 판본 | 0 | 0 | — |
| 이 run의 기준선 | 0 | 0 | — |
| 사람이 확정한 원본 | 0 | 0 | — |

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 6건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_bearer_identity | 계약이 Bearer 토큰의 검증 체계와 사용자 식별자 추출 규칙을 정하지 않은 자리에서 무엇을 사용자 키로 사용할 것인가? | Bearer 접두사 뒤의 비어 있지 않은 값 전체를 사용자 식별자로 사용 | least_harm | medium | 인증 어댑터와 사용자 키 추출을 교체하고 인메모리 저장 키의 마이그레이션이 필요하다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_create_slot_validation | 신청 본문의 시간대가 실제 운영 시간대인지 어떤 경로로 확인할 것인가? | AvailabilityApplicationService를 통해 저장 결과를 사용하거나 확인을 일으킨 뒤 슬롯을 정확히 비교 | contract_analogy | medium | 신청 검증 경로와 코트별 운영시간 데이터 공급원을 함께 변경해야 한다. | createAlert:201, createAlert:200, createAlert:422, createAlert:502, createAlert:503, createAlert:504 | standing |
| d_trace_id_format | 계약이 traceId의 문자열 타입만 정한 자리에서 어떤 식별자 형식을 사용할 것인가? | UUID 문자열을 traceId로 생성 | convention | medium | 문제 응답 생성기와 로그 상관관계 포맷을 교체해야 한다. | getCourtAvailability:500, listAlerts:500, createAlert:500, getAlert:500, cancelAlert:500 | standing |
| d_clock_source | UTC 타임스탬프를 계산할 시간 공급원을 어떻게 선택할 것인가? | Clock.systemUTC()를 주입해 모든 Instant를 UTC로 계산 | spec_implication | high | Clock Bean과 만료·지연 계산을 사용하는 모든 유스케이스를 변경해야 한다. | createAlert:201, createAlert:200, listAlerts:200, getAlert:200, cancelAlert:200, getCourtAvailability:200 | standing |
| d_last_checked_projection | 신청에 저장된 마지막 확인 시각과 공유된 코트·날짜 확인 결과를 어떻게 일치시킬 것인가? | 응답 생성 시 공유 availability 캐시의 더 최신 성공 시각을 lastCheckedAt으로 투영 | spec_implication | high | 신청 저장 모델에 확인 결과 참조 또는 이벤트 갱신을 추가하고 응답 계산을 변경해야 한다. | createAlert:201, createAlert:200, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_availability_coalescing_executor | 동일 코트·날짜의 동시 확인을 어떤 실행 방식으로 합칠 것인가? | 코트·날짜 키별 CompletableFuture를 공유하고 완료 시 제거 | contract_analogy | high | 확인 캐시와 동시성 제어를 별도 조정자 또는 실행기 구현으로 교체해야 한다. | getCourtAvailability:200, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504 | standing |

`d_bearer_identity` — 계약은 Bearer 사용만 정하고 토큰 형식·검증 주체를 정하지 않았으므로 추가 인증 규칙을 만들지 않고 소유권 격리만 보장한다.

`d_create_slot_validation` — 계약은 코트·날짜별 슬롯이 예약 상태 결과에 있다고 명시하고 제공 API도 그 결과만 제공하므로 별도 시간표를 발명하지 않았다.

`d_trace_id_format` — 계약은 traceId의 형식 세부를 정하지 않았고 UUID는 Java 표준 API로 생성 가능하다.

`d_clock_source` — 계약이 타임스탬프를 UTC로 요구하고 날짜·시각만 Asia/Seoul 기준으로 구분하므로 시각 계산은 UTC Clock으로 고정했다.

`d_last_checked_projection` — lastCheckedAt은 신청별 예약처 확인 시각이 아니라 해당 신청의 코트·날짜를 마지막으로 성공 확인한 시각이므로 공유 확인 결과를 읽는다.

`d_availability_coalescing_executor` — 계약이 같은 확인 결과를 기다리게 하므로 키별 단일 Future가 결과 동일성과 외부 호출 억제를 함께 보장한다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 46건 중 0건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_035a89 | response.status_missing | createAlert:400 | responses | open | — | 0 | 001 | — |
| v_047bda | response.required_weakened | cancelAlert:200 | Alert | open | — | 0 | 001 | — |
| v_061225 | response.status_missing | getAlert:404 | responses | open | — | 0 | 001 | — |
| v_06155f | response.required_weakened | cancelAlert:200 | Alert.delivery | open | — | 0 | 001 | — |
| v_15798d | response.required_weakened | getAlert:200 | Alert.delivery | open | — | 0 | 001 | — |
| v_213a44 | response.status_missing | listAlerts:500 | responses | open | — | 0 | 001 | — |
| v_2fd24a | response.required_weakened | createAlert:200 | Alert.slot | open | — | 0 | 001 | — |
| v_310e16 | response.required_weakened | listAlerts:200 | AlertList.items[].slot | open | — | 0 | 001 | — |
| v_36e659 | response.status_missing | getAlert:503 | responses | open | — | 0 | 001 | — |
| v_40839c | response.required_weakened | cancelAlert:200 | Alert.slot | open | — | 0 | 001 | — |
| v_42778f | extract.blind | — | x-error-codes | open | — | 0 | 001 | — |
| v_499bdd | response.status_missing | createAlert:422 | responses | open | — | 0 | 001 | — |
| v_4fac05 | response.status_missing | getCourtAvailability:404 | responses | open | — | 0 | 001 | — |
| v_52b438 | response.status_missing | createAlert:401 | responses | open | — | 0 | 001 | — |
| v_552a8e | response.status_missing | getAlert:500 | responses | open | — | 0 | 001 | — |
| v_57240f | response.status_missing | cancelAlert:401 | responses | open | — | 0 | 001 | — |
| v_5b8320 | response.status_missing | createAlert:500 | responses | open | — | 0 | 001 | — |
| v_5bdad4 | response.header_missing | createAlert:200 | headers.Location | open | — | 0 | 001 | — |
| v_5c0d30 | response.status_missing | listAlerts:503 | responses | open | — | 0 | 001 | — |
| v_662884 | response.required_weakened | listAlerts:200 | AlertList.items[].delivery | open | — | 0 | 001 | — |
| v_7acd13 | response.required_weakened | createAlert:200 | Alert | open | — | 0 | 001 | — |
| v_7e4938 | response.status_missing | cancelAlert:500 | responses | open | — | 0 | 001 | — |
| v_817689 | response.status_missing | getCourtAvailability:400 | responses | open | — | 0 | 001 | — |
| v_81cde5 | response.status_missing | cancelAlert:400 | responses | open | — | 0 | 001 | — |
| v_8298d6 | response.status_missing | cancelAlert:404 | responses | open | — | 0 | 001 | — |
| v_851e03 | response.status_missing | listAlerts:400 | responses | open | — | 0 | 001 | — |
| v_8ad7ff | response.status_missing | getCourtAvailability:502 | responses | open | — | 0 | 001 | — |
| v_932149 | response.required_weakened | getAlert:200 | Alert.slot | open | — | 0 | 001 | — |
| v_96b99a | response.status_missing | getCourtAvailability:503 | responses | open | — | 0 | 001 | — |
| v_98b6a8 | response.required_weakened | listAlerts:200 | AlertList.items[] | open | — | 0 | 001 | — |
| v_9c5b4a | response.status_missing | getAlert:401 | responses | open | — | 0 | 001 | — |
| v_9c7d67 | response.header_missing | createAlert:200 | headers.Idempotency-Replayed | open | — | 0 | 001 | — |
| v_a6ff3e | response.status_missing | createAlert:503 | responses | open | — | 0 | 001 | — |
| v_aad0b3 | response.status_missing | createAlert:201 | responses | open | — | 0 | 001 | — |
| v_ab307f | response.status_missing | getCourtAvailability:504 | responses | open | — | 0 | 001 | — |
| v_bf1a9b | response.required_weakened | listAlerts:200 | AlertList | open | — | 0 | 001 | — |
| v_bfc8b4 | response.required_weakened | getAlert:200 | Alert | open | — | 0 | 001 | — |
| v_c9ec52 | response.status_missing | getAlert:400 | responses | open | — | 0 | 001 | — |
| v_cf5ae1 | response.required_weakened | getCourtAvailability:200 | CourtAvailability | open | — | 0 | 001 | — |
| v_d44d72 | response.required_weakened | createAlert:200 | Alert.delivery | open | — | 0 | 001 | — |
| v_e374a3 | response.required_weakened | getCourtAvailability:200 | CourtAvailability.slots[] | open | — | 0 | 001 | — |
| v_e85568 | response.status_missing | cancelAlert:503 | responses | open | — | 0 | 001 | — |
| v_f31579 | response.status_missing | listAlerts:401 | responses | open | — | 0 | 001 | — |
| v_f3725a | response.status_missing | getCourtAvailability:500 | responses | open | — | 0 | 001 | — |
| v_f47ecb | response.status_missing | createAlert:409 | responses | open | — | 0 | 001 | — |
| v_f5534b | extract.blind | — | security | open | — | 0 | 001 | — |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_035a89 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_047bda | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_061225 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_06155f | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_15798d | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_213a44 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_2fd24a | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_310e16 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_36e659 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_40839c | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_42778f | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | — |
| v_499bdd | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_4fac05 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_52b438 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_552a8e | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_57240f | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_5b8320 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_5bdad4 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_5c0d30 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_662884 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_7acd13 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_7e4938 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_817689 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_81cde5 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_8298d6 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_851e03 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_8ad7ff | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_932149 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_96b99a | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_98b6a8 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_9c5b4a | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_9c7d67 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_a6ff3e | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_aad0b3 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_ab307f | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_bf1a9b | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_bfc8b4 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_c9ec52 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_cf5ae1 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_d44d72 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_e374a3 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_e85568 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_f31579 | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_f3725a | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_f47ecb | 계약이 선언한 응답 상태 코드가 구현에 없다. | — | — |
| v_f5534b | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | — |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 38건이다.

### 충족 주장과 게이트 판정 (32건)

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

### 축 판정의 어긋남 (3건)

Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다.

| iteration | axis | critique_id | eval_score | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | state_continuity | create-refresh-boundary | 4 | Critique는 이 축을 high로 지적했는데 Eval은 4를 줬다 |
| 001 | failure_faithfulness | raw-bearer-as-authentication | 4 | Critique는 이 축을 high로 지적했는데 Eval은 4를 줬다 |
| 001 | response_fidelity | expiry-provenance | 4 | Critique는 이 축을 high로 지적했는데 Eval은 4를 줬다 |

### 게이트가 세는 것을 다시 말한 지적 (3건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다.

| iteration | axis | critique_id | rule | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | state_continuity | create-refresh-boundary | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | failure_faithfulness | raw-bearer-as-authentication | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | response_fidelity | expiry-provenance | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |

### 게이트가 남긴 기록 (4건)

계약이 말하지 않은 자리의 초과다. REJECT 사유가 아니고 뜻은 Critique가 붙인다.

| 규칙 | 판정 지점 | 좌표 | iteration | 관찰 |
| --- | --- | --- | --- | --- |
| gate:request.field_extra | listAlerts | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:request.field_extra | createAlert | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:request.field_extra | getAlert | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
| gate:request.field_extra | cancelAlert | parameters.header.Authorization | 001 | 계약에 없는 파라미터다 |
