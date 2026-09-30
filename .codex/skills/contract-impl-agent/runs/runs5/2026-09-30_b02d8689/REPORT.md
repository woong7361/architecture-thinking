# 계약 적합성 run 리포트 — 2026-09-30_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T03:04:59+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 003까지 왔다. 위반 1건 가운데 0건을 코드로 닫고 0건을 계약으로 닫았으며 1건이 아직 열려 있다. 서 있는 결정은 10건, 계약 변경 기록은 0건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **high** 수준이다. 근거는 low confidence decisions: ['d_lazy_expiry_materialization']다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:g0.compile_failed@None: 컴파일 오류 1건
src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java:57:20 cannot find symbol
    symbol: method setRetryAfter(java.time.Duration)
    location: variable headers of type org.springframework.http.HttpHeaders

색인과 실물이 어긋난 경로가 54곳이다. 게이트는 색인이 선언한 것을 판정하므로 선언 밖에 놓인 파일은 판정되지 않은 표면이 된다.

| 경로 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/ApiModels.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AuthenticationException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/OperationId.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidationException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/TennisAlertController.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertApplication.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertApplicationConfiguration.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertApplicationException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertNotFoundException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertWindowClosedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityFailureException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityResult.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/ConcurrentUpdateConflictException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CourtNotSupportedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CreateAlertResult.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/IdempotencyKeyReusedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAlertRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAvailabilityRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/SlotNotSupportedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/StorageTimeoutException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertDelivery.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertStatus.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DeliveryStatus.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiModels.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AuthenticationException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/OperationId.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidationException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/TennisAlertController.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertApplication.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertApplicationConfiguration.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertApplicationException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertNotFoundException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertWindowClosedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityFailureException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityResult.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/ConcurrentUpdateConflictException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CourtNotSupportedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CreateAlertResult.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/IdempotencyKeyReusedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAlertRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAvailabilityRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/SlotNotSupportedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/StorageTimeoutException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertDelivery.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertStatus.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DeliveryStatus.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |

판정되지 않은 검사가 있다: changes, g1. 건너뛴 단계의 이유는 g0_reject다. 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |
| 002 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |
| 003 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |

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

신고된 결정은 10건이고 그중 1건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_lazy_expiry_materialization | 만료를 수행하는 주기와 스케줄러가 계약에 정해지지 않은 경우 EXPIRED 상태 전이를 언제 물질화할 것인가? | 신청 조회와 해제, 관련 확인 시점에 만료를 평가 | least_harm | low | 스케줄러, 전체 신청 순회 API, 종료 수명주기와 테스트 시간 제어를 추가해야 한다. | listAlerts:200, getAlert:200, cancelAlert:200, getCourtAvailability:200 | standing |
| d_trace_id_format | 계약이 traceId의 구체 형식을 정하지 않은 경우 어떤 식별자를 생성할 것인가? | UUID 문자열을 요청별 traceId로 생성 | convention | medium | 오류 핸들러의 traceId 생성기와 로그 상관관계 포맷을 교체해야 한다. | getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, createAlert:503, listAlerts:500, listAlerts:503, getAlert:500, getAlert:503, cancelAlert:500, cancelAlert:503 | standing |
| d_bearer_identity | 계약이 Bearer 사용만 정하고 토큰 검증자나 사용자 ID 형식을 정하지 않은 경우 사용자 식별값을 어떻게 정할 것인가? | Bearer 접두사를 제거한 토큰 문자열 자체를 사용자 식별자로 사용 | least_harm | medium | 인증 어댑터와 사용자 식별 변환을 추가하고 모든 소유권 조회 경계를 교체해야 한다. | createAlert:201, createAlert:200, listAlerts:200, getAlert:200, cancelAlert:200, createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401 | standing |
| d_create_upstream_failure_mapping | createAlert의 요청 조건 검증 중 외부 확인이 실패하고 POST 계약에 upstream 전용 응답이 없을 때 어떤 실패로 반환할 것인가? | INTERNAL_ERROR 500으로 반환 | least_harm | medium | POST 예외 매핑과 오류 계약을 함께 변경해야 한다. | createAlert:500, createAlert:INTERNAL_ERROR | standing |
| d_failed_refresh_coalescing | 같은 코트·날짜의 대기 요청이 첫 확인 실패 후 같은 실패를 받아야 할 때 실패 결과를 어떻게 공유할 것인가? | 확인 간격인 20초 동안 최근 실패를 코트·날짜별로 재사용 | contract_analogy | high | 동시 확인 캐시의 실패 상태와 만료 정책을 변경해야 한다. | getCourtAvailability:200, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, getCourtAvailability:UPSTREAM_TIMEOUT, getCourtAvailability:UPSTREAM_UNAVAILABLE, getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE | standing |
| d_slot_validation_source | 계약이 슬롯 카탈로그 포트를 별도로 제공하지 않을 때 지원 슬롯 여부를 어떻게 판단할 것인가? | 캐시된 또는 새로 확인한 CourtAvailability 결과의 슬롯과 정확히 일치하는지 검증 | contract_analogy | high | 신청 유스케이스의 외부 확인 의존성과 슬롯 검증 경로를 별도 포트로 분리해야 한다. | createAlert:201, createAlert:422, createAlert:500, getCourtAvailability:200, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504 | standing |
| d_expiry_boundary | 더 이른 예약 마감 시각 정보가 제공 API에 없을 때 expiresAt을 무엇으로 계산할 것인가? | date와 startTime을 Asia/Seoul의 이용 시작 시각으로 변환 | spec_implication | high | Alert 생성과 만료 판정, 응답 매핑의 시간 계산을 함께 변경해야 한다. | createAlert:201, createAlert:422, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_delivery_state_without_sender | 발송 채널과 발송 포트가 out_of_scope인 초안에서 빈자리 확인 후 delivery와 status를 어떻게 표현할 것인가? | 빈자리 확인 시 PENDING만 기록하고 실제 발송 전이는 채널 경로에 남긴다. | spec_implication | high | AsyncAPI 발송 포트와 성공·실패·재시도 상태 전이를 추가해야 한다. | getCourtAvailability:200, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_idempotency_snapshot | 처음 응답 뒤 상태가 변해도 같은 멱등 키의 응답을 그대로 재생하려면 무엇을 보관할 것인가? | 생성 시점 Alert 복사본과 상태 코드를 24시간 엔트리에 보관 | contract_analogy | high | 멱등 저장 구조와 응답 DTO 매핑을 함께 교체해야 한다. | createAlert:201, createAlert:200, createAlert:IDEMPOTENCY_KEY_REUSED | standing |
| d_location_prefix | 서버 URL에 /v1 접두사를 중복 매핑하지 않으면서 Location에는 어떤 상대 경로를 보낼 것인가? | 컨트롤러 매핑은 /alerts로 두고 Location은 /v1/alerts/{id}로 생성 | contract_analogy | high | 컨트롤러 매핑 또는 Location 생성 규칙과 배포 경로를 함께 변경해야 한다. | createAlert:201, createAlert:200 | standing |

`d_lazy_expiry_materialization` — 계약은 만료 결과를 HTTP 상태로 노출하지만 스케줄 주기를 정하지 않았고, 인메모리 HTTP 초안의 관찰 결과를 결정적으로 유지하기 위해 접근 시 평가한다.

`d_trace_id_format` — Problem 스키마는 string만 요구하며 stack에 없는 라이브러리를 추가하지 않고 고유 식별자를 제공한다.

`d_bearer_identity` — 인증 방식 자체가 계약 범위 밖이고 제공된 포트에 인증 검증 API가 없으므로 HTTP 표면에서 사용자별 격리만 보장한다.

`d_create_upstream_failure_mapping` — POST는 500과 503만 선언하고 upstream 상태 코드는 선언하지 않았으므로 계약에 없는 응답을 넓히지 않는다.

`d_failed_refresh_coalescing` — 확인 간격과 Retry-After가 20초이고, 같은 갱신에 묶인 요청은 모두 같은 실패를 받아야 한다.

`d_slot_validation_source` — CourtAvailability의 slots가 운영 시간대 전부이며 없는 시간대는 SLOT_NOT_SUPPORTED라는 계약 설명을 그대로 사용한다.

`d_expiry_boundary` — 요구사항은 이용 시작 시각을 기본 만료 시각으로 명시하고, 제공 포트에는 조기 예약 마감 시각이 없다.

`d_delivery_state_without_sender` — 발송 수단과 채널은 out_of_scope이며 계약은 PENDING을 발송 전 대기로 정의한다.

`d_idempotency_snapshot` — 계약이 재생 응답을 처음 처리한 시점의 내용으로 고정한다고 명시한다.

`d_location_prefix` — implementation_contract는 서버 URL 접두사를 operation path에 반복하지 말라고 하고 계약 예시는 Location에 /v1을 보여준다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 1건 중 0건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_579dfe | g0.compile_failed | — | mvnw test | open | — | 0 | 001 | — |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_579dfe | 컴파일 오류 1건 src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java:125:17 cannot find symbol     symbol: variable AlertApplication     location: class com.thinking.tennis.api.TennisAlertExceptionHandler | — | — |

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/TennisAlertExceptionHandler.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 126건이다.

### 충족 주장과 게이트 판정 (126건)

주장은 판정이 아니다. 어긋난 자리만 남긴다.

| iteration | point | 관찰 |
| --- | --- | --- |
| 001 | getCourtAvailability:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:502 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:COURT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_UNAVAILABLE | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:201 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:409 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:422 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:IDEMPOTENCY_KEY_REUSED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:COURT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:SLOT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:ALERT_WINDOW_CLOSED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:CONCURRENT_UPDATE_CONFLICT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:ALERT_NOT_FOUND | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:ALERT_NOT_FOUND | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:CONCURRENT_UPDATE_CONFLICT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:502 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:COURT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:UPSTREAM_UNAVAILABLE | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:UPSTREAM_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getCourtAvailability:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | listAlerts:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:201 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:409 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:422 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:IDEMPOTENCY_KEY_REUSED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:COURT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:SLOT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:ALERT_WINDOW_CLOSED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | createAlert:CONCURRENT_UPDATE_CONFLICT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:ALERT_NOT_FOUND | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | getAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:ALERT_NOT_FOUND | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | cancelAlert:CONCURRENT_UPDATE_CONFLICT | G1이 미판정이라 이 주장을 대조할 수 없다 |
