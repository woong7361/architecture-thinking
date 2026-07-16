# B-7 리포트: AI에게 시키고, 객체지향으로 판단하기

같은 요구사항(B-2의 티켓 예매)을 AI에게 두 번 구현시켰다. 하나는 아무 제약 없이, 하나는 B-3/B-4에서
세운 책임 분리와 의존 방향을 명시해서. 그다음 양쪽에 같은 변경 요구 세 가지를 가하고 비용을 쟀다.

---

## 결론 요약

**1. rule은 "좋은 설계를 만들어 주는 것"이 아니었다. 이미 잘하는 걸 *일관되게* 만드는 것이었다.**

무제약 arm도 지시 없이 Rich Domain을 만들었다(2회 재현). 그런데 **동전 던지기였다.** 1인 1매 규칙을
같은 프롬프트로 4번 시켰더니 2번은 도메인 객체(`Reserver`, `Buyer`)에 넣고 2번은 서비스 안 `if` 한 줄로
처리했다. rule arm은 **4번 다** 도메인 객체를 냈다.

| 1인 1매 규칙이 앉은 자리 (arm 당 4회) | 도메인 객체 | 서비스 `if` |
| --- | --- | --- |
| 무제약 | 2/4 | **2/4** |
| rule | **4/4** | 0/4 |

B-7이 묻는 "무엇을 어떻게 지시했을 때 더 **제어 가능한** 코드가 나왔나"의 답이 이것이다 —
**제어 가능성이란 평균이 좋은 게 아니라 최악이 없다는 것**이고, 그건 한 번 돌려서는 안 보인다.

**2. "고친 파일 수"라는 지표는 이 실험에서 사람을 속인다.**

세 변경 전부 무제약 arm이 파일을 더 적게 고쳤다. 결제사 교체마저 1파일 `+4/-4` 대 2파일 `+13/-8`로
무제약 arm이 적었다. 그런데 그 1파일이 **예매 정책이 들어 있는 파일**이었다. rule arm은 정책이 한 줄도
안 열렸다. 파일 수가 아니라 **"변경이 정책에 닿았나"** 를 봐야 갈린다.

**3. 의존 방향(DIP)이 값을 한 것은 벤더 교체 축, 딱 하나였다.**

포인트 결제와 1인 1매에서는 포트가 오히려 세금이었다(고칠 인터페이스+어댑터가 늘어난다).
B-5의 결론 **"저항력은 끊은 축에서만"** 이 그대로 재현됐고, 이번엔 그 **뒷면**까지 봤다 —
안 끊은 축에서 추상화는 공짜가 아니라 비용이다.

---

## 1. 실험 설계

### 독립변수는 하나

프롬프트는 글자 그대로 같고, **차이는 `rules.md`를 지시하는 한 문단뿐**이다.

```diff
  `spec.md`의 요구사항을 구현해줘.

  `contract.md`에 바깥에서 부르는 방법이 정해져 있어. 거기 적힌 이름과 시그니처는 그대로 지켜야 하고,
  `provided` 패키지와 `src/test/`에 이미 있는 파일들은 고치면 안 돼.

+ `rules.md`에 설계 규칙이 있어. 그 규칙을 지켜서 구현해줘.

  다 하고 나서 `mvn test`로 인수테스트가 전부 통과하는지 확인해줘.
```

### 상수

| 상수 | 내용 |
| --- | --- |
| 요구사항 | [`inputs/spec.md`](inputs/spec.md) — 설계 어휘(책임·도메인·포트·주입)를 한 글자도 쓰지 않은 자연어 명세 |
| 계약 | [`inputs/contract.md`](inputs/contract.md) — 진입점 시그니처 + 예외 3종 이름 |
| 주어진 세계 | [`harness/provided/`](harness/provided) — 저장소·결제사·저장 레코드. 수정 불가 |
| 인수테스트 | [`harness/net/`](harness/net) — 4개 시나리오. 수정 불가 |
| 빌드·모델 | JDK17 + Cucumber 7.18, `claude-opus-4-8`, arm마다 새 프로세스 |

### rule 문서의 수위 — 원칙만, 설계도는 주지 않았다

B-4 답안 2에는 `Ticket`/`ChargePort`/`TicketRepository` ASCII 설계도가 완성된 형태로 있다. 그걸 주면
arm B는 판단하는 게 아니라 받아쓰기를 하고, 실험은 "설계도를 주면 그 설계도가 나온다"는 동어반복이 된다.

그래서 [`inputs/rules.md`](inputs/rules.md)에는 **판단 기준만** 담았다 — 협력→책임→역할→생성 순서,
정보 전문가, 불변식은 상태 소유자가, 의존의 방향과 추상의 너비, 축 없는 추상화는 과설계.
**클래스 이름도 포트 목록도 없다.**

### Anemic vs Rich를 자유변수로 남긴 방법

`provided`가 주는 것은 **저장 레코드**(`TicketRecord`: 상태 + 접근자뿐)와 저장소·결제사뿐이다.
이 레코드를 그대로 주무를지, 규칙을 가진 별도의 것으로 옮겼다가 돌려놓을지는 **정해두지 않았다.**
B-3의 Anemic vs Rich가 전제가 아니라 측정 대상이 된다.

```java
// harness/provided/TicketRecord.java — 수정 불가. B-2 의 anemic Ticket 자리다.
public final class TicketRecord {
    private final long id;
    private final int price;
    private boolean reserved;
    private Long userId;
    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }
    public void setUserId(Long userId) { this.userId = userId; }
    // ...
}
```

### 오염 차단

repo(`architecture-thinking`)에는 루트 `AGENTS.md`와 `task2/CLAUDE.md`가 있고, 거기에 "협력 → 책임 →
역할 → 생성" 순서와 SOLID 적용 규칙이 통째로 들어 있다. Claude Code는 cwd 상위의 CLAUDE.md를 자동
로드하므로, **repo 안에서 돌리면 무제약 arm이 그 규칙을 공짜로 받는다.** 그래서 실행은 repo 바깥
(`task/ticket-kata-w1`, `task/ticket-kata-w2`)에서 했다.

arm 프로세스에 직접 물어 확인했다:

> **1. 로드된 규칙 파일**: `~/.claude/CLAUDE.md` (wmux 브라우저 사용법) 하나뿐.
> **2. 이전 대화 이력**: 없음. 이 메시지가 세션의 첫 사용자 메시지입니다.
> **3. 객체지향/SOLID/책임 분리/의존 방향 지침**: 없습니다.

---

## 2. 오염을 발견하고 다시 돌린 이야기

1차 실행 뒤 "같은 세션에서 돌려서 컨텍스트가 샌 것 아니냐"는 의심이 나왔다. 세션은 안 샜다(위 probe).
**그런데 그 질문이 다른 걸 잡아냈다.**

Claude Code는 **git 상태와 최근 커밋 메시지를 시스템 프롬프트에 주입한다.** 그런데 1차에서 커밋 메시지를
이렇게 썼다:

```
baseline: 무제약 구현 (arm A)
baseline: rule 제시 구현 (arm B)
```

변경 요구를 실행할 때 arm이 이걸 읽었다. 자기가 실험 중이라는 것도, 자기 조건이 "무제약"인지 "rule 제시"
인지도 알 수 있었다.

**근본 원인은 실험 기록과 실험 환경을 같은 git 저장소에 둔 것이다.** 커밋 메시지는 측정을 위한 내 실험
노트인데, 그 저장소가 동시에 피험자의 방이었다. 뼈아픈 건 이 채널을 이미 한 번 막았다는 점이다 —
변경 요구 문서의 원본에 "rule이 예고한 축이다, arm B 유리가 예상된다"가 있어서 **arm에 줄 버전을 따로
만들어 메타를 걷어냈다.** arm이 그 파일을 읽을 줄 알았으니까. 그래놓고 같은 메타를 커밋 메시지에 썼다.
같은 실수를 다른 채널에서 반복한 것이고, **git 메타데이터를 컨텍스트 채널로 세어보지 않은 게 원인**이다.

범위는 제한적이었다. baseline 실행 시점엔 저장소에 커밋이 중립적인 scaffold 하나뿐이라 라벨이 아직
존재하지 않았다. 오염은 baseline 커밋을 찍은 **다음**, 즉 변경 요구부터 시작된다.

### 2차에서 고친 것

1. 워크스페이스 이름 중립화 (`arm-a` → `ticket-kata-w1`) — arm은 cwd도 본다
2. 커밋 메시지 중립화 (`baseline: 무제약 구현 (arm A)` → `티켓 예매 구현`)
3. 브랜치 이름 중립화 (`c1` → `point-payment`)
4. **baseline도 재실행** — 덤으로 2번째 표본이 생겨 n=1 한계가 완화됐다
5. **결제사 교체(C3) 추가** — rule arm의 포트가 실제로 끊은 유일한 축. 이게 없으면 결론에 구멍이 남는다

이 리포트의 모든 수치는 **2차(오염 제거)** 기준이다. 1차 실행물은 실험실(`task/b7-lab/run1-leaky/`,
repo 밖 — 실험은 repo의 설계 규칙이 닿지 않는 곳에서 돌려야 하므로)에 보존했다.

### 재현 확인

| | 1차(오염) | 2차(중립) |
| --- | --- | --- |
| 무제약 baseline | 5 파일 / 136줄 / 인터페이스 0 | **5 파일 / 132줄 / 인터페이스 0** |
| rule baseline | 12 파일 / 264줄 / 인터페이스 3 | **12 파일 / 277줄 / 인터페이스 3** |

라벨 오염을 걷어내도 baseline 결론은 그대로다.

---

## 3. harness

### 인수테스트 — 구조를 보지 않는다

net은 **기존 인프라와 계약된 진입점에만** 의존한다. 구현이 클래스를 몇 개로 쪼갰는지, 인터페이스를 뒀는지는
보지 않는다. 그래서 두 arm에 같은 파일이 그대로 들어간다.

```gherkin
Scenario: 이미 예약된 티켓은 예매할 수 없고 결제도 일어나지 않는다
  Given 회원 1이 등록되어 있다
  And 가격 30000원짜리 이미 예약된 티켓 20이 있다
  When 회원 1이 카드정보 "card-token"으로 티켓 20을 예매하면
  Then 예매는 이미 예약됨으로 거부된다
  And 결제는 청구되지 않는다
```

```java
// harness/net/java/com/thinking/ticket/steps/TicketReservationSteps.java
@When("회원 {long}이 카드정보 {string}으로 티켓 {long}을 예매하면")
public void 예매하면(long userId, String paymentInfo, long ticketId) {
    TicketService service = new TicketService(ticketStore, userStore, paymentApi);
    try {
        this.result = service.reserveTicket(userId, ticketId, paymentInfo);
    } catch (Throwable t) {
        this.thrown = t;
    }
}

@Then("티켓 {long}은 회원 {long}에게 예약된다")
public void 티켓이_회원에게_예약된다(long ticketId, long userId) {
    TicketRecord stored = ticketStore.findById(ticketId);   // 상태로 단언한다. verify 가 아니라.
    assertThat(stored.isReserved()).isTrue();
    assertThat(stored.getUserId()).isEqualTo(userId);
}
```

세 harness(baseline·포인트·결제사교체)는 arm 실행 전에 **참조 구현으로 GREEN 검증**했다. net에 버그가
있으면 양쪽 arm이 다 실패해서 실행이 통째로 낭비되기 때문이다. 그 참조 구현은 arm에 주지 않았다.

---

## 4. baseline

| | 무제약 (w1) | rule (w2) |
| --- | --- | --- |
| 파일 | 5 | 12 |
| 줄 | 132 | 277 |
| 인터페이스 | 0 | 3 |
| 인수테스트 | 4/4 GREEN | 4/4 GREEN |

### 무제약 arm도 Rich Domain을 만들었다 — 지시 없이

가장 중요한 결과다. `TicketRecord`(고칠 수 없는 anemic 레코드)에서 값을 떠온 자기 `Ticket`을 만들고,
불변식을 그 안에 넣었다. setter는 없다.

```java
// code/no-rule/baseline/Ticket.java
final class Ticket {

    private final long id;
    private final int price;
    private boolean reserved;
    private Long userId;

    static Ticket from(TicketRecord record) {
        return new Ticket(record.getId(), record.getPrice(), record.isReserved(), record.getUserId());
    }

    /** 예매할 수 있는 티켓이 아니면 거부한다. 청구 전에 물어보라고 있는 것이다. */
    void requireReservable() {
        if (reserved) {
            throw new TicketAlreadyReservedException("이미 예매된 티켓입니다: " + id);
        }
    }

    void reserveFor(long userId) {
        requireReservable();
        this.reserved = true;
        this.userId = userId;
    }

    TicketRecord toRecord() { /* 값을 레코드로 되돌린다 */ }
}
```

B-3 답안 2가 처방한 `ticket.reserve(userId)`와 사실상 같다. **지시하지 않았는데 나왔다.**

```java
// code/no-rule/baseline/TicketService.java — 진입점 + 조립 + 정책이 한 몸
public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
    UserRecord user = users.findById(userId);
    if (user == null) {
        throw new UserNotFoundException("등록되지 않은 회원입니다: " + userId);
    }

    Ticket ticket = Ticket.from(tickets.findById(ticketId));
    ticket.requireReservable();

    if (!payments.charge(paymentInfo, ticket.price())) {          // ← 벤더를 직접 부른다
        throw new PaymentFailedException("청구가 거절되었습니다: 티켓 " + ticketId);
    }

    ticket.reserveFor(userId);
    tickets.save(ticket.toRecord());
    return true;
}
```

### rule arm — 갈린 곳은 의존 방향 하나

```java
// code/with-rule/baseline/ReservationDesk.java — 정책. provided 를 한 줄도 import 하지 않는다.
final class ReservationDesk {

    private final Tickets tickets;
    private final Members members;
    private final Payments payments;

    void reserve(long userId, long ticketId, String paymentInfo) {
        if (!members.isRegistered(userId)) {
            throw new UserNotFoundException(userId);
        }

        Ticket ticket = tickets.byId(ticketId);

        // 잡아본 뒤에 청구한다. 이미 잡힌 티켓이면 여기서 거부되므로 청구는 시도조차 되지 않는다.
        Ticket reserved = ticket.reserveFor(userId);

        if (!payments.charge(paymentInfo, ticket.price())) {
            throw new PaymentFailedException(ticketId, ticket.price());
        }

        // 남겨야 확정이다. 청구가 거절돼 여기까지 못 오면 티켓은 아무에게도 확정되지 않은 채로 남는다.
        tickets.save(reserved);
    }
}
```

```java
// code/with-rule/baseline/TicketService.java — 계약된 진입점은 조립만 맡는다
public class TicketService {
    private final ReservationDesk desk;

    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments) {
        this.desk = new ReservationDesk(
                new StoredTickets(tickets),
                new RegisteredMembers(users),
                new CardPayments(payments));
    }
    // ...
}
```

**두 arm의 진짜 차이는 여기 하나다.** 도메인 모델은 둘 다 Rich다. 무제약 arm은 정책이 벤더
`PaymentApi`를 직접 부르고, rule arm은 정책이 포트(`Payments`/`Tickets`/`Members`) 뒤에 있고 어댑터가
벤더를 감싼다.

---

## 5. 변경 요구 세 가지

각 변경은 baseline에서 **독립 브랜치**로 갈라 적용했다. 서로 섞이지 않는다.

```
baseline ──┬── point-payment   (포인트 결제 추가)   ← rule이 예고한 축
           ├── ticket-limit    (1인 1매 제한)      ← rule이 예고 안 한 축
           └── gateway-swap    (외부 결제사 교체)   ← rule arm의 포트가 실제로 끊은 축
```

### C1. 포인트 결제 — 둘 다 다형성으로 갔다

**무제약 arm도 `if/else`를 예매 흐름에 넣지 않았다.** 추상을 세우고 분기를 private 팩토리에 가뒀다.

```java
// code/no-rule/point-payment/Payment.java — 지시 없이 나온 추상
interface Payment {
    /** 값을 받아낸다. 받아내지 못하면 수단에 맞는 예외를 던진다 — 흐름이 예매를 중단할 수 있도록. */
    void pay(long userId, int amount);
}
```

```java
// code/no-rule/point-payment/TicketService.java — 분기는 팩토리 한 곳에만
private Payment paymentBy(String paymentMethod, String paymentInfo) {
    if (CARD.equals(paymentMethod)) {
        return new CardPayment(payments, paymentInfo);
    }
    if (POINT.equals(paymentMethod)) {
        return new PointPayment(points);
    }
    throw new IllegalArgumentException("알 수 없는 결제 수단입니다: " + paymentMethod);
}
```

rule arm은 같은 걸 하되 **너비를 더 좁혔다**(ISP). 무제약 arm의 `pay(userId, amount)`는 userId를 나르지만,
rule arm의 `Payer.pay(amount)`는 금액만 받는다 — 누가 무엇으로 치르는지는 이미 그 객체가 안다.

```java
// code/with-rule/point-payment/Payer.java
/**
 * 변경 축: 결제 수단이 늘어난다. 카드 하나였던 것이 포인트로 늘었고, 그때 예매 절차는 한 줄도 바뀌지 않았다.
 * 너비: 창구가 아는 것은 치를 금액뿐이다. 누가 무엇으로 치르는지는 이 객체가 이미 안다.
 */
public interface Payer {
    void pay(int amount);
}
```

**B-2 답안 2의 예측이 여기서 빗나갔다.** 답안은 "결제 타입 파라미터를 받으면 호출부가 깨지고, 메소드 몸통에
`if/else`가 삽입되고, 새 의존이 생성자에 붙는다"고 했다. 실제로는 분기가 흐름 밖으로 나갔고, 기존 3-인자
진입점은 위임으로 보존됐다. **그 예측은 사람이 절차적으로 고칠 때의 이야기였고, 지시 없는 AI는 그 길로
가지 않았다.**

### C2. 1인 1매 제한 — 여기서 갈렸다

**이 변경이 rule의 효과를 드러낸 유일한 자리다.**

```java
// code/no-rule/ticket-limit/TicketService.java — 규칙이 서비스 안 if 로 들어왔다
public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
    UserRecord user = users.findById(userId);
    if (user == null) { throw new UserNotFoundException(...); }

    if (tickets.countByUserId(userId) >= TICKETS_PER_USER) {        // ← 규칙이 여기 산다
        throw new TicketLimitExceededException(
                "이미 티켓을 가진 회원입니다: " + userId + " (한 회원은 " + TICKETS_PER_USER + "장까지)");
    }
    // ...
}
```

```java
// code/with-rule/ticket-limit/Holding.java — 규칙이 그 상태를 가진 객체 안에 있다
/**
 * 한 회원이 지금 몇 장을 가지고 있는지, 그 보유 현황.
 *
 * <p>"한 회원은 한 장만 가질 수 있다"는 규칙을 보유 수를 가진 이 객체가 스스로 지킨다.
 * 몇 장을 가졌는지 바깥에 내주고 바깥이 세어보게 두면, 그 규칙은 묻는 곳마다 흩어진다.
 * 그래서 수를 여는 접근자를 두지 않고, take 로 한 장을 가져가는 행위만 연다.
 */
public final class Holding {

    private static final int LIMIT = 1;
    private final long userId;
    private final int held;

    public Ticket take(Ticket ticket) {
        if (held >= LIMIT) {
            throw new TicketLimitExceededException(userId);
        }
        return ticket.reserveFor(userId);
    }
}
```

무제약 arm은 `countByUserId()`로 **묻고 바깥이 판단**했다(Ask). rule arm은 수를 여는 접근자를 두지 않고
`take()`로 **시켰다**(Tell). B-1 제출물 2에서 정리한 'Tell, Don't Ask'가 그대로 갈린다.

#### 분산 측정 — 이 결과가 우연인지 확인했다

1차 실행에서는 무제약 arm이 같은 요구에 `Reserver`라는 도메인 객체를 만들었다. 2차에서는 `if` 한 줄로 갔다.
**그런데 1차는 커밋 라벨에 오염된 실행이라**(§2), 그 차이가 자연스러운 분산인지 라벨 때문인지 알 수 없었다.
결론이 오염된 데이터에 기대면 안 되므로, **깨끗한 baseline에서 이 변경만 arm당 3회씩 더 돌렸다**
([`variance-test.sh`](variance-test.sh) · 코드 [`code/variance/`](code/variance)).

| 실행 | 무제약 | rule |
| --- | --- | --- |
| r1 (본실험) | 서비스 `if` | `Holding` |
| r2 | `Reserver` (도메인) | `Holdings` |
| r3 | `Buyer` (도메인) | `Holding` |
| r4 | 서비스 `if` | `Member` |
| **도메인 객체 비율** | **2/4** | **4/4** |

여덟 번 모두 인수테스트는 GREEN이다. **테스트로는 안 갈린다.** 갈리는 건 규칙이 어디 앉느냐뿐이다.

무제약 arm이 잘한 경우(`Buyer`)의 코드는 rule arm 것과 거의 구별되지 않는다:

```java
// ticket-kata-w1-r3/Buyer.java — 무제약 arm 이 지시 없이 낸 것
/**
 * 몇 장까지 가질 수 있는지 아는 회원.
 *
 * <p>UserRecord 는 저장소가 다루는 행이라 상태만 갖는다. 한 장 더 가져도 되는지 판단하는
 * 일은 이쪽이 맡는다. Ticket 이 티켓 쪽 규칙을 맡는 것과 같은 자리다.
 */
final class Buyer { ... }
```

**분산은 rule arm에도 있다 — 다만 층이 다르다.** rule arm은 4번 다 "규칙은 그 상태를 가진 객체가 지킨다"는
원칙을 지켰지만, **그 주인이 누구인지는 갈렸다** — 셋은 티켓 쪽 보유 현황(`Holding`/`Holdings`)에, 하나는
회원 쪽(`Member`)에 뒀다. 둘 다 정보 전문가로 정당화되는 선택이다. 즉 rule은 **원칙 준수를 고정하고
그 안에서 판단은 열어 뒀다.** 설계도를 줬다면 이 판단마저 닫혔을 것이다.

### C3. 결제사 교체 — 파일 수가 거짓말하는 자리

새 결제사는 모양이 다르다. 인자 순서가 반대고, 결과가 boolean이 아니라 승인번호이며 거절이 `null`이다.
**인수테스트 시나리오는 한 줄도 바뀌지 않았다** — 결제사가 바뀌어도 관찰 가능한 동작은 같아야 하므로.

```java
// harness/provided/PaymentGateway.java — 새 결제사. 수정 불가.
public String authorize(int amount, String cardToken)   // 승인이면 승인번호, 거절이면 null
```

**무제약 arm — 1파일, `+4/-4`. 그런데 그 파일이 정책이다.**

```diff
--- a/code/no-rule/gateway-swap/TicketService.java
+++ b/code/no-rule/gateway-swap/TicketService.java
-import com.thinking.ticket.provided.PaymentApi;
+import com.thinking.ticket.provided.PaymentGateway;
@@
-    private final PaymentApi payments;
+    private final PaymentGateway payments;
@@ 예매 흐름 한복판 @@
-        if (!payments.charge(paymentInfo, ticket.price())) {
+        if (payments.authorize(ticket.price(), paymentInfo) == null) {
             throw new PaymentFailedException("청구가 거절되었습니다: 티켓 " + ticketId);
         }
```

새 벤더의 관례 — 인자 순서, `null`이 거절이라는 것 — 이 **예매 흐름 안으로 들어왔다.** 예매 로직이 이제
결제사 SDK의 규약을 안다.

**rule arm — 2파일, `+13/-8`. 그런데 정책은 한 줄도 안 열렸다.**

```diff
--- a/code/with-rule/gateway-swap/infra/CardPayments.java
+++ b/code/with-rule/gateway-swap/infra/CardPayments.java
+/**
+ * <p>게이트웨이의 관례 — 인자가 (금액, 카드토큰) 순이고, 결과가 승인번호이며 거절이 null인 것 —
+ * 은 전부 이 클래스 안에서 번역되어 밖으로 새지 않는다. 정책은 승인 여부만 본다.
+ */
 public final class CardPayments implements Payments {
     @Override
     public boolean charge(String paymentInfo, int amount) {
-        return api.charge(paymentInfo, amount);
+        return gateway.authorize(amount, paymentInfo) != null;
     }
 }
```

나머지 하나는 `TicketService`의 생성자 파라미터 타입뿐이다 — 계약이 고정한 조립 지점이라 어느 설계든
불가피하다. **`ReservationDesk`(정책)와 `Ticket`(도메인)은 diff에 나타나지 않는다.**

파일 수로는 무제약 arm이 이겼고(1 vs 2), 라인 수로도 이겼다(`+4/-4` vs `+13/-8`).
**그런데 정책이 열린 쪽은 무제약 arm이다.** 지표가 구조를 못 본다.

---

## 6. 측정표

`git diff <branch>~1 <branch> -- src/main/java/com/thinking/ticket ':!*/provided'` 기준.
`<branch>~1`은 요구사항·인수테스트만 얹은 scaffold 커밋이라, 이 diff는 **AI가 구현을 고친 부분만** 담는다.

| 변경 | arm | 신규 | 고친 기존 | 전체 | 기존 파일만 | 정책 열림 | 인수테스트 | net 수정 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 포인트 결제 | 무제약 | 4 | **1** | +102/−7 | +36/−7 | ✅ 열림 | 6/6 GREEN | 0 |
| 포인트 결제 | rule | 6 | **3** | +165/−18 | +47/−18 | ✅ 열림 | 6/6 GREEN | 0 |
| 1인 1매 | 무제약 | 1 | **1** | +18/−0 | +9/−0 | ✅ 열림 | 6/6 GREEN | 0 |
| 1인 1매 | rule | 2 | **3** | +63/−4 | +17/−4 | ✅ 열림 | 6/6 GREEN | 0 |
| 결제사 교체 | 무제약 | 0 | **1** | +4/−4 | +4/−4 | ✅ **열림** | 4/4 GREEN | 0 |
| 결제사 교체 | rule | 0 | **2** | +13/−8 | +13/−8 | ❌ **안 열림** | 4/4 GREEN | 0 |

**세 변경 전부 무제약 arm이 파일을 적게 고쳤다.** 그리고 열네 번의 실행(본실험 8 + 분산 6) 모두
인수테스트가 GREEN이고, 수정 금지 파일(테스트·`provided`·`pom.xml`)은 한 번도 바뀌지 않았다.

1인 1매의 분산 실행 4회를 합치면 "고친 기존 파일" 수치는 이렇게 안정적이다.

| 1인 1매, 고친 기존 파일 | r1 | r2 | r3 | r4 |
| --- | --- | --- | --- | --- |
| 무제약 | 1 | 1 | 1 | 1 |
| rule | 3 | 3 | 3 | 4 |

**파일 수 차이는 재현된다.** 규칙이 어디 앉느냐와 무관하게, rule arm은 포트를 지나야 해서 늘 3~4개다.
무제약 arm은 늘 1개다 — 도메인 객체를 만들든 `if`를 쓰든 `TicketService` 하나만 연다.

### 규칙이 앉은 자리

| 변경 | 무제약 | rule |
| --- | --- | --- |
| 예약 불변식 | `Ticket.requireReservable()` | `Ticket.reserveFor()` |
| 결제수단 | `Payment` 인터페이스 + 구현 2 | `Payer` 인터페이스 + 구현 2 |
| **1인 1매** | **`TicketService` 안 `if`** | **`Holding.take()`** |
| 벤더 관례 | **예매 흐름 안** | `infra/CardPayments` 안 |

---

## 7. 예측 대조

변경 적용 전에 예측을 커밋으로 잠갔다(B-5의 `88301ab` 패턴).

**단서 하나.** 예측은 **1차 baseline**을 읽고 잠갔는데, 대조하는 실제값은 **2차**다. 2차 baseline은
재실행이라 코드가 완전히 같지는 않다. 다만 구조는 재현됐고(무제약 5파일/인터페이스 0, rule 12파일/
인터페이스 3), 예측의 근거가 된 `Payments.charge(paymentInfo, amount)`도 2차에 그대로 있어서 예측이
그대로 옮겨진다고 봤다. 엄밀히는 사후 대조다.

| | 예측 (무제약/rule) | 실제 | |
| --- | --- | --- | --- |
| 포인트 결제, 고친 기존 파일 | 1 / 4 | 1 / 3 | ~ 근접 |
| 1인 1매, 고친 기존 파일 | 1 / 3 | **1 / 3** | ✓ 정확 |
| 결제사 교체 | (사전 등록 안 함) | 1 / 2 | — |

**빗나간 예측 1.** "무제약 arm이 `if/else`를 예매 흐름에 삽입할 것" — 틀렸다. 다형성을 스스로 냈다.

**빗나간 예측 2.** "rule arm의 `Payments.charge(paymentInfo, amount)`가 userId를 못 받으니 포트를
고쳐야 할 것" — 틀렸다. 포트를 고치는 대신 `Payer`라는 **한 층 위 추상**을 얹어서 피했다.
시그니처가 안 맞을 때 그 포트를 수술하는 것 말고 위에 층을 얹는 길이 있다는 걸 못 봤다.

**맞은 예측.** "1인 1매에서 rule arm이 더 많이 고칠 것 — 새 정보가 포트를 지나와야 하니까." 정확히 맞았다.
`Tickets` 인터페이스 + `StoredTickets` 어댑터 + 정책 셋이다.

---

## 8. 결론

### AI에게 무엇을 어떻게 지시했을 때 더 제어 가능한 코드가 나왔나

이 실험을 시작할 때 기대한 답은 "책임 분리와 의존 방향을 명시하면 좋은 설계가 나온다"였다. **틀렸다.**
무제약 arm도 Rich Domain을 만들었고(2회 재현), 다형성을 스스로 냈고, 파일도 더 적게 고쳤다. 요즘 모델은
지시가 없어도 B-3/B-4가 처방한 걸 상당 부분 해낸다.

rule이 실제로 바꾼 것은 두 가지다.

**첫째, 분산.** 무제약 arm은 1인 1매 규칙을 4번 중 2번만 도메인 객체에 넣었다. 나머지 2번은 서비스 안
`if`였다. **같은 요구, 같은 프롬프트, 동전 던지기.** rule arm은 4번 다 도메인 객체를 냈다.
**평균이 아니라 최악이 다르다.** 그리고 "제어 가능하다"는 말의 뜻이 바로 이것이다 — 잘 나올 때가 아니라
**못 나올 때가 없다**는 것.

여기서 제일 불편한 사실은, **인수테스트로는 이 차이가 안 잡힌다**는 것이다. 여덟 번 다 GREEN이다.
테스트는 "동작하나"를 묻지 "이 규칙이 다음에 바뀔 때 어디를 열어야 하나"를 묻지 않는다. 그래서 AI가 짠
코드를 테스트 GREEN만으로 받으면, 절반의 확률로 규칙이 서비스에 눌어붙은 코드를 받고도 모른다.
**AI 시대에 객체지향이 기본기인 이유가 이것이라고 본다** — 테스트가 안 잡는 걸 사람이 읽어서 잡아야 하고,
읽어서 잡으려면 "이게 왜 나쁜가"를 말로 할 수 있어야 한다. B-2가 시킨 게 정확히 그거였다.

**둘째, 의존 방향.** 이건 지시하지 않으면 안 나왔다. 무제약 arm은 두 번 다 벤더를 정책에서 직접 불렀다.
그리고 결제사가 바뀌자 벤더의 관례가 예매 흐름 안으로 들어왔다. rule arm은 정책이 한 줄도 안 열렸다.

### 파일 수는 변경 비용이 아니다

B-7이 요구하는 지표("고친 파일/메서드 수")로 재면 무제약 arm이 세 변경 전부 이긴다. 결제사 교체마저
1파일 `+4/-4`로 이긴다. 그런데 그 1파일이 정책이다. **작은 diff가 나쁜 자리에 나는 것과, 큰 diff가 좋은
자리에 나는 것을 파일 수는 구분하지 못한다.**

포트의 값은 "이번에 몇 줄 고쳤나"가 아니라 **"무엇을 열지 않아도 됐나"** 로 나타난다. rule arm이 낸
+13줄은 벤더 관례가 정책까지 못 오게 막은 값이다. 그 값을 재려면 지표를 바꿔야 한다.

### 저항력은 끊은 축에서만 — 그리고 그 뒷면

B-5의 `change-resilience-test.md` 결론이 그대로 재현됐고, 이번엔 뒷면까지 봤다.

- **끊은 축(결제사 교체)**: 포트가 값을 했다. 정책 0줄.
- **안 끊은 축(1인 1매)**: 포트가 세금이었다. 인터페이스와 어댑터를 같이 고쳐야 해서 rule arm이 3파일,
  무제약 arm이 1파일.

B-2 답안 2에 쓴 문장 — "이 투자는 결제수단이 **늘어나는 축임을 아는** 지금에만 정당하다" — 이 실험이
그 문장을 양쪽에서 확인했다. 축이 맞으면 정책을 지키고, 축이 틀리면 고칠 곳만 늘린다.

### 그래서 나는 어떻게 지시할 것인가

**설계도가 아니라 판단 기준을 준다.** `rules.md`에는 클래스 이름도 포트 목록도 없었는데 rule arm은
포트를 세우고 축을 댔다. 설계도를 줬으면 받아쓰기가 됐을 것이고, 그건 AI가 판단한 게 아니라 내가 판단한
것이다.

**단발 결과로 판단하지 않는다.** 이 실험의 가장 큰 교훈은 1차와 2차가 달랐다는 것이다. AI가 한 번 잘한 걸
보고 "얘는 이거 잘하네"라고 결론 내리면 틀린다. 무제약 arm은 1차에 잘했고 2차에 못했다.

**의존 방향은 반드시 지시한다.** 나머지(Rich Domain, 다형성)는 지시 없이도 나오지만, 이건 안 나왔다.
그리고 이게 나중에 제일 비싸다.

---

## 9. 한계

**1. 표본이 작다.** 1인 1매만 arm당 4회 돌렸고(분산이 갈린 유일한 변경이라), baseline은 2회,
포인트 결제와 결제사 교체는 1회씩이다. **"무제약 2/4 vs rule 4/4"는 n=4다.** 방향은 뚜렷하지만
비율의 정확도를 주장할 수는 없다 — 무제약 arm의 실제 확률이 30%인지 60%인지는 이 데이터로 모른다.
포인트 결제와 결제사 교체에도 같은 분산이 있는지는 재지 않았다.

**2. 계약이 상수다.** 진입점 시그니처·예외 이름·"저장소와 결제사를 받아서 쓴다"는 형태를 고정한 것은
무제약 arm에 흘린 최소 힌트다. 다만 B-2 원본 죽은 코드도 이미 세 의존을 전부 생성자 주입받고 있었고,
B-2 답안 1이 "이 코드는 테스트 불가능한 게 아니다 — 셋 다 생성자 주입이라 가짜로 대체할 수 있다"고
짚었다. 이 정도는 **죽은 코드도 만족하던 수준**이지 rule arm에 유리한 힌트가 아니다.

**3. "호출부 깨짐"은 측정되지 않았다.** B-2 답안 2가 예측한 그 지표는, 계약을 고정하는 순간 두 arm에
동일하게 걸려 변별력이 없어진다. 대신 "기존 3-인자 진입점을 유지하라"고 요구해 기존 4개 시나리오가 계속
GREEN인지로 "기존 호출부가 보존되는가"만 잡았다.

**4. provided javadoc이 축 정당화 근거를 흘렸다.** rule arm의 `Points` 포트가 자기 정당화로
*"기존 포인트 API가 스스로 '운영에서는 포인트 서버, 이 실험에서는 메모리 시뮬레이터'라고 밝히고 있으니
이 축은 이미 둘로 늘어나 있다"* 고 적었다. **그 문장은 내가 `provided/PointApi` javadoc에 쓴 것이다.**
rule arm의 포트 개수는 `rules.md`만이 아니라 내 javadoc 문구에도 일부 기인한다. 양쪽이 같은 javadoc을
봤으니 편향은 아니고 상수지만(무제약 arm은 무시했다), rule의 순수 효과를 과대평가하지 않으려면 기록해야
한다.

**5. 1차 실행은 커밋 메시지 라벨에 오염됐다.** §2에 기록했다. 이 리포트의 수치는 전부 2차 기준이다.

**6. 결제사 교체는 사전 등록되지 않았다.** 포인트 결제와 1인 1매만 예측을 잠근 뒤 실행했고, 결제사 교체는
그 둘의 결과를 보고 "포트가 끊은 축이 측정되지 않았다"는 걸 깨달아 추가했다. **사후 탐색**이다.
결론에서 제일 중요한 자리를 사후에 추가한 축이 떠받치고 있다는 점을 밝혀 둔다.

---

## 부록: 재현 방법

```bash
# 전체 실험 (8 런)
bash run-experiment.sh

# 측정
bash measure.sh
```

- 실험 코드: [`code/no-rule/`](code/no-rule) · [`code/with-rule/`](code/with-rule) — arm × 4 브랜치
- 공통 harness: [`harness/`](harness) — `provided` + 인수테스트 + `pom.xml`
- 입력: [`inputs/`](inputs) — 요구사항·계약·규칙·프롬프트 원문
- 실행 로그: [`logs/`](logs) — 8 런의 전체 stream (도구 호출 포함)
