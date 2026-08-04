결론은 **"실패하면 던진다는 사실을 모든 메서드 이름에 붙인다"가 아니라, 호출자가 부재와 실패 의미를 추측하지 않도록 계약을 명시한다**가 더 정확합니다. 이름은 그 계약을 드러내는 수단 중 하나이고, 반환 타입·예외 문서·계약 테스트와 함께 써야 합니다.

`findByIdOrThrow`, `chargeOrThrow`를 코드베이스 전체에 붙이는 규칙으로 만들 필요는 없습니다. 특히 `OrThrow`는 업무 의미보다 구현 메커니즘을 이름에 새겨, 나중에 예외를 Result로 바꾸면 이름까지 함께 바꿔야 합니다.

## 어떤 이름과 계약이 맞는가

부재가 정상 결과라면 다음처럼 표현합니다.

```java
Optional<User> findById(UserId id);
```

호출자가 "없으면 생성", "없으면 무시", "없으면 실패" 중 하나를 결정할 수 있습니다.

반대로 이 연산의 목적 자체가 기존 사용자를 가져오는 것이고, 없으면 계약을 만족하지 못한다면 다음처럼 의미를 강화할 수 있습니다.

```java
User loadExistingUser(UserId id);
```

또는 팀 관례가 명확하다면 다음도 가능합니다.

```java
User requireUser(UserId id);
```

이름만으로 끝내지 않고 반환 또는 실패라는 사후 조건을 문서와 테스트로 고정합니다.

```java
/**
 * @return 존재하는 사용자
 * @throws UserNotFoundException 사용자가 존재하지 않을 때
 */
User loadExistingUser(UserId id);
```

결제 명령은 `chargeOrThrow`까지 적지 않아도 `charge` 자체가 행위를 수행하는 명령입니다.

```java
PaymentReceipt charge(PaymentCommand command);
```

정상 반환은 결제 완료를 뜻하고, 실패 시 어떤 예외가 발생하는지를 Port 계약으로 명시할 수 있습니다. 결제 거절이 정상적으로 예상되는 결과이고 후속 행동이 달라진다면 이름이 아니라 반환 타입으로 드러냅니다.

```java
ChargeResult charge(PaymentCommand command);
```

따라서 이름 선택 기준은 다음입니다.

| 의미 | 계약 예시 | 책임 |
| --- | --- | --- |
| 부재가 정상 결과 | `Optional<User> findById(...)` | 호출자가 부재 의미 결정 |
| 반드시 존재해야 함 | `User loadExistingUser(...)` | 해당 Port 연산이 존재 보장 |
| 명령이 성공하거나 실패 | `PaymentReceipt charge(...)` | Port가 성공·실패 계약 제공 |
| 여러 정상 결과가 있음 | `ChargeResult charge(...)` | 호출자가 결과에 따라 조율 |

## 나중에 하나씩 리팩터링하기 어렵지 않은가

약한 계약이 많은 호출자에게 퍼진 뒤 바꾸면 실제로 어렵습니다. 특히 범용 `findById()`의 반환 타입을 한 번에 바꾸면 모든 호출자가 영향을 받습니다. 그렇다고 지금 모든 Repository와 Port를 `Required` 방식으로 바꾸는 것도 잘못 예측한 정책을 공용 계약에 고정할 위험이 있습니다.

추천은 **기존 공용 계약을 깨지 않고, 필요한 유스케이스 앞에 좁은 계약을 먼저 추가하는 방식**입니다.

```java
public interface LoadExistingUserPort {
    User load(UserId id);
}
```

```java
public final class JpaLoadExistingUserAdapter implements LoadExistingUserPort {
    private final SpringDataUserRepository repository;

    @Override
    public User load(UserId id) {
        return repository.findById(id.value())
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

Service는 새 Port를 사용하지만 기존 `findById()` 호출자는 그대로 둡니다.

```java
User user = loadExistingUserPort.load(command.userId());
```

이 방식은 `orElseThrow`를 Adapter 안으로 단순히 숨기는 것에 그치지 않습니다. 코어가 정의한 `LoadExistingUserPort`의 연산 목적과 사후 조건이 "존재하는 사용자를 반환한다"로 달라집니다. Adapter는 DB의 부재를 그 계약의 실패로 번역합니다.

다만 사용자 부재가 유스케이스마다 다른 의미라면 범용 `UserRepository`에 이 계약을 강제하지 말고, `LoadExistingUserPort`처럼 해당 유스케이스에 필요한 좁은 Port로 제한해야 합니다.

## 세 가지 도입 전략

1. **새 코드와 변경하는 유스케이스부터 계약을 명시하고 기존 코드는 유지 — 추천**
   - 얻는 것: 대규모 변경 없이 약한 계약의 확산을 멈춥니다. 테스트로 보호하면서 한 경계씩 옮길 수 있습니다.
   - 비용: 전환 기간에는 `findById`와 `loadExistingUser`가 함께 존재합니다.
   - 적합 조건: 기존 호출자가 많거나 각 유스케이스의 부재 의미가 다를 때 적합합니다.

2. **범용 Repository 계약을 지금 한 번에 변경**
   - 얻는 것: 네이밍과 실패 정책이 즉시 통일됩니다.
   - 비용: 모든 호출자를 고쳐야 하고, 선택적 조회까지 예외 계약에 묶일 수 있습니다.
   - 적합 조건: 호출자가 매우 적고 모든 조회에서 부재가 동일한 실패임을 확인했을 때만 적합합니다.

3. **현재 계약을 유지하고 반복이 생길 때 추출**
   - 얻는 것: 지금의 추상화 비용이 없습니다.
   - 비용: `null/boolean → throw` 패턴이 여러 Service에 퍼지면 나중 비용이 커집니다.
   - 적합 조건: 작은 일회성 코드이고 변화 가능성이 낮을 때 적합합니다.

## 현재 예제에 대한 추천

현재 Ticket 예제에서는 사용자 부재와 결제 실패가 발생하면 다른 회복 행동 없이 즉시 종료합니다. 이 범위 안에서는 다음처럼 선형적인 계약을 사용해도 됩니다.

```java
User user = loadExistingUserPort.load(command.userId());
Ticket ticket = loadExistingTicketPort.load(command.ticketId());

ticket.ensureReservable();
PaymentReceipt receipt = chargePaymentPort.charge(command.paymentInfo(), ticket.price());
ticket.assignTo(user.id());
saveTicketPort.save(ticket);
```

하지만 여기서 일반화할 규칙은 `OrThrow` 접미사를 붙이라는 것이 아닙니다.

> 부재나 실패가 정상 결과라면 타입으로 드러내고, 해당 연산에서 언제나 실패로 끝난다면 연산의 의미와 사후 조건을 이름·반환 타입·예외 계약·테스트로 명시한다.

점진적 리팩터링이 힘들어지는 것을 막는 현실적인 규칙은 다음 두 가지면 충분합니다.

1. 새 Port에는 `null`과 의미 없는 `boolean`을 반환하지 않는다.
2. 기존 약한 계약을 사용하는 코드를 수정할 때, 같은 해석이 반복된다면 유스케이스에 맞는 좁은 Port를 추가한다.

이렇게 하면 전면 리팩터링을 미루면서도 새 부채가 계속 쌓이는 것을 막을 수 있습니다.
