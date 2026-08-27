# Original User Input

이름 하나를 바꿨을 뿐인데 파일, 클래스, 테스트 파일, 테스트 안의 호출이 전부 따라 바뀌었다. 그런데 확인하는 값은 하나도 안 바뀌었고 6개 테스트가 그대로 통과했다.

이 얘시가 좀 이상한거 아니야? 내가 주장하는 내용이랑


# Checked Context

# TDD 로그 — task1-3

> 사이클마다 Red→Green→Refactor를 한 줄씩 남긴다. 커밋 해시와 함께 적으면 커밋 히스토리와 대조된다.
> 기록 규율은 `task1/task1-3-history/CLAUDE.md`의 '세 단계를 기록하는 법' 절 참고.

## 대상 A: PRORATION 일할 계산 (순수 로직, Mock 없음)

| 사이클 | Red (기대·실패 이유) | Green (최소) | Refactor (무엇을 줄였나) | 커밋 |
|--------|---------------------|-------------|------------------------|------|
| 1 | 30000/30일/15일 → 15000 / **컴파일 실패 확인**(`cannot find symbol: Proration`) | `return 15000;` — 예제 1개뿐이라 삼각측량 규율상 하드코딩이 최소. 일반식은 아직 근거 없음(다음 예제가 깨뜨릴 때 창발) | — | `[Red]` 6ea678d · `[Green]` ca4a5da |
| 2 | 30000/30일/**20일** → 10000 / **단언 실패 확인**(`expected: 10000 but was: 15000` — 하드코딩 깨짐) | `remaining=total-elapsed; dailyRate=price/total(버림); dailyRate*remaining` — 두 점이 강제한 최소 일반식 | **테스트 중복 제거**: 구조 같고 데이터만 다른 test1·test2를 `@ParameterizedTest`+`@CsvSource` 한 메서드로 통합. **데이터 점 2개는 유지**(줄이면 하드코딩 뮤턴트 부활). 프로덕션 코드는 dailyRate 지역변수로 이미 의도 노출 → 손 안 댐 | `[Red]` 915576c · `[Green]` 57a09a5 · `[Refactor]` 0e03aae |
| 3 | 30000/30일/**30일**(전액 소진) → 0 / **Red 없음 — 추가 즉시 Green**(`dailyRate×0=0`, 일반식이 remaining=0을 자연히 처리) | 코드 변경 없음 — 새 회귀 안전망만 추가(§3: "코드를 안 건드려도 통과하는 사이클") | — | `[Green]` 1a5ef56 |
| 4 | 30000/30일/**0일**(미사용) → 30000 / **Red 없음 — 추가 즉시 Green**(`dailyRate×30=30000`, 일반식이 처리) ⚠️ 무료환불 정책값과 우연히 같아 정책 분기를 못 가름(9번이 담당) | 코드 변경 없음 — 회귀 안전망만 추가 | — | `[Green]` 6f3595c |
| 5 | 10000/30일/**20일**(안 나눠떨어짐) → 3330 / **Red 없음 — 추가 즉시 Green**(Java 정수 나눗셈이 양수에서 이미 버림과 동일: `10000/30=333`) | 코드 변경 없음 — 단, 버림 규칙을 **처음으로 실제 실행**시켜 고정하는 핵심 회귀 테스트(결함주입 후보5: 반올림으로 바꾸면 잡혀야 함) | — | `[Green]` 03f5092 |
| 6 | 무료환불 정책: 30000/30일/**7일** → 30000 / **단언 실패 확인**(`expected: 30000 but was: 23000` — 일할계산 새는 중, 정책 분기 없음) | `if (elapsed<=FREE_REFUND_DAY_LIMIT) return price;` 를 일반식 앞단에 추가 — 백로그가 못박은 정책값(7)을 상수로 명명 | **설계 노트 실행**(test-list.md): 일할계산 본식을 private `prorate()`로 분리, `calculate()`는 정책+계산 조합 진입점만 담당. 동작 불변(6/6 그대로 통과) | `[Red]` 72cfd4d · `[Green]` e390520 · `[Refactor]` d347f27 |
| 7 | (Refactor 전용, 새 Red 없음) | — | **SRP 위반 발견 후 클래스 경계 분리**: 사이클6의 private `prorate()` 분리는 가독성 정리일 뿐 책임 분리가 아니었음(CLAUDE.md SRP 체크리스트 §4 신설 계기). `Proration`은 정책을 전혀 모르는 순수 계산만 담당하도록 되돌리고, 새 클래스 `RefundPolicy`가 "elapsed≤7 전액, 아니면 Proration에 위임"을 담당. 정책 테스트(`경과일이_칠일_이하면...`)도 `ProrationTest`→`RefundPolicyTest`로 이동(assertion 값 불변, seam만 이동). 동작 불변(6/6 그대로 통과) | `[Refactor]` 3c58955 |
| 8 | (Refactor 전용, 새 Red 없음) | — | **네이밍 정정**: `RefundPolicy`는 최종 환불 금액(int)을 반환하는 진입점인데 이름이 "정책 판정만 함"으로 들려 이름-책임 괴리(SRP 체크리스트의 "이름이 거짓말한다" 신호). `RefundCalculator`로 리네임(파일·클래스·테스트 파일·테스트 내부 호출 전부). assertion 값 불변, 동작 불변(6/6 그대로 통과) | `[Refactor]` 08f59cc |
| 9 | 일할계산 전환 경계: 30000/30일/**8**일 → 22000 / **Red 없음 — 추가 즉시 Green**(기존 `elapsed<=7` 분기+`Proration` 위임이 자연히 처리. 9번(elapsed=7→전액) straddle pair) | 코드 변경 없음 — 회귀 안전망만 추가(새 테스트 메서드 `경과일이_칠일_초과면_일할계산금액이_환불된다`) | **테스트 중복 제거(재검토 후 병합)**: 두 테스트 메서드의 assertion 본문(호출+단언 두 줄)이 완전히 동일 — 사이클2와 같은 "구조 같고 데이터만 다른" 패턴으로 판단해 `@CsvSource` 한 테이블(elapsed=7/8 두 행)로 통합. 처음엔 "정책 분기 양쪽을 대조 문서화"를 이유로 병합 보류했으나, 두 행을 한 테이블에 나란히 두는 편이 오히려 경계를 더 잘 드러내 병합이 낫다고 재판단(사용자 지적으로 재검토). assertion 값 불변, 동작 불변(7/7 그대로 통과) | `[Green]` e915d60 · `[Refactor]` f8675a5 |
| 10 | 음수 금액(예외): price=**-1**/30/**15**(>7) → `IllegalArgumentException` / **단언 실패 확인**(`Expecting code to raise a throwable` — 가드 부재로 Proration에 위임돼 예외 없이 계산됨, 컴파일 실패 아님). elapsed=15로 잡아 정책 지름길이 아니라 일할계산 위임 경로에서도 가드가 필요함을 강제. **예외 타입 관례=IllegalArgumentException, 가드 위치=RefundCalculator(정책 앞단) 확정** | `if (price<0) throw new IllegalArgumentException(...)` 를 정책 분기(`elapsed<=7`) **앞단**에 추가 — "검증이 정책보다 먼저"(도메인 규칙 line19)를 코드에 고정. 8/8 통과 | **없음(지울 중복 0)** — 단, SRP 자가점검: `RefundCalculator`에 **검증 축**이 새로 얹힘(기존 정책+조합에 더해). 가드 1개로 별도 검증 클래스/메서드 추출은 조기 분리라 판단해 **미룸**. 6·7번(총일수0/경과일 범위)이 가드를 더 얹어 검증 로직이 뭉치면 그때 `validateInputs()` 추출 또는 검증 seam 분리 재검토 | `[Red]` 7b7fbeb · `[Green]` dd6f5e1 |
| 11 | 경과일수 범위 위반(예외): price=30000/30/**40**(elapsed>total) 과 30000/30/**-1**(elapsed<0) → `IllegalArgumentException` / **단언 실패 2건 확인**(`Expecting code to raise a throwable`, 컴파일 아님). 현재 40은 Proration 위임돼 -10000 계산, -1은 정책 지름길(≤7)로 전액 30000 반환 — 둘 다 예외 없이 샘. **한쪽 조건만 막는 가드는 다른 행이 살아남으므로 2점이 복합 범위 가드를 강제**(사이클2·9 패턴) | `if (elapsed<0 \|\| elapsed>total) throw ...` 를 price 가드 다음·정책 앞단에 추가. 2행이 강제한 최소 복합 조건. 10/10 통과 | **검증 축 그룹핑(예고된 지점 실행)**: 사이클10에서 "가드 2개 쌓이면 재검토"로 미뤘던 걸 실행 — price·범위 가드 2개를 private `validateInputs()`로 묶어 `calculate()`가 검증→정책→계산 순서로 읽히게 함. **가독성 정리이지 SRP 클래스 분리 아님**(CLAUDE.md 규칙). 별도 `Validator` 클래스 승격은 검증 불변식이 정책과 독립적으로 변할 압력이 없어 재차 미룸(근거 주석·로그 기록). 동작 불변(10/10 그대로) | `[Red]` b3be8f8 · `[Green]` cd2aa2e · `[Refactor]` 2ce787e |
| 12 | 총일수 0(예외): price=30000/**0**/**0** → `IllegalArgumentException` / **단언 실패 확인**(`Expecting code to raise a throwable`, 컴파일 아님). **관찰: `ArithmeticException`(0나누기)조차 안 남** — elapsed=0은 total=0을 범위 가드 통과시키는 유일한 값인데(elapsed>0이면 범위 가드가 잡음), 그 elapsed=0은 정책 지름길(≤7)로 빠져 `price/0` 나눗셈에 도달조차 안 하고 조용히 price(30000)를 반환. → 명시적 입력 가드 없으면 샌다는 걸 드러냄 | `if (totalDays==0) throw ...` 를 `validateInputs()` 안, **범위 가드보다 앞단**에 추가(총일수 유효성이 가장 근본, `elapsed>totalDays` 비교는 total 유효할 때만 의미). 도메인 규칙 line29에 정확히 맞춘 최소. 11/11 통과 | **없음(지울 중복 0)** — 가드 3개가 이미 사이클11 `validateInputs()`에 모여 한 줄만 추가됨. SRP 재점검: 3개 모두 "입력 유효성" 한 축의 단순·안정 불변식 → 별도 `Validator` 클래스 승격 여전히 과함, 계속 미룸(근거 사이클11 동일) | `[Red]` 9b3c0b1 · `[Green]` c6534a5 |
| 13 | (Refactor 전용, 새 Red 없음 — 코드 리뷰 발견) | — | **깨진 Javadoc 링크 수정**: `Proration`의 `{@link RefundPolicy}`가 사이클8 리네임(RefundPolicy→RefundCalculator) 때 누락된 스테일 참조 → 존재하지 않는 클래스를 가리켜 `@link`가 깨짐. `{@link RefundCalculator}`로 정정. CLAUDE.md SRP 체크리스트의 "이름이 거짓말한다" 신호의 문서판. 문서 전용, 동작 불변(11/11 그대로) | `[Refactor]` f97adc4 |

## 대상 C: 결제 취소 — PG 호출 → 상태 전이 (수행내용 2번, PG만 Mock)

> 환불 처리 흐름 전체가 아니라, **PG 하나만 닿는 실행 슬라이스**로 좁힘(오케스트레이터 전체는 mock 범벅=냄새).
> 백로그·전략은 [test-list-refund-service.md](./test-list-refund-service.md). 이 표는 사이클별 실행 서사만 쌓는다.

| 사이클 | Red | Green | Refactor | 끼운 Mock·이유 | 커밋 |
|--------|-----|-------|----------|---------------|------|
| 1 | PG 성공 시 `Refund=SUCCEEDED`, `Order=REFUNDED`, `cancelPayment(payment-uuid-1, 30000)` 호출 / **컴파일 실패 확인**(`cannot find symbol`: `PgClient`, `Order`, `Refund`, `RefundService` 등 대상 C 타입 부재) | `PgClient` 포트와 `RefundService.cancel(order, refund)` 성공 경로만 추가. `Refund.proration(30,7)`이 기존 `RefundCalculator`로 30000을 계산하고, PG 성공이면 `Refund.succeed()` + `Order.applyRefund(...)`를 호출한다. 실패/타임아웃/부분환불은 다음 Red가 요구할 때 열기 위해 미구현 | **없음(지울 중복 0)** — SRP 점검: `RefundService`는 PG 호출 결과에 따른 조립, `RefundCalculator`는 금액 계산, `Order`/`Refund`는 상태 보유·전이로 축을 나눴다. 아직 성공 분기 1개뿐이라 응답 분기 구조 일반화는 보류 | `PgClient`만 Mock — PG는 비관리형 외부 시스템이고 성공/실패 응답을 단위테스트에서 결정적으로 재현해야 하므로 내 포트만 목 처리. `RefundCalculator`·`Order`·`Refund`는 진짜 객체 | `[Red]` cc04008 · `[Green]` 0b5ec99 |
| 2 | PG 명확한 거부 시 `Refund=FAILED`, `Order=PAID`, `cancelPayment(payment-uuid-1, 30000)` 호출 / **컴파일 문제 확인**(`PgCancelResult.rejected()`와 `RefundStatus.FAILED` 부재) | `PgCancelResult.REJECTED`/`rejected()`/`isRejected()`, `RefundStatus.FAILED`, `Refund.fail()`을 추가하고 `RefundService.cancel()`에 거부 분기만 추가. 거부 시 `order.applyRefund(...)`를 호출하지 않아 주문 상태를 유지한다 | **테스트 중복 제거**: 두 테스트에 반복되던 주문/환불 fixture와 PG Mock 이유 주석을 상수·helper·필드 주석으로 모았다. 프로덕션 분기는 성공/거부 2개뿐이라 `switch` 등 구조 일반화는 타임아웃 Red까지 보류 | `PgClient`만 Mock — PG 거부 응답은 비관리형 외부 시스템의 실패 분기라 단위테스트에서 포트 stub으로 결정적으로 재현한다. `Order`·`Refund`는 진짜 객체로 상태를 단언 | `[Red]` 6f0d1e1 · `[Green]` fc8bb1b · `[Refactor]` 78f6405 |
| 3 | PG 응답 불확실 시 `Refund=TIMED_OUT`, `Order=PAID`, `cancelPayment(payment-uuid-1, 30000)` 호출 / **컴파일 문제 확인**(`PgCancelResult.timedOut()`와 `RefundStatus.TIMED_OUT` 부재) | `PgCancelResult.TIMED_OUT`/`timedOut()`/`isTimedOut()`, `RefundStatus.TIMED_OUT`, `Refund.timeOut()`을 추가하고 `RefundService.cancel()`에 타임아웃 분기만 추가. 타임아웃 시 주문은 적용하지 않아 `PAID` 상태를 유지한다 | **3분기 구조 정리**: 성공/거부/타임아웃이 모두 테스트로 고정된 뒤 연속 `if`를 enum `switch`로 바꿔 닫힌 PG 응답 분기를 드러냈다. `switch` 전환 후 사용처가 사라진 `PgCancelResult.is*()` 판별 메서드는 제거했다 | `PgClient`만 Mock — 타임아웃/불확실 응답은 비관리형 외부 시스템의 재현 어려운 실패 분기라 포트 stub으로 결정적으로 재현한다. `Order`·`Refund`는 진짜 객체로 상태를 단언 | `[Red]` 21a9c6b · `[Green]` 9753297 · `[Refactor]` fb9631b |

---

## 고의 결함 주입 (수행내용 4번)

| 결함 | 어디를 어떻게 틀렸나 | 테스트 결과(Red?) | Red 아니면 보탠 단언 |
|------|--------------------|------------------|---------------------|
| 1 | `Proration.calculate`의 일단가 계산을 **버림→반올림**으로: `price / totalDays` → `(int) Math.round((double) price / totalDays)` (결함 후보5) | **처음엔 안 잡힘 (초록 11개 유지).** 기존 5개 데이터가 전부 딱 떨어지거나(30000/30=1000) 소수부 .33(10000/30=333.33→반올림해도 333)이라 버림/반올림 결과가 같아 회귀 안전망이 눈이 멀었다. | **보탠 단언**: `ProrationTest` CsvSource에 `20000, 30, 20, 6660` 추가 (소수부 .67 → floor 666 vs round 667). 보강 후 실행하니 `expected: 6660 but was: 6670`으로 **Red**. 결함 잡힌 것 확인 후 코드 원복(버림 복원), 보강 단언은 회귀 안전망으로 유지(테스트 5→6개, 전체 11→12 초록). |
