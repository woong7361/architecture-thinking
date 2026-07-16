# Task B-2: '죽은' 코드 비평하기

(Grit's Why): 좋은 설계를 배우는 가장 빠른 길은 나쁜 설계가 왜 나쁜지 정확히 말로 하는 것입니다. 아래 절차지향 코드(TicketService)는 '티켓 예매'라는 단일 책임을 하는 것처럼 보이지만, 한 메소드에 모든 로직이 절차적으로 들어 있습니다. (참고: null 체크 누락 같은 사소한 결함이 아니라, 구조 관점(Testability/Flexibility)으로 비평하는 게 이 Task의 초점입니다.)

```java
/* [비평 대상 코드: 티켓 예매 서비스]
 * 하나의 메소드에 모든 로직이 절차적으로 구현되어 있습니다.
 */
public class TicketService {

    private final TicketRepository ticketRepo; // (DB 의존)
    private final UserRepository userRepo;     // (DB 의존)
    private final PaymentApi paymentApi;       // (외부 API 의존)

    // ... Constructor ...

    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        // 1. 유저 조회 (DB)
        User user = userRepo.findById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        // 2. 티켓 조회 (DB)
        Ticket ticket = ticketRepo.findById(ticketId);
        if (ticket.isReserved()) {
            throw new TicketAlreadyReservedException();
        }
        // 3. 결제 시도 (외부 API)
        boolean paymentSuccess = paymentApi.charge(paymentInfo, ticket.getPrice());
        if (!paymentSuccess) {
            throw new PaymentFailedException();
        }
        // 4. 티켓 상태 변경 (DB)
        ticket.setReserved(true);
        ticket.setUserId(userId);
        ticketRepo.save(ticket);
        return true;
    }
}
```

### 수행 내용

1. 비평 1 (Testability): 이 코드를 단위 테스트하려 할 때 무엇이 발목을 잡는지 구체적으로. 외부 의존을 어떻게 처리해야 하나, 1-1의 가짜 객체 기준을 적용.
2. 비평 2 (Flexibility): '포인트 결제를 추가하라'는 요구가 들어오면 어디가 어떻게 바뀌어야 하는지, 그 변경이 왜 위험한지.

### 제출물

- [x] Testability 비평. (최소 300자)
- [x] Flexibility 비평(포인트 결제 추가 시나리오 기준). (최소 300자)

---

## 답안 1: Testability 비평

**1. 무엇이 발목을 잡는가.**

이 코드는 "테스트 불가능"한 게 아니다. `ticketRepo`·`userRepo`·`paymentApi`가 모두 생성자 주입이라 가짜 객체로 대체할 수 있다. 진짜 발목은 **순수 규칙 하나를 검증하려 해도 외부 의존 전체를 Mock으로 세워야 한다**는 점이다. 결정(예약 검증 `isReserved`·상태 전이 `setReserved`)과 I/O(조회·결제·저장)가 한 메소드에 뒤섞여, "이미 예약된 티켓은 예매 불가"를 테스트하려 해도 **조회 뒤 쓰이지도 않는 `user`까지** 스텁해야 한다. 

**2. 1-1의 가짜 객체 기준으로 진단하면.**

1-1 기준은 "Mock은 외부 의존에만, 순수 도메인 로직은 Mock으로 감싸지 않는다(감싸면 구현 베끼기)"였다. 그런데 이 코드는 순수 규칙을 검증하려면 **반드시 Mock 파이프라인을 통과해야** 한다. 순수 로직이 Mock 뒤에 갇힌 것이다. 이는 1-1에서 말한 **"순수 로직이 Mock/통합으로 내몰리면 전략 문제가 아니라 경계를 잘못 그은 설계 신호"** 의 실증이다. 원인은 외부 의존의 존재가 아니라 **결정과 I/O의 경계가 없다**는 것.

**3. 외부 의존을 어떻게 처리해야 하나.**

Mock을 잘 쓰는 게 아니라 **경계를 다시 긋는 것**이 답이다. 순수 결정을 `Ticket`으로 옮기면 `new Ticket(...).reserve(userId)`로 **Mock 0개·값으로** 검증된다. 외부 의존은 얇은 Service에 남겨 **조립 흐름만** 검증하되, repository는 진실이 DB에 있으니 Testcontainers로, `paymentApi` 같은 외부 호출만 Mock/Fake로 격리한다. 그러면 Mock이 필요한 테스트는 규칙이 몇 개냐가 아니라 I/O 단계가 몇 개냐(조회·결제·저장)에만 달리게 된다. 예약 규칙이 늘어도 그건 Mock 없는 순수 테스트로 처리되므로, Mock을 세워야 하는 무거운 테스트는 더 늘지 않는다.

---

## 답안 2: Flexibility 비평

**1. 지금 이 코드가 틀린 건 아니다 (YAGNI).**

결제수단이 카드 하나뿐이고 앞으로도 그렇다면, 지금처럼 `paymentApi.charge`를 직접 부르는 절차적 구조도 과하지 않다. 아직 오지 않은 변경을 위해 미리 추상화하는 것 역시 또 다른 과설계다. 문제는 구조 자체가 아니라, **변경이 실제로 들어왔을 때 그 변경을 어디서 어떻게 받느냐**에서 드러난다.

**2. 포인트 결제가 들어오면 어디가 바뀌나.**

'포인트 결제 추가' 요구가 오면, 결제수단이 카드 하나뿐임을 전제로 짠 이 메소드는 두 곳이 반드시 열린다. (1) 메소드 몸통에 카드/포인트를 가르는 **`if/else`(또는 `switch`) 분기가 삽입**된다. (2) 포인트 차감을 위한 **새 의존(`pointApi`)이 `TicketService` 생성자에 추가**된다. 즉 결제수단 하나 늘리는 데 예매 로직 전체가 다시 열린다.

**3. 그 변경이 왜 위험한가.**

첫째, **OCP 위반.** 결제와 무관한 예약 검증·상태 전이·저장 로직이 든 메소드를 매번 다시 열게 되어, 잘 돌던 코드가 수정 위험(regression)에 노출된다. 둘째, **분기의 전염(shotgun surgery).** 결제수단 차이는 `charge`뿐 아니라 환불·수수료·검증에도 번질 수 있어, 같은 `switch(type)`가 여러 곳에 복제되고 새 수단 추가 시 하나라도 빠뜨리면 버그가 된다. 셋째, **테스트 조합 폭발** — 비평1에서 짚은 Mock 파이프라인이 결제 분기 수만큼 곱해진다. 넷째, `TicketService`가 모든 결제 API를 알게 되어 책임이 비대해진다.

**방향.** 결제수단을 하나의 역할(interface)로 뽑아 분기를 다형성으로 대체하면, 포인트 추가가 '기존 코드 수정'이 아니라 '새 구현 추가'로 바뀐다(OCP). 이는 task1 B-1에서 `switch(policy)`를 역할로 바꾼 논증과 동일하다. 단 이 투자는 결제수단이 **늘어나는 축임을 아는** 지금에만 정당하다.