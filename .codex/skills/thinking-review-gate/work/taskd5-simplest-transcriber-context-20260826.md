# 결정할 것

taskD-5의 한국어 1인 발표용 스피치 리허설 에이전트에서 `faster-whisper + 별도 분석기`가 가장 간단한지 재검토한다. 핵심 요구는 필러 워드 빈도, 말 속도, 도입-핵심-마무리 구조 피드백이다. 단어별 정밀 시각과 침묵 분석은 원래 요구에 명시되지 않았다.

# 프로젝트 문맥

- `task4/assignments/taskD-5.md`는 D-4 녹화 전사를 입력받아 `음/어/그` 빈도, 말 속도, 구조 피드백을 요구한다.
- 로컬 저장소에는 아직 D-4 음성이나 영상 파일이 없다.
- 설계 선택 단계이므로 사용자 승인 전 구현하지 않는다.

# 웹 탐색 근거

## SeloWhisper-ko-disfluency

- 공식 모델 카드: https://huggingface.co/rearleg/SeloWhisper-ko-disfluency
- MIT 라이선스이며 Whisper large-v3-turbo를 한국어 비유창성 인식에 미세 조정했다.
- `아/어/음/그/저/뭐/막`, 반복, 웃음, 기타 비유창성을 10개 특수 토큰으로 전사 안에 직접 출력한다.
- Transformers 추론 레시피를 제공한다.
- 자체 평가의 filler recall은 clean 0.8488, noisy 0.9039다. 따라서 자체 평가에서도 약 9.6~15.1% 누락이 있다.
- filler metric은 토큰 종류별 전체 개수를 비교하는 counter-based 지표다. 발생 위치가 맞는지는 평가하지 않는다.
- 방법론, 학습 데이터, 분석은 향후 논문으로 미뤘으며 현재는 가중치와 추론 레시피만 공개했다.
- 한국어 자발화에 최적화되었지만 방송, 코드 스위칭, 다른 언어에는 맞추지 않았다. 무음과 짧은 클립 환각 가능성이 있어 VAD 또는 RMS 필터를 권한다.
- 모델은 0.8B 파라미터다.

## Hugging Face Transformers

- 공식 문서: https://huggingface.co/docs/transformers/main_classes/pipelines
- ASR pipeline은 `chunk_length_s`와 `stride_length_s`를 제공하므로 긴 오디오를 나눠 처리할 수 있다.
- 오디오 파일 입력에 여러 포맷을 쓰려면 ffmpeg 설치가 필요하다고 문서에 적혀 있다.
- Whisper 공식 문서는 long-form transcription과 sequence/token timestamps를 지원한다고 설명하지만, SeloWhisper 모델 카드 자체는 timestamp 및 long-form 조합을 검증하거나 예시로 제공하지 않는다.

## faster-whisper

- 공식 저장소: https://github.com/SYSTRAN/faster-whisper
- MIT, 장문 전사, 단어별 타임스탬프, PyAV 디코딩, Silero VAD가 한 라이브러리에 들어 있다.
- 하지만 일반 Whisper가 비유창성을 생략하는 문제를 직접 해결하는 전용 한국어 모델은 아니다.
- `initial_prompt`와 hotwords는 특정 단어 생성을 유도할 수 있지만 보장되지 않으며, hotword를 실제 발화 없이 환각한 한국어 사례가 공식 이슈에 있다.
  - https://github.com/SYSTRAN/faster-whisper/discussions/458
  - https://github.com/SYSTRAN/faster-whisper/issues/1356

## 완성형 발표 코치

- VoxLab: https://github.com/yaotingchun/VoxLab
- WPM, filler, pause, 구조 평가를 모두 제공하지만 Google Cloud Speech-to-Text, Gemini/Vertex AI, Firebase를 요구한다. 로컬 오픈소스 모델만으로 끝나는 간단한 구성은 아니다.
- pitchprompter-ai: https://github.com/amit1858/pitchprompter-ai
- 연습 모드에 WPM, fillers, long pauses가 있지만 로컬 whisper.cpp provider가 향후 계획으로 표시되어 있다.

## CrisperWhisper 2.0

- 공식 저장소: https://github.com/nyrahealth/CrisperWhisper
- verbatim 전사, 단어 시각, 장문 처리를 한 패키지에 제공한다.
- 추론 코드는 MIT이나 표준 모델 가중치는 비상업 연구 라이선스이고, 공개 benchmark의 비영어 8개 언어는 합성 평가 셋이다.
- 현재 과제에는 라이선스와 한국어 독립 검증 불확실성이 있어 기본 추천으로 삼기 어렵다.

# 판단 기준

- "가장 간단함"은 설치 명령 수가 아니라 현재 요구를 충족하는 최소 구성 요소 수로 판단한다.
- 필러 누락 검증은 어떤 자동 모델에서도 완전히 제거하지 못한다. 다만 D-4 한 편을 사람이 라벨링해 일회성 캘리브레이션 셋으로 쓰면 이후 같은 화자와 녹음 환경에서 검증 비용을 줄일 수 있다.
- 원 요구에 없는 단어별 타임스탬프와 침묵 분석을 MVP에서 제외하면 설계가 단순해진다.
