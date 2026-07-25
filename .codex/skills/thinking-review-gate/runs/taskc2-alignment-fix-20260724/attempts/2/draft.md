# Task C-2: Domain Entity vs Persistence Entity (매핑 비용의 판단)

(Grit's Why): 헥사고날을 실제로 구현할 때 첫 갈림길은 '도메인 모델과 JPA @Entity를 분리할 것인가'입니다. 정답이 아니라 비용/효용 판단입니다.

### 수행 내용

1. Domain 모델과 JPA @Entity를 분리하는 방식 vs 합치는 방식을 각각 리서치하고, 현실적 장점(Benefit)과 비용(Cost/Complexity)을 정리하세요.
2. '항상 분리' 또는 '항상 합침'이 아니라, 이 매핑 비용을 감수할 가치가 있는 경우는 언제인지 본인 기준을 세우세요. (1-2의 Rich Domain Model과도 연결지어 생각해 보세요.)

### 제출물

- [x]  분리 vs 통합의 장점·비용 정리 + 매핑 비용을 감수할 본인 기준. (최소 400자)

---

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

[Jakarta Persistence 명세](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)는 Entity를 영속 Domain 객체로 설명하며 업무 메서드를 가지는 것을 금지하지 않는다. 따라서 `@Entity`가 붙었다는 이유만으로 빈약한 모델이 되거나 반드시 분리해야 하는 것은 아니다. 다만 JPA가 요구하는 생성자와 Proxy 관련 제약, 영속성 생명주기가 업무 규칙의 표현과 테스트에 들어오는지는 별도로 봐야 한다.

### 실제 사용 현장과 사이즈를 내 기준으로 해석하기

시스템 크기는 분리 여부를 직접 결정하지 않는다. 같은 대규모 시스템 안에서도 단순 기준정보는 통합할 수 있고, 작은 시스템에서도 핵심 규칙이 JPA 런타임에 묶이면 분리할 수 있다. 여기서 `D`는 보호할 업무 규칙의 존재이고, `P`는 그 규칙이 JPA 때문에 테스트와 변경 격리를 잃는 증거다. 현장과 사이즈는 D와 P가 나타날 가능성을 알려주는 보조 문맥이며, 최종 판정은 Aggregate별 `D AND P`로 한다.

| 현장 조건 | D/P로 다시 해석 | Domain/JPA Entity 판단 |
| --- | --- | --- |
| 한 팀, 한 관계형 DB, 대부분 CRUD | 보통 D가 없거나 작다. | `D=false`이면 통합한다. 업무 규칙이 생기면 P를 다시 확인한다. |
| 한 DB지만 핵심 Aggregate에 상태 전이와 불변식이 많음 | `D=true`일 가능성이 높다. 그러나 이것만으로 JPA 침범이 생긴 것은 아니다. | `P=false`이면 Rich JPA Entity로 통합한다. `P=true`일 때만 분리 후보가 된다. |
| Legacy Schema 또는 여러 데이터 소스 | 모델 변환과 변경 전파가 생겨 P가 드러날 가능성이 높다. | `D=true`이고 업무 규칙이 저장 모델 차이에 끌려가면 분리한다. `D=false`이면 Adapter DTO나 조회 모델만 변환하고 별도 Rich Domain Entity는 만들지 않는다. |
| 여러 팀이 하나의 Persistence Entity를 공유함 | 팀 경계와 Bounded Context가 섞인 별도 문제일 수 있다. | 먼저 팀별 계약과 Context 경계를 나눈다. 각 Context 내부의 Domain/JPA 분리는 다시 D/P로 판단한다. |
| 읽기 비중이 높고 화면마다 조회 모양이 다름 | 조회 최적화 문제이며 D/P와 다른 축이다. | 조회 Projection을 사용한다. 이 조건만으로 쓰기 Domain Entity와 JPA Entity를 분리하지 않는다. |

공개 사례는 이 기준을 대신하는 결정 규칙이 아니라 각 방식의 비용을 보여주는 보조 예시다. [Spring PetClinic의 `Owner`](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/owner/Owner.java)는 `@Entity`, `@OneToMany`, `addPet()`, `addVisit()`을 한 클래스에 둔다. 작은 CRUD 중심 Spring 애플리케이션에서 통합 모델이 얼마나 직선적인지 보여주는 구현 참고 사례다.

반대편 Java 구현 예시인 [Buckpal](https://github.com/thombergs/buckpal/blob/master/src/main/java/io/reflectoring/buckpal/adapter/out/persistence/AccountPersistenceAdapter.java)은 Domain `Account`와 `AccountJpaEntity`를 나누고 `AccountMapper`로 변환한다. Persistence Adapter가 여러 쿼리 결과로 Domain 객체를 조립하고 새 활동만 다시 JPA Entity로 바꿔 저장한다. 이 사례는 분리 시 Mapper가 단순 복사뿐 아니라 로딩 범위와 저장 정책까지 책임질 수 있음을 보여준다.

[Netflix Studio Workflows 사례](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)는 JPA 사례는 아니지만 여러 데이터 소스 교체가 실제로 필요한 운영 문맥을 보여준다. [Shopify의 Rails 모놀리스 사례](https://shopify.engineering/shopify-monolith)는 대규모라고 전면 분리가 자동 정답은 아니며 소유권과 컴포넌트 경계를 점진적으로 강화할 수도 있음을 보여준다. 두 사례 모두 규모 자체가 Domain/JPA Entity 분리를 결정한다는 근거로 사용하지 않는다.

### Domain Entity와 JPA Entity를 실제로 분리하는 방식

통합 방식도 Port와 Adapter 경계를 유지할 수 있다. 다만 Domain 행위와 JPA 매핑을 한 클래스에 두고 Core와 Persistence Adapter가 같은 `Ticket`을 사용한다.

```text
core
├─ domain/Ticket.java            -> @Entity와 reserve() 업무 규칙
└─ application/port/out
   ├─ LoadTicketPort.java        -> Optional<Ticket>
   └─ SaveTicketPort.java        -> save(Ticket)

adapter/out/persistence
├─ TicketJpaRepository.java      -> Spring Data JPA가 Ticket을 직접 저장
└─ TicketPersistenceAdapter.java -> Load/Save Port 구현
```

이 구조에는 Mapper가 없지만 Core의 `Ticket`이 JPA Annotation과 Entity 생명주기를 함께 안다. 따라서 Port와 Adapter를 사용하더라도 Domain 모델의 JPA 의존까지 제거되는 것은 아니다.

분리 방식은 Application Core가 Domain 타입으로 Outbound Port를 정의하고 Persistence Adapter가 Port를 구현하면서 두 모델을 변환한다.

```text
core
├─ domain/Ticket.java
└─ application/port/out
   ├─ LoadTicketPort.java        -> Optional<Ticket>
   └─ SaveTicketPort.java        -> save(Ticket)

adapter/out/persistence
├─ TicketJpaEntity.java          -> @Entity와 연관관계 매핑
├─ TicketJpaRepository.java      -> Spring Data JPA
├─ TicketMapper.java             -> Domain과 JPA 변환
└─ TicketPersistenceAdapter.java -> Load/Save Port 구현
```

컴파일 의존성은 바깥쪽에서 안쪽을 향한다.

```text
adapter/out/persistence -> application/port/out -> domain
adapter/out/persistence -> JPA
```

런타임 호출은 다음 방향으로 진행된다.

```text
Application Service -> Load/Save Port
                    -> TicketPersistenceAdapter
                    -> TicketJpaRepository
```

`LoadTicketPort`와 `SaveTicketPort`는 Application Core에 있고 `TicketPersistenceAdapter`가 이를 구현한다. Core의 Port는 `TicketJpaEntity`가 아니라 Domain `Ticket`을 입출력으로 사용한다. `TicketMapper`는 Persistence Adapter 패키지에 있으며 Domain은 Mapper와 JPA Entity를 모른다.

조회와 업무 실행은 다음 순서로 이뤄진다.

```text
TicketJpaRepository 조회
    -> TicketMapper.toDomain()
    -> Domain Ticket
    -> Ticket.reserve()
```

저장할 때는 Application Service가 `SaveTicketPort.save(Ticket)`을 호출한다. 새 객체라면 Mapper가 JPA Entity를 생성한다. 기존 객체라면 Adapter가 관리할 JPA Entity를 조회하고 Domain에서 변경된 상태를 반영한 뒤 저장한다. 기존 Entity를 갱신하는 이유는 DB가 관리하는 ID, Version, 자식 Entity의 식별자와 관계 상태를 잃지 않기 위해서다.

```text
Domain Ticket
    -> SaveTicketPort
    -> 기존 TicketJpaEntity 조회 또는 신규 생성
    -> Domain 상태 반영
    -> TicketJpaRepository 저장
```

이 구조에서 분리 비용은 `TicketMapper`와 Adapter에 모인다. Mapper가 단순 필드 복사를 넘어 기존 자식과 새 자식을 비교하고 삭제나 순서를 판단하기 시작하면 현재 Aggregate 전체를 그대로 두 벌로 만들지 않는다. Aggregate 경계를 줄이거나 업무 규칙이 있는 변경 모델만 분리한다.

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

### 예시 2: JPA 의존 피해 때문에 분리를 검토하는 경우

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

먼저 필요한 데이터를 Repository Query에서 명시적으로 조회할 수 있는지, Aggregate가 너무 큰 것은 아닌지 검토할 수 있다. 그래도 업무 규칙이 JPA의 실행 조건에 계속 끌려간다면 Domain `Ticket`과 `TicketJpaEntity`를 분리한다. Domain은 완성된 일반 Collection으로 규칙을 실행하고 JPA Adapter는 필요한 상태를 조회하고 변환하는 책임을 갖는다. 대신 Adapter가 규칙 실행에 필요한 Aggregate를 완전하게 복원하는지 검증해야 한다. 이 매핑 및 검증 비용이 분리의 대가다.

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

마지막으로 Mapper가 단순 값 변환으로 끝나는지 확인한다. 필드를 옮기는 순수 변환이라면 분리 비용을 예측할 수 있다. 반면 Mapper가 다음 책임을 맡기 시작하면 현재 Aggregate 전체를 그대로 두 벌로 만드는 분리 방식은 중단하고 범위를 다시 검토한다.

- 매핑 도중 DB를 다시 조회한다.
- 기존 자식과 새 자식을 비교한다.
- ID와 Version을 왕복 보존한다.
- 삭제 여부와 자식 순서를 결정한다.

이 경우 전면 통합으로 돌아가기보다 Aggregate 경계를 줄이거나 업무 규칙이 있는 변경 영역만 분리하고 조회는 별도 조회 모델로 처리한다. 구현 후에는 Domain과 JPA Entity의 왕복 매핑 테스트에서 ID, Version, 자식 추가·수정·삭제, 순서가 보존되는지 확인한다.

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
