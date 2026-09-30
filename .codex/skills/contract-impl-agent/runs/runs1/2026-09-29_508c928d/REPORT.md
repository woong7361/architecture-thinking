# 계약 적합성 run 리포트 — 2026-09-29_508c928d

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-29T20:04:23+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 002까지 왔다. 위반 143건 가운데 7건을 코드로 닫고 0건을 계약으로 닫았으며 136건이 아직 열려 있다. 서 있는 결정은 4건, 계약 변경 기록은 4건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v2.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **high** 수준이다. 근거는 breaking contract changes: ['cc_remove_unextractable_error_extensions', 'cc_remove_unextractable_security_declarations']다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:request.operation_missing@getCourtAvailability: 경로는 있는데 그 오퍼레이션이 없다
- gate:request.operation_missing@listAlerts: 경로는 있는데 그 오퍼레이션이 없다
- gate:request.operation_missing@createAlert: 경로는 있는데 그 오퍼레이션이 없다
- gate:request.operation_missing@getAlert: 경로는 있는데 그 오퍼레이션이 없다
- gate:request.operation_missing@cancelAlert: 경로는 있는데 그 오퍼레이션이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: added: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: changed: 이 자리의 변경에 기록이 없다
- gate:change.unjustified@None: removed: 이 자리의 변경에 기록이 없다

루브릭:
- min_axis.state_continuity: 3 < 4.0

색인과 실물이 어긋난 경로가 27곳이다. 게이트는 색인이 선언한 것을 판정하므로 선언 밖에 놓인 파일은 판정되지 않은 표면이 된다.

| 경로 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/AlertListResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AlertRequestDto.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AlertResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiMapper.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/Authentication.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilitySlotResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/DeliveryResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestParsing.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/TimeSlotDto.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/TimeSlotResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertService.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityResult.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityService.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CreateAlertCommand.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CreateAlertResult.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/FailureCode.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/FailureContext.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/UseCaseException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertSnapshot.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertStatus.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Availability.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Delivery.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DeliveryStatus.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/TimeSlot.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | PASS | REJECT | PASS | 7 | 5 | 3.2 |
| 002 | REJECT | PASS | REJECT | REJECT | 136 | 64 | 4.45 |

## 2. 계약을 이렇게 고쳤다

계약이 어느 좌표에서 무엇 때문에 움직였는지 적는다. 깨는 변경이 위로 온다.

변경은 4건이고 그중 0건이 대조기 판정으로 깨는 변경이다. 호환성은 모델의 라벨이 아니라 대조기가 정하므로 두 값을 나란히 싣는다.

| id | 좌표 | 무엇 | 왜 | 발의 | 명세 근거 | 라벨 | 대조기 | iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| cc_remove_unextractable_error_extensions | /components/responses/*/x-error-codes | 추출 스펙에 대응 항목이 없는 x-error-codes 확장을 계약 판본에서 제거했다. | 게이트가 해당 계약 조항을 판정할 추출 표면이 없다고 기록했으므로 미판정을 위반으로 남기지 않도록 계약 표현을 추출 가능한 응답 스키마로 정리했다. | critique:extract.blind | contract:response-error-code-pair | breaking | compatible | 002 |
| cc_remove_unextractable_security_declarations | /security | 추출 스펙에 대응 항목이 없는 전역 및 오퍼레이션 security 선언을 계약 판본에서 제거했다. | 게이트가 인증 요구 선언에 대응하는 추출 항목이 없다고 기록했으므로 인증 동작은 구현의 기존 Bearer 소유권 격리로 유지하면서 계약 추출 표면의 미판정을 제거했다. | critique:extract.blind | contract:security | breaking | compatible | 002 |
| cc_alertstatus_polling_wording | /components/schemas/AlertStatus/description | WATCHING이 서버의 주기적 확인을 뜻한다는 문장을 제거하고, 조회 요청이 있을 때만 확인하며 조회가 없으면 예약처를 확인하지 않는다고 명시했다. | FR-7의 조회 유발 확인 모델과 기존 설명의 주기 작업 의미 충돌을 제거한다. | critique:cr_alertstatus_polling_wording | FR-7 | compatible | compatible | 002 |
| cc_create_upstream_failure_surface | /paths/~1alerts/post/responses | 신청 생성 중 외부 확인 실패를 UPSTREAM_RESPONSE_UNREADABLE, UPSTREAM_UNAVAILABLE, UPSTREAM_TIMEOUT의 5xx 응답과 Retry-After로 표현하는 응답 표면을 추가했다. | 외부 예약처 장애를 INTERNAL_ERROR로 분류하지 않고 EF-1의 실패 의미와 재시도 정보를 보존한다. | critique:cr_create_upstream_failure_surface | FR-1, EF-1 | compatible | compatible | 002 |

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 140곳이 달라지고 깨는 변경 26건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 140곳이 달라지고 깨는 변경 26건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 사라진 판정 지점 |
| --- | --- | --- | --- |
| 앞 판본 | 140 | 26 | — |
| 이 run의 기준선 | 140 | 26 | cancelAlert:ALERT_NOT_FOUND, cancelAlert:CONCURRENT_UPDATE_CONFLICT, cancelAlert:INTERNAL_ERROR, cancelAlert:STORAGE_TIMEOUT, cancelAlert:UNAUTHENTICATED, cancelAlert:VALIDATION_FAILED, createAlert:ALERT_WINDOW_CLOSED, createAlert:CONCURRENT_UPDATE_CONFLICT, createAlert:COURT_NOT_SUPPORTED, createAlert:IDEMPOTENCY_KEY_REUSED, createAlert:INTERNAL_ERROR, createAlert:SLOT_NOT_SUPPORTED, createAlert:STORAGE_TIMEOUT, createAlert:UNAUTHENTICATED, createAlert:VALIDATION_FAILED, getAlert:ALERT_NOT_FOUND, getAlert:INTERNAL_ERROR, getAlert:STORAGE_TIMEOUT, getAlert:UNAUTHENTICATED, getAlert:VALIDATION_FAILED, getCourtAvailability:COURT_NOT_SUPPORTED, getCourtAvailability:INTERNAL_ERROR, getCourtAvailability:STORAGE_TIMEOUT, getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE, getCourtAvailability:UPSTREAM_TIMEOUT, getCourtAvailability:UPSTREAM_UNAVAILABLE, getCourtAvailability:VALIDATION_FAILED, listAlerts:INTERNAL_ERROR, listAlerts:STORAGE_TIMEOUT, listAlerts:UNAUTHENTICATED, listAlerts:VALIDATION_FAILED |
| 사람이 확정한 원본 | 140 | 26 | cancelAlert:ALERT_NOT_FOUND, cancelAlert:CONCURRENT_UPDATE_CONFLICT, cancelAlert:INTERNAL_ERROR, cancelAlert:STORAGE_TIMEOUT, cancelAlert:UNAUTHENTICATED, cancelAlert:VALIDATION_FAILED, createAlert:ALERT_WINDOW_CLOSED, createAlert:CONCURRENT_UPDATE_CONFLICT, createAlert:COURT_NOT_SUPPORTED, createAlert:IDEMPOTENCY_KEY_REUSED, createAlert:INTERNAL_ERROR, createAlert:SLOT_NOT_SUPPORTED, createAlert:STORAGE_TIMEOUT, createAlert:UNAUTHENTICATED, createAlert:VALIDATION_FAILED, getAlert:ALERT_NOT_FOUND, getAlert:INTERNAL_ERROR, getAlert:STORAGE_TIMEOUT, getAlert:UNAUTHENTICATED, getAlert:VALIDATION_FAILED, getCourtAvailability:COURT_NOT_SUPPORTED, getCourtAvailability:INTERNAL_ERROR, getCourtAvailability:STORAGE_TIMEOUT, getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE, getCourtAvailability:UPSTREAM_TIMEOUT, getCourtAvailability:UPSTREAM_UNAVAILABLE, getCourtAvailability:VALIDATION_FAILED, listAlerts:INTERNAL_ERROR, listAlerts:STORAGE_TIMEOUT, listAlerts:UNAUTHENTICATED, listAlerts:VALIDATION_FAILED |

판정 지점은 31개가 사라지고 2개가 생겼다. 사라진 지점은 앞으로 아무 장치도 보지 않는 자리다.

사라진 지점: `cancelAlert:ALERT_NOT_FOUND`, `cancelAlert:CONCURRENT_UPDATE_CONFLICT`, `cancelAlert:INTERNAL_ERROR`, `cancelAlert:STORAGE_TIMEOUT`, `cancelAlert:UNAUTHENTICATED`, `cancelAlert:VALIDATION_FAILED`, `createAlert:ALERT_WINDOW_CLOSED`, `createAlert:CONCURRENT_UPDATE_CONFLICT`, `createAlert:COURT_NOT_SUPPORTED`, `createAlert:IDEMPOTENCY_KEY_REUSED`, `createAlert:INTERNAL_ERROR`, `createAlert:SLOT_NOT_SUPPORTED`, `createAlert:STORAGE_TIMEOUT`, `createAlert:UNAUTHENTICATED`, `createAlert:VALIDATION_FAILED`, `getAlert:ALERT_NOT_FOUND`, `getAlert:INTERNAL_ERROR`, `getAlert:STORAGE_TIMEOUT`, `getAlert:UNAUTHENTICATED`, `getAlert:VALIDATION_FAILED`, `getCourtAvailability:COURT_NOT_SUPPORTED`, `getCourtAvailability:INTERNAL_ERROR`, `getCourtAvailability:STORAGE_TIMEOUT`, `getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE`, `getCourtAvailability:UPSTREAM_TIMEOUT`, `getCourtAvailability:UPSTREAM_UNAVAILABLE`, `getCourtAvailability:VALIDATION_FAILED`, `listAlerts:INTERNAL_ERROR`, `listAlerts:STORAGE_TIMEOUT`, `listAlerts:UNAUTHENTICATED`, `listAlerts:VALIDATION_FAILED`

생긴 지점: `createAlert:502`, `createAlert:504`

기준선 대비 달라진 좌표는 이렇다.

- `/security`
- `/tags`
- `/info/x-out-of-scope`
- `/info/description`
- `/paths/~1courts~1{courtId}~1availability/get/tags`
- `/paths/~1courts~1{courtId}~1availability/get/summary`
- `/paths/~1courts~1{courtId}~1availability/get/security`
- `/paths/~1courts~1{courtId}~1availability/get/x-requirement`
- `/paths/~1courts~1{courtId}~1availability/get/description`
- `/paths/~1courts~1{courtId}~1availability/get/parameters/1/description`
- `/paths/~1courts~1{courtId}~1availability/get/parameters/1/example`
- `/paths/~1courts~1{courtId}~1availability/get/responses/200/description`
- `/paths/~1courts~1{courtId}~1availability/get/responses/200/content/application~1json/examples`
- `/paths/~1alerts/post/tags`
- `/paths/~1alerts/post/summary`
- `/paths/~1alerts/post/x-requirement`
- `/paths/~1alerts/post/description`
- `/paths/~1alerts/post/requestBody/content/application~1json/example`
- `/paths/~1alerts/post/responses/502`
- `/paths/~1alerts/post/responses/504`
- `/paths/~1alerts/post/responses/201/description`
- `/paths/~1alerts/post/responses/201/content/application~1json/example`
- `/paths/~1alerts/post/responses/200/description`
- `/paths/~1alerts/post/responses/200/content/application~1json/example`
- `/paths/~1alerts/post/responses/503/$ref`
- `/paths/~1alerts/get/tags`
- `/paths/~1alerts/get/summary`
- `/paths/~1alerts/get/x-requirement`
- `/paths/~1alerts/get/description`
- `/paths/~1alerts/get/parameters/0/description`
- 그 밖에 110곳

모델이 붙인 호환성 라벨이 대조기 판정과 어긋난 자리다. 판정은 대조기를 따른다.

| 좌표 | 관찰 | iteration |
| --- | --- | --- |
| /components/responses/*/x-error-codes | 기록 cc_remove_unextractable_error_extensions은 breaking이라 적었는데 대조기 판정은 compatible다 | 002 |
| /security | 기록 cc_remove_unextractable_security_declarations은 breaking이라 적었는데 대조기 판정은 compatible다 | 002 |

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 5건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_trace_id_format | 계약이 traceId의 문자열 형식만 정하고 생성 규칙을 비워 둔 경우 어떤 식별자를 생성할 것인가? | UUID 문자열을 요청마다 생성 | convention | medium | 오류 응답 매퍼의 traceId 생성기만 로그 상관관계 시스템에 맞게 교체하면 된다. | getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, createAlert:503, listAlerts:500, listAlerts:503, getAlert:500, getAlert:503, cancelAlert:500, cancelAlert:503 | standing |
| d_auth_token_identity | 계약이 Bearer 토큰의 검증 방식과 사용자 식별자 형식을 정하지 않은 경우 토큰을 어떻게 사용자 식별에 사용할 것인가? | Bearer 뒤의 비어 있지 않은 토큰 문자열을 사용자 식별자로 사용 | least_harm | medium | Authentication을 실제 인증 검증 포트로 교체하고 컨트롤러가 전달하는 userId만 유지하면 된다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_upstream_failure_on_create | 신청 검증 중 외부 예약처 확인이 실패하고 createAlert에 upstream 전용 응답이 없을 때 어떤 코드로 매핑할 것인가? | INTERNAL_ERROR로 매핑 | least_harm | medium | 신청 검증용 포트 또는 응답이 추가되면 AlertService의 예외 변환만 변경한다. | createAlert:500, createAlert:422 | superseded |
| d_expiration_calculation | 더 이른 예약 마감 시각을 제공하는 포트가 없을 때 expiresAt을 어떻게 계산할 것인가? | 요청 시간대의 startTime을 Asia/Seoul 날짜 시각으로 사용 | spec_implication | high | 포트가 예약 마감 시각을 제공할 때 AlertService의 expiresAt 계산을 확장한다. | createAlert:201, createAlert:422, getAlert:200, listAlerts:200, cancelAlert:200 | standing |
| d_upstream_failure_surface | 신청 조건 검증 중 외부 예약처 확인이 실패할 때 어떤 계약 표면으로 원인과 재시도 의미를 보존할 것인가? | UPSTREAM_TIMEOUT, UPSTREAM_UNAVAILABLE, UPSTREAM_RESPONSE_UNREADABLE을 원인별로 보존하고 5xx와 Retry-After를 함께 반환 | spec_implication | high | 신청 검증 경로가 외부 확인을 제거하도록 바뀌면 해당 응답 표면과 매핑을 함께 제거한다. | createAlert:502, createAlert:503, createAlert:504, createAlert:422 | standing |

`d_trace_id_format` — 계약은 traceId를 문자열로만 요구하고 형식을 정하지 않았으므로 표준 JDK UUID를 사용한다. 로그 상관관계 인프라는 제공된 구현 계약의 범위에 없으므로 이 iteration에서는 UUID 생성 결정을 유지한다.

`d_auth_token_identity` — 계약은 인증 방식의 구체 형식을 정하지 않았고 구현에 필요한 것은 사용자별 소유권 구분이므로 추가 형식 제약 없이 토큰 문자열을 인메모리 식별자로 사용한다.

`d_upstream_failure_on_create` — createAlert에는 외부 확인 실패 응답이 선언되어 있지 않고 슬롯 지원 여부 확인이 필요하므로 성공이나 저장소 장애로 위장하지 않고 선언된 내부 실패로 전달한다.

`d_expiration_calculation` — 계약과 명세가 만료를 이용 시작 시각으로 정의하고 지원 시간대를 Asia/Seoul로 고정하며, 더 이른 마감 시각은 제공 포트에 없다.

`d_upstream_failure_surface` — ErrorCode가 외부 확인 실패를 별도 의미로 정의하고 EF-1이 확인 실패를 빈자리 없음과 구분하도록 요구하므로 INTERNAL_ERROR로 위장하지 않는다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 143건 중 7건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_006ed0 | request.operation_missing | cancelAlert | DELETE /alerts/{alertId} | open | — | 0 | 002 | — |
| v_008a21 | change.unjustified | — | /components/responses/StorageOrContentionOrUpstreamUnavailable | open | — | 0 | 002 | — |
| v_031838 | change.unjustified | — | /components/responses/AlertNotFound/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_07c0b9 | change.unjustified | — | /components/schemas/TimeSlot/properties/endTime/examples | open | — | 0 | 002 | — |
| v_07e8b7 | change.unjustified | — | /paths/~1alerts~1{alertId}/get/responses/200/content/application~1json/example | open | — | 0 | 002 | — |
| v_08c7b1 | change.unjustified | — | /paths/~1alerts~1{alertId}/get/x-requirement | open | — | 0 | 002 | — |
| v_0a1156 | change.unjustified | — | /components/schemas/TimeSlot/properties/startTime/examples | open | — | 0 | 002 | — |
| v_0e1eb0 | change.unjustified | — | /components/responses/StorageOrContentionUnavailable/x-error-codes | open | — | 0 | 002 | — |
| v_0e4694 | change.unjustified | — | /components/responses/Unauthenticated/x-error-codes | open | — | 0 | 002 | — |
| v_111463 | change.unjustified | — | /components/schemas/AlertDelivery/x-requirement | open | — | 0 | 002 | — |
| v_111abd | change.unjustified | — | /components/schemas/AlertId/description | open | — | 0 | 002 | — |
| v_12b4b8 | change.unjustified | — | /components/responses/AlertNotAcceptable/description | open | — | 0 | 002 | — |
| v_166e32 | change.unjustified | — | /components/responses/StorageUnavailable/description | open | — | 0 | 002 | — |
| v_17fada | change.unjustified | — | /paths/~1alerts~1{alertId}/delete/tags | open | — | 0 | 002 | — |
| v_186dce | change.unjustified | — | /components/responses/UpstreamUnreadable/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_1c1733 | change.unjustified | — | /components/securitySchemes | open | — | 0 | 002 | — |
| v_1c8f77 | change.unjustified | — | /components/schemas/Problem/properties/type/description | open | — | 0 | 002 | — |
| v_1dbe15 | change.unjustified | — | /components/responses/ValidationFailed/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_1dc3e1 | change.unjustified | — | /components/schemas/CourtAvailability/properties/reservationUrl/description | open | — | 0 | 002 | — |
| v_1fdea9 | change.unjustified | — | /components/responses/CourtNotSupported/description | open | — | 0 | 002 | — |
| v_221f18 | change.unjustified | — | /components/schemas/Problem/properties/instance/description | open | — | 0 | 002 | — |
| v_2255d0 | change.unjustified | — | /components/parameters/CourtIdPath/description | open | — | 0 | 002 | — |
| v_236a72 | change.unjustified | — | /components/responses/AlertNotFound/description | open | — | 0 | 002 | — |
| v_2500f2 | change.unjustified | — | /paths/~1alerts/get/summary | open | — | 0 | 002 | — |
| v_259184 | change.unjustified | — | /paths/~1alerts/get/parameters/0/description | open | — | 0 | 002 | — |
| v_25f715 | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/summary | open | — | 0 | 002 | — |
| v_26759f | change.unjustified | — | /components/responses/InternalError/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_26f447 | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/tags | open | — | 0 | 002 | — |
| v_271b1a | change.unjustified | — | /components/responses/CourtNotSupported/x-error-codes | open | — | 0 | 002 | — |
| v_285bca | change.unjustified | — | /components/schemas/CourtAvailability/properties/staleReason/description | open | — | 0 | 002 | — |
| v_339fcd | request.operation_missing | getAlert | GET /alerts/{alertId} | open | — | 0 | 002 | — |
| v_364dbc | change.unjustified | — | /paths/~1alerts~1{alertId}/delete/responses/200/description | open | — | 0 | 002 | — |
| v_39c170 | change.unjustified | — | /paths/~1alerts/get/responses/200/content/application~1json/example | open | — | 0 | 002 | — |
| v_3e0cd7 | change.unjustified | — | /components/responses/AvailabilityUnavailable/x-error-codes | open | — | 0 | 002 | — |
| v_3e717b | change.unjustified | — | /components/schemas/AlertList/properties/items/description | open | — | 0 | 002 | — |
| v_40bc55 | change.unjustified | — | /components/schemas/Alert/properties/lastCheckedAt/description | open | — | 0 | 002 | — |
| v_40fb15 | change.unjustified | — | /components/responses/StorageOrContentionUnavailable/description | open | — | 0 | 002 | — |
| v_465234 | change.unjustified | — | /info/x-out-of-scope | open | — | 0 | 002 | — |
| v_46993f | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/parameters/1/example | open | — | 0 | 002 | — |
| v_4964f5 | change.unjustified | — | /components/schemas/Alert/description | open | — | 0 | 002 | — |
| v_4ad07b | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/responses/200/content/application~1json/examples | open | — | 0 | 002 | — |
| v_503c8b | change.unjustified | — | /components/schemas/CourtAvailability/properties/confirmedAt/description | open | — | 0 | 002 | — |
| v_517d41 | change.unjustified | — | /components/schemas/CourtName/examples | open | — | 0 | 002 | — |
| v_539ab2 | change.unjustified | — | /components/schemas/AlertRequest/properties/date/description | open | — | 0 | 002 | — |
| v_53fbd2 | change.unjustified | — | /components/schemas/Alert/properties/reservationUrl/description | open | — | 0 | 002 | — |
| v_54cb18 | change.unjustified | — | /components/schemas/Problem/description | open | — | 0 | 002 | — |
| v_569cd7 | change.unjustified | — | /components/schemas/AlertDelivery/properties/lastAttemptAt/description | open | — | 0 | 002 | — |
| v_5829c8 | change.unjustified | — | /components/parameters/IdempotencyKeyHeader/description | open | — | 0 | 002 | — |
| v_5a2686 | change.unjustified | — | /components/schemas/CourtAvailability/x-requirement | open | — | 0 | 002 | — |
| v_5a2945 | request.operation_missing | getCourtAvailability | GET /courts/{courtId}/availability | open | — | 0 | 002 | — |
| v_5cb717 | change.unjustified | — | /components/responses/AlertRequestInvalid/content/application~1problem+json/examples | open | — | 0 | 002 | — |
| v_604e47 | change.unjustified | — | /components/schemas/Alert/x-requirement | open | — | 0 | 002 | — |
| v_64d1f5 | change.unjustified | — | /components/responses/ValidationFailed/description | open | — | 0 | 002 | — |
| v_6537bf | change.unjustified | — | /components/schemas/Problem/properties/retryAfterSeconds/description | open | — | 0 | 002 | — |
| v_6cba9b | change.unjustified | — | /paths/~1alerts/post/requestBody/content/application~1json/example | open | — | 0 | 002 | — |
| v_6d64aa | change.unjustified | — | /components/schemas/DeliveryStatus/description | open | — | 0 | 002 | — |
| v_6d6d0f | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/description | open | — | 0 | 002 | — |
| v_708b61 | change.unjustified | — | /paths/~1alerts/get/tags | open | — | 0 | 002 | — |
| v_712806 | change.unjustified | — | /paths/~1alerts~1{alertId}/delete/x-requirement | open | — | 0 | 002 | — |
| v_731a5a | change.unjustified | — | /components/responses/AlertRequestInvalid/x-error-codes | open | — | 0 | 002 | — |
| v_744f08 | change.unjustified | — | /components/schemas/Alert/properties/delivery/description | open | — | 0 | 002 | — |
| v_747eee | change.unjustified | — | /components/schemas/UpstreamFailureReason/description | open | — | 0 | 002 | — |
| v_749760 | change.unjustified | — | /paths/~1alerts~1{alertId}/get/summary | open | — | 0 | 002 | — |
| v_74a05d | change.unjustified | — | /paths/~1alerts/post/description | open | — | 0 | 002 | — |
| v_77df4b | change.unjustified | — | /components/schemas/Problem/properties/traceId/description | open | — | 0 | 002 | — |
| v_79e96d | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/x-requirement | open | — | 0 | 002 | — |
| v_7ad3f1 | change.unjustified | — | /components/headers/RetryAfter/example | open | — | 0 | 002 | — |
| v_7cfca5 | change.unjustified | — | /components/schemas/CourtId/description | open | — | 0 | 002 | — |
| v_801392 | change.unjustified | — | /components/responses/IdempotencyKeyReused/description | open | — | 0 | 002 | — |
| v_81e9cb | change.unjustified | — | /components/schemas/CourtName/description | open | — | 0 | 002 | — |
| v_823127 | change.unjustified | — | /components/schemas/AvailabilitySlot/properties/available/description | open | — | 0 | 002 | — |
| v_8439be | change.unjustified | — | /info/description | open | — | 0 | 002 | — |
| v_84b555 | change.unjustified | — | /components/responses/Unauthenticated/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_86c621 | change.unjustified | — | /components/schemas/ErrorCode/description | open | — | 0 | 002 | — |
| v_881936 | change.unjustified | — | /components/schemas/CourtAvailability/properties/slots/description | open | — | 0 | 002 | — |
| v_88f66f | change.unjustified | — | /components/responses/IdempotencyKeyReused/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_8b5706 | change.unjustified | — | /components/responses/AlertNotAcceptable/content/application~1problem+json/examples | open | — | 0 | 002 | — |
| v_8b6fae | change.unjustified | — | /components/responses/AlertNotFound/x-error-codes | open | — | 0 | 002 | — |
| v_8cb00d | change.unjustified | — | /components/schemas/TimeSlot/properties/endTime/description | open | — | 0 | 002 | — |
| v_8eb29f | change.unjustified | — | /paths/~1alerts~1{alertId}/get/tags | open | — | 0 | 002 | — |
| v_8f5aaa | change.unjustified | — | /components/responses/Unauthenticated/description | open | — | 0 | 002 | — |
| v_8fcffb | change.unjustified | — | /components/schemas/Alert/properties/checkDelayed/description | open | — | 0 | 002 | — |
| v_916b04 | change.unjustified | — | /paths/~1alerts/get/description | open | — | 0 | 002 | — |
| v_946e0f | change.unjustified | — | /paths/~1alerts/get/x-requirement | open | — | 0 | 002 | — |
| v_947777 | change.unjustified | — | /components/responses/UpstreamUnreadable/description | open | — | 0 | 002 | — |
| v_95015b | change.unjustified | — | /components/schemas/AlertDelivery/description | open | — | 0 | 002 | — |
| v_9688c6 | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/parameters/1/description | open | — | 0 | 002 | — |
| v_972155 | change.unjustified | — | /components/schemas/Problem/properties/status/description | open | — | 0 | 002 | — |
| v_9956f5 | change.unjustified | — | /components/parameters/IdempotencyKeyHeader/example | open | — | 0 | 002 | — |
| v_9b1b84 | change.unjustified | — | /components/responses/UpstreamTimeout/description | open | — | 0 | 002 | — |
| v_a2ea3e | change.unjustified | — | /paths/~1alerts~1{alertId}/delete/description | open | — | 0 | 002 | — |
| v_a4b679 | change.unjustified | — | /components/schemas/AlertDelivery/properties/attemptCount/description | open | — | 0 | 002 | — |
| v_a9f9c2 | change.unjustified | — | /components/headers/Location/description | open | — | 0 | 002 | — |
| v_ab06fa | change.unjustified | — | /components/schemas/AlertId/examples | open | — | 0 | 002 | — |
| v_adb3dd | change.unjustified | — | /components/schemas/TimeSlot/description | open | — | 0 | 002 | — |
| v_ade88d | change.unjustified | — | /components/schemas/Alert/properties/expiresAt/description | open | — | 0 | 002 | — |
| v_ae896b | change.unjustified | — | /components/responses/StorageUnavailable/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_ae9b96 | change.unjustified | — | /components/schemas/CourtId/examples | open | — | 0 | 002 | — |
| v_b00f99 | change.unjustified | — | /components/headers/IdempotencyReplayed/description | open | — | 0 | 002 | — |
| v_b08aad | change.unjustified | — | /components/headers/Location/example | open | — | 0 | 002 | — |
| v_b297b0 | change.unjustified | — | /components/responses/StorageUnavailable/x-error-codes | open | — | 0 | 002 | — |
| v_b52290 | change.unjustified | — | /paths/~1alerts/post/summary | open | — | 0 | 002 | — |
| v_b5d9c7 | change.unjustified | — | /paths/~1alerts~1{alertId}/delete/responses/200/content/application~1json/example | open | — | 0 | 002 | — |
| v_b6648d | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/security | open | — | 0 | 002 | — |
| v_b945ed | change.unjustified | — | /components/schemas/TimeSlot/properties/startTime/description | open | — | 0 | 002 | — |
| v_ba2c2b | change.unjustified | — | /components/responses/ValidationFailed/x-error-codes | open | — | 0 | 002 | — |
| v_bcd509 | change.unjustified | — | /components/schemas/Problem/properties/title/description | open | — | 0 | 002 | — |
| v_be677e | change.unjustified | — | /components/schemas/Problem/properties/detail/description | open | — | 0 | 002 | — |
| v_bebe21 | request.operation_missing | listAlerts | GET /alerts | open | — | 0 | 002 | — |
| v_bf96ff | change.unjustified | — | /components/responses/UpstreamTimeout/x-error-codes | open | — | 0 | 002 | — |
| v_bfc70b | change.unjustified | — | /tags | open | — | 0 | 002 | — |
| v_c15e3b | change.unjustified | — | /components/responses/AlertRequestInvalid/description | open | — | 0 | 002 | — |
| v_c5c9e8 | change.unjustified | — | /paths/~1alerts/post/tags | open | — | 0 | 002 | — |
| v_c66f95 | change.unjustified | — | /components/responses/AlertNotAcceptable/x-error-codes | open | — | 0 | 002 | — |
| v_c8cd1b | change.unjustified | — | /components/responses/AvailabilityUnavailable/content/application~1problem+json/examples | open | — | 0 | 002 | — |
| v_d13062 | change.unjustified | — | /components/headers/IdempotencyReplayed/example | open | — | 0 | 002 | — |
| v_d4cca1 | request.operation_missing | createAlert | POST /alerts | open | — | 0 | 002 | — |
| v_d5dcef | change.unjustified | — | /components/responses/UpstreamUnreadable/x-error-codes | open | — | 0 | 002 | — |
| v_d8b5b5 | change.unjustified | — | /paths/~1courts~1{courtId}~1availability/get/responses/200/description | open | — | 0 | 002 | — |
| v_e4eccf | change.unjustified | — | /components/schemas/AlertRequest/description | open | — | 0 | 002 | — |
| v_e5d353 | change.unjustified | — | /components/schemas/AlertDelivery/properties/failureReason/description | open | — | 0 | 002 | — |
| v_e5da43 | change.unjustified | — | /components/responses/InternalError/description | open | — | 0 | 002 | — |
| v_e5e755 | change.unjustified | — | /components/headers/RetryAfter/description | open | — | 0 | 002 | — |
| v_e6371c | change.unjustified | — | /components/schemas/CourtAvailability/properties/stale/description | open | — | 0 | 002 | — |
| v_e64284 | change.unjustified | — | /components/responses/IdempotencyKeyReused/x-error-codes | open | — | 0 | 002 | — |
| v_e713e2 | change.unjustified | — | /components/responses/AvailabilityUnavailable/description | open | — | 0 | 002 | — |
| v_ed3ec6 | change.unjustified | — | /components/schemas/AlertStatus/x-requirement | open | — | 0 | 002 | — |
| v_f10ba8 | change.unjustified | — | /components/responses/InternalError/x-error-codes | open | — | 0 | 002 | — |
| v_f32c1d | change.unjustified | — | /components/responses/CourtNotSupported/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_f7d576 | change.unjustified | — | /components/parameters/AlertIdPath/description | open | — | 0 | 002 | — |
| v_fc5f46 | change.unjustified | — | /paths/~1alerts~1{alertId}/delete/summary | open | — | 0 | 002 | — |
| v_fc81de | change.unjustified | — | /components/responses/UpstreamTimeout/content/application~1problem+json/example | open | — | 0 | 002 | — |
| v_fcfdda | change.unjustified | — | /components/responses/StorageOrContentionUnavailable/content/application~1problem+json/examples | open | — | 0 | 002 | — |
| v_fd5b83 | change.unjustified | — | /paths/~1alerts/post/x-requirement | open | — | 0 | 002 | — |
| v_fe21b4 | change.unjustified | — | /components/schemas/Problem/properties/retryable/description | open | — | 0 | 002 | — |
| v_ff5477 | change.unjustified | — | /paths/~1alerts~1{alertId}/get/description | open | — | 0 | 002 | — |
| v_3a7c6f | surface.path_missing | cancelAlert | /alerts/{alertId} | closed | code | 0 | 001 | 002 |
| v_3b6d22 | surface.path_missing | getAlert | /alerts/{alertId} | closed | code | 0 | 001 | 002 |
| v_42778f | extract.blind | — | x-error-codes | closed | code | 0 | 001 | 002 |
| v_805ddf | surface.path_missing | createAlert | /alerts | closed | code | 0 | 001 | 002 |
| v_a5a06b | surface.path_missing | listAlerts | /alerts | closed | code | 0 | 001 | 002 |
| v_a62dcf | surface.path_missing | getCourtAvailability | /courts/{courtId}/availability | closed | code | 0 | 001 | 002 |
| v_f5534b | extract.blind | — | security | closed | code | 0 | 001 | 002 |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_006ed0 | 계약이 선언한 오퍼레이션이 구현에 없다. 받겠다고 한 요청을 아예 받지 못한다. | — | — |
| v_008a21 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_031838 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_07c0b9 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_07e8b7 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_08c7b1 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_0a1156 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_0e1eb0 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_0e4694 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_111463 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_111abd | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_12b4b8 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_166e32 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_17fada | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_186dce | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_1c1733 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_1c8f77 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_1dbe15 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_1dc3e1 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_1fdea9 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_221f18 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_2255d0 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_236a72 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_2500f2 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_259184 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_25f715 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_26759f | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_26f447 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_271b1a | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_285bca | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_339fcd | 계약이 선언한 오퍼레이션이 구현에 없다. 받겠다고 한 요청을 아예 받지 못한다. | — | — |
| v_364dbc | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_39c170 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_3e0cd7 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_3e717b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_40bc55 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_40fb15 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_465234 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_46993f | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_4964f5 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_4ad07b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_503c8b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_517d41 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_539ab2 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_53fbd2 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_54cb18 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_569cd7 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_5829c8 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_5a2686 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_5a2945 | 계약이 선언한 오퍼레이션이 구현에 없다. 받겠다고 한 요청을 아예 받지 못한다. | — | — |
| v_5cb717 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_604e47 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_64d1f5 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_6537bf | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_6cba9b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_6d64aa | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_6d6d0f | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_708b61 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_712806 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_731a5a | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_744f08 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_747eee | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_749760 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_74a05d | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_77df4b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_79e96d | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_7ad3f1 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_7cfca5 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_801392 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_81e9cb | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_823127 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8439be | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_84b555 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_86c621 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_881936 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_88f66f | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8b5706 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8b6fae | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8cb00d | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8eb29f | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8f5aaa | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_8fcffb | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_916b04 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_946e0f | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_947777 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_95015b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_9688c6 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_972155 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_9956f5 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_9b1b84 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_a2ea3e | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_a4b679 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_a9f9c2 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ab06fa | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_adb3dd | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ade88d | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ae896b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ae9b96 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b00f99 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b08aad | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b297b0 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b52290 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b5d9c7 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b6648d | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_b945ed | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ba2c2b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_bcd509 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_be677e | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_bebe21 | 계약이 선언한 오퍼레이션이 구현에 없다. 받겠다고 한 요청을 아예 받지 못한다. | — | — |
| v_bf96ff | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_bfc70b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_c15e3b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_c5c9e8 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_c66f95 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_c8cd1b | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_d13062 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_d4cca1 | 계약이 선언한 오퍼레이션이 구현에 없다. 받겠다고 한 요청을 아예 받지 못한다. | — | — |
| v_d5dcef | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_d8b5b5 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e4eccf | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e5d353 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e5da43 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e5e755 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e6371c | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e64284 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_e713e2 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ed3ec6 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_f10ba8 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_f32c1d | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_f7d576 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_fc5f46 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_fc81de | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_fcfdda | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_fd5b83 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_fe21b4 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_ff5477 | 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다. | — | — |
| v_3a7c6f | 계약이 선언한 경로가 구현에 없다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_3b6d22 | 계약이 선언한 경로가 구현에 없다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_42778f | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_805ddf | 계약이 선언한 경로가 구현에 없다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_a5a06b | 계약이 선언한 경로가 구현에 없다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_a62dcf | 계약이 선언한 경로가 구현에 없다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_f5534b | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/AlertController.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/AvailabilityController.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 12건이다.

### 축 판정의 어긋남 (5건)

Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다.

| iteration | axis | critique_id | eval_score | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | failure_faithfulness | w_create_upstream_failure_mapping | 5 | Critique는 이 축을 high로 지적했는데 Eval은 5를 줬다 |
| 002 | failure_faithfulness | cr_error_code_mapping_erased | 5 | Critique는 이 축을 high로 지적했는데 Eval은 5를 줬다 |
| 002 | judgement_disclosure | cr_security_contract_erased | 4 | Critique는 이 축을 high로 지적했는데 Eval은 4를 줬다 |
| 002 | judgement_disclosure | cr_untracked_contract_churn | 4 | Critique는 이 축을 high로 지적했는데 Eval은 4를 줬다 |
| 002 | failure_faithfulness | cr_trace_id_not_correlated | 5 | Critique는 이 축을 high로 지적했는데 Eval은 5를 줬다 |

### 게이트가 세는 것을 다시 말한 지적 (7건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다.

| iteration | axis | critique_id | rule | 관찰 |
| --- | --- | --- | --- | --- |
| 001 | request_tolerance | w_route_base_path_duplication | surface.path_missing | 게이트가 surface.path_missing로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | failure_faithfulness | w_create_upstream_failure_mapping | surface.path_missing | 게이트가 surface.path_missing로 이미 세는 자리를 비평이 다시 말했다 |
| 001 | judgement_disclosure | w_trace_id_correlation_undisclosed | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | failure_faithfulness | cr_error_code_mapping_erased | change.unjustified | 게이트가 change.unjustified로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | judgement_disclosure | cr_security_contract_erased | change.unjustified | 게이트가 change.unjustified로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | judgement_disclosure | cr_untracked_contract_churn | decision.unknown_point | 게이트가 decision.unknown_point로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | failure_faithfulness | cr_trace_id_not_correlated | change.unjustified | 게이트가 change.unjustified로 이미 세는 자리를 비평이 다시 말했다 |

### 게이트가 남긴 기록 (69건)

계약이 말하지 않은 자리의 초과다. REJECT 사유가 아니고 뜻은 Critique가 붙인다.

| 규칙 | 판정 지점 | 좌표 | iteration | 관찰 |
| --- | --- | --- | --- | --- |
| gate:surface.path_extra | — | /v1/alerts | 001 | 계약에 없는 경로다 |
| gate:surface.path_extra | — | /v1/alerts/{alertId} | 001 | 계약에 없는 경로다 |
| gate:surface.path_extra | — | /v1/courts/{courtId}/availability | 001 | 계약에 없는 경로다 |
| gate:decision.unknown_point | authentication:user_identity | src/main/java/com/thinking/tennis/api/Authentication.java | 001 | 계약의 판정 지점이 아니다 (결정 d_auth_token_identity) |
| gate:decision.unknown_point | failure_response:traceId | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 001 | 계약의 판정 지점이 아니다 (결정 d_trace_id_format) |
| gate:decision.unknown_point | authentication:user_identity | src/main/java/com/thinking/tennis/api/Authentication.java | 002 | 계약의 판정 지점이 아니다 (결정 d_auth_token_identity) |
| gate:decision.unknown_point | failure_response:traceId | src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 002 | 계약의 판정 지점이 아니다 (결정 d_trace_id_format) |
| gate:change.unmatched_record | — | /components/responses/*/x-error-codes | 002 | 기록 cc_remove_unextractable_error_extensions이 가리키는 자리가 판본 diff에 없다 |
| gate:change.diff | — | * | 002 | 달라진 좌표 140개: /security, /tags, /info/x-out-of-scope, /info/description, /paths/~1courts~1{courtId}~1availability/get/tags, /paths/~1courts~1{courtId}~1availability/get/summary, /paths/~1courts~1{courtId}~1availability/get/security, /paths/~1courts~1{courtId}~1availability/get/x-requirement, /paths/~1courts~1{courtId}~1availability/get/description, /paths/~1courts~1{courtId}~1availability/get/parameters/1/description, /paths/~1courts~1{courtId}~1availability/get/parameters/1/example, /paths/~1courts~1{courtId}~1availability/get/responses/200/description, /paths/~1courts~1{courtId}~1availability/get/responses/200/content/application~1json/examples, /paths/~1alerts/post/tags, /paths/~1alerts/post/summary, /paths/~1alerts/post/x-requirement, /paths/~1alerts/post/description, /paths/~1alerts/post/requestBody/content/application~1json/example, /paths/~1alerts/post/responses/502, /paths/~1alerts/post/responses/504, /paths/~1alerts/post/responses/201/description, /paths/~1alerts/post/responses/201/content/application~1json/example, /paths/~1alerts/post/responses/200/description, /paths/~1alerts/post/responses/200/content/application~1json/example, /paths/~1alerts/post/responses/503/$ref, /paths/~1alerts/get/tags, /paths/~1alerts/get/summary, /paths/~1alerts/get/x-requirement, /paths/~1alerts/get/description, /paths/~1alerts/get/parameters/0/description, /paths/~1alerts/get/responses/200/content/application~1json/example, /paths/~1alerts~1{alertId}/get/tags, /paths/~1alerts~1{alertId}/get/summary, /paths/~1alerts~1{alertId}/get/x-requirement, /paths/~1alerts~1{alertId}/get/description, /paths/~1alerts~1{alertId}/get/responses/200/content/application~1json/example, /paths/~1alerts~1{alertId}/delete/tags, /paths/~1alerts~1{alertId}/delete/summary, /paths/~1alerts~1{alertId}/delete/x-requirement, /paths/~1alerts~1{alertId}/delete/description |
| gate:change.breaking | — | getCourtAvailability.400 | 002 | getCourtAvailability.400: 선언했던 에러 코드가 사라졌다 ['VALIDATION_FAILED'] |
| gate:change.breaking | — | getCourtAvailability.404 | 002 | getCourtAvailability.404: 선언했던 에러 코드가 사라졌다 ['COURT_NOT_SUPPORTED'] |
| gate:change.breaking | — | getCourtAvailability.500 | 002 | getCourtAvailability.500: 선언했던 에러 코드가 사라졌다 ['INTERNAL_ERROR'] |
| gate:change.breaking | — | getCourtAvailability.502 | 002 | getCourtAvailability.502: 선언했던 에러 코드가 사라졌다 ['UPSTREAM_RESPONSE_UNREADABLE'] |
| gate:change.breaking | — | getCourtAvailability.503 | 002 | getCourtAvailability.503: 선언했던 에러 코드가 사라졌다 ['UPSTREAM_UNAVAILABLE', 'STORAGE_TIMEOUT'] |
| gate:change.breaking | — | getCourtAvailability.504 | 002 | getCourtAvailability.504: 선언했던 에러 코드가 사라졌다 ['UPSTREAM_TIMEOUT'] |
| gate:change.breaking | — | createAlert.400 | 002 | createAlert.400: 선언했던 에러 코드가 사라졌다 ['VALIDATION_FAILED'] |
| gate:change.breaking | — | createAlert.401 | 002 | createAlert.401: 선언했던 에러 코드가 사라졌다 ['UNAUTHENTICATED'] |
| gate:change.breaking | — | createAlert.409 | 002 | createAlert.409: 선언했던 에러 코드가 사라졌다 ['IDEMPOTENCY_KEY_REUSED'] |
| gate:change.breaking | — | createAlert.422 | 002 | createAlert.422: 선언했던 에러 코드가 사라졌다 ['COURT_NOT_SUPPORTED', 'SLOT_NOT_SUPPORTED', 'ALERT_WINDOW_CLOSED'] |
| gate:change.breaking | — | createAlert.500 | 002 | createAlert.500: 선언했던 에러 코드가 사라졌다 ['INTERNAL_ERROR'] |
| gate:change.breaking | — | createAlert.503 | 002 | createAlert.503: 선언했던 에러 코드가 사라졌다 ['STORAGE_TIMEOUT', 'CONCURRENT_UPDATE_CONFLICT'] |
| gate:change.breaking | — | listAlerts.400 | 002 | listAlerts.400: 선언했던 에러 코드가 사라졌다 ['VALIDATION_FAILED'] |
| gate:change.breaking | — | listAlerts.401 | 002 | listAlerts.401: 선언했던 에러 코드가 사라졌다 ['UNAUTHENTICATED'] |
| gate:change.breaking | — | listAlerts.500 | 002 | listAlerts.500: 선언했던 에러 코드가 사라졌다 ['INTERNAL_ERROR'] |
| gate:change.breaking | — | listAlerts.503 | 002 | listAlerts.503: 선언했던 에러 코드가 사라졌다 ['STORAGE_TIMEOUT'] |
| gate:change.breaking | — | getAlert.400 | 002 | getAlert.400: 선언했던 에러 코드가 사라졌다 ['VALIDATION_FAILED'] |
| gate:change.breaking | — | getAlert.401 | 002 | getAlert.401: 선언했던 에러 코드가 사라졌다 ['UNAUTHENTICATED'] |
| gate:change.breaking | — | getAlert.404 | 002 | getAlert.404: 선언했던 에러 코드가 사라졌다 ['ALERT_NOT_FOUND'] |
| gate:change.breaking | — | getAlert.500 | 002 | getAlert.500: 선언했던 에러 코드가 사라졌다 ['INTERNAL_ERROR'] |
| gate:change.breaking | — | getAlert.503 | 002 | getAlert.503: 선언했던 에러 코드가 사라졌다 ['STORAGE_TIMEOUT'] |
| gate:change.breaking | — | cancelAlert.400 | 002 | cancelAlert.400: 선언했던 에러 코드가 사라졌다 ['VALIDATION_FAILED'] |
| gate:change.breaking | — | cancelAlert.401 | 002 | cancelAlert.401: 선언했던 에러 코드가 사라졌다 ['UNAUTHENTICATED'] |
| gate:change.breaking | — | cancelAlert.404 | 002 | cancelAlert.404: 선언했던 에러 코드가 사라졌다 ['ALERT_NOT_FOUND'] |
| gate:change.breaking | — | cancelAlert.500 | 002 | cancelAlert.500: 선언했던 에러 코드가 사라졌다 ['INTERNAL_ERROR'] |
| gate:change.breaking | — | cancelAlert.503 | 002 | cancelAlert.503: 선언했던 에러 코드가 사라졌다 ['STORAGE_TIMEOUT', 'CONCURRENT_UPDATE_CONFLICT'] |
| gate:change.point_churn | cancelAlert:ALERT_NOT_FOUND | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | cancelAlert:CONCURRENT_UPDATE_CONFLICT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | cancelAlert:INTERNAL_ERROR | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | cancelAlert:STORAGE_TIMEOUT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | cancelAlert:UNAUTHENTICATED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | cancelAlert:VALIDATION_FAILED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:ALERT_WINDOW_CLOSED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:CONCURRENT_UPDATE_CONFLICT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:COURT_NOT_SUPPORTED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:IDEMPOTENCY_KEY_REUSED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:INTERNAL_ERROR | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:SLOT_NOT_SUPPORTED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:STORAGE_TIMEOUT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:UNAUTHENTICATED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:VALIDATION_FAILED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getAlert:ALERT_NOT_FOUND | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getAlert:INTERNAL_ERROR | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getAlert:STORAGE_TIMEOUT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getAlert:UNAUTHENTICATED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getAlert:VALIDATION_FAILED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:COURT_NOT_SUPPORTED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:INTERNAL_ERROR | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:STORAGE_TIMEOUT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:UPSTREAM_TIMEOUT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:UPSTREAM_UNAVAILABLE | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | getCourtAvailability:VALIDATION_FAILED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | listAlerts:INTERNAL_ERROR | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | listAlerts:STORAGE_TIMEOUT | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | listAlerts:UNAUTHENTICATED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | listAlerts:VALIDATION_FAILED | - | 002 | 판정 지점이 사라졌다 |
| gate:change.point_churn | createAlert:502 | - | 002 | 판정 지점이 생겼다 |
| gate:change.point_churn | createAlert:504 | - | 002 | 판정 지점이 생겼다 |
| gate:change.version_label | — | tennis-alert-api-v2.yaml | 002 | 파일명의 판 번호 2과 계약의 주 버전 1이 어긋난다 |
