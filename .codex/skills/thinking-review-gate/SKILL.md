---
name: thinking-review-gate
description: "질문, 아이디어, 판단, 설계안, 구현 방향을 프로젝트 문맥에 맞춰 구체화하고 사용자가 검증 가능한 답변으로 발전시키는 workflow. 모든 적용 답변은 Level 1 inline verifier review를 거치며, 결정적 승격 조건에 걸리면 Level 2 file hand-off eval/gate로 올린다. 단순 개념 설명, 짧은 번역, 단순 요약, 낮은 위험의 일반 대화, 빠른 초안은 Level 1로만 처리한다."
---

# Thinking Review Gate

자연스러운 협업을 통해 질문, 아이디어, 판단, 설계안, 구현 방향을 사용자가 검증 가능한 답변으로 발전시키세요.

먼저 현재 프로젝트의 맥락을 파악한 다음, 필요한 질문을 하나씩 던지면서 무엇을 판단해야 하는지 구체화하세요. 답변 방향을 이해했다면 주요 claim, 근거, 가정, 불확실성, 대안, trade-off를 정리해 사용자가 따라가며 검토할 수 있는 형태로 제시하세요.

설계나 구현 판단이 포함된 경우, 사용자가 디자인이나 접근 방향을 승인하기 전까지 코드를 작성하거나, 프로젝트 뼈대를 만들거나, 구현 작업을 진행하지 마세요.

## 왜 사용하는가

답변은 사실, 추정, 의견, 선택이 섞여도 그럴듯하게 보일 수 있다. 특히 근거 없는 핵심 claim, 숨은 가정, 단일 선택지, 최신성 누락은 사용자가 답변을 검증하기 어렵게 만든다.

이 skill은 정답을 보장하려는 장치가 아니다. 프로젝트 문맥을 먼저 확인하고, 주요 claim을 나누고, 각 claim에 근거, 불확실성, 대안을 연결해서 사용자가 따라가며 검토할 수 있는 답변을 만든다.

## 기본 원칙

- 답변 전에 프로젝트 문맥을 먼저 확인한다.
- 확인 가능한 사실, 추정, 선택, 의견을 구분한다.
- 필요한 질문은 한 번에 하나씩 한다.
- 설계, 추천, 판단 답변에서는 2-3개 접근안과 trade-off를 비교한다.
- 주요 claim마다 근거, 불확실성, 대안을 점검한다.
- 설계나 구현으로 이어지는 작업이라면 먼저 간단한 디자인 또는 접근 방향을 제시하고 사용자 승인을 받는다.
- 검토는 하나의 skill 안에서 Level 1 review와 Level 2 eval/gate로 나누어 수행한다.

## 안티 패턴

"이건 너무 간단해서 검토가 필요 없다"라고 판단하지 마세요.

이 skill이 적용된 작업에서는 작은 기능, 설정 변경, 짧은 설계 판단도 간단한 내부 검토를 거칩니다. 검토는 짧아도 괜찮지만, 확인되지 않은 가정, 숨은 의존성, 놓친 대안을 먼저 확인하세요.

## 체크리스트

다음 항목을 순서대로 완료한다. 간단한 작업이면 각 항목을 짧게 처리하고, 복잡한 작업이면 별도 작업으로 나누어 진행한다.

1. 프로젝트 맥락 확인 — 관련 파일, 문서, 최근 run/report, 최근 커밋/변경, 기존 규칙을 확인한다.
2. 범위 분해 — 너무 크거나 여러 독립 주제가 섞여 있으면 하위 문제로 나누고 첫 하위 문제부터 다룬다.
3. 성공 기준 확인 — 목적, 제약 조건, 성공 기준, 검증 기준을 파악한다.
4. 질문하기 — 명확화 질문이 필요하면 한 번에 하나만 묻는다.
5. 시각 자료 사용 — 질문이나 설계가 그림으로 더 명확해지는 시점에만 제안하고, 미리 만들지 않는다.
6. 접근안 비교 — 가능한 접근 방식을 2-3개 제시하고, 각 방식의 장단점과 추천 이유를 설명한다.
7. 답변/설계 제시 — 답변 또는 설계를 섹션으로 나누어 제시한다. 긴 설계 토론에서는 큰 섹션이 끝날 때마다 방향이 맞는지 확인한다.
8. 검증 가능성 검토 — 주요 claim의 근거, 가정, 불확실성, 대안을 확인한다. 기본은 Level 1 inline review로 시작하고, 결정적 승격 조건에 걸리면 Level 2 file hand-off eval/gate로 올린다.
9. 검토 결과 반영 — Level 1에서는 내부 critique를 반영하고, Level 2에서는 artifact와 runner 결과를 기준으로 gate를 판단한다. 문제가 있으면 수정 후 다시 검토하되, 재작성 루프는 최대 2회까지만 반복한다.
10. 문서화 여부 확인 — 문서 산출물이 필요한 작업이면 Markdown 설계 문서를 작성하고 저장 위치를 사용자에게 명확히 알린다.
11. 실행 전 승인 — 실행 단계로 넘어가야 하면 먼저 사용자 승인을 받고, 실행 계획을 세운 뒤 진행한다.

## 과정

### 컨텍스트 확인

사용자에게 자세히 묻기 전에 프로젝트 안에서 확인 가능한 정보를 먼저 본다. 관련 파일, 문서, 최근 run/report, 최근 커밋/변경, 기존 규칙을 확인하고, 이미 답할 수 있는 것과 사용자에게 물어봐야 하는 것을 분리한다.

### 범위 판정

요청이 여러 독립 주제를 한 번에 다루면 바로 하위 문제로 나눈다. 하위 문제는 서로의 관계, 의존 순서, 독립적으로 검토 가능한 단위를 기준으로 나눈다. 전체를 한 번에 설계하려 하지 말고, 먼저 다룰 하위 문제를 정한다.

### 질문 정리

질문은 사용자가 결정해야 하는 것에만 사용한다. 프로젝트 문맥에서 확인 가능한 내용은 질문하지 않는다. 질문이 필요하면 한 번에 하나만 묻고, 목적, 제약 조건, 성공 기준, 검증 기준을 명확히 하는 데 집중한다.

### 접근안 비교

가능한 접근을 2-3개로 나눈다. 추천안을 먼저 제시하고, 왜 그 접근이 현재 문맥에 맞는지 설명한다. 각 접근의 장점, 단점, 필요한 근거, 실패 가능성을 함께 비교한다. 단일 선택지를 유일한 답처럼 제시하지 않는다.

### 답변/설계 제시

답변이나 설계는 복잡도에 맞게 섹션을 나눈다. 단순하면 몇 문장으로 끝내고, 판단이 복잡하면 아키텍처, 구성 요소, 데이터 흐름, 오류 처리, 테스트처럼 사용자가 검토할 수 있는 단위로 나눈다. 긴 설계 토론에서는 큰 섹션이 끝날 때마다 방향이 맞는지 확인한다.

### 검증 가능한 단위로 분리

답변을 사용자가 따라가며 확인할 수 있는 단위로 나눈다. 각 단위에 대해 무엇을 말하는지, 무엇에 의존하는지, 어떻게 확인할 수 있는지 드러낸다. 내부 추론을 믿으라고 요구하지 말고, 근거 링크, 파일 위치, 실행 결과, 확인 절차를 제공한다.

### 기존 프로젝트에서 작업하기

변경이나 판단을 제안하기 전에 기존 구조와 관례를 확인한다. 기존 문제를 언급할 때는 현재 목표에 영향을 주는 문제만 다룬다. 관련 없는 리팩터링, 큰 구조 변경, 불필요한 자동화 제안을 피한다.

## 생각 구조화

답변을 쓰기 전에 내부적으로 다음을 짧게 정리한다.

- `decision`: 사용자가 실제로 결정하려는 것
- `context`: 프로젝트 안에서 확인한 사실
- `claims`: 답변에 들어갈 주요 claim
- `evidence`: claim에 연결할 근거 또는 확인 방법
- `uncertainty`: 모르는 것, 변할 수 있는 것, 확인이 필요한 것
- `alternatives`: 가능한 다른 접근과 trade-off

## 검토 Level 선택

레벨을 나누는 이유는 답변의 신속성, 즉 생산성과 답변 품질 사이의 trade-off를 조절하기 위해서다. 모든 답변을 무거운 검토 흐름으로 보내면 응답 속도가 떨어진다. 반대로 검토를 아예 하지 않으면 근거 없는 단정, 숨은 가정, 잘못된 방향 제안 때문에 재질문과 재작업이 늘어 생산성이 낮아질 수 있다.

따라서 이 skill이 적용된 모든 답변에는 최소한의 Level 1 inline verifier review를 적용한다. 검토 비용이 답변 가치보다 커지지 않도록, 아래 결정 규칙에 걸릴 때만 Level 2 file hand-off eval/gate로 올린다.

레벨 선택은 결정적 규칙으로 수행한다. 단, 각 레벨 안에서 claim의 타당성, 근거 충분성, 대안의 품질을 평가하는 일은 여전히 비결정적일 수 있다. 결정적으로 고정하는 것은 "어떤 검토 흐름을 실행할 것인가"이다.

아래 표는 level을 결정하기 위한 요약이다. 실제 수행 절차는 각 level의 상세 흐름을 따른다.

| Level | 레벨 분류 방법 | 요약 흐름 |
| --- | --- | --- |
| Level 1: inline verifier review | Level 2 승격 조건에 걸리지 않는 기본 답변. 빠른 설명, 짧은 판단, 낮은 위험의 방향성 답변. | 문맥 확인 -> 초안 -> inline critique -> 수정 -> 최종 답변 |
| Level 2: file hand-off eval/gate review | 결정적 승격 조건 중 하나 이상에 걸리는 답변. 재현 가능한 기록, 점수화, schema validation, pass/fail gate가 필요한 경우. | run 생성 -> artifact 작성 -> validation -> artifact 재확인 -> 최종 답변 또는 fail 보고 |

분류 규칙은 다음 순서로 적용한다.

```text
1. Level 2 승격 조건 중 하나라도 참이면 Level 2를 선택한다.
2. Level 2 승격 조건이 모두 거짓이면 Level 1을 선택한다.
```

### Level 2 승격 조건

다음 조건 중 하나라도 참이면 Level 2 file hand-off eval/gate로 올린다. 판정은 답변을 작성한 뒤가 아니라, 사용자 질문과 프로젝트에서 확인 가능한 문맥만 보고 먼저 수행한다.

- 질문이 명시적으로 review case, file hand-off, eval, gate, runner, 재현 가능한 기록, schema validation, CI/hook 후보 검증, artifacted review를 요청한다.
- 질문이 `AGENTS.md`, `PROBLEM.md`, skill, hook, rule, rubric, verifier prompt 같은 작업 규칙의 생성, 수정, 삭제, 적용 기준 변경을 요구한다.
- 질문이 삭제, 배포, secret/credential, 보안, 권한, 비용/과금, 데이터 손실, 마이그레이션, 되돌리기 어려운 변경 중 하나를 실행하거나 승인할지 묻는다.
- 질문이 최신성, 외부 정책, 가격, 제품 스펙, 법률, API 또는 라이브러리 변경 가능성에 의존하는 판단을 요구한다.
- 질문이 "문제 없나", "이 구조가 낫나", "이 API가 이렇게 동작하나", "이 방법을 쓰면 되나"처럼 안전성, 적합성, 동작 보장, 채택 여부를 판단해 달라고 묻는다.
- 질문이 설계, 추천, 의사결정, 구현 방향, 실행 계획 중 하나를 선택하거나 비교해 달라고 요청하고, 선택 결과가 이후 작업 방향이나 비용을 크게 바꿀 수 있다.
- 질문 안에 서로 독립적인 하위 질문이나 claim이 3개 이상 포함되어 있고, 그중 하나의 오류가 결론 전체를 바꿀 수 있다.
- 질문이 `PROBLEM.md`에 기록된 열린 문제, 최근 실패 패턴, 최근 피드백과 같은 주제를 다시 다룬다.
- 질문을 답하려면 프로젝트 파일, 실행 결과, 공식 문서, 웹 검색 중 하나 이상의 외부 evidence anchor가 필요한데, 질문 자체에는 그 근거가 제공되어 있지 않다.

### Level 1: inline verifier review

현재 세션에서 짧게 점검하고 바로 답변한다.

```text
문맥 확인
-> 답변 초안 작성
-> prompts/verifier.md 기준으로 inline critique 수행
-> 문제 지점 / 확인 필요 / 수정 제안 반영
-> 최종 답변
```

- main agent가 `prompts/verifier.md`의 기준을 현재 세션 안에서 직접 적용한다.
- critique 결과는 내부 수정에만 사용하고 길게 출력하지 않는다.

### Level 2: file hand-off eval/gate review

file hand-off가 필요할 때만 사용한다.

Artifact:

```text
runs/{run_id}/
  manifest.json
  input.md
  attempts/
    1/
      draft.md
      critique.md
      eval.json
      validation.json
    2/
      draft.md
      critique.md
      eval.json
      validation.json
```

역할:

- `input.md`: 사용자 원문 요청, 확인한 프로젝트 문맥, 근거, 제약을 포함하는 run 단위 불변 입력
- `attempts/{n}/draft.md`: 해당 attempt에서 실제로 검토할 답변 초안
- `attempts/{n}/critique.md`: 해당 draft 수정을 위한 문제 지점과 수정 제안
- `attempts/{n}/eval.json`: 해당 draft의 축별 점수와 점수 이유만 포함
- `attempts/{n}/validation.json`: `validate.py`가 계산한 총점과 `gate_result`

`input.md`의 `Original User Input`에는 반드시 사용자의 실제 입력 원문을 그대로 적는다. 요약, 재해석, `-`, TODO, 빈 값으로 대체하지 않는다. 사용자의 원문을 확인할 수 없으면 Level 2 run을 만들지 말고 먼저 원문을 확보한다.

prompt 파일은 run artifact로 저장하지 않는다. critique/eval prompt는 `prompts/level2-*.system.md` 템플릿과 현재 run/attempt 경로를 기준으로 runner가 실행 시점에 구성한다.

절차:

```text
attempt 1:
  scripts/critique.py init
  -> scripts/runner.py <run_dir>
  -> 저장된 attempts/1/validation.json 읽기
  -> attempts/1/validation.json.gate_result 확인

if gate_result=pass:
  -> attempts/1/critique.md의 남은 주의점 반영
  -> 최종 답변

if gate_result=fail:
  -> attempts/1/critique.md와 attempts/1/validation.json.weak_axes를 반영해 revised draft 작성
  -> scripts/critique.py attempt <run_dir>으로 attempts/2/draft.md 생성
  -> scripts/runner.py <run_dir> --attempt 2 실행
  -> attempts/2/validation.json.gate_result 다시 확인

if attempt 2도 gate_result=fail:
  -> 한 번 더 revised draft 작성
  -> scripts/critique.py attempt <run_dir>으로 attempts/3/draft.md 생성
  -> scripts/runner.py <run_dir> --attempt 3 실행
  -> attempts/3/validation.json.gate_result 다시 확인

if attempt 3도 gate_result=fail:
  -> 더 이상 자체 수정하지 않고
  -> 남은 실패 축, 수정 한계, 필요한 사용자 결정을 보고
```

실행 명령:

```text
python .codex/skills/thinking-review-gate/scripts/critique.py init --run-id my-review --input-file path/to/input.md --context-file path/to/context.md --draft-file path/to/draft.md
python .codex/skills/thinking-review-gate/scripts/runner.py .codex/skills/thinking-review-gate/runs/my-review
python .codex/skills/thinking-review-gate/scripts/critique.py attempt .codex/skills/thinking-review-gate/runs/my-review --draft-file path/to/revised-draft.md
python .codex/skills/thinking-review-gate/scripts/runner.py .codex/skills/thinking-review-gate/runs/my-review --attempt 2
```

`--input-file`에는 사용자의 원문 요청을 그대로 담은 UTF-8 파일을 넣는다. 한글 등 다국어 원문은 PowerShell stdin 인코딩에 따라 깨질 수 있으므로 `--input-file` 또는 직접 `--input-text "원문"`을 우선한다. stdin을 사용할 때는 `--input-text -`를 쓰며, 이 경우 stdin 전체가 `Original User Input`으로 저장되어야 한다.

기본 최대 attempt 수는 3회다. 특별히 더 많은 재검토가 필요한 실험 run에서는 `critique.py attempt <run_dir> --max-attempts 5`처럼 명시적으로 상한을 올린다.

validation만 다시 실행할 때:

```text
python .codex/skills/thinking-review-gate/scripts/critique.py validate .codex/skills/thinking-review-gate/runs/my-review --attempt 1
```

규칙:

- `runner.py`는 critique와 eval을 병렬 실행하고, 각 완료 시 console output에 완료를 표시한다.
- eval은 `schemas/level2-eval.schema.json`으로 JSON 구조를 고정한다.
- main agent는 CLI stdout이 아니라 저장된 artifact 파일을 읽는다.
- Level 2의 gate 판단은 `runner.py`의 exit code나 console output이 아니라 반드시 해당 attempt의 `validation.json.gate_result`를 기준으로 한다.
- `schema_valid=true`는 eval JSON 형식이 유효하다는 뜻이고, `gate_result=pass|fail`은 rubric 기반 품질 gate 통과 여부를 뜻한다.
- `input.md`를 만들 때 `Original User Input`이 사용자 원문 그대로 들어갔는지 확인한다. 원문이 누락되면 artifact 전체의 근거가 약해지므로 run 생성을 중단하고 원문을 먼저 확보한다.
- 최초 run 1회와 재검토 최대 2회를 합쳐 총 attempt는 기본 최대 3회다. CLI는 `DEFAULT_MAX_ATTEMPTS=3`을 기본값으로 사용하며, 실험 목적일 때만 `--max-attempts`로 명시적으로 조정한다.
- 마지막 통과 attempt의 `draft.md`가 최종 응답의 기준본이다. 최종 응답의 결론, 근거, 조건, 추천이 마지막 통과 draft와 의미상 달라지면 새 attempt로 다시 검토한다.
- 별도의 `final.md`는 만들지 않는다. 실제 최종 응답은 사용자에게 보내는 assistant message이며, artifact에는 그 기준이 되는 마지막 통과 draft와 검토 결과만 남긴다.
- critique/eval agent는 지정 artifact 외 파일을 수정하지 않는다.
- 삭제, 이동, 이름 변경, `git reset`, `git checkout`, `git clean`, package install, dependency update를 하지 않는다.
- secret, credential, token, private key, 환경 변수 덤프를 읽거나 출력하지 않는다.

오래된 run 정리는 기본 dry-run이다. 실제 삭제는 사용자가 명시적으로 요청한 경우에만 `--confirm-delete`를 사용한다.

```text
python .codex/skills/thinking-review-gate/scripts/critique.py cleanup --older-than-days 7
python .codex/skills/thinking-review-gate/scripts/critique.py cleanup --older-than-days 7 --confirm-delete
```

## 검토 후 처리

검토 후 처리는 level에 따라 다르게 적용한다.

- Level 1: inline critique 결과의 문제 지점, 확인 필요, 수정 제안을 반영한 뒤 최종 답변으로 정리한다.
- Level 2: 해당 attempt의 `critique.md`, `eval.json`, `validation.json`, runner 결과를 확인하되, pass/fail gate는 `validation.json.gate_result`만 기준으로 판단한다.

문제가 발견되면 수정 후 다시 검토하되, 최초 run 이후 재검토 루프는 최대 2회까지만 반복한다. 사용자 선택이나 추가 정보가 필요하면 답변을 단정하지 말고 질문으로 돌린다.

2회 수정 후에도 통과하기 어렵다면 더 이상 혼자 고치려 하지 말고, 남은 실패 지점과 필요한 결정을 사용자에게 보여준다. 설계나 구현으로 이어지는 작업에서 gate 실패가 나오면, 수정된 접근 방향을 제시하고 승인 여부를 확인한다.

## critique 검토 기준

Level 1 inline critique와 Level 2 critique artifact는 다음을 본다.

- 주요 claim이 무엇인가
- claim에 연결된 evidence anchor가 있는가
- evidence anchor를 사용자가 따라갈 수 있는가
- 근거 없는 claim이 있는가
- 답변 내부 또는 근거와 모순되는 내용이 있는가
- 숨은 가정이 있는가
- 불확실성, 한계, 최신성 확인 필요가 표시되었는가
- 설계/추천/선택 답변에서 대안과 trade-off가 제시되었는가

## 최종 응답

- 사용자가 바로 쓸 수 있는 결론을 먼저 쓴다.
- 근거, 확인 방법, 불확실성을 짧게 붙인다.
- 검토가 `pass`여도 남은 문제, 약한 근거, 확인 필요 지점이 있으면 짧게 알린다.
- 검토가 `fail`이면 실패 지점과 필요한 사용자 결정을 먼저 알린다.
- high-risk일 때만 짧은 검토 요약을 포함한다.
- critique artifact는 최종 답변을 대신 쓰지 않고 검토 결과만 제공한다.
- 근거 없는 claim은 근거 있음으로 취급하지 않는다.
