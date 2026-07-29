# Task C-5: Cucumber 인수테스트 + Testcontainers (포트와 어댑터가 인수테스트를 만나는 지점)

(Grit's Why): C-3에서 쓴 Gherkin이 여기서 실제로 돌아갑니다. 그리고 그 인수테스트가 'AI가 짠 코드가 맞는지'를 판정하는 결정론 게이트(1-1)와 같은 역할을 환경 위에서 합니다.

### 수행 내용

1. C-3의 Feature에 Cucumber-JVM Step Definition을 붙이고, 헥사고날 Core를 통과시켜 인수테스트를 초록불로 만드세요. Happy Path뿐 아니라 Unhappy Path(경계·실패·거절)도 포함하세요.
2. Testcontainers로 실제 DB(+ 외부 의존) 컨테이너를 띄워 통합 검증하세요. CI Runner에서도 동일하게 돌도록 GitHub Actions 워크플로를 구성하세요.
3. 포트/어댑터가 인수테스트와 만나는 지점을 설명하세요. 인수테스트는 Inbound Port를 호출하고, Outbound Adapter는 Testcontainers의 실제 인프라로 대체된다는 흐름을 그리세요.
4. 이제 어댑터를 실제로 갈아끼워 Core의 독립성을 증명해 보세요. Outbound Adapter(예: Testcontainers로 띄운 DB)를 다른 구현(인메모리 Fake나 다른 저장소)으로 교체하고, Core 코드는 한 줄도 바꾸지 않은 채 C-3에서 쓴 인수테스트가 그대로 GREEN인지 확인하세요. 어댑터를 바꾸려고 Core를 건드려야 했다면, 의존 방향이 안쪽을 향하지 않고 새고 있는 것입니다. 어디를 고쳐야 Core를 안 건드리고 교체되는지 적어 주세요.

### 제출물

- [x]  Step Definition + 통과하는 도메인/어댑터 코드 + Testcontainers 설정을 GitHub에.
- [x]  CI에서 인수테스트가 초록불인 실행 결과(GitHub Actions 로그).
- [x]  'Unhappy Path를 먼저 떠올린 과정' + 포트/어댑터-인수테스트 접점 설명. (최소 400자)
- [x]  어댑터 교체 전후(무엇을 무엇으로 바꿨는지)와 Core 무수정 증빙(Core diff 없음 + 인수테스트 GREEN 로그).

---

## 한눈에

같은 인수테스트(Cucumber Feature)를 **두 가지 방식**으로 돌린다.

- **방식 A** — 저장소를 앱 메모리(in-memory 가짜)로, 결제를 가짜 객체로 두고 실행. 빠르고 DB가 필요 없다.
- **방식 B** — 저장소를 **진짜 MySQL**(Testcontainers가 테스트 중에 컨테이너로 띄움) + JPA로 바꿔 실행.

두 방식 모두 **같은 Feature 파일 · 같은 도메인(Core) 코드**를 쓰고, **바깥 어댑터만** 교체한다. 둘 다 초록불이면 "도메인은 저장 기술이 뭐든 그대로 동작한다"가 증명된다.

## 산출물 위치

- Feature(시나리오): [`ticket_reservation.feature`](../ticket-reservation/src/test/resources/features/ticket_reservation.feature) · [`new_requirements.feature`](../ticket-reservation/src/test/resources/features/new_requirements.feature)
- 방식 A(in-memory): [`CucumberAcceptanceTest`](../ticket-reservation/src/test/java/com/thinking/ticket/CucumberAcceptanceTest.java) · [스텝](../ticket-reservation/src/test/java/com/thinking/ticket/steps/TicketReservationSteps.java) · [가짜 어댑터](../ticket-reservation/src/test/java/com/thinking/ticket/support/)
- 방식 B(실제 MySQL/Testcontainers): [`JpaCucumberAcceptanceTest`](../ticket-reservation/src/test/java/com/thinking/ticket/jpa/JpaCucumberAcceptanceTest.java) · [Testcontainers 설정](../ticket-reservation/src/test/java/com/thinking/ticket/jpa/CucumberSpringConfiguration.java) · [스텝](../ticket-reservation/src/test/java/com/thinking/ticket/jpa/JpaTicketReservationSteps.java) · [결제 더블](../ticket-reservation/src/test/java/com/thinking/ticket/jpa/TestPaymentConfig.java)
- CI: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)

## CI에서 초록불 (배포 서버 없이)

GitHub Actions의 `ubuntu-latest` 러너에는 Docker가 깔려 있어, `mvn test` 한 줄이 방식 A·B를 모두 돌리고 방식 B는 Testcontainers가 **실제 MySQL 컨테이너를 자동으로 띄웠다 내린다.** 내 서버를 따로 둘 필요가 없다(러너는 잡이 끝나면 사라지는 일회용 VM이다).

- ✅ 초록불 실행: https://github.com/woong7361/architecture-thinking/actions/runs/30440557879
- `mvn test`는 테스트가 하나라도 실패하면 빨간불이 되므로, 초록불 = **방식 A·B의 7개 시나리오가 모두 통과**했다는 뜻.

## Unhappy Path를 먼저 떠올린 과정 + 포트/어댑터가 인수테스트를 만나는 지점

**Unhappy Path 먼저.** 예매가 "성공하는" 한 줄(Happy Path)은 쉽지만, 실제로 시스템을 지키는 건 "실패해야 할 때 제대로 실패하는가"다. 그래서 성공 시나리오 하나를 적기 전에, **거부되어야 하는 경우들을 먼저** 나열했다: 등록되지 않은 회원인가, 판매 중지된 티켓인가, 이미 예약된 티켓인가, 결제가 거절됐는가, 아예 없는 티켓인가. 각각은 "무엇 때문에 거부되고, 그때 결제는 일어나면 안 된다"까지 함께 못박았다(예: 결제 거절이면 티켓은 예약되지 않고, 회원이 없으면 결제 자체가 시도되지 않는다). 이렇게 실패 경로를 먼저 고정하면, 나중에 코드를 어떻게 리팩터링하든 "조용히 잘못 동작하는" 회귀를 인수테스트가 잡아낸다.

**포트/어댑터가 인수테스트를 만나는 지점.** 인수테스트의 `When`(예: "회원 1이 티켓 20을 예매하면")은 **Inbound Port 하나**(`ReserveTicketUseCase.reserve`)를 호출한다. 테스트는 서비스 구현 클래스를 직접 만지지 않고 이 계약만 부른다. 반대편에서 도메인이 필요로 하는 저장·결제는 **Outbound Port**(`TicketRepository`·`UserRepository`·`ChargePort`)로 정의돼 있고, 인수테스트를 돌릴 때 이 자리에 **어댑터를 끼워 넣는다** — 방식 A에선 in-memory 가짜가, 방식 B에선 Testcontainers가 띄운 실제 MySQL을 쓰는 JPA 어댑터가 들어간다. 즉 인수테스트는 "안쪽(포트)으로 들어가서, 바깥(어댑터)은 상황에 맞게 갈아끼우는" 지점에서 만난다. `Then`의 상태 단언도 그 어댑터를 통해 읽는다(가짜 저장소 또는 실제 MySQL 조회). 덕분에 같은 문장이 두 인프라 위에서 똑같이 판정된다.

## 어댑터 교체 전후 + Core 무수정 증빙

같은 Feature를 두고 **바깥 어댑터만** 바꿨다:

| 자리(Outbound Port) | 방식 A (전) | 방식 B (후) |
| --- | --- | --- |
| 저장 (`TicketRepository`/`UserRepository`) | `InMemoryTicketRepository` 등 메모리 가짜 | `TicketPersistenceAdapter`(JPA) + **실제 MySQL**(Testcontainers) |
| 결제 (`ChargePort`) | `RecordingPaymentApi` 가짜 | `TestChargePort`(통제 가능한 더블) |
| 조립(어디서 끼우나) | 스텝이 `new TicketService(...)`로 직접 조립 | Spring 컨텍스트가 주입(`@SpringBootTest`) |

- **Core(`core/` 디렉터리 = 도메인 + 유스케이스 + 포트)는 한 줄도 바꾸지 않았다.** 바뀐 것은 (1) 테스트 스텝의 조립 방식과 (2) Outbound 어댑터 구현뿐이다.
- 증빙: 위 CI 실행이 **방식 A·B를 함께 통과**했고, 두 방식은 같은 `features/` 파일과 같은 `core/` 코드를 공유한다.
- 정리 — **어댑터를 바꾸려고 Core를 건드릴 필요가 없었던 이유**: 도메인이 JPA·결제 SDK 같은 구체 기술이 아니라, 자기가 정의한 포트(`TicketRepository`·`ChargePort` 등)에만 의존하기 때문이다. 교체 지점은 항상 **포트 바깥(어댑터 + 조립부)** 이다. 만약 Core를 고쳐야 했다면 의존 방향이 바깥으로 새고 있다는 신호다.
