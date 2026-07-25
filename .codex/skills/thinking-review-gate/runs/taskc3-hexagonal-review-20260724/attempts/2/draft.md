맞습니다. B-4와 B-5는 이미 헥사고날의 핵심 의존 방향을 갖추고 있습니다. `task2/task5-history/src/main/java/com/thinking/ticket/TicketService.java`는 구체 DB와 결제 API 대신 `TicketRepository`, `UserRepository`, `ChargePort`에 의존하고, `task2/task5-history/src/test/java/com/thinking/ticket/support`의 Fake와 Test Double이 이 포트를 구현합니다. 따라서 C-3은 기존 설계를 비헥사고날이라고 부정하고 처음부터 다시 그리는 과제가 아닙니다. 이미 적용된 Core와 Outbound 경계를 식별하고, 아직 암묵적인 Inbound 경계와 Adapter 역할을 명시하며, C-5와 C-6에서 교체 가능성을 증명할 설계로 고정하는 과제라고 판단했습니다.

제출물은 `task3/assignments/taskC-3.md`에 작성했습니다. 현재 상태와 목표 상태 비교, Core와 Port 및 Adapter 다이어그램, When과 Inbound Port의 1:1 매핑, 다섯 시나리오로 구성한 티켓 예매 Feature, 500자 이상의 Why 설명, Task C 후속 과제와 연결한 보완점을 넣고 세 체크 항목을 완료 표시했습니다.

현재 `TicketService.reserveTicket` 공개 메서드도 사실상의 Inbound Port로 볼 수 있으므로 별도 Java 인터페이스가 헥사고날의 필수 조건이라고 쓰지 않았습니다. 다음 세 대안을 제출물에서 비교했습니다.

1. `ReserveTicketUseCase` 인터페이스와 Command로 경계를 명시한다. 타입이 늘지만 C-5의 Cucumber와 C-6의 HTTP Controller가 같은 계약에 의존하고 When과 Port를 1:1로 제시하기 쉽습니다. 이번 추천안입니다.
2. 현재 `TicketService.reserveTicket`을 Port로 간주한다. 변경은 가장 작지만 두 Inbound Adapter가 구체 서비스에 결합되고 과제의 Port 식별이 덜 선명합니다.
3. Inbound API를 별도 모듈로 분리한다. 컴파일 경계는 가장 강하지만 현재 walking skeleton에는 설정 비용이 큽니다.

추가로 손볼 곳은 일곱 가지를 제출물에 적었습니다.

1. C-2의 판단을 적용해 이번 과제에서는 순수 Domain `Ticket`과 `TicketJpaEntity`를 Adapter에서 분리합니다. 저장 Adapter 교체와 Core 무수정 증명이 이번 과제의 목표이기 때문이며, 모든 프로젝트에 항상 분리한다는 일반 규칙은 아닙니다.
2. C-5 Step Definition은 `new TicketService`를 호출하지 않고 주입받은 `ReserveTicketUseCase`만 호출합니다.
3. 동일 Feature를 JPA와 Testcontainers 조합, In-memory 조합에 각각 실행하고 두 GREEN 로그와 Core diff 없음으로 Adapter 교체를 증명합니다.
4. 현재 `InMemoryTicketRepository`는 같은 객체 참조를 Map에 보관합니다. 저장 전 변경 후 `save`가 실패해도 Map 상태가 바뀔 수 있으므로 조회와 저장에서 복사본을 사용하고, 저장 실패 뒤 다시 조회한 티켓이 미예약인지 검증합니다. Domain을 불변 객체로 바꾸는 대안은 더 강하지만 기존 API 변경이 커 이번 범위에서는 선택하지 않았습니다.
5. C-5는 Inbound Port부터 시작하는 Port-level 인수테스트로, C-6은 실제 HTTP Controller부터 시작하는 end-to-end 테스트로 구분합니다.
6. Core가 `adapter`, Spring, JPA 타입을 참조하지 않는지 의존성 테스트나 모듈 컴파일 경계로 자동 확인하고 C-6의 AI 단계별 검수에 사용합니다.
7. C-4 Dockerfile에서 JDK를 고정하고 Compose와 CI도 같은 이미지를 사용합니다. 현재 로컬 `JAVA_HOME`이 Java 8이면 JDK 17 바이트코드를 실행하지 못하는 환경 차이가 실제로 확인됐기 때문입니다.

B-5에서 확인된 결제 성공 후 저장 실패 문제도 Feature에 반영했습니다. 처리 대안은 즉시 결제 취소, 결제 승인과 확정 단계 분리, 현재 결함 유지 세 가지입니다. walking skeleton 범위에서는 `CancelChargePort`로 즉시 보상하는 방식을 목표 설계로 선택했습니다. 이것은 아직 구현 완료가 아닙니다. C-5에서 결제 식별자, 취소 멱등성, Fake와 Adapter 동작을 검증해야 하며 취소 자체가 실패할 때의 재시도와 실패 기록은 추가 결정이 필요합니다.

검증 범위도 분리했습니다. JDK 17을 지정해 기존 B-5에 `mvn clean test`를 실행한 결과 8개 시나리오와 53개 Step이 모두 통과했습니다. 이 결과는 현재 동작이 유지된다는 근거일 뿐, 새 Inbound Port, 결제 보상, Repository 복사 의미, JPA Adapter 교체가 이미 구현되거나 통과했다는 근거는 아닙니다. 후자의 구현과 GREEN 증빙은 C-5와 C-6에서 남겨야 합니다.
