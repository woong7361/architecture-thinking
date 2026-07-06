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
3. **계약 churn은 이 입력에선 어느 쪽도 실증되지 않았다(정직한 한계).** split은 동결로 구조적으로 0.
   bundled는 refine이 안 일어나 gherkin을 흔들 기회 자체가 없었다. "bundled가 계약을 흔든다"는 가설은
   *구조적 위험*(계약+단위가 한 draft라 refine이 계약을 건드릴 수 있음)일 뿐, **이 run으로 증명된 건 아니다.**
   더 어려운/모호한 정책으로 bundled가 refine을 여러 번 돌 때라야 관찰된다.
4. **split만 사람이 계약을 소유한다.** y/n 동결 게이트로 계약이 감사 정본으로 잠긴다. bundled엔 그 지점이 없다.

## 선택: split (권장 baseline 유지)

**무엇을**: split(contract→동결→unit)을 기본 토폴로지로 한다.

**왜**:
- **계약 무결성**: unit의 3회 refine(coverage 1→5 구조 포함)이 **계약을 전혀 위협하지 않고** 일어났다.
  bundled였다면 같은 refine 압력이 gherkin과 한 draft를 공유했다.
- **어려운 축에서 더 높은 품질**: refine 압력을 받는 split이 coverage 5에 도달, bundled는 4에서 정지.
- **사람 소유 게이트**: 계약이 y/n로 동결·감사된다.

**대가(정직하게)**: split은 Gen 5회 대 bundled 1회 — **비용·지연이 크다.** 그리고 이 입력에선 bundled도 4.35로
멀쩡히 PASS했다. 즉 "품질이 안정적이고 계약을 다시 안 건드려도 되는 단순 정책"에선 bundled가 합리적 선택일 수 있다.

**언제 bundled를 쓰나**: 계약 재사용·감사가 불필요하고 빠른 1회 산출이 중요할 때. 계약을 여러 단위 run이
제약으로 공유하거나 사람이 계약을 소유해야 하면 split.

## 다음 관찰 과제

- 계약 churn을 실제로 보려면 **bundled가 refine을 여러 번 도는 더 모호한 정책**을 물려 gherkin 변동을 로그로 잡는다.
- 이 관찰들이 쌓이면 v1 slow-loop이 (mode×rubric_name) 신호로 자동 집계한다([v1-slow-loop-design.md](v1-slow-loop-design.md) §2-1).

## 보존

- git tag: `generate-test-v0-phase4`
- commit id: <커밋 후 기입>
