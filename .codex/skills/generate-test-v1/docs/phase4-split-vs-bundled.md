# Phase 4 — split vs bundled 실측 비교 & 선택 기록

> 실제 codex(gpt-5.5) run 결과로 두 토폴로지를 비교하고, **무엇을 왜 골랐는지** 남긴다.
> 설계 배경은 [../readme.md](../readme.md) §0, slow-loop은 [v1-slow-loop-design.md](v1-slow-loop-design.md).
> 보존: 이 비교 시점의 파이프라인·로그는 git tag `generate-test-v0-phase4` (commit id는 문서 끝에).

## 통제 조건

세 run 모두 **같은 정책**(`task1/refund_design.md` 환불 정책)에서 나왔다 — 통제된 비교.
- split: contract run `f1ba346d` → 동결 → unit run `7f1fd837`(frozen_contract 주입).
- bundled: run `8dbc7a7c` (같은 정책, 계약+단위 한 draft).
- 세 run 모두 최종 **PASS**(min_total·min_axis 게이트 통과). MAX_ITER·실패 없음.

## 실측

| 축 | split (contract → unit) | bundled |
|---|---|---|
| 수렴 Gen 사이클 | contract 2 + unit 3 = **5** | **1** (첫 draft에서 PASS) |
| 최종 weighted_total | contract **4.3** / unit **4.675** | **4.35** |
| coverage 궤적 | unit **1 → 5 → 5** (refine이 끌어올림) | **4** (첫 판정에서 확정, refine 없음) |
| 계약 churn | **0 — 동결로 구조적 차단** | 미발생(1회 수렴이라 refine 자체가 없었음) |
| 사람 승인 게이트 | contract y/n 동결 **有** | **無** |
| rubric | contract:v1(4축)·unit:v1(4축) | bundled:v1(5축, 의도적으로 어정쩡) |

세부 점수:
- split contract: iter1 3.9(coverage 3) → iter2 **4.3**(coverage 4). weak_axes 없음.
- split unit: iter1 3.6(**coverage 1**) → iter2 4.3(coverage 5, unambiguity 3) → iter3 **4.675**(coverage 5, unambiguity 4.5).
- bundled: iter1 **4.35**(coverage 4, unambiguity 4.5, behavioral_altitude 4.5, mock_discipline 5, independence_executability 4) → 즉시 final.

## 읽어낸 것

1. **bundled가 5배 싸고 빠르다.** 같은 두 산출물(계약+단위)을 bundled는 Gen 1회로, split은 5회로 냈다.
   이건 bundled의 실제 우위다 — 비용·지연이 1/5.
2. **그러나 split이 어려운 축(coverage)에서 더 멀리 간다.** split unit은 첫 draft가 coverage=1로 심하게
   빗나갔지만 refine 3회가 **1→5**로 구조했다. bundled는 coverage=4에서 **첫 판정에 PASS해버려 refine 압력을
   못 받았다** — 4에서 멈췄다. 즉 bundled의 빠른 수렴은 "덜 다듬어진 채 게이트를 넘은" 것이기도 하다.
3. **계약 churn은 첫 run(위 표)에선 미실증이었으나, bundled 재실행에서 실증됐다(아래 "## 재관찰").**
   첫 run은 bundled가 1회 통과라 refine이 없어 흔들 기회 자체가 없었다. 재실행에서 bundled가 게이트를
   못 넘고 3회 refine을 돌자, **매 iteration마다 gherkin이 재작성**됐다.
4. **split만 사람이 계약을 소유한다.** y/n 동결 게이트로 계약이 감사 정본으로 잠긴다. bundled엔 그 지점이 없다.

## 선택: split (권장 baseline 유지)

**무엇을**: split(contract→동결→unit)을 기본 토폴로지로 한다.

**왜**:
- **계약 무결성**: unit의 3회 refine(coverage 1→5 구조 포함)이 **계약을 전혀 위협하지 않고** 일어났다.
  bundled였다면 같은 refine 압력이 gherkin과 한 draft를 공유했다.
- **어려운 축에서 더 높은 품질**: refine 압력을 받는 split이 coverage 5에 도달, bundled는 4에서 정지.
- **사람 소유 게이트**: 계약이 y/n로 동결·감사된다.

**대가(정직하게)**: split은 Gen 5회 대 bundled 1회(첫 run 기준) — **비용·지연이 크다.** 단, 아래 재관찰이
보여주듯 bundled의 1회 수렴은 보장이 아니라 **운**이다(같은 입력이 게이트를 두고 진동). 계약 무결성이 필요하면
그 변동성 자체가 split을 정당화한다.

**언제 bundled를 쓰나**: 계약 재사용·감사가 불필요하고, 1회 통과에 실패해도 그만인 저위험·단순 정책일 때.
계약을 여러 단위 run이 공유하거나 사람이 계약을 소유해야 하면 split.

## 재관찰 — bundled 재실행에서 churn·비수렴 실증

같은 refund 정책(해시 `8dbc7a7c`, 첫 run과 동일 입력)을 bundled로 **다시** 돌렸더니 정반대가 나왔다.
게이트 min_total **4.3** / min_axis 3.5 기준:

| iter | total | 반려 사유 | gherkin 줄수 |
|---|---|---|---|
| 001 | 4.2 | `min_total 4.2 < 4.3` | 110 |
| 002 | **3.8** | `min_total 3.8 < 4.3` + `independence_executability 3.0 < 3.5` | 141 |
| 003 | 4.0 | `min_total 4 < 4.3` | 162 |

→ **max_iteration 초과 → FAILED**(수렴 실패).

- **churn 실증**: refine마다 gherkin 재작성 — 110→141→162줄, **Background 삭제**, Given 어휘 전면 교체
  (`주문은 결제 완료 상태다`→`주문 상태는 PAID이다`), 시나리오→Outline 개조, Examples 컬럼 추가, 제목 변경.
  계약이 한 번도 안정되지 않았다.
- **refine 퇴행**: iter2에서 총점 4.2→3.8, independence_executability 4→3. 단위 약점을 쫓다 draft 전체 악화.
- **비수렴**: 같은 정책을 split은 통과(contract 4.3/unit 4.675)했는데 bundled는 3회 안에 못 넘고 실패.
- **불안정성**: 같은 입력인데 첫 run은 4.35로 1회 통과, 이 run은 4.0~4.2에서 맴돌다 실패 —
  bundled 품질이 4.3 게이트를 사이에 두고 **진동**한다.

**함의**: split의 계약 동결은 이 churn·퇴행·발산을 **구조적으로 원천 차단**한다(계약이 별도 파일로 잠겨
refine이 손댈 수 없음). bundled는 한 draft라 refine 압력이 계약으로 샌다. → §"선택: split"의 근거가 실증으로 굳었다.

## 다음 관찰 과제

- bundled 첫 run(1회 통과)과 재run(실패·churn)의 **분기 원인**을 더 좁힌다: gen 난수인지, 게이트 4.3이 bundled엔 과한지.
- 이 관찰들이 쌓이면 v1 slow-loop이 (mode×rubric_name) 신호로 자동 집계한다([v1-slow-loop-design.md](v1-slow-loop-design.md) §2-1).

## 보존

- git tag: `generate-test-v0-phase4`
- commit id: `c85cd27fa48f09554d10b57f6cc04d9ee3a51e58`
- 재현: `git checkout generate-test-v0-phase4` (또는 위 commit id)
