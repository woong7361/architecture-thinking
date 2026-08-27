# 로컬 mechanics 구현과 계약 검증 완료

`speech-rehearsal` skill과 두 project custom agent를 구현했다. 별도 runner는 만들지 않았다. 실제 API 호출 없이 검증 가능한 local mechanics와 입출력 계약까지 확인했으며, live transcription과 실제 녹화에 대한 reviewer 품질은 아직 검증하지 않았다.

## 단일 책임 위치

- orchestration instruction: `.codex/skills/speech-rehearsal/SKILL.md`
- delivery role prompt 본문: `.codex/skills/speech-rehearsal/references/roles/delivery-reviewer.md`
- senior logic role prompt 본문: `.codex/skills/speech-rehearsal/references/roles/senior-logic-reviewer.md`
- delivery rubric criteria: `.codex/skills/speech-rehearsal/references/rubrics/delivery.yaml`
- logic rubric criteria: `.codex/skills/speech-rehearsal/references/rubrics/logic.yaml`
- artifact shape: `.codex/skills/speech-rehearsal/schemas/`
- API 호출, tokenization, 계산, 검증, 집계, rendering: `.codex/skills/speech-rehearsal/scripts/`

Custom agent TOML의 `developer_instructions`는 Codex custom agent 형식상 필수다. 여기에 reviewer의 역할·평가 기준 본문을 넣지 않았다. 유일한 role Markdown을 읽고, 읽을 수 없으면 임의로 역할을 만들지 말라는 최소 loader 지침만 남겼다. 따라서 prompt 본문의 변경 책임은 role Markdown 한 곳에 있고 TOML과 script에는 복제본이 없다.

두 reviewer는 같은 immutable `review-context.json`을 보고 병렬 실행하며 서로의 결과를 먼저 읽지 않는다.

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

Checked Context를 만든 시점에는 단위 테스트 4개가 통과했다. 그 뒤 transcript JSON도 schema로 검증하도록 보완하고 `test_transcription_response_is_normalized_to_the_contract`를 추가했다. 최종 재실행 결과는 단위 테스트 5개 통과와 세 CLI entrypoint 정상 로드다.

prompt 책임 중복 검사는 다음 범위와 pattern으로 실행했다.

```powershell
rg -n "You are|당신은|도입|핵심 메시지|논리 도약|filler_and_disfluency|thesis_identifiability" .codex/skills/speech-rehearsal/scripts
```

검색 결과가 없음을 확인했다. 단위 테스트도 두 role의 대표 문장과 두 rubric에서 읽은 모든 criterion key가 `scripts/*.py`에 존재하지 않는지 검사한다. 이 검사는 자연어가 다른 표현으로 새는 모든 경우를 증명하지는 않지만, 현재 권위 파일의 실제 본문과 criterion 복제는 잡는다.

추가로 두 custom agent TOML을 Python `tomllib`으로 parse하고, 네 JSON Schema를 `python -m json.tool`로 검사했다. 새 skill과 agent 파일의 trailing whitespace, multipart body의 model과 원본 filename 포함 여부도 확인했다.

OpenAI 공식 문서의 [Subagents](https://learn.chatgpt.com/docs/agent-configuration/subagents)는 project custom agent를 `.codex/agents/` 아래에 두고 `name`, `description`, `developer_instructions`를 필수로 정의한다고 설명한다. 같은 문서는 독립 subagent의 병렬 실행과 상위 thread의 결과 수집도 설명한다. [GPT Transcribe 모델 문서](https://developers.openai.com/api/docs/models/gpt-transcribe)는 completed audio file transcription과 `/v1/audio/transcriptions` endpoint 지원을 명시한다. 확인일은 2026-08-26이다.

## 남은 검증

- `skill-creator`의 `quick_validate.py`는 현재 Python에 PyYAML이 없어 import 단계에서 중단됐다. 패키지를 임의 설치하지 않고 validator가 보는 frontmatter, skill name, TODO 항목은 수동으로 확인했다.
- 사용자 제공 `OPENAI_API_KEY`가 없어 live transcription을 실행하지 않았다.
- 새 custom agent 설정과 병렬 orchestration은 공식 문서와 TOML parse로 확인했지만, 이 세션에서 실제 두 reviewer를 병렬 실행하지 않았다.
- D-5의 실제 피드백 결과를 완성하려면 D-4 녹화 또는 transcript를 받고, 필요한 경우 사용자 key로 전사하고, 두 reviewer JSON을 생성한 뒤 최종 `feedback.md`를 확인해야 한다.
