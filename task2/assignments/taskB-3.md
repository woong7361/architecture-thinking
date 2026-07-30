# Task B-3: 도메인에 '행위'를 되돌려주기 (Anemic vs Rich Domain Model)

(Grit's Why): B-2의 죽은 코드를 보면 데이터(Ticket)는 getter/setter만 있고 진짜 로직은 전부 Service에 흩어져 있습니다. 이것이 빈약한 도메인 모델(Anemic Domain Model)의 전형입니다. 객체지향의 핵심은 '데이터와 그 데이터를 다루는 행위가 한 객체 안에 같이 사는 것'입니다.

### 수행 내용

1. Anemic Domain Model과 Rich Domain Model을 리서치하고, 각각이 무엇이며 무엇을 잃고 얻는지 본인 언어로 정리해 주세요. (마틴 파울러의 'AnemicDomainModel' 글을 직접 인용)
2. B-2의 TicketService를 진단해 주세요. 어떤 로직이 Service에 있지만 사실 도메인 객체(Ticket 등)의 책임이어야 하는지 짚고, Rich Domain Model로 옮기면 무엇이 달라지는지(예: ticket.reserve(userId)가 상태 검증과 변경을 스스로 책임) 설명해 주세요.
3. '항상 Rich가 답인가'도 생각해 주세요. 단순 CRUD나 표현 계층 DTO처럼 Anemic이 합리적인 경우는 언제인지 본인 기준을 세워 주세요.

### 제출물

- [x] Anemic vs Rich Domain Model 정리 + 본인 입장. (최소 400자)
- [x] TicketService의 Anemic 징후 진단 + Rich로 옮길 때의 변화. (최소 300자)
- [x] Rich가 과한 경우(Anemic이 합리적인 경우)에 대한 본인 기준. (최소 200자)

---

## 답안 0: 마틴 파울러 'AnemicDomainModel' 원문 먼저 읽기 (요약)

- **증상.** 겉보기엔 진짜 도메인 모델 같다. 도메인 명사로 이름 붙은 객체들이 관계·구조까지 갖췄다. 그러나 행위를 보면 *"there is hardly any behavior on these objects, making them little more than bags of getters and setters"* — getter/setter 자루일 뿐이고, 로직은 Service로 밀려나 있다.
- **근본 문제 — 객체지향의 부정.** 이 안티패턴의 핵심 공포는 **데이터와 행위(process)를 결합**한다는 객체지향 설계의 기본에 정면으로 반한다는 점이다. *"The anemic domain model is really just a procedural style design, exactly the kind of thing that object bigots like me (and Eric) have been fighting since our early days in Smalltalk."* 더 나쁜 건 **많은 사람이 이 빈약한 객체를 진짜 객체로 착각해, 객체지향의 요점을 통째로 놓친다**는 것이다.
- **비용만 내고 이득은 없음.** *"they incur all of the costs of a domain model, without yielding any of the benefits."* ORM·영속 계층 같은 도메인 모델 비용은 다 치르면서, 복잡한 로직을 객체로 조직하는 이득은 못 챙기고 트랜잭션 스크립트로 끝난다.
- **계층화와는 모순 아님.** 도메인에 행위를 넣는 것은 "도메인 로직을 영속성·프레젠테이션 책임과 분리"하는 계층화와 충돌하지 않는다. 도메인 객체에 들어갈 것은 **도메인 로직 — 검증·계산·비즈니스 규칙**이지, DB 접근이나 화면 로직이 아니다.
- **Service Layer 자체는 죄가 아님.** 도메인 모델 위에 절차적 Service Layer를 얹는 건 정당하다. 다만 *"this isn't an argument to make the domain model void of behavior"* — 도메인을 행위 없는 껍데기로 만들라는 뜻이 아니다. Service 옹호자도 **행위가 풍부한 도메인과 함께** Service를 쓴다. Eric Evans(DDD) 역시 로직은 도메인에 두라며 경고한다. *"the more common mistake is to give up too easily on fitting the behavior into an appropriate object, gradually slipping toward procedural programming."*
- **결론.** *"If all your logic is in services, you've robbed yourself blind."* 로직을 전부 Service에 두면 스스로를 탈탈 털어먹는 것이다.

---

## 답안 1: Anemic vs Rich Domain Model 정리 + 본인 입장

**정의.**

- **Anemic Domain Model(빈약한 도메인 모델).** 도메인 객체가 상태(필드)와 그에 대한 getter/setter만 갖고, **비즈니스 규칙·검증·상태 전이 같은 행위는 갖지 않는** 모델이다. 행위는 전부 도메인 바깥의 Service(트랜잭션 스크립트)로 나가고, 객체는 데이터 운반 그릇 역할만 한다. 파울러가 안티패턴으로 지목한 대상이며, 구조적으로는 데이터와 로직이 분리된 **절차지향 설계**다.
- **Rich Domain Model(풍부한 도메인 모델).** 데이터와 **그 데이터를 다루는 도메인 로직(검증·계산·비즈니스 규칙·상태 전이)이 같은 객체 안에 사는** 모델이다. 객체는 공개 setter로 아무렇게나 바뀌지 않고, `reserve(userId)`처럼 **의미 있는 행위 메서드**를 통해서만 상태가 바뀌며, 그 메서드가 불변식(invariant)을 스스로 지킨다. 데이터와 행위를 한 객체에 결합한다는 객체지향 설계의 기본에 부합한다.
- **경계.** Rich라고 해서 영속성(DB)·프레젠테이션 로직까지 도메인에 넣는 것은 아니다. 도메인 객체가 품는 것은 **순수 도메인 로직**뿐이고, I/O는 여전히 Service·Repository 등 바깥 계층이 담당한다

**비유 — 인형 vs 로봇 장난감.**

- **Anemic Domain Model = 스스로 못 움직이는 인형.** 인형(`Ticket`)은 팔·다리(데이터: `reserved`, `userId`, `price`)는 다 갖췄지만 스스로는 아무것도 못 한다. 움직이려면 항상 주인(`TicketService`)이 팔을 이렇게, 다리를 저렇게 하나하나 조종해야 한다. 규칙("이미 예약된 인형은 다시 예약하면 안 돼")도 인형은 모른다. **주인이 대신 기억**해야 한다. 그런데 주인이 깜빡하고 이미 예약된 인형을 또 예약해도, 인형은 "나 벌써 예약됐는데?"라고 말을 못 하니까 그냥 잘못된 채로 넘어간다. 주인이 여러 명이면 그중 한 명만 규칙을 까먹어도 사고가 난다.
- **Rich Domain Model = 스스로 움직이는 로봇 장난감.** 로봇(`Ticket`)은 "예약해!" 버튼(`reserve(userId)`)만 누르면 **스스로** "나 이미 예약됐나?"를 확인하고, 괜찮을 때만 자기 상태를 바꾼다. 규칙이 로봇 몸속에 들어 있어서, 아무나 로봇 배를 열어 부품을 함부로 바꿀 수 없다(setter 봉인). 버튼을 누르는 사람(Service)은 규칙을 몰라도 되고, 그냥 "예약해"라고만 하면 된다.

한 문장으로: **Anemic은 "데이터는 여기, 행위는 저기" 로 갈라놓은 것**이고, **Rich는 "데이터와 그 데이터를 다루는 행위를 한 몸에 둔 것"** 이다(그 행위가 규칙·불변식을 스스로 지킨다).

**본인 입장.** 나는 파울러에 원칙적으로 동의한다 — **로직이 자연스럽게 붙을 도메인 객체가 있는데도 Service로 밀어내는 것**은 손해다. 다만 "무엇이 자연스러운가"의 기준을 나는 **정보 전문가(Information Expert)**로 잡는다. **어떤 데이터를 가진 객체는, 그 데이터를 다루는 도메인 로직(검증·계산·상태 전이)의 자연스러운 소유자다.** 그 로직을 데이터에서 떼어 Service로 보내면, 데이터를 가장 잘 아는 자와 그 데이터를 다루는 자가 갈라져 변경이 두 곳으로 번진다 — B-1에서 세운 OOP의 목적인 "변경의 파급을 한 곳에 국소화"를 정면으로 어기는 것이다. 그래서 그 로직은 데이터를 가진 객체 안에 둔다(Rich).

이 도메인 로직에는 강도 차이가 있다. 가장 강한 트리거는 **불변식 보호**다 — 지켜야 할 상태 규칙(예: 예약된 티켓은 재예약 불가)이 있으면, 그 규칙은 상태를 가진 객체가 강제해야 **잘못된 상태 자체를 만들 수 없다**. 불변식이 없어도, `ticket.priceWithFee()`처럼 **데이터에서 파생되는 계산**은 그 데이터를 가진 객체에 두는 편이 응집에 이롭다. 둘은 "데이터를 가진 자가 그 데이터의 로직을 가진다"는 **한 원리의 두 사례**이고, 불변식 유무는 그중 가장 강한 트리거일 뿐이다. 즉 Rich는 목표가 아니라 **정보 전문가에게 도메인 로직을 돌려주기 위한 수단**이다

---

## 답안 2: TicketService의 Anemic 징후 진단 + Rich로 옮길 때의 변화

**Anemic 징후.** B-2의 `Ticket`은 상태 + getter/setter만 있고 행위가 없다. 그중 **티켓 책임인데 Service에 나가 있는 것**은 두 개다. ① **예약 가능 판단**(`if (ticket.isReserved()) throw ...`) — 티켓 자신의 불변식을 바깥이 대신 검사한다. ② **상태 전이**(`setReserved(true); setUserId(userId);`) — 공개 setter라 하나만 부르면 **"주인 없는 예약"** 같은 잘못된 상태가 컴파일·저장까지 통과한다. 반면 유저·티켓 조회, 결제, 저장은 **I/O**라 Service에 남는 게 맞다. 즉 "전부 Rich"가 아니라 **결정(도메인) vs I/O(Service)의 경계를 다시 긋는** 문제다.

**Rich로 옮기면.** 판단+상태 전이를 `Ticket`으로 내린다.

```java
public class Ticket {
    private boolean reserved;
    private Long userId;
    private final Money price;

    public void reserve(long userId) {          // 상태 검증과 변경을 스스로 책임
        if (this.reserved) {
            throw new TicketAlreadyReservedException();
        }
        this.reserved = true;
        this.userId = userId;
    }
    // setReserved/setUserId(공개 setter)는 제거 → 외부에서 상태 못 깸
}
```

그러면 Service는 **조립(orchestration)만** 남는다.

```java
@Transactional
public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
    User user = userRepo.findById(userId);
    if (user == null) throw new UserNotFoundException();

    Ticket ticket = ticketRepo.findById(ticketId);
    ticket.reserve(userId);   // ① 가능 검사 + 상태 전이 (안 되면 여기서 throw → 결제 안 함)

    if (!paymentApi.charge(paymentInfo, ticket.getPrice())) {  // ② 자리 잡은 뒤 결제
        throw new PaymentFailedException();
    }
    ticketRepo.save(ticket);
    return true;
}
```

**달라지는 것:** ① **불변식 강제** — 공개 setter가 없어 잘못된 상태를 만들 수 없다. ② **값 테스트** — `new Ticket(...).reserve(...)`로 Mock 0개 검증. ③ **규칙이 한 곳** — 다른 흐름(관리자 강제예약 등)도 `reserve()` 하나를 재사용.

---

## 답안 3: Rich가 과한 경우(Anemic이 합리적인 경우)에 대한 본인 기준

'항상 Rich'는 아니다. 답안 1의 정보 전문가는 "행위가 있을 때 어디 두나"의 **배치 규칙**이라, 그 앞에 관문이 하나 있다 — **이게 도메인 개념인가, 순수 운반체인가.** 운반체는 소유할 도메인 로직이 애초에 없어서, 정보 전문가가 배제하는 게 아니라 **배치할 행위 자체가 없어** Anemic이 정직하다. 판정은 두 질문으로 한다 — ① 잘못된 상태를 막을 **불변식**이 있나? ② 데이터에서 나오는 **파생 계산이나 도메인 행위**가 있나? 둘 다 "아니오"면 그 개념은 값 운반체이므로 Anemic이 맞다.

- **DTO·API 요청/응답 모델.** 값을 계층 간 나르는 게 전부라 불변식도 행위도 없다. 오히려 행위를 넣으면 도메인 규칙이 경계 밖으로 샌다. getter/setter 자루가 맞다.
- **규칙 없는 단순 CRUD.** 설정값·코드 테이블처럼 저장/조회가 전부이고 상태 전이 규칙이 없는 대상. 억지로 Rich를 만들면 이득 없는 추상화 비용, 즉 과설계가 된다.
- **읽기 전용 조회 모델.** CQRS의 Query 쪽이나 리포트·뷰가 여기 해당한다. 상태를 바꾸지 않으니 강제할 불변식이 없어, 조회에 최적화된 평평한 구조가 낫다.

즉 Anemic은 "게을러서"가 아니라 지킬 불변식도 담을 행위·계산도 없다는 사실을 정직하게 반영한 선택일 때 합리적이다. 단 값 운반이던 객체에 규칙이나 계산이 붙기 시작하면 그때 Rich로 옮기면 된다. 판정은 "지금 이 객체가 무엇을 담고 있나"로 한다.

<!-- notion-feedback:begin -->

---

## 리뷰 피드백 (Notion 원본)

<!--
  출처 페이지 : [Phase 1] 1-2(객체지향) 제출 - 현웅님
  URL        : https://sponge-girdle-ad1.notion.site/Phase-1-1-2-38a6276f9e0081c8900dc7d1e58c8ad3
  수집 방법   : 프로젝트 루트 notion_mcp.md 참조
  원문 보존   : 댓글 본문은 Notion comment 레코드의 텍스트를 그대로 옮긴 것이며 일절 수정하지 않았다.
  라인 기준   : 이 섹션 위쪽 본문의 라인 번호. 본문을 편집하면 다시 수집해야 한다.
-->

리뷰어가 이 문서의 **어느 라인, 어떤 부분**에 **어떤 피드백**을 남겼는지 정리한 것이다.
총 8건 (댓글 7건, 리액션만 1건).

### FB-B3-01 · L38

- **위치**: L38
- **지적된 부분**: 문단 전체 — 비유 — 인형 vs 로봇 장난감.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:49 KST
- **피드백 원문**:

```
재밌네요!
```

### FB-B3-02 · L45

- **위치**: L45
- **지적된 부분**: 문단 전체 — 어떤 데이터를 가진 객체는, 그 데이터를 다루는 도메인 로직(검증·계산·상태 전이)의 자연스러운 소유자다.
- **유형**: 이모지 리액션 (댓글 본문 없음)
- **피드백 원문**: (없음 — 하이라이트에 리액션만 달렸다)

### FB-B3-03 · L45

- **위치**: L45
- **지적된 부분**: 문단 전체 — 본인 입장. 나는 파울러에 원칙적으로 동의한다 — 로직이 자연스럽게 붙을 도메인 객체가 있는데도 Service로 밀어내는 것은 손해다. 다만 "무엇이 자연스러운가"의 기준을 나는 **정보 전문가(Information Expert)**로 잡는다. 어떤 데이터를 가진 객체는, 그 데이터를 다루는 도메인 로직(검증·계산·상태  …
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:50 KST
- **피드백 원문**:

```
파울러의 안티패턴 지적을 정보 전문가 패턴과 연결해서 본인만의 기준으로 논리를 세우려고 하신 것 같아요. 

그렇다면 만약 특정 할인 로직을 계산할 때 사용자 등급 정보와 현재 진행 중인 전사 프로모션 정보, 그리고 개별 티켓의 속성까지 세 개 이상의 서로 다른 도메인 애그리거트가 가진 데이터가 모두 필요하다면, 이때는 도대체 누구를 진정한 정보 전문가로 임명하나요? 그리고 행위를 어느쪽에 맡겨야 하나요 ?
```

### FB-B3-04 · L47

- **위치**: L47
- **지적된 부분**: 문단 전체 — 이 도메인 로직에는 강도 차이가 있다. 가장 강한 트리거는 불변식 보호다 — 지켜야 할 상태 규칙(예: 예약된 티켓은 재예약 불가)이 있으면, 그 규칙은 상태를 가진 객체가 강제해야 잘못된 상태 자체를 만들 수 없다. 불변식이 없어도, ticket.priceWithFee()처럼 데이터에서 파생되는 계산은 그 데이터를 가진 …
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:51 KST
- **피드백 원문**:

```
흠~~ 불변식 보호를 도메인 로직 이동의 가장 강력한 트리거로 삼은 기준 좋다고 생각합니다. 

그런데 만약 티켓 예매 불가 규칙이라는 그 불변식 자체가 매주 기획 팀의 변덕에 따라 수시로 바뀌는 극도로 휘발성이 높은 정책일 수도 있나요?

이 잦은 변경을 티켓 도메인 객체 한가운데에 계속 품고 가는 것이 과연 유지보수 관점에서 최선일지, 아니면 이 규칙 자체를 밖으로 빼내어 별도의 정책 객체로 분리해야 할지는 어떻게 판단할까요?
```

### FB-B3-05 · L80

- **위치**: L80 (코드/다이어그램 블록 내부)
- **지적된 부분**: 코드·다이어그램 일부 — if (user == null) throw new UserNotFoundException();
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:51 KST
- **피드백 원문**:

```
여기서 이 if 문은 필수불가결할까요 ?
```

### FB-B3-06 · L85

- **위치**: L85 (코드/다이어그램 블록 내부)
- **지적된 부분**: 코드·다이어그램 일부 — if (!paymentApi.charge(paymentInfo, ticket.getPrice())) { // ② 자리 잡은 뒤 결제
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:52 KST
- **피드백 원문**:

```
마찬가지로 여기의 if 문도요 ?
```

### FB-B3-07 · L78

- **위치**: L78 (코드/다이어그램 블록 내부)
- **지적된 부분**: 코드·다이어그램 일부 — public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:52 KST
- **피드백 원문**:

```
예매 로직이 통과해서 티켓 객체의 상태는 이미 메모리 상에서 예약으로 바뀌었는데 바로 다음 줄 결제 API 호출에서 실패 예외가 터져버린 상황은 어떨까요?
```

### FB-B3-08 · L105

- **위치**: L105
- **지적된 부분**: 문단 전체 — 즉 Anemic은 "게을러서"가 아니라 지킬 불변식도 담을 행위·계산도 없다는 사실을 정직하게 반영한 선택일 때 합리적이다. 단 값 운반이던 객체에 규칙이나 계산이 붙기 시작하면 그때 Rich로 옮기면 된다. 판정은 "지금 이 객체가 무엇을 담고 있나"로 한다.
- **유형**: 댓글
- **작성자 / 시각**: 그릿 · 2026-07-23 21:53 KST
- **피드백 원문**:

```
빈약한 모델을 무조건 악으로 규정하지 않고 담을 행위가 없는 객체의 정직한 상태로 인정해준 균형 매우 좋다고 생각합니다. 

다만 실무에서는 처음엔 단순한 값 운반체였던 객체에 야금야금 작은 계산 로직 하나씩이 묻기 시작하면서 서서히 절차지향의 늪으로 빠지는 경우가 훨씬 많죠.
```

