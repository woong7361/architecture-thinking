# 수정 결론

사용자의 지적이 맞다. 현재 현장·사이즈 표는 `Legacy Schema`, `여러 데이터 소스`, `여러 팀`을 곧바로 분리 추천으로 연결해 뒤의 개인 기준 `D AND P`보다 별도의 결정 규칙처럼 보인다. 이는 다음처럼 고쳐야 한다.

> 현장과 사이즈는 분리 결론이 아니라 D와 P를 어디서 확인할지 알려주는 문맥이다. 어떤 규모에서도 최종 판정은 Aggregate별 D/P로 한다.

또한 현재 답안에는 분리된 클래스의 배치, Port 타입, Mapper 위치, 읽기와 저장 흐름이 없어 수행 내용 1의 `분리하는 방식` 설명이 부족하다.

## 교체할 현장·사이즈 기준

시스템 크기는 분리 여부를 직접 결정하지 않는다. 같은 대규모 시스템 안에서도 단순 기준정보는 통합할 수 있고, 작은 시스템에서도 핵심 규칙이 JPA 런타임에 묶이면 분리할 수 있다. 규모와 현장 조건은 D와 P의 증거가 나타날 가능성과 분리 비용의 범위를 알려주는 보조 문맥이다.

| 현장 조건 | D/P로 다시 해석 | Domain/JPA Entity 판단 |
| --- | --- | --- |
| 한 팀, 한 DB, 대부분 CRUD | 보통 D가 없거나 작다. | `D=false`면 통합한다. 업무 규칙이 생기면 P를 다시 확인한다. |
| 한 DB지만 핵심 Aggregate에 상태 전이와 불변식이 많음 | `D=true`일 가능성이 높다. 그러나 JPA 침범을 뜻하지는 않는다. | `P=false`면 Rich JPA Entity로 통합한다. `P=true`일 때만 분리 후보가 된다. |
| Legacy Schema 또는 여러 데이터 소스 | 모델 변환과 변경 전파가 생겨 P의 변경 격리 실패가 드러날 가능성이 높다. | `D=true`이고 같은 업무 규칙이 저장 모델 차이에 끌려가면 분리한다. `D=false`면 Adapter DTO나 조회 모델 변환만 두고 Rich Domain Entity는 만들지 않는다. |
| 여러 팀이 하나의 Persistence Entity를 공유 | 팀 경계와 Bounded Context가 섞였다는 별도 문제일 수 있다. | 먼저 팀별 계약과 Context 경계를 분리한다. 각 Context 내부의 Domain/JPA 분리는 다시 D/P로 판단한다. |
| 읽기 비중이 높고 화면마다 조회 모양이 다름 | 조회 최적화 문제이며 D/P와 다른 축이다. | 조회 Projection을 사용한다. 이 조건만으로 쓰기 Domain Entity와 JPA Entity를 분리하지 않는다. |

PetClinic은 통합 JPA 모델을 이해하기 위한 작은 구현 사례이고, Buckpal은 분리 구조와 Adapter의 책임을 보여주는 Java 구현 사례다. Netflix는 여러 데이터 소스 교체가 실제로 필요한 운영 문맥을 보여주지만 JPA 사례나 모든 대규모 시스템의 분리 근거는 아니다. Shopify는 대규모 통합 모델의 경계 문제를 점진적으로 고친 반례다. 사례의 크기가 아니라 해당 Aggregate에서 D와 P가 함께 확인되는지가 최종 기준이다.

## 추가할 분리 방식

통합 방식은 Domain 행위와 JPA 매핑을 한 클래스에 둔다.

```text
Application Service
    -> JpaRepository<Ticket, Long>
        -> Ticket @Entity
            - JPA mapping
            - reserve() 업무 규칙
```

분리 방식은 Core가 Domain 타입으로 Port를 정의하고 Persistence Adapter가 JPA와 Domain 사이를 번역한다.

```text
core
├─ domain/Ticket.java
└─ application/port/out
   ├─ LoadTicketPort.java       -> Optional<Ticket>
   └─ SaveTicketPort.java       -> save(Ticket)

adapter/out/persistence
├─ TicketJpaEntity.java         -> @Entity와 연관관계 매핑
├─ TicketJpaRepository.java     -> Spring Data JPA
├─ TicketMapper.java            -> Domain ↔ JPA 변환
└─ TicketPersistenceAdapter.java-> Load/Save Port 구현
```

의존 방향은 다음과 같다.

```text
Application Core <- Port <- TicketPersistenceAdapter -> JPA Repository
Domain Ticket    <------ TicketMapper ------> TicketJpaEntity
```

Core의 Port는 `TicketJpaEntity`가 아니라 Domain `Ticket`을 입출력으로 사용한다. JPA 타입, Lazy Loading, Persistence Context를 아는 코드는 Persistence Adapter 안에만 둔다.

읽기 흐름은 다음과 같다.

```text
1. Application Service가 LoadTicketPort.load(TicketId)를 호출한다.
2. TicketPersistenceAdapter가 TicketJpaRepository를 조회한다.
3. TicketMapper.toDomain()이 TicketJpaEntity를 Domain Ticket으로 변환한다.
4. Application Service가 Ticket.reserve()를 실행한다.
```

저장 흐름은 다음과 같다.

```text
1. Application Service가 SaveTicketPort.save(Ticket)를 호출한다.
2. TicketPersistenceAdapter가 새 객체인지 기존 객체인지 구분한다.
3. 새 객체는 Mapper가 JPA Entity를 생성한다.
4. 기존 객체는 관리 중인 JPA Entity를 조회한 뒤 Domain 변경 상태를 반영한다.
5. JPA Repository가 저장한다.
```

기존 JPA Entity를 갱신하는 이유는 DB가 관리하는 ID, Version, 자식 Entity 식별자와 관계 상태를 잃지 않기 위해서다. Mapper가 단순 필드 복사를 넘어 기존 자식과 새 자식을 비교하고 삭제나 순서를 판단하기 시작하면 분리 비용이 커진다. 이때는 현재 Aggregate 전체를 그대로 두 벌로 만들지 않고 Aggregate 경계를 줄이거나 업무 규칙이 있는 변경 모델만 분리한다.

## 교정된 전체 판단 순서

```text
1. Aggregate에서 D를 확인한다.
   D=false -> 통합

2. D=true이면 P를 확인한다.
   P=false -> Rich JPA Entity로 통합
   P=true  -> 분리 후보

3. 분리 구조의 Mapper가 단순 변환인지 확인한다.
   단순 변환 -> Domain/JPA Entity 분리
   DB 조회, 자식 Diff, ID/Version/삭제 판단 필요 -> 분리 범위 재설계
```

따라서 현장과 사이즈 표, 코드 예시, 개인 기준, 분리 구현 방식이 모두 하나의 순서로 연결된다. 현장 조건이 D/P 검사 위치를 알려주고, D/P가 분리 편익을 결정하며, Mapper 책임이 분리 범위를 결정한다.
