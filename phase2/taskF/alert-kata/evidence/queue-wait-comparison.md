# 99명 대기: Spin과 단일 JVM Pub/Sub 비교

> 이 문서는 초기 원리 관찰 기록이다. 회차마다 작업 스레드를 만들고 Spin 확인 횟수만 기록했으므로 CPU 백분율과 Redis 결과를 직접 비교하는 근거로 사용하지 않는다. 같은 입력·재사용 자원·CPU 정규화를 적용한 최종 비교는 [공정 비교 기록](fair-queue-resources.md)이다.

이번 과제는 분산 환경의 장애 처리보다 단일 인스턴스에서 대기 방식의 원리를 비교하는 단계다. 그래서 Redis Pub/Sub 대신 Java의 `ReentrantLock`과 `Condition`으로 브로드캐스트 신호를 구현했다. 이 선택은 브로커 네트워크·직렬화·연결 관리 비용을 제외하고, 반복 확인과 신호 대기의 차이를 관찰하기 위한 것이다. Redis Pub/Sub는 여러 인스턴스가 같은 신호를 받아야 할 때 사용할 수 있지만, 이번 실험에 넣으면 Pub/Sub 자체의 인프라 비용까지 함께 측정하게 된다.

각 회차에 100명이 동시에 참여하고, 한 명이 먼저 `compareAndSet`에 성공해 20ms 동안 승자 상태를 유지한 뒤 완료 신호를 보낸다. 나머지 99명은 최종 승자 ID를 확인하고 패배자로 종료한다. Spin은 완료 상태를 `Thread.onSpinWait()`와 함께 반복해서 읽고, Pub/Sub는 모든 참여자가 구독을 등록한 뒤 `Condition.await()`로 대기하며 승자가 `signalAll()`을 호출한다. 대기자는 신호를 받더라도 상태를 다시 확인하는 조건문 안에서 기다리므로 신호 유실을 성공으로 처리하지 않는다.

두 방식을 각각 20회 실행하고, 같은 실험을 세 번 반복했다. 회차 시간은 100명이 시작한 뒤 최종 결과를 받은 시간이며, `spinChecks`는 Spin 참여자들이 완료 상태를 확인한 횟수, `pubSubWaits`는 Pub/Sub 참여자들이 조건 대기에 들어간 횟수다.

| 실행 | Spin 회차 중앙값 | Spin 상태 확인 | Pub/Sub 회차 중앙값 | Pub/Sub 대기 횟수 |
|---:|---:|---:|---:|---:|
| 1 | 36.909ms | 1,480,391회 | 25.499ms | 99회 |
| 2 | 37.775ms | 1,522,513회 | 25.901ms | 99회 |
| 3 | 39.260ms | 1,538,875회 | 25.700ms | 99회 |

세 실행의 중앙값으로 보면 Spin은 37.775ms, Pub/Sub는 25.700ms였다. Spin은 20ms 동안 약 152만 번 상태를 읽었지만, Pub/Sub는 99개 스레드가 한 번씩 대기했다. 따라서 이번 조건에서는 Pub/Sub가 더 짧게 끝났고, 대기 중 반복 연산도 훨씬 적었다. 상태 확인 횟수는 JVM 안에서 실제로 수행된 작업량을 나타내는 지표이며, 운영 환경의 CPU 사용률을 그대로 뜻하지는 않는다.

| 비교 기준 | Spin | 단일 JVM Pub/Sub(`Condition`) |
|---|---|---|
| 대기 방식 | 상태가 바뀔 때까지 계속 확인한다. | 신호를 받을 때까지 스레드를 재운다. |
| 이번 실험의 자원 사용 | 회차당 약 152만 번 상태를 확인했다. | 99개 대기 스레드가 신호를 기다렸다. |
| 짧은 대기 | 깨우기 비용이 없어 짧은 대기에서는 유리할 수 있다. | 잠들고 깨우는 비용이 상대적으로 크게 느껴질 수 있다. |
| 긴 대기 | 대기 시간 동안 CPU를 계속 사용한다. | 대기 스레드가 실행되지 않아 CPU를 덜 사용한다. |
| 코드 복잡도 | 상태 변수와 재시도 조건만 있으면 단순하다. | 구독 등록, 조건문을 포함한 대기, 신호 전달이 필요하다. |
| 신호 유실 | 상태를 계속 읽으므로 신호 자체를 놓치지 않는다. | 최종 상태를 함께 저장하고 조건문 안에서 확인해야 한다. |
| 인프라 | 추가 인프라가 필요 없다. | 이번 구현은 추가 인프라가 없지만 단일 JVM에서만 동작한다. |
| 여러 인스턴스 | 각 JVM의 상태가 달라져 사용할 수 없다. | Redis Pub/Sub 같은 공유 브로커가 필요하다. |

이번 범위에서는 단일 JVM의 대기 자원 차이를 확인하는 목적이므로 `Condition` 기반 Pub/Sub를 선택한다. 실제 여러 인스턴스가 같은 이벤트를 받아야 하는 기능이라면 Redis Pub/Sub를 검토할 수 있지만, 구독자가 끊겼을 때 메시지가 사라질 수 있으므로 승자 상태를 DB나 Redis에 별도로 저장하고 재조회하는 절차가 필요하다. [Java Condition 공식 문서](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/locks/Condition.html), [Java Thread.onSpinWait 공식 문서](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html#onSpinWait())

원본 로그는 [1차 실행](queue-wait-first.log), [2차 실행](queue-wait-final-run1.log), [3차 실행](queue-wait-final-run2.log), [4차 실행](queue-wait-final-run3.log)에서 확인할 수 있다. 테스트 코드는 [QueueWaitComparisonTest](../src/test/java/com/thinking/tennis/kata/QueueWaitComparisonTest.java)다.
