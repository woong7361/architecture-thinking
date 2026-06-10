## 목표
- **Step 1. 초안 생성 (Gen)** — 주제와 재료를 넣으면 초안을 뽑는 에이전트
- **Step 2. 비평 (Critique)** — 새 세션에 "시니어 편집자" 역할을 주고 초안의 약점 3개를 뽑게 함
- **Step 3. 퀄리티 평가 (Eval)** — 루브릭 기반 점수(구조 / 근거 / 문장 / 고유성 4축 각 5점) + 점수 근거
- **Step 4. 퇴고 (Refine)** — 비평 + 평가를 근거로 2차 초안 생성. 반복 여부 판정.

4개를 기준 축으로 하는 글쓰기 파이프라인 구축

### 파일 핸드오프 구조 (수정 필요)
```
input.json
 ↓ generator.py
output.json
 ↓ evaluator.py
verdict.json
 ↓ validate.py
 ├─ PASS → next_agent_input.json
 └─ REJECT → regen_request.json ↺ generator.py
```

### 전체 디렉토리 구조
```
pipelines/<pipeline_name>/
├── CLAUDE.md # 이 파이프라인의 규칙 · 도메인 언어 · 금지 행동
├── generator.py # P1: 생성자. evaluator.py를 import 금지
├── evaluator.py # P1: 평가자. generator 히스토리 미참조
├── validate.py # P3: 자동 검증 계약 (Ch4 §2-6)
├── rubric.yaml # P5: 가치 축 7개 + 가중치 (Ch4 §4-2-b)
├── prompts/
│ ├── gen_system.md # Generator용 시스템 프롬프트
│ └── eval_system.md # Evaluator용 시스템 프롬프트
├── schemas/
│ ├── input.schema.json # 브리프 스키마
│ ├── output.schema.json # 산출물 스키마
│ └── verdict.schema.json # 판정 스키마
├── runs/
│ ├── 2026-04-24_a1b2c3d4/ # 한 건당 하나의 디렉토리
│ │ ├── 01_input.json
│ │ ├── 02_output.json
│ │ ├── 03_verdict.json
│ │ └── 04_next.json # PASS 시에만 생성
│ └── 2026-04-24_e5f6a7b8/
│ ├── 01_input.json
│ ├── 02_output.json
│ ├── 03_verdict.json
│ └── 99_regen_request.json # REJECT 시 재생성 요청
├── archive/ # 30일 지난 runs 이동
└── README.md # 운영자 개요 · 티어 · HOC 사이클 기록
```

### 파일 이름 원칙
파일명에 timestamp + brief_hash가 들어갈 것
확장자는 .json 단일
디렉토리는 단계별로 분리: runs/{hash}/01_input.json , 02_output.json , 03_verdict.json ,
04_next.json

## 프롬프트 규칙
```
Generator의 시스템 프롬프트에는 당신은 이런 스타일의 콘텐츠를 만드는 창작자입니다
Evaluator의 시스템 프롬프트에는 당신은 이런 콘텐츠의 품질을 1~5점으로 평가하는 심사자입니다. 창작자가 아닙니다
    이 산출물을 당신이 만들었다고 가정하지 마세요. 다른 사람이 만든 콘텐츠를 심사하는 입장입니다. 5점
은 드뭅니다. 평균 3.0을 기준으로 채점하세요
```

## 평가자 프롬프트의 두 문장
```
"5점은 드뭅니다. 평균 3.0을 기준으로 채점하세요."
"각 축에 점수를 준 근거 한 줄을 함께 출력하세요."
```
첫 문장은 점수 팽창을 억제합니다. 두 번째 문장은 근거 요구가 곧 채점 품질이기 때문입니다. 근거를 못 쓰면
점수도 못 줍니다. 이 한 문장이 “왜 5점인지 모르는 5점”을 사라지게 합니다.

## 파이프 라인 흐름
1. generator.py 가 output.json 생성
2. evaluator.py 가 rubric.yaml 을 적용, rubric_scores 를 verdict.json 에 기록
3. validate.py 가 output.json + rubric_scores 를 받아 contract 검사
 └─ JSON 스키마 체크
 └─ 길이 체크
 └─ 금칙어 체크
 └─ 품질 하한 체크
4. verdict.json 의 최종 verdict:
 - PASS → next_agent_input.json 으로 핸드오프
 - REJECT → errors 배열과 함께 regen_request.json 으로 Generator 재호출

재호출 시 중요한 점. Generator에게 전달하는 regen_request.json에는 errors 배열과 rubric에서 3점 이하로
찍힌 축 목록이 들어갑니다. 총점을 알려 주지 않습니다. 총점을 알려 주면 Generator가 그 숫자를 맞추려 하면
서 축간 균형이 깨집니다. 약점만 알려 주는 편이 낫습니다.