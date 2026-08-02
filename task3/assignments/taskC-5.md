# Task C-5: Cucumber 인수테스트 + Testcontainers (포트와 어댑터가 인수테스트를 만나는 지점)

(Grit's Why): C-3에서 쓴 Gherkin이 여기서 실제로 돌아갑니다. 그리고 그 인수테스트가 'AI가 짠 코드가 맞는지'를 판정하는 결정론 게이트(1-1)와 같은 역할을 환경 위에서 합니다.

### 수행 내용

1. C-3의 Feature에 Cucumber-JVM Step Definition을 붙이고, 헥사고날 Core를 통과시켜 인수테스트를 초록불로 만드세요. Happy Path뿐 아니라 Unhappy Path(경계·실패·거절)도 포함하세요.
2. Testcontainers로 실제 DB(+ 외부 의존) 컨테이너를 띄워 통합 검증하세요. CI Runner에서도 동일하게 돌도록 GitHub Actions 워크플로를 구성하세요.
3. 포트/어댑터가 인수테스트와 만나는 지점을 설명하세요. 인수테스트는 Inbound Port를 호출하고, Outbound Adapter는 Testcontainers의 실제 인프라로 대체된다는 흐름을 그리세요.
4. 이제 어댑터를 실제로 갈아끼워 Core의 독립성을 증명해 보세요. Outbound Adapter(예: Testcontainers로 띄운 DB)를 다른 구현(인메모리 Fake나 다른 저장소)으로 교체하고, Core 코드는 한 줄도 바꾸지 않은 채 C-3에서 쓴 인수테스트가 그대로 GREEN인지 확인하세요. 어댑터를 바꾸려고 Core를 건드려야 했다면, 의존 방향이 안쪽을 향하지 않고 새고 있는 것입니다. 어디를 고쳐야 Core를 안 건드리고 교체되는지 적어 주세요.

### 제출물

- [x] Step Definition + 통과하는 도메인/어댑터 코드 + Testcontainers 설정을 GitHub에.
- [x] CI에서 인수테스트가 초록불인 실행 결과(GitHub Actions 로그).
- [x] 'Unhappy Path를 먼저 떠올린 과정' + 포트/어댑터-인수테스트 접점 설명. (최소 400자)
- [x] 어댑터 교체 전후(무엇을 무엇으로 바꿨는지)와 Core 무수정 증빙(Core diff 없음 + 인수테스트 GREEN 로그).

---

## 한눈에

같은 인수테스트(Cucumber Feature)를 **두 가지 방식**으로 돌린다.

- **방식 A** — 저장소를 앱 메모리(in-memory 가짜)로, 결제를 가짜 객체로 두고 실행. 빠르고 DB가 필요 없다.
- **방식 B** — 저장소를 **진짜 MySQL**(Testcontainers가 테스트 중에 컨테이너로 띄움) + JPA로 바꿔 실행.

## 산출물 위치

- Feature(시나리오): [`ticket_reservation.feature`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/resources/features/ticket_reservation.feature) · [`new_requirements.feature`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/resources/features/new_requirements.feature)
- 방식 A(in-memory): [`CucumberAcceptanceTest`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/CucumberAcceptanceTest.java) · [스텝](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/steps/TicketReservationSteps.java) · [가짜 어댑터](https://github.com/woong7361/architecture-thinking/tree/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/support)
- 방식 B(실제 MySQL/Testcontainers): [`JpaCucumberAcceptanceTest`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/jpa/JpaCucumberAcceptanceTest.java) · [Testcontainers 설정](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/jpa/CucumberSpringConfiguration.java) · [스텝](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/jpa/JpaTicketReservationSteps.java) · [결제 더블](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/src/test/java/com/thinking/ticket/jpa/TestPaymentConfig.java)
- CI: [`.github/workflows/ci.yml`](https://github.com/woong7361/architecture-thinking/blob/main/.github/workflows/ci.yml)

## CI에서 초록불 (배포 서버 없이)

GitHub Actions의 `ubuntu-latest` 러너에는 Docker가 깔려 있어, `mvn test` 한 줄이 방식 A·B를 모두 돌리고 방식 B는 Testcontainers가 **실제 MySQL 컨테이너를 자동으로 띄웠다 내린다.**

- ✅ 초록불 실행: [https://github.com/woong7361/architecture-thinking/actions/runs/30440557879](https://github.com/woong7361/architecture-thinking/actions/runs/30440557879)
- `mvn test`는 테스트가 하나라도 실패하면 빨간불이 되므로, 초록불 = **방식 A·B의 7개 시나리오가 모두 통과**했다는 뜻.

## Unhappy Path를 먼저 떠올린 과정 + 포트/어댑터가 인수테스트를 만나는 지점

**Unhappy Path는 경계 기준으로 잡았다.** E2E나 인수테스트에서 중요한 것은 내부 메서드가 어떤 예외를 던지는지가 아니라, 시스템 경계에서 관찰되는 실패가 올바른가다. 그래서 성공 시나리오 하나를 적기 전에 유스케이스가 만나는 경계를 따라 실패 목록을 만들었다. Inbound 쪽에서는 요청이 같은 `ReserveTicketUseCase.reserve`로 들어와야 한다. 조회 경계에서는 회원 없음과 티켓 없음이 거부되어야 하고, 도메인 규칙 경계에서는 이미 예약된 티켓과 판매 중지된 티켓이 거부되어야 한다. 결제 경계에서는 PG가 결제를 거절하면 예매가 실패하고 티켓이 예약되지 않아야 한다. 각 실패는 "무엇 때문에 거부되는가"와 "그때 바깥에서 관찰되는 부작용이 무엇인가"까지 함께 고정했다. 예를 들어 회원이 없거나 티켓이 이미 예약된 경우에는 결제 자체가 청구되지 않아야 하고, 결제 거절은 청구 시도는 있었지만 예약 확정은 없어야 한다.

**포트/어댑터가 인수테스트를 만나는 지점.** 인수테스트의 `When`(예: "회원 1이 티켓 20을 예매하면")은 **Inbound Port 하나**(`ReserveTicketUseCase.reserve`)를 호출한다. 테스트는 서비스 구현 클래스를 직접 만지지 않고 이 계약만 부른다. 반대편에서 도메인이 필요로 하는 조회·저장·결제는 **Outbound Port**(`TicketRepository`·`UserRepository`·`ChargePort`)로 정의돼 있고, 인수테스트를 돌릴 때 이 자리에 **어댑터를 끼워 넣는다** — 방식 A에선 in-memory 가짜가, 방식 B에선 Testcontainers가 띄운 실제 MySQL을 쓰는 JPA 어댑터가 들어간다. 여기서 `TicketRepository`와 `UserRepository`는 Spring Data JPA Repository가 아니라 Core가 소유한 Port다. 즉 인수테스트는 "안쪽(포트)으로 들어가서, 바깥(어댑터)은 상황에 맞게 갈아끼우는" 지점에서 만난다. `Then`의 상태 단언도 그 어댑터를 통해 읽는다(가짜 저장소 또는 실제 MySQL 조회). 덕분에 같은 문장이 두 인프라 위에서 똑같이 판정된다.

## 어댑터 교체 전후 + Core 무수정 증빙

같은 Feature를 두고 **저장 Outbound Adapter**를 바꿨다. 결제도 `ChargePort` 구현체가 테스트 구성별로 다르지만, 이번 증빙의 핵심은 저장소를 in-memory에서 실제 MySQL/JPA로 바꿔도 Core가 그대로라는 점이다.


| 자리(Outbound Port) | 방식 A (전) | 방식 B (후) |
| --- | --- | --- |
| 조회·저장 (`TicketRepository`/`UserRepository`) | `InMemoryTicketAdapter` 등 메모리 가짜 | `TicketPersistenceAdapter`(JPA) + **실제 MySQL**(Testcontainers) |

결제는 두 구성 모두 외부 PG를 직접 부르지 않고 통제 가능한 `ChargePort` 테스트 더블을 끼운다. 방식 A는 `RecordingPaymentApi`, 방식 B는 Spring 테스트 빈인 `TestChargePort`를 쓴다. 이는 결제 성공/거절을 결정론적으로 만들기 위한 테스트 구성 차이다.

- **Core(`core/` 디렉터리 = 도메인 + 유스케이스 + 포트)는 한 줄도 바꾸지 않았다.** 바뀐 것은 테스트 구성과 Outbound Adapter 구현뿐이다.
- 증빙: 위 CI 실행이 **방식 A·B를 함께 통과**했고, 두 방식은 같은 `features/` 파일과 같은 `core/` 코드를 공유한다.
- 정리 — **어댑터를 바꾸려고 Core를 건드릴 필요가 없었던 이유**: 도메인이 JPA·결제 SDK 같은 구체 기술이 아니라, 자기가 정의한 포트(`TicketRepository`·`UserRepository`·`ChargePort` 등)에만 의존하기 때문이다. 교체 지점은 항상 **포트 바깥(어댑터 + 조립부)** 이다. 만약 Core를 고쳐야 했다면 의존 방향이 바깥으로 새고 있다는 신호다.
