(Grit's Why): 이론은 코드로 증명해야 본인의 것이 됩니다. 작은 도메인 로직 하나를 골라 테스트를 직접 작성하며, A-2에서 세운 '가짜 객체를 언제 쓰는가' 기준을 손에 익히세요.

### 대상 도메인 (하나를 선택)

- 주문과 결제 도메인. 예를 들어 할인 정책 계산이나 재고 차감 검증.
- 본인 경력 도메인을 코드로 옮긴 주제 (권장). 현웅님은 회사에서 결제 도메인을 다루시니, 결제 검증 규칙이나 구독 상태 전이, 환불 정책 같은 순수 로직을 권장합니다. 실업무에서 쌓은 감각이 테스트 설계에 그대로 쓰입니다.
- 본인이 직접 정의한 도메인.

### 수행 내용

1. 순수 도메인 로직 하나를 TDD 사이클로 만들어 주세요. 실패하는 테스트 먼저(Red), 통과시키는 최소 코드(Green), 정리(Refactor) 순서를 지키고, 이 세 단계가 드러나도록 커밋 히스토리나 메모로 남겨 주세요. JUnit5와 AssertJ를 쓰고 경계값과 예외 케이스를 포함해 주세요.
2. 외부 의존(리포지토리나 외부 API)이 있는 로직 하나를 Mockito로 가짜 객체를 끼워 단위테스트해 주세요. 1-2에서 세운 기준을 실제로 적용해 주세요.
3. FIRST 원칙(Fast, Independent, Repeatable, Self-validating, Timely) 관점에서 본인 테스트를 자가 점검해 주세요.
4. 통과한 코드를 일부러 한 군데 틀리게 바꿔 보세요. (예: 경계 조건의 부등호를 뒤집거나, 더하기를 빼기로.) 그리고 테스트가 빨갛게(Red) 변하는지 확인하세요. 코드를 망가뜨렸는데도 테스트가 초록 그대로라면, 그 테스트는 통과만 보장할 뿐 결함은 잡아내지 못합니다. 어떤 단언을 보태야 그 변경을 잡아내는지 적고, 코드를 원래대로 되돌려 주세요.

### 제출물

- [x]  단위테스트 코드(순수 로직 하나 + Mockito 적용 하나)를 GitHub에. → 아래 "테스트 코드 위치" 참조.
- [x]  테스트 실행 결과가 전부 통과하는 화면 또는 로그. → 아래 "테스트 실행 로그" 참조.
- [x]  가짜 객체를 어디에 왜 끼웠는지 주석이나 메모. → 아래 "가짜 객체(Mock) 사용 메모" 참조.
- [x]  일부러 낸 결함 1개와 그때의 테스트 결과(빨개졌는지 여부). 안 잡혔다면 무엇을 보태 잡았는지. → 아래 "고의 결함 주입 메모" 참조.

---

### 테스트 코드 위치 (워크스페이스 상대 경로)

| 무엇 | 경로 |
|------|------|
| 순수 로직 — 대상 코드 | `task1/task1-3-history/src/main/java/com/thinking/payment/Proration.java`, `RefundCalculator.java` |
| 순수 로직 — 테스트 | `task1/task1-3-history/src/test/java/com/thinking/payment/ProrationTest.java`, `RefundCalculatorTest.java` |
| Mockito — 대상 코드 | `task1/task1-3-history/src/main/java/com/thinking/payment/RefundService.java` (+ 포트 `PgClient.java`) |
| Mockito — 테스트 | `task1/task1-3-history/src/test/java/com/thinking/payment/RefundServiceTest.java` |

---

### 테스트 실행 로그 (전부 통과)

`cd task1/task1-3-history && ./mvnw test` (JDK: corretto-17)

```text
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.thinking.payment.ProrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.208 s -- in com.thinking.payment.ProrationTest
[INFO] Running com.thinking.payment.RefundCalculatorTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.054 s -- in com.thinking.payment.RefundCalculatorTest
[INFO] Running com.thinking.payment.RefundServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.421 s -- in com.thinking.payment.RefundServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

### 고의 결함 주입 메모 (수행내용 4번)

**낸 결함**: `Proration.calculate`의 일단가 계산을 **버림 → 반올림**으로 바꿈.

```java
int dailyRate = price / totalDays;                          // 원래 (버림)
int dailyRate = (int) Math.round((double) price / totalDays); // 결함
```

**결과**: 처음엔 **안 잡힘**(11개 초록 그대로). 기존 데이터가 전부 딱 떨어지거나(`30000/30`) 소수부 .33이라(`10000/30→333`) 반올림해도 값이 같았다. → 소수부 .67인 `"20000, 30, 20, 6660"` 한 점을 보태니 **Red**(`expected: 6660 but was: 6670`).

확인 후 코드는 버림으로 원복, 보탠 단언은 유지(11→12개 초록). 상세는 `task1-3-history/tdd-log.md` 결함 표 #1.

---

### 어디에 끼웠나 (파일·위치)

| 무엇 | 파일 · 위치 |
|------|-----------|
| Mock 대상(포트) | `src/main/.../PgClient.java` — 우리가 정의한 인터페이스(포트) |
| Mock 선언 | `RefundServiceTest.java:22-23` (`@Mock private PgClient pg;`), 이유 주석 `:21` |

### 왜 `PgClient`에만 끼웠나

1. **PG(PortOne)는 비관리형 외부 시스템이다.** 실제 결제 취소라는 부작용이 밖으로 나가고, 느리고, 결과가 매번 같지 않다. 특히 **명확한 거부·타임아웃 같은 실패 분기는 실물 호출로 마음대로 재현할 수 없다.** 3분기(SUCCEEDED / REJECTED / TIMED_OUT)를 결정론적으로 검증하려면 Stub으로 응답을 주입하는 것이 유일한 방법이다.
2. **내가 소유한 타입(포트)을 목했다.** PortOne SDK·HTTP를 직접 목하지 않고, 우리 도메인 언어 인터페이스 `PgClient`를 목했다. 남의 API를 직접 목하면 실제 계약과 어긋난 목을 붙들게 되므로, 어댑터 뒤의 포트만 목한다.