# 리팩토링 상세 리포트 — task2 B-5 (TicketService)

부분별로 **무엇을·어떻게 바꿨고, 왜 행위가 보존되는지(매 커밋 GREEN)**를 기록한다.
계획은 [refactoring-strategy.md](refactoring-strategy.md), 커밋 요약표는 [refactoring-log.md](refactoring-log.md).

- **대상**: `reserveTicket` 유스케이스 (`TicketService`, `Ticket`, `PaymentApi→ChargePort`)
- **안전망**: `f97c1a7`의 Cucumber 6 시나리오 (매 커밋 GREEN 유지 확인)
- **테스트 실행**: `JAVA_HOME="C:/Program Files/Amazon Corretto/jdk17.0.19_10" mvn clean test` → 매 커밋 `6 Scenarios (6 passed) / 40 Steps (40 passed)`

---

## 전체 변화 한눈에

```
리팩토링 전 (절차형)                          리팩토링 후 (Rich + 조립자)
─────────────────────────                     ─────────────────────────────
TicketService.reserveTicket()                 TicketService.reserveTicket()  ← 조립·순서만
  if (ticket.isReserved()) throw   ──┐          ticket.ensureReservable()    ─┐ 검사(도메인)
  ...charge...                        │          ...charge...                  │
  ticket.setReserved(true)           ├─ 이동 →   ticket.assignTo(userId)      ─┘ 전이(도메인)
  ticket.setUserId(userId)         ──┘
                                                Ticket  ← 예약 불변식의 주인
Ticket = getter/setter 자루(Anemic)             + ensureReservable() / assignTo()
  + setReserved()/setUserId() (열림)            - public setter 봉인(제거)
interface PaymentApi                            interface ChargePort (역할명)
```

누적 변경: `6 files changed, 39 insertions(+), 35 deletions(-)` (`f97c1a7..b69d858`).

---

## C1 — 예약 가능 판단을 Ticket으로 추출 `4cd1bca`

- **스멜**: Feature Envy(#9) — 서비스가 `ticket.isReserved()`로 남의 데이터를 검사. Anemic(#22).
- **기법**: Extract Function (도메인으로 행위 이동)

**바꾼 곳 1 — `Ticket.java`: 도메인 메서드 신설**
```java
+   /* 예약 가능 판단(불변식)을 스스로 책임진다 — 이미 예약된 티켓은 다시 예약될 수 없다. */
+   public void ensureReservable() {
+       if (reserved) {
+           throw new TicketAlreadyReservedException();
+       }
+   }
```

**바꾼 곳 2 — `TicketService.java`: 인라인 검사를 호출로 교체**
```java
-   if (ticket.isReserved()) {
-       throw new TicketAlreadyReservedException();
-   }
+   ticket.ensureReservable();
```

**왜 GREEN인가 (행위 보존)**
- 던지는 예외 타입 동일(`TicketAlreadyReservedException`), 위치 동일(charge **앞**) → "이미 예약 → 무청구" 시나리오 유지.
- **quirk 유지**: 없는 티켓이면 `ticket`이 null. `null.ensureReservable()`는 인스턴스 메서드 호출 지점에서 **여전히 NPE**를 던진다(null 체크를 넣지 않음). "없는 티켓 → NPE" 시나리오 그대로.

---

## C2 — 상태 전이를 Ticket으로 추출 `bc33046`

- **스멜**: Anemic(#22) — 상태 전이가 서비스에. Feature Envy(#9) — 서비스가 `setReserved/setUserId`로 남의 상태를 변경.
- **기법**: Extract Function (도메인으로 행위 이동), **내부 방어 guard 포함**

**바꾼 곳 1 — `Ticket.java`: 전이 메서드 신설(불변식 자기방어)**
```java
+   /* 예약 상태 전이를 스스로 책임진다 — 불변식을 자기방어하며 소유자를 기록한다. */
+   public void assignTo(long userId) {
+       if (reserved) {                                   // ← 자기방어: 서비스에 의존하지 않고 스스로 막음
+           throw new TicketAlreadyReservedException();
+       }
+       this.reserved = true;
+       this.userId = userId;
+   }
```

**바꾼 곳 2 — `TicketService.java`: 인라인 전이를 호출로 교체**
```java
-   ticket.setReserved(true);
-   ticket.setUserId(userId);
+   ticket.assignTo(userId);
```

**왜 GREEN인가 (행위 보존)**
- `assignTo`를 charge **뒤**에 유지 → "결제 거절 → 미예약"(전이 미도달), "저장 실패 → 청구된 채 무보상"(charge→전이→save 순서 동일) 두 quirk 보존.
- 내부 guard는 **happy 경로에서 트리거되지 않는다**: C1의 `ensureReservable`가 앞에서 이미 통과했고 그 사이 재예약이 없어 `reserved`는 여전히 false. → 동작 불변, Ticket은 불변식의 완전한 주인.

---

## C3 — public setter 제거 `d5cceed`

- **스멜**: Mutable Data(#6) — 외부가 `setReserved/setUserId`로 상태를 아무 때나 변경 가능(잘못된 상태 생성).
- **기법**: Remove Setting Method

**바꾼 곳 1 — `Ticket.java`: setter 삭제(캡슐화)**
```java
-   public void setReserved(boolean reserved) { this.reserved = reserved; }
-   public void setUserId(long userId) { this.userId = userId; }
```
(getter `isReserved()`/`getUserId()`는 테스트 단언이 읽으므로 존치.)

**바꾼 곳 2 — `TicketReservationSteps.java`: 유일한 setter 사용처(테스트 seed)를 도메인 행위로 전환**
```java
-   ticket.setReserved(true);
+   ticket.assignTo(0L); // setter 제거 후: 도메인 행위로 '이미 예약됨' 상태를 만든다(소유자는 미단언이라 0)
```

**왜 GREEN인가 (행위 보존)**
- production 사용처는 C2에서 `assignTo`가 흡수 → 삭제해도 컴파일·동작 안전.
- seed는 갓 생성된(미예약) 티켓에 `assignTo(0L)` → 내부 guard 통과 → `reserved=true`. 시나리오는 예외 타입·무청구만 단언하고 소유자를 읽지 않아 **관찰상 동등**.

---

## C4 — PaymentApi를 ChargePort로 개명 `00da93d`

- **스멜**: Mysterious Name(#1) — 이름이 역할(청구 포트)과 어긋남.
- **기법**: Rename / Change Function Declaration (순수 개명, 구조 변화 없음)

**바꾼 곳** (`git mv`로 이력 보존)
```
PaymentApi.java → ChargePort.java   : interface PaymentApi → interface ChargePort
TicketService.java                  : 필드/생성자 타입 PaymentApi → ChargePort, paymentApi → chargePort
RecordingPaymentApi.java (test)     : import·implements PaymentApi → ChargePort
```

**왜 GREEN인가**: 타입/식별자 이름만 바뀌고 시그니처(`charge(String,int):boolean`)·주입·호출 순서는 그대로. 동작 0.

> **정직 메모**: 이건 순수 rename이다. `PaymentApi`는 이미 `charge` 단일 메서드였고 이미 생성자 주입이었으므로, **ISP(너비)도 DIP(방향)도 바뀌지 않는다** — 실효는 이름뿐. (B-4 답안의 뚱뚱한 `PaymentApi(charge+refund+queryHistory)`는 가상 예시였고 실제 코드엔 없다.)

---

## C5 — 단계 주석·낡은 헤더 정리 `bd73e98`

- **스멜**: Comments(#24) — `// 1.~// 4.` "무엇을 하는지" 설명 주석(냄새 탈취제) + 리팩토링 후 거짓이 된 헤더.
- **기법**: Comments 스멜 해소의 **삭제 단계** (C1·C2·C4가 코드를 self-describing으로 만든 뒤의 정리. 다른 기법 안 섞음.)

**바꾼 곳 1 — `TicketService.java`: 단계 주석 제거 + 헤더 갱신**
```java
-   // 1. 유저 조회 (DB)      ... // 2. 티켓 조회 ... // 3. 결제 시도 ... // 4. 티켓 상태 변경
    (모두 삭제 — ensureReservable()/chargePort.charge()/assignTo()가 스스로 설명)
-   /* ...하나의 메소드에 모든 로직이 절차적으로 구현... */
+   /* 티켓 예매 유스케이스의 조립자(orchestration). 규칙은 Ticket, I/O는 포트, 서비스는 순서만. */
```

**바꾼 곳 2 — `Ticket.java`: 헤더 갱신**
```java
-   /* 빈약한 도메인 모델(Anemic): getter/setter 자루. 규칙은 하나도 스스로 지키지 않는다. */
+   /* 풍부한 도메인 모델(Rich): 예약 불변식과 상태 전이를 스스로 소유한다. */
```

**왜 GREEN인가**: 주석만 변경, 실행 코드 0 변화.

---

## C6 — assignTo가 ensureReservable을 재사용(중복 제거) `b69d858`

- **스멜**: Duplicated Code(#2) — 예약 불변식 검사(`if (reserved) throw`)가 `ensureReservable()`와 `assignTo()`에 복제(C2에서 방어 심층화로 도입한 중복).
- **기법**: Remove Duplication (추출된 함수 재사용)

**바꾼 곳 — `Ticket.java`: 복제된 검사를 호출로 대체**
```java
    public void assignTo(long userId) {
-       if (reserved) {
-           throw new TicketAlreadyReservedException();
-       }
+       ensureReservable();
        this.reserved = true;
        this.userId = userId;
    }
```

**왜 GREEN인가 (행위 보존)**
- `assignTo`는 여전히 예약된 티켓이면 던지고, 아니면 전이 — 관찰 동작 동일. happy·seed 모두 진입 시 `reserved=false`라 통과.
- 검사 규칙이 한 곳(`ensureReservable`)에 모여, 규칙이 바뀌면 한 곳만 고치면 된다. "예약하려면 예약 가능해야 한다"가 코드로 드러난다.

---

## 최종 확인

- **행위 보존**: C0~C6 **매 커밋 6/6 GREEN**. quirk(없는 티켓 NPE, 저장 실패 무보상) 포함해 관찰 동작 불변.
- **구조 변화**: 예약 규칙(검사+전이)이 서비스 → `Ticket`으로 이동(Rich), 검사 규칙 단일화, setter 봉인, 포트 역할명 정렬, 서비스는 조립자로 축소.
- **한 커밋 한 기법 / 파울러 카탈로그명 / 매 커밋 GREEN** 요건 충족(리팩토링 커밋 6개, 최소 4~5 초과 달성).
- **범위 밖으로 남긴 것(정직하게)**: 원자성/보상 경계(롤백 인프라 부재로 quirk 박제 — 동작 변경이라 리팩토링 아님), 결제수단 다형성(카드 1개, v<2 → DEFER; 트리거 = 수행내용 5의 새 결제수단).
