# NEW_UNCOORDINATED 격리 수준 관찰

이번 실행은 `NEW_UNCOORDINATED` 조건만 대상으로 했다. 두 작업을 `ExecutorService`에 연속 제출하고, 작업 내부에서는 `CountDownLatch`를 기다리지 않은 상태로 서비스 메서드를 바로 호출했다. 따라서 `NEW_TOGETHER`처럼 두 작업을 준비시킨 뒤 출발 신호를 보내지 않았다. 각 격리 수준에서 100회씩 반복했고, 한 회차에 같은 조건의 요청 두 개를 보냈다. 실행에는 기본 단일 요청 테스트 5개도 포함됐다.

실행 명령은 다음과 같다.

```text
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_READ_UNCOMMITTED test
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED test
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ test
mvn -B -ntp -Pkata-red -Dspring.datasource.hikari.transaction-isolation=TRANSACTION_SERIALIZABLE test
```

| 격리 수준 | 중복 발생 회차 | 요청 오류 회차 | 호출 구간이 겹친 회차 |
|---|---:|---:|---:|
| READ UNCOMMITTED | 100/100 | 0/100 | 100/100 |
| READ COMMITTED | 100/100 | 0/100 | 100/100 |
| REPEATABLE READ | 100/100 | 0/100 | 100/100 |
| SERIALIZABLE | 0/100 | 100/100 | 100/100 |

세 가지 격리 수준에서는 두 요청이 각각 빈 조회 결과를 받은 뒤 INSERT해 중복 행이 남았다. SERIALIZABLE에서는 중복 행이 남지 않았지만, 두 요청의 조회가 충돌해 INSERT 단계에서 `Deadlock found when trying to get lock` 오류가 발생했다. 따라서 중복만 세면 SERIALIZABLE이 해결처럼 보이지만, 요청 계약까지 포함하면 실패한 상태다. 이 실행은 운영 환경에서의 발생 확률을 측정한 것이 아니라, 래치 없이 연속 제출한 동일 조건에서 경합이 반복되는지를 확인한 결과다.

원본 로그: [READ UNCOMMITTED](uncoordinated-read_uncommitted.log), [READ COMMITTED](uncoordinated-read_committed.log), [REPEATABLE READ](uncoordinated-repeatable_read.log), [SERIALIZABLE](uncoordinated-serializable.log)
