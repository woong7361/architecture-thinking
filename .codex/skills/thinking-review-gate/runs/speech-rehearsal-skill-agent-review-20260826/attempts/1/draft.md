# 결론

`skill 하나 + 구조 코치 custom agent 하나`로 만들되, 역할 프롬프트나 rubric 하나에 전사와 계산과 코칭을 모두 맡기지 않는 구성이 좋다.

```text
speech-rehearsal skill
  1. 입력과 실행 흐름 관리
  2. gpt-transcribe로 전사
  3. 코드로 필러와 속도 계산
  4. speech_coach agent에 구조 판단 위임
  5. 하나의 feedback.md로 합성
```

rubric은 필요하다. 다만 rubric은 구조 코칭에만 쓴다. 필러 횟수와 속도는 rubric으로 채점하지 않고 결정적 코드가 계산해야 한다. Role은 코치의 책임과 금지선을 정하고, rubric은 같은 transcript에 같은 평가 기준을 적용하게 한다.

# 제안 파일 구조

```text
.codex/
├── agents/
│   └── speech-coach.toml
└── skills/
    └── speech-rehearsal/
        ├── SKILL.md
        ├── agents/
        │   └── openai.yaml
        ├── scripts/
        │   ├── run_rehearsal.py
        │   ├── transcribe.py
        │   └── measure_delivery.py
        ├── references/
        │   └── structure-rubric.yaml
        └── tests/
            ├── fixtures/
            └── test_measure_delivery.py
```

`agents/openai.yaml`은 skill UI metadata이고 실제 코치 role은 아니다. 실제 Codex custom agent role은 프로젝트 범위의 `.codex/agents/speech-coach.toml`에 둔다.

# 입력 계약

skill은 다음 두 입력 모드를 지원한다.

1. media mode: 음성·영상 파일 하나를 받아 `gpt-transcribe`로 전사한 뒤 분석한다.
2. transcript mode: 기존 transcript JSON 또는 텍스트를 받아 전사를 건너뛰고 분석한다.

공통 선택 입력:

- 발표 계획: 한 줄 메시지, 구간별 목적, 목표 시간
- 목표 청자
- 목표 전체 시간

D-4 경로나 문장을 skill에 하드코딩하지 않는다. 이번 실행에서만 D-4 문서를 발표 계획 입력으로 넘긴다. 이렇게 해야 skill은 다른 발표에도 재사용된다.

# 각 구성 요소의 책임

## 1. 전사기

`gpt-transcribe`에 파일 하나를 보낸다. 사용자는 파일을 나누지 않는다.

- `language=ko`
- `keywords=[음, 어, 그]`
- 들린 말을 정리하거나 교정하지 말고 머뭇거림과 반복을 보존하라는 한국어 prompt
- `chunking_strategy=auto`
- 가능하면 `verbose_json`과 word/segment timestamps

원본 API 응답은 `transcript.raw.json`으로 그대로 보존한다. 읽기 좋게 다듬은 transcript를 필러 계측 입력으로 사용하지 않는다. timestamp 조합이 현재 모델에서 동작하는지는 첫 live smoke test에서 확인하고, 지원되지 않으면 전체 duration 기반 속도만 계산하거나 다른 timestamp adapter를 선택한다.

## 2. 결정적 계측기

LLM이 아니라 Python 코드가 계산한다.

- 전체 녹화 시간
- 전체 어절 수/분
- 실제 발화 시간 기준 어절 수/분
- 한국어 글자 수/분 보조 지표
- 30초 구간별 속도 변화
- `음`, `어` 확정 후보 횟수와 분당 횟수
- `그`의 전체 출현을 `review_required` 후보로 분리
- 각 후보의 timestamp와 주변 문맥

`그`는 자동으로 모두 필러라고 확정하지 않는다. transcript에 timestamp가 없으면 후보 문맥은 제공하되 구간별 속도와 정확한 발생 시각은 `unavailable`로 표시한다.

## 3. speech_coach agent

추천 role:

```text
당신은 발표 내용을 대신 쓰는 스피치라이터가 아니라 전달 품질을 진단하는 리허설 코치다.

사용자가 정한 핵심 메시지와 논리를 바꾸지 않는다. transcript, 계측값, 선택적으로 제공된 발표 계획만 근거로 실제 전달을 평가한다. 관찰과 해석을 구분하고 모든 구조 판단에 transcript 인용과 timestamp를 붙인다. 근거가 없으면 확인 불가라고 쓴다.

필러 횟수와 속도 수치를 다시 계산하지 않는다. 제공된 metrics를 해석한다. `그` 후보는 문맥상 필러인지 판단할 수 있지만 불명확하면 검토 필요로 남긴다.

수정 대본을 새로 쓰지 않는다. 다음 녹화에서 실행할 수 있는 행동을 우선순위 3개 이내로 제안한다.
```

agent는 transcript와 산출물을 수정할 필요가 없으므로 `sandbox_mode = "read-only"`로 둔다. model은 하드코딩하지 않고 부모 설정을 상속하는 안을 우선한다.

# 구조 rubric

rubric은 100점짜리 보편 평가보다 0/1/2의 세 단계가 맞다.

| 축 | 0 | 1 | 2 |
| --- | --- | --- | --- |
| 도입의 청자 연결 | 청자가 왜 들어야 하는지 없음 | 문제나 맥락은 있지만 핵심과 연결이 약함 | 청자의 문제와 발표 핵심이 초반에 연결됨 |
| 핵심 메시지 명료성 | 한 문장으로 식별 불가 | 메시지는 있으나 분산되거나 반복됨 | 중심 문장이 분명하고 이후 내용이 이를 지지함 |
| 전개와 예시 연결 | 구간 관계가 끊김 | 순서는 있으나 일부 예시가 주장과 느슨함 | 도입-핵심-예시가 인과적으로 이어짐 |
| 마무리 회수 | 새 주장으로 끝나거나 종료가 없음 | 요약은 있으나 도입·핵심 회수가 약함 | 핵심을 회수하고 청자가 가져갈 결론이나 행동이 분명함 |
| 근거 추적성 | 판단 근거가 없음 | 인용 또는 timestamp 중 하나만 있음 | 모든 주요 판단에 인용과 timestamp가 있음 |

점수보다 `evidence`, `diagnosis`, `next_action`이 더 중요하다. timestamp가 없는 transcript를 입력한 경우 근거 추적성에서 timestamp 부재 자체를 발표 결함으로 감점하지 않고, 도구의 evidence limitation으로 별도 표시한다.

# 피드백 출력

`feedback.md`는 다음 순서로 만든다.

1. 한눈에 보기: 전체 시간, 필러 후보, 전체·발화 속도
2. 필러: 종류별 수치, timestamp, `그` 검토 후보
3. 속도: 전체와 구간 변화, 목표 시간과의 차이
4. 구조: rubric별 상태, transcript 근거, 계획과 실제 차이
5. 다음 리허설: 가장 영향 큰 행동 최대 3개
6. 측정 한계: 필러 누락 가능성, timestamp 유무, 확신이 낮은 판단

보편적인 어절/분 임계값으로 빠르다 또는 느리다고 단정하지 않는다. 첫 실행은 관찰값을 기준선으로 저장하고, 후속 녹화부터 이전 자기 기록과 비교한다. D-4에서는 별도로 정한 3~5분과 구간별 계획을 기준으로 비교할 수 있다.

# rubric만 주거나 role만 주는 대안

| 접근 | 장점 | 단점 |
| --- | --- | --- |
| role + rubric + deterministic metrics | 책임이 분명하고 재현·검증 가능 | 파일과 단계가 조금 늘어남 |
| role만 둔 단일 agent | 가장 빨리 만들 수 있음 | 필러 수와 속도까지 LLM이 재계산해 결과가 흔들릴 수 있음 |
| rubric만 둔 단일 agent | 평가 항목은 고정됨 | 저자 영역 침범과 근거 없는 조언을 막는 행동 경계가 약함 |

첫 번째 방식을 추천한다.

# 검증

- fixture transcript로 `음/어/그` 후보 수가 예상값과 같은지 단위 테스트한다.
- duration과 timestamp fixture로 전체·발화 속도 수식과 30초 구간 집계를 테스트한다.
- agent 출력이 rubric 축마다 evidence, diagnosis, next_action을 갖는지 schema로 확인한다.
- API 호출은 mock으로 테스트하고 실제 key는 테스트나 저장소에 넣지 않는다.
- 사용자 D-4 녹화의 30~60초를 수동 라벨링해 필러 recall을 확인한다. 이 결과가 부족하면 전사 adapter만 교체한다.

# 승인 후 구현 범위

이 구조로 skill, custom agent, rubric, 전사·계측 scripts, fixture unit tests를 만든다. API key는 환경 변수 `OPENAI_API_KEY`에서만 읽고 `.env`를 만들거나 수정하지 않는다. key 없이도 mock tests까지 검증하며, 실제 D-4 피드백 생성은 사용자가 key와 녹화 파일을 준비한 뒤 수행한다.
