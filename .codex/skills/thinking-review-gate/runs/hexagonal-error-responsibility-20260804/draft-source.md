결론부터 말하면, 헥사고날 아키텍처에서는 `if`를 없애는 것보다 **외부 표현을 Adapter에서 번역하고, 코어가 소유한 의미 있는 Port 계약만 Application Service에 보이게 하는 것**이 우선입니다. 현재 예제라면 `UserRepository`는 `Optional<User>`를 반환하고, 결제 Port는 `boolean` 대신 `ChargeResult`를 반환하는 혼합형을 추천합니다. 티켓 불변식만 `Ticket`이 직접 예외로 강제합니다.

## 비유

해외 지점에서 보내는 보고서의 코드가 지점마다 다르다고 생각하면 됩니다. 본사는 각 지점의 원시 코드인 `null`, `false`, 문자열 오류 코드를 직접 해석하지 않습니다. 통역관인 Adapter가 본사의 공용 언어로 번역하고, Application Service는 번역된 결과에 따라 업무 순서를 조율합니다. 티켓 자신만 알 수 있는 상태 규칙은 티켓이 직접 지킵니다.

## 왜 이렇게 나누는가

Cockburn의 원문은 Adapter가 Port API와 외부 장치의 신호를 양방향 변환하고, DB 기술이 바뀌어도 Application 관점의 대화가 바뀌지 않아야 한다고 설명합니다. AWS의 공식 지침도 Port를 기술 중립 인터페이스, Adapter를 외부 기술 교환의 변환기로 설명합니다. 따라서 DB의 `null`, Spring의 `EmptyResultDataAccessException`, 결제사의 `boolean`이나 오류 코드를 코어 계약으로 그대로 노출하는 것은 경계를 약하게 만듭니다. [Cockburn 원문](https://alistair.cockburn.us/hexagonal-architecture), [AWS Hexagonal Architecture](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/hexagonal-architecture.html)

다만 Hexagonal Architecture 자체가 예외와 결과 객체 중 하나를 규정하지는 않습니다. 아래 배치는 이 원칙에 DDD의 불변식 책임과 예상 가능한 실패 처리 지침을 결합한 추천입니다.

## 추천 책임 배치

| 상황 | 판단 책임 | 권장 표현 |
| --- | --- | --- |
| 이미 예약된 티켓 | `Ticket` | `TicketAlreadyReservedException` 또는 도메인 결과 |
| 사용자 조회 결과가 없음 | Application Service | `Optional<User>`를 유스케이스의 `UserNotFoundException`으로 변환 |
| 결제사 고유 응답 코드 | Payment Adapter | `ChargeResult` 또는 코어가 정의한 기술 실패로 번역 |
| 결제 거절 후 예매 중단 | Application Service | `Declined` 결과에 따라 흐름 중단 |
| HTTP 상태와 응답 본문 | Web Adapter | `@RestControllerAdvice`에서 `ProblemDetail`로 변환 |

Microsoft의 DDD 지침은 aggregate와 domain entity가 상태 변경 중 불변식을 강제해야 한다고 설명합니다. 그러므로 `Ticket`이 자신의 `reserved` 상태를 검사하는 것은 자연스럽습니다. 반면 존재하지 않는 `User`는 예외를 던질 객체 자체가 없고, 결제 거절은 `Ticket`의 내부 상태가 아닙니다. [Microsoft Domain Model Validation](https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/domain-model-layer-validations)

## 권장 코드 형태

### 코어가 소유하는 Output Port

```java
public interface LoadUserPort {
    Optional<User> findById(UserId userId);
}

public interface ChargePaymentPort {
    ChargeResult charge(PaymentCommand command);
}

public sealed interface ChargeResult permits Approved, Declined {
}
```

Spring Data도 단일 aggregate의 부재 가능성을 `Optional`로 표현할 수 있다고 공식적으로 안내합니다. 중요한 점은 Spring Data Repository 타입을 코어에 그대로 노출하라는 뜻이 아니라, 코어가 정의한 `LoadUserPort`를 JPA Adapter가 구현하도록 하는 것입니다. [Spring Data null 처리](https://docs.spring.io/spring-data/data-jpa/reference/4.0/repositories/null-handling.html)

### Application Service

```java
@Transactional
public void reserveTicket(ReserveTicketCommand command) {
    User user = loadUserPort.findById(command.userId())
        .orElseThrow(() -> new UserNotFoundException(command.userId()));

    Ticket ticket = loadTicketPort.findById(command.ticketId())
        .orElseThrow(() -> new TicketNotFoundException(command.ticketId()));

    ticket.ensureReservable();

    ChargeResult result = chargePaymentPort.charge(
        new PaymentCommand(command.paymentInfo(), ticket.price()));

    if (result instanceof Declined declined) {
        throw new PaymentDeclinedException(declined.reason());
    }

    ticket.assignTo(user.id());
    saveTicketPort.save(ticket);
}
```

여기의 `if`는 외부 결제사의 `boolean`을 해석하는 조건문이 아니라, 코어가 정의한 유스케이스 결과에 따라 다음 단계를 선택하는 조율입니다. Application Service는 얇아야 하지만 순서와 성공·실패 흐름까지 없어야 하는 것은 아닙니다. 반대로 할인 계산이나 예약 자격 판단 같은 업무 규칙이 이 `if`에 들어가면 도메인 정책 또는 엔티티로 이동해야 합니다.

### Payment Adapter

```java
public final class PgChargeAdapter implements ChargePaymentPort {
    private final PgClient pgClient;

    @Override
    public ChargeResult charge(PaymentCommand command) {
        try {
            PgResponse response = pgClient.charge(command);
            return response.approved()
                ? new Approved(response.transactionId())
                : new Declined(response.declineReason());
        } catch (PgTimeoutException e) {
            throw new PaymentGatewayUnavailableException(e);
        }
    }
}
```

Adapter는 결제사의 응답 형식과 예외를 알고 번역합니다. `Declined`가 예매를 중단시킨다는 업무 의미와 그다음 순서는 Application Service가 결정합니다. 네트워크 timeout 같은 기술 실패는 코어가 정의한 Port 계약의 예외로 번역하므로 Application Service가 특정 PG SDK 예외에 의존하지 않습니다.

### Web Adapter

```java
@RestControllerAdvice
class ReservationExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handle(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```

도메인·애플리케이션 예외에 `@ResponseStatus`를 직접 붙이지 않고 Web Adapter에서 HTTP로 번역하면 코어가 Spring MVC에 의존하지 않습니다. Spring Framework는 `@ControllerAdvice`와 `ProblemDetail`을 이용한 중앙 오류 응답 매핑을 공식 지원합니다. [Spring Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

## 세 가지 선택지

1. **업무 거절은 Result, 기술 장애와 불변식 위반은 예외 — 추천**
   - 얻는 것: 예상 가능한 결제 거절과 비정상적인 timeout을 구분하고, `boolean`보다 실패 이유를 보존합니다.
   - 비용: `ChargeResult` 계층과 분기 코드가 추가됩니다.
   - 적합 조건: 결제 거절이 정상적으로 자주 발생하고 거절 사유별 처리가 발전할 가능성이 있을 때 적합합니다.

2. **Port가 성공값을 반환하고 모든 실패를 예외로 알림**
   - 예: `PaymentReceipt charge(...)`가 성공 시 영수증을 반환하고 거절·장애 시 예외를 던집니다.
   - 얻는 것: Application Service가 짧고 성공 흐름이 선명합니다.
   - 비용: 정상적으로 예상되는 결제 거절까지 예외 제어 흐름이 됩니다.
   - 적합 조건: 실패 후 분기 없이 유스케이스를 즉시 중단하고 실패 종류가 적을 때 적합합니다.

3. **모든 실패를 Result로 반환**
   - 얻는 것: 가능한 결과가 타입에 드러나고 예외 없는 명시적 흐름을 만들 수 있습니다.
   - 비용: DB 장애나 timeout까지 모든 호출부가 분기해야 해 조립 코드가 무거워질 수 있습니다.
   - 적합 조건: 함수형 스타일과 exhaustive matching을 팀 표준으로 쓰고 실패를 값으로 일관되게 다룰 때 적합합니다.

Fowler는 예상 가능한 실패라면 예외보다 Notification이나 결과가 적합할 수 있고, `boolean`은 진단 정보를 주지 못한다고 설명합니다. 다만 예외 여부는 문맥에 달렸다고 명시합니다. 이 기준을 결제에 적용하면 거절은 `ChargeResult`, timeout은 예외로 나누는 혼합형이 현재 예제에 가장 설명력이 좋습니다. 이는 결제에 대한 직접 규정이 아니라 해당 원칙을 적용한 설계 판단입니다. [Fowler의 Notification 글](https://martinfowler.com/articles/replaceThrowWithNotification.html)

## 최종 권장 흐름

```text
Web Adapter
  → ReserveTicketUseCase
    → LoadUserPort           → JPA Adapter
    → LoadTicketPort         → JPA Adapter
    → Ticket.ensureReservable()
    → ChargePaymentPort      → PG Adapter
    → Ticket.assignTo()
    → SaveTicketPort         → JPA Adapter
  ← 도메인·애플리케이션 결과/예외
Web Adapter가 HTTP ProblemDetail로 변환
```

따라서 앞선 질문에 대한 답은 다음과 같습니다.

> 헥사고날 관점에서도 `if`는 책임 배치를 드러내는 신호가 맞습니다. 하지만 모든 `throw`를 각 도메인 엔티티로 옮기지는 않습니다. 외부 형식의 해석은 Adapter, 유스케이스의 순서와 부재·거절에 대한 반응은 Application Service, 객체 자신의 불변식은 Domain Entity, HTTP 변환은 Web Adapter가 맡는 구분이 좋습니다. Application Service에 `if`가 남더라도 코어가 정의한 결과를 조율하는 분기라면 헥사고날 원칙에 어긋나지 않습니다.

별도로, 이 구조만으로 외부 결제와 DB 저장의 원자성이 해결되지는 않습니다. 결제 성공 후 저장 실패를 처리하려면 결제 승인·확정 분리, 멱등성, 보상 또는 상태 기반 workflow를 별도로 설계해야 합니다.
