# 판정용 Spring 스켈레톤

이 디렉터리는 생성된 API 구현 초안을 판정하기 위한 장치다. 파이프라인은 AI가 만든 초안 파일을 이 스켈레톤
안에 떨어뜨려 컴파일하고 부팅한 뒤, springdoc이 내는 `/v3/api-docs`를 받아 그것을 그 iteration의 계약 판본과
대조한다. 구현이 무엇을 약속했는지를 AI가 쓴 설명에서 받지 않고 부팅한 애플리케이션에서 뽑는 것이 요점이다.
설명을 받으면 "스펙대로 짰느냐"를 스펙대로 짠 쪽에 되묻는 일이 된다.

스켈레톤은 사람이 소유한다. 생성기는 아래 "생성기가 고칠 수 없는 파일"에 적힌 파일을 고칠 수 없고, 계약을
지키기 위해 그 시그니처가 불편하더라도 직접 바꾸지 않고 이유를 보고한다. 포트와 ArchUnit 규칙은 여러 iteration과
게이트가 함께 의존하는 계약이라, 한 iteration의 사정으로 바꾸면 다음 iteration의 판정 기준이 조용히 달라진다.

## 판정에 필요한 것만 담았다

의존은 web, actuator, springdoc, ArchUnit, spring-boot-starter-test 다섯이다. 저장은 인메모리이고 외부 예약처는
포트의 인메모리 구현으로 대체하므로 JPA와 MySQL과 Testcontainers는 넣지 않았다. 게이트가 iteration마다 부팅하니
부팅이 빨라야 하고, 계약을 지켰는지 판정하는 데 실제 데이터베이스가 필요하지 않다. 인수테스트용 cucumber도
이 조각의 판정 수단이 아니라서 없다.

actuator는 사람이 읽는 운영 화면이 아니라 부팅이 끝났는지 확인하는 신호로만 쓴다. `/actuator/health` 하나만
노출한다. springdoc도 화면 없이 `api`만 쓴다.

## 생성 코드가 어디에 무엇을 놓는가

패키지 규약은 `.codex/skills/contract-impl-agent/BUILD.md`가 정한 것과 같다. 컴포넌트 스캔 기준이
`com.thinking.tennis`이므로 그 하위에 놓인 스프링 빈은 따로 등록하지 않아도 잡힌다.

| 패키지 | 놓는 것 | 소유 |
| --- | --- | --- |
| `com.thinking.tennis.api` | 컨트롤러, 요청·응답 DTO, 에러 응답 매핑 | 생성 코드 |
| `com.thinking.tennis.app` | 유스케이스. 포트를 엮는 순서 | 생성 코드 |
| `com.thinking.tennis.domain` | 도메인 규칙과 상태 | 생성 코드 |
| `com.thinking.tennis.port` | 포트 인터페이스 | 사람 |
| `com.thinking.tennis.adapter` | 아웃바운드 어댑터 | 사람이 둔 인메모리 구현이 있다 |

생성 코드가 저장을 위한 포트를 새로 만들어야 하면 `port`에 인터페이스를 두고 `adapter`에 인메모리 구현을
둔다. 이때 만드는 파일은 생성 코드의 것이므로 사람이 소유한 `CourtAvailabilityPort`와
`InMemoryCourtAvailabilityAdapter`는 그대로 둔다.

`ArchUnit` 규칙이 다음을 강제한다. `domain`과 `app`은 `jakarta.servlet`·`org.springframework.web`·
`com.fasterxml.jackson` 타입을 참조하지 않고, `api`는 `adapter`를 참조하지 않으며, `domain`은 `api`를 참조하지
않는다. 규칙마다 빈 패키지를 허용해 두었으므로 생성 코드가 없는 상태에서도 초록불이다. 빈 상태에서 빨간불이
켜지면 "경계를 어겼다"와 "아직 아무것도 없다"를 구별할 수 없다.

## 외부 예약처는 포트 뒤에 있다

`CourtAvailabilityPort`가 계약의 `GET /courts/{courtId}/availability`가 필요로 하는 것만 드러낸다. 코트를
지원하는지, 그 날짜에 운영하는 시간대마다 마지막 확인 시점에 비어 있었는지, 그 확인이 언제 성공했는지, 그리고
확인이 실패했다면 무엇이 실패했는지다. 확인 실패는 상태 코드가 아니라 검사 예외 `CheckFailed`로 나온다.
저장된 성공 결과가 있으면 200에 `stale`을 세우고 없으면 5xx로 알리라는 계약의 갈림길을 호출하는 쪽이 반드시
지나가게 하려는 것이고, 실패를 상태 코드로 옮기는 판단은 인바운드 어댑터의 몫이라 포트에는 HTTP 타입이 없다.

`InMemoryCourtAvailabilityAdapter`가 그 포트를 구현한다. 외부 HTTP를 부르지 않고 지원 코트와 시간대를 코드 안의
고정 표로 들고 있다. 빈자리 여부는 코트·날짜·시각에서 계산하므로 같은 입력에 같은 답이 나온다. 난수를 쓰면
판정이 run마다 흔들린다. 테스트는 `putCourt`·`removeCourt`·`setSlots`·`clearSlots`·`setNextCheckFailure`·
`setClock`·`reset`으로 표를 바꿀 수 있다. 확인 실패를 만들거나 시간을 고정하려고 프로덕션 코드를 고치게 하면
안 되므로 세터를 두었고, 세터는 테스트를 위한 것이라 유스케이스는 부르지 않는다.

확인 간격과 중복 조회 억제는 포트의 몫이 아니다. 포트는 부르면 그때 확인하고, 얼마나 자주 부를지와 같은
코트·날짜로 몰린 요청을 어떻게 한 번으로 묶을지는 유스케이스가 정한다.

## 생성기가 고칠 수 없는 파일

- `pom.xml`
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- `src/main/java/com/thinking/tennis/TennisAlertApplication.java`
- `src/main/java/com/thinking/tennis/port/CourtAvailabilityPort.java`
- `src/main/java/com/thinking/tennis/adapter/InMemoryCourtAvailabilityAdapter.java`
- `src/main/resources/application.yaml`
- `src/test/java/com/thinking/tennis/arch/LayerBoundaryTest.java`
- `tools/dump_api_docs.py`
- `README.md`

## 검증 명령

경계 규칙을 확인한다. 생성 코드가 없는 빈 상태에서도 통과해야 한다.

```bash
cd phase2/taskE/task5/skeleton
./mvnw -B test
```

스펙을 추출한다. 부팅해서 `/v3/api-docs`를 받아 파일로 저장하고 애플리케이션을 종료한다.

```bash
python tools/dump_api_docs.py --skeleton . --out target/api-docs.json
```

컨트롤러가 없으면 `paths`가 빈 객체인 JSON이 나온다. 그것이 빈 상태의 정상이다.

두 명령 모두 JDK 17 이상을 요구한다. `JAVA_HOME`이 그보다 낮은 JDK를 가리키고 있으면 추출기는 다른 JDK를
찾아 나서지 않고 그 사실을 적고 멈춘다. 쓸 JDK를 고르는 것은 사람의 판단이므로 `--java-home`으로 넘긴다.

```bash
python tools/dump_api_docs.py --skeleton . --out target/api-docs.json \
  --java-home "/c/Program Files/Amazon Corretto/jdk17.0.19_10"
```

## 추출기가 실패를 어떻게 알리는가

이 스크립트가 게이트의 스펙 추출기이므로 실패를 조용히 통과시키지 않는다. 어디서 막혔는지를 종료 코드로
구분하고, 애플리케이션 로그의 마지막 부분을 stderr에 함께 적는다. 그 출력을 그대로 생성기에 되먹일 수 있다.

| 코드 | 뜻 |
| --- | --- |
| 0 | api-docs 를 받아 저장했다 |
| 2 | 스켈레톤 경로나 JDK 버전이나 메이븐이 잘못됐다 |
| 3 | 컴파일·패키징이 실패했다 |
| 4 | 부팅이 실패했거나 제한 시간 안에 포트를 열지 못했다 |
| 5 | 부팅은 했지만 api-docs 를 받지 못했다 |

주요 옵션이다. `--port`는 기본이 0이라 임의 포트로 띄우고 로그에서 실제 포트를 읽는다. 게이트가 여러 번
돌거나 다른 프로세스가 8080을 쥐고 있어도 충돌하지 않게 한 것이다. `--skip-build`는 이미 만들어 둔 jar를 그대로
쓰고, `--keep-log`는 애플리케이션 로그를 파일로 남기고, `--build-timeout`·`--boot-timeout`·`--fetch-timeout`은
각 단계의 제한 시간을 초로 받는다.

받아 낸 JSON의 `servers[0].url`에는 그때 쓴 포트가 들어간다. 부팅 방식이 남긴 값이고 구현이 약속한 계약이
아니므로, 계약과 대조하는 쪽은 이 항목을 비교 대상에서 빼야 한다.

## 메이븐 래퍼의 출처

`mvnw`·`mvnw.cmd`·`.mvn/`은 `task3/ticket-reservation-c6`에서 가져오기로 했지만 그 디렉터리에는 래퍼가 없다.
같은 저장소에서 래퍼를 가진 `task1/task1-4-history-A`에서 복사했다. 래퍼 판본은 3.3.4이고 메이븐 3.9.6을
내려받아 쓴다.
