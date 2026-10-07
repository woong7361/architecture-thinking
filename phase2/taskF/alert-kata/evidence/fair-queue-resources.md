# 99명의 대기 방식 공정 비교 기록

이 기록은 Spin 대기, JVM `Condition` 대기, Redis Pub/Sub를 같은 입력과 같은 실행 환경에서 비교한 결과다. 이전 기록은 회차마다 작업 스레드와 Redis 구독 연결을 새로 만들고, 방식마다 측정 단위도 달라 CPU 비율을 직접 비교하기 어려웠다. 이번 측정에서는 그 차이를 제거했다.

## 조건

- Java 25.0.4.1, Windows 10, 논리 CPU 20개, Docker 29.6.2
- Redis 7.4.11, `redis:7.4-alpine` 고정 digest
- 방식마다 작업자 100개, 측정 50회씩 3묶음, 워밍업 10회
- 회차마다 100개 요청, 같은 25바이트 결과 형식, 승자 1명과 패배자 99명
- 100개 작업자는 공통 준비 래치에서 동시에 출발하고 CAS로 승자 1명을 정한다.
- 승자 처리 뒤 모든 방식에 같은 20ms 모의 지연을 둔다. 실제 업무 처리 시간을 뜻하지 않고 대기 자원의 차이를 드러내기 위한 고정 조건이다.
- Spin과 `Condition`은 같은 100개 작업자 풀을 재사용한다. Redis도 구독 연결 100개와 발행 연결 1개를 측정 전에 만들어 재사용한다.
- 연결과 작업자 준비 시간은 `setupMs=696.362`로 별도 기록하고 처리 시간에는 넣지 않았다.

세 방식 모두 150회, 15,000개 요청을 처리했다. Redis 연결을 재사용한 것은 Redis 요청의 steady-state 비용을 보기 위한 선택이며, 로컬 대기 방식에 TCP 연결을 억지로 추가한 것이 아니다. 따라서 작업자 수와 요청 수는 같지만 로컬 방식과 Redis 방식의 네트워크 연결 수까지 같다는 뜻은 아니다.

## 결과

| 방식 | 회차 시간 중앙값 | 회차 p95 중앙값 | 정규화 CPU 중앙값 | CPU 범위 | 정확성 |
|---|---:|---:|---:|---:|---:|
| Spin | 29.965ms | 124.089ms | 92.356% | 91.966~92.429% | 150/150 |
| `Condition` | 22.258ms | 24.069ms | 0.505% | 0.295~0.921% | 150/150 |
| Redis Pub/Sub | 24.193ms | 26.467ms | 2.246% | 1.980~4.001% | 150/150 |

CPU는 50회 묶음 동안의 Java 프로세스 CPU 시간과 Redis `used_cpu_user + used_cpu_sys` 증가량을 합산해 측정 경과 시간으로 나눈 뒤, 논리 CPU 20개로 다시 나눴다.

```text
javaOneCorePct  = Java process CPU ms / elapsed ms × 100
redisOneCorePct = Redis CPU ms / elapsed ms × 100
normalizedPct   = (javaOneCorePct + redisOneCorePct) / 20
```

이 값은 두 프로세스가 20개 논리 CPU 용량 중 사용한 비율이다. Windows 작업 관리자 전체 CPU 사용률이 아니며, Docker 가상화 계층과 다른 프로세스의 CPU를 포함하지 않는다. Java CPU 시간은 JVM 프로세스의 누적 CPU 시간으로 측정했고, Redis CPU와 네트워크 카운터는 Redis `INFO`에서 읽었다. `INFO` 자체의 조회 트래픽도 네트워크 카운터에 포함되므로 네트워크 바이트는 애플리케이션 메시지만의 패킷 캡처 값이 아니다.

Spin의 높은 CPU는 20ms 동안 공유 상태를 계속 읽고 `Thread.onSpinWait()`를 호출한 결과다. `Condition`은 대기 스레드를 재우므로 CPU 사용이 낮았고, Redis는 네트워크 직렬화와 브로커 처리 때문에 `Condition`보다 CPU와 시간이 늘었다. Redis가 Spin보다 CPU를 적게 썼다는 사실만으로 전체 시스템 비용이 항상 더 작다고 결론 내릴 수는 없다. 브로커 프로세스와 연결 운영 비용, 네트워크 장애 처리가 추가되기 때문이다.

## 검증 범위와 한계

이 테스트는 모든 참가자가 정상적으로 연결된 단일 호스트에서 같은 종료 신호를 기다리는 비용을 비교한다. CAS 승자 선택과 결과 일치 여부를 매회 단언하지만, FIFO 도착 순서, 분산 락, Redis 장애 중 재연결, 메시지 재생과 복구는 검증하지 않는다. Redis Pub/Sub는 연결된 구독자에게만 전달되고 메시지를 재생하지 않으므로 운영 기능에서는 승자 결과를 Redis 키나 DB에 보존하고 Pub/Sub를 깨우기 신호로 사용하는 설계가 필요하다. [Redis Pub/Sub 공식 문서](https://redis.io/docs/latest/develop/pubsub/)

CPU 시간의 의미는 [Java OperatingSystemMXBean 공식 문서](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html), Redis CPU와 네트워크 카운터의 의미는 [Redis INFO 공식 문서](https://redis.io/docs/latest/commands/info/)를 기준으로 삼았다.

## 재현과 재집계

```powershell
mvn -B -ntp '-Dtest=FairQueueResourceComparisonTest' test
python evidence/summarize-fair-queue.py
```

테스트 코드는 [FairQueueResourceComparisonTest.java](../src/test/java/com/thinking/tennis/kata/FairQueueResourceComparisonTest.java), 원시 실행 로그는 [fair-queue-resources.log](fair-queue-resources.log), 재집계 결과는 [fair-queue-resources-summary.json](fair-queue-resources-summary.json)이다. 재집계 스크립트는 9개 묶음, 각 묶음의 50회·5,000개 요청·승자 50명·패배자 4,950명을 확인하고, 표의 CPU 계산식과 일치하는지도 검증한다.
