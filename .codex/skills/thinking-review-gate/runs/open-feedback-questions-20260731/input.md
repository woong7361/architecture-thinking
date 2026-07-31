# Original User Input

열린 질문은 

어떤 내용에 대해 어떤 문제점이 있고, 그 해결 방안 or 다른 해결책 2-3가지와 어떤 트레이드 오프가 있는지 알아봐줘


# Checked Context

# 요청 해석

직전 대화에서 확인한 열린 질문을 모두 대상으로 삼는다.

- B-0 직접 피드백의 열린 질문 2개
  1. 테스트 단언을 시스템 외부 관측 결과까지 밀어내면 테스트 수명과 리팩토링 자유도가 높아지는가.
  2. 혼자 오래 끄는 습관을 바꾸기 위해 짧은 버전 공유 외에 어떤 방법이 있는가.
- 제출 페이지 전체 총평의 열린 질문 3개
  3. 미시적 설계를 자동화한 뒤 인간 개발자가 책임져야 할 거시적·비즈니스 결단은 무엇인가.
  4. 기존 도메인 지식과 테스트가 기술 부채가 될 정도의 패러다임 변화에서 경계를 어떻게 다시 그릴 것인가.
  5. 풍부한 도메인 모델의 협력 반경이 비대해질 때 언제 분산된 컨텍스트로 분리할 것인가.

# 프로젝트 근거

## 테스트 경계

- `task2/assignments/taskB-0.md:59-66`: Gherkin은 유지됐지만 step glue가 `setReserved`와 `PaymentApi`에 의존해 구조 변경 때 컴파일이 깨졌다고 기록한다.
- `task2/task5-history/src/test/resources/features/ticket_reservation.feature:1-6`: 시나리오는 상호작용이 아니라 예약·청구 상태와 결과를 단언하지만, 널 참조와 결제 후 저장 실패 시 미보상 같은 기존 quirk도 박제한다.
- `task2/task5-history/src/test/java/com/thinking/ticket/steps/TicketReservationSteps.java:56-66`: 이미 예약된 상태는 setter 대신 `reserveTicket` 유스케이스로 만든다.
- 같은 파일 `112-122`: 단언은 저장소에서 `Ticket`을 꺼내 getter로 상태를 읽고 `RecordingPaymentApi`의 기록을 읽는다. 따라서 Gherkin 문장은 안정적이어도 실행 어댑터는 내부 저장소·타입에 여전히 결합된다.
- `task2/assignments/taskB-7.md:123-129`: 인수 테스트가 모두 통과해도 규칙이 서비스에 잘못 놓이는 구조 차이는 잡지 못했다고 기록한다.

## 자동화와 인간 판단

- `task2/assignments/taskB-7.md:242-248`: 규칙이 판정하는 것은 규칙에 맡기고, 규칙으로 환원되지 않는 협력과 책임에는 설계가 필요하다고 결론낸다.
- `task2/task7-history/report.md:303-316`: 포트 3개에 145줄을 지불해 벤더 변경 4줄을 아꼈으며, 이 결론은 작은 카타 규모에 한정된다고 기록한다.
- `task2/task7-history/report.md:335-353`: 테스트는 구조상 규칙이 어디에 놓이는지 잡지 못하며, 셀 수 있는 판단 기준이 정성적 설계 지시보다 일관된 결과를 냈다고 기록한다.

## 기존 안전망의 부채화

- `ticket_reservation.feature:41-55`: 널 참조와 결제 미보상을 명시적으로 기존 quirk로 보존한다. 새로운 비즈니스 정책이 정상 오류 처리나 보상을 요구하면 이 테스트는 보호해야 할 불변식이 아니라 폐기·교체할 대상이 된다.

## 컨텍스트 분리

- `task2/task7-history/report.md:315-316`: 현재 카타는 포트가 본전을 뽑기에도 너무 작다고 한계를 명시한다. 현재 프로젝트에 즉시 마이크로서비스나 분산 컨텍스트 분리를 권할 근거는 없다.

# 외부 근거

- Alistair Cockburn, Hexagonal Architecture: https://alistair.cockburn.us/hexagonal-architecture
  - 유스케이스와 기능 명세는 외부 기술이 아니라 애플리케이션 경계의 인터페이스에 대해 작성해야 더 안정적이라고 설명한다.
  - 테스트 하네스와 인메모리 저장소는 포트에 연결되는 어댑터가 될 수 있다.
- Martin Fowler, Refactoring: https://www.martinfowler.com/books/refactoring.html
  - 작은 행위 보존 변환으로 위험을 줄이는 리팩토링을 설명한다.
- Google Engineering Practices, Small CLs: https://google.github.io/eng-practices/review/developer/small-cls.html
  - 작은 변경은 더 빠르고 깊게 리뷰되며 방향이 틀렸을 때 낭비가 작다. 다만 페어 프로그래밍, 즉시 가능한 리뷰어, 수직 분할 등 리뷰 대기를 줄이는 별도 방법도 제시한다.
- Martin Fowler, Branch By Abstraction: https://martinfowler.com/bliki/BranchByAbstraction.html
  - 낡은 구현과 새 구현을 추상화 뒤에 공존시키고 점진적으로 교체해 대규모 변화를 진행 중에도 시스템을 동작 가능하게 유지한다.
- Microsoft Learn, Tactical DDD: https://learn.microsoft.com/en-ca/azure/architecture/microservices/model/tactical-domain-driven-design
  - 시스템 전체의 단일 모델 대신 bounded context별 모델을 권하며, 마이크로서비스는 aggregate보다 작지 않고 bounded context보다 크지 않게 설계하는 일반 원칙을 제시한다.
- AWS Prescriptive Guidance, Decompose by business capability: https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-decomposing-monoliths/decompose-business-capability.html
  - 안정된 비즈니스 역량 기반 분해는 느슨한 결합과 비즈니스 가치 중심 팀을 만들지만, 깊은 비즈니스 이해가 필요하고 설계가 비즈니스 모델에 결합된다는 trade-off가 있다.
- Microsoft Learn, Microservices Assessment and Readiness: https://learn.microsoft.com/en-us/azure/architecture/guide/technology-choices/microservices-assessment
  - 경계가 적고 의존성이 낮은 부분부터 Strangler Fig와 Anti-Corruption Layer로 점진 분해하고, 데이터 동기화·소유권·운영 역량 비용을 함께 평가하라고 권한다.

# 답변 제약

- 각 질문마다 대상 내용, 문제점, 대안 2~3개, trade-off, 현재 프로젝트 추천을 명시한다.
- 프로젝트에서 확인된 사실, 외부 근거, 작성자 해석을 구분한다.
- 마이크로서비스 분리를 큰 객체나 라인 수만으로 권하지 않는다.
- 현재 프로젝트는 작은 카타라는 한계를 반영한다.
- 구현 변경은 하지 않는다.
