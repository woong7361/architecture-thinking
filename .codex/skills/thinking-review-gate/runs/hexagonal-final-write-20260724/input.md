# Original User Input

그러면 해당 내용을 정리해서 적어줘 

그리고 헥사고날이 뭔지 먼저 적어주고


# Checked Context

# Project Context

- 대상 파일은 `task3/assignments/taskC-1.md`이다.
- 문서의 수행 내용 3은 헥사고날이 앞선 N-Tiered 비평을 DIP로 어떻게 해결하는지 설명하라고 요구한다.
- Inbound Port와 Outbound Port의 역할, Adapter가 구현체이자 인프라인 이유, `의존성은 안쪽을 향한다`의 의미를 포함해야 한다.
- 기존 답안 1은 HTTP와 JPA 기술 타입이 업무 코드에 누수되면 테스트 비용과 변경 전파 범위가 커진다고 비평한다.
- 기존 답안 2는 DDD가 업무 언어와 규칙을 담은 도메인 모델을 보호 대상으로 정하고, 헥사고날과 DIP를 그 보호 방법으로 예고한다.
- 기존 예시는 판매 중지된 티켓을 예약할 수 없다는 규칙과 `Ticket.reserveBy(UserId)`이다.
- 사용자는 `SaveTicketPort`와 `TicketJpaAdapter`의 차이를 질문했고, 전자는 Application Core가 소유한 저장 계약, 후자는 JPA로 계약을 수행하는 Infrastructure 구현체라고 설명했다.
- 런타임 호출 방향과 소스 코드 의존 방향을 같은 화살표로 표현하지 않는다.
- Port는 보통 Java 인터페이스로 표현하지만, 개념의 본질은 Application Core가 소유하는 목적 중심 계약이다.
- 모든 Port를 DDD Entity나 Value Object와 같은 도메인 모델로 부르지 않는다. Inbound Port는 Application Use Case 경계이고 Outbound Port는 Core가 외부에 요구하는 계약이다.
- 단순 CRUD에서는 Port, Adapter, Mapper의 비용이 보호 가치보다 클 수 있다는 trade-off를 포함한다.
- 근거는 Alistair Cockburn의 Hexagonal Architecture 원문과 Robert C. Martin의 Dependency Inversion Principle 원문을 사용한다.
- 한국어 Markdown이며 괄호식 부연을 지양하고, 원문을 보지 않은 독자도 이해할 수 있게 인과와 예시를 보존한다.
