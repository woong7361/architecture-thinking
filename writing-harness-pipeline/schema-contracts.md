# Schema Contracts

이 문서는 `writing-harness-pipeline`의 파일 계약 상세를 설명한다. 실제 JSON Schema 파일을 작성할 때 이 문서를 기준으로 `required`, `additionalProperties`, 필수 필드, 권장 필드, 예시를 옮긴다.

## 파일 계약

파일 계약은 `schemas/*.schema.json`을 먼저 정의하고, run 디렉토리의 JSON 파일은 그 schema를 따르는 instance로 본다. 즉 파일명은 저장과 추적을 위한 규칙이고, 실제 계약은 schema가 결정한다.

스키마는 다음 원칙으로 잡는다.

- top-level 필드는 단계 간 핸드오프에 필요한 안정적인 필드만 둔다.
- 자유롭게 확장될 수 있는 값은 `brief`, `metadata`, `details`처럼 의도된 하위 객체 안에 둔다.
- LLM이 임의 필드를 추가해도 되는 영역과 절대 추가하면 안 되는 영역을 구분한다.
- 금지 필드는 schema만 믿지 않고 validate에서도 한 번 더 검사한다.
- schema는 "구조"를 검증하고, validate는 길이, 금칙어, 점수 하한 같은 "기계적 계약"을 검증한다.

### input.schema.json

대상 파일:

```text
{brief_hash}_input.json
```

사용자가 제공한 글쓰기 요청의 원본 계약이다. top-level은 실행 추적에 필요한 최소 필드로 닫고, 글쓰기 입력의 확장은 `brief` 내부에서 허용한다.

Schema 작성 기준:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Pipeline Input",
  "type": "object",
  "required": ["brief_hash", "brief", "created_at"],
  "properties": {
    "brief_hash": {
      "type": "string",
      "pattern": "^[a-f0-9]{8,}$",
      "description": "브리프 해시. 한 건을 관통하는 유일 식별자"
    },
    "brief": {
      "type": "object",
      "description": "글쓰기 요청 본문. 내부 확장 허용",
      "required": ["topic"],
      "properties": {
        "topic": {
          "type": "string",
          "description": "글의 주제"
        }
      },
      "additionalProperties": true
    },
    "created_at": {
      "type": "string",
      "format": "date-time",
      "description": "입력 파일이 생성된 시각"
    }
  },
  "additionalProperties": false
}
```

`brief`는 사용자의 생각과 재료가 계속 늘어나는 영역이므로 `additionalProperties: true`를 허용한다. 대신 top-level은 닫아 두어 runner, validator, stage가 기대하지 않은 제어 필드를 읽지 않게 한다.

필수 필드:

- `brief_hash`: 입력 식별자
- `created_at`: 생성 시각
- `brief`: 글쓰기 요청 본문. 내부 확장 허용

`brief`의 필수 필드:

- `topic`: 글의 주제

`brief`의 권장 필드:

- `piece_type`: `essay` 또는 `retrospective`
- `intent`: 글을 쓰는 이유
- `audience`: 예상 독자
- `materials`: 사건, 메모, 인용, 경험, 참고 자료
- `constraints`: 길이, 톤, 금지 표현, 반드시 포함할 내용

예시:

```json
{
  "brief_hash": "a1b2c3d4",
  "created_at": "2026-06-12T15:30:00+09:00",
  "brief": {
    "topic": "AI 하네스를 설계하며 배운 점",
    "piece_type": "retrospective",
    "intent": "개인 회고 초안 생성",
    "audience": "AI native 개발 방식에 관심 있는 개발자",
    "materials": [
      "프롬프트보다 파일 계약이 중요했다",
      "평가자와 생성자를 분리해야 점수가 덜 부풀었다"
    ],
    "constraints": {
      "target_length": "1200-1800 Korean characters",
      "tone": "차분하고 구체적인 개발자 회고",
      "must_include": ["실패 사례", "다음 실험"],
      "avoid": ["과장된 성공담", "일반론만 나열"]
    }
  }
}
```

### draft.schema.json

대상 파일:

```text
{brief_hash}_iter-{iteration}_draft.json
```

Generator 또는 Refiner가 만든 초안 산출물이다.

Schema 작성 기준:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Pipeline Draft",
  "type": "object",
  "required": ["brief_hash", "iteration", "stage", "content", "generated_at", "model"],
  "properties": {
    "brief_hash": {
      "type": "string",
      "pattern": "^[a-f0-9]{8,}$",
      "description": "어떤 입력에서 나온 초안인지 연결하는 브리프 해시"
    },
    "iteration": {
      "type": "string",
      "pattern": "^[0-9]{3}$",
      "description": "몇 번째 반복에서 만들어진 초안인지 나타내는 번호"
    },
    "stage": {
      "type": "string",
      "enum": ["gen", "refine"],
      "description": "생성 주체. 최초 생성은 gen, 퇴고 생성은 refine"
    },
    "content": {
      "type": "string",
      "minLength": 1,
      "description": "다음 단계가 읽을 실제 초안 본문"
    },
    "generated_at": {
      "type": "string",
      "format": "date-time",
      "description": "초안 생성 시각"
    },
    "model": {
      "type": "string",
      "minLength": 1,
      "description": "초안을 만든 모델 또는 실행 프로필"
    },
    "metadata": {
      "type": "object",
      "description": "재현성에 필요한 부가 정보. 내부 확장 허용",
      "properties": {
        "prompt_version": {
          "type": "string",
          "description": "초안 생성에 사용한 prompt 버전"
        },
        "source_files": {
          "type": "array",
          "description": "runner가 stage에 전달한 입력 파일 목록",
          "items": {
            "type": "string"
          }
        },
        "token_usage": {
          "type": "object",
          "description": "비용과 품질 비교를 위한 토큰 사용량",
          "additionalProperties": true
        }
      },
      "additionalProperties": true
    }
  },
  "additionalProperties": false
}
```

`draft`는 창작 산출물일 뿐 판단 산출물이 아니다. 그래서 top-level은 `additionalProperties: false`로 닫고, 실험용 정보는 `metadata` 내부에만 둔다. 특히 자기 평가나 최종 판정이 들어오면 역할 경계가 깨진 것으로 본다.

필수 필드:

- `brief_hash`: 입력 파일과 같은 값이어야 한다.
- `iteration`: `001`, `002`처럼 파일명과 대응되는 반복 번호다.
- `stage`: `gen` 또는 `refine`만 허용한다.
- `content`: 비평, 평가, 최종 산출의 대상이 되는 초안 본문이다.
- `generated_at`: 초안 생성 시각이다.
- `model`: 초안을 만든 모델 또는 실행 프로필이다.

권장 필드:

- `metadata.prompt_version`: 어떤 generator/refiner prompt로 만들었는지 남긴다.
- `metadata.source_files`: runner가 stage에 전달한 입력 파일 목록이다.
- `metadata.token_usage`: 비용과 품질을 비교하기 위한 사용량이다.

금지 필드:

- `self_score`
- `self_critique`
- `verdict`

예시:

```json
{
  "brief_hash": "a1b2c3d4",
  "iteration": "001",
  "stage": "gen",
  "content": "처음에는 프롬프트를 잘 쓰는 일이 핵심이라고 생각했다...",
  "generated_at": "2026-06-12T15:34:00+09:00",
  "model": "gpt-5",
  "metadata": {
    "prompt_version": "gen_system:v1",
    "source_files": ["a1b2c3d4_input.json"]
  }
}
```

### critique.schema.json

대상 파일:

```text
{brief_hash}_iter-{iteration}_critique.json
```

Critique 단계의 산출물이다.

Schema 작성 기준:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Pipeline Critique",
  "type": "object",
  "required": ["brief_hash", "iteration", "summary", "strengths", "weaknesses", "revision_directions", "reader_risks"],
  "properties": {
    "brief_hash": {
      "type": "string",
      "pattern": "^[a-f0-9]{8,}$",
      "description": "비평 대상 입력 식별자"
    },
    "iteration": {
      "type": "string",
      "pattern": "^[0-9]{3}$",
      "description": "비평 대상 초안의 반복 번호"
    },
    "summary": {
      "type": "string",
      "minLength": 1,
      "description": "초안의 현재 상태를 한 단락으로 요약한 내용"
    },
    "strengths": {
      "type": "array",
      "description": "다음 초안에서도 보존해야 할 강점 목록",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "weaknesses": {
      "type": "array",
      "description": "수정해야 할 약점 목록",
      "items": {
        "type": "object",
        "required": ["issue", "why_it_matters", "suggestion"],
        "properties": {
          "issue": {
            "type": "string",
            "minLength": 1,
            "description": "초안에서 발견한 문제"
          },
          "why_it_matters": {
            "type": "string",
            "minLength": 1,
            "description": "그 문제가 독자 경험이나 글의 목적에 중요한 이유"
          },
          "suggestion": {
            "type": "string",
            "minLength": 1,
            "description": "다음 퇴고에서 적용할 수정 방향"
          },
          "severity": {
            "type": "string",
            "enum": ["low", "medium", "high"],
            "description": "약점의 심각도"
          }
        },
        "additionalProperties": false
      }
    },
    "revision_directions": {
      "type": "array",
      "description": "다음 퇴고에서 적용할 방향",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "reader_risks": {
      "type": "array",
      "description": "독자가 오해하거나 이탈할 위험",
      "items": {
        "type": "string",
        "minLength": 1
      }
    }
  },
  "additionalProperties": false
}
```

`critique`는 점수가 아니라 편집 판단을 남기는 파일이다. 약점은 단순 취향이 아니라 "문제", "왜 문제인지", "수정 방향"을 분리해 써야 Refine이 실행 가능한 입력으로 사용할 수 있다.

필수 필드:

- `brief_hash`: 비평 대상 draft와 같은 값이어야 한다.
- `iteration`: 비평 대상 draft의 반복 번호다.
- `summary`: 초안의 현재 인상을 짧게 정리한다.
- `strengths`: 다음 초안에서도 보존해야 할 요소다.
- `weaknesses`: 독자가 약하게 느낄 지점이다.
- `revision_directions`: Refine이 참고할 수정 방향이다.
- `reader_risks`: 독자의 이탈, 오해, 피로를 만들 수 있는 위험이다.

권장 필드와 규칙:

- 약점은 3개를 기본값으로 한다.
- 각 약점은 `issue`, `why_it_matters`, `suggestion`을 포함한다.
- `severity`는 `low`, `medium`, `high` 중 하나로 둔다.
- `reader_risks`는 "있다/없다"가 아니라 어떤 독자 반응이 생기는지로 적는다.
- 점수는 쓰지 않는다.

예시:

```json
{
  "brief_hash": "a1b2c3d4",
  "iteration": "001",
  "summary": "파일 계약의 중요성은 드러나지만, 실패 장면이 추상적이라 회고의 밀도가 약하다.",
  "strengths": [
    "생성자와 평가자를 분리하려는 문제의식이 선명하다"
  ],
  "weaknesses": [
    {
      "issue": "실패 사례가 일반론으로 처리된다",
      "why_it_matters": "독자가 실제로 무엇이 잘못됐는지 따라가기 어렵다",
      "suggestion": "하나의 run에서 어떤 필드가 빠져 다음 단계가 깨졌는지 구체적으로 보여준다",
      "severity": "high"
    }
  ],
  "revision_directions": [
    "초반에 실제 실패 장면을 먼저 배치한다",
    "마지막 문단에서 다음 실험을 한 가지로 좁힌다"
  ],
  "reader_risks": [
    "AI 하네스를 모르는 독자는 파일 계약이 왜 필요한지 늦게 이해할 수 있다"
  ]
}
```

### eval.schema.json

대상 파일:

```text
{brief_hash}_iter-{iteration}_eval.json
```

Evaluator 단계의 산출물이다.

Schema 작성 기준:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Pipeline Eval",
  "type": "object",
  "required": ["brief_hash", "iteration", "rubric_name", "rubric_scores", "axis_rationales", "evaluated_at", "model"],
  "properties": {
    "brief_hash": {
      "type": "string",
      "pattern": "^[a-f0-9]{8,}$",
      "description": "평가 대상 입력 식별자"
    },
    "iteration": {
      "type": "string",
      "pattern": "^[0-9]{3}$",
      "description": "평가 대상 초안의 반복 번호"
    },
    "rubric_name": {
      "type": "string",
      "minLength": 1,
      "description": "사용한 루브릭 이름"
    },
    "rubric_scores": {
      "type": "object",
      "description": "축별 점수, 가중치, 가중 합계",
      "required": ["scores", "weights", "weighted_total"],
      "properties": {
        "scores": {
          "type": "object",
          "description": "축별 원점수",
          "additionalProperties": {
            "type": "number",
            "minimum": 0,
            "maximum": 5
          }
        },
        "weights": {
          "type": "object",
          "description": "축별 가중치",
          "additionalProperties": {
            "type": "number",
            "minimum": 0,
            "maximum": 1
          }
        },
        "weighted_total": {
          "type": "number",
          "minimum": 0,
          "maximum": 5,
          "description": "validator가 임계값과 비교할 가중 총점"
        }
      },
      "additionalProperties": false
    },
    "axis_rationales": {
      "type": "object",
      "description": "각 축 점수의 근거",
      "additionalProperties": {
        "type": "string",
        "minLength": 1
      }
    },
    "evaluated_at": {
      "type": "string",
      "format": "date-time",
      "description": "평가 시각"
    },
    "model": {
      "type": "string",
      "minLength": 1,
      "description": "평가를 수행한 모델 또는 실행 프로필"
    },
    "calibration_note": {
      "type": "string",
      "description": "점수 팽창을 막기 위해 적용한 채점 기준"
    }
  },
  "additionalProperties": false
}
```

`eval`은 독립 채점 결과만 담는다. `PASS/REJECT`는 evaluator가 아니라 validator와 runner가 결정하므로 `eval.schema.json`에는 최종 판정 필드를 두지 않는다. 이렇게 해야 평가자의 의견과 기계적 계약 검사를 분리해서 나중에 원인을 추적할 수 있다.

필수 필드:

- `brief_hash`: 평가 대상 draft와 같은 값이어야 한다.
- `iteration`: 평가 대상 draft의 반복 번호다.
- `rubric_name`: 예를 들어 `writing:v1`처럼 평가 기준을 식별한다.
- `rubric_scores`: 축별 점수, 가중치, 가중 합계를 담는다.
- `axis_rationales`: 각 축에 점수를 준 근거다.
- `evaluated_at`: 평가 시각이다.
- `model`: 평가를 수행한 모델 또는 실행 프로필이다.

`rubric_scores`는 축별 점수와 가중 합계를 포함한다. `axis_rationales`는 각 축에 점수를 준 한 줄 근거를 포함한다.

권장 필드:

- `rubric_scores.scores`: `structure`, `evidence`, `sentence`, `originality` 같은 축별 점수다.
- `rubric_scores.weights`: 각 축의 가중치다.
- `rubric_scores.weighted_total`: validator가 임계값과 비교할 총점이다.
- `calibration_note`: 점수 팽창을 막기 위해 어떤 기준으로 엄격하게 봤는지 짧게 남긴다.

평가자 프롬프트에는 반드시 다음 문장을 포함한다.

```text
5점은 드뭅니다. 평균 3.0을 기준으로 채점하세요.
각 축에 점수를 준 근거 한 줄을 함께 출력하세요.
```

예시:

```json
{
  "brief_hash": "a1b2c3d4",
  "iteration": "001",
  "rubric_name": "writing:v1",
  "rubric_scores": {
    "scores": {
      "structure": 3.2,
      "evidence": 2.6,
      "sentence": 3.1,
      "originality": 2.8
    },
    "weights": {
      "structure": 0.3,
      "evidence": 0.3,
      "sentence": 0.2,
      "originality": 0.2
    },
    "weighted_total": 2.94
  },
  "axis_rationales": {
    "structure": "문제의식과 결론은 연결되지만 중간 전환이 약하다.",
    "evidence": "구체적 사건보다 일반 설명이 많아 설득력이 낮다.",
    "sentence": "문장은 읽히지만 일부 문단이 반복된다.",
    "originality": "AI 하네스 관점은 좋지만 개인적 발견이 더 필요하다."
  },
  "evaluated_at": "2026-06-12T15:41:00+09:00",
  "model": "gpt-5",
  "calibration_note": "평균 3.0을 기준으로, 실제 장면 부족을 엄격하게 반영했다."
}
```

### refine_request.schema.json

대상 파일:

```text
{brief_hash}_iter-{iteration}_refine-request.json
```

Refiner에게 넘기는 재작성 요청이다.

Schema 작성 기준:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Pipeline Refine Request",
  "type": "object",
  "required": ["brief_hash", "from_iteration", "to_iteration", "previous_draft", "critique_summary", "revision_directions", "contract_errors", "weak_axes", "original_brief_constraints"],
  "properties": {
    "brief_hash": {
      "type": "string",
      "pattern": "^[a-f0-9]{8,}$",
      "description": "재작성 대상 입력 식별자"
    },
    "from_iteration": {
      "type": "string",
      "pattern": "^[0-9]{3}$",
      "description": "재작성 전 초안 번호"
    },
    "to_iteration": {
      "type": "string",
      "pattern": "^[0-9]{3}$",
      "description": "재작성 후 생성할 초안 번호"
    },
    "previous_draft": {
      "type": "object",
      "description": "재작성할 직전 초안",
      "required": ["iteration", "content"],
      "properties": {
        "iteration": {
          "type": "string",
          "pattern": "^[0-9]{3}$",
          "description": "직전 초안의 반복 번호"
        },
        "content": {
          "type": "string",
          "minLength": 1,
          "description": "직전 초안 본문"
        }
      },
      "additionalProperties": false
    },
    "critique_summary": {
      "type": "string",
      "minLength": 1,
      "description": "비평의 핵심 요약"
    },
    "revision_directions": {
      "type": "array",
      "description": "Refiner가 실제로 반영해야 할 수정 지시",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "contract_errors": {
      "type": "array",
      "description": "validator가 찾은 기계적 계약 위반",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "weak_axes": {
      "type": "array",
      "description": "기준 이하인 루브릭 축 이름",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "original_brief_constraints": {
      "type": "object",
      "description": "원본 입력에서 유지해야 할 제약. 내부 확장 허용",
      "properties": {
        "target_length": {
          "type": "string",
          "description": "목표 길이"
        },
        "tone": {
          "type": "string",
          "description": "유지해야 할 톤"
        },
        "must_include": {
          "type": "array",
          "description": "반드시 포함할 내용",
          "items": {
            "type": "string"
          }
        }
      },
      "additionalProperties": true
    },
    "preserve": {
      "type": "array",
      "description": "다음 초안에서도 유지해야 할 강점",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "do_not_change": {
      "type": "array",
      "description": "사용자 의도, 사실관계, 톤 중 건드리면 안 되는 항목",
      "items": {
        "type": "string",
        "minLength": 1
      }
    },
    "revision_priority": {
      "type": "array",
      "description": "여러 문제가 있을 때 적용할 수정 우선순위",
      "items": {
        "type": "string",
        "minLength": 1
      }
    }
  },
  "additionalProperties": false
}
```

`refine_request`는 Refiner에게 필요한 정보만 좁혀 전달하는 방화벽이다. 총점 원문이나 평가 전문을 넘기면 모델이 글보다 점수 맞추기에 반응할 수 있으므로, 수정에 필요한 약점과 계약 오류만 전달한다.

필수 필드:

- `brief_hash`: 재작성 대상 입력 식별자다.
- `from_iteration`: 어떤 draft를 고치는지 나타낸다.
- `to_iteration`: 다음 draft가 어떤 번호로 생성되어야 하는지 나타낸다.
- `previous_draft`: 직전 초안 본문과 최소 식별 정보를 담는다.
- `critique_summary`: 비평의 핵심 요약이다.
- `revision_directions`: Refiner가 실제로 반영해야 할 지시다.
- `contract_errors`: 길이, 금칙어, 필수 조건 누락 같은 기계적 오류다.
- `weak_axes`: 기준 이하인 루브릭 축 이름만 담는다.
- `original_brief_constraints`: 원본 입력에서 유지해야 할 길이, 톤, 필수 포함 요소다.

`contract_errors`는 validate가 만든 기계적 오류 목록이다. `weak_axes`에는 기준 이하인 평가축 이름만 넣는다. Refine 단계로 넘길 때도 총점보다 이 필드를 우선한다.

권장 필드:

- `preserve`: 다음 초안에서도 유지해야 할 강점이다.
- `do_not_change`: 사용자 의도, 사실관계, 톤 중 건드리면 안 되는 항목이다.
- `revision_priority`: 여러 문제가 있을 때 우선순위를 명시한다.

금지 필드:

- `weighted_total`
- `raw_eval_full_text`
- `generator_hidden_state`

예시:

```json
{
  "brief_hash": "a1b2c3d4",
  "from_iteration": "001",
  "to_iteration": "002",
  "previous_draft": {
    "iteration": "001",
    "content": "처음에는 프롬프트를 잘 쓰는 일이 핵심이라고 생각했다..."
  },
  "critique_summary": "실패 장면이 추상적이라 회고의 밀도가 약하다.",
  "revision_directions": [
    "첫 문단에 실제 실패 사례를 배치한다",
    "다음 실험을 한 가지로 좁혀 결론을 선명하게 만든다"
  ],
  "contract_errors": [
    "length: 980 < 1200"
  ],
  "weak_axes": ["evidence", "originality"],
  "original_brief_constraints": {
    "target_length": "1200-1800 Korean characters",
    "tone": "차분하고 구체적인 개발자 회고",
    "must_include": ["실패 사례", "다음 실험"]
  },
  "preserve": [
    "파일 계약이 프롬프트보다 중요했다는 문제의식"
  ],
  "revision_priority": [
    "구체적 장면 추가",
    "결론 압축",
    "길이 하한 충족"
  ]
}
```

### final.schema.json

대상 파일:

```text
{brief_hash}_final.json
```

최종 통과 산출물이다.

Schema 작성 기준:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Pipeline Final",
  "type": "object",
  "required": ["brief_hash", "final_iteration", "content", "accepted_at", "quality_snapshot", "contract_result", "lineage"],
  "properties": {
    "brief_hash": {
      "type": "string",
      "pattern": "^[a-f0-9]{8,}$",
      "description": "최종본의 원본 입력 식별자"
    },
    "final_iteration": {
      "type": "string",
      "pattern": "^[0-9]{3}$",
      "description": "최종 통과한 draft 번호"
    },
    "content": {
      "type": "string",
      "minLength": 1,
      "description": "사용자가 가져가서 다듬을 최종 초안"
    },
    "accepted_at": {
      "type": "string",
      "format": "date-time",
      "description": "최종 통과 시각"
    },
    "quality_snapshot": {
      "type": "object",
      "description": "통과 시점의 핵심 품질 지표",
      "required": ["rubric_name", "weighted_total", "scores", "weak_axes"],
      "properties": {
        "rubric_name": {
          "type": "string",
          "minLength": 1,
          "description": "최종 통과 판단에 사용한 루브릭 이름"
        },
        "weighted_total": {
          "type": "number",
          "minimum": 0,
          "maximum": 5,
          "description": "최종 통과 당시의 가중 총점"
        },
        "scores": {
          "type": "object",
          "description": "최종 통과 당시의 축별 점수",
          "additionalProperties": {
            "type": "number",
            "minimum": 0,
            "maximum": 5
          }
        },
        "weak_axes": {
          "type": "array",
          "description": "남아 있는 약한 축",
          "items": {
            "type": "string"
          }
        }
      },
      "additionalProperties": false
    },
    "contract_result": {
      "type": "object",
      "description": "validator의 최종 기계적 검사 결과",
      "required": ["verdict", "contract_errors", "checked_rules"],
      "properties": {
        "verdict": {
          "type": "string",
          "enum": ["PASS"],
          "description": "최종 계약 검사 결과"
        },
        "contract_errors": {
          "type": "array",
          "description": "최종 통과 시점의 계약 오류 목록",
          "items": {
            "type": "string"
          }
        },
        "checked_rules": {
          "type": "array",
          "description": "validator가 확인한 규칙 목록",
          "items": {
            "type": "string",
            "minLength": 1
          }
        }
      },
      "additionalProperties": false
    },
    "lineage": {
      "type": "object",
      "description": "최종본이 만들어진 파일 경로 추적 정보",
      "required": ["run_id", "input", "draft", "critique", "eval"],
      "properties": {
        "run_id": {
          "type": "string",
          "minLength": 1,
          "description": "run 디렉토리 식별자"
        },
        "input": {
          "type": "string",
          "minLength": 1,
          "description": "원본 입력 파일 경로"
        },
        "draft": {
          "type": "string",
          "minLength": 1,
          "description": "최종 통과한 draft 파일 경로"
        },
        "critique": {
          "type": "string",
          "minLength": 1,
          "description": "최종 통과 iteration의 critique 파일 경로"
        },
        "eval": {
          "type": "string",
          "minLength": 1,
          "description": "최종 통과 iteration의 eval 파일 경로"
        },
        "refine_request": {
          "type": "string",
          "minLength": 1,
          "description": "최종본 직전 refine request 파일 경로"
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
}
```

`final`은 새 글을 다시 만드는 파일이 아니라 통과한 draft를 고정하는 파일이다. 따라서 최종본은 어떤 입력과 어떤 iteration, 어떤 평가와 검증을 거쳐 통과했는지 설명할 수 있어야 한다.

필수 필드:

- `brief_hash`: 최종본의 입력 식별자다.
- `final_iteration`: 최종 통과한 draft의 반복 번호다.
- `content`: 최종 초안 본문이다.
- `accepted_at`: runner가 최종 통과로 확정한 시각이다.
- `quality_snapshot`: 통과 당시의 루브릭 요약이다.
- `contract_result`: validate가 확인한 기계적 검사 결과다.
- `lineage`: input, draft, critique, eval, refine_request 등 관련 파일 경로다.

`contract_result`에는 validate가 확인한 기계적 검사 결과를 요약한다. `lineage`에는 어떤 iteration의 어떤 파일들을 거쳐 최종본이 되었는지 기록한다.

권장 필드:

- `quality_snapshot.weighted_total`: 최종 통과 당시의 가중 총점이다.
- `quality_snapshot.weak_axes`: 남아 있는 약한 축이 있다면 기록한다.
- `contract_result.checked_rules`: validator가 확인한 규칙 목록이다.
- `lineage.run_id`: run 디렉토리 식별자다.

예시:

```json
{
  "brief_hash": "a1b2c3d4",
  "final_iteration": "002",
  "content": "프롬프트를 더 정교하게 쓰면 문제가 해결될 거라고 믿었다...",
  "accepted_at": "2026-06-12T16:05:00+09:00",
  "quality_snapshot": {
    "rubric_name": "writing:v1",
    "weighted_total": 3.38,
    "scores": {
      "structure": 3.5,
      "evidence": 3.2,
      "sentence": 3.4,
      "originality": 3.3
    },
    "weak_axes": []
  },
  "contract_result": {
    "verdict": "PASS",
    "contract_errors": [],
    "checked_rules": ["schema", "brief_hash", "length", "banned_words", "min_total", "min_axis"]
  },
  "lineage": {
    "run_id": "2026-06-12_a1b2c3d4",
    "input": "a1b2c3d4_input.json",
    "draft": "iter_002/a1b2c3d4_iter-002_draft.json",
    "critique": "iter_002/a1b2c3d4_iter-002_critique.json",
    "eval": "iter_002/a1b2c3d4_iter-002_eval.json",
    "refine_request": "iter_001/a1b2c3d4_iter-001_refine-request.json"
  }
}
```
