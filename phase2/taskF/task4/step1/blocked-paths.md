# 가린 것과 남긴 것

작업 공간은 저장소 밖에 복사했고, 아래를 제거한 뒤 독립 git 저장소의 기준선 커밋으로 고정했다.
가렸다고 선언하고 실제로 남아 있으면 기록이 그만큼 거짓이 되므로, 남긴 것 중 힌트가 될 수 있는 것도 적는다.

## 제거한 것

| 경로 | 왜 |
| --- | --- |
| `src/main/.../PessimisticAlertService.java` | F-3의 락 구현. 조건을 주지 않아도 이것이 보이면 복사가 가장 싼 경로다 |
| `src/main/.../OptimisticAlertService.java` | 같음 |
| `src/main/.../OptimisticConflictException.java` | 위 구현이 쓰는 타입 |
| `src/main/.../OptimisticSubscriptionAttempt.java` | 위 구현의 재시도 트랜잭션 |
| `src/test/.../LockComparisonTest.java` | 두 락 구현을 호출하므로 답이 테스트 쪽에 드러난다 |
| `src/test/.../FairQueueResourceComparisonTest.java` | F-3의 대기 방식 비교. 단계 1의 판정 대상이 아니고, Redis 컨테이너를 요구해 run을 느리게 한다. CAS와 `Condition` 코드가 간접 힌트이기도 하다 |
| `src/test/.../QueueWaitComparisonTest.java` | 같음 |
| `src/test/.../RedisPubSubQueueWaitComparisonTest.java` | 같음 |

## 복사하지 않은 것

| 대상 | 왜 |
| --- | --- |
| `alert-kata/evidence/` 전부 | F-2·F-3의 관찰 로그와 측정 결과 |
| `alert-kata/README.md` 원본 | `F-3로 이어가기` 절에 "비관적 락을 걸거나 그 행의 version을 검사·증가시키는 방식"이라고 답이 적혀 있다. 실행 방법만 다시 쓴 README로 교체했다 |
| 저장소의 나머지 전부 | 과제 문서와 답안, 하네스 설계, 회고. taskF-3 답안에는 두 락의 코드 설명과 측정 결과가 있고 taskF-4 문서에는 이 run의 목적이 적혀 있다 |

## 남긴 것 중 힌트가 될 수 있는 것

`src/test/resources/schema.sql`의 `kata_users.version` 컬럼과 `KataUser.version` 필드를 남겼다. 낙관적 락의
흔적이다. 스키마와 엔티티는 현재 상태의 일부이고 설계 §4가 주는 것으로 분류했으므로 지우지 않았다. 컬럼
하나가 "사용자 행의 version을 조건부로 증가시켜라"까지 알려주지는 않지만, 눈에 보인다는 것은 사실이다.

`AlertSubscriptionTest`는 조건 이름으로 무엇이 깨지는지 알려준다. 이것은 판정 기준이라 가릴 수 없다.

## 놓친 것 — run 1과 2 뒤에 발견

**`AlertRepository`가 F-3의 보조 메서드 셋을 그대로 들고 있었다.** 파일 단위로만 가려서 메서드 단위의
흔적을 못 봤다.

| 남아 있던 것 | 무엇을 알려 주는가 |
| --- | --- |
| `lockUserForUpdate(long userId)` | `entityManager.find(KataUser.class, id, PESSIMISTIC_WRITE)` 한 줄. 무엇을 어떤 잠금으로 잠그면 되는지가 메서드 이름과 본문에 다 있다 |
| `currentUserVersion`, `advanceUserVersion` | 사용자 행의 version을 조건부로 증가시키는 낙관적 락의 재료 |

기준선에서 이 셋을 쓰는 코드는 F-3의 두 서비스뿐이었고, 그것을 지우면서 메서드는 미사용으로 남았다.
run 1과 run 2 모두 `lockUserForUpdate`를 그대로 호출해 문제를 닫았다. run 2는 그 한 줄을 `subscribe`
첫 줄에 넣은 것이 변경의 전부다.

그래서 **두 run이 "조건을 주지 않았다"는 전제 아래 있었다고 말하기 어렵다.** 조건을 지시문에 적지 않았을
뿐이고, 호출만 하면 되는 형태로 코드에 남아 있었다. 두 run이 같은 자리로 수렴한 이유로 이 힌트가 유력하다.
추정이고 두 run으로 확인할 수는 없다.

run 3은 이 셋을 제거한 기준선에서 돌린다. `AlertRepository`에는 `findActive`와 `save`만 남는다.

남은 흔적도 적어 둔다. `KataUser.version` 필드와 `kata_users.version` 컬럼은 그대로 뒀다. 스키마와
엔티티는 현재 상태의 일부라는 설계 §4의 분류를 따랐다.

## 판정 규칙의 기계적 허점

테스트 파일의 원본 해시로 판정하면 **줄 끝 변환만으로 어긋난다.** run 2의 작업 공간은 `git clone`으로
만들었고 체크아웃에서 LF가 CRLF로 바뀌어, 테스트를 한 줄도 고치지 않았는데 해시가 달라졌다.

그래서 판정은 둘을 같이 본다. `git status`로 추적 파일의 변경 목록을 보고, 해시는 줄 끝을 LF로 정규화한
뒤 비교한다. run 2는 변경 목록에 `AlertService.java`만 있었고 정규화 해시가 동결값과 같았다.

같은 허점이 하네스의 인용 대조에도 있다. 인용문을 사본의 문자열과 맞출 때 줄 끝이 다르면 멀쩡한 인용이
탈락한다. `citation_absent` 규칙은 비교 전에 줄 끝을 정규화해야 한다.

## 남는 위험

작업 공간은 저장소와 다른 트리에 있지만 파일 시스템 전체 읽기를 막는 장치는 없다. 상위로 올라가 저장소를
찾는 것은 지시로만 막히고, 실행 trace를 남기지 않으면 지시가 지켜졌는지 확인할 방법이 없다. run 기록에
읽은 경로를 남길 수 있으면 남긴다.
