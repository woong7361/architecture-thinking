# 결론

가능하다. 다만 정확히는 Codex나 Claude가 직접 음성을 듣는 것이 아니라, 전용 음성 모델이 받아쓰고 Codex나 Claude가 전체 작업을 지휘하는 구조다.

현재 taskD-5에는 `Codex -> OpenAI gpt-transcribe -> Codex 분석` 구성이 가장 단순하다. 사용자는 3~5분짜리 음성이나 지원되는 MP4 파일 하나만 주면 된다. 사람이 음성을 나눌 필요가 없다. 전사 API가 파일 하나를 받고, 필요하면 서버에서 `chunking_strategy=auto`로 VAD 기반 분할을 처리한다. 내부적으로 모델이 구간을 나눌 수는 있지만 사용자 UX에는 드러나지 않는다.

Codex 모델 자체는 audio/video 입력을 지원하지 않는다. 그러므로 Codex는 파일 확인, 전사 API 호출, 결과 저장, 필러워드 집계, 말하기 속도 계산, 구조 피드백 작성을 맡는다. Claude Code도 같은 orchestration은 가능하지만, Anthropic 공식 문서에서 Claude 자체의 audio transcription 입력이나 전용 STT API는 확인되지 않았다. Claude를 쓰려면 결국 OpenAI 전사 API나 로컬 Whisper 같은 별도 엔진을 붙여야 한다.

# 권장 흐름

1. 사용자가 3~5분 음성 또는 지원되는 MP4 파일 하나를 지정한다.
2. Codex가 파일을 그대로 `/v1/audio/transcriptions`의 `gpt-transcribe`에 보낸다. 지원되지 않는 영상 컨테이너일 때만 오디오를 자동 추출한다.
3. 전사 요청에 한국어 힌트와 `음`, `어`, `그` 같은 머뭇거림을 생략하지 말라는 prompt/keywords를 준다.
4. Codex가 transcript와 파일 재생시간으로 필러워드 횟수, 분당 발화량, 구간별 속도를 계산한다.
5. transcript를 바탕으로 도입-핵심-마무리 구조와 개선 예시를 작성한다.
6. `transcript.json`과 `feedback.md`를 결과로 남긴다.

# 로컬에서 처리해야 하나

아니다. OpenAI API를 선택하면 음성 인식 추론은 OpenAI 서버에서 수행된다. 내 컴퓨터는 파일 업로드와 결과 분석 명령만 실행한다. 반대로 개인정보 때문에 음성을 외부에 보내기 싫다면 SeloWhisper나 faster-whisper를 로컬에서 돌리는 편이 맞다. 현재 장비는 로컬 추론을 시도할 수 있는 수준이지만, 설치와 모델 관리가 추가된다.

# 선택지 비교

| 방식 | 수동 분할 | 로컬 추론 | 장점 | 단점 |
| --- | --- | --- | --- | --- |
| Codex + `gpt-transcribe` | 없음 | 없음 | 가장 단순하고 agent 자동화에 바로 연결 | 음성 업로드, API key와 과금 필요, 필러 보존 검증 필요 |
| Claude Code + 외부 STT | 없음 | 선택 | Claude 중심 workflow에서도 가능 | 전사 엔진은 별도로 필요해서 taskD-5에서는 한 단계 더 복잡 |
| Codex/Claude + 로컬 SeloWhisper | 없음 | 있음 | 음성을 외부로 보내지 않고 Korean filler 특화 | 설치와 GPU/모델 관리, 장문 전사 결합 검증 필요 |

# 비용과 남은 위험

`gpt-transcribe`의 현재 가격은 분당 USD 0.0045다. 3~5분이면 전사만 약 USD 0.0135~0.0225이며 분석 모델 비용은 별도다. Free tier는 지원하지 않는다.

가장 큰 기술적 위험은 일반 전사 모델이 `음`, `어`, `그`를 문장 정리 과정에서 누락할 수 있다는 점이다. 공식 문서는 필러워드 완전 보존을 보장하지 않는다. 따라서 설계 승인 후 첫 실제 녹음 한 편에 대해 사람이 30~60초만 정답표를 만들고, 필러 recall이 허용 기준을 넘는지 확인해야 한다. 부족하면 전사 단계만 SeloWhisper로 교체하고 나머지 분석 agent는 그대로 유지할 수 있다.

# 추천

MVP는 `Codex orchestrator + gpt-transcribe`로 시작한다. 사용자가 파일을 나누지 않는 UX, 로컬 모델 설치 없는 실행, 한 번의 명령으로 transcript와 피드백을 생성하는 목표에 가장 잘 맞는다. 단, 필러워드 검증을 통과해야 최종 채택한다.
