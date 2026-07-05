# 결제 취소 (RefundService.cancel) — 테스트 목록

> **형제 문서**: 순수 계산 도메인은 [test-list-refund-calc.md](./test-list-refund-calc.md).
> 이 문서는 **PG 하나만 닿는 결제 취소 서비스**(seam 바깥, 여기서 처음 Mock 등장). 수행내용 **2번** 대상, tdd-log의 **대상 C**와 짝.
> 캐리 문서 — 케이스를 끝내면 `[x]`, 새 케이스는 맨 아래 추가. 규율은 [CLAUDE.md](./CLAUDE.md), 사이클별 실행 서사는 [tdd-log.md](./tdd-log.md) '대상 C' 표.

---

## 확정된 결정 (locked)

> **무엇을 어디까지 테스트할지**만 고정한다. 코드 형태·switch 여부·리팩터링·타입은 **TDD로 창발**시키므로 여기 안 적는다.

- **대상**: `RefundService.cancel(order, refund)` **하나** — PG를 호출하고 응답에 따라 도메인 객체 상태를 전이시키는 애플리케이션 서비스. 
- **seam & mock 규율 (1-2 기준 적용)**: `RefundService → PgClient(포트)`. **PgClient만 Mock.** `RefundCalculator`(대상 A)·`Order`·`Refund`는 **진짜 객체**(mock 금지).
  - 왜 PG만: PG = **비관리형 외부 시스템**(실패 분기 실물 재현 불가) → 목 정당. DB/repo = 관리형 → 인수/통합 몫. PortOne SDK 직접 목 금지, **내 포트를 목**(GOOS "only mock types you own").
- **검증 방식**: 결과는 **도메인 객체 상태로** 단언(고전파). PG로 나간 인자만 `verify(pg).cancelPayment(uuid, amount)`(런던파 — 계산 결과가 경계로 정확히 나갔는지). stub 호출엔 `verify` 안 붙인다.
- **PG 3분기 도메인 의미** (refund_design.md 2-2 — 이미 확정, 발견 대상 아님):
  | PG 응답 | Refund 상태 | Order 상태 |
  |---|---|---|
  | 성공 | `SUCCEEDED` | `REFUNDED`(적용) |
  | 명확한 거부 | `FAILED` | 변화 없음 |
  | 타임아웃·불확실 | `TIMED_OUT` | 변화 없음 |

---

## 테스트 목록

> **한 사이클에 하나만** Red로. `[x]` = Green 통과, 옆에 사이클 번호. tdd-log '대상 C'와 대조.

- [x] **1. 성공 → 환불 성공 처리** *(happy)* — PG 성공 시 `Refund` `SUCCEEDED`, `Order` `REFUNDED`, `verify(pg).cancelPayment(uuid, amount)`. *(사이클1 Green)*
- [x] **2. 명확한 거부 → 환불 실패 처리** *(경계)* — 거부 stub 시 `Refund` `FAILED`, `Order` 그대로(미적용). *(사이클2 Green)*
- [ ] **3. 타임아웃·불확실 → 보류 처리** *(경계)* — 불확실 stub 시 `Refund` `TIMED_OUT`, `Order` 그대로.

*(새 실패 분기가 떠오르면 4, 5 … 로 추가)*

---

## 전략 메모 (짧게)

- **순서**: 성공(1) → 거부(2) → 타임아웃(3). seam 안쪽(계산)은 대상 A에서 이미 고정.
- **로그**: 사이클당 한 번, Refactor 완료 시 기록(CLAUDE.md §7).

---

## 세션 재개 (환경)

- 대상 A와 동일 모듈. **JDK**: corretto-17. **실행**: `cd task1/task1-3-history && ./mvnw test`.
- **의존성**: Mockito 첫 사용 → `pom.xml`에 `mockito-junit-jupiter` 확인/추가(사이클1 시작 시).
- **현재 상태**: 대상 C 사이클2 완료. `RefundService`는 PG 성공/명확한 거부 분기를 처리한다. 다음 Red = 목록 3번(타임아웃·불확실).
