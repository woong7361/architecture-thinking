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

---

## 실제 결과 (적용 후)

예측 잠금: `88301ab` → R1: `c8ddfbe` → R2: `0042eed`. 매 커밋 GREEN(6→7→8 시나리오).

### R1 — 실제로 바뀐 파일 (가격 축)

| 구분 | 파일 | 예측 | 실제 |
|---|---|:---:|:---:|
| production 신규 | `DiscountPolicy.java` | ✅ | ✅ |
| production 수정 | `TicketService.java`(금액 계산) | ✅ | ✅ |
| 테스트 수정 | `TicketReservationSteps.java`(생성 1줄) | ✅ | ✅ |
| 테스트 신규 | `new_requirements.feature` | ✅ | ✅ |

- **예측과 일치. production 수정 2곳.** `Ticket`·`ChargePort`·repos·`User`·예외 불변.
- **대조 확인**: `TicketService`의 바뀐 줄은 `charge(info, ticket.getPrice())` → `charge(info, discountPolicy.finalAmount(ticket.getPrice()))`. 이 `charge` 호출은 **리팩토링 전에도 서비스에 있던 라인**이라, 리팩토링 전이었어도 수정 위치·개수가 **동일**했다. → **리팩토링이 이 변경엔 저항력을 주지 못했다(예상대로).** 가격 축은 우리가 끊은 적이 없기 때문.

### R2 — 실제로 바뀐 파일 (예약 규칙 축)

| 구분 | 파일 | 예측 | 실제 |
|---|---|:---:|:---:|
| production 수정 | `Ticket.java`(필드+suspend()+ensureReservable 한 줄) | ✅ | ✅ |
| production 신규 | `TicketSuspendedException.java` | ✅ | ✅ |
| **`TicketService`** | **0곳** | ✅ | ✅ (`git status`로 미포함 확인) |
| 테스트 수정 | `TicketReservationSteps.java`(Given/Then 추가) | ✅ | ✅ |
| 테스트 신규 | `new_requirements.feature` | ✅ | ✅ |

- **예측과 일치. production 수정 2곳, 서비스 0곳.**
- **대조 확인**: 새 예약 조건이 `Ticket.ensureReservable()` **한 곳**에 떨어졌고, 서비스는 그 메서드를 **호출만** 하므로 자동 적용됐다(수정 0). 리팩토링 **전**이었다면 이 검사는 `reserveTicket` 절차의 `if(ticket.isReserved())` 자리에 **인라인**이라 **서비스를 열어** 넣어야 했다(후=0곳 / 전=1곳). 단위 검증도 후에는 `new Ticket(...).suspend()` 뒤 `ensureReservable()`로 **mock 0개**, 전에는 서비스+repo·결제 mock 필요.

### 종합 — 예측은 어긋나지 않았고, 저항력은 축마다 달랐다

| | production 수정 | `TicketService` | 리팩토링 전이었다면 | 저항력 |
|---|:---:|:---:|:---:|:---:|
| R1 할인 (가격 축) | 2곳 | 열림(금액 계산) | **동일** | 무관 |
| R2 판매중지 (예약 규칙 축) | 2곳 | **0곳** | 서비스 열림 | **개선됨** |

- **예측과 어긋난 곳 없음. 변경이 번지지도 않음**(둘 다 예측한 곳에만 갇힘).
- 그러나 "2곳에 갇힘"이 곧 "리팩토링 덕"은 아니다 — **R1은 baseline도 2곳**이라 리팩토링과 무관하고, **R2만 서비스가 안 열리는 진짜 차이**를 보였다.
- **결론**: 변화 저항력은 **끊은 축에서만** 생긴다. 우리 리팩토링은 **예약 규칙 축**을 `Ticket`으로 끊었으므로 그 축의 변경(R2)에서만 containment가 나타나고, **안 끊은 가격 축**의 변경(R1)은 baseline과 같다. "파일 몇 곳" 지표만 보면 둘 다 2곳이라 같아 보이지만, **서비스가 열리는가 / mock 없이 검증되는가**를 함께 봐야 차이가 드러난다.
- **안 끊은 축은 실패가 아니라 의도한 트레이드오프**: 가격 축은 아직 안 끊겼다. 그러나 이건 리팩토링의 실패가 아니다. "모든 결합을 끊어라"와 "미리 추상화하지 마라(YAGNI)"는 서로 반대 방향으로 당기고, 할인이 **1종류(v=1)**뿐인 지금 그 축을 미리 끊는 것이야말로 과설계(YAGNI 위반)다. 그래서 일부러 안 끊었다. 게다가 R1은 지금 **새지도 않는다** — 고친 곳이 2곳에 갇혀 있다. "샌다"는 **할인이 2종류로 늘 때(v=2)** 서비스가 열리는 미래 얘기이고, 그때 `DiscountPolicy` 인터페이스를 뽑아 끊는다(트리거 기록). 정리하면 **정리한 축(예약 규칙)은 변경이 한 곳에 갇혀 리팩토링이 값을 했고, 안 정리한 축(가격)은 지금 옳게 남겨둔 채 끊을 조건만 예약해뒀다.**
