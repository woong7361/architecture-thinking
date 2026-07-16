# 계약

바깥 세계가 이 코드를 부르는 방법이다. 아래 이름과 시그니처는 **바꿀 수 없다.**
그 외의 모든 것 — 클래스를 몇 개 두는지, 무엇을 어디에 두는지 — 은 정해진 바 없다.

## 진입점

```java
package com.thinking.ticket;

public class TicketService {
    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments) { ... }

    // 예매 성공이면 true
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) { ... }
}
```

## 예외

실패는 아래 예외로 알린다. 모두 `com.thinking.ticket` 패키지에 두고, 이름은 이대로 쓴다.
`RuntimeException`을 상속하면 되고, 내용은 자유다.

| 예외 | 언제 |
| --- | --- |
| `UserNotFoundException` | 등록되지 않은 회원이 예매를 시도 |
| `TicketAlreadyReservedException` | 이미 예매된 티켓을 예매 시도 |
| `PaymentFailedException` | 결제사가 청구를 거절 |

## 테스트

- 인수테스트가 `src/test/`에 이미 있다. **이 파일들은 고칠 수 없다.**
  - `src/test/resources/features/ticket_reservation.feature`
  - `src/test/java/com/thinking/ticket/steps/TicketReservationSteps.java`
  - `src/test/java/com/thinking/ticket/CucumberAcceptanceTest.java`
- `mvn test`로 전부 통과해야 한다.
- 테스트를 더 추가하는 것은 자유다.

## 고칠 수 없는 것

- `com.thinking.ticket.provided` 패키지 전체
- `src/test/`에 이미 있는 위 세 파일
- `pom.xml`
