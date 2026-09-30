# 계약 적합성 run 리포트 — 2026-09-30_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-30T05:23:59+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 002까지 왔다. 위반 524건 가운데 1건을 코드로 닫고 0건을 계약으로 닫았으며 523건이 아직 열려 있다. 서 있는 결정은 5건, 계약 변경 기록은 0건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api.yaml-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api.yaml-v1.yaml`이다.

`decision_risk`는 **low** 수준이다. 올릴 근거는 없다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:extract.blind@None: 계약은 (상태 코드, 에러 코드) 쌍을 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다
- gate:response.required_weakened@getCourtAvailability:200: 계약이 항상 담겠다고 보장한 필드 ['confirmedAt', 'courtId', 'courtName', 'date', 'reservationUrl', 'slots', 'stale', 'staleReason']
- gate:response.required_weakened@getCourtAvailability:200: 계약이 항상 담겠다고 보장한 필드 ['available', 'endTime', 'startTime']
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getCourtAvailability:400: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:400: 계약이 닫아 둔 객체다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getCourtAvailability:404: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:404: 계약이 닫아 둔 객체다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getCourtAvailability:500: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:500: 계약이 닫아 둔 객체다
- gate:response.header_missing@getCourtAvailability:500: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:502: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getCourtAvailability:502: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:502: 계약이 닫아 둔 객체다
- gate:response.header_missing@getCourtAvailability:502: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getCourtAvailability:503: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:503: 계약이 닫아 둔 객체다
- gate:response.header_missing@getCourtAvailability:503: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getCourtAvailability:504: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getCourtAvailability:504: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getCourtAvailability:504: 계약이 닫아 둔 객체다
- gate:response.header_missing@getCourtAvailability:504: 계약이 싣겠다고 선언한 헤더다
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['items']
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@listAlerts:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@listAlerts:400: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@listAlerts:400: 계약이 닫아 둔 객체다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@listAlerts:401: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@listAlerts:401: 계약이 닫아 둔 객체다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@listAlerts:500: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@listAlerts:500: 계약이 닫아 둔 객체다
- gate:response.header_missing@listAlerts:500: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@listAlerts:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@listAlerts:503: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@listAlerts:503: 계약이 닫아 둔 객체다
- gate:response.header_missing@listAlerts:503: 계약이 싣겠다고 선언한 헤더다
- gate:response.required_weakened@createAlert:201: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@createAlert:201: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@createAlert:201: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.header_missing@createAlert:201: 계약이 싣겠다고 선언한 헤더다
- gate:response.header_missing@createAlert:201: 계약이 싣겠다고 선언한 헤더다
- gate:response.required_weakened@createAlert:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@createAlert:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@createAlert:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.header_missing@createAlert:200: 계약이 싣겠다고 선언한 헤더다
- gate:response.header_missing@createAlert:200: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@createAlert:400: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:400: 계약이 닫아 둔 객체다
- gate:response.type_changed@createAlert:400: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@createAlert:401: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:401: 계약이 닫아 둔 객체다
- gate:response.type_changed@createAlert:401: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:409: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@createAlert:409: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:409: 계약이 닫아 둔 객체다
- gate:response.type_changed@createAlert:409: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:422: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@createAlert:422: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:422: 계약이 닫아 둔 객체다
- gate:response.type_changed@createAlert:422: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@createAlert:500: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:500: 계약이 닫아 둔 객체다
- gate:response.type_changed@createAlert:500: 계약 ['integer'] → 구현 ['string']
- gate:response.header_missing@createAlert:500: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@createAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@createAlert:503: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@createAlert:503: 계약이 닫아 둔 객체다
- gate:response.type_changed@createAlert:503: 계약 ['integer'] → 구현 ['string']
- gate:response.header_missing@createAlert:503: 계약이 싣겠다고 선언한 헤더다
- gate:response.required_weakened@getAlert:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@getAlert:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@getAlert:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getAlert:400: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:400: 계약이 닫아 둔 객체다
- gate:response.type_changed@getAlert:400: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getAlert:401: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:401: 계약이 닫아 둔 객체다
- gate:response.type_changed@getAlert:401: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getAlert:404: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:404: 계약이 닫아 둔 객체다
- gate:response.type_changed@getAlert:404: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getAlert:500: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:500: 계약이 닫아 둔 객체다
- gate:response.type_changed@getAlert:500: 계약 ['integer'] → 구현 ['string']
- gate:response.header_missing@getAlert:500: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@getAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@getAlert:503: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@getAlert:503: 계약이 닫아 둔 객체다
- gate:response.type_changed@getAlert:503: 계약 ['integer'] → 구현 ['string']
- gate:response.header_missing@getAlert:503: 계약이 싣겠다고 선언한 헤더다
- gate:response.required_weakened@cancelAlert:200: 계약이 항상 담겠다고 보장한 필드 ['alertId', 'checkDelayed', 'courtId', 'courtName', 'createdAt', 'date', 'delivery', 'expiresAt', 'lastCheckedAt', 'reservationUrl', 'slot', 'status']
- gate:response.required_weakened@cancelAlert:200: 계약이 항상 담겠다고 보장한 필드 ['endTime', 'startTime']
- gate:response.required_weakened@cancelAlert:200: 계약이 항상 담겠다고 보장한 필드 ['attemptCount', 'failureReason', 'lastAttemptAt', 'status']
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:400: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@cancelAlert:400: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:400: 계약이 닫아 둔 객체다
- gate:response.type_changed@cancelAlert:400: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:401: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@cancelAlert:401: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:401: 계약이 닫아 둔 객체다
- gate:response.type_changed@cancelAlert:401: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:404: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@cancelAlert:404: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:404: 계약이 닫아 둔 객체다
- gate:response.type_changed@cancelAlert:404: 계약 ['integer'] → 구현 ['string']
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:500: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@cancelAlert:500: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:500: 계약이 닫아 둔 객체다
- gate:response.type_changed@cancelAlert:500: 계약 ['integer'] → 구현 ['string']
- gate:response.header_missing@cancelAlert:500: 계약이 싣겠다고 선언한 헤더다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.field_missing@cancelAlert:503: 계약이 응답에 담겠다고 선언한 필드다
- gate:response.required_weakened@cancelAlert:503: 계약이 항상 담겠다고 보장한 필드 ['code', 'retryable', 'status', 'title', 'type']
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.field_extra_closed@cancelAlert:503: 계약이 닫아 둔 객체다
- gate:response.type_changed@cancelAlert:503: 계약 ['integer'] → 구현 ['string']
- gate:response.header_missing@cancelAlert:503: 계약이 싣겠다고 선언한 헤더다

루브릭:
- min_total: 1.75 < 4.2
- min_axis.request_tolerance: 3 < 4.0
- min_axis.response_fidelity: 1 < 4.0
- min_axis.failure_faithfulness: 1 < 4.0
- min_axis.state_continuity: 1 < 4.0
- min_axis.judgement_disclosure: 3 < 4.0

색인과 실물이 어긋난 경로가 11곳이다. 게이트는 색인이 선언한 것을 판정하므로 선언 밖에 놓인 파일은 판정되지 않은 표면이 된다.

| 경로 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/AlertController.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiMapper.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiModels.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityController.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/OpenApiConfiguration.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertCommand.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AlertService.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/ApiException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityService.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/CreateAlertResult.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DomainModels.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |

판정되지 않은 검사가 있다: changes, g1. 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |
| 002 | REJECT | PASS | REJECT | PASS | 523 | 4 | 1.75 |

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

신고된 결정은 5건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_bearer_subject | 계약이 Bearer 토큰의 검증 체계와 사용자 식별자 형식을 정하지 않은 상황에서 토큰을 어떻게 사용자로 연결할 것인가? | 비어 있지 않은 Bearer 토큰의 값 자체를 사용자 식별자로 사용한다. | least_harm | medium | 인증 어댑터와 사용자 식별 매핑을 별도 포트로 추출하고 네 오퍼레이션의 호출부를 교체해야 한다. | createAlert:401, listAlerts:401, getAlert:401, cancelAlert:401 | standing |
| d_lazy_expiry_evaluation | 인메모리 구현에서 서버 작업으로 선언된 만료 전이를 어떤 실행 지점에서 보장할 것인가? | 목록·단건 조회·해제·예약 상태 확인 시점에 지연 평가한다. | least_harm | medium | 만료 작업자와 저장소 상태 전이 코드를 추가하고 lazy transition 로직을 제거해야 한다. | listAlerts:200, getAlert:200, cancelAlert:200 | standing |
| d_alert_slot_upstream_failure | 신청 생성 중 시간대 검증을 위한 외부 확인이 실패했지만 createAlert 응답에 upstream 전용 상태와 코드가 선언되지 않은 경우 무엇을 반환할 것인가? | 500 INTERNAL_ERROR와 retryAfterSeconds=5를 반환한다. | least_harm | medium | 신청 생성의 검증 결과 타입과 컨트롤러 오류 매핑을 변경하고 계약에 선언된 오류 좌표와 다시 대조해야 한다. | createAlert:500, createAlert:INTERNAL_ERROR | standing |
| d_idempotency_retry_windows | 계약이 예시로 제시한 재시도 대기값을 인메모리 구현의 명시값으로 어떻게 사용할 것인가? | UPSTREAM 계열은 20초, CONCURRENT_UPDATE_CONFLICT는 1초, INTERNAL_ERROR는 5초를 사용한다. | contract_analogy | high | 오류 매핑의 상수와 Retry-After 응답 검증을 함께 수정해야 한다. | getCourtAvailability:502, getCourtAvailability:503, getCourtAvailability:504, createAlert:500, createAlert:503 | standing |
| d_delivery_pending_state | 알림 발송 경로가 out_of_scope인 상태에서 빈자리 발견을 신청 조회에 어떻게 표현할 것인가? | 빈자리를 발견하면 PENDING delivery를 기록하고 신청 상태는 WATCHING으로 유지한다. | spec_implication | high | 발송 포트와 작업자를 추가하고 PENDING 항목의 성공·실패 전이를 연결해야 한다. | getAlert:200, listAlerts:200, cancelAlert:200 | standing |

`d_bearer_subject` — 계약은 Bearer 토큰을 사용한다고만 정하고 사용자 검증 포트를 제공하지 않았으므로, 범위를 넓히지 않고 본인별 격리만 보장한다.

`d_lazy_expiry_evaluation` — 저장소와 메시지 발송 경로가 범위 밖이고 구현은 인메모리이므로, 노출되는 상태와 확인·발송 중단을 요청 경계에서 보장한다.

`d_alert_slot_upstream_failure` — createAlert가 선언한 실패 좌표 안에서 외부 확인 실패를 표현하면서 새 상태나 오류 코드를 발명하지 않는다.

`d_idempotency_retry_windows` — NFR-3의 확인 간격과 각 응답 예시에 같은 값이 명시되어 있어 별도 수치를 만들지 않는다.

`d_delivery_pending_state` — 계약은 실제 발송 전까지 PENDING이 가능하고 발송 경로는 out_of_scope이므로, HTTP 표면에서 관찰 가능한 대기 상태까지만 구현한다.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 524건 중 1건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_003a20 | response.field_missing | getCourtAvailability:400 | Problem.title | open | — | 0 | 002 | — |
| v_00483b | response.field_extra_closed | createAlert:400 | Problem.courtName | open | — | 0 | 002 | — |
| v_009e8e | response.field_extra_closed | getAlert:401 | Problem.slot | open | — | 0 | 002 | — |
| v_011662 | response.field_extra_closed | getCourtAvailability:404 | Problem.staleReason | open | — | 0 | 002 | — |
| v_015128 | response.field_missing | createAlert:422 | Problem.title | open | — | 0 | 002 | — |
| v_019094 | response.field_missing | getAlert:404 | Problem.instance | open | — | 0 | 002 | — |
| v_01c7ac | response.field_missing | getAlert:500 | Problem.retryable | open | — | 0 | 002 | — |
| v_01e6b5 | response.required_weakened | getCourtAvailability:400 | Problem | open | — | 0 | 002 | — |
| v_01ef74 | response.field_extra_closed | createAlert:422 | Problem.alertId | open | — | 0 | 002 | — |
| v_02a4d4 | response.field_missing | getCourtAvailability:404 | Problem.code | open | — | 0 | 002 | — |
| v_02b4f3 | response.field_extra_closed | cancelAlert:401 | Problem.delivery | open | — | 0 | 002 | — |
| v_03e216 | response.field_extra_closed | getAlert:404 | Problem.date | open | — | 0 | 002 | — |
| v_03f655 | response.field_missing | getCourtAvailability:504 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_047bda | response.required_weakened | cancelAlert:200 | Alert | open | — | 0 | 002 | — |
| v_04db94 | response.field_missing | getCourtAvailability:500 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_04f063 | response.field_missing | createAlert:500 | Problem.type | open | — | 0 | 002 | — |
| v_052c20 | response.required_weakened | listAlerts:400 | Problem | open | — | 0 | 002 | — |
| v_0543e3 | response.field_extra_closed | createAlert:400 | Problem.courtId | open | — | 0 | 002 | — |
| v_056a69 | response.field_extra_closed | createAlert:400 | Problem.slot | open | — | 0 | 002 | — |
| v_056b7b | response.type_changed | getAlert:404 | Problem.status | open | — | 0 | 002 | — |
| v_05b75f | response.field_extra_closed | createAlert:500 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_05cf83 | response.field_extra_closed | getCourtAvailability:500 | Problem.slots | open | — | 0 | 002 | — |
| v_06155f | response.required_weakened | cancelAlert:200 | Alert.delivery | open | — | 0 | 002 | — |
| v_062e09 | response.field_extra_closed | cancelAlert:401 | Problem.date | open | — | 0 | 002 | — |
| v_0706d4 | response.field_missing | createAlert:422 | Problem.detail | open | — | 0 | 002 | — |
| v_07195d | response.field_missing | cancelAlert:503 | Problem.code | open | — | 0 | 002 | — |
| v_0768d0 | response.field_missing | createAlert:401 | Problem.instance | open | — | 0 | 002 | — |
| v_079a98 | response.type_changed | createAlert:422 | Problem.status | open | — | 0 | 002 | — |
| v_087ac0 | response.field_extra_closed | getCourtAvailability:500 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_08b937 | response.field_missing | createAlert:503 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_08bfc8 | response.field_missing | createAlert:500 | Problem.retryable | open | — | 0 | 002 | — |
| v_097622 | response.required_weakened | cancelAlert:401 | Problem | open | — | 0 | 002 | — |
| v_0a1647 | response.field_missing | listAlerts:503 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_0a7f0c | response.field_missing | createAlert:409 | Problem.type | open | — | 0 | 002 | — |
| v_0a9075 | response.field_missing | createAlert:503 | Problem.instance | open | — | 0 | 002 | — |
| v_0ad8e5 | response.field_extra_closed | getAlert:401 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_0adfdc | response.field_missing | createAlert:409 | Problem.title | open | — | 0 | 002 | — |
| v_0d0400 | response.field_extra_closed | getAlert:404 | Problem.delivery | open | — | 0 | 002 | — |
| v_0d41d1 | response.field_missing | cancelAlert:400 | Problem.type | open | — | 0 | 002 | — |
| v_0d9bf7 | response.field_extra_closed | getCourtAvailability:500 | Problem.date | open | — | 0 | 002 | — |
| v_0e31ec | response.field_missing | createAlert:400 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_0e35c3 | response.required_weakened | listAlerts:401 | Problem | open | — | 0 | 002 | — |
| v_0e3a73 | response.field_extra_closed | cancelAlert:503 | Problem.courtName | open | — | 0 | 002 | — |
| v_0fa49f | response.field_missing | createAlert:500 | Problem.traceId | open | — | 0 | 002 | — |
| v_0facb8 | response.field_extra_closed | cancelAlert:503 | Problem.delivery | open | — | 0 | 002 | — |
| v_0ff2fa | response.field_extra_closed | cancelAlert:500 | Problem.courtName | open | — | 0 | 002 | — |
| v_10527f | response.field_missing | createAlert:409 | Problem.retryable | open | — | 0 | 002 | — |
| v_10b33d | response.field_missing | createAlert:422 | Problem.code | open | — | 0 | 002 | — |
| v_10e41a | response.field_extra_closed | getCourtAvailability:504 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_10ed55 | response.field_missing | getCourtAvailability:503 | Problem.title | open | — | 0 | 002 | — |
| v_11a5ca | response.field_missing | getCourtAvailability:404 | Problem.title | open | — | 0 | 002 | — |
| v_11b075 | response.field_extra_closed | getCourtAvailability:504 | Problem.staleReason | open | — | 0 | 002 | — |
| v_11e4b2 | response.field_missing | listAlerts:400 | Problem.retryable | open | — | 0 | 002 | — |
| v_130bc3 | response.field_missing | getAlert:500 | Problem.traceId | open | — | 0 | 002 | — |
| v_13cda4 | response.field_extra_closed | createAlert:500 | Problem.date | open | — | 0 | 002 | — |
| v_13f790 | response.field_extra_closed | getAlert:404 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_14c66f | response.field_missing | getCourtAvailability:504 | Problem.code | open | — | 0 | 002 | — |
| v_15798d | response.required_weakened | getAlert:200 | Alert.delivery | open | — | 0 | 002 | — |
| v_15abcd | response.field_extra_closed | getAlert:500 | Problem.courtName | open | — | 0 | 002 | — |
| v_15f2c7 | response.field_missing | listAlerts:500 | Problem.traceId | open | — | 0 | 002 | — |
| v_169ccb | response.field_extra_closed | createAlert:401 | Problem.createdAt | open | — | 0 | 002 | — |
| v_16b92c | response.field_extra_closed | cancelAlert:500 | Problem.slot | open | — | 0 | 002 | — |
| v_16be7f | response.field_extra_closed | cancelAlert:400 | Problem.courtId | open | — | 0 | 002 | — |
| v_1727fc | response.field_extra_closed | getCourtAvailability:503 | Problem.stale | open | — | 0 | 002 | — |
| v_174e11 | response.field_missing | getAlert:500 | Problem.code | open | — | 0 | 002 | — |
| v_174f79 | response.field_missing | getCourtAvailability:502 | Problem.instance | open | — | 0 | 002 | — |
| v_1757b8 | response.header_missing | listAlerts:503 | headers.Retry-After | open | — | 0 | 002 | — |
| v_177bac | response.field_missing | getCourtAvailability:400 | Problem.traceId | open | — | 0 | 002 | — |
| v_183564 | response.field_missing | getCourtAvailability:503 | Problem.status | open | — | 0 | 002 | — |
| v_19a4c1 | response.field_extra_closed | cancelAlert:500 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_19cd9d | response.field_extra_closed | createAlert:401 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_1a07e2 | response.field_extra_closed | createAlert:500 | Problem.courtId | open | — | 0 | 002 | — |
| v_1a6470 | response.field_extra_closed | getCourtAvailability:504 | Problem.date | open | — | 0 | 002 | — |
| v_1aa0fa | response.field_extra_closed | createAlert:500 | Problem.alertId | open | — | 0 | 002 | — |
| v_1b59f6 | response.field_missing | createAlert:422 | Problem.retryable | open | — | 0 | 002 | — |
| v_1bfe9f | response.type_changed | createAlert:500 | Problem.status | open | — | 0 | 002 | — |
| v_1c378f | response.field_missing | getAlert:503 | Problem.instance | open | — | 0 | 002 | — |
| v_1d0d58 | response.field_missing | getCourtAvailability:404 | Problem.instance | open | — | 0 | 002 | — |
| v_1d5c1d | response.field_missing | getAlert:400 | Problem.retryable | open | — | 0 | 002 | — |
| v_1d7641 | response.field_extra_closed | cancelAlert:404 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_1dc859 | response.field_extra_closed | cancelAlert:503 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_1e4b9b | response.field_missing | getCourtAvailability:504 | Problem.type | open | — | 0 | 002 | — |
| v_1f4592 | response.field_missing | listAlerts:401 | Problem.title | open | — | 0 | 002 | — |
| v_1f5ce7 | response.field_missing | createAlert:401 | Problem.retryable | open | — | 0 | 002 | — |
| v_1fa9ba | response.field_missing | getCourtAvailability:502 | Problem.code | open | — | 0 | 002 | — |
| v_1fe78b | response.field_extra_closed | getAlert:401 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_20f37a | response.field_extra_closed | createAlert:401 | Problem.delivery | open | — | 0 | 002 | — |
| v_2252d8 | response.field_missing | getCourtAvailability:400 | Problem.type | open | — | 0 | 002 | — |
| v_233e70 | response.field_missing | listAlerts:401 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_2390c4 | response.required_weakened | createAlert:503 | Problem | open | — | 0 | 002 | — |
| v_23d281 | response.field_extra_closed | getAlert:500 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_24d5a8 | response.field_missing | cancelAlert:400 | Problem.title | open | — | 0 | 002 | — |
| v_24e600 | response.field_extra_closed | getCourtAvailability:404 | Problem.date | open | — | 0 | 002 | — |
| v_24febe | response.field_missing | cancelAlert:503 | Problem.detail | open | — | 0 | 002 | — |
| v_25e91c | response.field_missing | getCourtAvailability:500 | Problem.instance | open | — | 0 | 002 | — |
| v_26f13c | response.required_weakened | getCourtAvailability:502 | Problem | open | — | 0 | 002 | — |
| v_276d86 | response.field_extra_closed | createAlert:409 | Problem.createdAt | open | — | 0 | 002 | — |
| v_280187 | response.field_missing | getCourtAvailability:404 | Problem.traceId | open | — | 0 | 002 | — |
| v_29a8b7 | response.field_missing | getCourtAvailability:504 | Problem.traceId | open | — | 0 | 002 | — |
| v_2a1543 | response.field_missing | createAlert:503 | Problem.code | open | — | 0 | 002 | — |
| v_2a1961 | response.field_missing | cancelAlert:503 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_2a3dcf | response.field_extra_closed | createAlert:409 | Problem.date | open | — | 0 | 002 | — |
| v_2a69da | response.field_extra_closed | getCourtAvailability:404 | Problem.courtName | open | — | 0 | 002 | — |
| v_2b343d | response.field_extra_closed | getAlert:400 | Problem.createdAt | open | — | 0 | 002 | — |
| v_2b4239 | response.field_extra_closed | createAlert:500 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_2b5103 | response.field_extra_closed | cancelAlert:404 | Problem.courtId | open | — | 0 | 002 | — |
| v_2b89c9 | response.field_extra_closed | getAlert:503 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_2ceab5 | response.field_extra_closed | getAlert:503 | Problem.courtId | open | — | 0 | 002 | — |
| v_2cecbf | response.header_missing | getCourtAvailability:504 | headers.Retry-After | open | — | 0 | 002 | — |
| v_2d04f7 | response.field_extra_closed | createAlert:500 | Problem.delivery | open | — | 0 | 002 | — |
| v_2e1f8c | response.field_missing | getAlert:404 | Problem.type | open | — | 0 | 002 | — |
| v_2e49de | response.field_extra_closed | getCourtAvailability:404 | Problem.stale | open | — | 0 | 002 | — |
| v_2eccf8 | response.field_extra_closed | cancelAlert:400 | Problem.slot | open | — | 0 | 002 | — |
| v_2f1e0b | response.field_missing | listAlerts:500 | Problem.instance | open | — | 0 | 002 | — |
| v_2fd24a | response.required_weakened | createAlert:200 | Alert.slot | open | — | 0 | 002 | — |
| v_3015d2 | response.required_weakened | cancelAlert:503 | Problem | open | — | 0 | 002 | — |
| v_301786 | response.field_missing | listAlerts:503 | Problem.status | open | — | 0 | 002 | — |
| v_310e16 | response.required_weakened | listAlerts:200 | AlertList.items[].slot | open | — | 0 | 002 | — |
| v_31520d | response.required_weakened | cancelAlert:404 | Problem | open | — | 0 | 002 | — |
| v_336018 | response.header_missing | getAlert:503 | headers.Retry-After | open | — | 0 | 002 | — |
| v_33bfb5 | response.field_extra_closed | cancelAlert:503 | Problem.createdAt | open | — | 0 | 002 | — |
| v_344122 | response.field_extra_closed | getCourtAvailability:500 | Problem.staleReason | open | — | 0 | 002 | — |
| v_34d799 | response.field_missing | cancelAlert:404 | Problem.detail | open | — | 0 | 002 | — |
| v_350b45 | response.field_missing | cancelAlert:500 | Problem.title | open | — | 0 | 002 | — |
| v_354ec4 | response.field_missing | listAlerts:503 | Problem.instance | open | — | 0 | 002 | — |
| v_35ab97 | response.field_missing | getAlert:503 | Problem.retryable | open | — | 0 | 002 | — |
| v_35bb01 | response.required_weakened | createAlert:500 | Problem | open | — | 0 | 002 | — |
| v_366701 | response.required_weakened | getAlert:401 | Problem | open | — | 0 | 002 | — |
| v_36849e | response.field_missing | cancelAlert:400 | Problem.instance | open | — | 0 | 002 | — |
| v_36a01a | response.field_missing | getAlert:500 | Problem.title | open | — | 0 | 002 | — |
| v_36eeb7 | response.field_extra_closed | getCourtAvailability:400 | Problem.staleReason | open | — | 0 | 002 | — |
| v_375234 | response.field_missing | cancelAlert:500 | Problem.type | open | — | 0 | 002 | — |
| v_37f79f | response.field_extra_closed | createAlert:409 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_3898c7 | response.field_missing | createAlert:409 | Problem.traceId | open | — | 0 | 002 | — |
| v_39b311 | response.field_extra_closed | cancelAlert:404 | Problem.courtName | open | — | 0 | 002 | — |
| v_39cb98 | response.field_extra_closed | cancelAlert:503 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_3a4002 | response.field_missing | getCourtAvailability:504 | Problem.detail | open | — | 0 | 002 | — |
| v_3b1734 | response.field_extra_closed | getAlert:400 | Problem.alertId | open | — | 0 | 002 | — |
| v_3b2c4c | response.header_missing | createAlert:503 | headers.Retry-After | open | — | 0 | 002 | — |
| v_3b4526 | response.field_extra_closed | createAlert:401 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_3b7c9a | response.field_missing | getAlert:401 | Problem.code | open | — | 0 | 002 | — |
| v_3c35e2 | response.type_changed | cancelAlert:404 | Problem.status | open | — | 0 | 002 | — |
| v_3ce2b1 | response.field_extra_closed | getAlert:503 | Problem.courtName | open | — | 0 | 002 | — |
| v_3dd386 | response.field_extra_closed | getCourtAvailability:500 | Problem.stale | open | — | 0 | 002 | — |
| v_3e0083 | response.field_extra_closed | createAlert:503 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_3e0888 | response.field_missing | cancelAlert:400 | Problem.retryable | open | — | 0 | 002 | — |
| v_3e9888 | response.field_extra_closed | cancelAlert:500 | Problem.date | open | — | 0 | 002 | — |
| v_3f27eb | response.field_extra_closed | createAlert:409 | Problem.alertId | open | — | 0 | 002 | — |
| v_3f44cf | response.header_missing | createAlert:201 | headers.Location | open | — | 0 | 002 | — |
| v_3f495f | response.field_extra_closed | cancelAlert:404 | Problem.alertId | open | — | 0 | 002 | — |
| v_403d15 | response.field_missing | getCourtAvailability:500 | Problem.detail | open | — | 0 | 002 | — |
| v_405cfa | response.field_extra_closed | getAlert:503 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_407d85 | response.field_missing | createAlert:500 | Problem.code | open | — | 0 | 002 | — |
| v_40839c | response.required_weakened | cancelAlert:200 | Alert.slot | open | — | 0 | 002 | — |
| v_40ecd1 | response.field_extra_closed | listAlerts:500 | Problem.items | open | — | 0 | 002 | — |
| v_41aa45 | response.field_extra_closed | cancelAlert:500 | Problem.courtId | open | — | 0 | 002 | — |
| v_422d3a | response.field_extra_closed | createAlert:503 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_425e5a | response.field_extra_closed | getCourtAvailability:404 | Problem.confirmedAt | open | — | 0 | 002 | — |
| v_42778f | extract.blind | — | x-error-codes | open | — | 0 | 002 | — |
| v_42f79e | response.field_missing | listAlerts:500 | Problem.code | open | — | 0 | 002 | — |
| v_438a78 | response.field_missing | cancelAlert:401 | Problem.detail | open | — | 0 | 002 | — |
| v_43a107 | response.field_extra_closed | createAlert:422 | Problem.courtId | open | — | 0 | 002 | — |
| v_44094b | response.field_missing | getAlert:401 | Problem.traceId | open | — | 0 | 002 | — |
| v_4464ed | response.field_missing | cancelAlert:400 | Problem.detail | open | — | 0 | 002 | — |
| v_44cac3 | response.field_missing | cancelAlert:500 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_451ab1 | response.field_missing | getCourtAvailability:502 | Problem.detail | open | — | 0 | 002 | — |
| v_45345e | response.field_extra_closed | cancelAlert:404 | Problem.createdAt | open | — | 0 | 002 | — |
| v_454a1b | response.field_missing | createAlert:503 | Problem.title | open | — | 0 | 002 | — |
| v_4572ca | response.field_missing | getCourtAvailability:503 | Problem.traceId | open | — | 0 | 002 | — |
| v_4672e8 | response.field_missing | getCourtAvailability:404 | Problem.type | open | — | 0 | 002 | — |
| v_46da31 | response.field_extra_closed | cancelAlert:404 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_4709b9 | response.field_missing | createAlert:500 | Problem.instance | open | — | 0 | 002 | — |
| v_4712ef | response.field_extra_closed | getAlert:503 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_483212 | response.type_changed | cancelAlert:401 | Problem.status | open | — | 0 | 002 | — |
| v_483d64 | response.field_missing | getAlert:400 | Problem.type | open | — | 0 | 002 | — |
| v_486eea | response.field_missing | getCourtAvailability:504 | Problem.instance | open | — | 0 | 002 | — |
| v_489bd2 | response.field_missing | getAlert:400 | Problem.code | open | — | 0 | 002 | — |
| v_4955fc | response.field_missing | getAlert:404 | Problem.retryable | open | — | 0 | 002 | — |
| v_49d5c5 | response.field_extra_closed | createAlert:400 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_4a95f6 | response.field_missing | getAlert:500 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_4adca3 | response.field_missing | getAlert:503 | Problem.code | open | — | 0 | 002 | — |
| v_4ae57b | response.field_missing | cancelAlert:401 | Problem.retryable | open | — | 0 | 002 | — |
| v_4afe52 | response.field_missing | createAlert:503 | Problem.detail | open | — | 0 | 002 | — |
| v_4b03df | response.field_extra_closed | createAlert:401 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_4b5b36 | response.field_extra_closed | cancelAlert:500 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_4b5e7c | response.field_extra_closed | getAlert:400 | Problem.delivery | open | — | 0 | 002 | — |
| v_4b7e01 | response.field_extra_closed | cancelAlert:503 | Problem.date | open | — | 0 | 002 | — |
| v_4c86f3 | response.type_changed | createAlert:401 | Problem.status | open | — | 0 | 002 | — |
| v_4cbdfb | response.field_extra_closed | createAlert:503 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_4cd049 | response.field_missing | cancelAlert:404 | Problem.retryable | open | — | 0 | 002 | — |
| v_4d65fd | response.field_missing | listAlerts:500 | Problem.status | open | — | 0 | 002 | — |
| v_4e7326 | response.field_missing | createAlert:409 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_4ea7c4 | response.field_extra_closed | getAlert:500 | Problem.slot | open | — | 0 | 002 | — |
| v_4ee57a | response.field_missing | listAlerts:400 | Problem.type | open | — | 0 | 002 | — |
| v_4f2e7b | response.field_extra_closed | createAlert:503 | Problem.courtName | open | — | 0 | 002 | — |
| v_4fcb76 | response.header_missing | cancelAlert:503 | headers.Retry-After | open | — | 0 | 002 | — |
| v_504b7e | response.type_changed | createAlert:400 | Problem.status | open | — | 0 | 002 | — |
| v_5072d5 | response.field_extra_closed | createAlert:503 | Problem.alertId | open | — | 0 | 002 | — |
| v_51ade6 | response.field_extra_closed | getAlert:503 | Problem.date | open | — | 0 | 002 | — |
| v_51bd29 | response.field_extra_closed | cancelAlert:401 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_51da6c | response.field_extra_closed | getAlert:404 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_51fcbf | response.field_missing | listAlerts:401 | Problem.retryable | open | — | 0 | 002 | — |
| v_520cda | response.field_extra_closed | cancelAlert:400 | Problem.date | open | — | 0 | 002 | — |
| v_5286a9 | response.field_extra_closed | createAlert:400 | Problem.alertId | open | — | 0 | 002 | — |
| v_536441 | response.field_extra_closed | getCourtAvailability:404 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_5375a0 | response.field_missing | getCourtAvailability:400 | Problem.instance | open | — | 0 | 002 | — |
| v_5469f0 | response.field_missing | cancelAlert:500 | Problem.retryable | open | — | 0 | 002 | — |
| v_5538b6 | response.field_extra_closed | getCourtAvailability:503 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_563a4d | response.field_extra_closed | createAlert:500 | Problem.courtName | open | — | 0 | 002 | — |
| v_563a6d | response.field_extra_closed | getCourtAvailability:400 | Problem.stale | open | — | 0 | 002 | — |
| v_56e2bd | response.field_missing | cancelAlert:500 | Problem.instance | open | — | 0 | 002 | — |
| v_576aff | response.field_missing | cancelAlert:500 | Problem.code | open | — | 0 | 002 | — |
| v_57d9d9 | response.field_missing | getCourtAvailability:504 | Problem.title | open | — | 0 | 002 | — |
| v_588478 | response.field_extra_closed | listAlerts:503 | Problem.items | open | — | 0 | 002 | — |
| v_58883e | response.required_weakened | getAlert:404 | Problem | open | — | 0 | 002 | — |
| v_588c7e | response.field_extra_closed | createAlert:503 | Problem.date | open | — | 0 | 002 | — |
| v_59297e | response.field_extra_closed | createAlert:503 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_595daf | response.field_extra_closed | cancelAlert:401 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_5a0cc1 | response.field_missing | getCourtAvailability:400 | Problem.detail | open | — | 0 | 002 | — |
| v_5ad1dc | response.field_missing | cancelAlert:404 | Problem.title | open | — | 0 | 002 | — |
| v_5b4184 | response.field_extra_closed | getAlert:503 | Problem.slot | open | — | 0 | 002 | — |
| v_5b69da | response.field_missing | getCourtAvailability:500 | Problem.title | open | — | 0 | 002 | — |
| v_5b9ae2 | response.field_extra_closed | getCourtAvailability:502 | Problem.date | open | — | 0 | 002 | — |
| v_5bdad4 | response.header_missing | createAlert:200 | headers.Location | open | — | 0 | 002 | — |
| v_5bf1da | response.field_extra_closed | cancelAlert:404 | Problem.date | open | — | 0 | 002 | — |
| v_5c3001 | response.field_extra_closed | createAlert:500 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_5cd8d1 | response.field_extra_closed | cancelAlert:500 | Problem.delivery | open | — | 0 | 002 | — |
| v_5d02a7 | response.field_extra_closed | cancelAlert:503 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_5e146c | response.field_extra_closed | createAlert:422 | Problem.slot | open | — | 0 | 002 | — |
| v_5e552e | response.field_extra_closed | getCourtAvailability:404 | Problem.slots | open | — | 0 | 002 | — |
| v_5fb627 | response.field_extra_closed | cancelAlert:401 | Problem.courtName | open | — | 0 | 002 | — |
| v_607258 | response.field_extra_closed | cancelAlert:401 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_6078e6 | response.field_extra_closed | getCourtAvailability:504 | Problem.courtName | open | — | 0 | 002 | — |
| v_608862 | response.field_extra_closed | getAlert:500 | Problem.date | open | — | 0 | 002 | — |
| v_60daa4 | response.field_missing | listAlerts:503 | Problem.traceId | open | — | 0 | 002 | — |
| v_61faee | response.field_missing | getCourtAvailability:503 | Problem.retryable | open | — | 0 | 002 | — |
| v_636b60 | response.field_extra_closed | getAlert:400 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_637516 | response.field_extra_closed | createAlert:422 | Problem.createdAt | open | — | 0 | 002 | — |
| v_63871f | response.field_missing | createAlert:500 | Problem.detail | open | — | 0 | 002 | — |
| v_645b75 | response.field_extra_closed | createAlert:503 | Problem.slot | open | — | 0 | 002 | — |
| v_650790 | response.field_missing | createAlert:400 | Problem.code | open | — | 0 | 002 | — |
| v_661103 | response.field_extra_closed | createAlert:401 | Problem.slot | open | — | 0 | 002 | — |
| v_662884 | response.required_weakened | listAlerts:200 | AlertList.items[].delivery | open | — | 0 | 002 | — |
| v_66c368 | response.header_missing | getCourtAvailability:500 | headers.Retry-After | open | — | 0 | 002 | — |
| v_67574f | response.field_missing | createAlert:401 | Problem.code | open | — | 0 | 002 | — |
| v_686f30 | response.required_weakened | createAlert:201 | Alert | open | — | 0 | 002 | — |
| v_688710 | response.field_missing | cancelAlert:404 | Problem.code | open | — | 0 | 002 | — |
| v_69051d | response.field_extra_closed | getCourtAvailability:400 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_693345 | response.field_extra_closed | cancelAlert:404 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_6986a6 | response.field_extra_closed | getAlert:500 | Problem.delivery | open | — | 0 | 002 | — |
| v_6a69fb | response.type_changed | getAlert:503 | Problem.status | open | — | 0 | 002 | — |
| v_6a7e90 | response.field_extra_closed | cancelAlert:503 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_6b46d0 | response.field_extra_closed | cancelAlert:400 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_6ba180 | response.field_extra_closed | createAlert:401 | Problem.courtId | open | — | 0 | 002 | — |
| v_6bcac7 | response.field_missing | cancelAlert:400 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_6be16a | response.field_extra_closed | cancelAlert:404 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_6cca4d | response.field_extra_closed | getAlert:404 | Problem.alertId | open | — | 0 | 002 | — |
| v_6cf634 | response.field_missing | getAlert:400 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_6dd422 | response.field_missing | createAlert:422 | Problem.instance | open | — | 0 | 002 | — |
| v_6ea2a4 | response.field_extra_closed | getCourtAvailability:504 | Problem.courtId | open | — | 0 | 002 | — |
| v_6ec551 | response.field_extra_closed | getAlert:400 | Problem.courtName | open | — | 0 | 002 | — |
| v_6ff840 | response.field_missing | getAlert:401 | Problem.type | open | — | 0 | 002 | — |
| v_708e9c | response.field_missing | listAlerts:400 | Problem.code | open | — | 0 | 002 | — |
| v_72158f | response.field_extra_closed | cancelAlert:400 | Problem.delivery | open | — | 0 | 002 | — |
| v_73bcfb | response.field_extra_closed | getCourtAvailability:503 | Problem.courtName | open | — | 0 | 002 | — |
| v_74b77e | response.field_missing | listAlerts:401 | Problem.detail | open | — | 0 | 002 | — |
| v_751e2f | response.field_missing | cancelAlert:404 | Problem.traceId | open | — | 0 | 002 | — |
| v_755950 | response.field_extra_closed | getCourtAvailability:400 | Problem.slots | open | — | 0 | 002 | — |
| v_75af80 | response.field_extra_closed | getCourtAvailability:504 | Problem.stale | open | — | 0 | 002 | — |
| v_75e3c4 | response.field_extra_closed | cancelAlert:400 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_7616d9 | response.field_missing | getCourtAvailability:503 | Problem.code | open | — | 0 | 002 | — |
| v_767b15 | response.field_extra_closed | cancelAlert:404 | Problem.delivery | open | — | 0 | 002 | — |
| v_7854e4 | response.field_extra_closed | createAlert:409 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_7968d4 | response.field_extra_closed | createAlert:503 | Problem.courtId | open | — | 0 | 002 | — |
| v_79a473 | response.field_missing | listAlerts:401 | Problem.instance | open | — | 0 | 002 | — |
| v_7a50e7 | response.field_extra_closed | createAlert:400 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_7a7359 | response.field_extra_closed | createAlert:422 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_7acd13 | response.required_weakened | createAlert:200 | Alert | open | — | 0 | 002 | — |
| v_7b4c38 | response.required_weakened | createAlert:422 | Problem | open | — | 0 | 002 | — |
| v_7beca7 | response.field_extra_closed | listAlerts:401 | Problem.items | open | — | 0 | 002 | — |
| v_7c0ca1 | response.field_extra_closed | createAlert:400 | Problem.createdAt | open | — | 0 | 002 | — |
| v_7c46ae | response.field_missing | getAlert:404 | Problem.code | open | — | 0 | 002 | — |
| v_7c55e4 | response.field_missing | cancelAlert:401 | Problem.code | open | — | 0 | 002 | — |
| v_7de05d | response.header_missing | listAlerts:500 | headers.Retry-After | open | — | 0 | 002 | — |
| v_7de922 | response.field_missing | cancelAlert:500 | Problem.traceId | open | — | 0 | 002 | — |
| v_7f1756 | response.field_missing | createAlert:400 | Problem.type | open | — | 0 | 002 | — |
| v_7f580b | response.field_extra_closed | getCourtAvailability:500 | Problem.confirmedAt | open | — | 0 | 002 | — |
| v_7faac2 | response.field_extra_closed | getAlert:400 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_809aea | response.field_missing | getAlert:500 | Problem.type | open | — | 0 | 002 | — |
| v_811d62 | response.field_extra_closed | cancelAlert:400 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_818faf | response.field_extra_closed | getAlert:401 | Problem.delivery | open | — | 0 | 002 | — |
| v_81d9dc | response.field_extra_closed | getAlert:401 | Problem.createdAt | open | — | 0 | 002 | — |
| v_82a28e | response.field_missing | cancelAlert:503 | Problem.traceId | open | — | 0 | 002 | — |
| v_82f5d9 | response.field_missing | getAlert:500 | Problem.instance | open | — | 0 | 002 | — |
| v_82f933 | response.field_extra_closed | getCourtAvailability:404 | Problem.courtId | open | — | 0 | 002 | — |
| v_835f42 | response.field_extra_closed | createAlert:503 | Problem.delivery | open | — | 0 | 002 | — |
| v_840977 | response.field_missing | listAlerts:400 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_844e19 | response.field_missing | createAlert:400 | Problem.traceId | open | — | 0 | 002 | — |
| v_845b0f | response.field_missing | listAlerts:503 | Problem.detail | open | — | 0 | 002 | — |
| v_84c85e | response.field_extra_closed | getAlert:404 | Problem.slot | open | — | 0 | 002 | — |
| v_86ce8e | response.field_missing | getCourtAvailability:404 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_882899 | response.field_extra_closed | cancelAlert:404 | Problem.slot | open | — | 0 | 002 | — |
| v_885afd | response.field_missing | getCourtAvailability:500 | Problem.traceId | open | — | 0 | 002 | — |
| v_894d41 | response.field_missing | cancelAlert:503 | Problem.retryable | open | — | 0 | 002 | — |
| v_89689f | response.field_missing | createAlert:400 | Problem.title | open | — | 0 | 002 | — |
| v_8affe5 | response.field_extra_closed | getAlert:500 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_8b1b67 | response.field_missing | createAlert:400 | Problem.detail | open | — | 0 | 002 | — |
| v_8ba7f5 | response.field_extra_closed | getCourtAvailability:502 | Problem.courtId | open | — | 0 | 002 | — |
| v_8cc4b2 | response.required_weakened | getCourtAvailability:500 | Problem | open | — | 0 | 002 | — |
| v_8d57da | response.field_missing | getAlert:503 | Problem.traceId | open | — | 0 | 002 | — |
| v_8d8338 | response.field_extra_closed | getAlert:404 | Problem.courtId | open | — | 0 | 002 | — |
| v_8dac61 | response.field_extra_closed | cancelAlert:400 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_8e5701 | response.field_missing | cancelAlert:503 | Problem.title | open | — | 0 | 002 | — |
| v_8e7ede | response.field_missing | getAlert:401 | Problem.title | open | — | 0 | 002 | — |
| v_8ec7bf | response.field_missing | getCourtAvailability:500 | Problem.type | open | — | 0 | 002 | — |
| v_8f12a2 | response.field_extra_closed | getAlert:401 | Problem.courtName | open | — | 0 | 002 | — |
| v_8fee5b | response.field_extra_closed | createAlert:422 | Problem.delivery | open | — | 0 | 002 | — |
| v_90996b | response.field_extra_closed | cancelAlert:503 | Problem.alertId | open | — | 0 | 002 | — |
| v_919892 | response.required_weakened | getCourtAvailability:404 | Problem | open | — | 0 | 002 | — |
| v_924322 | response.field_extra_closed | createAlert:400 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_9295b3 | response.field_missing | listAlerts:503 | Problem.type | open | — | 0 | 002 | — |
| v_931deb | response.field_missing | getAlert:401 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_932149 | response.required_weakened | getAlert:200 | Alert.slot | open | — | 0 | 002 | — |
| v_936138 | response.field_missing | getAlert:503 | Problem.detail | open | — | 0 | 002 | — |
| v_936cba | response.field_extra_closed | cancelAlert:400 | Problem.createdAt | open | — | 0 | 002 | — |
| v_93cb43 | response.field_missing | getCourtAvailability:500 | Problem.code | open | — | 0 | 002 | — |
| v_93dba3 | response.field_missing | cancelAlert:401 | Problem.traceId | open | — | 0 | 002 | — |
| v_93f5ef | response.field_extra_closed | getCourtAvailability:500 | Problem.courtId | open | — | 0 | 002 | — |
| v_93fa8b | response.field_missing | cancelAlert:401 | Problem.instance | open | — | 0 | 002 | — |
| v_94c498 | response.field_extra_closed | getAlert:503 | Problem.delivery | open | — | 0 | 002 | — |
| v_9571bc | response.field_missing | getCourtAvailability:404 | Problem.retryable | open | — | 0 | 002 | — |
| v_9599d4 | response.field_extra_closed | cancelAlert:401 | Problem.courtId | open | — | 0 | 002 | — |
| v_9685e7 | response.field_extra_closed | createAlert:401 | Problem.alertId | open | — | 0 | 002 | — |
| v_96b68e | response.field_extra_closed | getAlert:404 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_970375 | response.field_extra_closed | cancelAlert:500 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_97497e | response.field_extra_closed | createAlert:422 | Problem.date | open | — | 0 | 002 | — |
| v_9800b4 | response.header_missing | getAlert:500 | headers.Retry-After | open | — | 0 | 002 | — |
| v_98b426 | response.field_missing | getCourtAvailability:400 | Problem.code | open | — | 0 | 002 | — |
| v_98b6a8 | response.required_weakened | listAlerts:200 | AlertList.items[] | open | — | 0 | 002 | — |
| v_98dba0 | response.field_extra_closed | getCourtAvailability:400 | Problem.courtName | open | — | 0 | 002 | — |
| v_9916c1 | response.field_missing | getCourtAvailability:502 | Problem.retryable | open | — | 0 | 002 | — |
| v_994366 | response.field_missing | createAlert:503 | Problem.type | open | — | 0 | 002 | — |
| v_999c87 | response.field_extra_closed | createAlert:400 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_9a66dd | response.field_extra_closed | getCourtAvailability:502 | Problem.courtName | open | — | 0 | 002 | — |
| v_9a8c54 | response.field_extra_closed | getAlert:401 | Problem.alertId | open | — | 0 | 002 | — |
| v_9be107 | response.field_extra_closed | createAlert:401 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_9bf50e | response.type_changed | cancelAlert:500 | Problem.status | open | — | 0 | 002 | — |
| v_9c7d67 | response.header_missing | createAlert:200 | headers.Idempotency-Replayed | open | — | 0 | 002 | — |
| v_9d9bd3 | response.required_weakened | createAlert:400 | Problem | open | — | 0 | 002 | — |
| v_9edeb6 | response.field_missing | listAlerts:500 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_a20ef6 | response.field_extra_closed | getAlert:404 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_a2a44b | response.required_weakened | getCourtAvailability:504 | Problem | open | — | 0 | 002 | — |
| v_a3875a | response.field_extra_closed | getCourtAvailability:400 | Problem.courtId | open | — | 0 | 002 | — |
| v_a3b877 | response.field_missing | cancelAlert:503 | Problem.type | open | — | 0 | 002 | — |
| v_a41a49 | response.field_missing | createAlert:422 | Problem.type | open | — | 0 | 002 | — |
| v_a46866 | response.field_extra_closed | getCourtAvailability:503 | Problem.staleReason | open | — | 0 | 002 | — |
| v_a4740a | response.field_extra_closed | getAlert:400 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_a49794 | response.field_missing | getAlert:503 | Problem.title | open | — | 0 | 002 | — |
| v_a4c290 | response.field_extra_closed | getCourtAvailability:502 | Problem.stale | open | — | 0 | 002 | — |
| v_a6b9cc | response.field_missing | getCourtAvailability:502 | Problem.status | open | — | 0 | 002 | — |
| v_a889ef | response.field_extra_closed | getAlert:503 | Problem.alertId | open | — | 0 | 002 | — |
| v_a91197 | response.field_extra_closed | getAlert:404 | Problem.courtName | open | — | 0 | 002 | — |
| v_aab5e7 | response.field_extra_closed | createAlert:500 | Problem.slot | open | — | 0 | 002 | — |
| v_ab1b64 | response.field_missing | getCourtAvailability:400 | Problem.retryable | open | — | 0 | 002 | — |
| v_abac5a | response.type_changed | getAlert:400 | Problem.status | open | — | 0 | 002 | — |
| v_abb694 | response.field_extra_closed | createAlert:400 | Problem.date | open | — | 0 | 002 | — |
| v_ac43d1 | response.header_missing | getCourtAvailability:502 | headers.Retry-After | open | — | 0 | 002 | — |
| v_acce6b | response.field_missing | createAlert:401 | Problem.title | open | — | 0 | 002 | — |
| v_ae5150 | response.required_weakened | cancelAlert:500 | Problem | open | — | 0 | 002 | — |
| v_ae8334 | response.type_changed | createAlert:409 | Problem.status | open | — | 0 | 002 | — |
| v_afe25b | response.field_missing | getAlert:500 | Problem.detail | open | — | 0 | 002 | — |
| v_b0f904 | response.type_changed | getAlert:401 | Problem.status | open | — | 0 | 002 | — |
| v_b203d8 | response.field_missing | getAlert:401 | Problem.instance | open | — | 0 | 002 | — |
| v_b2f227 | response.field_missing | getCourtAvailability:503 | Problem.detail | open | — | 0 | 002 | — |
| v_b2fc61 | response.field_extra_closed | cancelAlert:500 | Problem.createdAt | open | — | 0 | 002 | — |
| v_b5977b | response.field_missing | getAlert:404 | Problem.traceId | open | — | 0 | 002 | — |
| v_b5d9de | response.field_missing | getAlert:503 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_b5db88 | response.required_weakened | getAlert:500 | Problem | open | — | 0 | 002 | — |
| v_b6ec3a | response.field_extra_closed | cancelAlert:401 | Problem.createdAt | open | — | 0 | 002 | — |
| v_b832dd | response.field_missing | listAlerts:400 | Problem.instance | open | — | 0 | 002 | — |
| v_b8926c | response.field_extra_closed | getAlert:503 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_b8c346 | response.field_extra_closed | getAlert:400 | Problem.date | open | — | 0 | 002 | — |
| v_b8fb4d | response.field_extra_closed | cancelAlert:503 | Problem.courtId | open | — | 0 | 002 | — |
| v_b914d4 | response.field_missing | getCourtAvailability:502 | Problem.title | open | — | 0 | 002 | — |
| v_b9d945 | response.field_extra_closed | createAlert:401 | Problem.date | open | — | 0 | 002 | — |
| v_b9dad7 | response.field_extra_closed | createAlert:422 | Problem.courtName | open | — | 0 | 002 | — |
| v_bbde5e | response.header_missing | cancelAlert:500 | headers.Retry-After | open | — | 0 | 002 | — |
| v_bd1a4d | response.header_missing | createAlert:500 | headers.Retry-After | open | — | 0 | 002 | — |
| v_bd2dbe | response.required_weakened | createAlert:409 | Problem | open | — | 0 | 002 | — |
| v_bd8688 | response.field_missing | createAlert:503 | Problem.retryable | open | — | 0 | 002 | — |
| v_be0f88 | response.field_missing | getCourtAvailability:503 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_be5e14 | response.field_extra_closed | getAlert:400 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_be77de | response.required_weakened | createAlert:401 | Problem | open | — | 0 | 002 | — |
| v_bf1a9b | response.required_weakened | listAlerts:200 | AlertList | open | — | 0 | 002 | — |
| v_bf2014 | response.field_extra_closed | getCourtAvailability:502 | Problem.slots | open | — | 0 | 002 | — |
| v_bf4e55 | response.field_missing | listAlerts:503 | Problem.title | open | — | 0 | 002 | — |
| v_bf888c | response.field_missing | createAlert:401 | Problem.detail | open | — | 0 | 002 | — |
| v_bfc8b4 | response.required_weakened | getAlert:200 | Alert | open | — | 0 | 002 | — |
| v_bfe60e | response.field_missing | listAlerts:500 | Problem.retryable | open | — | 0 | 002 | — |
| v_c10af6 | response.field_missing | getAlert:401 | Problem.detail | open | — | 0 | 002 | — |
| v_c13cb6 | response.field_missing | getCourtAvailability:404 | Problem.status | open | — | 0 | 002 | — |
| v_c16bc7 | response.field_extra_closed | getCourtAvailability:502 | Problem.confirmedAt | open | — | 0 | 002 | — |
| v_c1b955 | response.field_missing | listAlerts:400 | Problem.title | open | — | 0 | 002 | — |
| v_c1f2b1 | response.field_missing | cancelAlert:404 | Problem.type | open | — | 0 | 002 | — |
| v_c1ff42 | response.field_missing | listAlerts:503 | Problem.code | open | — | 0 | 002 | — |
| v_c35390 | response.field_extra_closed | getAlert:500 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_c3a1cf | response.field_extra_closed | getCourtAvailability:504 | Problem.slots | open | — | 0 | 002 | — |
| v_c3e78c | response.field_missing | listAlerts:401 | Problem.status | open | — | 0 | 002 | — |
| v_c419dc | response.field_missing | listAlerts:500 | Problem.type | open | — | 0 | 002 | — |
| v_c4b294 | response.field_missing | listAlerts:400 | Problem.detail | open | — | 0 | 002 | — |
| v_c69715 | response.field_extra_closed | cancelAlert:503 | Problem.slot | open | — | 0 | 002 | — |
| v_c69bd8 | response.field_missing | getCourtAvailability:404 | Problem.detail | open | — | 0 | 002 | — |
| v_c75445 | response.field_extra_closed | cancelAlert:400 | Problem.alertId | open | — | 0 | 002 | — |
| v_c76518 | response.field_missing | cancelAlert:503 | Problem.instance | open | — | 0 | 002 | — |
| v_c83077 | response.field_extra_closed | createAlert:409 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_c8ac79 | response.field_missing | getAlert:400 | Problem.instance | open | — | 0 | 002 | — |
| v_c8b94d | response.field_extra_closed | cancelAlert:401 | Problem.alertId | open | — | 0 | 002 | — |
| v_c8e9f9 | response.field_missing | createAlert:409 | Problem.instance | open | — | 0 | 002 | — |
| v_c924dd | response.field_extra_closed | createAlert:503 | Problem.createdAt | open | — | 0 | 002 | — |
| v_ca5017 | response.field_missing | createAlert:503 | Problem.traceId | open | — | 0 | 002 | — |
| v_cba2c5 | response.field_missing | cancelAlert:401 | Problem.type | open | — | 0 | 002 | — |
| v_cc2d8b | response.field_missing | getCourtAvailability:400 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_ccccc8 | response.field_missing | createAlert:500 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_cd1de3 | response.required_weakened | createAlert:201 | Alert.slot | open | — | 0 | 002 | — |
| v_cd8cf9 | response.field_extra_closed | getAlert:401 | Problem.courtId | open | — | 0 | 002 | — |
| v_cda998 | response.field_extra_closed | cancelAlert:400 | Problem.courtName | open | — | 0 | 002 | — |
| v_cf5ae1 | response.required_weakened | getCourtAvailability:200 | CourtAvailability | open | — | 0 | 002 | — |
| v_cfb17d | response.field_missing | getCourtAvailability:504 | Problem.status | open | — | 0 | 002 | — |
| v_cfd9af | response.field_extra_closed | getCourtAvailability:502 | Problem.staleReason | open | — | 0 | 002 | — |
| v_d0ba00 | response.field_missing | createAlert:500 | Problem.title | open | — | 0 | 002 | — |
| v_d135f5 | response.required_weakened | getAlert:503 | Problem | open | — | 0 | 002 | — |
| v_d17bcb | response.field_extra_closed | createAlert:409 | Problem.delivery | open | — | 0 | 002 | — |
| v_d22d80 | response.field_extra_closed | listAlerts:400 | Problem.items | open | — | 0 | 002 | — |
| v_d25def | response.field_missing | getAlert:400 | Problem.title | open | — | 0 | 002 | — |
| v_d2ea08 | response.field_missing | getAlert:404 | Problem.title | open | — | 0 | 002 | — |
| v_d44d72 | response.required_weakened | createAlert:200 | Alert.delivery | open | — | 0 | 002 | — |
| v_d516ba | response.field_extra_closed | createAlert:409 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_d6429a | response.required_weakened | listAlerts:500 | Problem | open | — | 0 | 002 | — |
| v_d6c229 | response.field_missing | getAlert:404 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_d72511 | response.field_extra_closed | createAlert:401 | Problem.courtName | open | — | 0 | 002 | — |
| v_d91ca9 | response.type_changed | createAlert:503 | Problem.status | open | — | 0 | 002 | — |
| v_da9e26 | response.field_extra_closed | getCourtAvailability:503 | Problem.slots | open | — | 0 | 002 | — |
| v_dae6c2 | response.field_missing | cancelAlert:404 | Problem.instance | open | — | 0 | 002 | — |
| v_db38d9 | response.field_missing | getCourtAvailability:502 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_db41af | response.required_weakened | cancelAlert:400 | Problem | open | — | 0 | 002 | — |
| v_db5a2f | response.field_extra_closed | createAlert:409 | Problem.slot | open | — | 0 | 002 | — |
| v_dbd865 | response.field_missing | createAlert:409 | Problem.code | open | — | 0 | 002 | — |
| v_dbe042 | response.field_extra_closed | getAlert:500 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_dbf32d | response.field_extra_closed | getAlert:401 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_dcbe02 | response.field_missing | createAlert:422 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_dd7033 | response.field_missing | createAlert:422 | Problem.traceId | open | — | 0 | 002 | — |
| v_df0e07 | response.field_missing | listAlerts:400 | Problem.status | open | — | 0 | 002 | — |
| v_dfc929 | response.field_missing | createAlert:409 | Problem.detail | open | — | 0 | 002 | — |
| v_e04b11 | response.field_extra_closed | getAlert:500 | Problem.alertId | open | — | 0 | 002 | — |
| v_e1ce13 | response.required_weakened | getCourtAvailability:503 | Problem | open | — | 0 | 002 | — |
| v_e23db0 | response.field_extra_closed | getCourtAvailability:400 | Problem.confirmedAt | open | — | 0 | 002 | — |
| v_e374a3 | response.required_weakened | getCourtAvailability:200 | CourtAvailability.slots[] | open | — | 0 | 002 | — |
| v_e3adb3 | response.field_extra_closed | createAlert:422 | Problem.checkDelayed | open | — | 0 | 002 | — |
| v_e3d7b8 | response.field_missing | listAlerts:401 | Problem.code | open | — | 0 | 002 | — |
| v_e46bab | response.field_missing | listAlerts:500 | Problem.title | open | — | 0 | 002 | — |
| v_e50d53 | response.field_missing | createAlert:401 | Problem.traceId | open | — | 0 | 002 | — |
| v_e62d3b | response.field_missing | cancelAlert:401 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_e67c09 | response.field_extra_closed | getCourtAvailability:503 | Problem.courtId | open | — | 0 | 002 | — |
| v_e68407 | response.field_extra_closed | getAlert:400 | Problem.courtId | open | — | 0 | 002 | — |
| v_e6fa80 | response.field_missing | getCourtAvailability:503 | Problem.type | open | — | 0 | 002 | — |
| v_e884f3 | response.field_extra_closed | createAlert:409 | Problem.courtName | open | — | 0 | 002 | — |
| v_e8c306 | response.field_extra_closed | cancelAlert:401 | Problem.slot | open | — | 0 | 002 | — |
| v_e8feb9 | response.field_extra_closed | getCourtAvailability:503 | Problem.confirmedAt | open | — | 0 | 002 | — |
| v_e919dc | response.field_extra_closed | createAlert:422 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_e942c0 | response.required_weakened | getAlert:400 | Problem | open | — | 0 | 002 | — |
| v_e95292 | response.field_extra_closed | createAlert:400 | Problem.delivery | open | — | 0 | 002 | — |
| v_e96513 | response.field_missing | createAlert:400 | Problem.instance | open | — | 0 | 002 | — |
| v_e9ad33 | response.field_missing | getAlert:401 | Problem.retryable | open | — | 0 | 002 | — |
| v_e9dd4e | response.field_missing | getAlert:503 | Problem.type | open | — | 0 | 002 | — |
| v_ea6d85 | response.field_extra_closed | getCourtAvailability:400 | Problem.date | open | — | 0 | 002 | — |
| v_eaff2b | response.field_missing | cancelAlert:400 | Problem.code | open | — | 0 | 002 | — |
| v_eb8aba | response.field_extra_closed | createAlert:422 | Problem.lastCheckedAt | open | — | 0 | 002 | — |
| v_ec7d3b | response.field_missing | getAlert:400 | Problem.detail | open | — | 0 | 002 | — |
| v_ec8749 | response.field_missing | createAlert:400 | Problem.retryable | open | — | 0 | 002 | — |
| v_ed1c24 | response.field_missing | listAlerts:503 | Problem.retryable | open | — | 0 | 002 | — |
| v_ed47bd | response.field_extra_closed | cancelAlert:500 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_ed5fe7 | response.field_extra_closed | getCourtAvailability:502 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_edcb3c | response.field_missing | getCourtAvailability:500 | Problem.status | open | — | 0 | 002 | — |
| v_eeecda | response.field_missing | getCourtAvailability:502 | Problem.type | open | — | 0 | 002 | — |
| v_ef0a2f | response.field_missing | getAlert:400 | Problem.traceId | open | — | 0 | 002 | — |
| v_ef3088 | response.field_missing | createAlert:401 | Problem.type | open | — | 0 | 002 | — |
| v_f0376a | response.field_extra_closed | getAlert:401 | Problem.date | open | — | 0 | 002 | — |
| v_f065fe | response.field_missing | getCourtAvailability:503 | Problem.instance | open | — | 0 | 002 | — |
| v_f16c64 | response.field_missing | listAlerts:401 | Problem.traceId | open | — | 0 | 002 | — |
| v_f2e523 | response.field_extra_closed | getAlert:400 | Problem.slot | open | — | 0 | 002 | — |
| v_f3d7f0 | response.field_extra_closed | createAlert:409 | Problem.courtId | open | — | 0 | 002 | — |
| v_f412a5 | response.field_extra_closed | getAlert:503 | Problem.createdAt | open | — | 0 | 002 | — |
| v_f41c23 | response.field_missing | cancelAlert:404 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_f449ee | response.type_changed | cancelAlert:400 | Problem.status | open | — | 0 | 002 | — |
| v_f4658e | response.field_extra_closed | getAlert:404 | Problem.createdAt | open | — | 0 | 002 | — |
| v_f560b4 | response.field_missing | getAlert:404 | Problem.detail | open | — | 0 | 002 | — |
| v_f5cc0f | response.required_weakened | listAlerts:503 | Problem | open | — | 0 | 002 | — |
| v_f618dc | response.field_extra_closed | getCourtAvailability:504 | Problem.confirmedAt | open | — | 0 | 002 | — |
| v_f65975 | response.field_extra_closed | getCourtAvailability:503 | Problem.date | open | — | 0 | 002 | — |
| v_f677a5 | response.header_missing | getCourtAvailability:503 | headers.Retry-After | open | — | 0 | 002 | — |
| v_f68663 | response.field_missing | listAlerts:500 | Problem.detail | open | — | 0 | 002 | — |
| v_f6cd09 | response.type_changed | cancelAlert:503 | Problem.status | open | — | 0 | 002 | — |
| v_f78122 | response.field_extra_closed | createAlert:500 | Problem.createdAt | open | — | 0 | 002 | — |
| v_f7eed3 | response.field_extra_closed | createAlert:500 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_f80b3f | response.field_missing | createAlert:401 | Problem.retryAfterSeconds | open | — | 0 | 002 | — |
| v_f87674 | response.field_missing | getCourtAvailability:400 | Problem.status | open | — | 0 | 002 | — |
| v_f8cf3e | response.field_missing | listAlerts:400 | Problem.traceId | open | — | 0 | 002 | — |
| v_f90dfa | response.type_changed | getAlert:500 | Problem.status | open | — | 0 | 002 | — |
| v_f964fe | response.field_extra_closed | cancelAlert:500 | Problem.alertId | open | — | 0 | 002 | — |
| v_f99679 | response.field_missing | cancelAlert:401 | Problem.title | open | — | 0 | 002 | — |
| v_f9d7a0 | response.required_weakened | createAlert:201 | Alert.delivery | open | — | 0 | 002 | — |
| v_fa7efb | response.field_missing | getCourtAvailability:504 | Problem.retryable | open | — | 0 | 002 | — |
| v_fa8e31 | response.field_extra_closed | getCourtAvailability:500 | Problem.courtName | open | — | 0 | 002 | — |
| v_fad2a0 | response.field_extra_closed | cancelAlert:401 | Problem.reservationUrl | open | — | 0 | 002 | — |
| v_faff5b | response.header_missing | createAlert:201 | headers.Idempotency-Replayed | open | — | 0 | 002 | — |
| v_faffde | response.field_missing | listAlerts:401 | Problem.type | open | — | 0 | 002 | — |
| v_fcb255 | response.field_missing | cancelAlert:400 | Problem.traceId | open | — | 0 | 002 | — |
| v_fd03a6 | response.field_extra_closed | getAlert:401 | Problem.expiresAt | open | — | 0 | 002 | — |
| v_fd0458 | response.field_extra_closed | getAlert:500 | Problem.courtId | open | — | 0 | 002 | — |
| v_fe4ea7 | response.field_missing | getCourtAvailability:502 | Problem.traceId | open | — | 0 | 002 | — |
| v_feed3a | response.field_missing | cancelAlert:500 | Problem.detail | open | — | 0 | 002 | — |
| v_ff92d3 | response.field_missing | getCourtAvailability:500 | Problem.retryable | open | — | 0 | 002 | — |
| v_fff33c | response.field_extra_closed | getAlert:500 | Problem.createdAt | open | — | 0 | 002 | — |
| v_579dfe | g0.compile_failed | — | mvnw test | closed | code | 0 | 001 | 002 |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_003a20 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_00483b | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_009e8e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_011662 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_015128 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_019094 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_01c7ac | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_01e6b5 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_01ef74 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_02a4d4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_02b4f3 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_03e216 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_03f655 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_047bda | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_04db94 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_04f063 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_052c20 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_0543e3 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_056a69 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_056b7b | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_05b75f | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_05cf83 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_06155f | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_062e09 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_0706d4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_07195d | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0768d0 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_079a98 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_087ac0 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_08b937 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_08bfc8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_097622 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_0a1647 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0a7f0c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0a9075 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0ad8e5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_0adfdc | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0d0400 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_0d41d1 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0d9bf7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_0e31ec | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0e35c3 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_0e3a73 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_0fa49f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_0facb8 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_0ff2fa | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_10527f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_10b33d | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_10e41a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_10ed55 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_11a5ca | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_11b075 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_11e4b2 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_130bc3 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_13cda4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_13f790 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_14c66f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_15798d | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_15abcd | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_15f2c7 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_169ccb | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_16b92c | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_16be7f | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1727fc | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_174e11 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_174f79 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1757b8 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_177bac | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_183564 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_19a4c1 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_19cd9d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1a07e2 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1a6470 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1aa0fa | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1b59f6 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1bfe9f | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_1c378f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1d0d58 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1d5c1d | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1d7641 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1dc859 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_1e4b9b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1f4592 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1f5ce7 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1fa9ba | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_1fe78b | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_20f37a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2252d8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_233e70 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_2390c4 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_23d281 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_24d5a8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_24e600 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_24febe | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_25e91c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_26f13c | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_276d86 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_280187 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_29a8b7 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_2a1543 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_2a1961 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_2a3dcf | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2a69da | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2b343d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2b4239 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2b5103 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2b89c9 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2ceab5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2cecbf | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_2d04f7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2e1f8c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_2e49de | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2eccf8 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_2f1e0b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_2fd24a | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_3015d2 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_301786 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_310e16 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_31520d | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_336018 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_33bfb5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_344122 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_34d799 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_350b45 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_354ec4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_35ab97 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_35bb01 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_366701 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_36849e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_36a01a | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_36eeb7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_375234 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_37f79f | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3898c7 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_39b311 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_39cb98 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3a4002 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_3b1734 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3b2c4c | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_3b4526 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3b7c9a | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_3c35e2 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_3ce2b1 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3dd386 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3e0083 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3e0888 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_3e9888 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3f27eb | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_3f44cf | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_3f495f | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_403d15 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_405cfa | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_407d85 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_40839c | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_40ecd1 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_41aa45 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_422d3a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_425e5a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_42778f | 추출한 스펙에 계약의 그 조항에 대응하는 것이 아예 없다. 미판정은 통과가 아니므로 위반으로 센다. | — | — |
| v_42f79e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_438a78 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_43a107 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_44094b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4464ed | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_44cac3 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_451ab1 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_45345e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_454a1b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4572ca | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4672e8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_46da31 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4709b9 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4712ef | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_483212 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_483d64 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_486eea | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_489bd2 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4955fc | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_49d5c5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4a95f6 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4adca3 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4ae57b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4afe52 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4b03df | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4b5b36 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4b5e7c | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4b7e01 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4c86f3 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_4cbdfb | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4cd049 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4d65fd | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4e7326 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4ea7c4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4ee57a | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_4f2e7b | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_4fcb76 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_504b7e | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_5072d5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_51ade6 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_51bd29 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_51da6c | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_51fcbf | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_520cda | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5286a9 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_536441 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5375a0 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_5469f0 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_5538b6 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_563a4d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_563a6d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_56e2bd | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_576aff | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_57d9d9 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_588478 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_58883e | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_588c7e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_59297e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_595daf | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5a0cc1 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_5ad1dc | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_5b4184 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5b69da | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_5b9ae2 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5bdad4 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_5bf1da | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5c3001 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5cd8d1 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5d02a7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5e146c | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5e552e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_5fb627 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_607258 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6078e6 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_608862 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_60daa4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_61faee | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_636b60 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_637516 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_63871f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_645b75 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_650790 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_661103 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_662884 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_66c368 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_67574f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_686f30 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_688710 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_69051d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_693345 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6986a6 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6a69fb | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_6a7e90 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6b46d0 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6ba180 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6bcac7 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_6be16a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6cca4d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6cf634 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_6dd422 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_6ea2a4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6ec551 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_6ff840 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_708e9c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_72158f | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_73bcfb | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_74b77e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_751e2f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_755950 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_75af80 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_75e3c4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7616d9 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_767b15 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7854e4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7968d4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_79a473 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_7a50e7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7a7359 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7acd13 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_7b4c38 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_7beca7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7c0ca1 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7c46ae | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_7c55e4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_7de05d | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_7de922 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_7f1756 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_7f580b | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_7faac2 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_809aea | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_811d62 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_818faf | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_81d9dc | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_82a28e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_82f5d9 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_82f933 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_835f42 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_840977 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_844e19 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_845b0f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_84c85e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_86ce8e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_882899 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_885afd | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_894d41 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_89689f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_8affe5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_8b1b67 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_8ba7f5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_8cc4b2 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_8d57da | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_8d8338 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_8dac61 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_8e5701 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_8e7ede | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_8ec7bf | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_8f12a2 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_8fee5b | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_90996b | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_919892 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_924322 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9295b3 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_931deb | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_932149 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_936138 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_936cba | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_93cb43 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_93dba3 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_93f5ef | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_93fa8b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_94c498 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9571bc | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_9599d4 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9685e7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_96b68e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_970375 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_97497e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9800b4 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_98b426 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_98b6a8 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_98dba0 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9916c1 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_994366 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_999c87 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9a66dd | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9a8c54 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9be107 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_9bf50e | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_9c7d67 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_9d9bd3 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_9edeb6 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_a20ef6 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_a2a44b | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_a3875a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_a3b877 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_a41a49 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_a46866 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_a4740a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_a49794 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_a4c290 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_a6b9cc | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_a889ef | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_a91197 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_aab5e7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_ab1b64 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_abac5a | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_abb694 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_ac43d1 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_acce6b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ae5150 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_ae8334 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_afe25b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b0f904 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_b203d8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b2f227 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b2fc61 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_b5977b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b5d9de | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b5db88 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_b6ec3a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_b832dd | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b8926c | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_b8c346 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_b8fb4d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_b914d4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_b9d945 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_b9dad7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_bbde5e | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_bd1a4d | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_bd2dbe | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_bd8688 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_be0f88 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_be5e14 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_be77de | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_bf1a9b | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_bf2014 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_bf4e55 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_bf888c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_bfc8b4 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_bfe60e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c10af6 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c13cb6 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c16bc7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c1b955 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c1f2b1 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c1ff42 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c35390 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c3a1cf | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c3e78c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c419dc | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c4b294 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c69715 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c69bd8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c75445 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c76518 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c83077 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c8ac79 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c8b94d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_c8e9f9 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_c924dd | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_ca5017 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_cba2c5 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_cc2d8b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ccccc8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_cd1de3 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_cd8cf9 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_cda998 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_cf5ae1 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_cfb17d | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_cfd9af | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_d0ba00 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_d135f5 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_d17bcb | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_d22d80 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_d25def | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_d2ea08 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_d44d72 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_d516ba | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_d6429a | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_d6c229 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_d72511 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_d91ca9 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_da9e26 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_dae6c2 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_db38d9 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_db41af | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_db5a2f | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_dbd865 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_dbe042 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_dbf32d | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_dcbe02 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_dd7033 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_df0e07 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_dfc929 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e04b11 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e1ce13 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_e23db0 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e374a3 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_e3adb3 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e3d7b8 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e46bab | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e50d53 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e62d3b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e67c09 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e68407 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e6fa80 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e884f3 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e8c306 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e8feb9 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e919dc | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e942c0 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_e95292 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_e96513 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e9ad33 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_e9dd4e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ea6d85 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_eaff2b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_eb8aba | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_ec7d3b | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ec8749 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ed1c24 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ed47bd | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_ed5fe7 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_edcb3c | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_eeecda | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ef0a2f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ef3088 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f0376a | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f065fe | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f16c64 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f2e523 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f3d7f0 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f412a5 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f41c23 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f449ee | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_f4658e | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f560b4 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f5cc0f | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_f618dc | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f65975 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f677a5 | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_f68663 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f6cd09 | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_f78122 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f7eed3 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f80b3f | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f87674 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f8cf3e | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f90dfa | 구현이 응답에 담는 타입이 계약이 약속한 타입과 다르다. 좁히는 것과 달리 읽는 쪽이 깨진다. | — | — |
| v_f964fe | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_f99679 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_f9d7a0 | 계약이 항상 담겠다고 보장한 필드를 구현이 선택으로 낮췄다. 읽는 쪽이 없는 경우를 다뤄야 한다. | — | — |
| v_fa7efb | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_fa8e31 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_fad2a0 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_faff5b | 계약이 응답에 싣겠다고 선언한 헤더가 구현에 없다. | — | — |
| v_faffde | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_fcb255 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_fd03a6 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_fd0458 | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_fe4ea7 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_feed3a | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_ff92d3 | 계약이 응답에 담겠다고 선언한 필드가 구현의 응답 타입에 없다. | — | — |
| v_fff33c | 계약이 닫아 둔 응답 객체에 계약이 선언하지 않은 필드가 있다. 계약이 그 객체를 열면 기록으로 내려간다. | — | — |
| v_579dfe | 컴파일 오류 2건 src/main/java/com/thinking/tennis/app/ApplicationException.java:5:71 interface expected here src/main/java/com/thinking/tennis/api/AvailabilityController.java:60:16 incompatible types: com.thinking.tennis.app.ApplicationException cannot be converted to com.thinking.tennis.app.ApiException | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/ApplicationException.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 69건이다.

### 충족 주장과 게이트 판정 (63건)

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
| 001 | getCourtAvailability:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
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
| 001 | listAlerts:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
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

### 게이트가 세는 것을 다시 말한 지적 (6건)

막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다.

| iteration | axis | critique_id | rule | 관찰 |
| --- | --- | --- | --- | --- |
| 002 | failure_faithfulness | w_error_code_lost | response.field_missing | 게이트가 response.field_missing로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | state_continuity | w_replay_not_snapshot | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | state_continuity | w_refresh_cache_bypass | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | response_fidelity | w_expires_at_ignores_cutoff | response.field_extra_closed | 게이트가 response.field_extra_closed로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | failure_faithfulness | w_alert_not_found_exception_path | response.required_weakened | 게이트가 response.required_weakened로 이미 세는 자리를 비평이 다시 말했다 |
| 002 | request_tolerance | w_blank_status_means_omitted | response.field_missing | 게이트가 response.field_missing로 이미 세는 자리를 비평이 다시 말했다 |

### 게이트가 남긴 기록 (4건)

계약이 말하지 않은 자리의 초과다. REJECT 사유가 아니고 뜻은 Critique가 붙인다.

| 규칙 | 판정 지점 | 좌표 | iteration | 관찰 |
| --- | --- | --- | --- | --- |
| gate:request.field_extra | listAlerts | parameters.header.Authorization | 002 | 계약에 없는 파라미터다 |
| gate:request.field_extra | createAlert | parameters.header.Authorization | 002 | 계약에 없는 파라미터다 |
| gate:request.field_extra | getAlert | parameters.header.Authorization | 002 | 계약에 없는 파라미터다 |
| gate:request.field_extra | cancelAlert | parameters.header.Authorization | 002 | 계약에 없는 파라미터다 |
