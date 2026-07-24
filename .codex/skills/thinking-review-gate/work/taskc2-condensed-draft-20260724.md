## 답안: Domain Entity와 JPA Entity의 분리 판단

통합 방식은 업무 상태와 행위, JPA 매핑을 한 클래스에 둔다. 분리 방식은 순수 Java Domain Entity와 `@Entity`를 따로 두고 Persistence Adapter의 Mapper가 변환한다. JPA Entity에도 업무 메서드를 둘 수 있으므로 `@Entity`라는 이유만으로 분리할 필요는 없다. 판단의 핵심은 업무 규칙을 JPA로부터 격리해 얻는 편익과 매핑 비용의 비교다.

### 두 방식의 현실적 Trade-off

| 항목 | 통합 | 분리 |
| --- | --- | --- |
| 개발·저장 | 클래스와 필드가 한 벌이고 Mapper 없이 Dirty Checking과 Optimistic Locking을 활용한다. | Domain, JPA Entity, Mapper와 매핑 테스트가 추가된다. |
| 업무 모델 | Rich Model도 가능하지만 생성자, Proxy, Lazy Loading과 연관관계 매핑이 설계에 개입할 수 있다. | 불변 객체와 Aggregate를 JPA 제약 없이 표현한다. |
| 테스트·변경 | 단순 CRUD는 직선적이다. JPA 생명주기가 업무 규칙에 들어오면 통합 테스트와 변경 전파가 늘어난다. | 업무 규칙은 plain Java로 테스트한다. 대신 Adapter 통합 테스트와 왕복 매핑 검증이 필요하다. |
| 주요 위험 | 영속성 타입과 실행 조건이 Core까지 샐 수 있다. | ID, Version, 자식 추가·삭제·순서를 잘못 변환할 수 있다. |

### 나의 기준

`D`는 보호할 업무 규칙이다. 현재 상태, 여러 업무 값, 자식이나 이력을 사용해 잘못된 상태를 막는 고유 규칙이 한 개 이상이면 `D=true`다. 단순 형식 검증과 CRUD만 있으면 `D=false`다. `D=true`는 보호 대상이 있다는 뜻일 뿐이며 P까지 확인되어야 분리를 검토한다.

`P`는 JPA 때문에 그 규칙의 테스트 또는 변경 격리가 깨진 증거다. 다음 중 하나가 코드나 테스트에서 확인되면 `P=true`다.

- 업무 규칙 실행에 DB, Proxy 초기화나 Persistence Context가 필요하다.
- 업무 메서드가 Lazy Association, JPA Lifecycle이나 Dirty Checking을 전제로 한다.
- Core Port가 JPA Entity를 노출하거나 저장 모델 차이를 Core가 직접 처리한다.

```text
D=false            -> 통합
D=true, P=false    -> Rich JPA Entity로 통합
D=true, P=true     -> 분리 후보
```

`D`와 `P`가 모두 참인 후보에서 Mapper 비용을 마지막으로 확인한다. 왕복 테스트가 단순 필드 비교로 끝나고 저장 시 추가 조회나 자식 삭제·순서·Version 판단이 필요 없다면 분리한다. 그렇지 않다면 현재 Aggregate 전체를 두 벌로 만들지 않고 Aggregate를 줄이거나 업무 규칙이 있는 변경 영역만 분리한다.

> 보호할 업무 규칙이 JPA의 타입과 실행 의미에 의존해 테스트 비용과 변경 전파를 만들고, 이를 격리하는 편익이 Mapper와 검증 비용보다 클 때 분리한다.

### 현장과 사이즈에 적용

| 현장 | 판단 |
| --- | --- |
| 한 팀, 한 DB, 대부분 CRUD | 대체로 `D=false`이므로 통합한다. |
| 한 DB의 핵심 Aggregate에 상태 전이와 불변식이 많음 | 먼저 P를 확인한다. `P=false`이면 Rich JPA Entity를 유지하고 `P=true`이면 선택적으로 분리한다. |
| Legacy Schema, 여러 데이터 소스나 팀 경계 | D가 있는 영역에서 저장 모델 차이가 Core로 전파되면 분리 편익이 커진다. D가 없으면 Adapter DTO나 조회 모델만 변환한다. |

따라서 회사 크기, 트래픽이나 테이블 수만으로 분리하지 않는다. [Spring PetClinic](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/owner/Owner.java)은 JPA Entity에 행위와 관계 매핑을 함께 둔 통합 구현 참고 사례다. [Buckpal](https://github.com/thombergs/buckpal/blob/master/src/main/java/io/reflectoring/buckpal/adapter/out/persistence/AccountPersistenceAdapter.java)은 Domain과 JPA Entity를 Mapper로 나눈 Java 구현 예시다. [Netflix Studio Workflows](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)는 JPA 사례는 아니지만 여러 데이터 소스를 하나의 Domain 뒤에서 교체해야 하는 대규모 운영 문맥을 보여준다.

### 실제 분리 방식과 예시

통합해도 Port와 Adapter는 유지할 수 있다. 통합에서는 `Ticket @Entity`가 Domain 역할도 맡고 Adapter는 Repository 호출만 감싼다. 따라서 Mapper는 없지만 Core가 JPA 의존을 허용한다. 분리하면 다음처럼 JPA 관련 책임을 바깥에 둔다.

```text
core
├─ domain/Ticket.java
├─ application/TicketService.java
└─ port/out/LoadTicketPort.java, SaveTicketPort.java

adapter/out/persistence
├─ TicketJpaEntity.java
├─ TicketJpaRepository.java
├─ TicketMapper.java
└─ TicketPersistenceAdapter.java   -> Load/Save Port 구현
```

```text
조회: JPA Entity -> Mapper -> Domain Ticket -> 업무 규칙 실행
저장: Domain Ticket -> Mapper/Adapter -> JPA Entity -> Repository
```

예를 들어 `Ticket.reserve()`가 `status`만 확인하고 plain Java 테스트로 검증된다면 `D=true`, `P=false`이므로 통합한다. 반대로 예약 규칙이 `@OneToMany(fetch = LAZY)`인 `reservations`를 직접 순회해 Persistence Context와 암묵적 조회에 영향을 받는다면 `D=true`, `P=true`인 분리 후보다. 먼저 필요한 데이터를 명시적으로 조회하거나 Aggregate를 줄일 수 있는지 확인하고, 의존이 계속 남으면 Domain `Ticket`과 `TicketJpaEntity`를 분리한다.

### 참고

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)
- [Microsoft, Design a DDD-oriented microservice](https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/microservice-domain-model)
- [Allegro, Hexagonal Architecture by Example](https://blog.allegro.tech/2020/05/hexagonal-architecture-by-example.html)
