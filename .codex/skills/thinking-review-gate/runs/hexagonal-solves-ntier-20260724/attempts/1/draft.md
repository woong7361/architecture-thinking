# 결론

앞선 비평의 핵심은 `Controller → Service → Repository`라는 모양 자체가 아니라 **Domain Policy가 HTTP와 JPA 같은 외부 기술을 향해 의존한다는 것**이었다.

헥사고날은 계층을 더 추가해서 이 문제를 해결하지 않는다. Application Core가 자신의 입출력 계약인 Port를 소유하게 하고, HTTP·JPA 같은 기술 코드가 Adapter로서 그 계약을 향해 의존하도록 **소스 코드 의존 방향을 뒤집는다**.

```text
기존 N-Tier

Controller ──▶ Service ──▶ JpaRepository ──▶ DB
                          └─▶ JPA @Entity

헥사고날

HTTP Adapter ──▶ Inbound Port ──▶ Application Service ──▶ Domain
                                              │
                                              ▼
                                        Outbound Port
                                              ▲
                                              │ implements
                                         JPA Adapter ──▶ DB
```

화살표는 런타임 데이터 흐름이 아니라 Compile-time 소스 코드 의존성이다. JPA Adapter는 Application이 정의한 Outbound Port를 구현하기 때문에 안쪽을 향해 의존한다. Application은 JPA Adapter 클래스를 import하지 않는다.

## 비유

Port는 USB 규격이고 Adapter는 USB 규격을 HDMI나 저장장치 기술에 맞게 변환하는 장치다.

컴퓨터의 핵심 기능이 특정 HDMI 제조사의 구현에 직접 의존하면 장치를 바꿀 때 컴퓨터 내부도 바꿔야 한다. 컴퓨터가 USB라는 목적 중심 계약을 정하고 각 장치가 그 계약에 맞는 Adapter를 제공하면 내부는 외부 장치의 구체 기술을 몰라도 된다.

헥사고날의 Port도 `HTTP 요청을 처리한다`, `JPA로 저장한다`처럼 기술로 정의하지 않는다. `티켓을 예약한다`, `티켓을 불러온다`처럼 Application이 수행하거나 요구하는 목적을 표현한다. Cockburn은 Port를 기술 종류가 아니라 목적 있는 대화로 설명하고, 같은 Port에 HTTP, Batch, Test Harness, SQL, In-memory Adapter가 연결될 수 있다고 설명한다. [Alistair Cockburn, Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture)

## 앞선 비평과 해결 장치의 일대일 대응

| 앞선 비평 | 헥사고날의 해결 장치 | 의존성 변화 | 얻는 효과 |
| --- | --- | --- | --- |
| Service가 `HttpServletRequest`와 헤더를 직접 읽는다 | HTTP Adapter가 요청을 업무 입력으로 변환하고 Inbound Port를 호출한다 | HTTP → Inbound Port | HTTP, Batch, Message가 같은 유스케이스를 재사용한다 |
| Service가 `JpaRepository`를 직접 사용한다 | Application Core가 Outbound Port를 정의하고 JPA Adapter가 구현한다 | JPA Adapter → Outbound Port | Domain/Application 테스트에서 JPA를 Fake로 교체할 수 있다 |
| JPA `@Entity`가 Service와 Controller까지 흐른다 | Persistence Adapter가 JPA Model과 Domain Model을 변환한다 | JPA Model이 Adapter 밖으로 나오지 않는다 | 테이블 매핑 변경이 Domain과 API로 전파되는 범위를 줄인다 |
| Controller가 JPA Entity를 API 응답으로 반환한다 | HTTP Adapter가 Use Case 결과를 Response DTO로 변환한다 | API Model이 Inbound Adapter에 머문다 | API와 DB 스키마가 서로의 공용 계약이 되지 않는다 |
| 업무 테스트에도 Spring, DB, 트랜잭션이 필요하다 | Inbound Port를 직접 호출하고 Outbound Port에 Fake Adapter를 연결한다 | Test Adapter → Port | 핵심 규칙을 빠른 단위·유스케이스 테스트로 검증한다 |

## 1. Inbound Port가 해결하는 것

앞선 비평의 입력 측 문제는 Service가 `HttpServletRequest`에서 `X-Customer-Grade`를 읽는 것이었다.

헥사고날에서는 HTTP Controller가 Inbound Adapter다. Controller가 다음 변환을 담당한다.

```text
HttpServletRequest
    ↓ HTTP Adapter가 해석
ReserveTicketCommand(UserId, CustomerGrade, TicketId)
    ↓
ReserveTicketUseCase라는 Inbound Port 호출
```

Application Core는 Servlet API를 모른다. Batch Adapter나 Message Consumer도 동일한 Inbound Port를 호출할 수 있다.

Inbound Port는 `ReserveTicketUseCase`처럼 Application이 외부에 제공하는 유스케이스 계약이다. Java에서는 Controller와 Batch가 같은 계약을 사용하고 구현을 대체할 수 있게 보통 `interface`로 표현한다. 그러나 Hexagonal의 Port가 개념적으로 반드시 Java `interface`여야 하는 것은 아니다. 중요한 것은 기술과 무관한 안정된 계약이라는 점이다.

Inbound Port는 Domain Entity나 Value Object 그 자체라기보다 **Application Core의 경계**에 속한다. 계층을 Domain과 Application으로 엄격히 나누면 보통 Application Layer에 둔다. 다만 계약에 사용하는 입력과 출력은 `HttpServletRequest`나 `ResponseEntity`가 아니라 업무 언어를 사용해야 한다.

## 2. Outbound Port가 해결하는 것

앞선 비평의 출력 측 문제는 Service가 Spring Data `JpaRepository`를 직접 사용하고 JPA Entity의 생명주기까지 알게 된 것이었다.

헥사고날에서는 안쪽이 자신에게 필요한 기능을 업무 의미로 정의한다.

```text
Application이 요구하는 계약
    LoadTicketPort.load(TicketId) → Ticket
    SaveTicketPort.save(Ticket)

외부 구현
    JpaTicketAdapter implements LoadTicketPort, SaveTicketPort
```

여기서 DIP가 작동한다.

기존에는 높은 수준의 예약 정책이 낮은 수준의 JPA 구현을 import했다.

```text
Reservation Policy ──▶ JpaRepository
```

바뀐 구조에서는 높은 수준의 Application Core가 추상 계약을 소유하고, 낮은 수준의 JPA Adapter가 그 계약을 구현한다.

```text
Reservation Policy ──▶ SaveTicketPort ◀── JPA Adapter
                                      ◀── In-memory Adapter
```

Robert C. Martin의 DIP는 높은 수준 정책과 낮은 수준 세부 구현이 모두 추상화에 의존하고, 세부 구현이 추상화 쪽을 향해 의존한다고 설명한다. [Robert C. Martin, Dependency Inversion Principle](https://objectmentor.com/resources/articles/dip.pdf)

Outbound Port가 안쪽에 있는 이유는 **외부 기술이 제공하는 API를 복사하기 위해서가 아니라, 안쪽 정책이 무엇을 필요로 하는지를 안쪽 언어로 선언하기 위해서**다.

따라서 다음 Port는 나쁘다.

```text
JpaTicketPort.saveAndFlush(JpaTicketEntity)
```

이름과 타입에 JPA 세부가 이미 들어왔기 때문에 인터페이스를 만들었어도 의존성은 의미상 뒤집히지 않았다.

다음처럼 업무 필요를 표현해야 한다.

```text
SaveTicketPort.save(Ticket)
```

## 3. Adapter가 인프라인 이유

Adapter는 Port와 특정 기술 사이를 변환하는 구현체다.

- HTTP Adapter: HTTP 요청과 응답을 Use Case 입력과 출력으로 변환한다.
- JPA Adapter: Domain Model과 JPA Model을 변환하고 DB에 저장한다.
- External API Adapter: 외부 API DTO와 Domain이 요구한 결과를 변환한다.
- Test Adapter: DB나 외부 서비스 대신 메모리에서 Port를 구현한다.

이 코드는 업무 규칙보다 기술이 바뀌는 이유로 변경된다. JSON 형식, Spring MVC, JPA 매핑, SQL, 외부 API SDK가 변경 원인이다. 그래서 Application Core 밖인 Infrastructure에 둔다.

Adapter가 구현체인 이유는 기술마다 구현이 달라지기 때문이다. 같은 `SaveTicketPort`를 JPA, JDBC, In-memory Adapter가 각각 구현할 수 있다. Core는 어느 구현이 연결되는지 모르며, 조립은 Application 시작 지점이나 Spring Configuration이 담당한다.

## 4. `의존성은 안쪽을 향한다`의 정확한 뜻

런타임 호출은 안쪽에서 바깥쪽으로 나갈 수 있다.

```text
Application Service → saveTicketPort.save(ticket)
```

하지만 Application Service가 아는 것은 Port뿐이다. 실제로 JPA Adapter가 실행된다는 사실은 Composition Root가 연결한다.

```text
Runtime call:
Application → Port → JPA Adapter → DB

Compile-time dependency:
HTTP Adapter ─┐
JPA Adapter ──┼─▶ Application Core의 Port와 Domain
Test Adapter ─┘
```

따라서 `안쪽을 향한다`는 말은 모든 메서드 호출이 안쪽으로만 흐른다는 뜻이 아니다. **소스 코드 import와 모듈 의존성이 안정적인 업무 정책 쪽을 향한다는 뜻**이다.

## Testability와 Flexibility가 실제로 어떻게 바뀌는가

### Testability

기존 테스트는 `JpaRepository`, Hibernate Proxy, Spring Context, DB가 필요할 수 있었다.

헥사고날에서는 다음 구성으로 유스케이스를 테스트할 수 있다.

```text
Test Driver
    → ReserveTicketUseCase
    → ReservationApplicationService
    → InMemoryTicketAdapter
```

Domain과 유스케이스 테스트는 HTTP와 JPA 없이 실행된다. 별도로 HTTP Adapter 테스트와 JPA Adapter 통합 테스트를 둔다. 테스트가 사라지는 것이 아니라 핵심 정책 테스트와 기술 통합 테스트가 분리된다.

### Flexibility

- HTTP를 Batch나 Message Consumer로 바꿔도 Inbound Port는 유지된다.
- JPA를 JDBC나 외부 저장 서비스로 바꿔도 Outbound Port는 유지된다.
- API 응답과 DB Entity를 분리하면 테이블 변경이 API 계약까지 자동 전파되지 않는다.

다만 교체 비용이 0이 되는 것은 아니다. Port의 의미 자체가 바뀌거나 새 저장 기술이 기존 일관성 요구를 지원하지 않으면 Core와 Port도 수정해야 한다. 헥사고날이 줄이는 것은 모든 변경 비용이 아니라 **기술 세부 변경의 전파 범위**다.

## 헥사고날이 자동으로 해결하지 못하는 것

- Port에 `HttpServletRequest`, `JpaEntity`, `Pageable` 같은 기술 타입을 넣으면 누수는 그대로다.
- Domain 규칙이 빈약하거나 잘못 모델링되면 Port를 추가해도 업무 모델은 좋아지지 않는다.
- 모든 클래스마다 인터페이스를 만들면 Port가 아니라 추상화 보일러플레이트가 된다.
- Domain과 JPA Model을 분리하면 Mapper와 중복 모델 비용이 생긴다.
- 트랜잭션 경계, Domain Event 발행, 재시도와 동시성은 별도로 설계해야 한다.
- 단순 CRUD에서는 전면적인 Port와 Adapter 구조가 N-Tier보다 비쌀 수 있다.

## 수행 내용 3의 답안 구조 제안

다음 순서로 작성하면 앞선 답안과 인과가 이어진다.

1. 비평 재진술: 문제는 계층 수가 아니라 Domain Policy가 HTTP와 JPA를 향해 의존하는 방향이다.
2. DIP 원리: Core가 추상 계약을 소유하고 Adapter가 그 계약을 구현하게 의존성을 역전한다.
3. Inbound Port: HTTP 요청을 업무 입력으로 번역하고 유스케이스를 기술과 분리한다.
4. Outbound Port: 저장과 외부 연동에 대한 업무 요구를 안쪽 언어로 선언한다.
5. Adapter: HTTP, JPA, 외부 API를 Port와 변환하는 바깥 구현체다.
6. 효과: 핵심 정책은 Fake Adapter로 테스트하고 기술 교체의 변경 전파를 Adapter로 제한한다.
7. 한계: Mapper와 추상화 비용이 있으며 잘못 설계한 Port는 누수를 막지 못한다.

이 방향이 승인되면 `taskC-1.md`의 답안 2 뒤에 헥사고날과 DIP 부분을 이어 쓰고, 두 번째 제출물 체크리스트를 완료할 수 있다.

## 레퍼런스

- Alistair Cockburn, [Hexagonal Architecture, original 2005 article](https://alistair.cockburn.us/hexagonal-architecture)
- Robert C. Martin, [The Dependency Inversion Principle](https://objectmentor.com/resources/articles/dip.pdf)
