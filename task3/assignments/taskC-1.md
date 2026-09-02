# Task C-1: 왜 포트 &amp; 어댑터인가 (N-Tiered 비평 → DDD → DIP)

(Grit's Why): 헥사고날은 외우는 패턴이 아니라, 'N-Tiered가 왜 테스트하기 어렵고 변화에 약한가'에 대한 해법입니다.

### 수행 내용

1. 과거 비평: 고전적인 N-Tiered 아키텍처가 Testability와 Flexibility 관점에서 왜 한계가 있는지 분석하세요. (JPA @Entity가 상위 계층으로 새어 강한 결합을 만드는 지점을 짚으세요.)
2. 가치 이해: DDD의 핵심, 즉 '왜 Domain을 분리하고 외부 의존성 0으로 보호해야 하는가'를 본인 언어로 정리하세요.
3. 해법 분석: 헥사고날이 1과 2를 DIP로 어떻게 푸는지 원리를 설명하세요. Inbound Port와 Outbound Port가 왜 인터페이스이고 도메인의 일부인지, Adapter가 왜 구현체이고 인프라인지, '의존성은 안쪽을 향한다'가 무슨 뜻인지.

### 제출물

- [x] N-Tiered 한계 비평(JPA 누수·결합 중심). (최소 400자)
- [x] DDD가 지키려는 것 + 헥사고날의 DIP 활용 원리. (최소 400자)

---

## 답안 1: N-Tiered 한계 비평

고전적인 N-Tiered 아키텍처는 Controller, Service, Repository처럼 책임을 계층별로 나눠 구조를 이해하기 쉽게 만든다. 하지만 책임의 분리가 곧 의존성의 분리를 뜻하지는 않는다. 상위 계층이 하위 계층을 향해 의존하는 구조에서 Service가 JPA Repository와 `@Entity`를 직접 사용하고 Controller까지 그 엔티티를 반환하면, 하나의 객체가 영속성 모델과 업무 모델, API 모델의 역할을 동시에 맡는다. 이때 상위 계층은 엔티티의 필드 구조뿐 아니라 지연 로딩, 프록시, 관리·분리 상태, 더티 체킹과 같은 영속성 문맥의 동작에도 간접적으로 묶인다.

이 결합은 Testability를 떨어뜨린다. 단위 테스트에서 만든 평범한 엔티티와 실제 Hibernate가 관리하는 프록시 객체의 동작이 달라질 수 있고, Mock Repository는 지연 로딩, 트랜잭션, 더티 체킹, DB 제약을 재현하지 못한다. 따라서 업무 규칙을 검증하려고도 Spring Context, 트랜잭션, 데이터베이스를 포함한 통합 테스트가 필요해질 수 있다. Flexibility도 낮아진다. 테이블이나 연관관계 매핑을 바꾼 일이 Service와 API 응답 변경으로 번지고, 저장 기술을 교체할 때 업무 코드까지 수정해야 하기 때문이다.

### 예시: 주문 조회에서 JPA 엔티티를 전 계층이 공유한다면

```java
@Entity
class Order {
    @OneToMany(fetch = FetchType.LAZY)
    private List<OrderItem> items;

    Money totalPrice() {
        return items.stream()
                .map(OrderItem::price)
                .reduce(Money.ZERO, Money::add);
    }
}
```

Repository가 반환한 `Order`를 Service가 사용하고 Controller가 그대로 응답한다고 하자. 단위 테스트에서는 `items`에 평범한 `List`를 넣으므로 `totalPrice()`가 통과한다. 실제 실행에서는 `items`가 Hibernate의 지연 로딩 컬렉션일 수 있다. 영속성 컨텍스트가 닫힌 뒤 접근하면 조회가 실패할 수 있고, 컨텍스트가 열려 있더라도 예상하지 못한 추가 쿼리가 실행될 수 있다. Mock을 사용한 테스트는 이 차이를 재현하지 못하므로 JPA를 포함한 통합 테스트가 추가로 필요해진다. [Hibernate 공식 문서](https://docs.hibernate.org/orm/6.6/userguide/html_single/)에서도 엔티티가 영속성 컨텍스트에 따라 관리 상태와 분리 상태를 가지며, 속성과 연관관계가 필요할 때 로딩될 수 있다고 설명한다.

변경에도 같은 결합이 드러난다. API가 `Order`를 그대로 응답하고 있다면 테이블 관계를 바꾸려고 `items` 매핑을 수정한 일이 Repository 안에서 끝나지 않는다. Service의 계산 방식과 Controller의 응답 구조까지 영향을 받을 수 있다. 저장 기술의 세부 모델이 업무와 API의 공용 계약이 되었기 때문이다.

JPA와 DB가 전혀 없어도 비슷한 결합이 생길 수 있다. `OrderController`가 `HttpServletRequest`를 `OrderService`에 그대로 넘기고, Service가 `X-Customer-Grade` 헤더를 읽어 할인 여부를 결정한다고 하자. 이제 할인 규칙을 단위 테스트하려면 Servlet 요청 객체나 Mock을 준비해야 한다. 나중에 같은 주문 기능을 HTTP가 아닌 배치나 메시지 소비자가 호출하면 `HttpServletRequest`가 없으므로 Service를 수정하거나 가짜 요청 객체를 만들어야 한다. 업무 규칙이 웹 계층의 입력 형식에 묶였기 때문이다. Service가 `CustomerGrade` 같은 업무 값만 받았다면 호출 채널이 바뀌어도 할인 규칙은 그대로 재사용할 수 있다.

그러나 이것을 JPA만의 문제나 모든 N-Tiered의 필연적 실패로 보지는 않는다. 계층을 나누더라도 웹 요청 객체나 외부 시스템의 응답 모델 같은 기술 타입을 Service가 직접 사용하면 경계는 다시 흐려진다. 또한 N-Tiered는 기술 역할을 수평으로 나누기 때문에 하나의 기능 변경이 여러 계층을 차례로 관통하고, 업무 규칙이 여러 계층에 흩어지기 쉽다. 결국 내가 보는 핵심 한계는 계층의 개수가 아니라 **도메인 정책이 외부 기술과 그 모델을 향해 의존한다는 것**이다. 이 방향을 그대로 둔 채 파일과 패키지만 나누면 겉으로는 분리되어도 테스트 비용과 변경 전파 범위는 줄어들지 않는다.

---

## 답안 2: DDD가 원하는 것과 방향성

지금까지 배운 OOP와 TDD는 모두 변경 비용을 낮추는 데 기여했다. OOP는 데이터와 행위를 책임에 따라 객체에 배치하고, TDD는 변경이 기존 동작을 깨뜨렸는지 빠르게 알려준다. 그러나 둘만으로는 **우리가 이해한 업무가 실제 업무와 같은가**, **어떤 업무 개념과 규칙을 코드의 중심에 놓아야 하는가**까지 결정해 주지 않는다. 잘못 이해한 규칙도 객체로 깔끔하게 나누고 테스트로 정확하게 보호할 수 있다.

내가 이해한 DDD의 목적은 소프트웨어를 기술 처리 절차가 아니라 **업무 지식이 드러나는 모델**로 만드는 것이다. 도메인 전문가와 개발자가 함께 업무를 탐구하고, 대화와 코드에서 같은 보편 언어를 사용한다. 하나의 용어와 모델이 일관된 의미를 유지할 수 있는 범위는 Bounded Context로 나누고, 사업의 차별성과 복잡성이 모인 Core Domain에 가장 많은 설계 노력을 집중한다. 그 결과 요구사항이 바뀌었을 때 개발자는 Controller, 테이블, 상태 코드를 뒤지는 대신 바뀐 업무 개념과 규칙이 표현된 모델을 먼저 찾을 수 있다.

예를 들어 요구사항이 "판매 중지된 티켓은 예약할 수 없다"라고 바뀌었다고 하자. 이 규칙이 Controller 조건문, Service의 상태 코드 비교, DB 제약으로 흩어져 있으면 수정할 위치를 모두 찾아야 하고 어느 경로에서는 규칙을 빠뜨릴 수 있다. DDD는 업무에서 사용하는 `Ticket`, `판매 중지`, `예약`이라는 말을 코드에도 그대로 사용하고, 예약 상태를 가진 `Ticket`이 불변식을 지키게 한다.

```java
public void reserveBy(UserId userId) {
    if (status == TicketStatus.SUSPENDED) {
        throw new SuspendedTicketCannotBeReserved();
    }
    if (status == TicketStatus.RESERVED) {
        throw new AlreadyReservedTicket();
    }
    this.status = TicketStatus.RESERVED;
    this.reservedBy = userId;
}
```

이렇게 하면 요구사항, 코드, 테스트가 모두 "판매 중지된 티켓은 예약할 수 없다"는 같은 언어를 사용한다. 규칙 변경은 그 규칙의 주인인 `Ticket`과 도메인 테스트를 중심으로 일어나며, 잘못된 상태는 객체가 스스로 막는다. DDD가 원하는 변경 용이성은 모든 변경을 무조건 싸게 만드는 것이 아니라, **핵심 업무 변화가 업무 언어와 모델을 따라 예측 가능한 위치에 모이게 하는 것**이다.

방향성은 분명하다. 먼저 AI와 반복해서 대화하며 도메인 규칙과 용어의 후보를 빠르게 발견한다. AI가 제안한 내용은 정책 문서, 실제 업무 사례, 운영 데이터로 검증한다. 검증된 언어와 규칙을 클래스와 메서드, 테스트에 반영하고, 의미가 달라지는 지점에는 명시적인 Context 경계를 세운다. Entity, Value Object, Aggregate 같은 전술 패턴은 이 모델과 불변식을 표현할 때 필요한 만큼만 사용한다. 단순 CRUD까지 모두 복잡한 도메인 모델로 만들지 않고, 변화가 잦고 사업적으로 중요한 영역에 집중한다.

도메인을 외부 의존성으로부터 보호하는 것도 이 방향의 연장선이다. 다만 "외부 의존성 0" 자체가 DDD의 정의는 아니다. DDD가 먼저 정하는 것은 **무엇을 보호할 것인가**이며, 그 대상은 업무 언어와 규칙을 담은 도메인 모델이다.

---

## 답안 3: 헥사고날이 앞선 비평을 해결하는 방식

### 헥사고날이란

헥사고날 아키텍처는 애플리케이션을 업무 규칙과 유스케이스가 있는 안쪽과 HTTP, 데이터베이스, 메시지 브로커 같은 기술이 있는 바깥쪽으로 나누는 방식이다. 핵심은 육각형 모양이나 여섯 개의 면이 아니라, 안쪽과 바깥쪽이 대화하는 경계를 명시하는 데 있다. Alistair Cockburn의 원문도 애플리케이션이 사용자 인터페이스나 데이터베이스 없이 동작하고 테스트될 수 있도록 안쪽과 바깥쪽을 분리하는 데 초점을 둔다.

이 경계의 계약이 Port이고, 특정 기술로 Port에 연결되는 번역기가 Adapter다. 예를 들어 티켓 예약 유스케이스를 호출하는 계약은 Inbound Port가 된다. 예약 결과를 저장해 달라는 Application Core의 요구는 `SaveTicketPort` 같은 Outbound Port로 표현할 수 있다. HTTP 요청을 예약 명령으로 바꾸는 객체와 Domain `Ticket`을 JPA Entity로 바꾸어 저장하는 `TicketJpaAdapter`는 각각 Inbound Adapter와 Outbound Adapter다.

Port는 보통 Java `interface`로 표현한다. 그러나 헥사고날의 본질은 인터페이스 문법 자체가 아니라 계약의 소유권과 사용하는 언어다. Port는 `HttpServletRequest`, `JpaEntity`, `saveAndFlush` 같은 기술 세부가 아니라 `ReserveTicketCommand`, `Ticket`, `save`처럼 유스케이스에 필요한 업무 개념을 사용해야 한다. Inbound Port는 DDD Entity 자체가 아니라 Application의 유스케이스 경계다. Outbound Port도 Entity나 Value Object라기보다 Application Core가 외부에 요구하는 업무 목적의 계약이다. 따라서 Port가 도메인의 일부라는 말은 모든 Port가 Domain Model이라는 뜻이 아니라, 바깥 기술이 아닌 안쪽의 업무 목적에 따라 정의되고 안쪽이 소유한다는 뜻으로 이해하는 편이 정확하다.

### DIP로 의존 방향을 뒤집는다

앞선 N-Tiered 예시의 문제는 Service가 `JpaRepository`와 `HttpServletRequest`를 직접 알아야 한다는 점이었다. 업무 정책이 바깥 기술을 향해 소스 코드 의존성을 가졌기 때문에 기술 변경이 업무 코드와 테스트까지 전파됐다.

Robert C. Martin의 DIP는 상위 수준의 정책과 하위 수준의 세부 구현이 모두 추상화에 의존해야 한다고 설명한다. 헥사고날은 이 원리를 경계에 적용한다. Application Core가 필요한 Port를 안쪽에 정의하고, 바깥 Adapter가 그 Port에 의존하게 만든다. 다음 그림은 실행 순서가 아니라 import와 구현 관계를 포함한 소스 코드 의존성이다. 클래스명은 티켓 예약에 적용한 설명용 예시다.

```text
TicketReservationHttpAdapter =====> ReserveTicketUseCase
ReservationApplicationService ===> LoadTicketPort, SaveTicketPort
TicketJpaAdapter =================> LoadTicketPort, SaveTicketPort, Domain Ticket
```

`ReservationApplicationService`는 `TicketJpaAdapter`를 직접 import하지 않는다. 반대로 `TicketJpaAdapter`가 안쪽의 `SaveTicketPort`를 구현하고 Domain `Ticket`을 사용한다. 이것이 `의존성은 안쪽을 향한다`는 뜻이다. 런타임에는 Application Service가 주입된 JPA Adapter를 호출하지만, 실행 순서와 소스 코드 의존 방향은 서로 다른 개념이다.

### 앞선 비평이 어떻게 해결되는가

이 과제에서 지적한 기술 누수를 막기 위해 HTTP Adapter가 `HttpServletRequest`를 업무 값으로 변환하도록 배치할 수 있다. 그러면 Domain과 Application Service는 Servlet API를 모른다. JPA Adapter가 Domain Model과 JPA Entity 사이를 변환하게 하면 Domain의 상태와 행위가 영속성 생명주기나 지연 로딩을 직접 전제로 하지 않게 만들 수 있다.

그 결과 판매 중지 티켓을 거절하는 규칙은 HTTP 서버와 DB 없이 Domain 객체만으로 테스트할 수 있다. 저장까지 포함한 Application 테스트에는 `SaveTicketPort`의 In-memory 구현을 주입할 수 있다. 반면 JPA 매핑, 트랜잭션, 실제 쿼리는 JPA Adapter 통합 테스트에서 따로 검증해야 한다. 이는 Cockburn과 Martin의 원문에 특정된 구현 규칙이라기보다, 두 원칙을 이 과제의 테스트 비용 문제에 적용한 설계 선택이다.

## 리뷰 피드백 (Notion 원본)

> **피드백 메타데이터**
> - 출처 페이지: [Phase 1] 1-3(헥사고날) 제출 - 현웅님
> - URL: [Notion 원본 페이지](https://sponge-girdle-ad1.notion.site/Phase-1-1-3-3a26276f9e0081b399c3f614fe445fa7)
> - 수집 방법: 프로젝트 루트 `notion_mcp.md` 참조
> - 원문 보존: 댓글 본문은 Notion comment 레코드의 텍스트를 그대로 옮긴 것이며 일절 수정하지 않았다.
> - 라인 기준: 이 섹션 위쪽 본문의 라인 번호. 본문을 편집하면 다시 수집해야 한다.

리뷰어가 이 문서의 **어느 라인, 어떤 부분**에 **어떤 피드백**을 남겼는지 정리한 것이다.
총 3건 (댓글 2건, 리액션만 1건).

### FB-C1-01 · L20

- **위치**: L20
- **지적된 부분**: 하지만 책임의 분리가 곧 의존성의 분리를 뜻하지는 않는다. 상위 계층이 하위 계층을 향해 의존하는 구조에서 Service가 JPA Repository와 @Entity를 직접 사용하고 Controller까지 그 엔티티를 반환하면, 하나의 객체가 영속성 모델과 업무 모델, API 모델의 역할을 동시에 맡는다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-08-10 15:59 KST
- **피드백 원문**:

```
엔티티가 여러 계층의 역할을 동시에 수행할 때 발생하는 강 결합의 문제를 짚어주셨습니다. 그렇다면 만약 API 응답 모델, 업무 모델, 영속성 모델을 철저하게 모두 분리하여 클래스 개수와 매핑 코드가 기하급수적으로 늘어나는 상황이 오면 어떨까요? 완벽한 분리로 인해 얻는 이득이 더 큰가요? 그 임계점은 어디일까요? 
```

**답변 초안:**

완벽한 분리 자체를 목표로 두지는 않겠습니다. 세 모델이 서로 다른 이유로 바뀌는지가 먼저입니다. 외부에 공개된 API의 호환성, 도메인의 불변식, 저장 스키마가 독립적으로 변하거나 한 모델의 기술 타입이 다른 계층까지 새어 테스트와 변경을 방해할 때는 분리 편익이 큽니다. 반대로 내부용 단순 CRUD처럼 세 모델의 모양과 생명주기가 같고 독립 변경 사례도 없다면 한 모델을 유지하는 편이 더 경제적일 수 있습니다.

임계점은 클래스 개수보다 변경 비용으로 보겠습니다. 하나의 DB 변경이 API 계약과 업무 코드까지 반복해서 흔드는지, 단위 테스트가 영속성 문맥을 필요로 하는지, Mapper를 유지하는 비용보다 변경 전파와 장애 위험이 더 큰지를 확인하겠습니다. 공개 API는 장기 호환성 때문에 별도 응답 모델을 우선 검토하되, Domain과 Persistence 분리는 이런 증거가 있는 Aggregate부터 선택적으로 적용하겠습니다.

### FB-C1-02 · L75

- **위치**: L75
- **지적된 부분**: 문단 전체 — 도메인을 외부 의존성으로부터 보호하는 것도 이 방향의 연장선이다. 다만 "외부 의존성 0" 자체가 DDD의 정의는 아니다. DDD가 먼저 정하는 것은 무엇을 보호할 것인가이며, 그 대상은 업무 언어와 규칙을 담은 도메인 모델이다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-08-10 16:00 KST
- **피드백 원문**:

```
만약 우리가 보호해야 할 핵심 업무 규칙 자체가 우리가 통제할 수 없는 외부 서드파티 시스템(예: 외부 결제망의 복잡한 상태 전이 정책)의 제약과 강하게 결합되어 돌아가야만 의미가 있는 도메인이라면, 이때 우리는 내부 도메인 모델의 순수성과 외부 시스템의 현실성 중 어느 쪽을 기준으로 모델링을 시작해야 할까요?
```

**답변 초안:**

그 경우에는 외부 시스템이 실제로 제공하는 보장과 상태를 먼저 확인하겠습니다. 내부 모델의 순수성을 지키겠다고 외부에 없는 원자성이나 확정 상태를 만들어내면 도메인 모델이 현실을 설명하지 못하기 때문입니다. 다만 외부 PG의 상태 코드와 SDK 타입을 그대로 Core의 언어로 삼지는 않겠습니다. Adapter가 외부의 승인, 거절, 타임아웃을 내부의 `승인됨`, `실패`, `확인 중` 같은 업무 의미로 번역하고, Core는 그 상태에서 예약을 확정할지 대사할지를 결정하게 하겠습니다.

외부 제약 자체가 사업의 성립 조건이라면 그것도 도메인의 일부로 명시해야 합니다. 예를 들어 PG가 결과를 확정할 수 없는 구간이 실제로 존재한다면 `UNKNOWN`을 기술적 예외로 숨기지 않고 결제 시도의 정식 상태로 모델링해야 합니다. 현실에서 출발하되 그 현실을 우리 업무가 어떤 의미로 받아들이는지는 내부가 소유하는 방식이, 순수성과 현실성 사이의 경계를 가장 정직하게 유지한다고 생각합니다.

### FB-C1-03 · L75

- **위치**: L75
- **지적된 부분**: DDD가 먼저 정하는 것은 무엇을 보호할 것인가이며, 그 대상은 업무 언어와 규칙을 담은 도메인 모델이다.
- **유형**: 이모지 리액션 (댓글 본문 없음)
- **피드백 원문**: (없음 — 하이라이트에 리액션만 달렸다)

변경의 영향도 경계에 모인다. HTTP를 배치 입력으로 바꾸면 새 Inbound Adapter를 추가하고, JPA를 JDBC로 바꾸면 같은 Outbound Port를 구현하는 새 Adapter를 만들 수 있다. 다만 Port의 계약 자체가 바뀌거나 업무 규칙이 바뀌면 Core도 수정해야 한다. 헥사고날은 모든 변경을 없애는 방식이 아니라 기술 변경이 업무 정책으로 불필요하게 번지는 범위를 줄이는 방식이다.

### Trade-off와 적용 기준

헥사고날을 전면 적용하면 기술 경계와 테스트 대역이 명확해지지만 Port, Adapter, Domain Model과 영속성 모델 사이의 Mapper가 늘어난다. Port 이름만 업무 용어로 바꾸고 매개변수에 JPA Entity나 HTTP 타입을 남기면 기술 의존성도 그대로 남는다. 또한 헥사고날이 잘못된 도메인 모델을 자동으로 바로잡아 주지는 않는다.

기존 계층형 구조에 DIP와 기술 중립 DTO만 부분 적용하는 방법은 변경량이 적다. 대신 안쪽과 바깥쪽의 경계 규율이 전면 헥사고날보다 약해질 수 있다. 단순 CRUD이고 외부 기술의 변경 가능성이 낮다면 Controller, Service, Repository 구조가 가장 경제적일 수 있지만, 규칙이 복잡해질 때 기술 모델이 업무 모델로 번지는지 계속 확인해야 한다. 중요한 판단 기준은 헥사고날이라는 이름을 채택했는지가 아니라, 보호할 업무 규칙의 가치가 추가 경계와 매핑 비용보다 큰지다.

### 참고

- [Alistair Cockburn, Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture)
- [Robert C. Martin, The Dependency Inversion Principle](https://objectmentor.com/resources/articles/dip.pdf)

