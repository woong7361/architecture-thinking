# Task C-3: 헥사고날 설계 + 죽은 코드 재설계

(Grit's Why): 이론은 본인 코드로 세워야 손에 붙습니다. 1-2에서 SOLID로 재설계한 그 도메인(예: 티켓 예매)을 이번엔 포트와 어댑터로 올립니다.

### 수행 내용

1. 1-2의 My Design(또는 B-2 kata 재설계 결과)을 헥사고날 구조로 그리세요. Core(도메인) / Inbound Port / Outbound Port / Adapter를 식별하고, 의존성이 모두 안쪽을 향하는지 확인하세요.
2. 핵심 유스케이스 하나를 Gherkin 시나리오(Given/When/Then)로 쓰세요. When 절이 Inbound Port와 1:1로 매칭되는지 확인하세요. (이 Feature는 C-5에서 실제로 실행됩니다.)
3. 헥사고날 다이어그램 + 'Why' 설명을 쓰세요. (예: 결제 Adapter를 외부 PG로 교체해도 Core는 무수정인 이유, Mock으로 Core만 빠르게 단위 테스트할 수 있는 이유.)

### 제출물

- [x]  헥사고날 다이어그램(Core/Port/Adapter 식별) + When-Inbound Port 매핑.
- [x]  핵심 유스케이스 Gherkin Feature 1개.
- [x]  'Why 헥사고날' 설명(어댑터 교체 무수정 + 테스트 용이성). (최소 500자)

---

## 목표 헥사고날 설계

Core는 업무 규칙(Domain)과 유스케이스 조정(Application)을 합친 영역이고, Adapter는 Core가 정한 Port를 통해서만 드나든다. Inbound Port는 `ReserveTicketUseCase` 인터페이스로 명시한다 — Cucumber(C-5)와 HTTP(C-6) 두 Driver가 같은 계약에 의존하고, 과제 기준인 When↔Port 1:1 대응을 그대로 보여줄 수 있기 때문이다. (인터페이스가 헥사고날의 필수 문법이라서가 아니라 이 과제의 검증 조건 때문의 선택이다.)

```text
        INBOUND ADAPTERS                     CORE                      OUTBOUND ADAPTERS
   ────────────────────────────────────────────────────────────────────────────────────────

   Cucumber Step Def (C-5) ──┐
                             ├──▶  «Inbound Port»
   ReservationController ────┘      ReserveTicketUseCase
   (C-6)                                  ▲ implements
                                    TicketService ──▶ Domain: Ticket · User · DiscountPolicy
                                          │
                                          ▼
                                    «Outbound Ports»
                                     TicketRepository ┐
                                     UserRepository   ┴◀── In-memory / JPA Repo Adapter
                                     ChargePort       ◀─── Payment API / Recording Payment Adapter

   화살표 = 소스 코드 의존 방향(항상 안쪽). Core는 어떤 Adapter도 import하지 않는다.
```

화살표는 런타임 호출 순서가 아니라 소스 코드의 의존 방향이다. Inbound Adapter는 Core의 Inbound Port를 알고, Outbound Adapter는 Core의 Outbound Port를 구현하며, Core는 어떤 Adapter도 import하지 않는다. 런타임 호출 순서는 다음과 같다.

```text
Cucumber Step Definition 또는 ReservationController
    -> ReserveTicketUseCase.reserve(command)
    -> TicketService
    -> UserRepository / TicketRepository
    -> Ticket.ensureReservable()
    -> DiscountPolicy.finalAmount()
    -> ChargePort
    -> Ticket.assignTo(userId)
    -> TicketRepository.save(ticket)
```

Core 안은 다시 두 겹이다. **Domain**(`Ticket`·`User`·`DiscountPolicy`)은 기술 타입을 모르는 순수 업무 규칙으로, 티켓이 예약 가능한지·할인 후 금액이 얼마인지 같은 판단을 스스로 안다. **Application**(`TicketService`)은 그 규칙들을 하나의 유스케이스로 조정한다 — 회원을 조회하고, 도메인에 판단을 위임하고, 결제를 요청하고, 결과를 저장하는 *순서*를 소유한다. 규칙 자체는 Domain에, 규칙을 엮는 절차는 Application에 둔다.

Port는 방향이 다른 두 종류다. **Inbound Port**(`ReserveTicketUseCase`)는 Core가 바깥에 *제공하는* 계약이고("reserve 유스케이스를 이렇게 호출하라"), **Outbound Port**(`TicketRepository`·`ChargePort` 등)는 Core가 바깥에 *요구하는* 계약이다("저장·결제 기능이 이런 모양으로 필요하다"). 둘 다 Core가 소유하고 정의한다는 점이 핵심이다. 특히 Outbound 쪽은 보통이라면 서비스가 DB·결제 라이브러리에 의존할 자리인데, 여기서는 반대로 라이브러리 Adapter가 Core의 인터페이스를 구현한다. 이 의존성 역전(DIP) 덕분에 다이어그램의 양쪽 Adapter 열이 모두 안쪽 Core로 화살표를 꽂는 그림이 성립한다.

## When ↔ Inbound Port 1:1 매핑

Gherkin의 `When ...`은 하나의 업무 행위이고, Step Definition은 값을 `ReserveTicketCommand`로 바꾼 뒤 Inbound Port 메서드 하나만 호출한다.

```java
public interface ReserveTicketUseCase {
    ReservationResult reserve(ReserveTicketCommand command);
}
```

```text
When 회원 1이 카드정보 "card-token"으로 티켓 20의 예매를 요청한다
    -> TicketReservationSteps.reserveTicket(...)
    -> ReserveTicketCommand(userId=1, ticketId=20, paymentInfo="card-token")
    -> ReserveTicketUseCase.reserve(command)
```

`new TicketService(...)`를 When 안에서 직접 만들지 않고, 조립 지점에서 `ReserveTicketUseCase` 구현을 주입한다. 그래야 Cucumber가 서비스 구현이 아니라 공개된 유스케이스 계약을 검증하고, C-6의 HTTP Controller도 같은 계약을 재사용할 수 있다.

## 핵심 유스케이스 Gherkin Feature

Feature는 티켓 예매 하나다. Happy Path와 C-5에서 요구하는 주요 Unhappy Path를 같은 Inbound Port 계약에 고정한다.

```gherkin
Feature: 티켓 예매
  회원은 예약 가능한 티켓의 결제에 성공하면 티켓을 예매할 수 있다.
  예매 조건을 충족하지 못하거나 외부 처리가 실패하면 일관되지 않은 상태를 남기지 않는다.

  Background:
    Given 회원 저장소와 티켓 저장소가 비어 있다

  Scenario: 등록된 회원이 예약 가능한 티켓을 예매한다
    Given 회원 1이 등록되어 있다
    And 가격 30000원짜리 미예약 티켓 20이 있다
    When 회원 1이 카드정보 "card-token"으로 티켓 20의 예매를 요청한다
    Then 예매는 성공한다
    And 티켓 20은 회원 1에게 예약된다
    And 30000원이 청구된다

  Scenario: 등록되지 않은 회원은 티켓을 예매할 수 없다
    Given 가격 30000원짜리 미예약 티켓 20이 있다
    When 회원 1이 카드정보 "card-token"으로 티켓 20의 예매를 요청한다
    Then 예매는 회원 없음으로 거부된다
    And 결제는 청구되지 않는다

  Scenario: 판매 중지된 티켓은 예매할 수 없다
    Given 회원 1이 등록되어 있다
    And 가격 30000원짜리 판매 중지된 티켓 20이 있다
    When 회원 1이 카드정보 "card-token"으로 티켓 20의 예매를 요청한다
    Then 예매는 판매 중지로 거부된다
    And 결제는 청구되지 않는다

  Scenario: 결제가 거절되면 티켓은 예약되지 않는다
    Given 회원 1이 등록되어 있다
    And 가격 30000원짜리 미예약 티켓 20이 있다
    And 결제는 거절되는 상황이다
    When 회원 1이 카드정보 "card-token"으로 티켓 20의 예매를 요청한다
    Then 예매는 결제 실패로 거부된다
    And 티켓 20은 예약되지 않는다
```

결제 후 저장이 실패하면 결제만 청구되고 취소가 없는 일관성 문제(B-5에서 특성화한 결함)가 남는다. 이 정합성 보정은 즉시 인라인 보상(결제 취소)이 아니라 **별도 대사(reconciliation)/배치**(결제 원장 + 멱등 정산)로 다룬다 — 외부 PG 결제는 DB 트랜잭션 밖이라 `@Transactional`로 롤백되지 않고, 보상 호출 자체도 실패할 수 있어 보상만으로는 정합성이 닫히지 않기 때문이다. 따라서 인수테스트에는 저장 실패-보상 시나리오를 두지 않는다.

## Why 헥사고날

헥사고날의 목적은 폴더를 육각형으로 만드는 게 아니라, 업무 정책이 외부 기술의 변경 이유를 떠안지 않게 하는 것이다. 티켓 예매의 핵심 유스케이스(회원 확인 → 예약 가능 판단 → 금액 결정 → 저장)가 `JpaRepository`나 특정 결제 SDK 타입을 직접 쓰면, 저장 기술·결제 벤더 교체가 곧 업무 코드 수정이 된다. 반대로 Core가 필요한 기능을 `TicketRepository`·`UserRepository`·`ChargePort` 같은 업무 목적 Port로 정의하면, JPA와 결제 SDK는 그 계약을 구현하는 Adapter가 된다.

**어댑터 교체 무수정.** JPA 저장소를 In-memory Fake로, 결제 API를 `RecordingPaymentAdapter`로 바꿔도 두 구현이 같은 Port 계약을 지키면 `Ticket`·`TicketService`·`ReserveTicketUseCase`는 손대지 않는다. C-5에서 같은 Gherkin Feature를 JPA/In-memory 두 조합에 실행하고 Core diff가 없음을 확인해 이 주장을 증명한다. 단 기술 변경이 Port의 의미를 바꾸거나 새 업무 규칙을 요구하면 Core도 바뀐다 — 헥사고날은 모든 변경을 없애는 구조가 아니라 교체의 영향이 Adapter 밖으로 번지지 않게 하는 구조다.

**테스트 용이성.** `Ticket`의 예약 불변식은 평범한 Java 객체라 Mock 없이 빠르게 단위 테스트하고, `TicketService`의 협력 순서는 In-memory Repository와 통제 가능한 결제 Fake를 Port에 끼워 검증한다. Mock으로 Core 자체를 흉내 내는 게 아니라 Core 바깥 I/O만 Test Double로 대체하는 것이다. JPA 매핑·SQL 제약·외부 API 직렬화처럼 Adapter 안에서만 나는 위험은 Testcontainers 통합 테스트로 따로 잡는다. 덕분에 규칙 하나 확인하려고 매번 Spring Context와 DB 전체를 띄우지 않아도 된다.

## Task C 전체와 연결해 추가로 손볼 곳

1. **C-3 반영 / C-5 구현** — 저장 Adapter 교체·Core 무수정이 이번 검증 목표이므로 `TicketJpaEntity`를 Adapter에 두고 순수 Domain과 분리한다(모든 프로젝트 공통 규칙이 아니라 이번 과제 목표 때문의 선택).
2. **C-5 구현** — Step Definition은 `TicketService`를 직접 생성하지 않고 Inbound Port만 호출한다. Feature 문장은 재사용하되 조립 코드는 Port 기준으로 고친다.
3. **C-5 검증** — JPA+Testcontainers 조합과 In-memory 조합에 같은 Feature를 실행하고, 두 GREEN 로그와 Core 디렉터리 diff가 없다는 결과를 함께 제출한다.
4. **C-5 구현·검증** — In-memory Fake가 실제 저장 경계를 흉내 내는지 확인한다. 지금은 같은 참조를 Map에 보관해 저장 실패 시 상태가 오염될 수 있으므로, 조회·저장에 복사본을 써서 저장 성공 시점에만 상태를 반영한다(불변 Domain 전환은 변경 폭이 커 이번엔 제외).
5. **C-5·C-6 검증** — C-5는 Inbound Port에서 시작하는 Port-level 인수테스트, C-6은 HTTP 요청이 `ReservationController`를 거쳐 같은 Port로 들어오는 end-to-end walking skeleton이다. 둘을 같은 테스트로 부르면 Inbound Adapter 검증이 빠진다.
6. **C-6 구현** — 경계는 패키지 이름이 아니라 자동 검증으로 고정한다. Core가 `adapter`·Spring·JPA 타입에 의존하지 않는지 의존성 테스트로 확인하고, AI 생성 단계마다 실행한다.
7. **C-4 구현** — Dockerfile에서 JDK 버전을 고정해 로컬 `JAVA_HOME` 차이를 제거하고, Compose·CI가 같은 이미지를 쓰게 해 재현성 근거로 삼는다.

현재 검증이 끝난 범위는 B-5 기존 코드의 회귀 테스트뿐이다. JDK 17로 실행해 8개 시나리오·53개 Step이 모두 통과했다. 새 Inbound Port, In-memory 복사 의미 수정, JPA Adapter 교체는 목표와 검증 시나리오만 정했고 아직 구현·GREEN 확인 전이며, C-5·C-6의 완료 증빙으로 남긴다.
