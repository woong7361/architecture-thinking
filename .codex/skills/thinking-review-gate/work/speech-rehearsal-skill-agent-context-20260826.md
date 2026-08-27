# 프로젝트 문맥

- `task4/assignments/taskD-5.md`는 D-4 녹화 transcript를 입력받아 필러 `음/어/그`, 말 속도, 도입-핵심-마무리 구조를 피드백하는 스피치 리허설 에이전트 코드와 실제 피드백 결과를 요구한다.
- `task4/assignments/taskD-4.md`의 3~5분 발표 계획은 도입 40초, 핵심 110초, 예시 80초, 마무리 30초이고, 한 줄 메시지와 구간별 목적이 이미 작성되어 있다.
- 사용자는 수동 파일 분할과 로컬 음성 모델 실행을 원하지 않는다. 직전 대화에서 OpenAI `gpt-transcribe`를 클라우드 전사 엔진으로 쓰는 방향에 동의했다.
- 프로젝트 규칙상 공용 skill과 rubric은 특정 D-4 사례에 고정하지 않고 일반적인 발표 리허설 개념으로 작성해야 한다. D-4 계획은 run 입력으로 넣는다.
- 설계 승인 전에는 구현하지 않는다. API key는 임의 생성하지 않고 `.env`를 수정하지 않는다.

# 기존 프로젝트 관례

- 프로젝트 skill은 `.codex/skills/<name>/SKILL.md` 아래에 두며 복잡한 반복 로직은 `scripts/` 또는 `pipeline/`, 평가 기준은 rubric 파일로 분리한다.
- skill의 `agents/openai.yaml`은 표시 이름, 설명, 기본 프롬프트 같은 UI 메타데이터다. 전문 역할을 가진 실제 Codex custom agent와는 다르다.
- 공식 Codex 문서상 프로젝트 custom agent는 `.codex/agents/*.toml`에 두며 `name`, `description`, `developer_instructions`가 필수다. custom agent는 context 분리와 좁은 역할에 유용하지만 별도 agent 실행 비용이 든다.

# 공식 문서 근거

- Skill: https://learn.chatgpt.com/docs/build-skills
  - skill은 재사용 workflow를 위한 instructions, optional scripts, references 묶음이다.
  - `agents/openai.yaml`은 appearance와 dependencies를 위한 선택적 파일이다.
- Custom agent: https://learn.chatgpt.com/docs/agent-configuration/subagents
  - 프로젝트 agent는 `.codex/agents/*.toml`에 정의한다.
  - 명확하고 좁은 job, 맞는 tool surface, adjacent work로 번지지 않는 instruction이 권장된다.
  - subagent 실행은 단일 agent보다 token을 더 소비한다.
- GPT Transcribe: https://developers.openai.com/api/docs/models/gpt-transcribe
  - completed audio file transcription, keyword hints, language hints를 지원한다.
- Transcription API: https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
  - 파일 한 개를 입력받고, `chunking_strategy=auto`, `language`, `keywords`, `prompt`를 지원한다.
  - `verbose_json`과 word/segment timestamp granularity가 문서에 있다. 실제 `gpt-transcribe` 조합은 live smoke test로 확인해야 한다.

# 핵심 불확실성

- `gpt-transcribe`가 한국어 필러를 항상 보존한다는 공식 보장은 없다.
- `그`는 지시어나 관형어일 수도 있어 문자열 출현 수를 곧바로 필러 확정 수로 쓰면 안 된다.
- 한국어 말 속도는 ASR 띄어쓰기에 영향을 받으므로 어절/분 하나만 보편적인 좋고 나쁨 기준으로 쓰면 안 된다.
- 별도 custom agent를 만드는 것은 과제의 agent 산출물을 분명하게 하지만, 구조 평가만 맡겨야 역할 분리의 이점이 있다.
