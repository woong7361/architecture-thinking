# 락 순서 데드락 재현과 제거

## 환경

- 창구: `src/main/java/com/thinking/tennis/kata/OrderedLockFacade.java`
- 갱신 경로: `AvailabilityRefreshService`, 발송 경로: `AlertDispatchService`
- 테스트: `src/test/java/com/thinking/tennis/kata/CrossResourceDeadlockTest.java`
- 명령: `JAVA_HOME=~/jdk-25/jdk-25.0.4.1+1 mvn -Dtest=CrossResourceDeadlockTest test`
- JDK 25.0.4.1+1, Redis `redis:7.4-alpine`, Testcontainers 2.0.5
- 두 락 모두 기한 1초, 재시도 간격 20밀리초, 집합 전체 제한 시간 5초, 작업 구간 20밀리초씩
- 락 키: `lock:availability:court7:2026-11-2x` 와 `lock:alert:court7:2026-11-2x:19:00`

## 측정 방법

두 경로는 창구에 필요한 락과 할 일만 넘긴다. 바뀌는 변수는 `LockOrder` 하나다. `AS_REQUESTED`는 호출자가 넘긴 순서를 그대로 쓰고, `GLOBAL`은 전역 순서로 정렬한다. 하네스가 개입하는 자리는 `CyclicBarrier`로 두 호출의 출발 시점을 맞추는 것뿐이다.

갱신 경로는 빈자리락만 선언하고 조회 결과로 알게 된 신청락을 `escalate`로 추가한다. 발송 경로는 두 락을 집합으로 선언한다.

## 결과

```
FACADE_ROUND order=AS_REQUESTED round=1 refreshLongestWait=992.3ms dispatchLongestWait=1017.3ms stalled=true ownershipLost=true
FACADE_ROUND order=AS_REQUESTED round=2 refreshLongestWait=987.3ms dispatchLongestWait=1010.3ms stalled=true ownershipLost=true
FACADE_ROUND order=AS_REQUESTED round=3 refreshLongestWait=991.5ms dispatchLongestWait=1011.8ms stalled=true ownershipLost=true
FACADE_ROUND order=AS_REQUESTED round=4 refreshLongestWait=981.1ms dispatchLongestWait=1001.8ms stalled=true ownershipLost=true
FACADE_ROUND order=AS_REQUESTED round=5 refreshLongestWait=997.5ms dispatchLongestWait=1019.3ms stalled=true ownershipLost=true
FACADE_SUMMARY  order=AS_REQUESTED rounds=5 stalledRounds=5 ownershipLostRounds=5 orderViolationRounds=0 completedPaths=10/10

FACADE_ROUND order=GLOBAL round=1 refreshLongestWait=1.7ms  dispatchLongestWait=66.0ms stalled=false ownershipLost=false
FACADE_ROUND order=GLOBAL round=2 refreshLongestWait=1.0ms  dispatchLongestWait=65.8ms stalled=false ownershipLost=false
FACADE_ROUND order=GLOBAL round=3 refreshLongestWait=1.0ms  dispatchLongestWait=65.4ms stalled=false ownershipLost=false
FACADE_ROUND order=GLOBAL round=4 refreshLongestWait=0.9ms  dispatchLongestWait=66.0ms stalled=false ownershipLost=false
FACADE_ROUND order=GLOBAL round=5 refreshLongestWait=44.0ms dispatchLongestWait=0.9ms  stalled=false ownershipLost=false
FACADE_SUMMARY  order=GLOBAL rounds=5 stalledRounds=0 ownershipLostRounds=0 orderViolationRounds=0 completedPaths=10/10
```

## 읽는 법

순서를 그대로 둔 조건에서는 다섯 회차 모두 두 경로의 대기가 락 기한에 붙었고, 그 시점에 두 경로가 첫 락의 소유권을 잃었다. 대기가 풀린 이유는 상대의 해제가 아니라 기한 만료다.

전역 순서에서는 멈춤과 소유권 상실이 모두 0이다. 늦게 도착한 경로의 대기는 44에서 66밀리초이고, 이것은 먼저 도착한 경로가 임계 구역에 머문 시간이다. 기다리는 경로가 아무 락도 쥐지 않은 채 기다리므로 사이클이 닫히지 않는다.

순서 위반은 두 조건에서 모두 0이다. 갱신 경로가 작업 도중 추가하는 신청락이 이미 쥔 빈자리락보다 뒤 순위이기 때문이다. 순위를 반대로 두면 이 추가가 위반이 되어 전부 해제하고 재시작해야 한다.

이 측정이 말하지 않는 것이 둘 있다. 기한이 획득 제한 시간보다 짧아 데드락이 영구 멈춤이 아니라 기한만큼의 상호 대기로 나타난다는 점, 그리고 창구를 지나지 않는 획득과 두 프로세스가 서로 다른 순위표를 쓰는 경우는 이 테스트의 사정권 밖이라는 점이다.
