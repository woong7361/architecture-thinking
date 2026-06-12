# Writing Harness Pipeline

## 목적

이 파이프라인은 사람이 글을 더 잘 쓰기 위한 글쓰기 앱이 아니다.

목적은 AI 에이전트를 단계별로 굴려서 더 나은 에세이와 회고 초안을 생산하는 하네스를 설계하는 것이다. 핵심 관심사는 최종 문장의 아름다움보다 다음에 있다.

- 같은 입력에서 어떤 초안이 나왔는지 추적 가능해야 한다.
- 어떤 비평과 평가 때문에 초안이 개선되었는지 남아야 한다.
- 생성자, 비평자, 평가자, 퇴고자가 서로의 역할을 침범하지 않아야 한다.
- 최종적으로 사용자가 바로 다듬어 쓸 수 있는 개선된 초안을 만들어야 한다.
- 반복 실행 기록은 다음 초안을 더 잘 만들기 위한 학습 재료로 남아야 한다.

즉, 이 프로젝트의 1차 산출물은 **개선된 초안**이고, 2차 산출물은 그 초안이 만들어진 과정을 재현하고 개선할 수 있는 **반복 가능한 초안 생성 시스템**이다.

## 핵심 구조

파이프라인은 4개 역할을 기준 축으로 한다.

1. **Gen**: 주제, 재료, 의도, 독자, 톤을 받아 1차 초안을 만든다.
2. **Critique**: 새 세션의 시니어 편집자 역할로 초안의 약점과 개선 방향을 뽑는다.
3. **Eval**: 루브릭 기반 점수와 점수 근거를 낸다.
4. **Refine**: 비평과 평가 결과를 근거로 다음 초안을 만든다.

각 역할은 파일을 통해서만 핸드오프한다. 함수 import, 공유 메모리, 대화 히스토리 공유를 기본값으로 삼지 않는다.

`validate.py`는 별도의 창작 에이전트가 아니라 각 단계 사이에 끼어드는 기계적 검사 게이트다. 모든 입력과 산출물은 다음 단계로 넘어가기 전에 스키마와 계약 검사를 통과해야 한다.

## 전체 흐름

```text
{brief_hash}_input.json
 ↓ validate input schema
 ↓ generator
{brief_hash}_iter-{iteration}_draft.json
 ↓ validate draft schema + draft mechanical checks
 ↓ critique
{brief_hash}_iter-{iteration}_critique.json
 ↓ validate critique schema
 ↓ evaluator
{brief_hash}_iter-{iteration}_eval.json
 ↓ validate eval schema + final contract checks
 ├─ PASS   → {brief_hash}_final.json
 └─ REJECT → {brief_hash}_iter-{iteration}_refine-request.json
              ↓ validate refine request schema
              ↓ refine
            {brief_hash}_iter-{next_iteration}_draft.json
              ↺ critique → eval → validate
```

반복은 `iter_N` 단위로 관리한다. 한 run 안에서 초안 버전이 늘어나며, 각 iteration은 동일한 파일 이름 계약을 유지한다.

`validate`는 하나의 파일만 검사하는 단일 위치가 아니다. 각 단계의 출력 JSON을 다음 단계 입력으로 사용해도 되는지 확인하는 공통 함수다. `{brief_hash}_iter-{iteration}_eval.json` 이후의 validate 호출은 스키마, 길이, 금칙어, 루브릭 점수 하한처럼 기계적으로 판정 가능한 항목을 검사하고, runner는 그 결과로 `final.json` 또는 `refine-request.json`을 만든다.

## 설계 원칙

### 역할 분리

Generator는 창작자다. 자기 점수, 자기 비평, 최종 판정을 만들지 않는다.

Critique는 편집자다. 점수표를 만들지 않고, 독자가 느낄 문제와 다음 퇴고 방향을 말한다.

Evaluator는 심사자다. 자신이 글을 쓴 것처럼 변명하지 않고, 루브릭에 따라 점수와 근거를 낸다.

Refiner는 재작성자다. 총점을 맞추려 하지 않고, 약점과 수정 요청을 반영해 다음 초안을 만든다.

Validator는 기계적 계약 검사자다. 글의 문학적 품질을 판단하지 않고, 스키마, 길이, 금칙어, 품질 하한 같은 자동 검증만 수행한다.

### 정보 차단

좋은 하네스는 모든 정보를 한 에이전트에게 몰아주지 않는다. 단계별로 봐도 되는 파일을 제한한다.

| 단계 | 입력으로 봐도 되는 파일 | 보면 안 되는 파일 |
| --- | --- | --- |
| Gen | `{brief_hash}_input.json` | critique, eval, refine request |
| Critique | `{brief_hash}_input.json`, `{brief_hash}_iter-{iteration}_draft.json` | eval 점수, refine request |
| Eval | `{brief_hash}_input.json`, `{brief_hash}_iter-{iteration}_draft.json`, `rubric.yaml` | generator 히스토리, critique |
| Validate | 검사 대상 JSON, 해당 schema, 기계적 계약, rubric threshold | LLM 대화 히스토리 |
| Refine | `{brief_hash}_input.json`, `{brief_hash}_iter-{iteration}_draft.json`, `{brief_hash}_iter-{iteration}_critique.json`, `{brief_hash}_iter-{iteration}_refine-request.json` | eval 총점 원문, generator 내부 히스토리 |

Eval은 Critique의 판단에 anchor되지 않고 초안 자체를 독립 채점해야 하므로 `critique.json`을 입력으로 받지 않는다.

특히 Refine 단계에는 총점을 그대로 넘기지 않는다. 총점을 넘기면 모델이 숫자를 맞추려는 방향으로 움직일 수 있다. 대신 `weak_axes`, `contract_errors`, `critique_summary`, `revision_instructions`만 넘긴다.

파일명에 `brief_hash`와 `iteration`을 넣는 것은 정보 차단 그 자체가 아니다. 따라서 runner는 각 단계에 허용된 파일만 읽고, 그 내용을 agent 입력 payload로 구성해 넘긴다.

## 프로젝트 원칙 (AGENTS.md)

`AGENTS.md`에는 이 파이프라인에서 AI가 작업할 때 지켜야 할 실행 규칙을 둔다.

- 역할 경계: Generator, Critique, Evaluator, Refiner, Validator의 책임을 섞지 않는다.
- 파일 핸드오프: stage는 파일명을 직접 만들지 않고, runner가 경로와 payload를 제어한다.
- 정보 차단: Eval은 Critique를 보지 않고, Refine은 eval 총점 원문을 보지 않는다.
- 검증 기준: validate는 기계적으로 판정 가능한 계약만 검사한다.
- 파일명 규칙: 숫자 prefix 대신 `{brief_hash}_iter-{iteration}_{artifact}.json` 형식을 따른다.
- 프롬프트 원칙: 역할별 system prompt를 섞지 않고, 평가자 점수 팽창 방지 문구를 유지한다.
- 금지 행동: LLM stage가 임의 파일을 읽거나, 자기 평가와 최종 판정을 생성하지 않는다.

## 디렉토리 구조

```text
writing-harness-pipeline/
├── AGENTS.md # 파이프라인 작업 규칙과 금지 행동
├── README.md # 파이프라인 설계와 운영 기준
├── schema-contracts.md # schema별 파일 계약 상세
├── runner.py # 전체 실행 순서와 파일 핸드오프 제어
├── stages/ # LLM을 호출하는 역할별 단계
│   ├── generator.py # input을 받아 초안 생성
│   ├── critique.py # 초안의 약점과 수정 방향 비평
│   ├── evaluator.py # 루브릭 기반 독립 평가
│   └── refine.py # 비평과 검증 결과를 반영해 재작성
├── validators/ # 기계적 검증 유틸
│   ├── validate.py # 검증 흐름을 조합하는 entrypoint
│   └── schema.py # JSON schema 검사
├── prompts/ # 역할별 system prompt
│   ├── gen_system.md # Generator 역할 지시
│   ├── critique_system.md # Critique 역할 지시
│   ├── eval_system.md # Evaluator 역할 지시
│   └── refine_system.md # Refiner 역할 지시
├── schemas/ # 파일 계약 JSON schema
│   ├── input.schema.json # 사용자 입력 계약
│   ├── draft.schema.json # 초안 산출물 계약
│   ├── critique.schema.json # 비평 산출물 계약
│   ├── eval.schema.json # 평가 산출물 계약
│   ├── refine_request.schema.json # 재작성 요청 계약
│   └── final.schema.json # 최종 초안 계약
├── rubrics/ # 평가 기준
│   └── writing.yaml # 에세이와 회고 공통 평가 루브릭
├── runs/ # 실행 결과 저장소
│   └── 2026-06-12_a1b2c3d4/ # run_id 단위 실행 디렉토리
│       ├── a1b2c3d4_input.json # 원본 입력
│       ├── iter_001/ # 첫 번째 생성/평가 반복
│       │   ├── a1b2c3d4_iter-001_draft.json # 현재 iteration 초안
│       │   ├── a1b2c3d4_iter-001_critique.json # 현재 iteration 비평
│       │   ├── a1b2c3d4_iter-001_eval.json # 현재 iteration 평가
│       │   └── a1b2c3d4_iter-001_refine-request.json # 다음 iteration 재작성 요청
│       ├── iter_002/ # 두 번째 생성/평가 반복
│       │   ├── a1b2c3d4_iter-002_draft.json # 재작성 초안
│       │   ├── a1b2c3d4_iter-002_critique.json # 재작성 초안 비평
│       │   └── a1b2c3d4_iter-002_eval.json # 재작성 초안 평가
│       └── a1b2c3d4_final.json # 통과한 최종 초안
└── archive/ # 7일 이상 지난 run 보관
```

현재처럼 루트에 `generator.py`, `evaluator.py`, `validate.py`를 둘 수도 있지만, 설계 기준으로는 창작/판단 에이전트는 `stages/` 아래에 모으고, 기계적 검증은 `validators/` 아래로 분리하는 편이 낫다. `validate.py`는 하나의 에이전트 단계가 아니라 모든 단계 사이에서 호출되는 공통 게이트이기 때문이다.

`schema.py`는 JSON 구조가 맞는지만 본다. `validate.py`는 schema 검사와 함께 글자 수 범위, 금칙어, `brief_hash` 일치, 루브릭 총점 하한, 축별 최소 점수 같은 기계적 계약 검사를 수행하고, runner가 사용할 `PASS/REJECT`, `contract_errors`, `weak_axes`를 돌려준다.

## Run 식별자

run 디렉토리 이름은 다음 형식을 사용한다.

```text
runs/{yyyy-mm-dd}_{brief_hash}/
```

예시:

```text
runs/2026-06-12_a1b2c3d4/
```

`brief_hash`는 원본 입력의 핵심 필드를 정규화한 뒤 만든다. 같은 주제와 같은 재료를 다시 실행했을 때 비교 가능해야 하므로, 임의 UUID만 사용하는 방식은 피한다.

## 파일 이름 규칙

실제 파일 이름에는 `brief_hash`, `iteration`, `artifact name`을 넣는다.

```text
{brief_hash}_input.json
{brief_hash}_iter-{iteration}_{artifact}.json
{brief_hash}_final.json
```

예시:

```text
a1b2c3d4_input.json
a1b2c3d4_iter-001_draft.json
a1b2c3d4_iter-001_eval.json
a1b2c3d4_final.json
```

번호만 앞에 붙인 `00_input.json`, `10_gen_request.json` 같은 이름은 사용하지 않는다. 정렬에는 편하지만, AI와 사람이 숫자의 의미를 추측해야 하고 다른 run의 파일과 섞였을 때 식별력이 떨어진다.

## 파일 계약

파일 계약 상세는 [schema-contracts.md](schema-contracts.md)에 둔다. README는 파이프라인의 목적, 흐름, 운영 원칙을 설명하고, schema별 `required`, `additionalProperties`, 필수 필드, 권장 필드, 예시는 별도 문서에서 관리한다.

요약 원칙은 다음과 같다.

- top-level 필드는 단계 간 핸드오프에 필요한 안정적인 필드만 둔다.
- 자유롭게 확장될 수 있는 값은 `brief`, `metadata`, `details`처럼 의도된 하위 객체 안에 둔다.
- 기본적으로 `additionalProperties: false`를 사용하고, 확장이 필요한 객체에만 명시적으로 허용한다.
- 금지 필드는 schema만 믿지 않고 validate에서도 한 번 더 검사한다.
- schema는 구조를 검증하고, validate는 길이, 금칙어, 점수 하한 같은 기계적 계약을 검증한다.

## Rubric 설계

초기에는 에세이와 회고를 `writing.yaml` 하나로 평가한다.

기본 축은 다음 4개로 시작한다.

| 축 | 설명 |
| --- | --- |
| `structure` | 글의 흐름, 주장과 근거의 배열, 결론의 선명함 |
| `evidence` | 경험, 사례, 관찰, 인용, 수치가 주장을 받치는 정도 |
| `sentence` | 문장 밀도, 리듬, 군더더기, 읽히는 힘 |
| `originality` | 일반론이 아니라 이 사람의 관점과 언어가 드러나는 정도 |

확장 축은 필요할 때만 추가한다.

- `hook`: 첫 문장 또는 첫 문단의 흡입력
- `actionability`: 읽은 뒤 남는 다음 행동
- `tone`: 의도한 화자성과 독자 거리감
- `length_calibration`: 목표 길이에 맞는 밀도

처음부터 많은 축을 쓰면 평가가 정교해지는 대신 Refine 요청이 산만해질 수 있다. 따라서 기본 운영은 4축으로 시작하고, 필요해질 때만 확장 축이나 글 유형별 루브릭을 분리한다.

## PASS/REJECT 기준

Validator는 다음 기준을 본다.

- JSON schema 통과 여부
- `brief_hash` 일치 여부
- 글자 수 하한과 상한
- 금칙어 포함 여부
- 검사 가능한 필수 조건 포함 여부
- 루브릭 총점 하한
- 특정 핵심 축의 최소 점수

검사 가능한 필수 조건은 exact keyword, regex, min/max length, required field처럼 기계적으로 판정할 수 있는 조건이다. "실패 사례가 설득력 있게 들어갔는가" 같은 조건은 Validator가 아니라 Critique나 Eval에서 다룬다.

예시 기준:

```yaml
min_total: 3.2
min_axis:
  structure: 3.0
  evidence: 3.0
  sentence: 3.0
  originality: 2.5
max_iterations: 3
```

`max_iterations`에 도달했는데도 PASS하지 못하면 `{brief_hash}_final.json`을 만들지 않고 `{brief_hash}_failed.json`을 남긴다. 실패도 학습 데이터이므로 삭제하지 않는다.

## 실패 정책

실패는 단순히 "좋지 않은 초안"이 아니라 다음 단계로 안전하게 넘길 수 없는 상태를 뜻한다. 따라서 runner는 실패를 숨기거나 덮어쓰지 않고, 원인과 조치가 보이도록 파일로 남긴다.

실패 상태는 크게 두 가지로 나눈다.

- `REJECT`: 파일은 정상적으로 생성됐지만 schema, 계약, 품질 하한, 필수 조건을 통과하지 못한 상태다. 대개 refine으로 회복 가능하다.
- `ERROR`: stage 실행, 파일 읽기/쓰기, JSON 파싱, schema/rubric 로딩처럼 파이프라인 자체가 다음 단계로 진행하지 못한 상태다. 같은 입력으로 재시도하거나 환경을 고쳐야 한다.

`REJECT` 또는 `ERROR`가 발생하면 그 사유를 다음 `category` 중 하나로 기록한다. 모든 category가 에러를 뜻하지는 않는다. `quality_reject`처럼 파이프라인은 정상 동작했지만 품질 기준을 넘지 못해 재작성으로 보내는 거절 사유도 있다.

| category | 의미 | 기본 조치 |
| --- | --- | --- |
| `schema_error` | JSON 구조, required 필드, 타입, enum, additionalProperties 위반 | 같은 stage 재생성 또는 stage prompt/schema 수정 |
| `contract_error` | 길이, 금칙어, `brief_hash` 불일치, 필수 조건 누락 같은 기계적 계약 위반 | refine request에 포함 |
| `quality_reject` | 실행 에러가 아니라 루브릭 총점 또는 핵심 축 점수가 하한 미만인 품질 거절 | weak_axes와 함께 refine request에 포함 |
| `role_boundary_violation` | draft에 `self_score`, eval에 `verdict`처럼 역할을 침범한 필드가 포함됨 | 해당 stage prompt와 schema 수정 |
| `stage_error` | LLM 호출 실패, timeout, 빈 응답, invalid JSON 응답 | 동일 stage 재시도 후 계속 실패하면 terminal failure |
| `runner_error` | 파일 경로, 권한, schema/rubric 파일 누락, I/O 실패 | 즉시 terminal failure로 처리하고 실행 환경 또는 runner 수정 |
| `max_iteration_exceeded` | 최대 반복 횟수까지 PASS하지 못함 | `{brief_hash}_failed.json` 생성 |

각 validate 호출은 검사 대상 파일을 수정하지 않는다. `PASS`한 검사는 기본적으로 별도 파일을 남기지 않고, 최종 통과 시 `final.json`의 `contract_result`에 요약한다. 단, `REJECT` 또는 `ERROR`가 발생한 validate 호출은 원인 분석을 위해 별도 결과 파일을 남긴다.

```text
{brief_hash}_input.validation.json
iter_001/{brief_hash}_iter-001_draft.validation.json
iter_001/{brief_hash}_iter-001_critique.validation.json
iter_001/{brief_hash}_iter-001_eval.validation.json
iter_001/{brief_hash}_iter-001_refine-request.validation.json
```

실패 validation 결과 파일의 기본 형태는 다음과 같다.

```json
{
  "brief_hash": "a1b2c3d4",
  "iteration": "001",
  "artifact": "draft",
  "checked_file": "iter_001/a1b2c3d4_iter-001_draft.json",
  "checked_at": "2026-06-12T16:10:00+09:00",
  "status": "REJECT",
  "checked_rules": ["schema", "brief_hash", "length", "banned_words"],
  "failures": [
    {
      "category": "contract_error",
      "rule": "length_min",
      "severity": "medium",
      "retryable": true,
      "message": "content length is below minimum",
      "expected": ">= 1200",
      "actual": 980,
      "json_path": "$.content",
      "next_action": "refine에서 구체적 사례를 추가해 길이 하한을 맞춘다"
    }
  ]
}
```

성공한 validation의 세부 결과를 매번 파일로 남기지 않는 이유는 run 디렉토리의 노이즈를 줄이기 위해서다. 성공은 다음 단계가 실행됐다는 사실과 최종 `final.json`의 `contract_result.checked_rules`로 확인한다. 실패한 validation의 `failures`는 사람이 읽을 수 있는 `message`와 기계가 분류할 수 있는 `category`, `rule`, `severity`, `retryable`을 함께 가진다. 이렇게 해야 나중에 실패 run을 모아 "schema가 자주 깨지는가", "품질 하한이 너무 높은가", "특정 stage prompt가 JSON을 자주 망가뜨리는가"를 분석할 수 있다.

최종 실패 파일인 `{brief_hash}_failed.json`은 run 루트에 한 번만 만든다. 이 파일은 마지막 오류만 담지 않고, 어떤 iteration에서 어떤 이유로 막혔는지 요약한다.

```json
{
  "brief_hash": "a1b2c3d4",
  "run_id": "2026-06-12_a1b2c3d4",
  "failed_at": "2026-06-12T16:30:00+09:00",
  "terminal_reason": "max_iteration_exceeded",
  "last_iteration": "003",
  "failure_counts_by_category": {
    "contract_error": 2,
    "quality_reject": 3
  },
  "last_failures": [
    {
      "category": "quality_reject",
      "rule": "min_axis.evidence",
      "severity": "high",
      "retryable": false,
      "message": "evidence score stayed below minimum after max iterations"
    }
  ],
  "lineage": {
    "input": "a1b2c3d4_input.json",
    "last_draft": "iter_003/a1b2c3d4_iter-003_draft.json",
    "last_critique": "iter_003/a1b2c3d4_iter-003_critique.json",
    "last_eval": "iter_003/a1b2c3d4_iter-003_eval.json"
  },
  "next_actions": [
    "원본 brief에 구체적 사례를 추가한다",
    "evidence 축 기준이 현재 글 유형에 과하게 높은지 확인한다"
  ]
}
```

`failed.json`은 `final.json`의 대체물이 아니다. 통과한 초안이 없다는 사실과 실패 원인을 고정하는 실행 로그다. 따라서 실패 run도 archive 전까지 그대로 보존하고, 다음 실험에서는 `failed.json`과 각 `*.validation.json`을 함께 본다.

## 반복 정책

반복은 다음 조건에서 발생한다.

- Validator가 `REJECT`를 반환한다.
- 핵심 축 중 하나가 최소 점수 미만이다.
- Critique가 치명적 독자 리스크를 표시했다.
- 검사 가능한 필수 조건이 빠졌다.

반복은 기본적으로 최대 3회까지 허용한다.

```text
iter_001: 초기 생성
iter_002: 비평과 검증 오류 반영
iter_003: 마지막 보정
```

3회 이후에는 계속 돌리지 않는다. 반복 횟수가 늘어날수록 글이 좋아지기보다 평가자 취향에 과적합될 수 있기 때문이다.

## 에이전트별 프롬프트 원칙

### Generator

Generator 시스템 프롬프트는 창작자 역할을 준다.

```text
당신은 개인 에세이와 개발자 회고 초안을 쓰는 창작자입니다.
주어진 재료에서 구체적인 장면, 판단, 변화의 흐름을 뽑아 초안을 작성하세요.
자기 평가 점수나 비평은 출력하지 마세요.
```

### Critique

Critique 시스템 프롬프트는 시니어 편집자 역할을 준다.

```text
당신은 개인 에세이와 개발자 회고를 다루는 시니어 편집자입니다.
초안을 다시 쓰지 말고, 독자가 약하게 느낄 지점과 다음 퇴고 방향을 제시하세요.
점수는 출력하지 마세요.
```

### Evaluator

Evaluator 시스템 프롬프트는 심사자 역할을 준다.

```text
당신은 이런 콘텐츠의 품질을 1~5점으로 평가하는 심사자입니다. 창작자가 아닙니다.
이 산출물을 당신이 만들었다고 가정하지 마세요. 다른 사람이 만든 콘텐츠를 심사하는 입장입니다.
5점은 드뭅니다. 평균 3.0을 기준으로 채점하세요.
각 축에 점수를 준 근거 한 줄을 함께 출력하세요.
```

### Refiner

Refiner 시스템 프롬프트는 재작성자 역할을 준다.

```text
당신은 비평과 검증 오류를 반영해 초안을 개선하는 퇴고자입니다.
원문의 의도와 재료를 유지하되, 약점으로 지적된 부분을 우선 수정하세요.
평가 총점을 추측하거나 점수를 맞추려 하지 마세요.
```

## 아카이브 정책

`runs/`는 최근 작업을 위한 공간이다. 오래된 run은 `archive/`로 이동한다.

권장 기준:

- 7일 이상 지난 PASS run은 archive로 이동
- 실패 run은 분석이 끝난 뒤 archive로 이동
- archive 이동 시 파일 내용은 변경하지 않는다

## 현재 설계상 우선 정리할 것

현재 파일 구조와 설계 문서 사이에는 몇 가지 정리 지점이 있다.

- `generator.py`, `critique.py`, `evaluator.py`, `refine.py`는 `stages/`로 이동할지 결정한다.
- `validate.py`는 `stages/`가 아니라 `validators/` 또는 `contracts/`로 분리할지 결정한다.
- `critique.py`, `refine.py`의 파일 계약을 위 구조에 맞춘다.
- `output.schema.json`은 `draft.schema.json`으로 이름을 바꾸는 편이 명확하다.
- `verdict.schema.json`은 `eval.schema.json`으로 정리하고, validate 결과는 `refine_request.schema.json`과 `final.schema.json`에 필요한 필드만 포함한다.
- `rubric.yaml`은 기본 4축과 확장 축을 구분하도록 정리한다.
- `prompts/refine_systme.md`는 `prompts/refine_system.md`로 수정한다.

## 성공 기준

이 설계가 잘 동작한다는 것은 다음을 의미한다.

- 초안이 왜 통과했는지 파일만 보고 설명할 수 있다.
- 초안이 왜 거절됐는지 파일만 보고 설명할 수 있다.
- Refine 단계가 어떤 약점을 고쳤는지 추적할 수 있다.
- Generator와 Evaluator가 서로의 역할을 침범하지 않는다.
- 실패한 run도 다음 프롬프트, 루브릭, 계약 개선에 사용할 수 있다.
