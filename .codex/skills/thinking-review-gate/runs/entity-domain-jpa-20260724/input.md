# Original User Input

근데 entity로 가는게 맞아? domain으로 가야하는거 아니야? entity를 domain으로 쓸 수도 있지만 트레이드오프가 있잖아


# Checked Context

# Project Context

- 사용자는 Java와 Spring을 선호하는 백엔드 개발자다.
- 직전 설명에서 `Order`를 `Aggregate Root이면서 Entity`라고 표현했다. 사용자는 이것이 JPA Entity를 의미하는 것으로 읽힐 수 있으며, Domain Model과 분리 여부의 trade-off가 빠졌다고 지적했다.
- 직전 Java 예제의 `Order`에는 `@Entity`가 없으므로 실제 의도는 순수 Domain Model의 DDD Entity였다.
- `task3/assignments/taskC-1.md` 20~46행은 한 객체가 영속성 모델, 업무 모델, API 모델을 동시에 맡을 때 JPA 지연 로딩, 프록시, 관리 상태, 더티 체킹이 상위 계층에 누수될 수 있다고 비평한다.
- `task3/assignments/taskC-1.md`의 결론은 `@Entity` 존재 자체보다 도메인 정책이 외부 기술과 그 모델을 향해 의존하는 것을 문제로 본다.

# Evidence Anchors

- Eric Evans, DDD Reference: https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf
  - Entity는 속성이 아니라 생명주기와 정체성으로 정의하는 도메인 모델 요소다.
  - Aggregate Root는 Aggregate의 진입점으로 선택된 Entity다.
- Jakarta Persistence 3.2 Specification: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.pdf
  - JPA Entity는 경량 영속 도메인 객체로 정의되며 `@Entity`, 인자 없는 생성자, non-final 같은 영속성 제약을 갖는다.
- Hibernate ORM User Guide: https://docs.hibernate.org/orm/7.0/userguide/html_single/
  - Hibernate는 Application Domain Model을 영속화하지만 Persistence Context, Proxy, Lazy Loading 등의 실행 의미를 추가한다.
- Spring Data JPA Core Concepts: https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html
  - Spring Data 문서는 entity, domain type, aggregate 용어를 일부 문맥에서 교환해 사용하므로 용어 혼동 가능성이 있다.

# Constraints

- `Domain`과 `Entity`가 양자택일이 아님을 먼저 설명한다.
- DDD Entity와 JPA `@Entity`를 명확히 구분한다.
- 순수 Domain Model 분리, Domain과 JPA 겸용, 단순 CRUD 접근을 3가지 선택지로 비교한다.
- 한 선택지를 절대 정답으로 만들지 않고 복잡도와 변경 압력에 따른 추천 조건을 제시한다.
- 패키지 위치보다 의존성 방향과 실행 의미 누수가 중요함을 설명한다.
- 직전 답변의 모호한 표현을 수정한다.
