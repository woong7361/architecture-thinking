# 리팩토링 로그 — task2 B-5 (TicketService)

## 개요 — 이 로그를 왜 남기나

B-5는 Red→Green→Refactor 사이클이 아니라 **행위 보존 리팩토링**이다. 그래서 이 로그는 사이클 표가 아니라
"**어떤 수정(파울러 카탈로그 기법)을 어떤 순서로 커밋했나**"만 남긴다. 이렇게 기록하는 이유는:

- **행위 보존을 커밋 단위로 추적하려고.** 리팩토링은 구조만 바꾸고 동작은 그대로여야 한다. 안전망(특성화
  테스트)이 **매 커밋 GREEN**임을 조건으로 걸면, "구조는 바뀌었는데 행위는 그대로"임이 커밋마다 증명된다.
- **한 커밋 = 한 기법임을 검증하려고.** 각 커밋이 메서드 추출 / 의존성 주입 / 인터페이스 도출 / 도메인으로
  행위 이동 중 **하나만** 담았는지 히스토리로 대조한다. 그래야 리뷰와 되돌리기가 쉽다.
- **왜 그 수정을 했는지 사람이 읽게 하려고.** 파울러 기법명을 커밋 제목과 이 표에 명시하면, 코드 diff만으로는
  안 보이는 "무슨 냄새를 어떤 기법으로 없앴나"가 남는다.

## 대상 / 스택

- 대상: B-2 kata `TicketService.reserveTicket` (절차적 원본). baseline은 이 폴더의 Maven 프로젝트.
- 스택: JDK 17(Amazon Corretto 17.0.19) + Cucumber-JVM 7.18 + junit-platform-suite + AssertJ 3.25.
- 안전망: reserveTicket 유스케이스 경계의 Cucumber 인수테스트(6 시나리오). repos=in-memory fake,
  결제=test double로 상태·결과를 검증한다. `mvn test`로 실행하며, 리팩토링 전 baseline에서 GREEN이다.

## 커밋 히스토리 (제목 순서)

| # | 적용한 수정 (파울러 기법명) | 스멜 | 커밋 제목 | 커밋 | 테스트 |
|---|---------------------------|:---:|----------|------|:---:|
| 0 | (안전망) 리팩토링 전 특성화 인수테스트로 현재 동작 고정 (quirk 포함) | — | `test: TicketService 특성화 인수테스트 — 리팩토링 전 안전망 (6 GREEN)` | `f97c1a7` | 6✅ |
| — | (계획) 게이트 적용 + 커밋 시퀀스 | — | `docs: B-5 리팩토링 실행 전략 — 게이트 적용 + 커밋 계획(C1~C5)` | `0e1cd12` | — |
| 1 | **Extract Function** (도메인으로 행위 이동) — 예약 가능 판단 → `Ticket.ensureReservable()` | #9 #22 | `refactor: 예약 가능 판단을 Ticket으로 추출 …` | `4cd1bca` | 6✅ |
| 2 | **Extract Function** (도메인으로 행위 이동) — 상태 전이 → `Ticket.assignTo(userId)` (+내부 guard) | #22 #9 | `refactor: 상태 전이를 Ticket으로 추출 …` | `bc33046` | 6✅ |
| 3 | **Remove Setting Method** — `setReserved`·`setUserId` 삭제 | #6 | `refactor: Ticket의 public setter 제거 …` | `d5cceed` | 6✅ |
| 4 | **Rename / Change Function Declaration** — `PaymentApi` → `ChargePort` | #1 | `refactor: PaymentApi를 ChargePort로 개명 …` | `00da93d` | 6✅ |
| 5 | **Comments 정리**(#24 해소의 삭제 단계) — 단계 주석·낡은 헤더 정리 | #24 | `refactor: 재사용성 잃은 단계 주석·낡은 클래스 주석 정리 …` | `bd73e98` | 6✅ |

> 1~5번이 B-3(Rich Domain)+B-4(SOLID) 방향의 행위 보존 리팩토링 커밋(각 커밋 = 파울러 기법 하나, **매 커밋 6 시나리오 GREEN**).
> 부분별 상세(무엇을 어떻게 바꿨나 · before/after · GREEN 보존 근거)는 **[refactoring-report.md](refactoring-report.md)** 참조.
>
> **재현 주의:** 이 머신은 `JAVA_HOME`이 JDK 8을 가리켜 `mvn test`가 포크 실패한다. JDK 17로 실행해야 GREEN:
> `JAVA_HOME="C:/Program Files/Amazon Corretto/jdk17.0.19_10" mvn clean test`
