# 실행 순서별 격리 수준 관찰

F-2의 재현 테스트는 두 요청을 거의 같은 시각에 보내므로, 양쪽이 모두 조회를 마친 뒤에 저장이 일어나는 순서만 만든다. 실제로는 뒤 요청의 조회가 앞 요청의 저장보다 늦게 도착할 수도 있다. 그 순서에서도 격리 수준이 결과를 바꾸지 않는지 확인하려고 순서를 직접 고정해 돌렸다.

## 방법

DB 세션 두 개(1번, 2번)를 열고 두 요청의 실행 순서를 코드로 고정했다. 두 세션 모두 서비스와 같은 판단을 한다. 같은 조건으로 조회하고, 결과가 비어 있을 때만 저장한다. 조회 조건은 `user_id = 1 AND court_id = 10 AND play_date = '2026-10-10' AND time_slot = '09:00-10:00' AND status = 'WATCHING'`이다.

| 순서 | 실행 단계 |
|---|---|
| A (`BOTH_READ_FIRST`) | 조회(1) → 조회(2) → 저장(1) → 저장(2) → 커밋(1) → 커밋(2) |
| B (`READ_BEFORE_COMMIT`) | 조회(1) → 저장(1) → 조회(2) → 저장(2) → 커밋(1) → 커밋(2) |
| C (`READ_AFTER_COMMIT`) | 조회(1) → 저장(1) → 커밋(1) → 조회(2) → 저장(2) → 커밋(2) |

A는 재현 테스트가 만드는 순서다. B는 2번 조회가 1번의 저장 뒤 커밋 전에, C는 1번의 커밋 뒤에 일어난다.

네 격리 수준에서 각 순서를 20회씩 반복했다. 매 회차 시작에 테이블을 비웠고, 두 세션 모두 `innodb_lock_wait_timeout = 1`로 두어 잠금에 막히면 1초 뒤 오류로 돌아오게 했다. 회차마다 2번이 조회에서 본 건수, 두 저장의 성공 여부, 마지막에 남은 행 수를 기록했다.

## 실행 환경

재현 테스트와 달리 Spring을 거치지 않는 단일 파일 Java 프로그램이다. JDBC만 쓰므로 JDK 17로 실행했다. DB는 재현 테스트와 같은 `mysql:8.4.8` 이미지이되 Testcontainers 대신 직접 띄웠다.

```text
docker run -d --name kata-interleaving -e MYSQL_ROOT_PASSWORD=<password> -e MYSQL_DATABASE=alert_kata -p 13399:3306 mysql:8.4.8
docker exec -i kata-interleaving mysql -uroot -p<password> alert_kata < ../src/test/resources/schema.sql
docker exec -i kata-interleaving mysql -uroot -p<password> alert_kata -e "INSERT INTO kata_users (id, version) VALUES (1, 0), (2, 0)"
KATA_DB_PASSWORD=<password> java -cp <mysql-connector-j.jar> InterleavingProbe.java
```

소스는 [InterleavingProbe.java](InterleavingProbe.java), 전체 로그는 [interleaving-probe.log](interleaving-probe.log)에 있다.

## 결과

열두 칸 모두 20회차가 같은 결과였다.

| 순서 | READ UNCOMMITTED | READ COMMITTED | REPEATABLE READ | SERIALIZABLE |
|---|---|---|---|---|
| A 조회·조회 뒤 저장 | 중복 2건 | 중복 2건 | 중복 2건 | 저장이 막힘 (0건) |
| B 커밋 전에 조회 | **중복 없음** | 중복 2건 | 중복 2건 | 조회가 막힘 (1건) |
| C 커밋 뒤에 조회 | 중복 없음 | 중복 없음 | 중복 없음 | 중복 없음 |

"중복 없음"인 칸에서는 2번 조회가 1번의 신청을 보고 저장을 건너뛰었다. 남은 행은 1건이다.

SERIALIZABLE의 두 칸은 모두 `Lock wait timeout exceeded; try restarting transaction`이다. 순서 A에서는 두 저장이 차례로, 순서 B에서는 2번 조회가 막혔다.

## 읽는 법

**순서 A에서는 격리 수준이 결과를 바꾸지 못한다.** 두 조회가 모두 상대의 저장보다 먼저 끝나므로 양쪽 다 빈 결과를 받고, 네 수준 중 셋에서 중복이 남는다. 조회 시점에 행이 아예 없으니 무엇을 보여 줄지 정하는 설정으로는 손댈 것이 없다. 1절 표에서 READ UNCOMMITTED도 100회차 모두 중복이었던 것이 이 칸과 같다.

**순서 B에서는 READ UNCOMMITTED만 다르다.** 2번 조회가 1번이 아직 커밋하지 않은 신청을 보고 "있다"고 판단해 저장을 건너뛰었다. 커밋되지 않은 값을 읽는 것이 더티 리드이고, 이 순서에서는 그것이 중복을 막는다. 다만 1번이 롤백하면 2번은 사라진 신청의 식별자를 돌려준 셈이 되므로, 중복을 막는 수단으로 쓸 수는 없다.

**순서 C는 네 수준 모두 중복이 없다.** 2번 조회가 이미 커밋된 신청을 본다. REPEATABLE READ도 마찬가지인데, InnoDB는 트랜잭션을 열 때가 아니라 그 트랜잭션의 첫 조회에서 스냅숏을 만들기 때문이다. 서비스가 트랜잭션 안에서 보내는 첫 SQL이 그 조회이므로 스냅숏이 1번의 커밋 뒤에 잡힌다.

**SERIALIZABLE에서 막힌 오류는 재현 테스트의 교착과 다르다.** 이 프로브는 두 저장을 한 줄로 차례대로 실행하므로 둘이 같은 순간에 기다리지 않아 대기 순환이 생기지 않고, 잠금 대기 1초를 넘겨 오류로 돌아온다. 재현 테스트처럼 두 요청을 같은 시각에 보내면 두 저장이 함께 기다려 순환이 생기고, DB가 교착으로 판정해 한쪽을 롤백한다.
