# ticket-reservation-c6 (walking skeleton, 파이프라인 산출)

HTTP 요청 하나가 **컨트롤러 → Inbound Port → Core → Outbound Port → 어댑터 → 실제 MySQL**까지
끝에서 끝까지 도는 가장 얇은 골격. `src/main` 은 전부 **AI 파이프라인이 레이어 단위로 생성**했고,
계약(`core/domain`·`core/port`)과 심판(`src/test`)은 사람이 고정했다.

- 손으로 세운 같은 골격: [`../ticket-reservation/`](../ticket-reservation/) — C-4·C-5의 산출물
- 파이프라인·검수 로그: [`../../.codex/skills/skeleton-agent/`](../../.codex/skills/skeleton-agent/)
- 과제 답안: [`../assignments/taskC-6.md`](../assignments/taskC-6.md)

## 이 폴더가 따로 있는 이유

`../ticket-reservation/` 은 C-4·C-5 답안이 링크로 참조한다. 그 히스토리를 덮지 않기 위해
파이프라인 산출물을 별도 폴더로 둔다. **두 폴더는 같은 심판·같은 계약을 쓰고 `src/main` 만 다르다.**
둘 다 CI에서 같은 인수테스트를 통과하므로, 손으로 만든 것과 생성된 것을 나란히 비교할 수 있다.

## 구조 (의존성은 항상 안쪽 = Core 를 향한다)

```
com.thinking.ticket
├─ core                        사람이 고정한 계약 — 파이프라인이 수정할 수 없다
│  ├─ domain      Ticket · User · DiscountPolicy · *Exception   (순수 자바)
│  ├─ port.in     ReserveTicketUseCase · Command · Result
│  └─ port.out    LoadTicketPort · SaveTicketPort · LoadUserPort · ChargePort
├─ core.application            L0 생성 — TicketService
├─ adapter.out                 L1 생성 — JPA 어댑터 · Mapper · PgChargeAdapter
├─ config                      L2 생성 — CompositionRoot
└─ adapter.in.web              L3 생성 — Controller · ExceptionHandler · Request DTO
```

## 1-command 기동

> 사전: Docker Desktop 엔진이 켜져 있어야 함.

```bash
docker compose up --build
```

`db`(mysql:8.4) → healthy → `app` 순서로 뜬다. `mock-pg`(WireMock)는 결제사 스텁이다.
기동 환경에서만 `SQL_INIT_MODE=always` 가 주어져 `resources/data.sql` 의 데모 시드가 들어간다
(테스트 구성에는 주지 않으므로 시나리오가 만든 데이터만 존재한다).

시드: 티켓 20(예약가능) / 21(판매중지) / 22(이미예약) / 23(5만원, 할인대상), 회원 1.

## 기동 확인 + 스모크

```bash
curl http://localhost:8080/actuator/health      # {"status":"UP", db: UP ...}
```

```bash
# 성공 → 200
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"ticketId":20,"paymentInfo":"card-token"}'
# {"reserved":true,"ticketId":20,"userId":1}

# 결제 거절 → 402 (mock-pg 가 200 + {"approved":false} 로 답한다)
curl -i -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"ticketId":23,"paymentInfo":"declined-card"}'
```

거부 응답은 전부 `application/problem+json` 이다 — 판매 중지·이미 예약 409, 없는 회원·없는 티켓 404,
결제 거절 402.

**결제 왕복은 인수테스트가 검증하지 않는다.** 결제는 모든 테스트 구성에서 대역으로 대체되므로,
이 스모크가 그 구간의 유일한 확인이다.

## 테스트

```bash
mvn -B test
```

네 가지가 함께 돈다 — in-memory 구성 7 · 실제 MySQL 구성 7 · HTTP 관통 구성 7 · 경계 규칙 5 = **26개**.

로컬에 JDK/Maven이 없으면 컨테이너 안에서 실행한다. Testcontainers가 컨테이너 안에서 도커에 붙으려면
API 버전을 맞춰 줘야 한다 — 최신 엔진은 docker-java의 낡은 기본값을 400으로 거절하고,
이 값은 환경변수가 아니라 시스템 프로퍼티로만 읽히며 테스트는 별도 JVM에서 돌기 때문에 `argLine` 으로 넣는다.

```bash
docker run --rm -v "$(pwd)":/app -w /app \
  -v maven-repo:/root/.m2 -v //var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  maven:3.9-eclipse-temurin-17 mvn -B test -DargLine=-Dapi.version=1.44
```

## 종료

```bash
docker compose down          # 컨테이너 제거 (DB 볼륨 유지)
docker compose down -v       # DB 볼륨까지 제거
```
