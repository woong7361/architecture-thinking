# DB 격리 수준 관찰 결과

2026-10-06에 Testcontainers로 MySQL 8.4.8/InnoDB를 실행하고, 같은 동작 테스트를 네 격리 수준에서 각각 수행했다. 각 수준에서 다섯 조건을 100회씩 실행했다. 신규 신청을 비운 뒤 두 요청을 겹쳐 보낸 조건과, 기존 신청을 미리 만든 조건을 구분했다. 한 수준에서 실험 요청은 1,000개이며, 실행 명령은 다음과 같다.

```text
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_READ_UNCOMMITTED test
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED test
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ test
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_SERIALIZABLE test
```

## 결과

| 격리 수준 | 신규·동시 요청 중복 | 신규·동시 요청 오류 | 기존 신청 재사용 | 서로 다른 사용자 |
|---|---:|---:|---:|---:|
| READ UNCOMMITTED | 100/100회 | 0/100회 | 정상 100/100회 | 정상 100/100회 |
| READ COMMITTED | 100/100회 | 0/100회 | 정상 100/100회 | 정상 100/100회 |
| REPEATABLE READ | 100/100회 | 0/100회 | 정상 100/100회 | 정상 100/100회 |
| SERIALIZABLE | 0/100회 | 100/100회 | 정상 100/100회 | 오류 34/100회 |

여기서 오류는 회차 단위다. SERIALIZABLE의 신규·동시 요청에서는 두 트랜잭션이 모두 빈 결과를 읽은 뒤 INSERT 잠금을 얻으려 하면서 MySQL이 `Deadlock found when trying to get lock`을 반환했다. 따라서 이 코드에서는 중복이 없어졌지만 두 요청이 모두 성공한 것은 아니다. 서로 다른 사용자 조건에서도 같은 연결 흐름을 사용했기 때문에 일부 회차에서 잠금 충돌이 발생했다.

현재 기능에서 직접 확인한 현상은 더티 리드나 비반복 읽기가 아니라, **조회 결과가 비어 있는 동안 다른 트랜잭션도 같은 조건을 검사하고 각각 INSERT하는 동시 삽입 경합**이다. 같은 트랜잭션에서 두 번 조회해 새 행이 보이는지를 확인한 실험은 아니므로, 이 결과만으로 팬텀 리드를 관찰했다고 표현하지 않는다. 팬텀 리드 자체를 확인하려면 한 트랜잭션의 첫 조회와 두 번째 조회 사이에 다른 트랜잭션이 행을 커밋하는 별도 실험이 필요하다.

MySQL InnoDB의 기본 격리 수준은 REPEATABLE READ다. 이 수준의 일반 SELECT는 트랜잭션이 시작한 시점의 일관된 읽기를 사용하므로, 조회 결과를 안정적으로 유지하지만 조회와 INSERT를 하나의 원자적 업무로 만들어 주지는 않는다. 그래서 이번 코드에서는 기본값을 사용해도 중복이 계속 발생했다. SERIALIZABLE은 일반 SELECT까지 더 강하게 잠그지만, 충돌한 요청을 자동으로 재시도하지 않으므로 교착 상태나 잠금 오류를 호출자에게 남길 수 있다. [MySQL 격리 수준 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)

MySQL 문서가 REPEATABLE READ를 기본값으로 둔 정책 이유를 직접 설명하지는 않는다. 다만 이 수준은 SERIALIZABLE보다 잠금으로 인한 대기를 줄이면서도 일관된 읽기를 제공하므로, 높은 직렬화 비용과 읽기 일관성 사이의 절충안으로 이해할 수 있다. 이는 문서의 격리 수준 동작을 바탕으로 한 해석이다. 기본 테스트 5개는 격리 수준 실험과 별도로 `mvn -B -ntp test`에서 모두 통과했다.

원본 실행 로그는 [READ UNCOMMITTED](isolation-read_uncommitted.log), [READ COMMITTED](isolation-read_committed.log), [REPEATABLE READ](isolation-repeatable_read.log), [SERIALIZABLE](isolation-serializable.log)에서 확인할 수 있다.
