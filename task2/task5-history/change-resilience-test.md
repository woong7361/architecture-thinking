# 변화 저항력 시험 — task2 B-5 제출물 4

리팩토링(C1~C6)이 산 **변화 저항력**을 **축(axis)별로** 시험한다. 두 요구사항을 넣되,
**변경 전에 예측을 먼저 적어 잠그고**(이 커밋), 적용 후 실제 바뀐 파일과 대조한다.

- **R1 = 대조군**(가격 축 — 리팩토링이 **안 끊은** 축): 저항력이 안 생겼을 것으로 예상.
- **R2 = 실험군**(예약 규칙 축 — 리팩토링이 **끊은** 축): 저항력이 생겼을 것으로 예상.

핵심 가설: **저항력은 축마다 다르다. 우리가 실제로 끊은 축(예약 규칙)에서만 containment가 나타난다.**
"파일 몇 곳"은 무딘 지표라, **서비스가 열리는가 / 도메인 단위 테스트가 mock 없이 되는가**를 함께 본다.

---

## R1 — 금액 할인 (가격 축, 대조군)

**요구사항**: 예매 금액이 **50,000원 이상이면 10% 할인**해 청구한다.

**설계 판단(게이트)**: 할인 규칙이 **하나뿐(v=1)** → `DiscountPolicy` **인터페이스/전략은 안 만든다**(Type B, v<2 = Speculative Generality #15, DEFER). 대신 가격 정책에 **구체 클래스 한 채**만 준다(SRP, Type A). 인터페이스는 **2번째 할인 종류 확정 시(v=2)** 그때 뽑는다(트리거 기록). 할인은 티켓의 불변식이 아닌 가격정책이라 `Ticket`에 넣지 않는다.

**변경 전 예측 — 건드릴 곳**

| 구분 | 파일 | 변경 |
|---|---|---|
| production(신규) | `DiscountPolicy.java` | 구체 클래스 `int finalAmount(int basePrice)` (임계 50000/10%, 규칙 자기소유) |
| production(수정) | `TicketService.java` | `DiscountPolicy` 주입 + `charge(info, discountPolicy.finalAmount(ticket.getPrice()))` |
| 테스트(수정) | `TicketReservationSteps.java` | When 스텝의 서비스 생성 1줄에 `new DiscountPolicy()` |
| 테스트(신규) | `new_requirements.feature` | 50000→45000 시나리오(스텝 재사용) |

- **예측 production 수정 = 2곳**(신규 1 + 수정 1). `Ticket`·`ChargePort`·`*Repository`·`User`·예외 = **불변**.
- **대조 예측(핵심)**: 이 변경은 **리팩토링 전이었어도 같은 2곳**이다. 할인은 `charge(...)`의 **금액**을 건드리는데, 그 호출은 리팩토링 전·후 모두 서비스에 있었고(결제=I/O=조립자 몫), 우리 리팩토링은 그 라인을 건드리지 않았다(`ticket.getPrice()` 동일). → **저항력 개선 없음(당연)**.

---

## R2 — 판매 중지 티켓 예약 불가 (예약 규칙 축, 실험군)

**요구사항**: 티켓이 **판매 중지(suspended)** 상태면 예약할 수 없다(도메인 예외로 거부, 무청구).

**설계 판단**: "판매 중지면 예약 불가"는 **티켓의 예약 불변식**이다 → 정보 전문가 = `Ticket`. 리팩토링으로 예약 불변식을 `Ticket.ensureReservable()`에 모아뒀으니, 새 조건은 **그 메서드 한 곳**에 붙는다. `suspend()`는 (봉인한 setter가 아니라) 의미 있는 도메인 상태 전이다.

**변경 전 예측 — 건드릴 곳**

| 구분 | 파일 | 변경 |
|---|---|---|
| production(수정) | `Ticket.java` | `suspended` 필드 + `suspend()` + `ensureReservable()`에 `if(suspended) throw` 한 줄 |
| production(신규) | `TicketSuspendedException.java` | 새 도메인 예외 |
| 테스트(수정) | `TicketReservationSteps.java` | 새 Given(판매중지 seed) + 새 Then(예외 단언) |
| 테스트(신규) | `new_requirements.feature` | 판매중지 예약 거부 시나리오 |

- **예측 production 수정 = 2곳**(수정 1 + 신규 1), **`TicketService` = 0곳**. ← 핵심.
- **대조 예측(핵심)**: 리팩토링 **전**이라면 예약 검사가 서비스 절차에 **인라인**(`if(ticket.isReserved())` 자리)이라, 새 조건도 **서비스를 열어** 거기 넣어야 한다. → **리팩토링 후에는 서비스가 안 열리는데(0곳), 전에는 열린다(1곳).** 이 차이가 리팩토링이 산 저항력의 증거다. 또 후에는 `new Ticket(...).suspend(); ensureReservable()`로 **mock 0개** 단위 검증이 되지만, 전에는 서비스+repo·결제 mock을 세워야 한다.

---

## 예측 요약 (적용 전 잠금)

| | 타는 축 | 리팩토링이 끊었나 | production 수정 | `TicketService` | 전 vs 후 |
|---|---|:---:|:---:|:---:|---|
| R1 할인 | 가격 | ❌ | 2곳 | 열림(금액 계산) | **같음** |
| R2 판매중지 | 예약 불변식 | ✅ | 2곳 | **0곳** | **다름**(전엔 열림) |

> 적용 후 이 문서에 **"## 실제 결과"** 절을 덧붙여 예측과 대조한다(어긋났으면 원인·끊는 법 한 줄).
