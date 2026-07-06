---
name: generate-test
description: 자연어 도메인 정책/요구사항을 실행 가능한 테스트(Gherkin 인수 계약 또는 JUnit 단위 테스트)로 고정하는 하네스. 정책만 받아(테스트 케이스는 Gen이 생성) intake로 정책 공백을 메우고, mode(contract/unit/bundled)로 파이프라인을 돌린다. "이 정책/요구사항으로 테스트 만들어줘", "gherkin 계약 생성", "단위 테스트 생성해줘", split(계약 먼저 동결 후 단위) 요청 시 사용.
---

# Generate Test

## Overview

도메인 정책을 짧은 intake 대화와 4단 하네스(Gen→Critique→Eval→Refine, Validate 게이트)로 **테스트**로 바꾼다.
**input에는 정책만 담는다 — 테스트 케이스는 Gen이 생성한다**(설계 §0-A). 규칙과 역할은 `CLAUDE.md` 참조.

두 토폴로지를 비교한다:
- **split**(권장): 계약(contract) run을 먼저 통과·**동결**한 뒤, 단위(unit) run이 동결 계약을 제약으로 받는다.
- **bundled**: 계약+단위를 한 draft로 생성(비교 대상).

## Workflow

1. 사용자의 정책/요구사항 재료를 대화나 UTF-8 파일에서 읽는다.
2. **intake — 정책 공백만 메운다.** 판정기준(테스트 케이스)을 묻지 말고, 정책의 모호성만 질문한다:
   미정의 경계("딱 7일째는 무료인가 8일부터인가?"), 규칙 우선순위, 외부 의존 범위. 추론이 되면 기본값으로 제안.
3. `pipeline/intake_to_input.py`로 input JSON을 생성한다(정책만).
4. 토폴로지 선택: 기본 **split**. (사용자가 bundled 비교를 원하면 bundled도.)
5. **split 실행 (동결 게이트는 이 skill이 소유):**
   한 split 실험의 contract·unit은 **하나의 그룹 폴더**로 묶는다. 그룹 id는 contract run_id(=`<오늘>_<contract_hash>`).
   두 run 모두 `--runs-dir`로 그룹 안의 스트림 하위폴더를 명시한다 — 안 그러면 `runs/split/` 바로 밑에 흩어진다.
   1. `run_draft.py --mode contract --runs-dir runs/split/<group>/contract` 실행.
      (그룹 id는 contract input 파일명의 `<hash>`로 정해진다: `<오늘>_<contract_hash>`.)
   2. PASS면 `runs/split/<group>/contract/<run_id>/<hash>_final.json`의 `content`(gherkin 계약)를 **사용자에게 제시**한다.
   3. **y/n 승인을 묻는다.** 자동 PASS만으로 동결하지 않는다 — 사람이 계약을 소유한다.
   4. 승인 시: **동결 = 제자리 고정(freeze-in-place)** — 별도 폴더로 복사하지 않는다. contract 아티팩트
      `runs/split/<group>/contract/<run_id>/artifact/<name>.feature`를 **읽기전용(chmod a-w)**으로 잠근다.
      그 파일을 `--frozen-contract-file`로 가리켜 원문을 `frozen_contract`로 실은 **unit input**을 새로 생성한다
      (`intake_to_input.py --frozen-contract-file runs/split/<group>/contract/<run_id>/artifact/<name>.feature`).
   5. `run_draft.py --mode unit --runs-dir runs/split/<group>/unit` 실행 (**같은 group**).
6. **bundled 실행:** `run_draft.py --mode bundled` (같은 정책 input으로).
7. final draft 경로(또는 실패 아티팩트 경로)와 수렴 iteration 수를 보고한다.
8. **사용자 피드백 캡처 (problem.md).** final을 제시할 때 "누락된 케이스나 어색한 단언이 있으면 알려주세요"로
   가볍게 연다. 사용자가 생성 테스트에 반응하면(누락 지적/좋다/이건 틀렸다) `problem.md`에 한 줄 추가한다:
   `- (YYYY-MM-DD, run_id, verdict=pos|neg) 사용자 반응 요약 → 도출한 교훈`.
   긍정도 회귀 방지 신호이므로 기록한다. 매 run 강제 질문은 하지 않는다 — 사용자가 자발적으로 반응할 때만 캡처.

> **산출물 승격(materialize):** Gen/Refine은 **파일 매니페스트**(`files: [{path, content}]`)를 내고,
> PASS 시 runner가 각 파일을 `<runs-dir>/<run_id>/artifact/<path>`로 떨군다 (split이면
> `runs/split/<group>/{contract,unit}/<run_id>/artifact/<path>`) — unit은 대상 클래스마다
> 별도 `.java`, contract는 `.feature`, bundled는 `.feature`+여러 `.java`. final.json의 `content`는
> 매니페스트를 헤더로 이어붙인 **텍스트 뷰**(critique/eval/guard용)일 뿐, 실제로 쓰는 건 artifact 파일이다.
> 동결은 이 contract artifact를 사람이 승인해 **제자리에서 읽기전용으로 잠그는** 단계다(복사 없음).

## Intake Rules — 정책만, 케이스는 안 묻는다

추론 우선. 정책이 강하게 함의하면 열린 질문 대신 기본값으로 제안한다.

- **묻는 것(정책 공백):** 규칙 간 우선순위, 경계의 정확한 포함/배제, 외부 의존 목록, 절사·반올림 같은 산술 규칙.
- **묻지 않는 것:** "경계값 테스트 뭐뭐 넣을까요?" 류 — **그건 Gen이 발굴한다.** intake가 케이스를 열거하면 Gen의 역할을 뭉갠다.

좋은 intake 메시지 예:

```text
정책을 읽어보니 환불 금액 산출 규칙이네요. 한 가지 모호한 지점만 확인할게요.
- "7일 이하 무료"에서 딱 7일째는 무료인가요, 8일부터 유료인가요?
- MANUAL 지정과 7일 무료가 충돌하면 무엇이 우선인가요?
나머지(경계값·실패 케이스)는 파이프라인이 정책에서 발굴합니다.
```

## Input Contract

`pipeline/schemas/input.schema.json`이 정본. 전체 JSON 형태를 여기 복제하지 않는다. 핵심:

- `brief.requirement`(필수) — 테스트로 고정할 정책.
- `brief.source_material`(필수) — 설계/스펙 **원문**. 요약·발췌·생략 금지. 길어도 원문 그대로. 넣기 어려우면 멈추고 사용자에게 분할/참조 방법을 묻는다.
- `brief.policy_rules`(선택) — 정책을 **규칙**으로 분해(케이스 아님).
- `brief.external_dependencies`(선택) — mock 판정 근거.
- `brief.frozen_contract`(선택) — split-unit 전용. 동결된 gherkin 계약 원문.
- 생성한 input은 실행 전 `intake_to_input.py`(내부에서 validate) 또는 `validate.py --artifact input`으로 검증.

## 동결 게이트 (split) — 이 skill의 책임

runner는 "한 mode 실행"만 하는 기계다. **두 run 오케스트레이션·사람 y/n 승인·동결(제자리 잠금)·frozen 주입은 이 skill이 한다.**

- **동결 = 제자리 고정(freeze-in-place)**: 별도 `frozen/` 폴더로 복사하지 않는다. contract 아티팩트를
  그 자리(`runs/split/<group>/contract/<run_id>/artifact/`)에서 읽기전용(chmod a-w)으로 잠근다. 이후 수정 금지.
  unit run은 그 원문을 입력(`frozen_contract`)으로만 받는다. (원문은 unit input JSON에 인라인되므로 unit run이
  소비하는 실물은 그 인라인본이고, 잠긴 아티팩트는 사람이 소유하는 정본·감사 기준이다.)
- **사람 승인 게이트**: 자동 PASS만으로 동결하지 않는다. 계약을 사용자에게 보여주고 y/n을 받는다.
- **unit 제약 주입**: `frozen_contract`를 unit input에 실으면 gen_unit이 "계약과 모순 금지, 계약이 남긴 산술 세부를 단위로 채워라"를 지킨다(프롬프트에 규칙 있음).

## Modes

| mode | 생성 | gen 프롬프트 | rubric | codex eval 스키마 | runs |
|---|---|---|---|---|---|
| contract | gherkin 계약 | gen_contract.md | contract.rubric.yaml | eval_output.contract.schema.json | runs/split/&lt;group&gt;/contract/ |
| unit | JUnit 단위(계약 제약) | gen_unit.md | unit.rubric.yaml | eval_output.unit.schema.json | runs/split/&lt;group&gt;/unit/ |
| bundled | 계약+단위 | gen_bundled.md | bundled.rubric.yaml | eval_output.bundled.schema.json | runs/bundled/ |

> bundled는 Phase 3 예정. 매핑은 `pipeline/runner.py`의 `MODE_*` 상수에 있다.

## Scripts

정책 input 생성(내부 validate):

```bash
python -B .codex/skills/generate-test/pipeline/intake_to_input.py \
  --title "환불 금액 산출 정책" \
  --requirement "..." \
  --source-material-file /path/to/design.md \
  --policy-rule "MANUAL > 7일무료 > PRORATION" \
  --external-dependency "PG" --external-dependency "DB" \
  --output-dir .codex/skills/generate-test/pipeline/inputs
```

unit input(동결 계약 주입 — 잠근 contract 아티팩트를 제자리에서 가리킨다):

```bash
python -B .codex/skills/generate-test/pipeline/intake_to_input.py \
  --requirement "..." --source-material-file /path/to/design.md \
  --frozen-contract-file .codex/skills/generate-test/runs/split/<group>/contract/<run_id>/artifact/refund.feature \
  --output-dir .codex/skills/generate-test/pipeline/inputs
```

파이프라인 실행(mode별):

```bash
python -B .codex/skills/generate-test/pipeline/run_draft.py \
  .codex/skills/generate-test/pipeline/inputs/<hash>_input.json \
  --mode contract          # contract | unit | bundled
  # split이면 그룹 하위폴더를 명시: --runs-dir runs/split/<group>/contract  (unit은 .../unit)
```

`run_draft.py`는 `--runs-dir` 미지정 시 mode에 따라 `runs/{split,bundled}/`로 쓴다(흩어짐).
split은 위 5절대로 `--runs-dir runs/split/<group>/{contract,unit}`로 **한 그룹에 묶는다**.
파이프라인은 `pipeline/` 아래에 있다. 사용자가 명시적으로 요청하지 않으면 저장소 레벨 원본 파이프라인은 건드리지 않는다.
