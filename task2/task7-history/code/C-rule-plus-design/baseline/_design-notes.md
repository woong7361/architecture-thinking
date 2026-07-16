# 설계 노트 — my-design.md 대비 되돌아온 지점

`my-design.md`는 "확정 스펙이 아니라 구현이 검증할 가설"이라고 스스로 밝힌다.
구현하며 ③(역할)이 틀렸음을 발견해 ②로 되돌아왔다. `rules.md` §1이 말한 정상적인 되돌아오기다.

---

## 1. 유지한 것 — `Ticket` (도메인)

**분류: A(재배치).** 새 추상화 축을 만들지 않고, 이미 있는 개념(`TicketRecord`) 사이에서
행위를 옮긴다. 간접비용 ≈ 0 → **GO**.

`rules.md` §3의 두 질문에 모두 "예":

1. **불변식이 있나?** 있다 — "이미 예약된 티켓은 다시 예약될 수 없다"(spec 규칙 2).
2. **담을 도메인 행위가 있나?** 있다 — `reserve(userId)`(판단 + 상태 전이).

`TicketRecord`는 `setReserved`/`setUserId`를 다 연 순수 anemic 레코드이고 **고칠 수 없다.**
그대로 쓰면 예약 규칙이 레코드 **밖**(서비스)에서 벌어진다. `Ticket`이 그 규칙의 주인이 된다.

## 2. 되돌린 것 — 포트 3개를 만들지 않는다

`my-design.md` §3은 `ChargePort` / `TicketRepository` / `UserRepository`를 두고
"§4의 게이트를 통과한 것만 만든다"고 적었지만, **v를 실제로 세지 않았다.** 세어 보면:

**분류: B(추상화 도입).** `v = (기존 구체 변형 수) + (확정 예정 변형 수)`

| 포트 | 기존 구체 | 확정 예정 (spec.md에 명시된 것만) | v | 판정 |
|---|---|---|---|---|
| `ChargePort` | 1 — `PaymentApi` | 0 — 환불·취소는 spec "범위 밖" | **1** | **DEFER** |
| `TicketRepository` | 1 — `TicketStore` | 0 | **1** | **DEFER** |
| `UserRepository` | 1 — `UserStore` | 0 | **1** | **DEFER** |

`v < 2 → DEFER`. 셋 다 만들지 않는다.

### 근거로 댄 축이 왜 무너지나

- **"결제수단 추가(OCP)"** — `spec.md`에 없다. 오히려 환불·취소를 "범위 밖"으로 명시했다.
- **"DB 교체"** — `spec.md`에 없다. "이미 회사에 있는 것을 **그대로** 쓴다"고만 적혀 있다.
- **"테스트 대체 축(Fake 주입)"** — **인수테스트를 읽어 반증했다.** `TicketReservationSteps`는
  Fake를 꽂지 않는다. 진짜 `TicketStore`·`UserStore`·`PaymentApi`를 그대로 생성자에 넘긴다.
  provided 3종이 **이미 메모리 시뮬레이터**라서 대체할 이유 자체가 없다. 이 축은 실재하지 않는다.
- **남은 근거는 javadoc의 "실제 운영에서는 …"뿐인데**, `rules.md` §4가 정확히 이것을 막는다:
  *"코드 주석이나 문서에 '실제 운영에서는 다를 수 있다'고 적혀 있는 것도 확정 요구가 아니다."*

### 만들었다면 켜졌을 C 신호

계약이 생성자를 `TicketService(TicketStore, UserStore, PaymentApi)`로 **못박아 두었다.**
그래서 포트를 두면 구체 타입을 받아 어댑터로 감싸는 층이 반드시 따라붙는다:

| 신호 | 무엇이 걸리나 |
|---|---|
| **Speculative Generality** | 구현이 **1개뿐인** 인터페이스 3개. 사용처도 1곳(`TicketService`)뿐. |
| **Lazy Element** | 어댑터 3장 전부 `store.findById(id)` 수준의 **한 줄 위임 래퍼**. |
| **Middle Man** | 어댑터의 메서드 **전부**가 provided 객체로 위임만 한다. |

### 여는 트리거 (DEFER는 "영원히 안 함"이 아니다)

아래 중 **하나라도 요구사항으로 확정되면** v가 2가 되므로 그 포트를 만든다.

- **`ChargePort`** ← 두 번째 결제수단(계좌이체·간편결제 등)이 요구사항에 들어올 때.
  또는 환불이 범위에 들어와 `charge`/`refund`를 **다른 클라이언트가 나눠 쓰게** 될 때(ISP).
- **`TicketRepository` / `UserRepository`** ← provided 저장소가 진짜 DB/원격 호출로 바뀌어
  테스트가 느려지거나, 두 번째 저장 매체가 확정될 때.

지금 하지 않는 이유는 "나중에 안 할 것"이라서가 아니라, **셌더니 v=1이라서**다.

## 3. 실효성을 잃은 가설 — h의 "보상 트랜잭션"

`my-design.md`는 `g` 실패 시 `e`를 **보상**한다고 적었다. 구현할 수 없다:

- `PaymentApi`에는 `charge`밖에 없다. **환불 통로 자체가 없다.**
- `spec.md`가 환불을 "범위 밖"으로 명시했다. 없는 요구를 미리 만들지 않는다.

**대신 순서로 원자성을 지킨다** — ①의 fail-fast 결정이 실제로 하는 일이 이것이다:

```
b 회원 확인 → c 대상 확보 → d·f ticket.reserve()  ← 모든 도메인 판단을 여기서 끝낸다
                                  e charge()        ← 통과한 뒤에야 처음으로 돈이 움직인다
                                  g save()          ← 청구 직후, 사이에 실패 지점 없음
```

거절 가능한 판단(b·d)이 **전부** 청구 앞에 오므로 "못 파는 자리에 돈부터 걷는" 일이 없고,
`e`와 `g` 사이에는 실패할 수 있는 코드가 없다. 보상이 필요한 창(window)을 **없애는 쪽**을 택했다.

### 이 순서가 드러낸 함정 — `Ticket`은 `TicketRecord`를 제자리에서 고치면 안 된다

`TicketStore`는 `seed`된 **바로 그 인스턴스**를 맵에 들고 있다. `Ticket`이 그 레코드의
`setReserved(true)`를 부르면 `save`를 안 불러도 저장소가 이미 오염된다 →
"결제 거절 시 티켓은 예약되지 않는다"(spec 규칙 3)가 깨진다.

그래서 `Ticket`은 **자기 상태를 따로 들고**, `toRecord()`로 **새 레코드를 만들어** 낸다.
저장소는 `save` 시점에만 바뀐다.

## 4. 최종 구조

```
        ┌─────────────────────────────────────────┐
        │  Ticket  「예약 불변식의 주인」 (도메인)  │   I/O 없음 → Mock 0개로 검증
        │  - reserved / userId / price            │
        │  + reserve(userId) … 이미 예약이면 거부  │
        └────────────────▲────────────────────────┘
                         │ d·f 판단·전이
        ┌────────────────┴────────────────────────┐
        │             TicketService                │   ← 유스케이스 흐름 (조립 · 순서)
        │   reserveTicket(userId, ticketId, info)  │
        └───┬──────────────┬──────────────┬───────┘
            ▼              ▼              ▼
       TicketStore     UserStore      PaymentApi        ← provided (이미 시뮬레이터)
```

**책임표 (변경 이유 하나 = SRP):**

| 행위 | 객체 | 정보 전문가 근거 | 변경 축 |
|---|---|---|---|
| d·f 예약 판단·전이 | `Ticket` | 예약 상태(`reserved`,`userId`)를 소유 | 예약 규칙 |
| a·순서·경계 | `TicketService` | 협력 순서를 앎 | 유스케이스 흐름 |
| b·c·e·g | provided 3종 | 저장 매체·외부 결제를 앎 | (고칠 수 없음) |

`Ticket`은 `TicketRecord`와 **매핑**(`from`/`toRecord`)만 알고 I/O는 모른다.
`rules.md` §3 "경계"가 금지하는 것은 도메인이 **I/O를 품는 것**이고, 저장 호출은
`TicketService`에 남아 있다. 그래서 `Ticket`은 `new` 하나로, Mock 없이 검증 가능한 상태다.

> 단, **단위테스트는 실제로 추가하지 못했다.** 계약이 "테스트 추가는 자유"라고 했지만
> `pom.xml`도 **고칠 수 없고**, 거기에 `junit-jupiter-api`가 없다(플랫폼·suite·cucumber뿐).
> `@Test`를 쓸 방법이 없어 인수테스트(Cucumber) 4개가 현재의 안전망 전부다.
> 네 시나리오가 spec의 규칙 1~4에 하나씩 대응해 유스케이스는 덮인다.

**Tell, Don't Ask.** 서비스는 `ticket.isReserved()`를 묻지 않는다 — `ticket.reserve(userId)`로
**시킨다.** 거부는 `Ticket`이 스스로 한다. `price()`를 여는 것은 규칙 노출이 아니다:
서비스는 그 값으로 **판단하지 않고** 청구에 넘기기만 한다. 반대로 `ticket.charge(payments, ...)`로
시켰다면 도메인이 I/O를 품어 §3의 경계를 깬다.
