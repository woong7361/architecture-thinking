# 새 요구사항: 포인트 결제

회원이 카드 대신 **포인트로도** 예매할 수 있어야 한다.

1. 포인트로 예매하면 티켓 가격만큼 회원의 포인트가 차감되고, 카드 청구는 일어나지 않는다.
2. 포인트 잔액이 티켓 가격보다 적으면 예매는 실패한다. 티켓은 확정되지 않고 포인트도 차감되지 않는다.
3. 기존 카드 예매의 동작은 하나도 달라지지 않는다.

## 새로 주어지는 것

`com.thinking.ticket.provided.PointApi` — 사내 포인트 시스템 API. **고칠 수 없다.**

- `boolean deduct(long userId, int amount)` — 차감. 성공이면 true, 잔액이 모자라면 false.

## 계약 변경

기존 3-인자 진입점은 **그대로 남아 카드 결제로 동작해야 한다.**

```java
public class TicketService {
    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments, PointApi points) { ... }

    // 기존 — 카드 결제. 시그니처와 동작 그대로 유지.
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) { ... }

    // 신규 — paymentMethod 는 "CARD" 또는 "POINT".
    // "POINT" 이면 paymentInfo 는 쓰이지 않는다.
    public boolean reserveTicket(long userId, long ticketId, String paymentMethod, String paymentInfo) { ... }
}
```

### 새 예외

`com.thinking.ticket.InsufficientPointException` — 포인트 잔액이 티켓 가격보다 적을 때.

## 인수테스트

`src/test/`에 포인트 시나리오가 추가됐다. 기존 `ticket_reservation.feature`의 4개 시나리오도
**계속 통과해야 한다.** `src/test/`와 `provided` 패키지, `pom.xml`은 고칠 수 없다.
