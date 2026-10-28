# 더티 리드·팬텀 리드 직접 관찰

F-2에서 관찰한 중복이 더티 리드인지 팬텀 리드인지 확인하려고, 두 현상의 정의를 그대로 재현하는 프로브를 따로 돌렸다. 중복 재현 테스트는 요청 두 개를 겹쳐 보내기만 하므로 두 현상의 정의가 요구하는 조회 순서를 만들지 못한다.

## 방법

두 개의 DB 세션(reader, writer)을 열고 조회 순서를 코드로 고정했다. 조회 조건은 서비스가 쓰는 것과 같다. `user_id = 1 AND court_id = 10 AND play_date = '2026-10-10' AND time_slot = '09:00-10:00' AND status = 'WATCHING'`

**더티 리드 프로브** — 커밋되지 않은 변경이 보이는가

1. writer가 트랜잭션을 열고 조건에 맞는 행을 INSERT한다. 커밋하지 않는다.
2. reader가 조건으로 조회한다.
3. 1이 보이면 `VISIBLE`(더티 리드 발생), 0이면 `NOT_VISIBLE`, 잠금에 막히면 `READ_BLOCKED`.

**팬텀 리드 프로브** — 같은 트랜잭션의 두 조회 사이에 커밋된 행이 보이는가

1. reader가 트랜잭션을 열고 조건으로 조회한다(`firstRead`).
2. writer가 조건에 맞는 행을 INSERT하고 커밋한다. 막히면 `WRITE_BLOCKED`.
3. reader가 같은 트랜잭션에서 같은 조건으로 다시 조회한다(`secondRead`).
4. `secondRead`가 0이면 reader도 "없으니 만든다"를 수행해 INSERT하고 커밋한다. 서비스의 판단을 그대로 흉내 낸 단계다.
5. `secondRead > firstRead`면 `VISIBLE`(팬텀 발생). 마지막에 남은 전체 행 수를 `rowsAfterReaderInsert`로 기록한다.

네 격리 수준에서 각 프로브를 20회씩 반복했다. 두 세션 모두 `innodb_lock_wait_timeout = 1`로 두어, 잠금에 막히면 1초 뒤 오류로 돌아오게 했다.

## 실행 환경

이 프로브는 Spring 테스트가 아니라 단일 파일 Java 프로그램이다. 프로젝트 테스트는 Java 25를 요구하지만, 프로브는 JDBC만 쓰므로 JDK 17로 실행했다. DB는 재현 테스트와 같은 `mysql:8.4.8` 이미지이되 Testcontainers 대신 직접 띄웠다.

```text
docker run -d --name kata-anomaly -e MYSQL_ROOT_PASSWORD=<password> -e MYSQL_DATABASE=alert_kata -p 13399:3306 mysql:8.4.8
docker exec -i kata-anomaly mysql -uroot -p<password> alert_kata < ../src/test/resources/schema.sql
docker exec -i kata-anomaly mysql -uroot -p<password> alert_kata -e "INSERT INTO kata_users (id, version) VALUES (1, 0), (2, 0)"
KATA_DB_PASSWORD=<password> java -cp <mysql-connector-j.jar> IsolationAnomalyProbe.java
```

소스는 [IsolationAnomalyProbe.java](IsolationAnomalyProbe.java), 전체 로그는 [isolation-anomaly-probe.log](isolation-anomaly-probe.log)에 있다.

## 결과

네 수준 모두 20회가 같은 결과였다.

| 격리 수준 | 더티 리드 | 팬텀 리드 | 두 번째 조회 뒤 남은 행 |
|---|---|---|---:|
| READ UNCOMMITTED | 보인다 (20/20) | 보인다 (20/20) | 1 |
| READ COMMITTED | 보이지 않는다 (20/20) | 보인다 (20/20) | 1 |
| REPEATABLE READ | 보이지 않는다 (20/20) | 보이지 않는다 (20/20) | **2** |
| SERIALIZABLE | 조회가 막힌다 (20/20) | 쓰기가 막힌다 (20/20) | 1 |

SERIALIZABLE의 두 칸은 모두 `Lock wait timeout exceeded; try restarting transaction`이다. 더티 리드 프로브에서는 reader의 조회가, 팬텀 프로브에서는 writer의 INSERT가 막혔다.

## 읽는 법

**더티 리드는 READ UNCOMMITTED에서만 일어난다.** 서비스가 쓰는 REPEATABLE READ에서는 커밋되지 않은 INSERT가 20회 모두 보이지 않았다. 그러므로 F-2의 중복은 더티 리드가 아니다.

**팬텀 리드는 REPEATABLE READ에서 일어나지 않는다.** 첫 조회가 만든 스냅숏이 유지되어, 그 사이에 다른 트랜잭션이 커밋한 행도 두 번째 조회에 나타나지 않았다.

**그 보이지 않음이 중복을 만든다.** REPEATABLE READ의 마지막 열만 2다. 두 번째 조회가 여전히 0을 돌려주니 reader는 "없으니 만든다"로 판단해 INSERT했고, 이미 커밋된 writer의 행과 합쳐 두 건이 됐다. READ COMMITTED에서는 두 번째 조회가 1을 돌려줘 reader가 만들지 않았다.

다만 이 비교를 "READ COMMITTED면 중복이 안 난다"로 읽으면 안 된다. 프로브의 reader는 조회를 두 번 하지만 서비스는 한 번만 한다. 조회가 한 번이면 그 결과가 낡는 구간은 READ COMMITTED에도 그대로 있고, 실제로 [래치 없는 격리 수준 관찰](uncoordinated-isolation-results.md)에서 READ COMMITTED도 100/100 중복이었다.

**writer의 INSERT가 reader의 조회 범위에 들어가는 것 자체는 막히지 않는다.** SERIALIZABLE만 그 쓰기를 막았다. REPEATABLE READ는 그 행을 reader에게 보여 주지 않을 뿐, 쓰기가 일어나는 것은 막지 않는다.
