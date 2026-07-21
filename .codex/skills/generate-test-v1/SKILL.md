---
name: generate-test-v1
description: (v1 실험 복사본) spec-first — 테스트 코드가 아니라 "명세 문서"를 생성한다. boundary feature(유스케이스 경계 Gherkin, When=Inbound Port 1:1) + 도메인 규칙 예시표. 메커니즘 검증·사후 특성화는 별도 스킬로 분리. 결정 근거는 docs/v1-spec-first-design.md.
---

# Generate Test v1 — spec-first (명세 문서 생성)

## Overview

도메인 정책을 짧은 intake와 4단 하네스(Gen→Critique→Eval→Refine, Validate 게이트)로 **명세 문서**로 바꾼다.
**테스트 "코드"(step/fake/단위 구현)는 만들지 않는다** — 그건 사람이 이후 사이클에서 바인딩한다.
결정 근거·trade-off: [docs/v1-spec-first-design.md](docs/v1-spec-first-design.md). 규칙과 역할은 `CLAUDE.md` 참조.

두 산출 문서(mode):
- **contract = boundary feature** — 유스케이스 경계의 Gherkin. `When`이 **Inbound Port 하나에 1:1**,
  `Then`은 **경계에서 관찰 가능한 결과(반환·도메인 상태)만**(호출 순서·횟수·내부 상태·원시 기본값 단언 금지).
  계산 산식은 여기 넣지 않는다.
- **rules = 도메인 규칙 예시표** — 계산·상태 전이·불변식의 **입력→출력 표**. feature에서 뺀 산식을 담는다.
  기대 출력값은 산식에서 재계산으로 검증 가능해야 한다(환각 방지). 인터페이스(클래스·메서드)는 정하지 않는다.

> **왜 코드가 아니라 문서인가**: 사전 일괄 단위 생성은 구현을 잠그고(lock-in), step/fake는 인터페이스 설계를
> 미리 못박는다. feature는 시그니처가 아니라 경계 행동만 고정해 인터페이스 설계·그 오류 신호를 사람의 실제
> 사이클로 미룬다. "결정론적 게이트"가 아니라 **"설계의 결정론적 명세"**(실행 게이트는 사람이 step 바인딩 시 완성).

## Workflow

1. 정책/요구사항 재료를 대화나 UTF-8 파일에서 읽는다.
2. **intake — 정책 공백만 메운다.** 케이스는 묻지 않는다(케이스는 Gen이 발굴). 미정의 경계·규칙 우선순위·외부 의존만.
3. `pipeline/intake_to_input.py`로 input JSON(정책만)을 생성한다.
4. **boundary feature 생성**: `run_draft.py --mode contract`.
5. **feature를 rules에 주입**: contract가 낸 feature(`runs/feature/<run_id>/artifact/<domain>.feature`)를
   `intake_to_input.py --boundary-feature-file ...`로 rules input에 실어 **새 input**을 만든다. feature가 미룬
   산식을 rules가 채우게 하는 링크다(사람 검토 후 주입 권장 — feature가 확정돼야 rules가 그 산식을 채운다).
6. **도메인 규칙 예시표 생성**: `run_draft.py --mode rules` (feature 주입된 input).
7. final 문서 경로(또는 실패 아티팩트 경로)와 수렴 iteration 수를 보고한다.
8. **사람 검토(명세를 사람이 소유).** feature의 `When=Inbound Port 1:1`·경계 관찰 단언, 규칙표의 **값 검산**을
   사람이 확인한다. step/fake/단위 구현은 이 다음 사이클에서 사람이 바인딩한다.
9. **피드백 캡처 (problem.md).** 누락 케이스·틀린 값·어색한 명세에 대한 사용자 반응을 한 줄로 남긴다.

## Modes

| mode | 산출 문서 | gen 프롬프트 | rubric | codex eval 스키마 | runs |
|---|---|---|---|---|---|
| contract | `<domain>.feature` | gen_contract.md | contract.rubric.yaml | eval_output.contract.schema.json | runs/feature/ |
| rules | `<domain>.rules.md` | gen_rules.md | rules.rubric.yaml | eval_output.rules.schema.json | runs/rules/ |

## Input Contract

`pipeline/schemas/input.schema.json`이 정본. 핵심: `brief.requirement`(필수) + `brief.source_material`(필수, 원문
보존) + 선택 `policy_rules`·`external_dependencies`·`constraints`. **테스트 케이스는 넣지 않는다** — Gen 산출물이다.
생성한 input은 `intake_to_input.py`(내부 validate) 또는 `validate.py --artifact input`으로 검증.

## Scripts

```bash
# 정책 input 생성(내부 validate)
python -B .codex/skills/generate-test-v1/pipeline/intake_to_input.py \
  --title "환불 금액 산출 정책" --requirement "..." \
  --source-material-file /path/to/design.md \
  --policy-rule "MANUAL > 7일무료 > PRORATION" \
  --output-dir .codex/skills/generate-test-v1/pipeline/inputs

# 1) boundary feature 생성
python -B .codex/skills/generate-test-v1/pipeline/run_draft.py inputs/<hash>_input.json --mode contract

# 2) 나온 feature를 rules input에 주입 (새 input 생성)
python -B .codex/skills/generate-test-v1/pipeline/intake_to_input.py \
  --requirement "..." --source-material-file /path/to/design.md \
  --boundary-feature-file .codex/skills/generate-test-v1/runs/feature/<run_id>/artifact/<domain>.feature \
  --output-dir .codex/skills/generate-test-v1/pipeline/inputs

# 3) 규칙 예시표 생성 (feature 주입된 input)
python -B .codex/skills/generate-test-v1/pipeline/run_draft.py inputs/<rules_hash>_input.json --mode rules
```

`run_draft.py`는 `--runs-dir` 미지정 시 mode에 따라 `runs/{feature,rules}/`로 쓴다.

## v0와의 차이 (무엇을 뺐나)

- **테스트 코드 생성 제거**: `unit`·`bundled` 모드, 계약 동결(split)·frozen 게이트, step/fake 생성 없음. 산출은 문서 2종.
- **메커니즘 검증·사후 특성화**(mock·호출 순서·예외 타입·quirk 박제)는 이 스킬 밖 — 별도 test-after 스킬 몫.
- 용어: "결정론적 게이트" → **"설계의 결정론적 명세"**.
- (v0 코드·로그는 원본 `generate-test/`에 보존.)
