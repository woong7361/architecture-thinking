# Original User Input

헥사고날 아키텍처 관점에서는 어떻게 하는편이 좋아? 웹에서 권장 패턴을 한번 검색해서 알려줘봐


# Checked Context

# 프로젝트 문맥

- 대상은 `task2/assignments/taskB-3.md`의 `TicketService.reserveTicket` 예시다.
- 사용자는 `if (user == null) throw ...`와 `if (!paymentApi.charge(...)) throw ...`가 각 객체의 예외 책임을 정하는 문제인지 물은 뒤, 헥사고날 아키텍처 관점의 권장 패턴을 웹에서 확인해 달라고 했다.
- 현재 예시의 핵심 외부 협력은 사용자 Repository, 티켓 Repository, 결제 API다.
- 사용자 선호 언어와 프레임워크는 Java와 Spring이다.

# 확인한 외부 근거

1. Alistair Cockburn, Hexagonal Architecture original article
   - https://alistair.cockburn.us/hexagonal-architecture
   - 외부 장치별 adapter가 port API와 장치 신호를 양방향 변환한다. DB가 SQL에서 파일로 바뀌어도 application 관점의 대화 API는 바뀌지 않아야 한다.
2. AWS Prescriptive Guidance, Hexagonal architecture pattern
   - https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/hexagonal-architecture.html
   - Port는 기술 중립 인터페이스이고 adapter는 외부 기술과의 교환을 변환한다. Application core는 business logic을 담고 외부 API와 DB 통합 코드로부터 격리한다.
3. Spring Data JPA, Null Handling of Repository Methods
   - https://docs.spring.io/spring-data/data-jpa/reference/4.0/repositories/null-handling.html
   - 단일 aggregate 조회는 부재 가능성을 `Optional`로 표현할 수 있다. wrapper가 없으면 null 반환도 가능하나 명시적 nullability가 필요하다.
4. Microsoft Learn, Designing validations in the domain model layer
   - https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/domain-model-layer-validations
   - Aggregate와 domain entity는 상태 변경 시 invariant를 강제한다. behavior method에서 exception을 던지거나 Specification, Notification을 사용할 수 있다.
5. Martin Fowler, Replacing Throwing Exceptions with Notification in Validations
   - https://martinfowler.com/articles/replaceThrowWithNotification.html
   - 실패가 예상되는 정상 결과라면 exception보다 notification/result가 적합할 수 있다. boolean은 진단 정보를 제공하지 못한다. 적용 여부는 context에 달렸다.
6. Spring Framework, Error Responses
   - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
   - `@ControllerAdvice`, `@ExceptionHandler`, `ProblemDetail`로 예외를 HTTP 응답에 매핑할 수 있다.

# 제약과 불확실성

- Hexagonal Architecture 원문은 예외 타입 배치까지 규정하지 않는다. 구체적인 오류 분류는 Hexagonal의 의존성 원칙과 DDD·공식 프레임워크 지침을 결합한 설계 판단이다.
- 결제 거절이 정상적인 업무 결과인지 예외 상황인지 현재 요구사항만으로 확정할 수 없다.
- HTTP 상태 코드와 보상·재시도 정책은 현재 API 계약과 결제사 계약을 확인해야 확정할 수 있다.
