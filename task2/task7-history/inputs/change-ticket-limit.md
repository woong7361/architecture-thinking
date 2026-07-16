# 새 요구사항: 1인 1매 제한

한 회원은 티켓을 **한 장만** 가질 수 있다.

1. 이미 예약한 티켓이 있는 회원이 다른 티켓을 예매하면 실패한다. 이때 청구는 일어나지 않는다.
2. 아직 아무 티켓도 없는 회원의 예매는 지금처럼 동작한다.

## 저장소에 추가된 것

`com.thinking.ticket.provided.TicketStore`에 조회가 하나 늘었다. **고칠 수 없다.**

- `int countByUserId(long userId)` — 그 회원 앞으로 예약된 티켓 수.

## 계약 변경

**없다.** 진입점도 생성자도 지금 그대로다.

### 새 예외

`com.thinking.ticket.TicketLimitExceededException` — 이미 티켓을 가진 회원이 추가 예매를 시도할 때.

## 인수테스트

`src/test/`에 제한 시나리오가 추가됐다. 기존 `ticket_reservation.feature`의 4개 시나리오도
**계속 통과해야 한다.** `src/test/`와 `provided` 패키지, `pom.xml`은 고칠 수 없다.
