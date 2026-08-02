# Task C-4: 재현 가능한 환경 (Docker, Stateless, docker-compose)

(Grit's Why): '제 컴퓨터에선 됐는데요'는 시스템이 아닙니다. 어디서든 같게 도는 환경이 곧 신뢰입니다.

### 수행 내용

1. 애플리케이션을 Dockerfile로 이미지화하고, docker-compose로 앱 + DB(+ 필요 시 cache/queue)를 한 번에 띄우세요. docker compose up 한 줄로 기동되어야 합니다.
2. 상태를 외부화(Stateless)해서 수평 확장이 가능한 구조로 두세요. 무엇을 왜 외부로 뺐는지 적으세요.
3. Spring Boot 헬스 체크 엔드포인트를 두고, 컨테이너가 정상 기동했는지 확인하는 방법을 적으세요.

### 제출물

- [x] Dockerfile + docker-compose.yml을 GitHub에.
- [x] docker compose up으로 1-command 기동되는 화면 또는 로그.
- [x] Stateless 설계 메모(무엇을 왜 외부화했는가). (최소 300자)

---

## 산출물 위치

- 프로젝트: [`../ticket-reservation/`](https://github.com/woong7361/architecture-thinking/tree/main/task3/ticket-reservation) — task2 B-5 티켓 예매 도메인을 헥사고날(포트/어댑터)로 재배치하고 Spring Boot(web/jpa/actuator)로 감싼 walking skeleton.
- [`Dockerfile`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/Dockerfile) · [`docker-compose.yml`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/docker-compose.yml) · [`docker-compose.scale.yml`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/docker-compose.scale.yml) · [`.env`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/.env)
- [`README.md`](https://github.com/woong7361/architecture-thinking/blob/main/task3/ticket-reservation/README.md) (실행/검증 안내)

## 1-command 기동 커맨드 &amp; 로그

```bash
docker compose up --build      # db(mysql:8.4) + app + mock-pg 를 한 번에 기동
```

DB가 healthy 된 뒤 app 이 기동한다(compose `depends_on: service_healthy`). `app`과 `db`는 healthcheck로 healthy 상태를 확인했고, `mock-pg`는 WireMock 스텁 컨테이너로 함께 기동된다:

```
NAME                           STATUS                    PORTS
ticket-reservation-app-1       Up (healthy)              0.0.0.0:8080->8080/tcp
ticket-reservation-db-1        Up (healthy)              0.0.0.0:3306->3306/tcp
ticket-reservation-mock-pg-1   Up                         8080/tcp, 8443/tcp
```

## 정상 기동 확인 (헬스 체크)

`/actuator/health` 의 `db` 인디케이터가 DB 연결까지 검사한다 → `UP` 이면 정상 기동:

```bash
$ curl http://localhost:8080/actuator/health
{"status":"UP","components":{"db":{"status":"UP","details":{"database":"MySQL"}}, ...}}
```

기동 후 스모크: `POST /api/reservations` 로 예약 성공 시 `200 {"reserved":true}`.

## Stateless 설계 메모 (무엇을 왜 외부화했나)

이 앱은 티켓 예매 서버다. 핵심은 **앱이 데이터를 자기 안에 들고 있지 않게 만드는 것**이다.

- **무엇을 뺐나 — 예약/티켓/회원 데이터를 MySQL로.** 원래 이 데이터는 앱 메모리에 있었다. 그러면 앱을 2대 띄웠을 때 각 앱이 서로 다른 데이터를 갖게 되어 확장이 불가능하다. 그래서 데이터를 앱 밖의 MySQL 한 곳으로 옮겼다. DB 접속 정보 같은 설정도 코드에 박지 않고 `.env`(환경변수)로 뺐다.
- **왜 — 앱을 "무상태(stateless)"로 만들려고.** 앱이 아무 데이터도 안 들고 있으면, 같은 앱을 여러 개 띄워도 전부 똑같이 동작한다. 어떤 앱이 어떤 요청을 받든 결과가 같다.
- **참고 — 결제 Mock 서버(`mock-pg`)는 상태 외부화가 아니다.** 실제 결제사(PG)를 로컬에서 부를 수 없어 가짜 서버로 대신 띄운 것뿐이다. 앱은 이걸 HTTP로 호출한다.
