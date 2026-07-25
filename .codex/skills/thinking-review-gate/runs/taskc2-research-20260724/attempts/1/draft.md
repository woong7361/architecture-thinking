# 조사 결론

작고 데이터 중심인 경계에서는 Domain 모델과 JPA Entity를 합치는 편이 대체로 경제적이다. 반대로 업무 규칙이 풍부하고, DB 스키마와 업무 모델이 다르며, 데이터 소스나 팀 경계가 여러 개인 곳에서는 분리의 매핑 비용이 보호 비용으로 바뀐다. 그러나 트래픽, 클래스 수, 회사 규모 하나만으로 결정할 수는 없다. 가장 현실적인 기본안은 전체 시스템을 한 번에 통일하지 않고 애그리거트별로 선택하는 것이다.

## 비유

통합은 주방 주문서 한 장을 조리와 회계에 함께 쓰는 방식이다. 작은 식당에서는 복사와 대조가 없어 빠르다. 분리는 조리용 주문서와 회계 전표를 따로 두고 둘 사이를 변환하는 방식이다. 기록은 두 벌이지만 조리법이 회계 양식에 끌려가지 않는다.

## 두 방식의 정의

- 통합: `@Entity class Ticket` 한 클래스가 `reserveBy()` 같은 업무 행위와 테이블, 연관관계 매핑을 함께 가진다.
- 분리: 순수 Java `Ticket`과 `TicketJpaEntity`를 따로 두고 persistence adapter의 mapper가 왕복 변환한다.

JPA Entity에 업무 메서드를 둘 수 없다는 말은 사실이 아니다. Jakarta Persistence 3.2는 entity를 persistent domain object로 설명하고 상태를 accessor 또는 business method로 제공할 수 있다고 명시한다. 다만 public/protected 기본 생성자, non-final 클래스와 메서드 같은 제약을 요구한다. https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2

## 장점과 비용

| 판단 축 | 통합 | 분리 |
| --- | --- | --- |
| 초기 개발 | 클래스와 필드가 한 벌이고 mapper가 없어 빠르다. | Domain, JPA Entity, mapper와 왕복 테스트가 추가된다. |
| 저장 동작 | JPA의 식별성, dirty checking, cascade, optimistic locking을 같은 객체에서 직접 쓴다. | Domain 객체의 변경을 JPA Entity에 반영하는 정책을 adapter가 책임져야 한다. |
| 도메인 표현 | rich model도 가능하지만 기본 생성자, proxy, lazy loading, 연관관계 매핑이 설계 선택에 개입한다. | 불변 객체, value object, 업무에 맞는 aggregate를 JPA 제약 없이 설계할 수 있다. |
| 변경 격리 | 테이블과 모델이 거의 1:1이면 중복 수정이 없다. 스키마나 ORM 변경이 Domain에 직접 닿는다. | DB 스키마, 외부 API, 캐시 모델이 달라도 adapter에서 흡수한다. Domain과 persistence 양쪽이 함께 바뀌면 mapper까지 세 곳을 고친다. |
| 테스트 | mapper 테스트가 없고 단순 CRUD 통합 테스트가 직선적이다. 업무 테스트가 proxy나 persistence context를 건드리면 JPA 통합 테스트가 늘 수 있다. | 업무 규칙은 plain object로 빠르게 검증할 수 있다. 대신 JPA adapter와 매핑 정확성은 별도 통합 테스트가 필요하다. |
| 성능·조회 | lazy/eager, entity graph, JPQL을 바로 활용하기 쉽다. 잘못된 연관관계 탐색이 N+1이나 과도한 로딩으로 이어질 수 있다. | 조회에 필요한 모양을 adapter가 명시할 수 있다. 큰 aggregate를 매번 전부 복원하거나 변경분 추적을 직접 구현하면 오히려 비싸다. |
| 주요 실패 | 영속성 생명주기와 기술 타입이 업무 코드와 API까지 퍼진다. | 필드 누락, enum 변환, 자식 ID, version, 삭제된 자식 처리의 매핑 버그가 생긴다. |

Spring의 canonical PetClinic은 `Owner` 하나에 `@Entity`, `@OneToMany`, `addPet()`, `addVisit()`를 함께 둔다. 작은 CRUD 중심 Spring 애플리케이션에서 통합이 얼마나 직선적인지 보여주는 구현 참고 사례다. 다만 production 규모 사례로 해석하면 안 된다. https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/owner/Owner.java

반대편 Java 구현 참고인 Buckpal은 `AccountJpaEntity`와 Domain `Account`를 나누고 `AccountMapper`로 변환한다. persistence adapter가 여러 쿼리 결과를 조립해 Domain Account를 만들며 새 Activity만 다시 JPA Entity로 변환해 저장한다. 이는 분리하면 단순 필드 복사뿐 아니라 로딩 범위와 변경 감지 정책도 adapter 책임이 된다는 점을 보여준다. https://github.com/thombergs/buckpal/blob/master/src/main/java/io/reflectoring/buckpal/adapter/out/persistence/AccountPersistenceAdapter.java

## 현장과 사이즈 기준

사이즈는 요청량이나 테이블 수 하나가 아니라 변경 표면으로 봐야 한다.

| 현장 형태 | 추천 출발점 | 이유 |
| --- | --- | --- |
| 한 팀, 한 관계형 DB, 테이블과 업무 객체가 거의 1:1, 대부분 CRUD | 통합 | mapper가 보호할 차이가 거의 없어서 중복 비용이 더 크다. |
| 한 bounded context, 한 DB지만 핵심 aggregate에 상태 전이와 불변식이 많음 | 선택적 분리 | 단순 기준정보는 통합하고 핵심 aggregate만 분리하면 비용을 집중할 수 있다. JPA 제약이 도메인 표현을 실제로 방해하지 않으면 rich JPA entity도 가능한 대안이다. |
| legacy schema와 업무 모델이 다름, 여러 DB·외부 API·프로토콜을 같은 업무 개념 뒤에서 교체, 여러 팀이 경계를 소유 | 분리 | 하나의 저장 모델을 공유할 때 생기는 변경 전파가 mapper 비용보다 커질 가능성이 높다. |
| 읽기 비중이 높고 화면별 조회 모양이 크게 다름 | 쓰기 Domain은 선택적 분리, 읽기는 projection/CQRS 검토 | Domain aggregate를 모든 조회에 재사용하면 불필요한 복원과 join 비용이 커질 수 있다. |

Microsoft의 DDD microservice 가이드도 하나의 회사 안에서 ordering처럼 변화하는 규칙이 많은 서비스에는 rich DDD를 쓰고, catalog 같은 단순 CRUD에는 단순 모델을 쓰는 다중 아키텍처를 설명한다. 이는 시스템 전체 크기보다 bounded context의 규칙 복잡도가 기준이라는 근거다. https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/microservice-domain-model

Netflix Studio Workflows 사례는 분리 쪽의 실제 조건을 잘 보여준다. 새 앱은 여러 업무 도메인을 가로질렀고 영화, 일정, 직원, 장소 데이터를 gRPC, JSON API, GraphQL 등 여러 시스템에서 받아야 했다. 이전 모놀리스는 한때 30명 넘는 개발자와 300개 넘는 테이블 규모였다. 팀은 저장 위치를 모르는 Entity와 repository interface를 두었고, 실제로 한 Entity의 읽기 소스를 JSON API에서 GraphQL로 약 2시간 만에 전환했다. 여기서 분리의 가치는 대기업이라는 이름이 아니라 데이터 소스 교체가 예정되어 있고 업무 규칙을 보호해야 했다는 데 있다. https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749

대형 시스템이라고 처음부터 전면 분리해야 한다는 반례도 있다. Shopify의 Rails/Active Record 사례는 JPA 사례는 아니지만 같은 객체가 저장 모델 역할까지 맡는 구조의 현장 유사 사례다. 280만 줄, 50만 커밋, 수백 명 개발자 규모에서 Active Record model을 어디서나 공유하는 관례가 component 경계를 깨는 문제로 나타났다. 그러나 Shopify는 전면 재작성하지 않고 소유권과 경계를 점진적으로 강제했다. 규모가 커졌다는 사실보다 모델을 여러 팀과 도메인이 공유한다는 것이 문제였고, 기존 시스템에서는 전면 분리의 마이그레이션 비용도 크다는 점을 보여준다. https://shopify.engineering/shopify-monolith

## 예시: 티켓 예약

통합하면 다음처럼 한 객체에서 규칙과 저장 상태를 관리한다.

```java
@Entity
class Ticket {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    protected Ticket() {}

    void reserveBy(UserId userId) {
        if (status != TicketStatus.ON_SALE) {
            throw new TicketCannotBeReserved();
        }
        status = TicketStatus.RESERVED;
    }
}
```

예약 규칙이 몇 개이고 `tickets` 테이블과 객체가 거의 같다면 이것이 가장 싸다. 트랜잭션 안에서 조회한 객체의 상태를 바꾸면 dirty checking이 저장을 담당한다.

분리하면 Domain `Ticket`에는 `@Entity`가 없고 adapter가 다음을 변환한다.

```text
TicketJpaEntity(id=10, status_code="S", version=7)
             ↓ toDomain
Ticket(TicketId(10), TicketStatus.ON_SALE)
             ↓ reserveBy
Ticket(TicketId(10), TicketStatus.RESERVED)
             ↓ toJpa
TicketJpaEntity(id=10, status_code="R", version=7)
```

legacy DB가 `S/R` 코드를 요구하고 Domain은 의미 있는 enum을 원하면 이 변환은 차이를 흡수하므로 가치가 있다. 반면 mapper가 `version=7`을 빼먹으면 optimistic locking이 깨질 수 있고, 좌석 자식들의 기존 ID나 삭제 여부를 잃으면 불필요한 INSERT/DELETE가 생길 수 있다. 분리의 실제 비용은 클래스 수가 두 배라는 사실보다 이런 identity, version, 관계, 부분 로딩의 왕복 의미를 보존하는 일이다.

## 실무 판단용 체크

다음 질문 중 둘 이상이 반복해서 예라면 분리 검토 가치가 커진다는 휴리스틱을 쓸 수 있다. 이는 업계 표준 임계값이 아니라 의사결정 보조 기준이다.

1. JPA 제약 때문에 도메인 생성 규칙, 불변성, aggregate 경계를 양보하고 있는가?
2. 같은 업무 개념의 모양이 테이블, 외부 API, Domain에서 실제로 다른가?
3. 저장 기술이나 데이터 소스 교체가 막연한 가능성이 아니라 계획되었거나 반복된 적이 있는가?
4. JPA 없이 업무 규칙을 검증하려 해도 proxy, lazy loading, transaction이 끼어드는가?
5. 서로 다른 팀이나 bounded context가 같은 persistence entity를 직접 공유하는가?

반대로 mapper 대부분이 같은 이름의 필드를 1:1 복사하고, 업무 행위가 거의 없고, 저장소도 하나라면 통합을 유지하는 편이 낫다. Allegro의 엔지니어링 예시도 `Article`과 `ArticleEntity`가 같은 필드를 복제하기만 하는 경우 헥사고날이 과잉 설계처럼 보인다고 지적한다. https://blog.allegro.tech/2020/05/hexagonal-architecture-by-example.html

공개 자료에는 보편적인 수치 임계값이 없다. 위 사이즈 표는 공식 문서와 공개 현장 사례에서 추출한 휴리스틱이며, 다음 단계에서는 이 기준을 현재 티켓 도메인에 대입해 수행내용 2의 개인 기준으로 좁힐 수 있다.
