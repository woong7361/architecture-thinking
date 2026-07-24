맞습니다. B-4와 B-5는 이미 헥사고날의 핵심을 갖추고 있습니다. `TicketService`가 구체 DB와 결제 API 대신 `TicketRepository`, `UserRepository`, `ChargePort`에 의존하고, 테스트 Fake가 이 포트를 구현합니다. 다만 `TicketService.reserveTicket`이 사실상의 Inbound 경계일 뿐 별도 계약으로 명시되지 않았고, Step Definition이 구체 서비스를 직접 호출하며, 패키지와 프로덕션 Adapter도 아직 분리되지 않았습니다. 따라서 C-3은 처음부터 재설계하는 과제라기보다 기존 설계를 명시적인 헥사고날 구조로 완성하고 증명하는 과제로 정리했습니다.

`taskC-3.md` 제출물에는 다음을 반영했습니다.

- 기존 상태와 C-3 목표 상태의 비교
- Core, Inbound Port, Outbound Port, Adapter를 구분한 Mermaid 다이어그램
- `ReserveTicketUseCase.reserve(command)`와 Gherkin When의 1:1 매핑
- Happy Path와 주요 Unhappy Path를 포함한 티켓 예매 Feature
- Adapter 교체 시 Core가 바뀌지 않는 조건과 테스트 분리 이유
- C-2부터 C-6까지 연결해 추가로 손볼 일곱 가지

추가 보완점 중 중요한 것은 세 가지입니다. C-5에서는 기존 Step Definition이 `TicketService`를 직접 생성하지 않고 Inbound Port를 호출해야 합니다. 결제 후 저장 실패 시 결제가 남는 B-5의 기존 결함은 `CancelChargePort`를 통한 보상 시나리오로 고정했습니다. 현재 In-memory Repository는 같은 객체 참조를 보관하므로 저장 실패 전 변경이 저장 상태에 새어 보일 수 있어, 복사본 반환이나 저장 성공 시 반영 방식으로 실제 저장 경계와 의미를 맞춰야 합니다.

검증은 JDK 17을 지정한 `mvn clean test`로 수행했고 기존 B-5 테스트 8개 시나리오와 53개 Step이 모두 통과했습니다. C-0과 C-4부터 C-6은 아직 실제 구현과 증빙이 없는 후속 과제이므로 완료 표시하지 않았습니다.
