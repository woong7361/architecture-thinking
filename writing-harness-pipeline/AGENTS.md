# AGENTS.md

이 문서는 `writing-harness-pipeline` 안에서 AI 에이전트가 작업할 때 따라야 하는 프로젝트 규칙이다.

이 프로젝트의 1차 산출물은 사람이 바로 다듬어 쓸 수 있는 **개선된 초안**이고, 2차 산출물은 그 초안이 만들어진 과정을 재현하고 개선할 수 있는 **반복 가능한 초안 생성 시스템**이다.

## 기본 원칙

- 구현 전 `readme.md`, `schema-contracts.md`, `schemas/*.schema.json`, `prompts/*_system.md`를 먼저 확인한다.
- 기존 파일 계약, 파일명 규칙, run 디렉토리 구조를 우선한다.
- 단계별 역할을 섞지 않는다.
- LLM stage 사이의 핸드오프는 파일과 JSON payload로만 한다.
- runner가 경로, 파일명, payload 구성을 제어한다.
- stage 코드나 프롬프트가 임의로 run 디렉토리 전체를 훑어 읽게 만들지 않는다.
- 검증 가능한 형태로 마무리한다. 코드 변경 후 가능한 경우 `python runner.py ...` 또는 `python validate.py ...` 계열 검증을 실행한다.

## 실행 명령어

명령어는 기본적으로 `writing-harness-pipeline` 디렉토리에서 실행한다.

```powershell
cd writing-harness-pipeline
```

입력 JSON만 검증할 때:

```powershell
python -B ./validate.py ./input.json --artifact input
```

MVP 파이프라인을 실행할 때:

```powershell
python -B ./runner.py ./input.json
```

같은 날짜와 `brief_hash`, iteration의 산출물을 의도적으로 다시 만들 때만 `--overwrite`를 붙인다.

```powershell
python -B ./runner.py ./input.json --overwrite
```

생성된 draft를 별도로 검증할 때:

```powershell
python -B ./validate.py ./runs/YYYY-MM-DD_{brief_hash}/iter_001/{brief_hash}_iter-001_draft.json --artifact draft --brief-hash {brief_hash} --iteration 001
```

Python 문법만 빠르게 확인할 때는 `__pycache__`가 생기지 않도록 `py_compile` 대신 `compile(...)` 기반 명령을 사용한다.

```powershell
python -B -c "from pathlib import Path; files=['runner.py','validate.py','stages/generator.py','stages/scripts/codex_client.py']; [compile(Path(f).read_text(encoding='utf-8'), f, 'exec') for f in files]; print('syntax ok')"
```

## 역할 경계

### Generator

- 입력: `{brief_hash}_input.json`
- 출력: `{brief_hash}_iter-{iteration}_draft.json`
- 책임: 주어진 brief에서 초안 본문을 만든다.
- 금지: 자기 평가, 자기 비평, 최종 판정, 점수 생성.

### Critique

- 입력: `{brief_hash}_input.json`, `{brief_hash}_iter-{iteration}_draft.json`
- 출력: `{brief_hash}_iter-{iteration}_critique.json`
- 책임: 독자가 약하게 느낄 지점, 보존할 강점, 다음 퇴고 방향을 제시한다.
- 금지: 점수표 생성, 초안 전체 재작성, 최종 판정.

### Evaluator

- 입력: `{brief_hash}_input.json`, `{brief_hash}_iter-{iteration}_draft.json`, rubric
- 출력: `{brief_hash}_iter-{iteration}_eval.json`
- 책임: 루브릭 기반 점수와 축별 근거를 낸다.
- 금지: critique를 읽고 채점하기, PASS/REJECT 최종 판정 생성, 글 재작성.

### Refiner

- 입력: `{brief_hash}_input.json`, 이전 draft, critique, refine request
- 출력: `{brief_hash}_iter-{next_iteration}_draft.json`
- 책임: 비평과 검증 오류를 반영해 다음 초안을 만든다.
- 금지: eval 총점 원문을 보고 점수 맞추기, generator 내부 히스토리 사용.

### Validator

- 입력: 검사 대상 JSON, 해당 schema, 기계적 계약, 필요한 rubric threshold
- 출력: PASS/REJECT/ERROR 성격의 검사 결과
- 책임: schema, `brief_hash`, 길이, 금칙어, 필수 조건, 점수 하한처럼 기계적으로 판정 가능한 항목만 검사한다.
- 금지: 문학적 품질 판단, 창작, 비평, 점수 근거 작성.

## 정보 차단 규칙

단계별로 허용된 입력만 payload로 전달한다.

| 단계 | 봐도 되는 파일 | 보면 안 되는 파일 |
| --- | --- | --- |
| Gen | `{brief_hash}_input.json` | critique, eval, refine request |
| Critique | input, current draft | eval, refine request |
| Eval | input, current draft, rubric | critique, generator 히스토리 |
| Validate | 검사 대상 JSON, schema, 기계적 계약 | LLM 대화 히스토리 |
| Refine | input, previous draft, critique, refine request | eval 총점 원문, generator 내부 히스토리 |

특히 Eval은 Critique에 anchor되지 않아야 한다. Refine에는 `weak_axes`, `contract_errors`, `critique_summary`, `revision_instructions`처럼 수정에 필요한 정보만 넘기고, `weighted_total` 같은 총점 원문은 넘기지 않는다.

## 프롬프트 규칙

- 역할별 system prompt를 섞지 않는다.
- `prompts/gen_system.md`는 창작 지시만 담는다.
- `prompts/critique_system.md`는 편집 비평 지시만 담는다.
- `prompts/eval_system.md`는 독립 평가 지시와 점수 팽창 방지 문구를 유지한다.
- `prompts/refine_system.md`는 재작성 지시만 담고, 점수 맞추기를 유도하지 않는다.
- 프롬프트를 바꿀 때는 어떤 schema 출력과 연결되는지 함께 확인한다.

## 실패와 검증 결과

- 성공한 validate 결과는 기본적으로 별도 파일로 남기지 않는다.
- 실패한 validate 결과만 원인 분석을 위해 `*.validation.json`으로 저장한다.
- `REJECT`는 파일은 생성됐지만 schema, 계약, 품질 하한, 필수 조건을 통과하지 못한 상태다.
- `ERROR`는 stage 실행, 파일 읽기/쓰기, JSON 파싱, schema/rubric 로딩 등 파이프라인 자체가 진행하지 못한 상태다.
- validate 호출은 검사 대상 파일을 직접 수정하지 않는다.
- `max_iterations`까지 통과하지 못하면 `final.json` 대신 `{brief_hash}_failed.json`을 남기는 설계를 따른다.

## 금지 행동

- LLM stage가 허용되지 않은 파일을 임의로 읽게 하지 않는다.
- Generator가 `self_score`, `self_critique`, `verdict`를 만들게 하지 않는다.
- Critique가 점수표를 만들게 하지 않는다.
- Evaluator가 critique를 읽거나 PASS/REJECT를 결정하게 하지 않는다.
- Refiner에게 eval 총점 원문이나 generator 내부 히스토리를 넘기지 않는다.
- Validator가 글의 문학적 품질을 주관적으로 판단하게 하지 않는다.
- schema를 우회하기 위해 임의 필드를 top-level에 추가하지 않는다.
- 동일 run artifact를 사용자 의도 없이 덮어쓰지 않는다. 재실행 덮어쓰기는 명시적 `--overwrite`가 있을 때만 허용한다.
