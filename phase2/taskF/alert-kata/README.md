# 알림 신청 동시성 Kata

같은 사용자가 같은 코트·날짜·시간대로 알림을 신청하면 감시 중인 신청은 하나만 남아야 한다. 이 프로젝트는 taskE의 해당 규칙을 작은 서비스로 옮겨, 순차 요청에서는 정상으로 보이지만 동시 요청에서는 중복이 생기는 F-2 실습이다.

Java의 테스트가 Spring 서비스를 호출하고, 서비스는 Testcontainers가 실행한 실제 MySQL에 저장한다. 웹 서버·인증·외부 예약 조회·알림 발송은 포함하지 않는다. 요청 재시도 키의 처리도 별도 API 계약이므로 이번 서비스 테스트의 대상이 아니다.

## 실행

필요한 도구는 JDK 25, Maven 3.9, Linux 컨테이너를 실행할 수 있는 Docker다. Spring Boot 3.5.16, Testcontainers 2.0.5, MySQL `mysql:8.4.8`, Redis `redis:7.4-alpine`을 사용한다. 처음 실행하면 의존성과 컨테이너 이미지를 내려받는다.

PowerShell에서 이 디렉터리로 이동한 뒤 실행한다. `JAVA_HOME`은 설치한 JDK 25 경로로 지정한다. 이번 실습의 폐기용 DB 비밀번호는 사용자가 지정한 `test`이며 `.env`는 필요하지 않다.

```powershell
$env:JAVA_HOME = Read-Host 'JDK 25 디렉터리 경로'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:KATA_DB_PASSWORD = 'test'
mvn test
mvn -Pkata-red test
```

`mvn test`는 기본 동작 5개와 F-3의 비관적·낙관적 락 3개, 단일 JVM Spin·Condition 비교 2개, Redis Pub/Sub 비교 1개를 실행한다. 모든 테스트를 실행하려면 Docker가 실행 중이어야 한다.

세 방식의 자원 사용량을 같은 조건으로 비교하려면 다음 테스트를 실행한다. 작업자 100개, 방식마다 50회씩 3묶음, 회차마다 100개 요청을 사용하며 Redis 연결은 미리 만들어 재사용한다.

```powershell
mvn -B -ntp '-Dtest=FairQueueResourceComparisonTest' test
python evidence/summarize-fair-queue.py
```

결과 표와 CPU 계산 범위는 [공정 비교 기록](evidence/fair-queue-resources.md), 원시 로그는 [fair-queue-resources.log](evidence/fair-queue-resources.log)에 있다. 표의 CPU 백분율은 작업 관리자 전체 사용률이 아니라 Java와 Redis 프로세스 CPU 시간을 논리 CPU 20개 용량으로 정규화한 값이다.

`mvn -Pkata-red test`는 위 테스트와 함께 다섯 조건을 각각 100회 실행한다. 조건은 순차 신규 신청, 동시 신규 신청, 시작 신호 없이 제출한 신규 신청, 기존 신청의 동시 재사용, 서로 다른 사용자의 동시 신청이다. 매회 두 요청의 결과를 모두 수집한 뒤 업무 조건별 DB 건수와 반환 ID를 확인하며, 100회를 마친 후 위반 회차가 0인지 단언한다. 시간 초과로 중단되면 완성된 100회 관찰로 취급하지 않는다.

2026-10-06 재시험에서는 순차 신규·기존 신청 재사용·다른 사용자 조건의 중복이 각각 0/100회였고, 같은 조건의 신규 신청 두 요청은 시작 신호 유무 모두 100/100회 중복됐다. 요청 오류는 없었으며 JUnit 결과는 10개 중 2개 실패다. [사전 검토와 재시험 결과](evidence/review-results.md), [실행 증거](evidence/README.md)에 상세 조건과 로그를 정리했다.

## 코드 읽는 순서

1. [AlertService](src/main/java/com/thinking/tennis/kata/AlertService.java)는 기존 신청을 조회하고, 있으면 같은 ID를 반환하며 없으면 생성한다.
2. [AlertRepository](src/main/java/com/thinking/tennis/kata/AlertRepository.java)는 실제 JPA 조회와 INSERT를 수행한다.
3. [PessimisticAlertService](src/main/java/com/thinking/tennis/kata/PessimisticAlertService.java)와 [OptimisticAlertService](src/main/java/com/thinking/tennis/kata/OptimisticAlertService.java)는 F-3의 두 락 방식을 구현한다.
4. [AlertSubscriptionTest](src/test/java/com/thinking/tennis/kata/AlertSubscriptionTest.java)와 [LockComparisonTest](src/test/java/com/thinking/tennis/kata/LockComparisonTest.java)는 업무 규칙과 최종 DB 건수를 검사한다.
5. [QueueWaitComparisonTest](src/test/java/com/thinking/tennis/kata/QueueWaitComparisonTest.java)와 [RedisPubSubQueueWaitComparisonTest](src/test/java/com/thinking/tennis/kata/RedisPubSubQueueWaitComparisonTest.java)는 99명의 대기 방식을 비교한다.
6. [FairQueueResourceComparisonTest](src/test/java/com/thinking/tennis/kata/FairQueueResourceComparisonTest.java)는 세 방식을 같은 요청 수와 재사용된 실행 자원으로 비교한다.

동시 시작 조건은 서비스 호출 직전에만 `CountDownLatch`로 출발 신호를 맞춘다. 시작 신호 없는 조건은 두 작업을 실행기에 연달아 제출하고, 순차 조건은 첫 호출의 반환을 기다린 뒤 다음 작업을 제출한다. 실제 Spring 서비스와 저장소를 그대로 사용하며 서비스 내부에는 대기나 관찰용 SQL을 넣지 않는다. 연결 풀은 최대 4개이며 호출 직전·직후 시간은 SQL 실행 구간과 구분해 기록한다.

조건별 발생 횟수는 같은 JVM·DB·연결 풀을 재사용한 로컬 반복 관찰값이다. 실제 HTTP 요청의 도착 간격이나 다른 트래픽을 재현한 것은 아니므로 운영 환경의 발생 확률을 뜻하지 않는다. 다른 실행에서는 중복 횟수가 달라질 수 있으며, 0회여도 해당 실행에서 발견하지 못했다는 뜻이다.

## F-3로 이어가기

[스키마](src/test/resources/schema.sql)는 사용자와 신청 두 테이블이다. 현재 서비스는 조회로 기존 신청을 확인하고 없으면 생성하며, 업무 조건을 묶은 UNIQUE 제약이나 락은 아직 적용하지 않았다. 순차 동작은 검증했지만 동시 요청까지 규칙을 지키는지는 이번 반복 실험으로 확인한다. 사용자 행은 테스트 시작 전에 커밋해 둔다.

F-3에서는 같은 사용자 행을 대상으로 비관적 락을 걸거나, 그 행의 `version`을 검사·증가시키는 방식으로 비교한다. 새 신청을 넣는 트랜잭션 안에서 함께 처리해야 하며, 새 신청 각각에 version만 붙여서는 서로 다른 두 INSERT의 충돌을 감지할 수 없다. 현재 `KataUser.version`은 F-3의 낙관적 충돌 감지에 사용하고, F-2의 기본 서비스에서는 증가시키거나 검사하지 않는다.

F-3에서도 같은 서비스 호출 테스트를 사용해 매회 신청이 1건이고 전체 중복 발생 회차가 0인지 확인할 수 있다. 최초의 조회 후 대기 실험은 사용자의 피드백에 따라 교체했으며, 해당 코드와 로그는 [이전 실험 기록](evidence/previous-controlled/README.md)에 보관한다.

99명의 대기 방식은 단일 JVM의 `Condition` 비교와 Redis `redis:7.4-alpine` Pub/Sub 비교를 나누어 실행한다. Redis Pub/Sub 테스트는 `mvn -Dtest=RedisPubSubQueueWaitComparisonTest test`로 단독 실행할 수 있다. Redis Pub/Sub는 연결된 구독자에게만 메시지를 전달하므로, 최종 승자 상태를 별도 저장해야 한다.

## 근거

- [taskE의 신청 규칙과 검증 시나리오](../../taskE/tennis-court-service-plan.md): FR-1과 V-1이 기대값의 근거다.
- [Testcontainers MySQL 모듈](https://java.testcontainers.org/modules/databases/mysql/): 실제 MySQL 컨테이너 구성과 JDBC 드라이버 의존성을 확인했다.
- [Spring Boot 3.5 시스템 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html): Java 25 지원 범위를 확인했다.
- [MySQL 8.4 Consistent Nonlocking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html): 일반 SELECT의 일관 읽기는 다른 트랜잭션의 쓰기를 막는 락을 설정하지 않는다는 설명이다.
- [Oracle CountDownLatch](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/CountDownLatch.html): 작업 간 대기와 진행 신호를 조율하는 근거다.
- [Redis Pub/Sub](https://redis.io/docs/latest/develop/use-cases/pub-sub/): 연결된 구독자에게 이벤트를 전달하고, 메시지 재생이 필요하면 Streams를 고려해야 하는 근거다.

계획과 완료 조건은 [작업 계획](../../../.omx/plans/taskf-2-kata.md), 제출용 설명은 [F-2 답안](../assignments/taskF-2.md)에 기록한다.
