# Project context

- `task4/assignments/taskD-5.md`의 목표는 발표 연습 음성에서 transcript를 만들고, 필러워드 빈도, 말하기 속도, 도입-핵심-마무리 구조를 피드백하는 agent를 설계하는 것이다.
- 직전 탐색에서는 로컬 실행안으로 Korean disfluency 모델인 SeloWhisper와 faster-whisper를 검토했다.
- 사용자는 음성을 직접 나누는 UX를 원하지 않으며, 로컬 컴퓨터에서 추론해야 하는지도 묻고 있다.
- 현재 컴퓨터에는 RTX 3060 12GB와 약 64GB RAM이 있어 로컬 추론 자체는 가능하지만 필수 조건은 아니다.
- 구현은 아직 승인 전이므로 하지 않는다.

# Official evidence checked on 2026-08-26

- OpenAI GPT-Transcribe model page: https://developers.openai.com/api/docs/models/gpt-transcribe
  - completed audio files와 streamed file transcription을 지원한다.
  - 가격은 현재 오디오 1분당 USD 0.0045다.
  - audio input은 지원하지만 video modality는 지원하지 않는다.
  - Free tier는 지원하지 않는다.
- OpenAI transcription API reference: https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
  - `/v1/audio/transcriptions`는 flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm 파일 하나를 받는다.
  - `chunking_strategy=auto`를 지정하면 서버가 loudness normalization과 VAD로 경계를 선택한다.
  - 지정하지 않으면 audio를 single block으로 전사한다.
  - Korean language hint와 prompt/keywords를 줄 수 있다.
- OpenAI GPT-5-Codex model page: https://developers.openai.com/api/docs/models/gpt-5-codex
  - Codex 모델 자체는 audio와 video modality를 지원하지 않는다.
  - 따라서 Codex는 전사 모델을 호출하고 결과를 분석하는 orchestrator 역할이다.
- Anthropic Claude upload help: https://support.claude.com/en/articles/8241126-upload-files-to-claude
  - 현재 지원 목록은 문서와 이미지이며 audio/video는 열거되어 있지 않다.
- Anthropic Claude Code CLI reference: https://code.claude.com/docs/en/cli-usage
  - CLI input format은 text 또는 stream-json이다.
  - Claude Code는 shell과 MCP 같은 도구를 통해 별도 전사 엔진을 호출할 수 있는 agent이지만, 공식 문서상 자체 음성 전사 입력은 확인되지 않았다.

# Important uncertainty

- `gpt-transcribe`가 한국어 필러워드 `음`, `어`, `그`를 항상 보존한다고 공식 문서가 보장하지 않는다.
- prompt와 keyword hints는 제공할 수 있지만 필러워드 누락을 없앤다는 보장은 아니다.
- 첫 실제 발표 녹음으로 수작업 정답과 비교하는 calibration이 필요하다.
- API를 쓰려면 사용자가 발급하고 관리하는 OpenAI API key와 API billing이 필요하다. key 생성이나 `.env` 수정은 하지 않는다.
