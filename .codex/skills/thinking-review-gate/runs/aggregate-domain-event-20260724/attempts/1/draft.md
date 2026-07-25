# 먼저 한 문장으로

- **Aggregate**는 함께 저장되는 객체 묶음이 아니라, **한 번의 상태 변경에서 반드시 함께 일관성을 지켜야 하는 최소 경계**다.
- **Domain Event**는 그 경계 안에서 상태 변경이 성공한 뒤, **도메인에서 의미 있는 일이 이미 일어났음을 나타내는 불변의 사실**이다.

## 1. 알기 쉬운 비유

식당의 한 테이블 주문서를 생각해 보자.

- `Order`는 주문서 자체다. 주문 번호가 있으므로 Entity다.
- `OrderLine`은 주문서 안의 각 메뉴 항목이다. 같은 메뉴라도 주문 항목 번호가 다르면 별개의 항목이므로 Entity로 볼 수 있다.
- `Money`는 금액이라는 값으로 비교하므로 Value Object다.
- 주문서에 메뉴를 추가하거나 결제를 확정할 때는 직원이 주문 항목을 따로 고치지 않고 주문서를 통해 처리한다. 주문서가 수량, 합계, 결제 상태 같은 규칙을 한꺼번에 지킨다. 이 주문서와 내부 항목을 둘러싼 규칙 경계가 Aggregate이고, 주문서가 Aggregate Root다.
- 결제가 성공하면 주방이나 영수증 발급 쪽에서 알아야 할 수 있다. 이때 울리는 `OrderPaid`라는 알림이 Domain Event다. “결제하라”는 명령이 아니라 “주문이 결제되었다”는 과거의 사실이다.

비유에서 중요한 점은 주문서가 단순히 메뉴 항목을 담는 폴더가 아니라는 것이다. 주문서가 없으면 내부 항목을 마음대로 고칠 수 없고, 주문서가 전체 규칙을 책임진다.

## 2. 정의와 핵심 개념

### 네 개념의 관계

| 개념 | 판단 기준 | 주문 예시 | 핵심 질문 |
| --- | --- | --- | --- |
| Entity | 시간이 지나도 추적할 고유한 정체성이 있는가 | `Order`, `OrderLine` | 같은 대상인가? |
| Value Object | 정체성 없이 값으로 동등성을 판단하는가 | `Money` | 같은 값인가? |
| Aggregate | 한 트랜잭션에서 반드시 함께 지킬 규칙의 경계인가 | `Order`와 내부 `OrderLine` | 무엇이 동시에 일관돼야 하는가? |
| Domain Event | 도메인에서 이미 일어난 중요한 사실인가 | `OrderPaid` | 무슨 일이 일어났는가? |

### Aggregate

Aggregate는 하나 이상의 Entity와 Value Object를 일관성 규칙에 따라 묶은 경계다. 하나의 Entity만으로도 Aggregate가 될 수 있다. 객체 수보다 **트랜잭션 불변식**이 경계를 결정한다.

여기서 불변식은 상태 변경이 끝났을 때 반드시 참이어야 하는 업무 규칙이다. 주문 예에서는 다음이 해당한다.

- 결제된 주문에는 항목을 추가할 수 없다.
- 빈 주문은 결제할 수 없다.
- 주문 총액은 주문 항목 금액의 합과 같다.

Aggregate에는 외부 진입점인 **Aggregate Root**가 하나 있다. 외부 코드는 내부 `OrderLine`을 직접 저장하거나 변경하지 않고 `Order.addLine()`이나 `Order.pay()`를 호출한다. Repository도 보통 Root 단위로 조회하고 저장한다. 다른 Aggregate는 객체 전체를 직접 연결하기보다 ID로 참조해 경계를 넘는 변경을 피한다. Eric Evans의 DDD Reference도 Root만 외부에서 참조하고, Aggregate 경계를 트랜잭션과 일관성 경계로 사용하도록 설명한다. [Eric Evans, DDD Reference](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf#page=29)

### Domain Event

Domain Event는 도메인에서 일어난 일 가운데 업무적으로 추적하거나 반응할 가치가 있는 사실이다. 보통 과거형으로 이름 짓고 생성 후에는 바꾸지 않는다.

- 좋은 예: `OrderPaid`, `TicketReserved`, `DeliveryCancelled`
- 나쁜 예: `OrderRowUpdated`, `SaveCompleted`, `ButtonClicked`

뒤의 예들은 구현이나 UI에서 일어난 기술적 사건이지 도메인 언어로 표현한 업무 사실이 아니다. Eric Evans는 Domain Event를 도메인 실무자가 관심을 갖는 과거의 사건으로 설명하며, 보통 불변 객체로 다룬다. [Eric Evans, DDD Reference](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf#page=26)

Domain Event는 Kafka 메시지와 같은 뜻이 아니다. 같은 프로세스 안에서 전달할 수도 있고, 단지 Aggregate 내부의 이벤트 목록에 기록했다가 트랜잭션 직전이나 직후에 처리할 수도 있다. 또한 Domain Event를 쓴다고 Event Sourcing을 해야 하는 것도 아니다.

외부 서비스나 다른 Bounded Context로 보내는 메시지는 보통 **Integration Event**로 구분한다. Domain Event는 현재 도메인 모델의 의미를 표현하고, Integration Event는 커밋된 결과를 외부에 안정적으로 전달하기 위한 공개 계약이다. 외부 전달에는 재시도, 중복 처리, 메시지 유실을 고려해 Outbox 같은 별도 신뢰성 장치가 필요하다. [Microsoft Learn, Domain events](https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/domain-events-design-implementation)

## 3. DDD에서 둘을 어떻게 사용하는가

일반적인 흐름은 다음과 같다.

```text
Application Service
    │  Aggregate Root 조회
    ▼
Order.pay()
    │  Aggregate 내부 불변식을 즉시 검사
    │  상태를 PAID로 변경
    ▼
OrderPaid 기록
    │
    ├─ Order Aggregate 저장
    └─ 커밋 전후에 Event Handler 실행
           ├─ 영수증 생성
           ├─ 별도 Aggregate 갱신
           └─ 필요하면 Integration Event를 Outbox에 기록
```

역할을 분리하면 다음과 같다.

1. Application Service가 유스케이스와 트랜잭션을 시작한다.
2. Aggregate Root의 메서드가 현재 상태와 입력을 검사해 불변식을 지킨다.
3. 상태 변경에 성공한 Aggregate가 Domain Event를 기록한다.
4. Repository가 Aggregate를 하나의 단위로 저장한다.
5. Event Handler가 Aggregate 바깥의 후속 반응을 처리한다.

Aggregate 내부에서 즉시 참이어야 하는 규칙을 Domain Event Handler에 맡기면 안 된다. 예를 들어 빈 주문의 결제를 거부하는 판단은 `Order.pay()` 안에서 즉시 해야 한다. `OrderPaid`가 발생한 뒤 Handler에서 빈 주문인지 검사하면 이미 잘못된 상태 변경을 허용한 셈이다.

반대로 주문 결제 후 고객의 누적 구매액을 갱신하는 일처럼 별도 Aggregate가 책임지는 후속 반응은 Domain Event 후보가 된다. Aggregate 간에는 독립적인 생명주기와 트랜잭션이 있으므로, 이벤트를 통해 최종 일관성으로 연결할 수 있다. Microsoft의 Tactical DDD 문서도 Aggregate를 트랜잭션 일관성 경계로 설명하고, 경계 사이의 작업에는 Domain Event와 최종 일관성을 사용하도록 안내한다. [Microsoft Learn, Tactical DDD](https://learn.microsoft.com/en-us/azure/architecture/microservices/model/tactical-domain-driven-design)

## 4. 구체적인 Java 예시

아래 코드는 `Order`가 Aggregate Root이고, `OrderLine`이 내부 Entity, `Money`가 Value Object, `OrderPaid`가 Domain Event인 예다.

```java
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Order {
    private final OrderId id;
    private final List<OrderLine> lines = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    public Order(OrderId id) {
        this.id = Objects.requireNonNull(id);
    }

    public void addLine(
            OrderLineId lineId,
            ProductId productId,
            Money unitPrice,
            int quantity
    ) {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("결제된 주문은 변경할 수 없다");
        }
        lines.add(new OrderLine(lineId, productId, unitPrice, quantity));
    }

    public void pay(Instant paidAt) {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제된 주문이다");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("빈 주문은 결제할 수 없다");
        }

        status = OrderStatus.PAID;
        domainEvents.add(new OrderPaid(id, total(), paidAt));
    }

    public Money total() {
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.ZERO, Money::add);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copied = List.copyOf(domainEvents);
        domainEvents.clear();
        return copied;
    }
}

record OrderId(UUID value) {
    OrderId {
        Objects.requireNonNull(value);
    }
}

record OrderLineId(UUID value) {
    OrderLineId {
        Objects.requireNonNull(value);
    }
}

record ProductId(UUID value) {
    ProductId {
        Objects.requireNonNull(value);
    }
}

record Money(BigDecimal amount) {
    static final Money ZERO = new Money(BigDecimal.ZERO);

    Money {
        Objects.requireNonNull(amount);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없다");
        }
    }

    Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    Money multiply(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 한다");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }
}

final class OrderLine {
    private final OrderLineId id;
    private final ProductId productId;
    private final Money unitPrice;
    private final int quantity;

    OrderLine(OrderLineId id, ProductId productId, Money unitPrice, int quantity) {
        this.id = Objects.requireNonNull(id);
        this.productId = Objects.requireNonNull(productId);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 한다");
        }
        this.quantity = quantity;
    }

    Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}

sealed interface DomainEvent permits OrderPaid {
}

record OrderPaid(OrderId orderId, Money total, Instant occurredAt)
        implements DomainEvent {
    OrderPaid {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(total);
        Objects.requireNonNull(occurredAt);
    }
}

enum OrderStatus {
    DRAFT,
    PAID
}
```

여기서 중요한 지점은 다음과 같다.

- 외부 코드는 `OrderLine`을 직접 추가하거나 저장하지 않고 `Order.addLine()`을 거친다.
- `Order.pay()`가 빈 주문과 중복 결제를 즉시 차단한다.
- `OrderPaid`는 결제를 수행하는 객체가 아니라 성공한 결제 사실을 담는다.
- 이벤트 발행 기술이나 Handler는 `Order` 안에 들어오지 않는다. Aggregate는 이벤트를 기록하고, Application 또는 Infrastructure 계층이 커밋 시점에 꺼내 전달한다.

## 5. 비교와 trade-off

### Aggregate 크기

| 선택 | 장점 | 비용과 위험 |
| --- | --- | --- |
| 큰 Aggregate | 여러 규칙을 한 트랜잭션으로 지키기 쉽다 | 로딩 범위, 잠금 충돌, 동시 수정 실패가 커진다 |
| 작은 Aggregate | 독립 변경과 확장이 쉽고 경쟁이 줄어든다 | Aggregate 간 최종 일관성, 재시도와 보상 처리가 필요하다 |

추천 기준은 “관계가 있으니 묶는다”가 아니라 **잠깐이라도 깨져서는 안 되는 규칙을 지키는 데 필요한 최소 데이터만 묶는다**이다.

예를 들어 주문 총액과 주문 항목 합계는 항상 일치해야 하므로 같은 Aggregate가 자연스럽다. 반면 주문 결제와 고객의 누적 포인트가 잠시 어긋나도 복구 가능한 정책이라면 `Order`와 `Customer`를 분리하고 `OrderPaid`로 연결할 수 있다.

### 직접 호출과 Domain Event

| 선택 | 적합한 경우 | trade-off |
| --- | --- | --- |
| Aggregate 메서드 내부 직접 처리 | 같은 Aggregate의 즉시 불변식 | 가장 명시적이고 원자적이지만 경계 밖 책임을 넣으면 비대해진다 |
| 같은 Bounded Context의 Domain Event | 별도 Aggregate나 여러 후속 반응 | 결합을 줄이지만 실행 순서, 실패, 디버깅이 복잡해진다 |
| Integration Event | 다른 Bounded Context나 외부 서비스 | 배포 독립성이 생기지만 중복, 지연, 유실, 스키마 호환을 다뤄야 한다 |

## 6. 언제 지양해야 하는가

### Aggregate를 과하게 쓰지 말아야 할 때

- 단순 CRUD이고 함께 지켜야 할 업무 불변식이 거의 없다. Entity 하나를 저장하는 것으로 충분하면 복잡한 Root 계층이나 전용 Factory를 만들 필요가 없다.
- DB 외래 키나 객체 연관관계가 있다는 이유만으로 모두 하나의 Aggregate에 넣으려 한다. 관계가 아니라 트랜잭션 규칙이 경계를 결정한다.
- 조회 화면을 만들기 위해 거대한 Aggregate 전체를 로딩한다. 복잡한 조회는 전용 Query, DTO, Projection으로 해결하는 편이 낫다.
- 내부 Entity마다 Repository를 만들어 Root를 우회한다. 이 방식은 Aggregate가 지켜야 할 규칙을 건너뛸 수 있다.
- 한 Aggregate를 너무 크게 만들어 서로 무관한 변경이 같은 잠금과 버전 충돌을 공유한다.

단, 단순 CRUD의 Entity 하나도 개념적으로는 Aggregate Root일 수 있다. 지양해야 하는 것은 명칭이 아니라, 이득 없이 복잡한 객체 묶음과 계층을 추가하는 일이다.

### Domain Event를 지양해야 할 때

- 같은 Aggregate 안에서 즉시 지켜야 하는 규칙을 Handler로 미룬다.
- 아무도 반응하지 않고 업무적으로 기록할 가치도 없는 사건을 “나중을 위해” 전부 발행한다.
- `EntityUpdated`, `RepositorySaved`처럼 기술적 변경을 도메인 사건으로 포장한다.
- 호출 대상이 하나이고 흐름도 안정적인데 단순 메서드 호출보다 Event Bus를 써서 제어 흐름만 숨긴다.
- 트랜잭션 커밋 전에 외부 메시지를 발행해 DB 저장은 실패했는데 외부에서는 성공한 것으로 보이게 한다.
- 재시도와 중복 가능성이 있는데 Handler를 멱등하게 만들지 않는다.
- Domain Event, Integration Event, Event Sourcing을 같은 것으로 취급한다.

Domain Event는 결합을 없애는 마법이 아니다. 호출 결합을 시간, 순서, 실패 처리에 대한 결합으로 바꾸는 도구다. 후속 반응이 하나뿐이고 동기적으로 반드시 성공해야 하며 같은 책임 안에 있다면 직접 호출이 더 읽기 쉽다.

## 7. 실무에서 경계를 찾는 질문

다음 순서로 물으면 된다.

1. 상태 변경이 끝나는 순간 반드시 참이어야 하는 규칙은 무엇인가?
2. 그 규칙을 판단하는 데 필요한 최소 Entity와 Value Object는 무엇인가?
3. 그 객체들을 통제할 Root는 누구인가?
4. 다른 Aggregate와 잠시 불일치해도 되는가?
5. 상태 변경 후 다른 책임이 알아야 할 도메인 사실이 있는가?

1~3의 답이 Aggregate를 만들고, 4~5의 답이 Domain Event가 필요한지 결정한다.

## 레퍼런스

- Eric Evans, [DDD Reference](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf), Domain Events와 Aggregates
- Microsoft Learn, [Use Tactical DDD to Design Microservices](https://learn.microsoft.com/en-us/azure/architecture/microservices/model/tactical-domain-driven-design)
- Microsoft Learn, [Domain events: Design and implementation](https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/domain-events-design-implementation)

구현 방식에는 팀별 선택지가 있다. 특히 Domain Event를 커밋 전 같은 트랜잭션에서 처리할지, 커밋 후 별도 트랜잭션에서 처리할지는 필요한 일관성과 실패 복구 정책에 따라 결정해야 한다.
