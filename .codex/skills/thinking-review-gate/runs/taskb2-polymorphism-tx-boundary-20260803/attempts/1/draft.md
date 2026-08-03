# 결론

현재 B-2 수준에서는 `ReservationFlow`까지 나누는 것은 과할 가능성이 크다. 우선 하나의 `TicketReservationService`가 공통 예매 흐름을 소유하고 `PaymentMethod`만 다형화하는 편이 낫다. 결제 방식에 따라 실행 순서, 완료 상태, 실패 복구가 실제로 달라져 공통 서비스가 계속 분기될 때만 유스케이스 전체를 별도 flow로 분리한다.

또한 workflow로 바꾼다고 transaction boundary가 자동으로 생기지 않는다. workflow는 업무 순서와 실패 처리를 표현하며 경계를 소유할 자리를 제공할 뿐이다. 실제 경계는 함께 원자적으로 바뀌어야 하는 데이터와 참여 가능한 transaction resource를 확인한 뒤 정한다.

## 이렇게까지 나눠야 하는가

항상 그렇지는 않다. 앞서 제안한 `ReservationFlow`는 얇은 `PaymentMethod`로는 카드와 포인트의 차이를 표현할 수 없을 정도로 실행 의미가 달라졌을 때 쓰는 상한선에 가깝다.

현재 task에서 먼저 적용할 구조는 다음과 같다.

```java
public interface PaymentMethod {
    PaymentResult pay(PaymentRequest request);
}

public final class TicketReservationService {
    private final PaymentMethodResolver paymentMethods;

    public ReservationResult reserve(ReservationCommand command) {
        User user = userRepository.findById(command.userId());
        Ticket ticket = ticketRepository.findById(command.ticketId());

        PaymentMethod method = paymentMethods.resolve(command.paymentType());
        PaymentResult payment = method.pay(
                new PaymentRequest(user.id(), ticket.price(), command.paymentInfo())
        );

        ticket.reserve(user.id());
        ticketRepository.save(ticket);
        return ReservationResult.confirmed(ticket.id(), payment.id());
    }
}
```

카드와 포인트가 모두 “금액을 지불하고 성공 또는 실패 결과를 돌려준다”는 동일한 계약을 지킬 수 있다면 이 정도 다형성으로 충분하다. 공통 조회, 예약 규칙, 저장은 한 서비스에 남아 중복되지 않는다.

### 이 구조의 장점

- 결제수단별 기술과 차감 방식이 구현체에 국소화된다.
- 예매 유스케이스의 공통 흐름을 한곳에서 읽을 수 있다.
- 새 결제수단은 `PaymentMethod` 구현 추가로 받을 수 있다.
- `Ticket`은 결제 기술을 모르고 예약 불변식만 지킨다.

### 한계

이 구조는 다형성을 제공하지만 atomicity를 해결하지는 않는다. 포인트 차감과 티켓 저장이 같은 transaction에 참여할 수 있는지, 카드 승인 뒤 티켓 저장 실패를 어떻게 복구할지는 별도 일관성 설계다.

## OOP의 장점이 사라지는가

`ReservationFlow`로 나눈다고 OOP의 장점이 사라지는 것은 아니다. 각 구현이 서로 다른 협력과 실패 정책을 캡슐화한다는 장점은 남는다. 하지만 두 flow의 대부분이 같은 조회·검증·저장 코드라면 중복과 간접 계층이 커져 현재 규모에서 얻는 이익보다 비용이 커질 수 있다.

OOP의 목적은 가능한 한 큰 단위를 모두 다형화하는 것이 아니다. 실제로 함께 변하는 책임을 한곳에 묶고, 다른 변경 이유를 분리하는 것이다. 카드와 포인트의 차이가 결제 단계에만 머물면 `PaymentMethod`가 올바른 경계다. 다음 차이가 나타나면 경계를 다시 검토한다.

- 카드만 `PENDING`을 반환하고 포인트는 즉시 `CONFIRMED`된다.
- 카드 흐름은 외부 승인 뒤 보상이 필요하고 포인트는 로컬 rollback으로 끝난다.
- 결제 방식에 따라 티켓 잠금과 저장 순서가 달라진다.
- 공통 서비스에 `if card`, `if point`가 반복된다.

이 신호가 실제로 생겼을 때 `ReservationFlow`로 올리는 것이 YAGNI에 맞다.

## workflow로 가면 transaction boundary를 바로 세울 수 있는가

아니다. workflow와 transaction은 다른 문제다.

- workflow는 어떤 단계를 어떤 순서로 수행하고 실패 시 무엇을 할지 표현한다.
- transaction boundary는 어떤 상태 변경을 하나의 commit 또는 rollback 단위로 묶을지 정한다.

transaction 경계를 정하려면 다음을 먼저 확인해야 한다.

1. 동시에 성공하거나 실패해야 하는 불변식이 무엇인가.
2. 관련 데이터가 같은 DB와 같은 transaction manager에 있는가.
3. 외부 API처럼 local transaction에 참여할 수 없는 자원이 있는가.
4. 원자적으로 묶지 못하면 보상, 재시도, 멱등성, 중간 상태 중 무엇이 필요한가.

### 포인트 결제

포인트 행과 티켓 행이 같은 DB와 같은 transaction manager에 있고 repository 호출이 같은 transaction에 참여한다면 application service의 transaction 안에서 행 잠금, 포인트 차감, 티켓 예약을 원자적으로 처리할 수 있다.

```java
@Transactional
public ReservationResult reserveWithPoint(ReservationCommand command) {
    PointAccount point = pointRepository.findForUpdate(command.userId());
    Ticket ticket = ticketRepository.findForUpdate(command.ticketId());

    point.deduct(ticket.price());
    ticket.reserve(command.userId());
    return ReservationResult.confirmed(ticket.id());
}
```

하지만 `PointPaymentMethod`가 별도 transaction으로 먼저 commit하거나 다른 DB를 사용하면 티켓 저장과 원자적으로 묶이지 않는다. `@Transactional`을 어느 클래스에 붙였는지만으로 판단할 수 없고 transaction manager와 propagation을 확인해야 한다.

### 카드 결제

외부 카드 API는 local DB transaction에 참여하지 못한다. application service에 `@Transactional`을 붙여도 카드 승인까지 rollback되는 것은 아니다. 카드 승인 후 DB commit이 실패할 수 있으므로 다음 중 하나를 선택해야 한다.

- 카드 승인 취소를 보상으로 호출한다.
- 카드 승인 후 로컬 저장을 명시적 transaction 안에서 실행하고 실패 시 보상을 기록한다.
- 예약을 `PENDING`으로 먼저 저장하고 승인 후 확정한다.
- outbox나 재시도 작업으로 불일치를 복구한다.

이때 workflow는 승인, 저장, 보상 순서를 표현하는 데 도움이 되지만 어느 단계가 atomic한지는 각각 별도로 정해야 한다.

## 단계별 선택지

1. **현재 추천: 하나의 service와 `PaymentMethod` 유지**

   결제 방식의 차이가 결제 단계 안에서 끝나는 동안 사용한다.

   트레이드오프: 가장 단순하고 공통 흐름을 보존하지만 외부 카드와 로컬 DB 사이의 atomicity는 별도 보상 정책으로 다뤄야 한다.

2. **중간안: 하나의 service가 공통 흐름과 보상까지 소유**

   `PaymentResult`에 승인 식별자를 담고, 티켓 저장 실패 시 service가 `payment.cancel(result)`을 요청한다.

   트레이드오프: flow 분리 없이 복구를 명시할 수 있지만 service가 결제 생명주기와 transaction 세부를 더 많이 알게 된다. DB commit 실패는 proxy 밖에서 발생할 수 있으므로 명시적 transaction 실행이나 transaction synchronization 같은 추가 설계가 필요하다.

3. **분기점 이후: `ReservationFlow` 분리**

   결제 방식별 순서, 완료 상태, 보상 전략이 달라져 하나의 service가 분기로 비대해질 때 사용한다.

   트레이드오프: 차이를 온전히 캡슐화하지만 공통 코드 중복과 객체 수가 늘어난다.

## 피드백에 대한 수정된 답변

> 포인트 결제의 transaction 범위가 다르다는 이유만으로 예매 유스케이스 전체를 바로 `ReservationFlow`로 나눌 필요는 없다고 봅니다. 카드와 포인트가 같은 `PaymentMethod` 계약을 지킬 수 있다면 공통 예매 흐름은 하나의 application service에 두고 결제 단계만 다형화하는 것이 더 단순합니다. 다만 이 인터페이스가 transaction 차이를 해결해 주는 것은 아닙니다. 포인트와 티켓이 같은 local transaction에 참여할 수 있는지는 DB와 transaction manager 구성을 확인해야 하고, 외부 카드 승인은 local rollback 대상이 아니므로 보상이나 중간 상태가 필요합니다. 결제 방식에 따라 실행 순서, 완료 상태, 복구 방식까지 실제로 달라져 공통 service에 분기가 반복될 때만 다형성의 단위를 `ReservationFlow` 전체로 올리겠습니다.
