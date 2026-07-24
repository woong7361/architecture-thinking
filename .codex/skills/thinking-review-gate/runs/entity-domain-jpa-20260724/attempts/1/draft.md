# 결론

지적이 맞다. 내가 `Order는 Aggregate Root이면서 Entity`라고만 써서 **DDD Entity**와 **JPA `@Entity`**를 구분하지 못하게 만들었다.

정확한 표현은 다음과 같다.

> `Order`는 Domain Model에 속하는 Aggregate Root이며, DDD 분류로는 정체성을 가진 Entity다. 이 Domain Model을 JPA Entity와 같은 클래스로 영속화할지는 별도의 구현 선택이다.

따라서 `Entity로 갈지 Domain으로 갈지`는 양자택일이 아니다.

- Domain은 업무 모델이 존재하는 영역이다.
- Entity는 그 Domain Model을 구성하는 객체의 한 종류다.
- JPA `@Entity`는 그 객체를 ORM으로 영속화하기 위한 기술 역할이다.

직전 코드의 `Order`에는 `@Entity`가 없었으므로 의도상 **순수 Domain Entity**였다. 패키지로 표현하면 `domain.order.Order`에 두는 쪽이다.

## 비유

Domain Entity는 실제 업무에서 사용하는 주문서다. JPA Entity는 그 주문서를 데이터베이스 서식에 맞게 보관하는 방식이다.

같은 종이 한 장에 업무 규칙과 보관용 칸을 함께 넣을 수도 있다. 작성과 보관이 단순해진다. 하지만 DB 서식이 바뀌면 업무 문서도 같이 바뀔 수 있다.

업무 주문서와 DB 보관 서식을 나누면 업무 규칙은 깨끗하게 유지된다. 대신 두 서식 사이를 옮겨 적는 Mapper가 필요하다.

## 용어를 먼저 분리해야 한다

| 용어 | 소속 | 의미 |
| --- | --- | --- |
| DDD Entity | Domain Model | 정체성과 생명주기를 가진 업무 객체 |
| Aggregate Root | Domain Model | Aggregate 외부 요청을 받으며 불변식을 보호하는 DDD Entity |
| JPA `@Entity` | Persistence 기술 | ORM이 테이블과 매핑하고 Persistence Context에서 관리하는 객체 |

Aggregate Root는 DDD Entity의 특별한 역할이다. 모든 DDD Entity가 Root는 아니지만 모든 Aggregate Root는 정체성을 가진 Entity다. Eric Evans의 DDD Reference는 Entity를 정체성과 생명주기로 설명하고, Aggregate에서는 하나의 Entity를 Root로 선택한다. [Eric Evans, DDD Reference](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)

Jakarta Persistence도 JPA Entity를 `persistent domain object`라고 정의한다. 그러므로 Domain 객체와 JPA 객체를 같은 클래스로 쓰는 것 자체가 규격 위반이나 항상 잘못된 설계는 아니다. 다만 JPA Entity에는 인자 없는 생성자, non-final 클래스 같은 영속성 제약이 생긴다. [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.pdf)

## 선택지 1. 순수 Domain Model과 JPA Entity를 분리한다

복잡한 업무 규칙이 핵심이라면 이 방식을 우선 검토할 수 있다.

```text
domain/order/
  Order.java                 ← Aggregate Root, DDD Entity
  OrderLine.java             ← 내부 DDD Entity
  Money.java                 ← Value Object
  OrderPaid.java             ← Domain Event
  OrderRepository.java       ← Domain이 요구하는 Port

infrastructure/persistence/order/
  OrderJpaEntity.java        ← JPA 영속성 모델
  OrderLineJpaEntity.java
  SpringDataOrderRepository.java
  OrderPersistenceAdapter.java
  OrderMapper.java
```

의존 방향은 다음과 같다.

```text
Infrastructure/JPA ───────▶ Domain
                          OrderRepository 구현

Domain ──X──▶ JPA, Hibernate, Spring Data
```

장점:

- Domain 테스트에서 Persistence Context, Proxy, Lazy Loading을 고려하지 않아도 된다.
- 테이블 정규화나 조회 최적화 때문에 JPA 모델이 바뀌어도 업무 모델의 형태를 유지할 수 있다.
- Domain 객체를 불변에 가깝게 만들고 생성 규칙을 자유롭게 설계할 수 있다.
- JPA Entity가 API 응답까지 새는 것을 구조적으로 막기 쉽다.

비용:

- Domain과 JPA 객체 사이 Mapper가 필요하다.
- 필드가 중복되고 저장·복원 코드가 늘어난다.
- Aggregate가 크면 매핑 비용과 변경 동기화 비용이 커진다.
- 단순 CRUD에서는 얻는 이점보다 보일러플레이트가 더 클 수 있다.

## 선택지 2. Domain Entity가 JPA Entity를 겸한다

다음과 같은 형태다.

```text
domain/order/
  Order.java        ← Aggregate Root + DDD Entity + @Entity
  OrderLine.java    ← 내부 DDD Entity + @Entity
  Money.java        ← Value Object + @Embeddable
```

장점:

- 별도 영속성 모델과 Mapper가 필요하지 않다.
- Repository에서 읽은 객체에 바로 도메인 행위를 수행할 수 있다.
- Spring Data JPA와 Dirty Checking을 활용하기 쉽다.
- 중간 정도 복잡도의 일반적인 업무 시스템에서 구현량을 크게 줄일 수 있다.

비용:

- 인자 없는 생성자와 Proxy를 위한 non-final 같은 JPA 요구가 Domain 설계에 들어온다.
- Lazy Collection을 Domain 메서드가 접근하면 테스트와 실제 실행의 동작이 달라질 수 있다.
- `equals()`와 `hashCode()`, 생성 ID, Managed와 Detached 상태를 함께 고려해야 한다.
- 테이블과 연관관계 매핑 요구가 Aggregate 경계를 끌고 갈 위험이 있다.
- Controller까지 같은 객체를 반환하면 API, Domain, Persistence 역할이 하나로 합쳐진다.

Hibernate는 Application Domain Model을 직접 영속화할 수 있지만 그 객체에 Persistence Context, Proxy, Lazy Loading 같은 실행 의미를 추가한다. 따라서 핵심은 `@Entity` 어노테이션 한 줄보다 **Domain 행위가 열린 Persistence Context나 Lazy Loading에 의존하는지**다. [Hibernate ORM User Guide](https://docs.hibernate.org/orm/7.0/userguide/html_single/)

이 방식을 선택하더라도 다음 경계는 지키는 편이 좋다.

- Controller에 JPA Entity를 직접 반환하지 않는다.
- Domain에서 `EntityManager`나 Spring Data Repository를 직접 사용하지 않는다.
- 연관관계 편의보다 Aggregate 불변식으로 객체 경계를 정한다.
- Public Setter 대신 의미 있는 Domain 메서드를 둔다.
- Lazy Loading이 Domain 규칙 실행의 숨은 전제가 되지 않게 한다.

## 선택지 3. 단순 CRUD 모델로 제한한다

업무 규칙이 거의 없고 데이터 입력과 조회가 중심이라면 Rich Domain Model을 만들지 않는 편이 나을 수 있다.

```text
application/
  ProductCrudService.java

infrastructure/persistence/
  ProductJpaEntity.java
  ProductJpaRepository.java
```

장점:

- 구조가 단순하고 개발 속도가 빠르다.
- Mapper와 중복 모델이 필요하지 않다.
- 관리 화면이나 기준 정보 CRUD에 적합하다.

비용:

- 업무 규칙이 늘어나면 Service와 Entity 여러 곳으로 흩어질 수 있다.
- 복잡도가 높아진 뒤 Domain Model로 옮기려면 경계를 다시 찾아야 한다.

## 어떤 것을 선택할까

현재 과제의 방향이라면 다음 기준이 가장 일관된다.

| 상황 | 추천 |
| --- | --- |
| 핵심 업무 규칙이 복잡하고 DB 구조가 자주 모델을 왜곡한다 | Domain과 JPA 모델 분리 |
| 업무 규칙은 있지만 JPA 매핑이 Domain 형태를 크게 해치지 않는다 | Domain Entity와 JPA Entity 겸용 |
| 단순 CRUD와 관리 기능이 중심이다 | JPA 중심 모델, Rich Domain Model 생략 |

이 프로젝트의 `taskC-1.md`는 JPA 어노테이션 자체를 문제로 삼지 않는다. Service와 Controller가 JPA Entity의 필드 구조, Lazy Loading, Managed 상태에 의존해 Domain 정책이 영속성 기술 쪽으로 끌려가는 것을 문제로 본다.

따라서 판단 질문은 `@Entity가 Domain에 있는가` 하나로 끝나지 않는다.

1. Domain 규칙을 JPA 없이 단위 테스트할 수 있는가?
2. Domain 메서드가 Lazy Loading이나 Persistence Context를 암묵적으로 요구하는가?
3. 테이블 구조 변경이 업무 용어와 객체 경계를 자주 바꾸는가?
4. Mapper 비용보다 Domain 보호의 가치가 큰가?
5. JPA Entity가 API 모델까지 겸하고 있는가?

1번이 가능하고 2~3번이 거의 없다면 한 클래스 겸용도 실용적이다. 2~3번 문제가 반복되고 4번의 가치가 크다면 분리가 낫다.

## 앞선 표현의 수정

앞선 답변의 다음 표현은 모호했다.

> `Order`: Aggregate Root이면서 Entity

다음처럼 고치는 것이 정확하다.

> `Order`: Domain Model에 속하는 Aggregate Root이며 DDD Entity다. 예제에서는 JPA와 분리된 순수 Domain 객체다.

즉, 사용자의 지적대로 **Domain으로 가는 것이 먼저**다. 그 Domain 객체를 JPA Entity와 겸용할지는 도메인 복잡도, Persistence 누수, Mapper 비용을 비교해 결정한다.

## 레퍼런스

- Eric Evans, [DDD Reference](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)
- Jakarta EE, [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.pdf)
- Hibernate, [Hibernate ORM User Guide](https://docs.hibernate.org/orm/7.0/userguide/html_single/)
- Spring Data JPA, [Core Concepts](https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html)

Spring Data 문서는 일부 문맥에서 Entity, Domain Type, Aggregate를 교환해 사용한다. 프레임워크 문서의 용어와 DDD 모델링 용어를 구분해서 읽어야 한다.
