# 파이프라인 제안 vs 손 리팩토링(B-5) 비교 (B-6 수행내용 #2)

- **대상**: B-2 kata `TicketService`(절차적 원본).
- **파이프라인**: `runs/c0/`(Diagnose→Implement→Validate→Critique∥Eval, clean run).
- **손 리팩토링**: `task2/task5-history/`(B-5, C0→C6 커밋, [refactoring-log.md](../../../../task2/task5-history/refactoring-log.md)).
- **baseline**: 동일(`refactor-agent-c0-baseline` = f97c1a7 원본 + 경계-클린 글루).

## 축별 대조

| 축 | 파이프라인 (C0) | 손 리팩토링 (B-5) |
|---|---|---|
| **진단한 위반** | #9 Feature Envy, #22 Data Class, #6 Mutable Data, #3 Long Function, #24 Comments (#11 Primitive Obsession은 LEAVE) | #9, #22, #6, #1 Mysterious Name, #24, #2 Duplicated Code |
| **놓친 위반** | **#1(PaymentApi→ChargePort, DIP)**, **#2(중복)** | — |
| **파울러 기법** | Move Function ×2 + Remove Setting Method (3) | Extract Function ×2 + Remove Setting Method + Rename + Comments 정리 + Remove Duplication (6) |
| **변경 단위** | 한 Implement에 **묶음**(제안 3 → 1 구현) | **한 커밋 한 기법 ×6** (리뷰·롤백 쉬움) |
| **행위 보존** | 6 Scenarios GREEN | 매 커밋 6 GREEN |
| **테스트 용이성** | ensureReservable/reserveTo 도메인 이동 (Eval 4/5) | 동일 + 가드 내재화 |
| **과설계** | 없음 — OCP 결제 다형성 **안 제안**(Type B·v<2, 확정 요구 없음 → YAGNI) | 없음 (동일하게 DEFER) |

## 실제 코드 차이 (핵심)

**1. #24 Comments — 진단은 했으나 구현이 안 따라감.**
파이프라인 Diagnose는 `#24 Comments`를 **GO**로 올렸지만, Implement가 낸 `reserveTicket`엔
`// 1. 유저 조회` ~ `// 4. 티켓 상태 변경` 단계 주석이 **그대로 남았다**. 진단↔구현 불일치.
B-5는 C5에서 실제로 제거(Extract가 이름으로 설명). → **진단/구현 분리**가 이 갭을 노출.

**2. 불변식 위치 — pipeline은 opt-in, B-5는 내재화.**
- 파이프라인 `reserveTo(userId)`: 내부에서 `ensureReservable()`을 **안 부른다** → 이미 예약된 티켓도
  조용히 덮어쓸 수 있고, 서비스가 `ensureReservable()`을 **잊으면 규칙이 안 지켜진다**(호출 순서 의존).
- B-5 `assignTo(userId)`: 내부에서 `ensureReservable()`을 호출(C6 Remove Duplication) → **불변식이 도메인
  객체 안에서 원자적으로** 지켜진다. 캡슐화가 더 강하다.

**3. DIP.** B-5는 `PaymentApi→ChargePort` 개명으로 벤더 이름 의존을 역할로 바꿨다(C4). 파이프라인은
이 위반을 아예 안 짚었다(진단 누락). 단, 이 개명은 테스트 글루의 타입 참조를 건드리므로 파이프라인 설계상
"테스트 코드 불가침"과 충돌하는 지점이기도 하다(경계-인접 변경).

## 어느 쪽이 더 나았나 — 정직한 판정

**손 리팩토링(B-5)이 더 완성도·규율이 높다.**
- 6개 기법으로 **더 완전**(개명/DIP·주석·중복까지), **한 커밋 한 기법**이라 리뷰·되돌리기 쉽고,
  불변식을 도메인 안에 내재화해 **설계 품질이 높다**.

**파이프라인은 강한 1차 제안자이나 더 얕다.**
- **핵심 스멜을 맞췄고**(#9/#22/#6/#3), **행위를 보존**했고, **YAGNI를 지켰다**(과설계 0).
- 그러나 (a) 변경을 묶어 granularity가 낮고, (b) **진단한 #24를 코드로 안 옮겼고**, (c) #1·#2를 놓쳤고,
  (d) 가드를 내재화하지 않았다.
- **주목**: 이 갭들(#24 미구현·가드 opt-in·#3 잔존)을 **파이프라인 자신의 Critique가 정확히 짚었다.**
  즉 파이프라인은 자기 한계를 스스로 플래그한다 → "사람이 완성하는 1차 제안"으로 쓰면 강하다.

> **결론:** 완성도·규율은 사람(B-5) 승. 파이프라인은 진단 일치·행위 안전·YAGNI에서 대등하고 **빠르며,
> 자기 약점을 Critique로 드러낸다.** 이는 B-6가 의도한 "리팩토링 **제안** 에이전트"의 위치 그대로다 —
> 대체가 아니라, 사람이 검토·완성하는 검증된 후보를 낸다.

## 과설계 사례 기록 (수행내용 #3)

- **과설계 발생: 없음.** 파이프라인은 B-2 답안의 "결제수단을 Strategy로"(OCP, Type B)를 **제안하지 않았다** —
  확정된 결제수단이 카드 1개뿐이라 `v<2 → DEFER`(내장 과설계 프로브 통과). 참조 2부 게이트가 결정적으로 억제.
- 결정적 술어(`Type B ∧ v<2 ∧ GO`) 위반 0건. 사람(B-5)도 동일하게 DEFER.
- 즉 이 kata에선 양쪽 다 과설계를 피했고, 파이프라인의 억제는 **감이 아니라 게이트 규칙**으로 재현 가능하다.
