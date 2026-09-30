# 계약 적합성 run 리포트 — 2026-09-30_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T18:45:55+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

**기계 판정이 깨끗한 iteration은 없다.** 다만 001, 002, 003은 판정까지는 갔고 위반이 남아 걸렸다. iteration 001은 여기서 걸렸다: G1이 REJECT다 — 위반이 있다.

판정은 **REJECT**이고 iteration 003까지 왔다. 위반 8건 가운데 2건을 코드로 닫고 0건을 계약으로 닫았으며 6건이 아직 열려 있다. 서 있는 결정은 21건, 계약 변경 기록은 13건이다.

위반은 지점 단위로 센 것이고 그 뒤의 원인은 8개다. 고칠 일의 개수는 원인 쪽이므로 5절을 원인으로 묶어 함께 낸다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v3.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v2.yaml`이다.

`decision_risk`는 **medium** 수준이다. 올릴 근거는 없다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:change.unjustified@None: 기록 cc_watching_check_source이 인용한 'cr_watching_says_periodic_check'가 이 iteration의 비평에 없다
- gate:change.unjustified@None: 기록 cc_create_schedule_source이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다
- gate:change.unjustified@None: 기록 cc_create_upstream_unreadable이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다
- gate:change.unjustified@None: 기록 cc_create_upstream_unreadable은 표현 변경인데 representation_basis가 비었다 (닫힌 값: interop, spec_implication, contract_consistency)
- gate:change.unjustified@None: 기록 cc_create_upstream_timeout이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다
- gate:change.unjustified@None: 기록 cc_create_upstream_timeout은 표현 변경인데 representation_basis가 비었다 (닫힌 값: interop, spec_implication, contract_consistency)
- gate:change.unjustified@None: 기록 cc_create_503_covers_upstream이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다
- gate:change.unjustified@None: 기록 cc_alert_create_unavailable_response이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다
- gate:change.unjustified@None: 기록 cc_alert_create_unavailable_response은 표현 변경인데 representation_basis가 비었다 (닫힌 값: interop, spec_implication, contract_consistency)

루브릭:
- min_total: 3.4 < 4.2
- min_axis.response_fidelity: 3 < 4.0
- min_axis.state_continuity: 3 < 4.0
- min_axis.judgement_disclosure: 3 < 4.0

색인과 실물이 어긋난 경로가 84곳이다. 게이트는 색인이 선언한 것을 판정하므로 선언 밖에 놓인 파일은 판정되지 않은 표면이 된다.

| 경로 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/ApiPaths.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityController.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/BearerTokens.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/OpenApiDeclaration.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/Problems.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidationException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValues.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/UnauthenticatedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AlertDeliveryResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AvailabilitySlotResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/CreateAlertRequest.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/DeliveryStatusValue.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/ErrorCodeValue.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/Payloads.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/ProblemResponse.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/TimeSlotPayload.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/UpstreamFailureReasonValue.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertCondition.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertExpiryJob.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertQueryService.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/ApplicationConfig.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityView.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/IdempotencyStore.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/WatchPolicy.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/AlertNotFoundException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/AlertWindowClosedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/AvailabilityUnavailableException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/ConcurrentUpdateException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/IdempotencyKeyReusedException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/StorageTimeoutException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/UnsupportedCourtException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/store/InMemoryAvailabilityRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/store/InMemoryIdempotencyStore.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertDelivery.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertStatus.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DeliveryStatus.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/TimeSlot.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/UpstreamFailureReason.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| contract/tennis-alert-api-v2.yaml | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiPaths.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityController.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/BearerTokens.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/OpenApiDeclaration.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/Problems.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidationException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValues.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/UnauthenticatedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AlertDeliveryResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AlertListResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AlertResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AlertStatusValue.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/AvailabilitySlotResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/CreateAlertRequest.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/DeliveryStatusValue.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/ErrorCodeValue.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/Payloads.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/ProblemResponse.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/TimeSlotPayload.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/dto/UpstreamFailureReasonValue.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertCondition.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertExpiryJob.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertQueryService.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertView.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/ApplicationConfig.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/WatchPolicy.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/AlertNotFoundException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/AlertWindowClosedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/AvailabilityUnavailableException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/ConcurrentUpdateException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/IdempotencyKeyReusedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/ScheduleUnknownException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/SlotNotSupportedException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/StorageTimeoutException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/error/UnsupportedCourtException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/store/InMemoryAlertRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/store/InMemoryAvailabilityRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertDelivery.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DeliveryStatus.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/TimeSlot.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/UpstreamFailureReason.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |

합격선은 두 문장이 함께 참인 상태다. **판정했다**와 **그리고 위반이 없다**를 같은 칸에 넣지 않는다. 뒤의 문장만 적으면 게이트를 부수는 것이 게이트를 통과하는 가장 쉬운 길이 된다.

| iteration | 판정 | 판정했다 | 위반 없다 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | 예 | 아니다 | PASS | REJECT | PASS | 2 | 31 | 4.2 |
| 002 | REJECT | 예 | 아니다 | PASS | PASS | REJECT | 3 | 41 | 3.65 |
| 003 | REJECT | 예 | 아니다 | PASS | PASS | REJECT | 9 | 41 | 3.4 |

합격선을 넘지 못한 자리마다 무엇에 걸렸는지 적는다.

| iteration | 무엇에 걸렸는가 |
| --- | --- |
| 001 | G1이 REJECT다 — 위반이 있다 |
| 002 | 판본 대조가 REJECT다 — 위반이 있다 |
| 003 | 판본 대조가 REJECT다 — 위반이 있다 |

## 2. 계약을 이렇게 고쳤다

계약이 어느 좌표에서 무엇 때문에 움직였는지 적는다. 약속을 줄인 변경이 위로 온다.

변경은 13건이고 그중 0건이 계약의 약속을 줄였다. 대조기 판정으로 깨는 변경은 0건이다. 호환성은 모델의 라벨이 아니라 대조기가 정하므로 두 값을 나란히 싣는다.

약속 변경과 표현 변경을 가르는 것은 판정 지점과 필수와 에러 코드 쌍과 산문 슬롯이다. 그 가운데 하나라도 줄면 약속 변경이고, 이름과 설명만 달라진 것은 표현 변경이다. 지운 약속은 이후 어느 장치도 다시 보지 않으므로 이 절의 맨 위에 둔다.

| id | 종류 | 좌표 | 무엇 | 왜 | 발의 | 명세 근거 | 라벨 | 대조기 | iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| cc_alert_create_unavailable_response | representation | /components/responses/AlertCreateUnavailable | 신청 오퍼레이션의 503 응답을 새 components 항목으로 더했다. x-error-codes가 UPSTREAM_UNAVAILABLE·STORAGE_TIMEOUT·CONCURRENT_UPDATE_CONFLICT이고 예시 셋을 담는다. | 해제 오퍼레이션이 쓰는 StorageOrContentionUnavailable에 예약처 실패를 더하면 예약처를 부르지 않는 오퍼레이션까지 그 코드를 선언하게 된다. 신청 전용 응답을 따로 두어 오퍼레이션마다 선언이 사실과 같게 한다. | critique:cr_create_upstream_failure | EF-1, FR-1 | compatible | compatible | 002 |
| cc_create_503_covers_upstream | representation | /paths/~1alerts/post/responses/503 | 503이 가리키는 응답을 StorageOrContentionUnavailable에서 AlertCreateUnavailable로 바꿨다. 같은 상태 코드가 예약처에 닿지 못한 실패까지 덮는다. | 예약처에 닿지 못한 실패는 조회에서 503이고, 이 오퍼레이션의 503이 이미 저장소와 경합을 덮고 있어 에러 코드만 합치면 된다. 상태 코드를 새로 만들지 않고 코드로 원인을 가른다. | critique:cr_create_upstream_failure | EF-1, FR-3 | compatible | compatible | 002 |
| cc_create_schedule_source | representation | /paths/~1alerts/post/description | 운영 시간대를 그 코트·날짜의 저장된 확인 결과로 판정하고, 저장된 결과가 확인 간격보다 오래됐거나 없으면 이 요청이 확인을 일으키며, 그 확인이 실패하고 저장된 성공 결과도 없으면 예약 상태 조회와 같은 조건으로 502·503·504 중 하나를 돌려준다는 문단을 더했다. | 계약은 SLOT_NOT_SUPPORTED로 거절하라고 하면서 그 판정의 근거와 근거를 얻지 못했을 때의 응답을 정하지 않았다. 운영 시간대의 유일한 출처는 확인 결과이고 그 확인은 실패할 수 있으므로, 판정 근거와 실패 시의 답을 계약이 말해야 한다. | critique:cr_create_upstream_failure | FR-1, FR-3, EF-1 | compatible | compatible | 002 |
| cc_create_upstream_timeout | representation | /paths/~1alerts/post/responses/504 | 예약 상태 조회가 쓰는 UpstreamTimeout 응답을 504로 더했다. | 예약처가 제때 응답하지 않아 운영 시간대를 확인하지 못한 경우를 조회와 같은 상태 코드로 알린다. | critique:cr_create_upstream_failure | EF-1 | compatible | compatible | 002 |
| cc_create_upstream_unreadable | representation | /paths/~1alerts/post/responses/502 | 예약 상태 조회가 쓰는 UpstreamUnreadable 응답을 502로 더했다. | 확인이 실패했고 저장된 마지막 성공 결과도 없을 때 이 오퍼레이션에는 그 사실을 말할 코드가 없었다. 조회와 같은 조건이므로 같은 응답을 쓴다. | critique:cr_create_upstream_failure | EF-1 | compatible | compatible | 002 |
| cc_watching_check_source | representation | /components/schemas/AlertStatus/description | WATCHING 항목의 둘째 문장에서 '서버가 주기적으로 예약 상태를 확인한다'를 지우고, 확인은 예약 상태 조회가 일으키며 아무도 조회하지 않으면 확인도 일어나지 않고 그 상태가 checkDelayed로 드러난다는 문장으로 바꿨다. | 명세는 '확인을 일으키는 것은 조회뿐이다. 감시 중인 신청만 보고 예약처를 도는 주기 작업은 두지 않아, 아무도 조회하지 않는 코트·날짜는 확인도 알림도 일어나지 않는다'로 정했다. 계약의 문장이 그 반대를 말해 둘 중 하나가 틀린 상태였고, 출처가 명세이므로 계약을 고쳤다. | critique:cr_watching_says_periodic_check | FR-7, FR-3 | compatible | compatible | 002 |
| cc_create_201_example_last_checked | representation | /paths/~1alerts/post/responses/201/content/application~1json/example | 201 예시의 lastCheckedAt을 null에서 createdAt 직전의 타임스탬프로 바꿨다. delivery는 null로 두었다. 그 예시의 시간대는 예약 상태 조회 예시에서 비어 있지 않은 자리라 두 예시가 같은 사실을 말한다. | 확인을 일으킨 신청은 그 확인이 성공한 시각을 바로 갖게 되므로 널을 그리는 예시는 오히려 드문 경우가 됐고, 예시를 계약으로 읽은 클라이언트가 이 필드를 '신청 직후에는 언제나 널'로 다루게 된다. | critique:cr_create_201_example_last_checked | FR-3, NFR-3 | compatible | compatible | 003 |
| cc_create_201_state_note | representation | /paths/~1alerts/post/responses/201/description | 201 설명을 한 줄에서 세 줄로 늘려, lastCheckedAt이 이 요청이 일으킨 확인의 시각이거나 확인 간격 안의 저장된 결과의 시각이고 한 번도 확인에 성공하지 못한 코트·날짜에서만 null이라는 것과, 판정에 쓴 결과에 그 시간대가 비어 있었으면 delivery가 곧바로 PENDING이라는 것을 적었다. | 신청이 확인을 일으키도록 계약이 바뀌었는데 이 응답의 설명은 그 전 그대로였다. 예시 하나가 그리는 모양을 계약으로 읽는 것을 막으려면 어느 경우에 무엇이 오는지를 설명이 말해야 한다. | critique:cr_create_201_example_last_checked | FR-3, NFR-3 | compatible | compatible | 003 |
| cc_create_delivery_at_birth | representation | /paths/~1alerts/post/description | 판정에 쓴 확인 결과에 그 시간대가 비어 있으면 응답의 delivery가 곧바로 PENDING이고, 아니면 null이며 이후 확인에서 빈자리가 드러날 때 대기에 오른다는 문단을 더했다. 계약이 둘 중 하나를 고르라고 한 자리에서 앞쪽을 골랐다. | 신청이 확인을 일으키게 된 뒤로 서버는 신청을 만드는 그 순간 그 시간대가 비어 있는지를 알고 있는데, 계약이 그 사실을 새 신청에 적용하는지를 말하지 않아 두 읽기가 다 가능했다. 뒤쪽을 고르면 이미 비어 있는 자리의 대기가 다음 확인까지, 아무도 조회하지 않으면 영원히 오르지 않는다. | critique:cr_create_delivery_at_birth | FR-1, FR-4 | compatible | compatible | 003 |
| cc_create_unopened_date_accepted | representation | /paths/~1alerts/post/description | 판정에 쓴 결과의 slots가 비어 있으면 시간대 판정을 미루고 신청을 받으며, 이후 확인에서 운영하지 않는 시간대로 드러나면 감시만 계속되고 알림이 나가지 않는다는 문단을 더했다. | slots의 산문을 고쳤으면 그 규칙이 이 오퍼레이션에서 무엇을 뜻하는지도 같은 판본이 말해야 한다. 아직 열리지 않은 날짜와 운영하지 않는 시간대를 같은 코드로 거절하면 사용자는 나중에 열릴 날짜를 포기한다. | critique:cr_unopened_date_cannot_subscribe | FR-1, FR-7, NFR-1 | compatible | compatible | 003 |
| cc_dedup_scoped_to_user | representation | /paths/~1alerts/post/description | 중복 신청 문단의 주어를 '같은 사용자의 감시 중인 신청'과 '같은 사용자의 이미 끝난 신청'으로 좁히고, 남이 같은 조건을 감시 중인 것은 이 판정에 들지 않으며 돌려줄 수도 없다는 문장을 더했다. | 주어가 없는 문장을 문자 그대로 읽으면 남의 신청을 돌려주라는 뜻이 되고, 그러면 alertId와 createdAt과 expiresAt이 남의 자원을 가리켜 FR-2의 '남의 신청은 조회·해제할 수 없다'와 정면으로 부딪친다. FR-1의 원문과 같은 범위로 좁혔다. | critique:cr_dedup_omits_user_scope | FR-1, FR-2 | compatible | compatible | 003 |
| cc_replayed_header_on_dedup | representation | /components/headers/IdempotencyReplayed/description | 같은 조건을 감시 중인 신청을 돌려주는 200은 이번 요청이 판정한 결과이므로 false이고, true는 같은 키로 앞서 보관한 응답을 그대로 돌려줄 때만이라는 두 문장을 더했다. | 헤더의 정의가 그 200을 어느 쪽으로 다루는지 말하지 않아 두 읽기가 다 가능했고, 계약이 비어 있는 한 같은 계약에서 두 서버가 다른 헤더를 낸다. | critique:cr_replayed_header_on_dedup_200 | FR-1, V-1 | compatible | compatible | 003 |
| cc_unopened_date_not_rejected | representation | /components/schemas/CourtAvailability/properties/slots/description | '여기에 없는 시간대로 신청하면 SLOT_NOT_SUPPORTED로 거절된다'는 규칙의 적용 범위를 slots가 비어 있지 않은 경우로 한정하고, 빈 배열은 운영 시간대를 아직 판정할 수 없다는 뜻이라 그 코드로 거절되지 않는다는 문장을 더했다. | 두 문장을 나란히 읽으면 예약처가 열지 않은 날짜에는 어떤 시간대로도 신청할 수 없다는 뜻이 되는데, 예약이 열리기 전에 미리 걸어 두는 것이 이 서비스의 주된 쓰임이고 NFR-1도 피크를 예약 오픈 시각으로 잡았다. 계약이 자기 목적을 막는 자리였다. | critique:cr_unopened_date_cannot_subscribe | FR-1, FR-7, NFR-1 | compatible | compatible | 003 |

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 10곳이 달라지고 깨는 변경 0건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 10곳이 달라지고 깨는 변경 0건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 대조하지 못한 자리 | 사라진 판정 지점 |
| --- | --- | --- | --- | --- |
| 앞 판본 | 5 | 0 | 0 | — |
| 이 run의 기준선 | 10 | 0 | 0 | — |
| 사람이 확정한 원본 | 10 | 0 | 0 | — |

판정 지점은 0개가 사라지고 5개가 생겼다. 사라진 지점은 앞으로 아무 장치도 보지 않는 자리다.

생긴 지점: `createAlert:502`, `createAlert:504`, `createAlert:UPSTREAM_RESPONSE_UNREADABLE`, `createAlert:UPSTREAM_TIMEOUT`, `createAlert:UPSTREAM_UNAVAILABLE`

기준선 대비 달라진 좌표는 이렇다.

- `/paths/~1alerts/post/description`
- `/paths/~1alerts/post/responses/502`
- `/paths/~1alerts/post/responses/504`
- `/paths/~1alerts/post/responses/201/description`
- `/paths/~1alerts/post/responses/201/content/application~1json/example/lastCheckedAt`
- `/paths/~1alerts/post/responses/503/$ref`
- `/components/headers/IdempotencyReplayed/description`
- `/components/schemas/AlertStatus/description`
- `/components/schemas/CourtAvailability/properties/slots/description`
- `/components/responses/AlertCreateUnavailable`

같은 좌표를 여러 번 흔든 자리가 있다. 계약의 공백일 수 있으므로 slow loop의 재료가 된다.

| 좌표 | 관찰 |
| --- | --- |
| /paths/~1alerts/post/description | 같은 계약 좌표를 2번 바꿨다 (iteration 002, 003) |

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 23건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_authentication_before_validation | 토큰이 없고 요청 형식도 어긴 요청에 401과 400 중 무엇을 먼저 내는가. | 본문을 값으로 옮기는 데 성공한 뒤부터 식별을 먼저 한다. 본문을 읽지 못한 실패와 본문의 미디어 타입 실패는 식별보다 앞서 400 VALIDATION_FAILED로 나간다. | convention | medium | 각 컨트롤러 메서드에서 식별과 검증 두 줄의 순서를 바꾸면 된다. 완전한 관철은 필터를 더해야 한다. | createAlert:400, createAlert:401, listAlerts:400, listAlerts:401, getAlert:400, getAlert:401, cancelAlert:400, cancelAlert:401 | standing |
| d_create_may_trigger_check | 신청 조건을 판정할 운영 시간대를 어디서 얻는가. 저장된 결과가 없으면 확인을 일으키는가. | 저장된 결과를 먼저 쓰고, 확인 간격보다 오래됐거나 한 번도 없으면 조회와 같은 경로로 확인을 일으킨다. 확인이 실패해도 저장된 마지막 성공 결과가 있으면 그 결과로 판정하며, 그 결과의 나이에는 상한을 두지 않는다. | spec_implication | medium | AlertCommandService가 부르는 조회 메서드를 저장된 결과만 읽는 것으로 바꾸면 되고, 그때 확인 결과가 없을 때의 응답을 다시 정해야 한다. | createAlert:422, createAlert:201, createAlert:200, createAlert:502, createAlert:503, createAlert:504, createAlert:SLOT_NOT_SUPPORTED, getCourtAvailability:200 | standing |
| d_failure_suppression_window | 확인이 실패한 뒤 다음 확인을 언제 허용하는가. | 실패를 코트·날짜 단위로 저장하고 확인 간격 동안 새 시도를 막는다. 경계는 배타로 두어 실패 시각에 확인 간격을 더한 그 시점부터는 다시 확인한다. | spec_implication | medium | isWithinFailureWindow 한 메서드를 고치면 된다. 창을 없애려면 storeFailure와 findLastFailure를 부르는 자리를 지운다. | getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, getCourtAvailability:200, createAlert:502, createAlert:503, createAlert:504 | standing |
| d_delivery_pending_only | 이 표면이 발송 대기를 기록하는가. 기록한다면 어디까지 옮기는가. | PENDING 대기를 올리고 버리는 전이만 한다. 대기를 올리는 계기는 둘이다. 예약처 확인에 새로 성공한 직후 그 코트·날짜를 감시 중인 신청 전부를 훑는 자리와, 신청을 만든 직후 그 신청 하나에 판정에 쓴 결과를 적용하는 자리다. 버리는 계기는 새 확인 결과에 빈자리가 없을 때와 해제·만료다. SENT와 NOTIFIED로 옮기는 일은 발송 경로가 붙을 때까지 하지 않는다. | spec_implication | medium | AvailabilityQueryService의 전이 메서드 하나와 AlertCommandService의 적용 메서드 하나를 지우면 대기가 다시 생기지 않는다. 도메인의 두 전이 메서드는 해제·만료가 쓰는 자리와 같아 남는다. | getAlert:200, listAlerts:200, cancelAlert:200, createAlert:201, createAlert:200, getCourtAvailability:200 | standing |
| d_spec_declaration_scope | 추출되는 스펙에 계약의 무엇까지 명시적으로 선언하는가. | 예시는 요청 본문과 스칼라 필드에만 두고, 응답 예시는 계약에 맡긴다. 상태 코드·필수 여부·에러 코드 확장·보안 요구는 도구 기본값에 맡기지 않고 모두 선언한다. 배열 필드의 필수 여부는 도구가 배열 선언 안쪽의 값을 읽지 않는 자리가 있어 직렬화 애너테이션으로도 같은 말을 적는다. | convention | medium | 예시를 요구하면 각 응답 선언에 예시 블록을 더하면 된다. 코드 동작은 바뀌지 않는다. | getCourtAvailability:200, createAlert:201, createAlert:200, listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_delivery_stays_absent | 빈자리를 확인했을 때 이 표면이 발송 대기를 만들어 delivery를 PENDING으로 올리는가. | 발송 대기를 만들지 않는다. delivery는 널이고 상태 전이는 해제와 만료만 일어난다. | least_harm | medium | 발송 경로가 붙을 때 도메인의 대기 생성과 꺼내는 작업을 함께 더해야 한다. 응답 모양은 이미 발송 결과를 담을 수 있어 표면은 바뀌지 않는다. | createAlert:201, createAlert:200, getAlert:200, listAlerts:200, cancelAlert:200 | superseded |
| d_expires_at_not_advanced | 더 이른 예약 마감 시각이 확인되면 만료 시각을 그 시각으로 앞당긴다는 계약의 문장을 무엇으로 지키는가. | 앞당기지 않는다. 만료 시각은 코트 지역 시간대로 읽은 이용 시작 시각이다. | least_harm | medium | 포트가 마감 시각을 드러내면 만료 시각 계산 한 메서드에서 더 이른 값을 고르면 된다. | createAlert:201, createAlert:422, getAlert:200, listAlerts:200, cancelAlert:200 | standing |
| d_expiry_sweep_interval | 만료 시각이 지난 신청을 만료로 옮기는 작업을 얼마나 자주 도는가. | 1초 주기로 감시 중인 신청을 훑어 만료 시각이 지난 것을 만료로 옮긴다. | convention | medium | AlertExpiryJob의 주기 문자열 하나를 고치면 된다. | getAlert:200, listAlerts:200, cancelAlert:200, createAlert:201, createAlert:422 | standing |
| d_in_progress_key_yields_contention | 같은 멱등 키의 앞선 요청이 아직 처리 중인데 이번 요청의 지문이 다르면 409와 503 중 무엇으로 물러나는가. | 처리 중인 자리에서는 지문을 보고 상태 코드를 가르지 않고 모두 503으로 물러난다. 다만 지문이 다르면 재시도의 성공을 약속하지 않고 앞선 요청의 결말에 따라 판정된다고만 알린다. 409는 2xx로 기록된 앞선 응답이 있을 때만 낸다. | contract_analogy | medium | claim의 판정 두 줄과 안내 문장 하나를 고치면 된다. | createAlert:503, createAlert:409, createAlert:201, createAlert:200, createAlert:CONCURRENT_UPDATE_CONFLICT | standing |
| d_pending_at_creation | 신청을 판정하는 데 쓴 확인 결과가 그 시간대를 빈자리로 말할 때, 그 결과가 오래된 것이어도 새 신청의 발송 대기를 올리는가. | 판정에 쓴 결과가 그 시간대를 빈자리로 말하면 그 결과의 신선도를 가리지 않고 대기를 올린다. 이미 감시 중이던 신청을 돌려주는 200에서는 하지 않고, 경합에 밀리면 넘어간다. | spec_implication | medium | applyCheckedSchedule을 부르는 자리에 신선도 조건을 더하면 된다. 그러려면 knownSchedule이 신선도를 함께 돌려줘야 한다. | createAlert:201, createAlert:200, getAlert:200, listAlerts:200, cancelAlert:200 | standing |
| d_protocol_failures_outside_contract | 계약이 선언하지 않은 프로토콜 수준 실패를 계약이 정한 실패 응답으로 옮기는가. | 본문의 미디어 타입 실패만 400 VALIDATION_FAILED로 옮긴다. 매핑에 consumes 조건을 걸지 않아 그 실패가 처리기까지 오게 하고, 요청 줄과 헤더의 프로토콜 실패는 스프링 기본에 맡긴다. | least_harm | medium | 처리기 하나를 지우면 스프링 기본으로 되돌아간다. 모두 옮기려면 ResponseEntityExceptionHandler를 상속해 계약에 없는 상태 코드의 본문만 따로 정해야 한다. | createAlert:400, listAlerts:400, getAlert:400, cancelAlert:400, getCourtAvailability:400 | standing |
| d_window_closed_before_dedup | 감시 중인 같은 조건의 신청이 있고 그 조건의 만료 시각이 이미 지났을 때 무엇을 내는가. | 조건 판정을 먼저 하고, 만료 시각이 지났으면 감시 중인 같은 조건이 있어도 422를 낸다. | contract_analogy | medium | 신청 처리에서 조건 판정과 기존 신청 조회의 순서를 바꾸면 된다. | createAlert:422, createAlert:200, createAlert:201 | standing |
| d_schedule_unknown_maps_to_internal | 신청 처리 중 예약처 확인이 실패하고 저장된 성공 결과도 없을 때 무엇으로 내보내는가. | 500 INTERNAL_ERROR로 낸다. | least_harm | medium | ScheduleUnknownException의 처리기 한 곳을 고치면 된다. | createAlert:500, createAlert:422 | superseded |
| d_trace_id_format | traceId를 어떤 형식의 문자열로 만드는가. | UUID 문자열을 쓴다. | convention | high | Problems의 식별자 생성 한 줄을 고치면 된다. | getCourtAvailability:500, getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, createAlert:502, createAlert:503, createAlert:504, listAlerts:500, listAlerts:503, getAlert:500, getAlert:503, cancelAlert:500, cancelAlert:503 | standing |
| d_problem_optional_keys_absent | 값이 없는 Problem의 선택 필드를 키까지 빼는가 널로 싣는가. | 실패 본문을 순서 있는 맵으로 만들어 값이 없는 선택 필드의 키를 싣지 않는다. 스키마 선언은 그대로 Problem 타입이 한다. | contract_analogy | high | Problems의 본문 조립 메서드 하나를 고치면 된다. 응답 타입 선언은 그대로다. | getCourtAvailability:400, getCourtAvailability:404, createAlert:401, createAlert:409, createAlert:422, listAlerts:401, getAlert:404, cancelAlert:404, getCourtAvailability:500 | standing |
| d_version_prefix_in_emitted_uris | 응답에 실어 보내는 Location과 Problem.instance에 주 버전 접두사를 붙이는가. | 붙인다. Location은 /v1/alerts/{alertId}이고 instance는 /v1 + 요청 경로다. | contract_analogy | high | ApiPaths의 접두사 상수를 비우면 두 자리가 함께 바뀐다. | createAlert:201, createAlert:200, getCourtAvailability:400, getCourtAvailability:404, getAlert:404, cancelAlert:404, createAlert:409, createAlert:422 | standing |
| d_bearer_token_identity | Bearer 토큰에서 사용자 식별자를 어떻게 얻는가. | 토큰 문자열 자체를 사용자 식별자로 쓴다. 헤더가 없거나 Bearer 스킴이 아니거나 토큰이 비면 401이다. | least_harm | high | BearerTokens 한 파일을 고치면 된다. 유스케이스는 식별자 문자열만 받으므로 바뀌지 않는다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401, getAlert:404, cancelAlert:404, listAlerts:200 | standing |
| d_court_identity_read_fresh | 코트 이름과 예약 화면 주소를 어느 시점의 값으로 내는가. 그 규칙을 어느 표면에 적용하는가. | 읽는 시점에 포트에서 읽는다. 규칙을 CourtIdentity 한 자리에 두고 신청 조회와 예약 상태 조회가 모두 그 자리를 지난다. 코트가 더 이상 지원되지 않으면 보관된 값으로 물러난다. 확인 결과에는 코트 신원을 담지 않는다. | contract_analogy | high | CourtIdentity의 read 한 메서드를 보관된 값만 쓰도록 고치면 두 표면이 함께 바뀐다. | getAlert:200, listAlerts:200, cancelAlert:200, createAlert:201, createAlert:200, getCourtAvailability:200 | standing |
| d_create_upstream_failure_codes | 신청 처리 중 예약처 확인이 실패하고 저장된 성공 결과도 없을 때 무엇으로 내보내는가. | 확인 실패의 이유를 예외에 담아 올리고, 예약 상태 조회와 같은 매핑으로 502·503·504와 같은 이름의 코드로 낸다. | contract_analogy | high | ScheduleUnknownException의 처리기 한 곳과 컨트롤러의 응답 선언 셋을 고치면 된다. | createAlert:502, createAlert:503, createAlert:504, createAlert:500, createAlert:422 | standing |
| d_storage_timeout_declared_unreachable | 계약이 선언한 STORAGE_TIMEOUT을 실제로 던지는 경로를 이 run에서 두는가. | 선언과 매핑은 두고 도달 경로는 두지 않는다. 저장이 실제 저장소로 바뀌면 그 어댑터가 StorageTimeoutException을 던지고 처리기와 응답 선언은 그대로 쓴다. | least_harm | high | 저장 어댑터가 예외를 던지기 시작하면 고칠 것이 없다. 처리기와 선언이 이미 있다. | listAlerts:503, getAlert:503, cancelAlert:503, createAlert:503, getCourtAvailability:503 | standing |
| d_idempotency_fingerprint | 같은 멱등 키가 다른 내용의 요청에 쓰였는지를 무엇으로 견주는가. | 정규화한 조건 셋을 지문으로 쓴다. | contract_analogy | high | AlertCondition.fingerprint 한 메서드를 고치면 된다. | createAlert:409, createAlert:201, createAlert:200, createAlert:503 | standing |
| d_replayed_false_on_existing_watch | 같은 조건을 감시 중인 신청을 새 키로 돌려줄 때 Idempotency-Replayed를 무엇으로 두는가. | false로 둔다. 같은 키로 보관된 응답을 재생할 때만 true다. | contract_analogy | high | 컨트롤러가 헤더에 넣는 값 한 곳을 고치면 된다. | createAlert:200, createAlert:201 | standing |
| d_list_order_tiebreak | 신청 시각이 같은 두 신청의 앞뒤를 무엇으로 정하는가. | 신청 시각으로 먼저 정렬하고, 같으면 저장된 순서가 나중인 것을 앞에 둔다. | convention | high | 저장소의 비교기 한 곳을 고치면 된다. | listAlerts:200 | standing |

`d_authentication_before_validation` — 계약은 두 실패를 함께 선언하고 앞뒤를 정하지 않았다. 본인 것만 보이게 하려는 요구가 있는 표면에서 식별 전에 값을 판정해 알려 주면 그 요구가 새는 방향으로 기운다. 다만 본문 역직렬화는 컨트롤러 메서드가 불리기 전에 일어나므로 규칙이 그 앞까지 미치지 않는다. 그 사실을 감추지 않고 범위를 사실에 맞춰 적는다. 관철하려면 인증을 필터로 올려야 하는데, 계약에 없는 계층을 들이는 값이 그 정확도보다 크다.

`d_create_may_trigger_check` — 명세는 확인 간격과 코트·날짜 단위 확인을 정했고 계약은 신청이 지원하는 시간대인지 판정하라고 한다. 두 요구를 함께 지키는 방법은 조회가 쓰는 같은 확인 경로를 재사용하는 것이다. 예약처 장애에 실제로 노출되는 것은 한 번도 확인된 적 없는 코트·날짜뿐이고, 그 대신 임의로 오래된 운영 시간대 목록으로 422를 내거나 신청을 받을 수 있다. 운영 시간대는 빈자리 여부보다 훨씬 느리게 바뀌므로 그 나이를 이유로 거절을 뒤집지 않는다. 오래된 목록이라도 그 목록에 있는 시간대면 받고, 목록이 비어 있으면 판정을 미루고 받는다.

`d_failure_suppression_window` — 명세의 EF-2는 확인이 일어나는 동안 몰린 요청을 한 번으로 묶으라고 했고, NFR-3은 예약처 허용량을 모르므로 보수적으로 가라고 했다. 실패한 예약처에 성공했을 때보다 더 자주 묻는 것은 그 두 뜻에 어긋난다. 창의 길이를 확인 간격과 같게 둔 것은 그래서이고, 새 수치를 만들지 않았다. 다만 그 길이가 실패 응답이 안내하는 초와 같으므로 경계까지 포함으로 두면 안내가 거짓이 된다. 경계만 배타로 돌려 두 요구를 함께 지킨다. 함께 적어 둘 사실이 하나 있다. 확인을 한 번만 통과시키는 자물쇠 표는 코트·날짜마다 항목이 생기고 지워지지 않는다.

`d_delivery_pending_only` — FR-4는 '빈자리를 확인하면 발송 대기에 기록하고, 발송은 별도 작업이 맡는다'로 둘을 갈랐다. 범위 밖인 것은 보내는 일이고 기록하는 일은 이 표면의 몫이다. 계기를 새 확인 성공 하나로 두면 저장된 결과가 확인 간격 안이어서 예약처를 부르지 않은 경로에서 그 문이 열리지 않아, 이미 비어 있는 자리를 보고 신청한 사용자에게 대기가 오르지 않는다. 그래서 생성 경로에도 같은 전이를 걸었고 그 선택의 갈림길은 [d_pending_at_creation]에 따로 적는다.

`d_spec_declaration_scope` — 구현 규약이 명시하라고 꼽은 것은 상태 코드와 필수 여부와 에러 코드 확장과 보안 요구다. 예시는 그 목록에 없고, 같은 값을 두 곳에 두면 어긋남을 만들 자리만 늘어난다. 다만 필수 여부는 도구가 어느 자리를 읽는지에 달려 있어, 읽히지 않는 선언 하나만으로는 약속이 스펙에 나타나지 않는다는 것이 지난 iteration에 드러났다.

`d_delivery_stays_absent` — 발송 경로는 이 run의 범위 밖이고 계약도 그 경로에 엔드포인트를 두지 않았다. 꺼내는 쪽 없이 대기만 쌓으면 상태와 발송 결과를 함께 읽으라는 약속이 지켜지지 않는 상태가 계속 남는다. 대기가 없는 동안의 모양은 계약이 널로 적어 두었다.

`d_expires_at_not_advanced` — 계약은 더 이른 마감 시각이 '확인되면' 그 시각을 쓰라고 했고, 그 확인은 예약처에서 온다. 이 run의 포트는 마감 시각을 드러내지 않으므로 확인되는 일이 없다. 값을 지어내는 것보다 확인되지 않았다는 사실에 따라 이용 시작 시각을 쓰는 쪽이 계약과 어긋나지 않는다.

`d_expiry_sweep_interval` — 계약과 명세는 만료가 서버 작업의 몫이라는 것과 그 사이에 잠시 감시 중으로 보일 수 있다는 것까지만 정하고 주기를 정하지 않았다. 응답에 보이는 틈을 사람이 눈치채기 어려운 크기로 줄이는 쪽을 골랐다.

`d_in_progress_key_yields_contention` — 계약은 409를 '같은 키가 다른 내용의 요청에 이미 쓰였다'로 적었고 2xx로 끝난 응답만 보관한다고 적었다. 앞선 요청이 4xx로 끝나면 그 키는 쓰이지 않은 것이 되므로 처리 중에는 아직 쓰였다고 말할 수 없다. 이것이 이 선택의 근거 전부다. 앞선 초안이 함께 적었던 '판정의 근거가 되는 앞선 응답이 아직 없다'는 사실과 다르다. 지문은 자리를 잡는 순간 기록되므로 처리 중에도 알 수 있고, 알 수 있는 것을 모른 척해 재시도 성공을 약속하면 그 안내가 거짓이 되므로 안내만 가른다.

`d_pending_at_creation` — EF-1은 확인 실패를 빈자리 없음으로 다루지 말라고 했고, 오래된 성공 결과는 그 시각의 사실이지 실패가 아니다. 그 결과로 신청을 받아들이면서 같은 결과로 대기를 올리지 않으면 한 요청 안에서 같은 사실을 두 번 다르게 읽는 것이 된다. 대기가 실제로 나가는 알림이 아니라는 점이 이 선택의 값을 낮춰 준다. EF-4가 정한 대로 더 최신 결과에 빈자리가 없으면 그 대기는 버려지고 알림은 나가지 않는다.

`d_protocol_failures_outside_contract` — 계약은 요청의 형식 위반을 VALIDATION_FAILED로 오게 했고 본문의 미디어 타입은 본문을 값으로 옮기는 일의 일부다. 반면 메서드와 Accept 헤더의 실패는 요청 줄을 고쳐야 풀리고 계약이 그 자리에 코드를 두지 않았다. 없는 코드를 지어내지 않고, 있는 코드가 덮는 자리만 덮는다.

`d_window_closed_before_dedup` — 두 문장이 같은 요청에 겹치고 계약은 앞뒤를 정하지 않았다. 만료 시각이 지난 조건은 감시할 수 없다는 것이 이 코드의 뜻이고, 계약도 이 시각이 지났는데 상태가 아직 감시 중이면 시각을 기준으로 판단하라고 적었다.

`d_schedule_unknown_maps_to_internal` — 신청 오퍼레이션에는 예약처 확인 실패를 알리는 상태 코드가 선언되지 않았다. 남은 코드 중 재시도 가능하고 원인을 왜곡하지 않는 것은 서버 내부 실패뿐이고, 계약도 이 코드를 그런 나머지로 적었다.

`d_trace_id_format` — 계약은 traceId를 형식 없는 문자열로 두고 로그에서 요청을 찾는 식별자라고만 적었다. 예시의 값은 ULID처럼 보이지만 스키마가 형식을 요구하지 않으므로, 새 의존 없이 충돌하지 않는 식별자를 쓰는 쪽을 골랐다.

`d_problem_optional_keys_absent` — Problem의 선택 필드는 널을 허용하지 않고, Alert의 lastCheckedAt처럼 널을 허용하면서 필수인 필드는 키가 남아야 한다. 스켈레톤이 소유한 설정이 널을 지우지 않는 쪽으로 고정돼 있어 그 필드들의 키는 응답에 남고, 실패 본문만 필드 단위로 만들어 두 요구를 함께 지킨다.

`d_version_prefix_in_emitted_uris` — 계약의 Location 예시와 모든 Problem 예시의 instance가 /v1로 시작한다. 경로 접두사를 오퍼레이션 경로에 다시 넣지 말라는 규약은 요청을 받는 자리의 규약이고, 돌려주는 값은 밖에서 보이는 주소라 접두사가 붙은 모양이 맞다.

`d_bearer_token_identity` — 계약은 인증 방식 자체를 정하지 않고 사용자 식별이 필요한 까닭만 적었다. 식별자를 얻는 방법 중 계약이 검사할 수 있는 성질은 '같은 토큰이면 같은 사용자'뿐이라, 그 성질만 만족하는 가장 얇은 선택을 골랐다.

`d_court_identity_read_fresh` — 계약은 이 주소를 '알림에 담기는 링크와 같다'로 적었다. 알림은 보내는 시점의 코트 정보로 만들어지므로 조회도 그 시점의 값을 보여야 두 문장이 같은 것을 가리킨다. 앞 초안은 이 규칙을 신청 조회에만 적용해 정작 근거로 든 조회 표면이 확인 시점의 스냅숏을 냈다. 확인 결과가 오래될 수 있다는 것이 그 스냅숏의 나이에 상한이 없다는 뜻이라 어긋남이 더 크다. 그래서 도메인의 확인 결과에서 신원을 빼고 읽는 자리를 하나로 모았다.

`d_create_upstream_failure_codes` — 앞 판본에는 이 오퍼레이션에 확인 실패를 말할 코드가 없어 남은 코드 중 가장 덜 나쁜 것을 골랐지만, 그 선택은 원인을 왜곡하고 재시도 안내를 거짓으로 만들었다. 리뷰어의 발의로 계약이 같은 조건의 세 코드를 선언했으므로 구현은 그 코드를 그대로 쓴다. 기다릴 초는 계약이 확인 실패에 확인 간격을 따르라고 한 대로 20이다.

`d_storage_timeout_declared_unreachable` — 저장소 장애 주입은 이 run의 범위 밖이고 저장은 인메모리다. 그래도 계약이 선언한 실패는 계약의 약속이므로 표면에서 지우지 않는다. 지금 비어 있는 것은 그 실패를 만드는 쪽이고, 받는 쪽은 준비돼 있다.

`d_idempotency_fingerprint` — 계약의 요청 본문은 조건 셋뿐이고 추가 필드를 두지 않았다. 재시도 안전이 이 키의 목적이라, 같은 뜻의 재시도가 막히지 않는 쪽으로 견준다.

`d_replayed_false_on_existing_watch` — 계약은 이 헤더를 같은 키로 앞서 처리한 응답을 그대로 돌려준 것인지로 정의했다. 조건이 같아 기존 신청을 돌려주는 200은 이번 요청이 키를 처음 써서 처리한 결과이므로 재생이 아니다. 리뷰어의 발의로 이번 판본이 그 갈림길을 계약에 적었으므로, 다음 구현자가 반대로 고를 자리도 함께 닫혔다.

`d_list_order_tiebreak` — 계약은 최근에 신청한 것이 앞에 온다고만 적었고 시각이 같을 때를 정하지 않았다. 순서가 정해지지 않으면 같은 요청에 다른 목록이 나오므로 안정된 기준을 하나 더 둔다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 8건 중 2건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

라벨은 그 차이가 쓰는 사람에게 어떤 뜻인지다. 판정을 가르지 않고 이 절의 순서만 정한다. 위반에 붙는 라벨만 여기 온다 — 응답의 값을 계약보다 좁게 선언한 것처럼 손해가 없는 자리는 위반이 아니라 기록이고 6절에 있다.

| 라벨 | 뜻 |
| --- | --- |
| withholds_promised_response | 계약이 주겠다고 한 것을 구현이 주지 않게 만드는 차이다. 읽는 쪽이 있다고 믿은 것이 없다. |

그 8건은 원인 8개에서 나왔다. 한 원인이 한 지점에만 걸렸으므로 고칠 일의 개수가 위반의 개수와 같다.

| 원인 | 규칙 | family | 좌표 | 라벨 | 닿는 지점 | 상태 | 닫은 방법 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| v_519dc1 | change.unjustified | — | /paths/~1alerts/post/responses/503 | — | 0 | open | — |
| v_74a05d | change.unjustified | — | /paths/~1alerts/post/description | — | 0 | open | — |
| v_8bbdf2 | change.unjustified | — | /components/responses/AlertCreateUnavailable | — | 0 | open | — |
| v_af7d78 | change.unjustified | — | /components/schemas/AlertStatus/description | — | 0 | open | — |
| v_d30880 | change.unjustified | — | /paths/~1alerts/post/responses/502 | — | 0 | open | — |
| v_db8a07 | change.unjustified | — | /paths/~1alerts/post/responses/504 | — | 0 | open | — |
| v_03c067 | response.field_differs | differs | AlertList | withholds_promised_response | 1 | closed | code |
| v_430fb8 | response.field_differs | differs | CourtAvailability | withholds_promised_response | 1 | closed | code |

접힌 잎이 있는 원인은 2개이고 잎은 모두 2개다. 게이트가 한 자리를 통째로 다르다고 보고 아래를 접은 것이므로, 대조기가 보지 못한 자리와 다르다. 보지 못한 자리는 3절에 따로 있다.

### `change.unjustified` @ `/paths/~1alerts/post/responses/503`

기록 cc_create_503_covers_upstream이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다. 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다.

주장한 수정은 없다. 확인된 수정은 없다.

### `change.unjustified` @ `/paths/~1alerts/post/description`

기록 cc_create_schedule_source이 인용한 'cr_create_upstream_failure'가 이 iteration의 비평에 없다. 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다.

주장한 수정은 없다. 확인된 수정은 없다.

### `change.unjustified` @ `/components/responses/AlertCreateUnavailable`

기록 cc_alert_create_unavailable_response은 표현 변경인데 representation_basis가 비었다 (닫힌 값: interop, spec_implication, contract_consistency). 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다.

주장한 수정은 없다. 확인된 수정은 없다.

### `change.unjustified` @ `/components/schemas/AlertStatus/description`

기록 cc_watching_check_source이 인용한 'cr_watching_says_periodic_check'가 이 iteration의 비평에 없다. 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다.

주장한 수정은 없다. 확인된 수정은 없다.

### `change.unjustified` @ `/paths/~1alerts/post/responses/502`

기록 cc_create_upstream_unreadable은 표현 변경인데 representation_basis가 비었다 (닫힌 값: interop, spec_implication, contract_consistency). 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다.

주장한 수정은 없다. 확인된 수정은 없다.

### `change.unjustified` @ `/paths/~1alerts/post/responses/504`

기록 cc_create_upstream_timeout은 표현 변경인데 representation_basis가 비었다 (닫힌 값: interop, spec_implication, contract_consistency). 닿는 판정 지점은 0곳이다: .

왜 문제인가 — 앞 판본과 달라진 자리에 신고가 없거나, 신고에 발의한 비평과 명세 근거 중 하나가 비었다.

주장한 수정은 없다. 확인된 수정은 없다.

### `response.field_differs` @ `AlertList`

필수: items: 계약이 항상 담겠다고 보장했는데 구현은 선택이다. 닿는 판정 지점은 1곳이다: `listAlerts:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/dto/AlertListResponse.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 필수: items: 계약이 항상 담겠다고 보장했는데 구현은 선택이다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/dto/AlertListResponse.java
+++ b/src/main/java/com/thinking/tennis/api/dto/AlertListResponse.java
@@ -2,2 +2,3 @@
 
+import com.fasterxml.jackson.annotation.JsonProperty;
 import com.thinking.tennis.app.AlertView;
@@ -18,2 +19,8 @@
 
+        /*
+         * 필수 여부를 @JsonProperty 로도 적는다. 배열 필드의 필수 선언을 @ArraySchema 안쪽에만 두면
+         * 추출되는 스펙의 required 목록에 이 이름이 실리지 않아, 계약이 항상 담겠다고 한 필드가
+         * 선택으로 나간다. 두 자리가 같은 말을 하므로 어느 쪽이 읽히든 결과는 같다.
+         */
+        @JsonProperty(required = true)
         @ArraySchema(arraySchema = @Schema(
```

### `response.field_differs` @ `CourtAvailability`

필수: slots: 계약이 항상 담겠다고 보장했는데 구현은 선택이다. 닿는 판정 지점은 1곳이다: `getCourtAvailability:200`.

왜 문제인가 — 계약이 그 필드에 정한 타입·형식·모양·제약·열거형·필수가 구현에서 다르다.

주장한 수정은 없다. 확인된 수정은 `src/main/java/com/thinking/tennis/api/dto/CourtAvailabilityResponse.java`, `src/main/java/com/thinking/tennis/app/AlertCommandService.java`, `src/main/java/com/thinking/tennis/app/AlertViewFactory.java`, `src/main/java/com/thinking/tennis/app/AvailabilityQueryService.java`, `src/main/java/com/thinking/tennis/domain/CourtDayAvailability.java`이다.

게이트가 한 건으로 접은 잎 1개다. 접은 것과 보지 못한 것은 다르다 — 여기 적힌 것은 게이트가 보고 접은 것이다.
- 필수: slots: 계약이 항상 담겠다고 보장했는데 구현은 선택이다

작업 폴더가 앞 iteration과 달라진 자리는 이렇다.

```diff
--- a/src/main/java/com/thinking/tennis/api/dto/CourtAvailabilityResponse.java
+++ b/src/main/java/com/thinking/tennis/api/dto/CourtAvailabilityResponse.java
@@ -2,2 +2,3 @@
 
+import com.fasterxml.jackson.annotation.JsonProperty;
 import com.thinking.tennis.app.AvailabilityView;
@@ -68,2 +69,8 @@
 
+        /*
+         * 필수 여부를 @JsonProperty 로도 적는다. 배열 필드의 필수 선언을 @ArraySchema 안쪽에만 두면
+         * 추출되는 스펙의 required 목록에 이 이름이 실리지 않아, 계약이 항상 담겠다고 한 필드가
+         * 선택으로 나간다. 두 자리가 같은 말을 하므로 어느 쪽이 읽히든 결과는 같다.
+         */
+        @JsonProperty(required = true)
         @ArraySchema(arraySchema = @Schema(
--- a/src/main/java/com/thinking/tennis/app/AlertCommandService.java
+++ b/src/main/java/com/thinking/tennis/app/AlertCommandService.java
@@ -4,2 +4,3 @@
 import com.thinking.tennis.app.error.AlertWindowClosedException;
+import com.thinking.tennis.app.error.AvailabilityUnavailableException;
 import com.thinking.tennis.app.error.ConcurrentUpdateException;
@@ -115,6 +116,6 @@
 
-        CourtDayAvailability schedule = availability.knownSchedule(court, condition.date())
-                .orElseThrow(() -> new ScheduleUnknownException(condition.courtId(), condition.date()));
+        CourtDayAvailability schedule = operatingSchedule(court, condition);
         if (!schedule.operates(condition.slot())) {
-            throw new SlotNotSupportedException(condition.courtId(), condition.date(), condition.slot());
+            throw new SlotNotSupportedException(condition.courtId(), condition.date(), condition.slot(),
+                    schedule.operatingSlots());
         }
@@ -132,2 +133,21 @@
         return new CreateResult(views.of(stored), created ? 201 : 200, false);
+    }
+
+    /*
+     * 조건을 판정할 운영 시간대를 얻는다. 그 코트·날짜의 확인 결과가 유일한 출처이고, 확인은 실패할 수
+     * 있다. 실패를 삼키면 운영하지 않는 시간대로 거절하거나 원인을 서버 결함으로 바꿔 말하게 되므로,
+     * 무엇이 실패했는지를 지닌 예외로 옮겨 그대로 올려보낸다.
+     *
... (13줄 더)
```

판정은 지점 단위로 한다. 어느 지점이 아직 열려 있는지는 지점으로만 말할 수 있다.

| id | 규칙 | 판정 지점 | 좌표 | 라벨 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_519dc1 | change.unjustified | — | /paths/~1alerts/post/responses/503 | — | open | — | 0 | 002 | — |
| v_74a05d | change.unjustified | — | /paths/~1alerts/post/description | — | open | — | 0 | 002 | — |
| v_8bbdf2 | change.unjustified | — | /components/responses/AlertCreateUnavailable | — | open | — | 0 | 003 | — |
| v_af7d78 | change.unjustified | — | /components/schemas/AlertStatus/description | — | open | — | 0 | 002 | — |
| v_d30880 | change.unjustified | — | /paths/~1alerts/post/responses/502 | — | open | — | 0 | 003 | — |
| v_db8a07 | change.unjustified | — | /paths/~1alerts/post/responses/504 | — | open | — | 0 | 003 | — |
| v_03c067 | response.field_differs | listAlerts:200 | AlertList | withholds_promised_response | closed | code | 0 | 001 | 002 |
| v_430fb8 | response.field_differs | getCourtAvailability:200 | CourtAvailability | withholds_promised_response | closed | code | 0 | 001 | 002 |

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/AlertController.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/dto/AlertResponse.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/dto/AlertStatusValue.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AlertRepository.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AlertView.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/error/ScheduleUnknownException.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/error/SlotNotSupportedException.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/store/InMemoryAlertRepository.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/api/dto/CourtAvailabilityResponse.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AlertViewFactory.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AvailabilityQueryService.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AvailabilityView.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/CourtIdentity.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/IdempotencyStore.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/store/InMemoryIdempotencyStore.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/domain/CourtDayAvailability.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 13건이다.

### 충족 주장과 게이트 판정 (2건)

주장은 판정이 아니다. 어긋난 자리만 남긴다.

| iteration | point | 관찰 |
| --- | --- | --- |
| 001 | getCourtAvailability:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |
| 001 | listAlerts:200 | 충족했다고 주장한 판정 지점에 게이트 위반이 남아 있다 |

### 말없는 되돌림 (5건)

서 있는 결정을 바꾸려면 `supersedes`에 이전 id와 이유를 적어야 한다.

| iteration | decision_id | 관찰 |
| --- | --- | --- |
| 002 | d_authentication_before_validation | 결정 d_authentication_before_validation의 고른 것이 supersedes 없이 달라졌다: '사용자 식별을 먼저 한다. 인증 오퍼레이션은 401을 먼저 낸다.' → '본문을 값으로 옮기는 데 성공한 뒤부터 식별을 먼저 한다. 본문을 읽지 못한 실패와 본문의 미디어 타입 실패는 식별보다 앞서 400 VALIDATION_FAILED로 나간다.' |
| 002 | d_spec_declaration_scope | 결정 d_spec_declaration_scope의 고른 것이 supersedes 없이 달라졌다: '예시는 요청 본문과 스칼라 필드에만 두고, 응답 예시는 계약에 맡긴다. 상태 코드·필수 여부·에러 코드 확장·보안 요구는 도구 기본값에 맡기지 않고 모두 선언한다.' → '예시는 요청 본문과 스칼라 필드에만 두고, 응답 예시는 계약에 맡긴다. 상태 코드·필수 여부·에러 코드 확장·보안 요구는 도구 기본값에 맡기지 않고 모두 선언한다. 배열 필드의 필수 여부는 도구가 배열 선언 안쪽의 값을 읽지 않는 자리가 있어 직렬화 애너테이션으로도 같은 말을 적는다.' |
| 003 | d_in_progress_key_yields_contention | 결정 d_in_progress_key_yields_contention의 고른 것이 supersedes 없이 달라졌다: '처리 중인 자리에서는 지문을 보지 않고 503으로 물러난다. 409는 2xx로 기록된 앞선 응답이 있을 때만 낸다.' → '처리 중인 자리에서는 지문을 보고 상태 코드를 가르지 않고 모두 503으로 물러난다. 다만 지문이 다르면 재시도의 성공을 약속하지 않고 앞선 요청의 결말에 따라 판정된다고만 알린다. 409는 2xx로 기록된 앞선 응답이 있을 때만 낸다.' |
| 003 | d_create_may_trigger_check | 결정 d_create_may_trigger_check의 고른 것이 supersedes 없이 달라졌다: '저장된 결과를 먼저 쓰고, 확인 간격보다 오래됐거나 한 번도 없으면 조회와 같은 경로로 확인을 일으킨다.' → '저장된 결과를 먼저 쓰고, 확인 간격보다 오래됐거나 한 번도 없으면 조회와 같은 경로로 확인을 일으킨다. 확인이 실패해도 저장된 마지막 성공 결과가 있으면 그 결과로 판정하며, 그 결과의 나이에는 상한을 두지 않는다.' |
| 003 | d_court_identity_read_fresh | 결정 d_court_identity_read_fresh의 고른 것이 supersedes 없이 달라졌다: '읽는 시점에 포트에서 읽는다. 코트가 더 이상 지원되지 않으면 신청에 보관된 값으로 물러난다.' → '읽는 시점에 포트에서 읽는다. 규칙을 CourtIdentity 한 자리에 두고 신청 조회와 예약 상태 조회가 모두 그 자리를 지난다. 코트가 더 이상 지원되지 않으면 보관된 값으로 물러난다. 확인 결과에는 코트 신원을 담지 않는다.' |

### 축 판정의 어긋남 (3건)

Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다. 방향을 함께 적는다 — 누가 더 엄한지가 읽는 사람에게 필요하다. 게이트까지 셋이 갈린 자리가 자기 평가와 계약 테스트 결과가 어긋난 지점의 후보다.

| iteration | axis | critique_id | gate_verdict | eval_score | direction | 관찰 |
| --- | --- | --- | --- | --- | --- | --- |
| 001 | failure_faithfulness | w_create_retry_after_lies | REJECT | 4 | Critique가 더 엄하다 | 게이트도 위반을 찾았으니 갈린 것은 Eval과 Critique다. Eval이 4로 통과시킨 축이다. |
| 002 | request_tolerance | w_unopened_date_rejected_as_slot | REJECT | 4 | Critique가 더 엄하다 | 게이트도 위반을 찾았으니 갈린 것은 Eval과 Critique다. Eval이 4로 통과시킨 축이다. |
| 003 | failure_faithfulness | cr_window_closed_after_upstream | REJECT | 4 | Critique가 더 엄하다 | 게이트도 위반을 찾았으니 갈린 것은 Eval과 Critique다. Eval이 4로 통과시킨 축이다. |

### 게이트가 세는 것을 다시 말한 지적 (3건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다. 겹친다고 말하려면 같은 좌표를 가리켜야 하므로 겹친 좌표를 함께 적는다.

| iteration | axis | critique_id | rule | shared | 관찰 |
| --- | --- | --- | --- | --- | --- |
| 002 | state_continuity | w_new_alert_ignores_its_own_check | change.unjustified | /alerts.post | 게이트가 change.unjustified로 이미 세는 좌표 `/alerts.post`를 비평이 다시 말했다 |
| 003 | failure_faithfulness | cr_window_closed_after_upstream | change.unjustified | /alerts.post | 게이트가 change.unjustified로 이미 세는 좌표 `/alerts.post`를 비평이 다시 말했다 |
| 003 | response_fidelity | cr_201_last_checked_overclaim | change.unjustified | post.description | 게이트가 change.unjustified로 이미 세는 좌표 `post.description`를 비평이 다시 말했다 |

### 게이트가 남긴 기록 (113건)

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
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:400 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:404 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:500 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:502 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:503 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:504 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:200 | AlertList.items[].delivery | 002 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:400 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:401 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:500 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:503 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:201 | Alert.delivery | 002 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:200 | Alert.delivery | 002 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:400 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:401 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:409 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:422 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:500 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:502 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:503 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:504 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:200 | Alert.delivery | 002 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:400 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:401 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:404 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:500 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:503 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:200 | Alert.delivery | 002 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:400 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:401 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:404 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:500 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:503 | Problem | 002 | 형식: 2곳에서 좁혔다 |
| gate:change.diff | — | — | — | 기준선 대비 | 002 | 기준선과 달라진 좌표 6개: /paths/~1alerts/post/description, /paths/~1alerts/post/responses/502, /paths/~1alerts/post/responses/504, /paths/~1alerts/post/responses/503/$ref, /components/schemas/AlertStatus/description, /components/responses/AlertCreateUnavailable |
| gate:change.diff | — | — | — | * | 002 | 달라진 좌표 6개: /paths/~1alerts/post/description, /paths/~1alerts/post/responses/502, /paths/~1alerts/post/responses/504, /paths/~1alerts/post/responses/503/$ref, /components/schemas/AlertStatus/description, /components/responses/AlertCreateUnavailable |
| gate:change.point_churn | — | — | createAlert:502 | - | 002 | 판정 지점이 생겼다 |
| gate:change.point_churn | — | — | createAlert:504 | - | 002 | 판정 지점이 생겼다 |
| gate:change.point_churn | — | — | createAlert:UPSTREAM_RESPONSE_UNREADABLE | - | 002 | 판정 지점이 생겼다 |
| gate:change.point_churn | — | — | createAlert:UPSTREAM_TIMEOUT | - | 002 | 판정 지점이 생겼다 |
| gate:change.point_churn | — | — | createAlert:UPSTREAM_UNAVAILABLE | - | 002 | 판정 지점이 생겼다 |
| gate:change.version_label | — | — | — | tennis-alert-api-v2.yaml | 002 | 파일명의 판 번호 2과 계약의 주 버전 1이 어긋난다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:400 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:404 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:500 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:502 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:503 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getCourtAvailability:504 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:200 | AlertList.items[].delivery | 003 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:400 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:401 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:500 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | listAlerts:503 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:201 | Alert.delivery | 003 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:200 | Alert.delivery | 003 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:400 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:401 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:409 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:422 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:500 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:502 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:503 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | createAlert:504 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:200 | Alert.delivery | 003 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:400 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:401 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:404 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:500 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | getAlert:503 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:200 | Alert.delivery | 003 | 형식: attemptCount: 계약 None → 구현 int32 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:400 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:401 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:404 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:500 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:response.value_narrowed | narrowed | harmless_to_client | cancelAlert:503 | Problem | 003 | 형식: 2곳에서 좁혔다 |
| gate:change.unmatched_record | — | — | — | /components/schemas/AlertStatus/description | 003 | 기록 cc_watching_check_source이 가리키는 자리가 판본 diff에 없다 |
| gate:change.unmatched_record | — | — | — | /paths/~1alerts/post/responses/502 | 003 | 기록 cc_create_upstream_unreadable이 가리키는 자리가 판본 diff에 없다 |
| gate:change.unmatched_record | — | — | — | /paths/~1alerts/post/responses/504 | 003 | 기록 cc_create_upstream_timeout이 가리키는 자리가 판본 diff에 없다 |
| gate:change.unmatched_record | — | — | — | /paths/~1alerts/post/responses/503 | 003 | 기록 cc_create_503_covers_upstream이 가리키는 자리가 판본 diff에 없다 |
| gate:change.unmatched_record | — | — | — | /components/responses/AlertCreateUnavailable | 003 | 기록 cc_alert_create_unavailable_response이 가리키는 자리가 판본 diff에 없다 |
| gate:change.diff | — | — | — | 기준선 대비 | 003 | 기준선과 달라진 좌표 10개: /paths/~1alerts/post/description, /paths/~1alerts/post/responses/502, /paths/~1alerts/post/responses/504, /paths/~1alerts/post/responses/201/description, /paths/~1alerts/post/responses/201/content/application~1json/example/lastCheckedAt, /paths/~1alerts/post/responses/503/$ref, /components/headers/IdempotencyReplayed/description, /components/schemas/AlertStatus/description, /components/schemas/CourtAvailability/properties/slots/description, /components/responses/AlertCreateUnavailable |
| gate:change.diff | — | — | — | * | 003 | 달라진 좌표 5개: /paths/~1alerts/post/description, /paths/~1alerts/post/responses/201/description, /paths/~1alerts/post/responses/201/content/application~1json/example/lastCheckedAt, /components/headers/IdempotencyReplayed/description, /components/schemas/CourtAvailability/properties/slots/description |
| gate:change.version_label | — | — | — | tennis-alert-api-v3.yaml | 003 | 파일명의 판 번호 3과 계약의 주 버전 1이 어긋난다 |

기록에 붙은 라벨의 뜻이다. 손해가 없는 차이는 위반이 아니라 여기 온다 — 판정은 그대로 남기고 리포트에서 뒤로 보낼 뿐이다.

| 라벨 | 뜻 |
| --- | --- |
| harmless_to_client | 쓰는 쪽에 손해가 없는 차이다. 응답의 값의 범위나 형식이 계약보다 좁은 자리다. 계약대로 읽는 쪽은 받을 수 있다고 믿은 값의 부분집합을 받는다. 리포트에서 뒤로 온다. |
