# Original User Input

그러면 이제 수행내용3을 해결하러가보자 헥사고날이 내가 제시한 비평 어떻게 해결하는지


# Checked Context

# Project Context

- `task3/assignments/taskC-1.md` 수행 내용 3은 헥사고날이 앞선 N-Tier 비평과 DDD 보호 목표를 DIP로 어떻게 푸는지 설명하라고 요구한다.
- 과제는 Inbound Port와 Outbound Port가 왜 인터페이스이고 안쪽 모델의 일부인지, Adapter가 왜 구현체이고 인프라인지, 의존성이 안쪽을 향한다는 뜻을 요구한다.
- 답안 1의 핵심 비평은 계층 개수가 아니라 Domain Policy가 JPA, HTTP 요청 타입, DB 모델 같은 외부 기술을 향해 의존한다는 것이다.
- 답안 1의 구체 문제는 Service가 JPA Repository와 `@Entity`를 사용하고, Controller가 JPA Entity를 API로 반환하며, Service가 `HttpServletRequest`에서 헤더를 직접 읽는 것이다.
- 답안 2는 DDD가 보호하려는 대상을 업무 언어와 규칙이 담긴 Domain Model로 정의했다.
- `PROBLEM.md`에는 이번 주제와 직접 관련된 열린 문제가 없다.

# Evidence Anchors

- Alistair Cockburn, original Hexagonal Architecture article: https://alistair.cockburn.us/hexagonal-architecture
  - Application이 UI와 DB 없이도 동작하고 자동 테스트될 수 있게 만드는 것이 의도다.
  - Port는 기술이 아니라 목적 있는 대화를 식별하고, Adapter는 기술 입력을 Application이 사용할 호출이나 메시지로 변환한다.
  - 하나의 Port에 HTTP, Batch, Test Harness, SQL, In-memory Adapter 등이 연결될 수 있다.
- Robert C. Martin, Dependency Inversion Principle: https://objectmentor.com/resources/articles/dip.pdf
  - High-level policy가 low-level detail에 의존하지 않고 abstraction에 의존한다.
  - Detail implementation이 abstraction을 향해 의존하면서 기존 의존 방향이 역전된다.

# Constraints

- 헥사고날 구성 요소 나열보다 답안 1의 각 문제와 해결 장치를 일대일 대응한다.
- Compile-time dependency와 runtime call flow를 구분한다.
- Port가 반드시 Java interface여야 한다고 단정하지 않는다. Java에서는 계약과 대체 구현을 표현하기 위해 보통 interface를 쓴다고 설명한다.
- Inbound Port는 Domain Entity 자체보다 Application Core의 Use Case 계약이라고 설명한다.
- Outbound Port는 안쪽이 외부에 요구하는 업무 의미의 계약이며 안쪽이 소유한다고 설명한다.
- Hexagonal이 Domain Modeling 오류, 잘못 설계한 Port, 트랜잭션 문제를 자동 해결하지 않는 한계를 포함한다.
- 아직 `taskC-1.md`를 수정하지 않고 답안 접근 방향을 제시해 사용자 승인을 받는다.
