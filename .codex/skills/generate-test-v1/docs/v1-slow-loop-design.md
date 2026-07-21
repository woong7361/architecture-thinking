# generate-test v1 — Slow Loop 설계

> v0 설계는 [../readme.md](../readme.md)("TDD 하네스 v0 설계"). 이 문서는 그 위에 얹는 v1 = **자가 피드백 루프(slow loop)**를 정의한다.

---

## 0. 왜 지금 / 무엇을 재사용하나

v0 readme §5는 slow loop을 의도적으로 뺐지만 **그 입력이 되는 run 로그 포맷은 미리 맞춰뒀다**:
attempt별 `eval.json`(axis 점수+rationale+`rubric_name`+weighted_total), `critique.json`(weaknesses),
메타(mode/iteration/terminal_reason). v1은 이 로그를 갈아엎지 않고 위에 얹는다.

fast loop(v0)은 **산출물 하나**(한 테스트)를 생성·비평·평가·판정한다.
slow loop(v1)은 fast loop이 남긴 run 로그를 모아 **파이프라인 자체를 고치는 제안**을 만든다.
slow loop의 3분할·정보차단·고정점 원칙을 따른다.

- **재사용**: 오케스트레이션(파일 핸드오프·검증 게이트·정보 차단), rubric 스키마 포맷(0–5/weights/thresholds)과 로더, 단일 CHANGELOG, 버저닝. (blog-draft의 pending/reviewed **폴더 이동**은 v0 동결과 충돌해 **제자리 `.reviewed` 마커**로 대체 — §7.)
- **새로/조정**: proposal 전용 rubric(`proposal:v1`)의 축, `propose_*` 프롬프트 내용, 그리고 아래 §2의 **generate-test 고유 4개 조정**.

---

## 1. 잠긴 결정 (5개 + v2)

| # | v1 결정 | 근거 |
|---|---|---|
| 1 | slow loop = **풀 slow-loop 파이프라인**: `analyze_runs.py`(결정적 집계) → proposer 4단(gen/critique/eval/refine + `proposal:v1`) → **사람 적용**. **자동 발동 안 함**: 미검토 run ≥ 5면 스킬 세션 끝에 사람에게 물음(§5-B). 상태는 **제자리 `.reviewed` 마커**(폴더 이동 없음, §7). 단일 CHANGELOG. | 검증된 구조, 로그 포맷 이미 정합. 비용 나는 루프는 사람이 켠다. 폴더 이동은 v0 동결과 충돌 |
| 2 | 신호를 **mode × rubric_name으로 분할**. "어디 고칠지" 후보 테이블을 테스트 스테이지로 재작성(§2). | contract/unit/bundled는 rubric·gen 프롬프트가 달라 통계를 섞으면 안 됨 |
| 3 | **실행 기반 신호 없음.** SUT(구현체)가 없어 생성 테스트를 돌릴 수 없다(§3). **손 참조 목록 대조도 slow-loop 앵커에서 뺀다**(특정 도메인 한정이라 재사용 스킬에 결합 불가). 유일 앵커 = **problem.md 사람 피드백**. | 테스트-only 생성기의 구조적 제약 + 도메인 결합 제거 |
| 4 | **problem.md = 사용자 피드백 누적 전용**(§5). 설계 쟁점은 루트 `PROBLEM.md`. | 실행·손목록 신호가 없어진 지금 **problem.md가 유일한 non-circular 앵커**(load-bearing) |
| 5 | proposer가 **rubric을 건드리는 제안**은 위험="높음 + 효과 검증 장치 없음(v2까지 보류 권장)" 라벨. proposer는 **생성기/프롬프트/intake 제안을 우선**. rubric은 최후 수단. | "잣대"를 가장 덜 건드리고 세게 게이트(§4) |
| **v2** | rubric 변경의 효과 검증 = **동결 캘리브레이션 셋 + 사람-앵커 대조**(§6). 데이터가 쌓인 뒤. | v1엔 사람 verdict가 없어 검증 장치를 지을 수 없음 |

---

## 2. 3분할과 generate-test 고유 조정

흔들림을 3분할한다.

| 단계 | 주체 | 흔들림 | 산출 |
|---|---|---|---|
| 신호 집계 | 코드 (`analyze_runs.py`) | 없음 | `analysis.json` |
| 수정 제안 | LLM (proposer 4단) | 있음 | `changelog/proposals/YYYY-MM-DD.md` (diff 초안) |
| 기준 변경 | **사람** | — | 대상 파일 + `CHANGELOG.md` |

최종 변경은 사람이 한다(전역 AGENTS.md "확인 없이 규칙 파일 수정 안 함"과 일치).

### 2-1. 조정 A — 신호를 mode × rubric_name으로 분할

`analyze_runs.py`는 통계를 **(mode, rubric_name) 키**로 나눠 집계한다. contract(`contract:v0`)·unit(`unit:v0`)·
bundled(`bundled:v0`)를 한 통계에 섞지 않는다. `eval.json.rubric_name`이 이미 이 구분을 self-describing으로 담는다.
→ v0의 원래 비교 목적("bundled가 계약을 흔드나")이 slow-loop 신호로 자동 승격된다:
bundled의 특정 axis가 split보다 반복 미달이면 그 자체가 제안 후보 신호.

### 2-2. 조정 B — "어디 고칠지" 후보 테이블(테스트 스테이지판)

낮은 점수는 target을 지목하지 않는다. `analyze_runs.py`는 신호·후보만 띄우고, 진단은 proposer, 결정은 사람.

| 가능한 원인 | 고칠 대상 후보 |
|---|---|
| Gen이 경계·실패를 안 발굴 | `prompts/gen_contract.md` / `gen_unit.md` |
| Refine이 약점을 못 살림 | `prompts/refine.md`(공유) |
| 기준(bar)이 오조정 | `rubrics/<mode>.rubric.yaml` — **위험 높음, §4** |
| Eval이 축을 과/소평가 | `prompts/eval_system.md`(공유) |
| intake가 정책 공백을 못 메워 재료 부족 | `SKILL.md` intake 규칙 / `intake_to_input.py` |

신호 → 후보 매핑:

| 신호 | 시사 후보 |
|---|---|
| 같은 axis 반복 미달 | rubric 또는 gen 프롬프트 |
| critique 같은 지적 반복 | gen/refine 프롬프트(생성이 못 잡음) |
| eval rationale "이건 못 봤다" 반복 | `eval_system.md`(평가 시야) |
| problem.md 사용자 neg 반복 | 지적 내용에 따라(위 표) — **유일한 사람발 앵커** |

패턴 임계(기본): 한 axis가 (해당 mode) pending의 **60% 이상**에서 `min_axis` 미달이면 제안 후보로 올린다.

---

## 3. 왜 실행 기반 신호가 없나 (결정 #3)

slow-loop이 rubric 점수(LLM, 흔들림)만 보는 걸 막으려면 실행 기반 ground-truth 신호가 있으면 좋겠지만,
generate-test 구조상 **불가능**하다.

- **돌릴 대상이 없다.** generate-test는 정책 → 테스트만 만들고 **구현체(SUT)를 만들지 않는다.** unit 테스트는
  자기가 지어낸 API(`RefundService.calculate(...)`)를 호출하는데 그 클래스가 저장소에 없어 컴파일조차 안 된다.
  contract의 gherkin은 step definition 없이는 실행 개념이 없다.
- **참조 구현이 있어도 안 붙는다.** 설령 어떤 도메인에 손 참조 구현이 있어도, 생성 테스트가 추측한 이름·시그니처가
  그 구현과 일치한다는 보장이 없다. 매 run 수동 어댑터 없이는 결합 불가.

**특정 도메인의 손 참조 목록도 slow-loop 앵커로는 쓰지 않는다.** 그런 목록은 그 도메인에만 존재해
새 도메인엔 없고, "생성 테스트가 경계를 덮었나"의 대응 판정도 문자열 매칭이 아니라 LLM이 개입해야 해
순수 기계 신호도 아니다. 재사용 스킬의 상시 신호에 특정 도메인을 결합시키는 leak이라 뺀다.

**따라서 실행 없이 남는 독립 앵커는 하나뿐이다:**

- **problem.md 사람 피드백** — 세션 내 사용자 verdict(§5).

**정직한 한계:** problem.md가 비어 있는 동안(특히 새 도메인 초기) slow-loop의 신호는 rubric axis + critique뿐인데,
이 둘은 같은 LLM이 같은 draft를 봐서 블라인드스팟이 상관된다(§4 우려). 즉 **problem.md가 차기 전까지 slow-loop은
사실상 자기참조**다. 그래서 problem.md 캡처 배선(§5-A)이 v1의 필수 조건이고, rubric 제안 가드(§4)가 그때까지의 안전장치다.

---

## 4. rubric 제안 가드 — "잣대"를 지키기 (결정 #5)

낮은 점수만 보고 고치면 두 문제가 생긴다: (a) **순환성** — 채점자(rubric)가 곧 최적화 목표가 됨(Goodhart),
(b) **잣대 이동** — rubric을 바꾸면 그 axis의 before/after 비교가 정의상 무효.

핵심은 **모든 제안이 잣대를 움직이는 게 아니다**라는 것:

- **생성기/프롬프트/intake 제안**(gen_*·refine·eval_system·intake): rubric 고정 → **axis before/after 비교 유효.**
  대부분의 제안은 여기 있어야 한다.
- **rubric 제안**: axis before/after **무효.** 검증하려면 rubric 밖의 고정된 사람 기준이 필요(§6, v2).

그래서 v1은 rubric 변경을 **미루되 정직하게** 미룬다(공짜 가드):

- proposer 프롬프트: **생성기/프롬프트/intake 대상을 우선**하라. rubric은 최후 수단.
- rubric을 건드리는 제안은 **위험="높음" + "효과 검증 장치 없음, v2까지 보류 권장"** 라벨(위험 티어에 rubric을 얹는다).
- 정당화 조건: **"점수가 낮다"는 rubric을 고칠 근거가 못 된다**(→ 생성기를 고쳐라). **"rubric이 사람 판정과 어긋난다"**만 근거가 된다 — 그리고 그 판정은 v2 캘리브레이션 셋에서 온다.

이는 **고정점 원칙**(slow loop 자기 rubric은 사람만 변경)을 fast-loop rubric까지 확장한 것이다.

---

## 5. problem.md — 사용자 피드백 누적 (결정 #4)

`problem.md`는 "사용자 피드백 누적" 전용이다(설계 쟁점은 담지 않는다 — 그건 루트 `PROBLEM.md`).
실행·손목록 신호가 없어진 지금 **이것이 유일한 사람발 non-circular 앵커**다(load-bearing).
- **역할**: 같은 지적이 반복되면 proposer가 이 섹션을 context로 읽어 proposal에 반영. 긍정 피드백은 회귀 방지 신호(제안이 긍정 지점을 건드리면 경고 근거).

항목 형식:

```
- (YYYY-MM-DD, run_id, verdict=pos|neg) 사용자 반응 요약 → 도출한 교훈
```

### 5-A. 캡처 배선 (v1 필수) — 안 하면 앵커가 장식으로 죽는다

파이프라인은 JSON in/out이라 피드백을 못 받는다. 채울 수 있는 주체는 **인터랙티브 스킬 세션(Claude)뿐**이다.
따라서 SKILL.md workflow에 캡처 스텝이 **없으면 problem.md는 영원히 비고, §3·§4·§6이 공중에 뜬다.** v1은 이걸 배선한다:

- SKILL.md에 스텝 추가: "final 제시 후, 사용자가 생성 테스트에 반응하면(누락 지적/좋다/틀렸다) `problem.md`에
  `- (날짜, run_id, verdict=pos|neg) 요약 → 교훈` 한 줄 추가. 긍정도 회귀 방지 신호이므로 기록."
- **언제 묻나**: 매 run 강제 질문은 피곤하니, final 제시할 때 "누락된 케이스 있으면 알려주세요" 정도로 가볍게 열어두고,
  사용자가 자발적으로 반응할 때만 캡처한다.

### 5-B. 트리거 — 자동 발동 안 함, 세션 끝에 물어봄

blog-draft는 매 run 끝에 pending을 세서 자동 발동하지만, proposer는 codex 4단을 돌려 **실제 비용·시간**이 난다
([[codex-run-pacing]]와 상충 — 사용자가 예상 못 한 시점에 비용). v1은 **사람이 켠다**:

- **미검토 run(= `.reviewed` 마커 없는 run) ≥ 5**가 쌓이면, 스킬 세션의 한 process가 끝난 시점에 사용자에게
  "미검토 run N개 쌓였습니다. slow-loop 돌릴까요?"를 **묻는다**(자동 실행 금지). 발동은 사용자 승인 시.
- 이 트리거 체크·질문은 **SKILL.md가 소유**한다(runner/파이프라인이 아니라 오케스트레이션 층).
- **입도**: 미검토 카운트는 전체로 세되, proposer는 **표본 충분한 mode만**(해당 mode 미검토 ≥ 3) 제안한다.
  표본 부족한 mode는 `analysis.json`에 "insufficient data"로 표기하고 제안하지 않는다(§2-2 60% 임계가 무의미해지는 것 방지).

---

## 6. v2로 미루는 것 — rubric 변경 검증

v1은 rubric 제안을 라벨링만 하고 효과는 검증하지 않는다. v2에서 다음을 짓는다(데이터가 쌓인 뒤):

- **동결 캘리브레이션 셋**: 사람 verdict가 붙은 과거 아티팩트 소수를 얼려둔다(출처: problem.md verdict). 특정 도메인의 손 참조 목록은 재사용 소스로 쓰지 않는다.
- **rubric 변경 검증**: rubric v→v'를 이 동결 셋에 재채점 → **어느 쪽이 사람 판정과 더 일치하는가**로 판정.
  ground truth(동결 아티팩트의 사람 판정)는 rubric이 바뀌어도 안 움직이므로 before/after가 성립한다.
- **적중률 측정**: CHANGELOG의 겨냥 axis를 근거로 적용 후 분포를 before/after 비교(`version_deltas`).

**정직한 제약**: rubric을 신뢰성 있게 바꿀 수 있는 속도는 사람 verdict가 쌓이는 속도에 묶인다. 사람 앵커 없이 rubric을 자동으로 못 고친다.

---

## 7. 신규 / 변경 파일

신규:

```
pipeline/
  changelog/
    CHANGELOG.md
    proposals/YYYY-MM-DD.md
  stages/proposer.py            # 제안 gen/critique/eval/refine 오케스트레이션
  prompts/
    propose_gen_system.md  propose_critique_system.md
    propose_eval_system.md propose_refine_system.md
  rubric_proposal.yaml          # proposal:v1 (테스트 rubric과 별개)
  schemas/analysis.schema.json
  analyze_runs.py               # (mode,rubric_name)별 신호 집계 + proposer 호출 + .reviewed 마커 기록
docs/v1-slow-loop-design.md     # 이 문서
problem.md                      # 사용자 피드백 누적 (§5)
```

**상태 = 제자리 마커(폴더 이동 없음).** run 폴더는 v0 위치 그대로 두고, analyze가 분석을 끝낸 run 폴더에
빈 `.reviewed` 파일을 떨군다. 미검토 = `.reviewed` 없는 run. 폴더를 안 옮기므로 split 동결 아티팩트 경로가
안 깨지고(v0 freeze-in-place와 충돌 소멸), unit input의 provenance 포인터도 그대로 유효하다.

변경(최소):

- `run_draft.py`: **출력 경로 변경 없음**(v0 그대로). 트리거 체크·질문은 여기가 아니라 SKILL.md가 소유(§5-B).
- `SKILL.md`: (1) problem.md 캡처 스텝(§5-A), (2) 미검토 ≥ 5면 세션 끝에 slow-loop 발동 여부를 묻는 스텝(§5-B).
- `pipeline/AGENTS.md`(또는 CLAUDE.md): `## Slow Loop` 섹션 + 제안 단계 역할 경계 + 정보 차단 두 층위.

---

## 8. 정보 차단 — slow loop 예외

- **층위 1(제안 파이프라인 내부)**: fast loop과 동일 차단. 제안 critique가 제안 eval에 anchor되지 않는다.
- **층위 2(context 입력)**: diff는 관계적이라 대상 파일을 봐야 판단됨. 각 단계는 context(대상 파일)는 읽되 **다른 단계 산출물은 안 읽는다.**

| 단계 | 읽는 것 | 안 읽는 것 |
|---|---|---|
| gen | `analysis.json` + 후보 target 전체(rubric·모든 프롬프트·SKILL.md/CLAUDE.md·관련 코드) + problem.md | 다른 단계 산출물 |
| critique | `analysis.json` + 제안 + 제안이 건드린 파일만 | 제안 eval |
| eval | `analysis.json` + 제안 + 건드린 파일 + `proposal:v1` rubric | 제안 critique |
| refine | `analysis.json` + 직전 제안 + critique + 건드린 파일 | 제안 eval 총점 원문 |

이중 방어: (1) `proposer.py`가 각 단계 payload를 위 표대로 제한, (2) 각 `propose_*` 프롬프트에 "받은 context와 제안만 보라" 명시.

---

## 9. 빌드 순서

0. **게이트: Phase 4 비교를 먼저 끝내고 동결.** v0의 존재 이유는 bundled vs split vs 손을 **같은 파이프라인 버전**에서
   1회 비교하는 것(readme §0). slow-loop이 gen/rubric을 바꾸기 시작하면 비교군 버전이 어긋나 결론이 오염된다.
   → **Phase 4 완주·로그 동결 후에 slow-loop을 배선한다.** 이후 변경은 항상 버전 스코프로만 비교(§6 version_deltas).
1. `analyze_runs.py` — (mode,rubric_name)별 신호 집계 + `analysis.schema.json`. problem.md를 앵커 신호로 포함. 미검토 = `.reviewed` 없는 run.
2. proposer 4단 + `proposal:v1` rubric + `propose_*` 프롬프트(§4 rubric 가드 반영).
3. `problem.md` 생성(빈 항목) + SKILL.md에 캡처 스텝(§5-A)과 트리거 질문 스텝(§5-B, 미검토 ≥ 5) 배선.
4. 버저닝 + 단일 CHANGELOG. analyze가 분석한 run에 `.reviewed` 마커 기록.
5. 고정점 원칙 명문화: slow loop 자신의 `proposal:v1`·`propose_*`는 사람만 변경.
