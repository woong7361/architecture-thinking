# ticket-reservation (Task C-4)

task2 B-5의 티켓 예매 도메인을 **헥사고날(포트/어댑터)** 로 재배치하고, **Docker 재현 환경**(app + MySQL + Mock PG) 위에 올린 walking skeleton.

- Stateless 설계 메모 + 기동 로그: `task3/assignments/taskC-4.md`
- 헥사고날 설계 원본: `task3/assignments/taskC-3.md`

## 구조 (의존성은 항상 안쪽 = Core 를 향한다)

```
com.thinking.ticket
├─ core
│  ├─ domain        Ticket · User · DiscountPolicy · *Exception   (순수 자바, 프레임워크 0)
│  ├─ application   TicketService (implements ReserveTicketUseCase)
│  └─ port
│     ├─ in         ReserveTicketUseCase · ReserveTicketCommand · ReservationResult
│     └─ out        TicketRepository · UserRepository · ChargePort
├─ adapter
│  ├─ in.web        ReservationController · ReservationExceptionHandler
│  └─ out
│     ├─ persistence  Ticket/UserJpaEntity · JpaRepository · PersistenceAdapter · DataSeeder
│     └─ payment      PaymentHttpAdapter  (→ mock-pg 를 HTTP 호출)
└─ config          CoreConfig (Composition Root)
```

## 1-command 기동

> 사전: Docker Desktop 엔진이 켜져 있어야 함.

```bash
docker compose up --build
```

`db`(mysql:8.4) → healthy → `app` 기동 순서로 뜬다. `mock-pg`(WireMock)는 결제사 스텁.

## 정상 기동 확인 (헬스 체크)

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"}, ...}}

docker compose ps          # app STATUS 가 healthy 인지 확인
```

## REST 데모 (walking skeleton)

시드 데이터: 티켓 20(예약가능) / 21(판매중지) / 22(이미예약) / 23(5만원, 할인대상), 회원 1.

```bash
# 성공 — 티켓 20 예약, 30000원 청구
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"ticketId":20,"paymentInfo":"card-token"}'
# {"reserved":true,"ticketId":20,"userId":1}

# 결제 거절 — mock-pg 가 declined-card 를 거절로 응답 → 402
curl -i -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"ticketId":23,"paymentInfo":"declined-card"}'

# 판매 중지 티켓 → 409 / 이미 예약된 티켓(22) 재예약 → 409
```

## 수평 확장(scale-out) 증빙

```bash
docker compose -f docker-compose.yml -f docker-compose.scale.yml up -d --build --scale app=3
docker compose ps          # app-1/2/3 이 모두 같은 db/mock-pg 를 보고 healthy
```

무상태 앱이라 3개가 동일하게 뜨고, 동시 예약은 DB 원자 UPDATE(`reserveIfFree`)가 이중 예약을 막는다.

## 테스트

Cucumber 인수테스트(7 시나리오)와 Testcontainers 통합 테스트, CI(GitHub Actions)는 **Task C-5**에서 다룬다 → `task3/assignments/taskC-5.md`.

로컬에서 바로 돌려보려면(로컬 JDK/Maven 불필요, 컨테이너 안에서 실행):

```bash
docker run --rm -v "$(pwd)":/app -w /app maven:3.9-eclipse-temurin-17 mvn -B test
```

## 종료

```bash
docker compose down          # 컨테이너 제거 (DB 볼륨 유지)
docker compose down -v       # DB 볼륨까지 제거
```
