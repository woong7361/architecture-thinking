# Recruiting Harness Pipeline

## 현재 문서 상태

이 디렉토리는 `writing-harness-pipeline`을 복사해서 만든 초기 실험 공간이다.

아직 `runner.py`, `schemas/`, `prompts/`, `AGENTS.md`에는 글쓰기 하네스의 이름과 계약이 남아 있다. 이 문서는 채용공고 분석 하네스의 현재 설계 기준이다. 이후 구현 단계에서 schema, prompt, stage 이름, runner 흐름을 이 문서에 맞춰 정리한다.

## 목적

이 파이프라인은 채용공고 20~30개를 사람이 하나씩 정독하지 않고도, 공고 문장에 드러난 표면 요구와 그 뒤의 숨은 기대치를 구조화해서 읽기 위한 하네스다.

핵심 관심사는 단순 키워드 빈도보다 다음에 있다.

- 회사가 어떤 사람을 원하는지 읽는다.
- 어떤 역량을 직접 요구하고, 어떤 역량을 간접적으로 암시하는지 분리한다.
- "Kafka/Redis 경험" 같은 기술명 뒤에 숨은 대용량 트래픽, 캐시 전략, 비동기 처리, 운영 경험의 가능성을 추론한다.
- 교육, 의료, 금융, 커머스처럼 도메인에 따라 같은 표현이 다르게 읽히는 지점을 반영한다.
- 회사 규모별로 채용 언어가 어떻게 달라지는지 비교한다.
- 추론이 원문 근거 없이 부풀지 않도록 source spans, evidence id, confidence를 남긴다.

즉, 이 프로젝트의 1차 산출물은 **회사 규모별 채용 언어 차이 리포트**이고, 2차 산출물은 그 리포트가 어떤 공고 문장과 어떤 추론에서 나왔는지 추적 가능한 **반복 가능한 채용공고 분석 시스템**이다.

## MVP 범위

초기 버전은 4개의 LLM stage로 간소화한다.

```text
1. Keyword Extract
   3~5개 채용공고를 한 번에 읽고 공고별 원문 문장, 표면 키워드, 표면 요구사항을 추출한다.

2. Frequency & Subtext Analyze
   전체 keyword 결과를 모아 키워드/역량 빈도와 문장 뒤의 숨은 기대치를 분석한다.

3. Evaluate
   분석 결과가 원문 근거와 연결되는지, 과잉 일반화가 있는지 평가한다.

4. Report
   평가를 통과한 분석 결과를 회사 규모별 채용 언어 차이 리포트로 정리한다.
```

4단계 모두 LLM을 호출한다. 다만 각 stage가 맡는 판단의 종류는 분리한다. Keyword Extractor는 공고별 근거를 뽑고, Frequency & Subtext Analyst는 전체 패턴을 읽고, Evaluator는 근거 품질을 검토하고, Reporter는 최종 문서로 편집한다.

`validate.py`는 별도 LLM stage가 아니다. 각 LLM 산출물이 schema와 evidence contract를 지키는지 확인하는 기계적 게이트로만 둔다.

초기에는 크롤러도 만들지 않는다. 입력은 복붙한 공고 텍스트 또는 사람이 저장한 JSON을 기준으로 한다. 크롤링은 사이트별 정책, 로그인, 동적 렌더링, 중복 제거 문제가 생기므로 MVP 이후로 미룬다.

## 전체 흐름

```text
{brief_hash}_input.json
 ↓ validate input schema
 ↓ keyword extract batch 001
{brief_hash}_batch-001_keywords.json
 ↓ keyword extract batch 002
{brief_hash}_batch-002_keywords.json
 ↓ ...
{brief_hash}_batch-N_keywords.json
 ↓ validate keyword extraction schema
 ↓ frequency & subtext analyze
{brief_hash}_analysis.json
 ↓ validate analysis schema
 ↓ evaluate analysis
{brief_hash}_eval.json
 ↓ validate eval schema + evidence contract
 ↓ report
{brief_hash}_report.json
{brief_hash}_report.md
 ↓ validate report schema
 ↓ final
{brief_hash}_final.json
```

공고 원문은 한 번에 20~30개를 모두 LLM에 넣지 않는다. 3~5개 단위로 keyword extraction을 수행한다. 이렇게 하면 LLM 호출 수를 줄이면서도 공고별 근거 추적성을 유지할 수 있다.

30개 공고 기준 예상 LLM 호출은 다음 정도다.

```text
keyword extract: 6~10 calls
frequency & subtext analyze: 1 call
evaluate: 1 call
report: 1 call
```

총 9~13회 정도의 LLM 호출을 예상한다. 비용이 부담되면 batch size를 5에서 6~8로 키울 수 있지만, 공고별 문장 근거가 흐려지는지 확인해야 한다.

## 역할 경계

### Keyword Extractor

- 입력: `{brief_hash}_input.json`에서 runner가 잘라낸 3~5개 공고 batch
- 출력: `{brief_hash}_batch-{batch_no}_keywords.json`
- 책임: 공고별로 원문 문장, 표면 키워드, 표면 요구사항, 공고 내 섹션을 추출한다.
- 금지: 전체 입력 묶음의 빈도 분석, 회사 규모별 일반화, 최종 리포트 작성, 최종 판정.

### Frequency & Subtext Analyst

- 입력: 모든 keyword artifact
- 출력: `{brief_hash}_analysis.json`
- 책임: 키워드와 capability_type의 반복 패턴, 문장 뒤의 숨은 기대치, 근거 분포를 분석한다.
- 금지: 최종 리포트 문체로 포장하기, 평가자처럼 PASS/REJECT 판정하기, source signal 없는 claim 만들기.

### Evaluator

- 입력: `{brief_hash}_analysis.json`, 모든 keyword artifact
- 출력: `{brief_hash}_eval.json`
- 책임: 분석 결과가 원문 근거와 연결되는지, 표본 수에 비해 과잉 일반화하지 않았는지, alternative reading을 충분히 남겼는지 평가한다.
- 금지: 리포트 재작성, 새로운 시장 분석 추가, 점수만 주고 근거를 생략하기.

### Reporter

- 입력: `{brief_hash}_analysis.json`, `{brief_hash}_eval.json`
- 출력: `{brief_hash}_report.json`, `{brief_hash}_report.md`
- 책임: 평가를 반영해 회사 규모별 채용 언어 차이, 반복 패턴, 대표 근거, 지원자 관점 시사점을 정리한다.
- 금지: eval에서 위험하다고 표시한 주장을 확정적으로 쓰기, source signal 없는 주요 주장 만들기, 표본 수 한계를 숨기기.

### Validator

- 입력: 검사 대상 JSON, 해당 schema, evidence contract
- 출력: PASS/REJECT/ERROR 성격의 검사 결과
- 책임: schema, brief hash, posting id, item id, signal id, source spans, evidence reference 같은 기계적 계약만 검사한다.
- 금지: 채용 시장에 대한 주관적 해석, 리포트 문장 재작성, 점수형 품질 평가.

## Schema 확정안

이 섹션은 이후 `schemas/*.schema.json`과 stage prompt를 만들 때 기준이 되는 계약이다. 입력에는 판단을 넣지 않고, `Keyword Extract`는 표면 추출만 하며, 추론은 `Frequency & Subtext Analyze`부터 허용한다.

## Input Schema

입력은 사람이 제공한 원문과 메타데이터만 담는다. 키워드, 역량, 숨은 기대치, 평가, 회사 규모 추정 같은 판단 결과를 넣지 않는다.

```json
{
  "brief_hash": "a1b2c3d4",
  "created_at": "2026-06-19T21:00:00+09:00",
  "analysis_goal": "백엔드 개발자 채용공고에서 회사 규모별로 원하는 사람과 요구 역량의 언어 차이를 분석한다.",
  "batch_size": 5,
  "postings": [
    {
      "posting_id": "p001",
      "company_name": "Example Startup",
      "company_size": "startup",
      "domain": "education",
      "domain_note": "AI 교육, 에듀테크, 부트캠프",
      "role_title": "Backend Engineer",
      "reference_link": "https://example.com/jobs/1",
      "captured_at": "2026-06-19T21:00:00+09:00",
      "raw_text": "채용공고 원문..."
    }
  ]
}
```

필수 필드:

- `brief_hash`
- `created_at`
- `analysis_goal`
- `batch_size`
- `postings`
- `postings[].posting_id`
- `postings[].company_name`
- `postings[].company_size`
- `postings[].domain`
- `postings[].role_title`
- `postings[].reference_link`
- `postings[].raw_text`

`company_size`와 `domain`은 모델이 추정하지 않고 입력에서 받은 값을 그대로 쓴다. 모르면 `unknown`을 사용한다.

권장 `company_size` 값:

```text
startup
scaleup
mid_size
enterprise
unknown
```

권장 `domain` 값:

```text
education
healthcare
finance
commerce
saas
game
ai
content
public_sector
manufacturing
mobility
unknown
```

## Keyword Extract Schema

`Keyword Extractor`는 3~5개 공고 batch를 입력받아 원문에서 보이는 표면 항목만 추출한다. 한 요구사항이 여러 문장이나 여러 bullet에 걸쳐 있을 수 있으므로 단일 `source_sentence`를 쓰지 않고 `source_spans` 배열을 사용한다.

```json
{
  "brief_hash": "a1b2c3d4",
  "batch_no": "001",
  "stage": "keyword_extract",
  "postings": [
    {
      "posting_id": "p001",
      "company_name": "Example Startup",
      "company_size": "startup",
      "domain": "education",
      "role_title": "Backend Engineer",
      "reference_link": "https://example.com/jobs/1",
      "items": [
        {
          "item_id": "p001-k001",
          "item_type": "technical_stack",
          "section": "preferred_qualifications",
          "source_spans": [
            {
              "text": "Kafka/Redis 경험이 있으신 분"
            }
          ],
          "terms": ["kafka", "redis"]
        }
      ],
      "warnings": []
    }
  ]
}
```

필수 필드:

- `brief_hash`
- `batch_no`
- `stage`
- `postings[].posting_id`
- `postings[].company_name`
- `postings[].company_size`
- `postings[].domain`
- `postings[].role_title`
- `postings[].reference_link`
- `postings[].warnings`
- `postings[].items[].item_id`
- `postings[].items[].item_type`
- `postings[].items[].section`
- `postings[].items[].source_spans`
- `postings[].items[].source_spans[].text`
- `postings[].items[].terms`

`section`과 `item_type`은 서로 다른 정보다.

- `section`: 해당 문장이나 bullet이 공고 원문 안에서 놓인 위치/문맥이다. 예를 들어 `required_qualifications`, `preferred_qualifications`, `responsibilities`, `tech_stack`, `culture`, `benefits`, `unknown`처럼 쓴다.
- `item_type`: 해당 항목이 어떤 종류의 요구인지 분류한 값이다. 예를 들어 같은 `preferred_qualifications` 섹션 안에서도 기술 스택 요구는 `technical_stack`, 협업 요구는 `collaboration`, 도메인 경험 요구는 `domain_context`가 될 수 있다.

예시:

```json
{
  "item_id": "p001-k001",
  "item_type": "technical_stack",
  "section": "preferred_qualifications",
  "source_spans": [
    {
      "text": "Kafka/Redis 경험이 있으신 분"
    }
  ],
  "terms": ["kafka", "redis"]
}
```

이 예시는 "우대사항 섹션에 있던 기술 스택 요구"를 뜻한다. `section`은 원문 위치이고, `item_type`은 요구 유형이다.

권장 `section` 값:

```text
required_qualifications
preferred_qualifications
responsibilities
tech_stack
culture
benefits
company_intro
hiring_process
unknown
```

권장 `item_type` 값:

```text
technical_stack
required_experience
responsibility
collaboration
product_context
domain_context
culture
process
seniority
benefit
other
```

Keyword Extract 금지 필드:

- `inferred_expectation`
- `confidence`
- `reasoning`
- `alternative_reading`
- `capability_type`
- `company_size_claim`
- `domain_claim`

## Analysis Schema

`Frequency & Subtext Analyst`는 모든 keyword artifact를 모아 빈도와 행간을 분석한다. 이 단계에서 처음으로 숨은 기대치와 추론 근거를 만든다.

```json
{
  "brief_hash": "a1b2c3d4",
  "stage": "analyze",
  "sample_summary": {
    "posting_count": 30,
    "company_size_counts": {
      "startup": 8,
      "scaleup": 9,
      "mid_size": 7,
      "enterprise": 6,
      "unknown": 0
    },
    "domain_counts": {
      "education": 30,
      "healthcare": 0,
      "finance": 0,
      "commerce": 0,
      "saas": 0,
      "game": 0,
      "ai": 0,
      "content": 0,
      "public_sector": 0,
      "manufacturing": 0,
      "mobility": 0,
      "unknown": 0
    }
  },
  "frequency_summary": {
    "term_frequencies": [
      {
        "term": "Kafka",
        "count": 3,
        "posting_ids": ["p001", "p004", "p011"]
      }
    ],
    "item_type_counts": {
      "technical_stack": 18,
      "required_experience": 0,
      "responsibility": 0,
      "collaboration": 11,
      "product_context": 0,
      "domain_context": 0,
      "culture": 0,
      "process": 0,
      "seniority": 0,
      "benefit": 0,
      "other": 0
    }
  },
  "signals": [
    {
      "signal_id": "s001",
      "source_item_ids": ["p001-k001", "p004-k002", "p011-k006"],
      "evidence_distribution": {
        "unique_posting_count": 3,
        "supporting_item_count": 3,
        "company_size_counts": {
          "startup": 3,
          "scaleup": 0,
          "mid_size": 0,
          "enterprise": 0,
          "unknown": 0
        },
        "domain_counts": {
          "education": 3,
          "healthcare": 0,
          "finance": 0,
          "commerce": 0,
          "saas": 0,
          "game": 0,
          "ai": 0,
          "content": 0,
          "public_sector": 0,
          "manufacturing": 0,
          "mobility": 0,
          "unknown": 0
        },
        "posting_ids": ["p001", "p004", "p011"]
      },
      "surface_pattern": "Kafka/Redis 등 메시징, 캐시 기술 경험 반복",
      "capability_type": "scale_operation",
      "inferred_expectation": "대용량 트래픽, 비동기 처리, 캐시 전략을 실제 운영해본 사람을 원할 가능성이 있다.",
      "confidence": "medium",
      "reasoning": "여러 공고에서 Kafka, Redis가 기술명으로 반복되며 일부 문맥에서 트래픽, 성능, 운영 표현과 함께 등장한다.",
      "alternative_reading": "현재 사용 중인 기술 스택에 바로 적응할 수 있는 사람을 찾는 의미일 수도 있다.",
      "limitations": ["표본 수가 3건이라 강한 일반화는 어렵다."]
    }
  ],
  "subtext_readings": [
    {
      "reading_id": "r001",
      "source_item_ids": ["p023-k008", "p033-k002", "p040-k011"],
      "linked_signal_ids": ["s001"],
      "evidence_distribution": {
        "unique_posting_count": 3,
        "supporting_item_count": 3,
        "company_size_counts": {
          "startup": 0,
          "scaleup": 2,
          "mid_size": 1,
          "enterprise": 0,
          "unknown": 0
        },
        "domain_counts": {
          "education": 1,
          "healthcare": 0,
          "finance": 0,
          "commerce": 0,
          "saas": 2,
          "game": 0,
          "ai": 0,
          "content": 0,
          "public_sector": 0,
          "manufacturing": 0,
          "mobility": 0,
          "unknown": 0
        },
        "posting_ids": ["p023", "p033", "p040"]
      },
      "surface_phrase_group": "주도성 / 문제 해결 / 먼저 제안",
      "representative_surface_phrases": [
        "원활하고 주도적인 커뮤니케이션 능력",
        "프로젝트를 주도적으로 이끌어본 경험이 있으신 분",
        "기존 코드/시스템의 비효율이나 구조적 문제를 발견하고 개선을 주도해본 경험"
      ],
      "plain_translation": "시키는 일만 기다리기보다 문제를 먼저 발견하고 해결 방향을 제안하는 사람을 원할 가능성이 있다.",
      "possible_team_context": "업무 범위가 명확히 쪼개져 있지 않거나 리더가 모든 문제를 직접 정의해주기 어려운 환경일 수 있다.",
      "candidate_opportunity": "지원자는 주도적이라는 표현보다 문제 발견, 제안, 실행, 결과까지 이어진 사례를 보여주는 편이 강하다.",
      "confidence": "medium",
      "reasoning": "주도, 먼저 제안, 문제 발견, 개선 주도 표현이 여러 공고에서 반복된다.",
      "alternative_reading": "일부는 실제 조직 결핍보다 일반적인 인재상 문구일 수 있다.",
      "limitations": ["팀 내부 상황은 공고만으로 단정할 수 없다."]
    }
  ]
}
```

필수 필드:

- `brief_hash`
- `stage`
- `sample_summary`
- `frequency_summary`
- `signals`
- `signals[].signal_id`
- `signals[].source_item_ids`
- `signals[].evidence_distribution`
- `signals[].surface_pattern`
- `signals[].capability_type`
- `signals[].inferred_expectation`
- `signals[].confidence`
- `signals[].reasoning`
- `signals[].alternative_reading`
- `signals[].limitations`
- `subtext_readings`
- `subtext_readings[].reading_id`
- `subtext_readings[].source_item_ids`
- `subtext_readings[].linked_signal_ids`
- `subtext_readings[].evidence_distribution`
- `subtext_readings[].surface_phrase_group`
- `subtext_readings[].representative_surface_phrases`
- `subtext_readings[].plain_translation`
- `subtext_readings[].possible_team_context`
- `subtext_readings[].candidate_opportunity`
- `subtext_readings[].confidence`
- `subtext_readings[].reasoning`
- `subtext_readings[].alternative_reading`
- `subtext_readings[].limitations`

`evidence_distribution`은 signal이 적용되는 일반화 범위가 아니라, 이 입력 묶음 안에서 해당 패턴이 어디에서 관찰되었는지를 나타낸다. `unique_posting_count`는 몇 개 공고에서 관찰됐는지, `supporting_item_count`는 근거 item이 총 몇 개인지를 뜻한다. 예를 들어 `company_size_counts.startup = 3`은 "스타트업 전체가 그렇다"가 아니라 "이번 입력에서 startup으로 표시된 공고 3건에서 관찰되었다"는 뜻이다.

`subtext_readings`는 전체 keyword item을 모두 번역하는 목록이 아니라, 반복 빈도와 해석 가치가 높은 채용 문구 묶음을 최대 10개만 선정한 행간 해석이다. `representative_surface_phrases`는 keyword item의 `source_spans[].text`에서 가져온 대표 문구이고, `plain_translation`, `possible_team_context`, `candidate_opportunity`는 각각 실제 기대, 가능한 팀 맥락, 지원자 관점의 기회를 분리한다. 팀 내부 사정은 공고만으로 단정하지 않고 가설로 표현한다.

권장 `capability_type` 값:

| capability_type | 의미 |
| --- | --- |
| `scale_operation` | 대용량 트래픽, 성능, 캐시, 메시징, 장애 대응, 운영 안정성 |
| `ownership` | 모호한 문제를 스스로 정의하고 끝까지 끌고 가는 역량 |
| `product_sense` | 기술 선택을 제품 가치, 사용자 문제, 실험과 연결하는 역량 |
| `collaboration` | 협업, 커뮤니케이션, 문서화, 이해관계자 조율 |
| `execution_speed` | 빠른 실험, 빠른 출시, 짧은 피드백 루프 |
| `architecture_design` | 시스템 설계, 모듈 경계, 확장 가능한 구조 설계 |
| `quality_engineering` | 테스트, 코드 품질, 리뷰, 유지보수성 |
| `domain_adaptation` | 특정 도메인, 레거시, 규제, 비즈니스 맥락에 적응하는 역량 |
| `leadership` | 기술 리딩, 멘토링, 의사결정, 우선순위 조정 |
| `learning_agility` | 낯선 기술이나 문제를 빠르게 학습하고 적용하는 역량 |

이 분류는 고정 진리가 아니다. 여러 run에서 `other`가 자주 나오면 분류 체계를 조정한다.

## Eval Schema

`Evaluator`는 분석 결과가 원문 근거와 연결되는지, 표본 수에 비해 과잉 일반화하지 않았는지, 리포트로 넘기면 위험한 주장이 무엇인지 평가한다. 리포트를 다시 쓰거나 새로운 분석을 추가하지 않는다.

```json
{
  "brief_hash": "a1b2c3d4",
  "stage": "evaluate",
  "verdict": "pass_with_cautions",
  "signal_reviews": [
    {
      "signal_id": "s001",
      "evidence_status": "supported",
      "generalization_risk": "medium",
      "domain_fit": "supported",
      "company_size_fit": "weak",
      "comment": "source_item_ids는 충분하지만 startup 표본 3건만으로 강한 일반화는 어렵다.",
      "required_action": "리포트에서는 '경향'이 아니라 '관찰'로 표현한다."
    }
  ],
  "unsafe_claims": [
    {
      "target_id": "s001",
      "reason": "표본 수 대비 표현이 강하다.",
      "suggested_handling": "삭제하거나 낮은 확신도 표현으로 바꾼다."
    }
  ],
  "report_guidance": [
    "표본 수가 작은 회사 규모는 별도 한계 문장으로 처리한다."
  ]
}
```

필수 필드:

- `brief_hash`
- `stage`
- `verdict`
- `signal_reviews`
- `unsafe_claims`
- `report_guidance`

허용 `verdict` 값:

```text
pass
pass_with_cautions
revise
reject
```

`revise`나 `reject`가 나와도 MVP에서는 자동 refine loop를 돌리지 않는다. `Reporter`가 `report_guidance`와 `unsafe_claims`를 반영해 위험한 주장을 낮은 확신도 표현으로 바꾸거나 제외한다.

## Report Schema

`Reporter`는 analysis와 eval을 입력으로 받아 사람이 읽을 Markdown과 추적 가능한 JSON을 함께 만든다. 새로운 signal이나 새로운 통계 판단을 만들지 않는다.

```json
{
  "brief_hash": "a1b2c3d4",
  "stage": "report",
  "title": "회사 규모별 채용 언어 차이 리포트",
  "markdown": "# 회사 규모별 채용 언어 차이 리포트\n\n...",
  "sample_summary": {
    "posting_count": 30,
    "company_size_counts": {
      "startup": 8,
      "scaleup": 9,
      "mid_size": 7,
      "enterprise": 6
    },
    "domain_counts": {
      "education": 30
    }
  },
  "claims": [
    {
      "claim_id": "c001",
      "claim": "스타트업 공고에서는 ownership과 execution_speed 신호가 반복적으로 관찰된다.",
      "claim_type": "company_size_pattern",
      "claim_scope": {
        "company_sizes": ["startup"],
        "domains": ["education"],
        "scope_note": "이 입력 묶음에 포함된 education 도메인 startup 공고에서 관찰된 경향이다."
      },
      "evidence_distribution": {
        "unique_posting_count": 5,
        "supporting_item_count": 7,
        "company_size_counts": {
          "startup": 5
        },
        "domain_counts": {
          "education": 5
        }
      },
      "evidence_signal_ids": ["s002", "s005"],
      "confidence": "medium",
      "limitation": "표본 수가 적어 전체 스타트업 시장으로 일반화하기는 어렵다."
    }
  ],
  "company_size_insights": [
    {
      "insight_id": "size-001",
      "comparison": "startup 공고에서는 ownership과 execution_speed 신호가 상대적으로 더 눈에 띈다.",
      "company_sizes": ["startup", "enterprise"],
      "evidence_signal_ids": ["s002", "s005", "s007"],
      "confidence": "low",
      "limitation": "enterprise 표본 수가 적어 강한 차이로 단정하지 않는다."
    }
  ],
  "sections": [
    {
      "heading": "분석 범위와 한계",
      "summary": "교육 도메인 백엔드 공고 30개를 대상으로 했다."
    }
  ],
  "lineage": {
    "analysis": "a1b2c3d4_analysis.json",
    "eval": "a1b2c3d4_eval.json"
  }
}
```

Markdown 권장 구조:

```text
# 회사 규모별 채용 언어 차이 리포트

## 분석 범위와 한계
## 전체 패턴 요약
## 회사 규모별 차이
## 반복적으로 나타난 숨은 기대치
## 표면 기술 요구와 실제 기대 역량
## 지원자 관점 시사점
## 근거 signal 목록
```

필수 필드:

- `brief_hash`
- `stage`
- `title`
- `markdown`
- `sample_summary`
- `claims`
- `company_size_insights`
- `claims[].claim_id`
- `claims[].claim`
- `claims[].evidence_signal_ids`
- `claims[].confidence`
- `claims[].limitation`
- `lineage`

Report의 `claim_scope`는 최종 리포트에서 claim을 어디까지 말할지 제한하는 필드다. Analysis의 `evidence_distribution`을 보고 Reporter가 보수적으로 정한다. 표본이 작으면 `claim_scope.scope_note`와 `limitation`에 반드시 한계를 적는다.

`company_size_insights`는 회사 규모별 비교를 담는 유일한 위치다. Analysis 단계에서는 비교 결론을 만들지 않고, Report 단계에서 analysis signal과 eval guidance를 근거로 제한적으로 작성한다.

## Final Schema

`final`은 새 분석을 만들지 않고, 통과한 report와 lineage를 고정한다.

```json
{
  "brief_hash": "a1b2c3d4",
  "accepted_at": "2026-06-19T22:00:00+09:00",
  "final_report_json": "a1b2c3d4_report.json",
  "final_report_markdown": "a1b2c3d4_report.md",
  "contract_result": {
    "verdict": "PASS",
    "checked_rules": ["schema", "lineage", "evidence_references"],
    "contract_errors": []
  },
  "lineage": {
    "input": "a1b2c3d4_input.json",
    "keyword_batches": [
      "a1b2c3d4_batch-001_keywords.json",
      "a1b2c3d4_batch-002_keywords.json"
    ],
    "analysis": "a1b2c3d4_analysis.json",
    "eval": "a1b2c3d4_eval.json",
    "report": "a1b2c3d4_report.json"
  }
}
```

## Validate 기준

기계적 validate는 해석 품질을 판단하지 않는다. 파일이 다음 단계로 넘어갈 수 있는지 계약만 확인한다.

- input에는 `postings[].raw_text`, `postings[].company_size`, `postings[].domain`, `postings[].reference_link`가 있어야 한다.
- keyword item에는 `item_id`, `source_spans[].text`, `terms`가 있어야 한다.
- keyword item에는 `inferred_expectation`, `confidence`, `reasoning`, `alternative_reading`, `capability_type`이 없어야 한다.
- analysis signal에는 `source_item_ids`, `evidence_distribution`, `surface_pattern`, `capability_type`, `inferred_expectation`, `confidence`, `reasoning`, `alternative_reading`이 있어야 한다.
- analysis subtext_reading에는 `source_item_ids`, `linked_signal_ids`, `evidence_distribution`, `representative_surface_phrases`, `plain_translation`, `possible_team_context`, `candidate_opportunity`, `confidence`, `alternative_reading`이 있어야 한다.
- eval의 `signal_reviews[].signal_id`는 analysis의 signal id를 참조해야 한다.
- report의 `claims[].evidence_signal_ids`는 analysis의 signal id를 참조해야 한다.
- final의 lineage 파일들은 같은 `brief_hash`를 공유해야 한다.

## 파일 이름 규칙 확정안

기존 writing harness의 `{brief_hash}_iter-{iteration}_{artifact}.json` 규칙은 채용 분석에는 맞지 않는다. 새 규칙은 brief와 batch 중심으로 잡는다.

```text
{brief_hash}_input.json
{brief_hash}_batch-{batch_no}_keywords.json
{brief_hash}_analysis.json
{brief_hash}_eval.json
{brief_hash}_report.json
{brief_hash}_report.md
{brief_hash}_final.json
{brief_hash}_failed.json
```

예시:

```text
a1b2c3d4_input.json
a1b2c3d4_batch-001_keywords.json
a1b2c3d4_batch-002_keywords.json
a1b2c3d4_analysis.json
a1b2c3d4_eval.json
a1b2c3d4_report.json
a1b2c3d4_report.md
a1b2c3d4_final.json
```

Run 디렉토리는 기존처럼 날짜와 hash를 함께 쓴다.

```text
runs/2026-06-19_a1b2c3d4/
```

## 구현 순서

1. `AGENTS.md`를 recruiting harness 기준으로 수정한다.
2. `schemas/input.schema.json`을 brief input 계약으로 바꾼다.
3. `schemas/keyword_extraction.schema.json`, `schemas/analysis.schema.json`, `schemas/eval.schema.json`, `schemas/report.schema.json`, `schemas/final.schema.json`을 만든다.
4. 기존 `generator`, `critique`, `evaluator`, `refine` stage를 `keyword_extract`, `analyze`, `evaluate`, `report`로 정리한다.
5. `runner.py`를 4단계 LLM 흐름으로 단순화한다.
6. `validate.py`를 score threshold 중심에서 evidence contract와 stage별 schema 중심으로 바꾼다.
7. 샘플 `input.json`에 3~5개 공고만 넣어 MVP를 먼저 돌린다.
8. 안정화 후 20~30개 공고로 확장한다.

## 보류할 것

다음 기능은 MVP에 넣지 않는다.

- 채용 사이트 크롤러
- 회사 규모 자동 추정
- 반복 refine 루프
- 시장 전체에 대한 강한 일반화
- 지원자 맞춤 이력서/자기소개서 생성

먼저 "공고 문장 -> signal -> 회사 규모별 리포트"의 근거 추적성이 살아 있는지 확인한다.

