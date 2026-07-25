# Original User Input

task3\assignments\taskC-3.md 를 확인해보면 수행내용1에 과거 task의 헥사고날 구조를 그리라고 되어있는데 

이미 헥사고날로 되어있지 않나?

그리고 taskC의 내용을 확인하고 추가적으로 손볼 곳이 있는지 확인하고 제출물에 적어줘


# Checked Context

# 확인한 프로젝트 문맥

- `task3/assignments/taskC-3.md`는 기존 B-2 또는 B-4 설계를 Core, Inbound Port, Outbound Port, Adapter로 식별하고 Gherkin Feature와 Why 설명을 제출하라고 요구한다.
- `task2/assignments/taskB-4.md`의 설계도에는 `TicketService`, `Ticket`, `ChargePort`, `TicketRepository`, `UserRepository`, 테스트 Fake와 결제 Adapter가 이미 있다.
- `task2/task5-history/src/main/java/com/thinking/ticket/TicketService.java`는 세 Outbound Port와 `DiscountPolicy`에 의존한다.
- 현재 `TicketService.reserveTicket`은 사실상의 Inbound 경계로 볼 수 있지만 별도 Inbound Port 인터페이스는 없다.
- `task2/task5-history/src/test/java/com/thinking/ticket/steps/TicketReservationSteps.java`는 When에서 `TicketService`를 직접 생성하고 호출한다.
- 프로덕션 코드는 단일 `com.thinking.ticket` 패키지에 있고, 현재 Adapter 구현은 테스트용 In-memory Fake와 결제 Test Double뿐이다.
- B-5 Feature에는 결제 성공 후 저장 실패 시 결제가 남고 보상이 없다는 기존 결함이 특성화되어 있다.
- 현재 In-memory Ticket Repository는 객체 참조를 Map에 그대로 보관하므로 저장 실패 전 Domain 객체 변경이 저장 상태에도 보일 수 있다.
- JDK 17을 명시해 `mvn clean test`를 실행한 결과 B-5의 8개 시나리오와 53개 Step이 모두 통과했다.
- C-4는 Docker와 Compose 재현 환경, C-5는 Inbound Port 기반 Cucumber와 Testcontainers 및 Adapter 교체, C-6은 HTTP부터 DB까지 walking skeleton과 AI 경계 검수를 요구한다.
- `PROBLEM.md`에는 이 과제 구조와 직접 관련된 열린 문제가 없다.

# 적용한 접근

- 기존 구조를 비헥사고날이라고 부정하지 않고, 헥사고날의 핵심 방향이 이미 적용됐다고 판정한다.
- 별도 Inbound Port 인터페이스는 헥사고날의 필수 문법이 아니라 C-5와 C-6의 두 Driver와 When-Port 매핑을 명시하기 위한 선택으로 설명한다.
- 현재 구조, 목표 구조, 대안을 비교하고 의존 방향과 런타임 흐름을 분리해 제시한다.
- C-5와 C-6에서 실제 검증할 Gherkin과 Adapter 교체 증빙, 결제 보상, Fake 저장 의미, 자동 의존성 검사를 추가 보완점으로 연결한다.
