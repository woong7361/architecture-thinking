# Original User Input

Aggregate, domain event는 잘 모르겠어 entity, value object는 이해가 가는데 설명해줘봐 그리고 ddd에서 어떻게 쓰이는지도, 그리고 지양해야할때는 언제인지도 알려줘


# Checked Context

# Project Context

- 사용자는 Java와 Spring을 선호하는 백엔드 개발자다.
- `task3/assignments/taskC-1.md`는 DDD의 전술 패턴을 필요한 만큼 사용하고 복잡한 도메인에 집중한다고 설명한다.
- 기존 과제 예시는 `Ticket`이 예약 불변식을 지키는 Rich Domain Model을 사용한다.
- 사용자는 Entity와 Value Object는 이해하지만 Aggregate와 Domain Event의 개념, DDD에서의 사용법, 지양 조건을 알고 싶어 한다.

# Evidence Anchors

- Eric Evans, DDD Reference, Aggregate: https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf#page=29
  - Entity와 Value Object를 Aggregate로 묶고 Root를 하나 정한다.
  - Aggregate 내부 규칙은 동기적으로, 경계 밖 갱신은 비동기적으로 다룬다.
- Eric Evans, DDD Reference, Domain Events: https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf#page=26
  - 도메인에서 일어났고 도메인 실무자가 관심을 갖는 일을 불변 객체로 모델링한다.
- Microsoft Learn, Tactical DDD: https://learn.microsoft.com/en-us/azure/architecture/microservices/model/tactical-domain-driven-design
  - Aggregate를 트랜잭션 불변식의 일관성 경계로 설명한다.
  - 작은 Aggregate, 다른 Aggregate는 ID로 참조, 경계 간 최종 일관성을 권한다.
- Microsoft Learn, Domain events: https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/domain-events-design-implementation
  - Domain Event는 같은 도메인 안에서 Aggregate 간 부수 효과를 명시적으로 전달한다.
  - Integration Event와 구분하고 지연 발행 방식을 설명한다.

# Constraints

- 쉬운 비유로 시작한다.
- 정의와 핵심 개념, 비교와 trade-off, Java 예시, 지양 조건, 레퍼런스 순서로 설명한다.
- Aggregate를 단순 객체 그래프나 ORM 연관관계로 설명하지 않는다.
- Domain Event를 Kafka 메시지 또는 Event Sourcing과 동일시하지 않는다.
- 즉시 지켜야 할 Aggregate 내부 불변식을 Event Handler로 미루지 않는다.
- 단순 CRUD에 전술 패턴을 강제하지 않는다.
