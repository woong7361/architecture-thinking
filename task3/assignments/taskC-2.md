# Task C-2: Domain Entity vs Persistence Entity (매핑 비용의 판단)

(Grit's Why): 헥사고날을 실제로 구현할 때 첫 갈림길은 '도메인 모델과 JPA @Entity를 분리할 것인가'입니다. 정답이 아니라 비용/효용 판단입니다.

### 수행 내용

1. Domain 모델과 JPA @Entity를 분리하는 방식 vs 합치는 방식을 각각 리서치하고, 현실적 장점(Benefit)과 비용(Cost/Complexity)을 정리하세요.
2. '항상 분리' 또는 '항상 합침'이 아니라, 이 매핑 비용을 감수할 가치가 있는 경우는 언제인지 본인 기준을 세우세요. (1-2의 Rich Domain Model과도 연결지어 생각해 보세요.)

### 제출물

- [x] 분리 vs 통합의 장점·비용 정리 + 매핑 비용을 감수할 본인 기준. (최소 400자)

---

## 답안: Domain Entity와 JPA Entity의 분리 판단

통합 방식은 업무 상태와 행위, JPA 매핑을 한 클래스에 둔다. 분리 방식은 순수 Java Domain Entity와 `@Entity`를 따로 두고 Persistence Adapter의 Mapper가 변환한다. JPA Entity에도 업무 메서드를 둘 수 있으므로 `@Entity`라는 이유만으로 분리할 필요는 없다. 핵심은 업무 규칙을 JPA로부터 격리해 얻는 편익과 매핑 비용의 비교다.

### 두 방식의 현실적 Trade-off


| 항목     | 통합                                                                 | 분리                                                           |
| ------ | ------------------------------------------------------------------ | ------------------------------------------------------------ |
| 개발·저장  | 클래스와 필드가 한 벌이고 Mapper 없이 Dirty Checking과 Optimistic Locking을 활용한다. | Domain, JPA Entity, Mapper와 매핑 테스트가 추가된다.                    |
| 업무 모델  | Rich Model도 가능하지만 생성자, Proxy, Lazy Loading과 연관관계 매핑이 설계에 개입할 수 있다. | 불변 객체와 Aggregate를 JPA 제약 없이 표현한다.                            |
| 테스트·변경 | 단순 CRUD는 직선적이다. JPA 생명주기가 업무 규칙에 들어오면 통합 테스트와 변경 전파가 늘어난다.         | 업무 규칙은 plain Java로 테스트한다. 대신 Adapter 통합 테스트와 왕복 매핑 검증이 필요하다. |
| 주요 위험  | 영속성 타입과 실행 조건이 Core까지 샐 수 있다.                                      | ID, Version, 자식 추가·삭제·순서를 잘못 변환할 수 있다.                       |


### 나의 기준

`D`는 분리 사유가 아니라 보호할 업무 규칙의 존재 여부다. 현재 상태, 여러 업무 값, 자식이나 이력을 사용해 잘못된 상태를 막는 고유 규칙이 한 개 이상이면 `D=true`다. 단순 형식 검증과 CRUD만 있으면 `D=false`다.

`P`는 JPA 때문에 그 규칙의 테스트 또는 변경 격리가 깨진 증거다. 다음 중 하나가 코드나 테스트에서 확인되면 `P=true`다.

- 업무 규칙 실행에 DB, Proxy 초기화나 Persistence Context가 필요하다.
- 업무 메서드가 Lazy Association, JPA Lifecycle이나 Dirty Checking을 전제로 한다.
- Core Port가 JPA Entity를 노출하거나 저장 모델 차이를 Core가 직접 처리한다.

```text
D=false         -> 통합
D=true, P=false -> Rich JPA Entity로 통합
D=true, P=true  -> 분리 후보
```

따라서 불변식 한 개나 `@Entity`만으로 분리하지 않는다. `D`와 `P`가 모두 참인 후보에서 Mapper 비용을 마지막으로 확인한다. 왕복 테스트가 단순 필드 비교로 끝나고 저장 시 추가 조회나 자식 삭제·순서·Version 판단이 필요 없다면 분리한다. 그렇지 않다면 Aggregate 전체를 두 벌로 만들지 않고 범위를 줄이거나 업무 규칙이 있는 변경 영역만 분리한다.

### 현장과 사이즈에 적용


| 현장                                 | 판단                                                                           |
| ---------------------------------- | ---------------------------------------------------------------------------- |
| 한 팀, 한 DB, 대부분 CRUD                | 대체로 `D=false`이므로 통합한다.                                                       |
| 한 DB의 핵심 Aggregate에 상태 전이와 불변식이 많음 | 먼저 P를 확인한다. `P=false`이면 Rich JPA Entity를 유지하고 `P=true`이면 선택적으로 분리한다.         |
| Legacy Schema, 여러 데이터 소스나 팀 경계     | D가 있는 영역에서 저장 모델 차이가 Core로 전파되면 분리 편익이 커진다. D가 없으면 Adapter DTO나 조회 모델만 변환한다. |


회사 크기, 트래픽이나 테이블 수만으로 분리하지 않는다. [Spring PetClinic](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/owner/Owner.java)은 JPA Entity에 행위와 관계 매핑을 함께 둔 통합 구현 참고 사례다. [Buckpal](https://github.com/thombergs/buckpal/blob/master/src/main/java/io/reflectoring/buckpal/adapter/out/persistence/AccountPersistenceAdapter.java)은 Domain과 JPA Entity를 Mapper로 나눈 Java 구현 예시다. [Netflix Studio Workflows](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)는 JPA 사례는 아니지만 여러 데이터 소스를 다루는 대규모 운영 문맥에서 헥사고날을 적용한 사례다.

### 실제 분리 방식과 예시

통합해도 Port와 Adapter는 유지할 수 있다. 통합에서는 `Ticket @Entity`가 Domain 역할도 맡고 Adapter는 Repository 호출만 감싼다. 따라서 Mapper는 없지만 Core가 JPA 의존을 허용한다. 분리하면 JPA 관련 책임을 바깥에 둔다.

여기서 주의할 점은 `Repository`라는 이름이다. Core가 외부 저장 기능을 필요로 해서 바라보는 계약은 `LoadTicketPort`, `SaveTicketPort`다. 이것을 Port라고 부른다. 반면 `TicketJpaRepository`는 Spring Data JPA를 쓰는 구체 저장 도구이고, Persistence Adapter 내부의 세부 구현이다. 따라서 `Repository`가 등장해도 그 타입이 Core로 새지 않고 Adapter 안에서만 쓰이면 괜찮다.

```text
                       source dependency
                            points inward

core
├─ domain/Ticket.java
├─ application/TicketService.java
│    └─ depends on -> port/out/LoadTicketPort, SaveTicketPort
└─ port/out/LoadTicketPort.java, SaveTicketPort.java

adapter/out/persistence
├─ TicketJpaEntity.java                    (JPA model)
├─ TicketJpaRepository.java                (Spring Data JPA detail)
├─ TicketMapper.java                       (Domain <-> JPA Entity)
└─ TicketPersistenceAdapter.java
     └─ implements -> LoadTicketPort, SaveTicketPort
     └─ uses       -> TicketJpaRepository
```

조회의 런타임 흐름은 `TicketService -> LoadTicketPort -> TicketPersistenceAdapter -> TicketJpaRepository -> TicketJpaEntity -> Mapper -> Domain Ticket`이다. 저장은 `TicketService -> SaveTicketPort -> TicketPersistenceAdapter -> Mapper -> TicketJpaEntity -> TicketJpaRepository` 순서다. 호출은 바깥으로 나가는 것처럼 보이지만, 소스 코드 의존성은 반대다. Core는 Adapter나 `TicketJpaRepository`를 import하지 않고, Adapter가 Core의 Port를 구현한다. Core Port는 JPA Entity가 아니라 Domain `Ticket`을 사용한다.

예를 들어 `Ticket.reserve()`가 `status`만 확인하고 plain Java 테스트로 검증된다면 `D=true`, `P=false`이므로 통합한다. 반대로 예약 규칙이 `@OneToMany(fetch = LAZY)`인 `reservations`를 직접 순회해 Persistence Context와 암묵적 조회에 영향을 받는다면 `D=true`, `P=true`인 분리 후보다. 먼저 필요한 데이터를 명시적으로 조회하거나 Aggregate를 줄일 수 있는지 확인하고, 의존이 계속 남으면 Domain `Ticket`과 `TicketJpaEntity`를 분리한다.

## 리뷰 피드백 (Notion 원본)

> **피드백 메타데이터**
> - 출처 페이지: [Phase 1] 1-3(헥사고날) 제출 - 현웅님
> - URL: [Notion 원본 페이지](https://sponge-girdle-ad1.notion.site/Phase-1-1-3-3a26276f9e0081b399c3f614fe445fa7)
> - 수집 방법: 프로젝트 루트 `notion_mcp.md` 참조
> - 원문 보존: 댓글 본문은 Notion comment 레코드의 텍스트를 그대로 옮긴 것이며 일절 수정하지 않았다.
> - 라인 기준: 이 섹션 위쪽 본문의 라인 번호. 본문을 편집하면 다시 수집해야 한다.

리뷰어가 이 문서의 **어느 라인, 어떤 부분**에 **어떤 피드백**을 남겼는지 정리한 것이다.
총 1건.

### FB-C2-01 · L18

- **위치**: L18
- **지적된 부분**: 문단 전체 — 통합 방식은 업무 상태와 행위, JPA 매핑을 한 클래스에 둔다. 분리 방식은 순수 Java Domain Entity와 @Entity를 따로 두고 Persistence Adapter의 Mapper가 변환한다. JPA Entity에도 업무 메서드를 둘 수 있으므로 @Entity라는 이유만으로 분리할 필요는 없다. 핵심은 업무 규칙을 JPA로부터 격리해 얻는 편익과 매핑 비용의 비교다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-08-10 16:03 KST
- **피드백 원문**:

```
만약 시간이 지나면서 이 통합된 엔티티 내부에 업무 규칙이 하나둘씩 늘어나 결국 도메인을 분리해야만 하는 임계점에 도달했을 때, 이미 시스템 전반에서 이 통합 엔티티를 강하게 참조하고 있다면 그 마이그레이션 비용을 감당해야 할 텐데요, 그런 거 생각하면 그냥 처음부터 분리하는 게 좋지 않나요 ? 
```

**답변 초안:**

처음부터 분리하면 미래 마이그레이션 위험을 줄일 수 있지만, 그 대신 실제로 분리가 필요해지지 않아도 모든 변경마다 Domain, JPA Entity, Mapper를 함께 고치는 비용을 계속 냅니다. 그래서 저는 모델을 반드시 두 벌로 만들기보다 통합 엔티티가 시스템 전반에 퍼지지 않게 이음새를 먼저 만들겠습니다. 엔티티를 API에 직접 노출하지 않고, 저장 접근을 Persistence Adapter 안에 모으며, 다른 유스케이스는 Port를 통해서만 접근하게 하면 나중에 분리하더라도 영향 범위를 해당 Adapter와 Aggregate로 제한할 수 있습니다.

이미 시스템 전반이 통합 엔티티를 직접 참조한다면 문제는 `처음부터 두 모델을 만들지 않았다`는 것보다 `경계 밖으로 엔티티를 노출했다`는 데 더 가깝다고 생각합니다. 업무 규칙 증가와 JPA 결합이 실제로 관찰되면 특성화 테스트로 현재 동작을 고정한 뒤, 변경이 많은 Aggregate 하나부터 새 Domain 모델과 Mapper를 넣어 점진적으로 옮기겠습니다. 반대로 규칙 증가나 저장 모델 변화가 이미 구체적인 로드맵으로 확인된 핵심 영역이라면 그때는 선행 분리 비용을 지불할 근거가 충분합니다.

