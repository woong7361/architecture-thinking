# 제안 결론

사용자의 수정 방향이 더 적절하다. 필러 여부는 문맥 의존적인 의미 판정이므로 delivery reviewer AI가 분류하고, Python은 AI가 반환한 구조화 결과의 schema 검증과 집계만 담당한다. 말 속도도 숫자 계산은 코드가 맡고, 해당 속도가 발표 전달을 방해하는지는 AI가 해석한다.

## 실행 흐름

```text
audio/video
  -> Codex skill orchestrator
  -> gpt-transcribe
  -> transcript.json + timestamps
  -> 동일한 immutable review-context.json 생성
       ├─ delivery-reviewer 병렬 실행
       │    필러 문맥 분류, 속도 해석, 언어적 어투, 발표 구조
       └─ senior-logic-reviewer 병렬 실행
            주장, 근거, 예시 적합성, 논리 도약, 예상 질문
  -> schema validation + deterministic aggregation
  -> Codex가 두 결과를 종합해 feedback.md 생성
```

두 reviewer는 전사가 끝난 뒤 동시에 실행한다. 서로의 결과를 보지 않고 같은 transcript와 발표 계획을 읽는다. 그래야 논리 reviewer가 delivery reviewer의 해석에 끌려가지 않고 독립적인 관점을 유지한다.

## 책임 경계

### Delivery reviewer

역할은 발표 전달 코치다. 다음을 평가한다.

- 필러 후보를 `filler`, `lexical`, `uncertain`으로 문맥 분류하고 근거 구간과 confidence를 반환한다.
- 전체 및 구간별 속도, 긴 멈춤, 급격한 속도 변화를 해석한다. 분당 어절 수와 시간 계산 자체는 입력으로 받는다.
- 문장 종결 반복, 불필요한 완곡 표현, 모호한 연결어, 긴 문장처럼 transcript에서 확인할 수 있는 언어적 어투를 본다.
- 도입에서 청중과 메시지가 드러나는지, 핵심이 식별되는지, 예시가 핵심과 연결되는지, 마무리가 메시지를 회수하는지 본다.
- 가장 효과가 큰 리허설 행동 세 가지를 제안한다.

실제 목소리의 음높이, 음량, 에너지, 강세는 transcript-only 실행의 평가 범위에서 제외하고 limitation에 기록한다.

### Senior logic reviewer

역할은 회의에서 발표를 듣는 회의적인 시니어 개발자다. 다음을 평가한다.

- 한 문장 핵심 주장이 식별되는지 본다.
- 전제에서 주장, 예시, 결론으로 이어지는 연결을 추적한다.
- 예시가 주장을 실제로 뒷받침하는지 확인한다.
- 생략된 전제, 과장, 모순, 논리 도약을 찾는다.
- 시니어 청중이 물을 가능성이 높은 후속 질문을 제시한다.

필러, 속도, 표현 습관은 평가하지 않는다. 발표자의 주장을 임의로 새로 만들지 않고 관찰, 추론, 누락된 근거를 구분한다.

## role, rubric, schema 분리

```text
.codex/
├─ agents/
│  ├─ delivery-reviewer.toml
│  └─ senior-logic-reviewer.toml
└─ skills/speech-rehearsal/
   ├─ SKILL.md
   ├─ agents/openai.yaml
   ├─ prompts/
   │  ├─ delivery-reviewer.md
   │  └─ senior-logic-reviewer.md
   ├─ rubrics/
   │  ├─ delivery.yaml
   │  └─ logic.yaml
   ├─ schemas/
   │  ├─ run-input.schema.json
   │  ├─ delivery-output.schema.json
   │  └─ logic-output.schema.json
   ├─ scripts/
   │  ├─ transcribe.py
   │  ├─ prepare_review.py
   │  ├─ aggregate_review.py
   │  └─ render_report.py
   └─ tests/fixtures/
```

- Markdown는 역할, 판단 원칙, 금지 사항처럼 사람이 검토해야 하는 지침에 쓴다.
- YAML은 평가 축, 판정 기준, 우선순위처럼 사람이 자주 조정하는 rubric에 쓴다. 프로젝트의 기존 관례와도 맞는다. JSON을 선호하면 같은 구조로 바꿀 수 있다.
- JSON과 JSON Schema는 실행 입력과 agent 출력 계약에 쓴다.
- TOML의 `developer_instructions`에는 전체 rubric을 복사하지 않는다. 읽어야 할 role, rubric, output schema 경로와 책임 경계만 둔다.
- Python에는 prompt나 점수 기준 문자열을 넣지 않는다. 자원 로드, 병렬 실행 준비, schema 검증, 수치 계산, 결과 집계만 맡긴다.

각 실행은 실제로 사용한 role과 rubric 버전 또는 해시를 manifest에 남긴다. 그러면 나중에 feedback이 달라졌을 때 어떤 기준 변경 때문인지 추적할 수 있다.

## 필러 출력 계약 예시

```json
{
  "filler_annotations": [
    {
      "surface": "그",
      "start_seconds": 12.4,
      "end_seconds": 12.7,
      "label": "filler",
      "confidence": 0.82,
      "context": "그, 제가 이 부분에서 말하고 싶은 것은",
      "reason": "다음 발화를 계획하는 동안 의미 없이 사용됨"
    }
  ],
  "limitations": [
    "transcript와 timestamp만 사용해 음높이와 음량은 평가하지 않음"
  ]
}
```

집계 코드는 `label=filler`만 확정 빈도에 포함하고 `uncertain`은 별도로 보고한다. 이렇게 하면 AI가 의미를 판정하되, 총계가 임의로 흔들리거나 근거 없는 숫자로 보이는 문제를 줄일 수 있다.

## 선택지와 추천

1. 모든 판단과 집계를 AI에게 맡긴다. 구현은 단순하지만 실행마다 숫자가 달라질 수 있고 누락 검증이 어렵다.
2. 필러를 정규식으로 판정하고 AI는 설명만 한다. 재현성은 높지만 `그`처럼 실제 의미가 있는 표현을 오탐할 수 있다.
3. AI가 문맥을 분류하고 코드는 schema 검증과 집계를 한다. 의미 판정과 재현성의 경계가 분명하다.

3번을 추천한다. 사용자의 의도를 반영하면서 D-5가 요구하는 빈도 수치를 검증 가능한 산출물로 남길 수 있기 때문이다.

## 승인 후 구현 범위

- `speech-rehearsal` skill
- 두 project custom agent
- role Markdown, rubric YAML, output JSON Schema
- 전사 호출과 review input 준비, 집계, report 생성 스크립트
- 필러의 `filler/lexical/uncertain` 구분과 병렬 reviewer 독립성에 대한 fixture 테스트
- API key가 없는 환경에서는 mock transcript로 검증하고, 실제 녹화와 사용자가 제공한 환경 변수의 key가 있을 때 live 전사 검증

이 수정안에 사용자가 동의한 뒤 구현을 시작한다.
