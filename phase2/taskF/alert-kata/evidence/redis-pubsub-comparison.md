# Redis Pub/Sub 대기 비교 결과

> 이 문서는 구독 연결을 회차마다 새로 만든 초기 인프라 비용 관찰 기록이다. 같은 요청 수와 재사용 연결로 다시 측정한 결과와 직접 순위를 비교하지 않는다. 최종 비교는 [공정 비교 기록](fair-queue-resources.md)에서 확인한다.

단일 JVM `Condition` 결과에 Redis의 네트워크 비용을 추가해 비교했다. Redis는 `redis:7.4-alpine` 컨테이너로 실행하고 Lettuce를 사용했다. 각 참여자는 Redis Pub/Sub 연결을 하나씩 열어 같은 채널을 구독했으며, 승자가 `PUBLISH`한 결과를 99명의 패배자와 승자 자신이 받도록 했다. 이 구성은 독립적인 100명의 구독자를 모델링하므로 구독 연결 비용이 포함된다. 실제 서비스에서 연결을 공유하거나 multiplexing하면 수치는 달라질 수 있다.

각 실행은 100명·20회이며, 세 번 반복했다. 1명은 20ms 동안 승자 상태를 유지한 뒤 메시지를 발행하고, 99명은 메시지를 기다렸다. Redis 입력·출력 바이트는 Redis `INFO stats`의 실행 전후 차이로 기록했다.

| 실행 | 회차 시간 중앙값 | 전달 메시지 | 구독 연결 | Redis 입력 | Redis 출력 |
|---:|---:|---:|---:|---:|---:|
| 1 | 52.739ms | 2,000 | 2,000 | 443,611B | 628,576B |
| 2 | 53.572ms | 2,000 | 2,000 | 443,611B | 628,577B |
| 3 | 52.771ms | 2,000 | 2,000 | 443,613B | 628,776B |

세 실행의 중앙값은 52.771ms, Redis 입력 443,611B, 출력 628,577B였다. 같은 조건에서 단일 JVM `Condition`의 중앙값은 25.700ms였으므로, 이번 독립 연결 구성의 Redis Pub/Sub는 로컬 신호보다 약 2배 느렸다. Spin의 중앙값 37.775ms보다도 느렸지만, Spin은 회차당 약 152만 번 상태를 읽었고 Redis는 네트워크를 통해 2,000개 메시지를 전달했다. Redis는 같은 Docker 호스트의 컨테이너에서 실행했으므로, 이 결과는 원격 네트워크 지연이 아니라 Redis 처리·소켓·Docker 네트워크·직렬화 비용을 포함한 값이다. 따라서 속도만으로 비교하지 않고 반복 연산과 네트워크 비용을 함께 봐야 한다.

Redis Pub/Sub는 `PUBLISH` 시점에 연결된 구독자에게만 메시지를 전달하는 일회성 전송이다. 구독자가 끊겼을 때 메시지를 재생할 수 없으므로, 승자 결과 자체를 Redis 키나 DB에 먼저 저장하고 메시지는 깨우기 신호로 사용해야 한다. 재생과 재처리가 필요하면 Redis Streams를 검토해야 한다. [Redis Pub/Sub 공식 문서](https://redis.io/docs/latest/develop/use-cases/pub-sub/), [Redis PUBLISH 공식 문서](https://redis.io/docs/latest/commands/PUBLISH/)

원본 로그는 [1차 실행](redis-pubsub-run1.log), [2차 실행](redis-pubsub-run2.log), [3차 실행](redis-pubsub-run3.log)에서 확인할 수 있다. 테스트 코드는 [RedisPubSubQueueWaitComparisonTest](../src/test/java/com/thinking/tennis/kata/RedisPubSubQueueWaitComparisonTest.java)다.
