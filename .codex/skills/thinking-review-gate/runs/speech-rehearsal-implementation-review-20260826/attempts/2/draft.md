# 로컬 구현 완료

`speech-rehearsal` skill과 두 project custom agent를 구현했다. 별도 runner는 만들지 않았다. 실제 API 호출 없이 검증 가능한 local mechanics와 입출력 계약까지 확인했으며, live transcription과 실제 녹화에 대한 reviewer 품질은 아직 검증하지 않았다.

## 단일 책임 위치

- orchestration: `.codex/skills/speech-rehearsal/SKILL.md`
- delivery role prompt: `.codex/skills/speech-rehearsal/references/roles/delivery-reviewer.md`
- senior logic role prompt: `.codex/skills/speech-rehearsal/references/roles/senior-logic-reviewer.md`
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
- `aggregate_review.py`: context와 두 reviewer JSON을 schema로 검증하고 rubric coverage와 token evidence를 확인한 뒤 AI label만 집계해 `feedback.json`과 `feedback.md`를 만든다.

## 재현 가능한 검증

```powershell
python -B -m unittest discover -s .codex/skills/speech-rehearsal/tests -v
python -B .codex/skills/speech-rehearsal/scripts/transcribe.py --help
python -B .codex/skills/speech-rehearsal/scripts/prepare_review.py --help
python -B .codex/skills/speech-rehearsal/scripts/aggregate_review.py --help
```

결과는 단위 테스트 5개 통과와 세 CLI entrypoint 정상 로드다. 테스트에는 AI label 집계, 의미 표현과 uncertain 분리, 중복 token annotation 차단, transcript contract 정규화, script의 role/rubric 책임 중복 금지가 포함된다.

다음 범위를 별도로 검사했다.

- 두 custom agent TOML을 Python `tomllib`으로 parse했다.
- 네 JSON Schema를 `python -m json.tool`로 검사했다.
- `.codex/skills/speech-rehearsal/scripts/*.py`를 대상으로 두 role 본문과 rubric criterion 이름을 `rg`로 검색해 복제가 없음을 확인했다.
- 새 skill과 agent 파일 전체에서 trailing whitespace를 검사했다.
- multipart body에 model과 원본 filename이 포함되는지 확인했다.

OpenAI 공식 문서의 Subagents 항목은 project custom agent를 `.codex/agents/` 아래에 두고 `name`, `description`, `developer_instructions`를 필수로 정의한다고 설명한다. 같은 문서는 독립 subagent를 병렬 실행하고 상위 thread가 결과를 모을 수 있다고 설명한다. GPT Transcribe 모델 문서는 completed audio file transcription과 `/v1/audio/transcriptions` endpoint 지원을 명시한다.

## 남은 검증

- `skill-creator`의 `quick_validate.py`는 현재 Python에 PyYAML이 없어 import 단계에서 중단됐다. 패키지를 임의 설치하지 않고 validator가 보는 frontmatter, skill name, TODO 항목은 수동으로 확인했다.
- 사용자 제공 `OPENAI_API_KEY`가 없어 live transcription을 실행하지 않았다.
- 새 custom agent 설정과 병렬 orchestration은 공식 문서와 TOML parse로 확인했지만, 이 세션에서 실제 두 reviewer를 병렬 실행하지 않았다.
- D-4 녹화나 transcript가 제공되면 live 전사, 두 reviewer의 실제 JSON, 최종 `feedback.md`를 end-to-end로 확인해야 D-5 결과물까지 완성된다.
