# refactor:v2 재평가

`testability_improvement`만 바뀐 refactor:v2를 기존 두 회차에 적용했다. 나머지 세 축은 기준과 코드가 모두 같아 v1 점수를 유지했다.

| 대상 | 조건 A | 조건 B | 테스트 용이성 | 가중 총점 | 판정 |
| --- | --- | --- | ---: | ---: | --- |
| 리팩터링 전 원본 | 외부 test double 3개 필요 | 회원 조회·결제·저장 흐름까지 함께 거침 | 1 | 해당 없음 | 비교 기준 |
| iter_001 | 외부 test double 0개 | 실패 원인이 재예약 금지 규칙 1개 | 5 | 4.2 | PASS |
| iter_002 | 외부 test double 0개 | 실패 원인이 재예약 금지 규칙 1개 | 5 | 4.2 | PASS |

원본은 `TicketService.reserveTicket` 안에 재예약 금지 판단이 있어 `TicketRepository`, `UserRepository`, `PaymentApi`를 함께 준비해야 한다. 리팩터링 뒤에는 `Ticket`만 만들어 재예약 금지 규칙을 직접 호출할 수 있다.

이 결과는 v1의 3점·4점과 우열 비교하지 않는다. v1은 설명에 포함된 변경 전후 Mock 수와 새 요구 격리를 봤고, v2는 외부 test double 0개와 규칙 하나라는 두 조건만 본다. 잣대가 달라졌으므로 v2 결과는 별도 파일에 남겼다.
