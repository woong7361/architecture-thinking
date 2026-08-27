# 프로젝트 문맥

- `task4/assignments/taskD-5.md`는 D-4 녹화 전사를 입력받아 필러 워드 빈도, 말 속도, 도입-핵심-마무리 구조를 피드백하는 스피치 리허설 에이전트를 요구한다.
- `task4/assignments/taskD-4.md`의 실제 발표는 한국어 1인 발표이며, 3~5분 스피치는 도입 40초, 핵심 110초, 예시 80초, 마무리 30초로 설계되어 있다. 별도로 1분 엘리베이터 피치도 있다.
- D-4에는 한 줄 메시지, 구간별 목적, 실제 예시, 금지 사항, 자가 체크가 이미 있으므로 구조 평가는 일반적인 말하기 모범답안이 아니라 이 계획과 실제 전사의 차이를 봐야 한다.
- 이번 요청은 구현 전 기술 선택과 접근 방향을 검토하는 단계다. 사용자가 승인하기 전에는 에이전트 코드를 만들지 않는다.

# 공식 근거

1. faster-whisper 공식 저장소
   - MIT 라이선스다.
   - Python 3.9 이상을 요구하고 PyAV가 FFmpeg 라이브러리를 번들하므로 시스템 FFmpeg 설치가 필수는 아니다.
   - 단어별 시작/종료 시각을 지원한다.
   - Silero VAD를 통합하며 CPU int8과 GPU 실행 예시를 제공한다.
   - https://github.com/SYSTRAN/faster-whisper

2. OpenAI Whisper 공식 저장소
   - 다국어 음성 인식 모델이고 코드와 모델 가중치가 MIT 라이선스다.
   - turbo는 large-v3의 속도 최적화형이며 정확도 저하가 작다고 공식 README가 설명한다.
   - https://github.com/openai/whisper

3. WhisperX 공식 저장소와 논문
   - faster-whisper 기반 전사, VAD, 강제 정렬을 이용한 단어 시각, 화자 분리를 제공한다.
   - BSD-2-Clause 라이선스다.
   - 겹치는 발화와 화자 분리는 완벽하지 않고 언어별 정렬 모델이 필요하다는 한계가 있다.
   - https://github.com/m-bain/whisperX
   - https://arxiv.org/abs/2303.00747

4. whisper.cpp 공식 저장소
   - Windows, Java 등 여러 플랫폼과 로컬 실행을 지원한다.
   - 단어 단위 타임스탬프는 공식 문서에서 experimental로 표시된다.
   - https://github.com/ggml-org/whisper.cpp

5. 비유창성 전사 한계에 대한 연구 근거
   - 기본 Whisper 출력은 읽기 쉬운 의도 중심 전사를 만들며 비유창성과 머뭇거림을 생략하는 경향이 있다.
   - https://www.isca-archive.org/slate_2023/ma23_slate.html
   - CrisperWhisper 논문도 Whisper가 필러, 반복 발화 등을 제거하는 경향을 지적하고 verbatim 전사를 별도 목표로 다룬다.
   - https://www.isca-archive.org/interspeech_2024/zusag24_interspeech.pdf

6. 한국어 verbatim 실험 모델
   - `Sky-Kim/crisper-whisper2-base-finetuned-ko` 모델 카드는 한국어 필러와 반복 보존을 목표로 한다.
   - 다만 비상업 연구 전용이며 KsponSpeech의 제한된 이용 조건을 상속한다. 모델 카드가 제시한 수치는 해당 모델 작성자의 in-domain 평가이므로 독립 검증 결과로 간주하면 안 된다.
   - https://huggingface.co/Sky-Kim/crisper-whisper2-base-finetuned-ko

# 판단 제약

- 필러 워드는 원문 전사에서 지운 뒤 세면 안 된다. 원문 전사와 읽기용 정제 전사를 분리한다.
- 한국어 `그`는 지시어와 필러가 모두 될 수 있어 문자열 일괄 카운트가 오탐을 만든다.
- 말 속도는 전체 녹화 시간 기준 속도와 실제 발화 시간 기준 속도를 구분해야 한다.
- 구조는 통계만으로 판정할 수 없고 D-4의 계획과 전사를 타임코드 근거로 대조해야 한다.
- 기본 Whisper가 필러를 누락할 수 있으므로 D-4 첫 실행에서는 사람이 오디오와 후보 타임코드를 대조하여 필러 탐지 정확도를 확인해야 한다.
