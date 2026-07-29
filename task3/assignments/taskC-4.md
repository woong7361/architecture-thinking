# Task C-4: 재현 가능한 환경 (Docker, Stateless, docker-compose)

(Grit's Why): '제 컴퓨터에선 됐는데요'는 시스템이 아닙니다. 어디서든 같게 도는 환경이 곧 신뢰입니다.

### 수행 내용

1. 애플리케이션을 Dockerfile로 이미지화하고, docker-compose로 앱 + DB(+ 필요 시 cache/queue)를 한 번에 띄우세요. docker compose up 한 줄로 기동되어야 합니다.
2. 상태를 외부화(Stateless)해서 수평 확장이 가능한 구조로 두세요. 무엇을 왜 외부로 뺐는지 적으세요.
3. Spring Boot 헬스 체크 엔드포인트를 두고, 컨테이너가 정상 기동했는지 확인하는 방법을 적으세요.

### 제출물

- [x]  Dockerfile + docker-compose.yml을 GitHub에.
- [x]  docker compose up으로 1-command 기동되는 화면 또는 로그.
- [x]  Stateless 설계 메모(무엇을 왜 외부화했는가). (최소 300자)

---

## 산출물 위치

- 프로젝트: [`../ticket-reservation/`](../ticket-reservation/) — task2 B-5 티켓 예매 도메인을 헥사고날(포트/어댑터)로 재배치하고 Spring Boot(web/jpa/actuator)로 감싼 walking skeleton.
- [`Dockerfile`](../ticket-reservation/Dockerfile) · [`docker-compose.yml`](../ticket-reservation/docker-compose.yml) · [`docker-compose.scale.yml`](../ticket-reservation/docker-compose.scale.yml) · [`.env`](../ticket-reservation/.env)
- [`STATELESS.md`](../ticket-reservation/STATELESS.md) (설계 메모 전문) · [`README.md`](../ticket-reservation/README.md) (실행/검증 안내)

## 1-command 기동 커맨드 & 로그

```bash
docker compose up --build      # db(mysql:8.4) + app + mock-pg 를 한 번에 기동
```

빌드 스테이지(`mvn package`)가 인수테스트를 실행하므로, GREEN이 아니면 이미지가 만들어지지 않는다(빌드=검증 게이트):

```
#20 15.24 8 Scenarios (8 passed)
#20 15.24 53 Steps (53 passed)
#20 15.30 [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
#20 17.59 [INFO] BUILD SUCCESS
#22 naming to docker.io/library/ticket-reservation-app:latest done
```

기동 결과 — DB가 healthy 된 뒤 app 기동, 3개 컨테이너 모두 healthy:

```
NAME                           STATUS                    PORTS
ticket-reservation-app-1       Up (healthy)              0.0.0.0:8080->8080/tcp
ticket-reservation-db-1        Up (healthy)              0.0.0.0:3306->3306/tcp
ticket-reservation-mock-pg-1   Up (healthy)              8080/tcp, 8443/tcp
```

헬스 체크(정상 기동 확인) — `db` 인디케이터가 DB 연결까지 검사:

```bash
$ curl http://localhost:8080/actuator/health
{"status":"UP","components":{"db":{"status":"UP","details":{"database":"MySQL"}}, ...}}
```

REST end-to-end 실증: 성공 `200` / 이중예약·판매중지·이미예약 `409` / 결제거절 `402`(mock-pg HTTP 호출) / 없는회원 `404`.

## Stateless 설계 메모 (요약 · 전문은 STATELESS.md)

**"상태 외부화"는 서버를 여러 개 띄우는 게 아니라, 앱 프로세스가 요청 사이에 상태를 들고 있지 않게 만드는 것**이다. 컨테이너가 갈라지는 이유는 서로 다른 두 축이다.

- **축 A — 상태 외부화(→ MySQL):** 티켓 예약 여부·소유자·회원 데이터는 원래 앱 메모리(in-memory Map)에 있었다. 앱이 상태를 들면 인스턴스마다 데이터가 갈라져 수평 확장 자체가 성립하지 않으므로, 앱 밖의 **단일 진실원(MySQL)** 으로 뺐다. 설정·비밀은 `.env`(12-factor), HTTP 세션은 애초에 만들지 않아 sticky/Redis가 불필요하다.
- **축 B — 외부 의존 격리(→ Mock PG):** `mock-pg`(WireMock)는 상태 외부화가 아니라, 통제 불가한 외부 결제사를 재현 환경에서 대체한 스텁이다. 앱의 `PaymentHttpAdapter`가 `ChargePort`를 구현해 HTTP로 호출한다.

**수평 확장(scale-out):** 축 A 덕에 앱은 로컬 상태 0 → 동일 이미지를 `docker compose ... --scale app=3`으로 복제해도 모두 같은 MySQL을 본다(3인스턴스 healthy 확인). 무상태만으로 부족한 **동시 이중예약**은 DB의 단일 원자 UPDATE(`... WHERE reserved=false`)로 막아, 여러 인스턴스가 동시에 시도해도 실제로 행을 바꾸는 건 하나뿐이고 나머지는 `409 Conflict`로 거부된다.
