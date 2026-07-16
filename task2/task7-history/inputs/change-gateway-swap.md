# 새 요구사항: 결제사 교체

지금 쓰던 결제사와 계약이 끝났다. **새 결제사로 갈아탄다.**

1. 예매의 동작은 하나도 달라지지 않는다. 회원이 카드로 예매하면 티켓 가격만큼 청구되고,
   거절되면 예매가 실패하는 것도 그대로다.
2. 청구는 이제 새 결제사를 통해 나간다.

## 없어지는 것

`com.thinking.ticket.provided.PaymentApi` — **삭제됐다.** 더 이상 쓸 수 없다.

## 새로 주어지는 것

`com.thinking.ticket.provided.PaymentGateway` — 새 결제사 게이트웨이. **고칠 수 없다.**

- `String authorize(int amount, String cardToken)` — 승인 요청. 승인되면 승인번호를 돌려주고,
  거절되면 null을 돌려준다.

기존 결제사와 모양이 다르다. 인자 순서가 반대고, 결과를 boolean이 아니라 승인번호로 준다.

## 계약 변경

생성자가 받는 결제 쪽 타입만 바뀐다. 진입점 시그니처는 그대로다.

```java
public class TicketService {
    public TicketService(TicketStore tickets, UserStore users, PaymentGateway payments) { ... }

    // 그대로.
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) { ... }
}
```

예외도 그대로다. 새 결제사가 거절하면(`authorize`가 null) `PaymentFailedException`을 던진다.

## 인수테스트

인수테스트의 시나리오는 **한 줄도 바뀌지 않았다.** 결제사가 바뀌어도 예매의 관찰 가능한 동작은
같아야 하기 때문이다. `src/test/`와 `provided` 패키지, `pom.xml`은 고칠 수 없다.
