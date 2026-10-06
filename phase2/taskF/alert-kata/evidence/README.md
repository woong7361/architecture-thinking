# 실행 증거

## 최신 결과: 사전 검토 후 다섯 조건 재시험

현재 코드의 실행 결과는 [review-rerun.log](review-rerun.log), [JUnit 결과](review-rerun-junit.txt), [집계 JSON](review-rerun-summary.json)에 있다. 조건별 중복은 순차 신규 0/100, 공통 시작 신규 100/100, 시작 신호 없는 신규 100/100, 기존 신청 재사용 0/100, 다른 사용자 0/100이다. 요청 오류는 모든 조건에서 0회이며 JUnit은 10개 중 2개 실패했다.

실행 명령은 `mvn -B -ntp -Pkata-red test`이며 종료 코드는 1이다. [재시험 전 검토](review-before-rerun.md)를 먼저 수행하고 [결과 보고서](review-results.md)에 판정과 한계를 정리했다. `review-compile.log`는 재시험 준비 중의 컴파일 기록이다.

회차별 로그에는 초기 조건, 최종 행 ID, 반환 ID, 요청 오류, 중복 여부, 계약 위반 여부, 호출 시작 차이와 구간 겹침이 있다. 오류·중복·계약 위반 지표는 서로 겹칠 수 있다. `ABORT`가 있으면 해당 조건은 완주하지 못한 것이며 이번 실행에는 없다.

## 이전 결과: 신규 신청 한 조건만 100회 관찰

2026-10-06에 조회 후 대기와 테스트용 저장소 교체를 제거하고 실제 MySQL로 다시 실행했다. 정상 테스트 5개는 통과했고, 요청 2개씩 100회 실행한 결과 중복은 100회, 정상은 0회, 요청 오류와 기타 결과는 각각 0회였다.

| 실행 | 원본 로그 | JUnit 결과 | 종료 코드 |
|---|---|---|---|
| `mvn -B -ntp test` | [baseline.log](baseline.log) | [5개 통과](baseline-junit.txt) | 0 |
| `mvn -B -ntp -Pkata-red test` | [concurrent-100.log](concurrent-100.log) | [6개 중 집계 테스트 실패 1개](concurrent-100-junit.txt) | 1 |

각 회차 시작 전에 신청 데이터를 비우고 서비스 호출 직전의 시작 신호만 맞췄다. 이후 내부 실행 순서는 제어하지 않았으며, 총 200개의 호출이 정상 커밋된 뒤 회차별로 2건을 확인했다. 중복 발생 회차가 0이어야 한다는 단언이 실제 100회와 달라 실패했다.

로그의 `ENV`는 MySQL 8.4.8, `REPEATABLE-READ`, 두 테이블의 `InnoDB`를 보여준다. `ROUND`는 회차별 요청 수·커밋 수·기대값·실제값·호출별 반환 ID와 오류를 기록하며 `SUMMARY`는 100회의 합계다. 서비스 내부의 조회 시점이나 연결 ID는 따로 측정하지 않는다.

발생 횟수는 이 로컬 실험의 관찰값이며 운영 환경의 발생 확률이 아니다. [이전 제어 실험](previous-controlled/README.md)의 코드·로그와 기존 `kata-red.log`, `kata-red-junit.txt`, `compile.log`는 과거 기록으로 남겨 두며 현재 결과에 합산하지 않는다.

준비한 도구는 Temurin 25.0.4.1+1, Maven 3.9.12, Docker 29.6.2이며, MySQL 이미지 태그는 `mysql:8.4.8`이다. 내려받은 이미지의 저장소 digest는 `sha256:2952e3be7807f06fc18de50b3ea1a632d5c70d63482ff7d7376fe3aa8999babf`다.

JDK ZIP은 [Adoptium 공식 배포](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4.1%2B1)의 `OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1.zip`을 사용했다. 공식 API의 SHA-256 `00c847d804f4a78e9f04f2683faf14fed898535b177b7fc704486cb0284e9283`과 다운로드 파일이 일치함을 확인했다.
