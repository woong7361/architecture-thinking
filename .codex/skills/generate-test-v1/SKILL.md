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
4. **boundary feature 생성**: `run_draft.py --mode contract`. 런은 여러 iter를 도느라 오래 걸린다 —
   **긴 런을 시작하기 전에 사용자에게 "런 도중 떠오른 제약·요청을 추가할 수 있다"고 안내하고**, 원하면
   `--inbox <경로>`를 붙여 실행한다(→ [§제약 인박스](#제약-인박스--런-도중-요청제약-추가-선택)).
5. **feature를 rules에 주입**: contract가 낸 feature(`runs/<group>/feature/<run_id>/artifact/<domain>.feature`)를
   `intake_to_input.py --boundary-feature-file ... --group <feature_group>`로 rules input에 실어 **새 input**을
   만든다. `--group`으로 feature와 같은 그룹에 묶는다. feature가 미룬 산식을 rules가 채우게 하는 링크다
   (사람 검토 후 주입 권장 — feature가 확정돼야 rules가 그 산식을 채운다).
6. **도메인 규칙 예시표 생성**: `run_draft.py --mode rules` (feature 주입된 input).
7. final 문서 경로(또는 실패 아티팩트 경로)와 수렴 iteration 수를 보고한다.
8. **사람 검토(명세를 사람이 소유).** feature의 `When=Inbound Port 1:1`·경계 관찰 단언, 규칙표의 **값 검산**을
   사람이 확인한다. step/fake/단위 구현은 이 다음 사이클에서 사람이 바인딩한다.
9. **피드백 캡처 (problem.md).** 누락 케이스·틀린 값·어색한 명세에 대한 사용자 반응을 한 줄로 남긴다.

## Modes

| mode | 산출 문서 | gen 프롬프트 | rubric | codex eval 스키마 | runs |
|---|---|---|---|---|---|
| contract | `<domain>.feature` | gen_contract.md | contract.rubric.yaml | eval_output.contract.schema.json | runs/`<group>`/feature/ |
| rules | `<domain>.rules.md` | gen_rules.md | rules.rubric.yaml | eval_output.rules.schema.json | runs/`<group>`/rules/ |

`<group>`은 input의 `group` 필드(미지정 시 `brief_hash`). feature와 그 파생 rules가 같은 group을 가져
`runs/<group>/{feature,rules}/`로 한 폴더에 모인다. 실행 위치가 input에서 결정되므로 각 모드는 **플래그 없이
독립 실행**된다(rules를 단독으로 돌려도 제 그룹으로 들어간다). rules input의 group은 intake `--group`으로
원본 feature의 group(=feature `brief_hash`)을 물려받는다.

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

# 2) 나온 feature를 rules input에 주입 (새 input 생성; --group으로 feature와 한 그룹)
python -B .codex/skills/generate-test-v1/pipeline/intake_to_input.py \
  --requirement "..." --source-material-file /path/to/design.md \
  --boundary-feature-file .codex/skills/generate-test-v1/runs/<group>/feature/<run_id>/artifact/<domain>.feature \
  --group <feature_group> \
  --output-dir .codex/skills/generate-test-v1/pipeline/inputs

# 3) 규칙 예시표 생성 (feature 주입된 input)
python -B .codex/skills/generate-test-v1/pipeline/run_draft.py inputs/<rules_hash>_input.json --mode rules
```

`run_draft.py`는 `--runs-dir` 미지정 시 input의 `group`과 mode에 따라 `runs/<group>/{feature,rules}/`로 쓴다.

## 제약 인박스 — 런 도중 요청/제약 추가 (선택)

한 런은 gen→critique→eval→refine를 여러 iter 반복해 **오래 걸린다.** 그동안 사람이 새 요구·제약이 떠오르면,
런을 멈추거나 재시작하지 않고 **인박스 파일에 한 줄씩 append**해서 다음 iter부터 반영할 수 있다. 이 스킬을
운영할 때는 **긴 런을 시작하며 사용자에게 이 사용법을 먼저 안내**해, 도중에 떠오른 요청을 흘리지 않게 한다.

- **사용**: `run_draft.py <input> --mode contract --inbox <경로>` — 인박스 파일 경로를 지정한다(미지정 시 비활성).
  파일은 미리 없어도 되며, 필요할 때 만든다.
- **추가**: 런이 도는 동안 그 파일에 제약을 한 줄씩 적는다. 예:
  `echo "환불이 거절되면 그 사유를 관찰 가능한 도메인 결과로 남긴다" >> <경로>`
- **반영**: runner가 **각 iter 맨 위에서 한 번 drain**해 `input.brief.constraints.must_include`로 **멱등 병합**한다
  (중복 줄 무시). 스테이지(critique/eval/refine)는 인박스를 **직접 읽지 않고** 이 확정본만 본다.

**배리어 규칙(왜 안전한가):** iter **도중** 도착한 제약은 **그 iter를 못 보고 다음 iter 맨 위에서만** 반영된다.
그래서 한 iter 안에서 critique·eval·refine이 서로 다른 제약 집합을 보는 일이 없다(iter별 제약 집합 불변).
인박스가 비어 있으면 무비용 통과 — **"생각나면 넣고, 없으면 그냥 흐른다."** 실측: iter2 도중 넣은 제약은
iter1·2 드레인엔 `active=0`, iter3 드레인에서 처음 `added`로 잡혀 흡수된 뒤 PASS.

**무엇을 넣나 = "무엇을 지켜야 하나"(제약·요구)까지.** Gherkin 시나리오 문장을 직접 써 넣지 마라 — 그건
정답 주입이라 순환성(정보 차단, `CLAUDE.md` §4)을 깬다. 제약은 coverage 축이 열거·채점하고 critique가
사냥하고 refine이 고친다. 새 제약이 늦게 들어오면 `--max-iterations`를 넉넉히 줘 충족할 iter를 남긴다.

## v0와의 차이 (무엇을 뺐나)

- **테스트 코드 생성 제거**: `unit`·`bundled` 모드, 계약 동결(split)·frozen 게이트, step/fake 생성 없음. 산출은 문서 2종.
- **메커니즘 검증·사후 특성화**(mock·호출 순서·예외 타입·quirk 박제)는 이 스킬 밖 — 별도 test-after 스킬 몫.
- 용어: "결정론적 게이트" → **"설계의 결정론적 명세"**.
- (v0 코드·로그는 원본 `generate-test/`에 보존.)
