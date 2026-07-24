# Task C-2 제출물 초안

## 답안: Domain Entity와 JPA Entity의 분리 판단

Domain Entity와 JPA Entity를 합치면 하나의 클래스가 업무 상태와 행위, 테이블 매핑을 함께 담당한다. 반대로 분리하면 순수 Java Domain Entity와 `@Entity`가 붙은 Persistence Entity를 따로 두고 Persistence Adapter의 Mapper가 두 모델을 변환한다. 어느 방식이 항상 우월한 것은 아니다. 핵심은 보호할 업무 규칙을 JPA로부터 격리해서 얻는 편익과 Mapper가 추가하는 비용을 비교하는 것이다.

### 통합과 분리의 현실적 Trade-off

| 판단 항목 | 통합 | 분리 |
| --- | --- | --- |
| 초기 개발 | 클래스와 필드가 한 벌이고 Mapper가 없어 빠르다. | Domain Entity, JPA Entity, Mapper와 매핑 테스트가 추가된다. |
| 업무 모델 | JPA Entity에도 업무 메서드를 둘 수 있어 Rich Domain Model을 만들 수 있다. 다만 기본 생성자, Proxy, Lazy Loading과 연관관계 매핑이 모델 설계에 개입할 수 있다. | 불변 객체와 Value Object, Aggregate 경계를 JPA 제약 없이 표현할 수 있다. |
| 테스트 | 단순 CRUD와 JPA 저장 테스트가 직선적이다. 업무 규칙이 Proxy나 Persistence Context에 기대면 단위 테스트와 실제 실행 조건이 달라질 수 있다. | 업무 규칙은 평범한 Java 객체로 빠르게 테스트할 수 있다. 대신 Mapper와 JPA Adapter의 통합 테스트가 필요하다. |
| 변경 격리 | 테이블과 업무 모델이 거의 같으면 중복 수정이 없다. JPA 연관관계나 스키마 변경이 업무 코드까지 전파될 수 있다. | DB 스키마, 외부 API, 캐시 모델의 차이를 Adapter에서 흡수할 수 있다. 두 모델이 함께 바뀌면 Mapper까지 수정해야 한다. |
| 저장 동작 | Dirty Checking, Cascade, Optimistic Locking을 같은 객체에서 직접 활용한다. | ID, Version, 자식 관계, 삭제와 순서를 왕복 보존하는 책임이 Mapper로 이동한다. |
| 주요 실패 | 영속성 타입과 생명주기가 Domain과 API 계약까지 새어 나갈 수 있다. | 필드 누락과 잘못된 Enum 변환, 자식 식별자 유실 같은 매핑 버그가 생길 수 있다. |

Jakarta Persistence 명세는 Entity를 영속 Domain 객체로 설명하며 업무 메서드를 가지는 것을 금지하지 않는다. 따라서 `@Entity`가 붙었다는 이유만으로 빈약한 모델이 되거나 반드시 분리해야 하는 것은 아니다. 다만 JPA가 요구하는 생성자와 Proxy 관련 제약, 영속성 생명주기가 업무 규칙의 표현과 테스트에 들어오는지는 별도로 봐야 한다.

### 실제 사용 현장과 사이즈 기준

시스템 크기는 요청량이나 테이블 수 하나로 판단하기보다 변경 표면으로 판단하는 편이 현실적이다. 변경 표면은 업무 규칙의 복잡도, 저장 모델 수, 팀 소유 경계, 스키마와 업무 모델이 독립적으로 바뀌는 정도를 뜻한다.

| 현장 | 추천 출발점 | 이유 |
| --- | --- | --- |
| 한 팀, 한 관계형 DB, 테이블과 객체가 거의 같고 대부분 CRUD | 통합 | Mapper가 보호할 차이가 작아 중복 비용이 더 크다. |
| 한 DB를 사용하지만 일부 핵심 Aggregate에 상태 전이와 불변식이 집중됨 | 선택적 분리 | 기준정보와 단순 조회는 통합하고 JPA 의존 피해가 있는 핵심 Aggregate에만 비용을 쓸 수 있다. |
| Legacy Schema와 업무 모델의 구조가 다르거나 여러 DB와 외부 API가 같은 업무 개념을 제공함 | 분리 | Adapter가 데이터 소스별 차이를 흡수해 Domain 변경 전파를 줄일 수 있다. |
| 여러 팀이 하나의 Persistence Entity를 직접 공유함 | 경계별 분리 검토 | 한 팀의 매핑 변경이 다른 업무 영역으로 전파될 가능성이 크다. |
| 읽기 비중이 높고 화면마다 필요한 조회 형태가 다름 | 조회 전용 모델 검토 | 모든 조회를 Domain Aggregate로 복원하면 불필요한 Join과 변환 비용이 생길 수 있다. |

Spring PetClinic의 `Owner`는 `@Entity`, `@OneToMany`, `addPet()`과 `addVisit()`을 한 클래스에 둔다. 작은 CRUD 중심 Spring 애플리케이션에서 통합 모델이 얼마나 직선적인지 보여주는 구현 참고 사례다. 이 프로젝트는 대규모 운영 근거라기보다 통합 방식의 이해를 위한 예시다.

반대편 Java 구현 예시인 Buckpal은 Domain `Account`와 `AccountJpaEntity`를 나누고 `AccountMapper`로 변환한다. Persistence Adapter가 여러 쿼리 결과로 Domain 객체를 조립하고 새 활동만 다시 JPA Entity로 바꿔 저장한다. 이 사례는 분리 시 Mapper가 단순 복사뿐 아니라 로딩 범위와 저장 정책까지 책임질 수 있음을 보여준다.

Netflix Studio Workflows는 JPA 사례는 아니지만 대규모 현장에서 분리가 가치 있어지는 조건을 보여준다. 하나의 업무 Entity를 gRPC, JSON API, GraphQL 같은 여러 데이터 소스 뒤에 두고 저장 위치를 모르는 Domain과 Repository 계약을 사용했다. 팀은 한 Entity의 읽기 소스를 JSON API에서 GraphQL로 약 두 시간 만에 전환했다고 설명한다. 여기서 중요한 것은 회사 크기가 아니라 실제 데이터 소스 교체와 업무 모델 보호가 필요했다는 점이다.

반대로 Shopify의 대규모 Rails 모놀리스 사례는 규모가 크다고 전면 분리가 자동 정답은 아니라는 점을 보여준다. 여러 팀이 Active Record 모델을 공유하면서 경계가 약해졌지만 전면 재작성 대신 소유권과 컴포넌트 경계를 점진적으로 강화했다. 이는 JPA 사례가 아니라 통합된 영속 모델을 대규모 조직에서 공유할 때의 유사한 Trade-off 사례다.

### 예시 1: 업무 규칙은 있지만 통합을 유지하는 경우

```java
@Entity
class Ticket {
    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    protected Ticket() {}

    public void reserve() {
        if (status == TicketStatus.SUSPENDED) {
            throw new CannotReserveSuspendedTicket();
        }
        status = TicketStatus.RESERVED;
    }
}
```

판매 중지 티켓을 예약할 수 없다는 업무 규칙이 있으므로 보호할 Domain은 존재한다. 그러나 `Ticket`을 평범한 Java 객체로 만들어 규칙을 테스트할 수 있고, 규칙 실행이 Lazy Association이나 JPA Lifecycle에 의존하지 않는다면 별도 Domain Entity로 분리해도 제거되는 문제가 작다. 이 경우 Rich JPA Entity로 통합을 유지하는 편이 경제적이다.

### 예시 2: JPA 의존 피해 때문에 분리하는 경우

```java
@Entity
class Ticket {
    @OneToMany(fetch = FetchType.LAZY)
    private List<Reservation> reservations;

    public void reserve(UserId userId) {
        boolean alreadyReserved = reservations.stream()
                .anyMatch(it -> it.reservedBy(userId));

        if (alreadyReserved) {
            throw new AlreadyReserved();
        }

        reservations.add(Reservation.by(userId));
    }
}
```

여기서는 중복 예약을 막는 업무 규칙이 JPA가 관리하는 Lazy Collection을 직접 탐색한다. 단위 테스트에서는 일반 `List`를 넣어 통과하지만 실제 실행에서는 Persistence Context 밖에서 접근하면 실패하거나 규칙 실행 중 예상하지 못한 조회가 발생할 수 있다. Fetch 전략과 연관관계 변경도 업무 메서드에 영향을 준다.

Domain `Ticket`과 `TicketJpaEntity`를 분리하면 Domain은 완성된 일반 Collection으로 규칙을 실행하고, JPA Adapter는 필요한 상태를 조회하고 변환하는 책임을 갖는다. 대신 Adapter가 규칙 실행에 필요한 Aggregate를 완전하게 복원하는지 검증해야 한다. 이 매핑 및 검증 비용이 분리의 대가다.

### 나의 분리 기준

나는 먼저 `D`와 `P` 두 가지를 확인한다.

`D`는 보호할 업무 규칙이다. 현재 상태, 둘 이상의 업무 값, 자식, 이력이나 시간을 사용해 잘못된 상태를 막는 고유 규칙이 한 개 이상이면 `D=true`다. Null, 문자열 길이, JSON 형식처럼 기술적인 입력 검증만 있다면 `D=false`다.

`P`는 Persistence가 그 규칙을 침범하고 있다는 증거다. 다음 중 하나가 코드나 테스트에서 확인되면 `P=true`로 판단한다.

- 업무 규칙 테스트에 EntityManager, DB, Proxy 초기화나 Persistence Context가 필요하다.
- 업무 메서드가 Lazy Association 또는 JPA Lifecycle과 Dirty Checking을 실행 전제로 삼는다.
- Core Port가 JPA Entity를 입력이나 출력 타입으로 노출한다.
- 같은 업무 개념을 여러 Persistence Model이나 데이터 소스에서 구성해야 한다.

단순히 `@Entity`가 붙어 있다는 사실만으로 `P=true`로 보지 않는다. 분리 편익은 다음과 같이 판단한다.

```text
분리 편익 = D AND P
```

- `D=false`이면 별도 Domain Entity로 보호할 규칙이 없으므로 통합한다.
- `D=true`, `P=false`이면 Rich JPA Entity로도 규칙을 독립적으로 테스트할 수 있으므로 통합을 유지한다.
- `D=true`, `P=true`이면 분리 후보가 된다. 업무 규칙을 JPA 실행 의미에서 격리해 테스트 비용과 변경 전파를 줄일 수 있기 때문이다.

마지막으로 Mapper가 단순 값 변환으로 끝나는지 확인한다. 필드를 옮기는 순수 변환이라면 분리 비용을 예측할 수 있다. 반면 Mapper가 기존 자식과 새 자식을 비교하거나 ID와 Version을 왕복 보존하고 삭제와 순서를 결정하거나 DB를 다시 조회해야 한다면 Mapper 자체가 새로운 복잡성의 중심이 된다. 이 경우 전면 통합으로 돌아가기보다 Aggregate 경계를 줄이거나 업무 규칙이 있는 변경 영역만 분리하고 조회는 별도 조회 모델로 처리한다.

내 최종 기준은 다음과 같다.

> 보호할 업무 규칙이 있고, 그 규칙이 JPA의 타입이나 실행 의미에 의존해 테스트 비용과 변경 전파를 만들 때 분리를 검토한다. 분리로 얻는 격리 편익이 단순 Mapper와 매핑 검증 비용보다 크면 분리한다. Mapper가 DB 상태를 해석하고 관계 변경을 결정해야 할 정도라면 현재 Aggregate 전체를 그대로 복제하지 않고 분리 범위를 다시 설계한다.

DB 모델과 업무 모델의 모양이 다르거나 변경 이유가 다르다는 사실은 분리 편익을 키우는 증폭 신호다. 그러나 보호할 업무 규칙과 실제 Persistence 의존 피해가 없다면 모델 모양이 다르다는 이유만으로 별도 Rich Domain Entity를 만들지는 않는다.

### 참고

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)
- [Spring PetClinic Owner](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/owner/Owner.java)
- [Buckpal AccountPersistenceAdapter](https://github.com/thombergs/buckpal/blob/master/src/main/java/io/reflectoring/buckpal/adapter/out/persistence/AccountPersistenceAdapter.java)
- [Microsoft, Design a DDD-oriented microservice](https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/microservice-domain-model)
- [Netflix, Ready for Changes with Hexagonal Architecture](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)
- [Shopify, Deconstructing the Monolith](https://shopify.engineering/shopify-monolith)
- [Allegro, Hexagonal Architecture by Example](https://blog.allegro.tech/2020/05/hexagonal-architecture-by-example.html)
