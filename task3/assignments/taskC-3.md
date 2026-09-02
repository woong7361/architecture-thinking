# Task C-3: 헥사고날 설계 + 죽은 코드 재설계

(Grit's Why): 이론은 본인 코드로 세워야 손에 붙습니다. 1-2에서 SOLID로 재설계한 그 도메인(예: 티켓 예매)을 이번엔 포트와 어댑터로 올립니다.

### 수행 내용

1. 1-2의 My Design(또는 B-2 kata 재설계 결과)을 헥사고날 구조로 그리세요. Core(도메인) / Inbound Port / Outbound Port / Adapter를 식별하고, 의존성이 모두 안쪽을 향하는지 확인하세요.
2. 핵심 유스케이스 하나를 Gherkin 시나리오(Given/When/Then)로 쓰세요. When 절이 Inbound Port와 1:1로 매칭되는지 확인하세요. (이 Feature는 C-5에서 실제로 실행됩니다.)
3. 헥사고날 다이어그램 + 'Why' 설명을 쓰세요. (예: 결제 Adapter를 외부 PG로 교체해도 Core는 무수정인 이유, Mock으로 Core만 빠르게 단위 테스트할 수 있는 이유.)

### 제출물

- [x] 헥사고날 다이어그램(Core/Port/Adapter 식별) + When-Inbound Port 매핑.
- [x] 핵심 유스케이스 Gherkin Feature 1개.
- [x] 'Why 헥사고날' 설명(어댑터 교체 무수정 + 테스트 용이성). (최소 500자)

---

## 목표 헥사고날 설계

Core는 업무 규칙(Domain)과 유스케이스 조정(Application)을 합친 영역이고, Adapter는 Core가 정한 Port를 통해서만 드나든다. Inbound Port는 `ReserveTicketUseCase` 인터페이스로 명시한다 — Cucumber(C-5)와 HTTP(C-6) 두 Driver가 같은 계약에 의존하고, 과제 기준인 When↔Port 1:1 대응을 그대로 보여줄 수 있기 때문이다. (인터페이스가 헥사고날의 필수 문법이라서가 아니라 이 과제의 검증 조건 때문의 선택이다.)

```text
                         source-code dependency points inward

        Driving adapters                                      Driven adapters
  +---------------------------+                         +---------------------------+
  | Cucumber Step Definitions |                         | In-memory Persistence     |
  | HTTP ReservationController|                         | JPA Persistence Adapter   |
  |                           |                         | Recording Payment Adapter |
  |                           |                         | External PG HTTP Adapter  |
  +-------------+-------------+                         +-------------+-------------+
                |                                                     |
                | calls                                               | implements
                v                                                     v
            ____/-------------------------------------------------------\____
           /                                                               \
          /   Inbound Port                              Outbound Ports      \
         /    ReserveTicketUseCase                      TicketRepository      \
        /                                                 UserRepository        \
       |                                                  ChargePort            |
       |                                                                       |
       |                       APPLICATION CORE                                |
       |                                                                       |
       |        TicketService implements ReserveTicketUseCase                  |
       |        - depends on outbound ports, not JPA repositories              |
       |        - coordinates reservation use case                             |
       |                                                                       |
       |        Domain: Ticket, User, DiscountPolicy                           |
       |        - owns reservable check, state transition, amount policy       |
        \                                                                     /
         \                                                                   /
          \                                                                 /
           \____-------------------------------------------------------____/

```

화살표는 런타임 호출 순서가 아니라 소스 코드의 의존 방향이다. Inbound Adapter는 Core의 Inbound Port를 호출하고, 그 Port는 `TicketService`가 구현한다. Outbound Adapter는 Core의 Outbound Port를 구현하며, Core는 어떤 Adapter도 import하지 않는다. 여기서 `TicketRepository`와 `UserRepository`는 Spring Data JPA Repository가 아니라 Core가 소유한 Outbound Port다. JPA 구체 타입은 Adapter 내부의 `TicketJpaRepository`, `UserJpaRepository`에만 둔다. 런타임 호출 순서는 다음과 같다.

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

Port는 방향이 다른 두 종류다. **Inbound Port**(`ReserveTicketUseCase`)는 Core가 바깥에 *제공하는* 계약이고("reserve 유스케이스를 이렇게 호출하라"), **Outbound Port**(`TicketRepository`·`UserRepository`·`ChargePort` 등)는 Core가 바깥에 *요구하는* 계약이다("회원·티켓 조회와 저장, 결제 기능이 이런 모양으로 필요하다"). 둘 다 Core가 소유하고 정의한다는 점이 핵심이다. 특히 Outbound 쪽은 보통이라면 서비스가 Spring Data JPA Repository나 결제 SDK에 의존할 자리인데, 여기서는 반대로 JPA·결제 Adapter가 Core의 인터페이스를 구현한다. 이 의존성 역전(DIP) 덕분에 다이어그램의 양쪽 Adapter 열이 모두 안쪽 Core로 화살표를 꽂는 그림이 성립한다.

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

결제 후 저장 실패나 결제 응답 타임아웃은 예매 요청 안에서 즉시 보상으로 닫지 않는다. 외부 PG 결제는 DB 트랜잭션 밖이라 `@Transactional`로 함께 롤백되지 않고, 취소 요청 자체도 실패할 수 있기 때문이다. 따라서 현재 예매 Feature에는 저장 실패-보상 시나리오를 넣지 않는다.

다만 이것은 테스트하지 않는다는 뜻이 아니라 **별도 유스케이스로 분리해 테스트할 문제**라는 뜻이다. 결제 원장, 보정 대기 상태, 멱등 키가 생기는 시점에는 `ReconcilePaymentUseCase`나 `HandlePaymentWebhookUseCase` 같은 Inbound Port를 두고, 대사 배치나 PG webhook Adapter가 그 Port를 호출하게 만들 수 있다. 예를 들어 결제 요청이 타임아웃되면 예약은 확정하지 않고 결제 시도 원장을 `UNKNOWN`으로 남긴 뒤, 나중에 대사가 PG 승인 기록을 확인하면 티켓을 확정하거나 필요하면 취소 요청을 남기는 흐름을 별도 Feature로 고정할 수 있다. 이 경우에도 핵심은 같다. 예매 Feature의 `When`은 `ReserveTicketUseCase` 하나에, 대사 Feature의 `When`은 `ReconcilePaymentUseCase` 하나에 대응시켜 유스케이스 경계를 섞지 않는다.

## Why 헥사고날

헥사고날의 목적은 폴더를 육각형으로 만드는 게 아니라, 업무 정책이 외부 기술의 변경 이유를 떠안지 않게 하는 것이다. 티켓 예매의 핵심 유스케이스(회원 확인 → 예약 가능 판단 → 금액 결정 → 저장)가 `JpaRepository`나 특정 결제 SDK 타입을 직접 쓰면, 저장 기술·결제 벤더 교체가 곧 업무 코드 수정이 된다. 반대로 Core가 필요한 기능을 `TicketRepository`·`UserRepository`·`ChargePort` 같은 업무 목적 Port로 정의하면, JPA와 결제 SDK는 그 계약을 구현하는 Adapter가 된다.

**어댑터 교체 무수정.** JPA 저장소를 In-memory Fake로, 결제 API를 `RecordingPaymentAdapter`로 바꿔도 두 구현이 같은 Port 계약을 지키면 `Ticket`·`TicketService`·`ReserveTicketUseCase`는 손대지 않는다. C-5에서 같은 Gherkin Feature를 JPA/In-memory 두 조합에 실행하고 Core diff가 없음을 확인해 이 주장을 증명한다. 단 기술 변경이 Port의 의미를 바꾸거나 새 업무 규칙을 요구하면 Core도 바뀐다 — 헥사고날은 모든 변경을 없애는 구조가 아니라 교체의 영향이 Adapter 밖으로 번지지 않게 하는 구조다.

**테스트 용이성.** `Ticket`의 예약 불변식은 평범한 Java 객체라 Mock 없이 빠르게 단위 테스트하고, `TicketService`의 협력 순서는 In-memory Adapter와 통제 가능한 결제 Fake를 Port에 끼워 검증한다. Mock으로 Core 자체를 흉내 내는 게 아니라 Core 바깥 I/O만 Test Double로 대체하는 것이다. JPA 매핑·SQL 제약·외부 API 직렬화처럼 Adapter 안에서만 나는 위험은 Testcontainers 통합 테스트로 따로 잡는다. 덕분에 규칙 하나 확인하려고 매번 Spring Context와 DB 전체를 띄우지 않아도 된다.

## 리뷰 피드백 (Notion 원본)

> **피드백 메타데이터**
> - 출처 페이지: [Phase 1] 1-3(헥사고날) 제출 - 현웅님
> - URL: [Notion 원본 페이지](https://sponge-girdle-ad1.notion.site/Phase-1-1-3-3a26276f9e0081b399c3f614fe445fa7)
> - 수집 방법: 프로젝트 루트 `notion_mcp.md` 참조
> - 원문 보존: 댓글 본문은 Notion comment 레코드의 텍스트를 그대로 옮긴 것이며 일절 수정하지 않았다.
> - 라인 기준: 이 섹션 위쪽 본문의 라인 번호. 본문을 편집하면 다시 수집해야 한다.

리뷰어가 이 문서의 **어느 라인, 어떤 부분**에 **어떤 피드백**을 남겼는지 정리한 것이다.
총 1건.

### FB-C3-01 · L137

- **위치**: L137
- **지적된 부분**: 문단 전체 — 결제 후 저장 실패나 결제 응답 타임아웃은 예매 요청 안에서 즉시 보상으로 닫지 않는다. 외부 PG 결제는 DB 트랜잭션 밖이라 @Transactional로 함께 롤백되지 않고, 취소 요청 자체도 실패할 수 있기 때문이다. 따라서 현재 예매 Feature에는 저장 실패-보상 시나리오를 넣지 않는다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-08-10 16:05 KST
- **피드백 원문**:

```
실패를 억지로 한 트랜잭션 안에서 닫지 않고 별도 유스케이스로 미루셨습니다. 그러면 그 사이에 사용자는 무엇을 보나요? 결제는 승인됐는데 예약은 확정되지 않은 몇 분 동안 화면에 뭐라고 쓰시겠어요? 이 질문부터는 기술 결정이 아니라 커뮤니케이션 설계인데, 그 문구까지 지금 정해두시면 어떨까요?
```

**답변 초안:**

이 구간을 실패나 성공으로 단정해서 보여주면 안 된다고 생각합니다. 예약 상태를 `결제 확인 중`으로 두고 화면에는 다음과 같이 안내하겠습니다.

> 결제 결과를 확인하고 있습니다. 예약은 아직 확정되지 않았습니다. 중복 결제를 피하기 위해 다시 결제하지 마세요. 확인이 끝나면 예약 확정 또는 결제 취소 결과를 알려드리겠습니다.

사용자가 화면을 닫아도 같은 요청의 상태를 다시 조회할 수 있어야 하고, 결과가 바뀌면 알림과 예약 내역에 반영해야 합니다. 안내 시간은 임의로 `몇 분`이라고 약속하지 않고 PG 확인 시간과 대사 주기, 운영 대응 시간을 근거로 서비스 수준을 정한 뒤 문구에 넣겠습니다. 이 상태를 도입하면 기술적으로는 결제 시도 원장, 멱등 키, 상태 조회, 알림과 운영 처리까지 필요합니다. 별도 유스케이스로 미룬다는 결정은 처리 책임을 없애는 것이 아니라, 사용자에게 보이는 `확인 중` 상태와 그 상태를 끝내는 운영 책임까지 함께 설계한다는 뜻이어야 합니다.
