# 단계 0 — 재현 테스트 빨간불 고정

2026-10-28 실행. 이 로그와 해시가 단계 1의 판정 기준이다. 단계 1의 구현은 여기 실패한 두 자리가 초록불이
되는지로만 판정하고, 테스트 파일이 이 해시에서 달라지면 그 run은 판정 대상이 아니다.

## 실행

```powershell
$env:JAVA_HOME = 'C:\Users\PC-220627-03\AppData\Local\codex-task-tools\jdk-25.0.4.1+1'
$env:KATA_DB_PASSWORD = 'test'
mvn -B -ntp -Pkata-red -Dtest=AlertSubscriptionTest test
```

종료 코드 1. 원본 로그는 [step0-red-2026-10-28.log](step0-red-2026-10-28.log), JUnit 결과는
[step0-red-junit.txt](step0-red-junit.txt)다.

재현 테스트 한 벌만 돌렸다. F-3의 락 비교와 대기 방식 비교는 단계 0의 판정 대상이 아니라서 제외했다.
`mvn -Pkata-red test`로 전부 돌리면 그 테스트들까지 실행된다.

## 환경

로그의 `ENV` 줄에서 그대로 옮긴다.

| 항목 | 값 |
| --- | --- |
| MySQL | `mysql:8.4.8`, InnoDB |
| 격리 수준 | REPEATABLE-READ |
| JDK | 25.0.4.1 |
| 테이블 | `alert_subscriptions`, `kata_users` |

## 결과

테스트 10개 중 2개 실패다. 조건마다 두 요청씩 100회, 실험 요청 1,000개다.

| 조건 | 중복 | 계약 위반 | 요청 오류 | 호출 구간 겹침 |
| --- | ---: | ---: | ---: | ---: |
| `NEW_SEQUENTIAL` | 0/100 | 0/100 | 0/100 | 0/100 |
| `NEW_TOGETHER` | **100/100** | **100/100** | 0/100 | 100/100 |
| `NEW_UNCOORDINATED` | **100/100** | **100/100** | 0/100 | 100/100 |
| `EXISTING_TOGETHER` | 0/100 | 0/100 | 0/100 | 100/100 |
| `DIFFERENT_USERS` | 0/100 | 0/100 | 0/100 | 100/100 |

실패한 자리는 `AlertSubscriptionTest.repeatedRequestsMustRespectTheSubscriptionContract`의 두 파라미터이고,
단언 둘이 함께 깨졌다. `Duplicate rounds out of 100 expected: <0> but was: <100>`과
`Contract violation rounds expected: <0> but was: <100>`이다.

같은 조건의 신규 신청 두 건이 겹칠 때만 깨진다. 기존 신청의 재사용과 서로 다른 사용자는 호출 구간이 100회
모두 겹쳤는데도 중복이 없었으므로, 깨지는 원인은 호출이 겹친다는 것 자체가 아니라 빈 조회 뒤 두 요청이 각각
새 행을 넣는 자리다.

요청 오류가 전 조건 0회라는 것도 함께 고정한다. 단계 1의 구현이 중복을 0으로 만들면서 요청 오류를 만들면
그것은 통과가 아니다. 중복과 요청 오류를 함께 보는 기준은 F-3에서 가장 강한 격리 수준이 중복 0건에 오류
100회를 낸 관찰에서 나왔다.

## 동결한 것

[test-files.sha256](test-files.sha256)에 `src/test` 아래 전부의 해시가 있다. 재현 테스트는
`AlertSubscriptionTest.java`의 `d30a953d...`다.

단계 1은 `src/main` 아래만 쓴다. 테스트 경로가 변경 목록에 나타나면 그 run은 탈락이다. 테스트를 고쳐
초록불을 만드는 것이 가장 싼 경로이기 때문이다.

## 한계

같은 로컬 환경에서 같은 JVM과 연결 풀을 재사용한 반복 관찰이다. 100/100은 이 조건에서 결함을 일으키는
겹침이 매회 만들어졌다는 뜻이고, 운영 환경의 발생 확률이 아니다. 다른 실행에서는 횟수가 달라질 수 있으며
0회여도 그 실행에서 발견하지 못했다는 뜻이다.
