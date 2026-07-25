## 답안 3: 헥사고날이 앞선 비평을 해결하는 방식

### 헥사고날이란

헥사고날 아키텍처는 애플리케이션을 업무 규칙과 유스케이스가 있는 안쪽과 HTTP, 데이터베이스, 메시지 브로커 같은 기술이 있는 바깥쪽으로 나누는 방식이다. 핵심은 육각형 모양이나 여섯 개의 면이 아니라, 안쪽과 바깥쪽이 대화하는 경계를 명시하는 데 있다.

이 경계의 계약이 Port이고, 특정 기술로 Port에 연결되는 번역기가 Adapter다. 예를 들어 `ReserveTicketUseCase`는 티켓 예약이라는 업무 입력을 받는 Inbound Port다. `SaveTicketPort`는 예약 결과를 저장해 달라는 Application Core의 요구를 표현한 Outbound Port다. HTTP 요청을 예약 명령으로 바꾸는 `TicketReservationHttpAdapter`와 Domain `Ticket`을 JPA Entity로 바꾸어 저장하는 `TicketJpaAdapter`는 각각의 Adapter다.

Port는 보통 Java `interface`로 표현한다. 그러나 헥사고날의 본질은 인터페이스 문법 자체가 아니라 계약의 소유권과 사용하는 언어다. Port는 `HttpServletRequest`, `JpaEntity`, `saveAndFlush` 같은 기술 세부가 아니라 `ReserveTicketCommand`, `Ticket`, `save`처럼 유스케이스에 필요한 업무 개념을 사용해야 한다. Inbound Port는 DDD Entity 자체가 아니라 Application의 유스케이스 경계다. Outbound Port도 Entity나 Value Object라기보다 Application Core가 외부에 요구하는 업무 목적의 계약이다.

### DIP로 의존 방향을 뒤집는다

앞선 N-Tiered 예시의 문제는 Service가 `JpaRepository`와 `HttpServletRequest`를 직접 알아야 한다는 점이었다. 업무 정책이 바깥 기술을 향해 소스 코드 의존성을 가졌기 때문에 기술 변경이 업무 코드와 테스트까지 전파됐다.

헥사고날은 DIP를 적용해 Application Core가 필요한 Port를 안쪽에 정의하고, 바깥 Adapter가 그 Port에 의존하게 만든다. 다음 그림은 import와 구현 관계를 포함한 소스 코드 의존성이다.

```text
TicketReservationHttpAdapter =====> ReserveTicketUseCase
ReservationApplicationService ===> LoadTicketPort, SaveTicketPort
TicketJpaAdapter =================> LoadTicketPort, SaveTicketPort, Domain Ticket
```

`ReservationApplicationService`는 `TicketJpaAdapter`를 직접 import하지 않는다. 반대로 `TicketJpaAdapter`가 안쪽의 `SaveTicketPort`를 구현하고 Domain `Ticket`을 사용한다. 이것이 `의존성은 안쪽을 향한다`는 뜻이다. 런타임에는 Application Service가 주입된 JPA Adapter를 호출하지만, 실행 순서와 소스 코드 의존 방향은 서로 다른 개념이다.

### 티켓 예약 예시

`POST /tickets/T-100/reservations` 요청에 사용자 `U-10`이 들어왔다고 하자. 런타임 동작은 다음 순서로 진행된다.

1. `TicketReservationHttpAdapter`가 URL과 헤더를 `ReserveTicketCommand`로 변환한다.
2. Inbound Port인 `ReserveTicketUseCase`를 통해 `ReservationApplicationService`를 호출한다.
3. Application Service가 `LoadTicketPort`로 `Ticket`을 조회한다.
4. Domain `Ticket`의 `reserveBy(UserId)`가 판매 중지와 중복 예약 규칙을 검사한다.
5. 규칙을 통과한 `Ticket`을 `SaveTicketPort`에 전달한다.
6. 주입된 `TicketJpaAdapter`가 Domain `Ticket`을 JPA Entity로 변환하고 DB에 저장한다.

여기서 `SaveTicketPort`와 `TicketJpaAdapter`는 같은 역할이 아니다. `SaveTicketPort`는 안쪽이 선언한 `save(Ticket)` 저장 계약이다. 저장 기술을 모르며, 왜 저장이 필요한지만 드러낸다. `TicketJpaAdapter`는 바깥쪽 구현체다. JPA Entity 조회와 변환, Spring Data Repository 호출처럼 어떻게 저장할지를 담당한다. 테스트에서는 같은 Port를 구현하는 `InMemoryTicketAdapter`를 대신 주입할 수 있다.

### 앞선 비평이 어떻게 해결되는가

HTTP Adapter가 `HttpServletRequest`를 업무 값으로 변환하므로 Domain과 Application Service는 Servlet API를 모른다. JPA Adapter가 Domain Model과 JPA Entity 사이를 변환하므로 Domain의 상태와 행위가 영속성 생명주기나 지연 로딩을 직접 전제로 하지 않아도 된다. 따라서 판매 중지 티켓을 거절하는 테스트는 HTTP 서버와 DB 없이 Domain 객체나 In-memory Adapter로 실행할 수 있다. JPA 매핑, 트랜잭션, 실제 쿼리는 별도의 Adapter 통합 테스트에서 검증한다.

변경의 영향도 경계에 모인다. HTTP를 배치 입력으로 바꾸면 새 Inbound Adapter를 추가하고, JPA를 JDBC로 바꾸면 같은 Outbound Port를 구현하는 새 Adapter를 만들 수 있다. 다만 Port의 계약 자체가 바뀌거나 업무 규칙이 바뀌면 Core도 수정해야 한다. 헥사고날은 모든 변경을 없애는 방식이 아니라 기술 변경이 업무 정책으로 불필요하게 번지는 범위를 줄이는 방식이다.

### Trade-off와 적용 기준

Port, Adapter, Domain Model과 영속성 모델 사이의 Mapper를 두면 클래스와 변환 코드가 늘어난다. Port 이름만 업무 용어로 바꾸고 매개변수에 JPA Entity나 HTTP 타입을 남기면 기술 의존성도 그대로 남는다. 또한 헥사고날이 잘못된 도메인 모델을 자동으로 바로잡아 주지는 않는다.

업무 규칙이 복잡하고 외부 기술 교체 가능성이나 빠른 단위 테스트의 가치가 크다면 헥사고날의 비용을 감수할 이유가 충분하다. 기존 계층형 구조에서도 Service가 기술 중립 DTO를 받고 Repository 인터페이스를 안쪽에 두는 방식으로 DIP를 부분 적용할 수 있다. 반대로 단순 CRUD이고 기술 변경 가능성이 낮다면 Controller, Service, Repository 구조가 더 경제적일 수 있다. 중요한 판단 기준은 헥사고날이라는 이름을 채택했는지가 아니라, 보호할 업무 규칙의 가치가 추가 경계와 매핑 비용보다 큰지다.

### 참고

- [Alistair Cockburn, Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture)
- [Robert C. Martin, The Dependency Inversion Principle](https://objectmentor.com/resources/articles/dip.pdf)
