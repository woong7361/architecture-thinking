# 구현 완료

`speech-rehearsal` skill과 두 custom agent를 구현했다. 별도 runner는 만들지 않았으며 Python 스크립트에 role prompt나 rubric criterion을 넣지 않았다.

## 책임 위치

- orchestration: `.codex/skills/speech-rehearsal/SKILL.md`
- delivery role: `.codex/skills/speech-rehearsal/references/roles/delivery-reviewer.md`
- senior logic role: `.codex/skills/speech-rehearsal/references/roles/senior-logic-reviewer.md`
- delivery rubric: `.codex/skills/speech-rehearsal/references/rubrics/delivery.yaml`
- logic rubric: `.codex/skills/speech-rehearsal/references/rubrics/logic.yaml`
- artifact shapes: `.codex/skills/speech-rehearsal/schemas/`
- mechanics only: `.codex/skills/speech-rehearsal/scripts/`

Custom agent TOML에는 role 내용을 복제하지 않고 유일한 role Markdown을 읽으라는 로더 지침만 남겼다. 두 reviewer는 같은 immutable `review-context.json`을 보고 병렬 실행하며 서로의 결과를 먼저 읽지 않는다.

## 구현된 동작

- `transcribe.py`: 파일 하나를 `gpt-transcribe` endpoint로 보내 transcript JSON을 만든다. 수동 분할은 하지 않는다. `OPENAI_API_KEY` 환경 변수만 읽는다.
- `prepare_review.py`: transcript 전체를 중립적인 token ID로 나누고, duration이 있으면 발화 속도를 계산한다. role, rubric, schema의 경로와 SHA-256을 context에 고정한다.
- Delivery reviewer: 문맥으로 필러를 `filler`, `lexical`, `uncertain`으로 분류하고 전달 방식과 구조를 진단한다.
- Senior logic reviewer: 주장, reasoning link, evidence fit, assumption, contradiction, follow-up question을 진단한다.
- `aggregate_review.py`: 두 JSON을 schema와 rubric coverage로 검증하고 token evidence를 확인한 뒤 AI label만 집계해 `feedback.json`과 `feedback.md`를 만든다.

## 검증 결과

- 단위 테스트 4개 통과
- custom agent TOML parse 통과
- JSON Schema 문법 검사 통과
- prompt duplication scan 통과
- trailing whitespace 검사 통과
- multipart request construction 검사 통과

`quick_validate.py`는 현재 Python에 PyYAML이 없어 실행 전 import 단계에서 중단됐다. 패키지를 임의 설치하지 않고 동일한 frontmatter, skill name, TODO 검사를 수동으로 확인했다. 실제 전사 호출도 사용자 key가 없어 실행하지 않았다. 따라서 transcript 기반 mechanics는 검증됐고 live API와 실제 두 reviewer의 결과 품질은 D-4 녹화 또는 transcript가 제공될 때 확인해야 한다.

OpenAI 공식 문서에 따라 custom agent는 project `.codex/agents/` 아래에 두었고 두 독립 reviewer는 병렬 실행하도록 skill에 고정했다.
