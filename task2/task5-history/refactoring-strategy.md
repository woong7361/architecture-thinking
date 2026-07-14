# 리팩토링 실행 전략 — task2 B-5 (TicketService)

`refactoring-criteria.md`(제출물 3)의 게이트를 실제 코드에 1회 적용한 **실행 계획**.
- **입력**: (목표 = 절차적 `reserveTicket`을 B-3 Rich Domain + B-4 SOLID로, 행위 보존) · (target = `reserveTicket`이 만지는 코드)
- **안전망**: `f97c1a7`의 Cucumber 6 시나리오. **매 커밋 GREEN**이 조건. 롤백 인프라는 없다(전제).

---

## §1. 스코프

| 구분 | 대상 |
|---|---|
| 만짐 | `reserveTicket`, `Ticket`, `PaymentApi`(→`ChargePort`) + 딸린 테스트 지원(seed·타입명) |
| LEAVE | `User`, `UserRepository`, `TicketRepository` 내부, 결제 실구현 어댑터 |

---

## §2. 스멜 탐지 (기준 1부 대조 — 걸린 행만)

| 기준# | 스멜 | Type | 신호 | 위치 |
|---|---|:---:|---|---|
| 22 | Anemic | A | `Ticket`은 필드+getter/setter만, 예약 규칙이 Service에서 벌어짐 | `Ticket.java` |
| 9 | Feature Envy | A | `reserveTicket`이 `ticket.isReserved()/setReserved()/setUserId()`를 조작 | `TicketService:27,36-37` |
| 6 | Mutable Data | A | `public setReserved/setUserId` — 외부가 상태를 아무 때나 변경 | `Ticket.java:29,37` |
| 3 | Long Function | A | 조회→검증→외부호출→저장 한 몸 + 단계 주석 | `TicketService:19-40` |
| 24 | Comments | A | `// 1. 유저 조회` 류 단계 설명 주석 | `TicketService:20,25,30,35` |
| 1 | Mysterious Name | A(약) | `PaymentApi` 이름이 역할(청구 포트)과 어긋남 | `PaymentApi.java` |
| 12 | Repeated Switches/OCP | **B** | 결제수단 다형성 축 — 단 지금 카드 1개뿐(잠재) | (없음) |

**후보 아님:** DIP/DI/인터페이스 도출 — 세 의존이 **이미 인터페이스 + 생성자 주입**(baseline given). DIP 이미 충족 → 가짜 "인터페이스 도출" 커밋 안 만듦.

---

## §3. 게이트 판정 (기준 2부)

| 스멜 | Type | 판정 | 출력 |
|---|:---:|---|:---:|
| #22 #9 #6 #3 #24 | A | 탐지=이득, 망 있음, 간접비≈0 | **GO** |
| #1 Mysterious Name | A | 구조 변화 없는 **순수 Rename**(포트 역할명 정렬). 가장 약함, 접어도 무방 | **GO(약)** |
| #12 결제 다형성 | B | `v = 카드1 + 확정0 = 1 < 2` | **DEFER** |

- **#6은 순서 제약**: setter는 #9·#22가 전이를 흡수한 **뒤에야** 안전 삭제(§4 C3).
- **DEFER 트리거**: "2번째 결제수단 요구 확정 시"(= 수행내용 5). 그때 `ChargePort` 다형성 도입. 지금은 안 만듦(YAGNI).
- **`ChargePort` 개명에 ISP/DIP 크레딧 없음**: `PaymentApi`는 이미 단일 메서드·이미 주입. 좁힐 너비도 뒤집을 방향도 없어 실효는 이름(#1)뿐. (B-4의 뚱뚱한 `PaymentApi`는 가상 예시였고 실제 코드엔 refund·queryHistory가 없다.)

---

## §3.5. 설계 제약 — 안전망이 검사/전이 분리를 강제한다 (계획의 축)

롤백이 없고 `InMemoryTicketRepository.findById`가 **저장된 같은 인스턴스**를 돌려주므로(`:31`), 인메모리 티켓을 전이하면 저장 전에도 관찰된다. 그래서 검사+전이를 한 몸으로 묶은 단일 `reserve()`는 **어디 놓아도 깨진다**:

| 위치 | 깨지는 시나리오 |
|---|---|
| `charge` 앞 | 결제 거절 → 전이가 이미 박혀 "티켓 미예약" 단언 RED |
| `charge` 뒤 | 이미예약 검사가 밀려 결제부터 됨 → "무청구" 단언 RED |

**→ 검사(charge 앞)와 전이(charge 뒤)를 결제 사이에 두고 분리한다:**

```
ticket.ensureReservable();                  // 검사 — 이미예약이면 throw(무청구)
if (!chargePort.charge(info, price)) throw; // 결제
ticket.assignTo(userId);                    // 전이 — 결제 성공 후에만
ticketRepo.save(ticket);
```

- Rich는 그대로 달성: 검사·전이가 **둘 다 `Ticket` 안**으로 들어간다(한 메서드가 아니라 역할 다른 두 메서드). `assignTo`도 내부에서 `reserved`를 방어 검사해 `Ticket`이 불변식의 완전한 주인이 된다(동작 불변, happy 경로 미트리거).
- **보존 quirk**: ①없는 티켓 → `ensureReservable()`를 null에 호출해도 여전히 NPE(null 체크 **안 넣음**). ②저장 실패 → charge→전이→save 순서 유지, 예외 그대로 전파·무보상.

---

## §4. 커밋 계획 (기준 2부 4단계 프록시 · 한 커밋 한 기법 · 매 커밋 GREEN)

| 커밋 | 단계 | 기법 | 무엇을 | 스멜 |
|:---:|:---:|---|---|:---:|
| C1 | 추출 | **Extract Function**(도메인으로 이동) | 검사 → `Ticket.ensureReservable()`, `if(isReserved)throw`를 호출로 교체 | #9 |
| C2 | 추출 | **Extract Function**(도메인으로 이동) | 전이 → `Ticket.assignTo(userId)`(+내부 guard), `setReserved+setUserId`를 호출로 교체 | #22 #9 |
| C3 | 제거 | **Remove Setting Method** | `setReserved`·`setUserId` 삭제, 테스트 seed는 `assignTo(0L)`로 | #6 |
| C4 | 개명 | **Rename / Change Function Declaration** | `PaymentApi` → `ChargePort`(필드·생성자·`RecordingPaymentApi`·import 반영) | #1 |
| C5 | 정리 | **주석 삭제**(#24 해소의 잔여 정리) | self-describing된 뒤 남은 단계 주석 `// 1.~// 4.` 삭제 | #24 |

- **순서 제약**: C3는 C2(전이 추출) **뒤**여야 GREEN(호출자 사라진 뒤 삭제). #3 Long Function은 C1·C2로 행위가 빠지며 **부수적으로** 해소.
- **명명 주의**: C1·C2는 인라인 문장을 다른 클래스로 빼므로 "Move Function"(기존 함수 이동)이 아니라 **Extract Function**. C5는 카탈로그 기법이 아니라 C1·C2·C4가 남긴 주석의 삭제 단계(딴 기법 안 섞음).
- **커밋 수**: C1~C5 = 5(+ 안전망 `f97c1a7` = C0). 요건 "최소 4~5" 충족.

### 커밋 메시지 예
```
refactor: 예약 가능 판단을 Ticket으로 추출 (Extract Function → 도메인으로 행위 이동)

Feature Envy(#9)/Anemic(#22): 인라인 ticket.isReserved() 검사를
Ticket.ensureReservable()로 추출해 Ticket이 자기 불변식을 소유하게 한다.
행위 보존 — charge 앞 위치·null NPE quirk 유지. 안전망 6 시나리오 GREEN.
```
