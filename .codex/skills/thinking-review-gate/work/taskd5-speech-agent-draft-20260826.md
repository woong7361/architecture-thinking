네, 있다. 다만 전사 엔진 하나에 모든 판단을 맡기기보다 **전사기, 계측기, 코치**를 나누는 편이 맞다. 녹음 내용을 받아 적는 전사기와 속도계를 같은 장치로 만들 수는 있지만, 발표 구조까지 그 장치가 정확히 이해한다고 보면 안 된다.

현재 taskD-5에는 다음 조합을 추천한다.

```text
음성·영상
  -> faster-whisper: 원문 전사 + 단어/구간 타임코드
  -> 결정적 분석기: 필러 빈도 + 말 속도 + 침묵 구간
  -> LLM 비평기: D-4 계획과 실제 구조를 타임코드 근거로 대조
  -> report.md + transcript.json
```

## 추천안

전사는 [faster-whisper](https://github.com/SYSTRAN/faster-whisper)가 가장 현실적인 시작점이다. MIT 라이선스이고, 한국어를 포함하는 Whisper 계열 모델을 로컬에서 실행할 수 있으며, 단어별 시작·종료 시각과 Silero VAD를 지원한다. PyAV가 오디오 디코딩을 맡으므로 영상 파일을 받기 위해 시스템 FFmpeg를 반드시 따로 설치할 필요도 없다.

단, **전사된 `음`, `어`, `그`를 세면 실제 필러 수가 정확히 나온다고 보장할 수는 없다.** 기본 Whisper는 읽기 좋은 전사를 만들면서 머뭇거림과 비유창성을 생략하는 경향이 있다는 연구 결과가 있다. 따라서 첫 버전은 “자동 측정값 + 검토 가능한 타임코드”를 내야 한다. D-4는 3~5분이므로 첫 녹화에서는 사람이 오디오를 한 번 들으며 누락과 오탐을 확인하는 비용도 크지 않다. [Whisper의 말하기 평가 한계 연구](https://www.isca-archive.org/slate_2023/ma23_slate.html), [CrisperWhisper 논문](https://www.isca-archive.org/interspeech_2024/zusag24_interspeech.pdf)

## 분석 결과에 담을 내용

1. 필러 워드
   - 종류별 횟수: `음`, `어`, `아`, 반복·말 고침
   - 분당 횟수와 전체 발화 토큰 100개당 횟수
   - 각 발생 시각과 앞뒤 문장
   - `그`처럼 실제 의미가 있을 수 있는 항목은 `확정`과 `검토 필요`로 분리

2. 말 속도
   - 전체 속도: 공백 기준 발화 토큰 수 / 녹화 전체 분
   - 발화 속도: 공백 기준 발화 토큰 수 / 실제 음성 구간 분
   - 한국어 글자 수/분을 함께 기록해 ASR 띄어쓰기 변화의 영향을 확인
   - 침묵 횟수, 중앙값, 긴 침묵, 가장 긴 침묵의 타임코드

3. 구조
   - D-4의 계획인 도입 40초, 핵심 110초, 예시 80초, 마무리 30초와 실제 구간 비교
   - 도입에 청자의 문제가 등장했는지
   - 한 줄 메시지와 세 핵심 질문이 실제로 회수됐는지
   - 예시가 주장과 연결됐는지
   - 마무리가 도입 질문과 다음 행동을 회수했는지
   - 모든 판단에 전사 인용과 타임코드를 붙이고, 근거가 없으면 `확인 불가`로 출력

예상 산출물은 다음처럼 잡을 수 있다.

```json
{
  "transcript": {
    "segments": [],
    "words": []
  },
  "delivery": {
    "fillers": {
      "total": 8,
      "per_minute": 1.9,
      "occurrences": [
        {"at": 12.4, "token": "어", "status": "confirmed"},
        {"at": 37.1, "token": "그", "status": "review_required"}
      ]
    },
    "pace": {
      "tokens_per_total_minute": 124,
      "tokens_per_speaking_minute": 151,
      "korean_characters_per_minute": 286
    },
    "pauses": {
      "long_pause_threshold_seconds": 1.5,
      "long_pause_count": 4,
      "longest": {"start": 93.2, "end": 96.0}
    }
  },
  "structure": {
    "introduction": {"status": "present", "evidence": []},
    "core": {"status": "partial", "evidence": []},
    "example": {"status": "present", "evidence": []},
    "closing": {"status": "missing", "evidence": []}
  }
}
```

숫자는 출력 형태를 보여주기 위한 예시일 뿐, 좋은 발표의 보편 기준값은 아니다. 실제 기준은 D-4 녹화 몇 편을 보고 정해야 한다.

## 다른 선택지와 트레이드오프

| 선택지 | 적합한 경우 | 장점 | 단점 |
|---|---|---|---|
| **faster-whisper + 자체 분석기** | 현재 D-4의 1인 한국어 발표 | 가장 단순하고 MIT이며 단어 시각·VAD 사용 가능 | 필러 누락을 별도 검증해야 함 |
| [WhisperX](https://github.com/m-bain/whisperX) | 인터뷰처럼 여러 명이 말하거나 더 정밀한 정렬이 필요함 | 강제 정렬과 화자 분리 제공 | 언어별 정렬 모델이 필요하고, 공식 문서도 겹침 발화·화자 분리 한계를 명시함 |
| [whisper.cpp](https://github.com/ggml-org/whisper.cpp) | Python 없이 가벼운 네이티브 CLI가 중요함 | Windows와 Java 등 다양한 환경, 완전 로컬 실행 | 단어 단위 시각 기능이 실험적이어서 이번 분석 파이프라인에는 우선순위가 낮음 |

한국어 필러 보존을 목표로 한 실험 모델도 있다. [Sky-Kim의 한국어 CrisperWhisper2 모델](https://huggingface.co/Sky-Kim/crisper-whisper2-base-finetuned-ko)은 `음`, `어`, 반복 보존을 목표로 하지만, 비상업 연구 전용이고 제한된 학습 데이터 조건을 상속한다. 모델 카드의 자체 평가 외 독립 검증도 부족하므로 기본 엔진이 아니라 비교 실험용으로만 두는 편이 안전하다.

## 구현 방향

MVP에서는 원문 전사를 절대 정제하지 않고 보존한다. 필러와 속도를 계산한 뒤에만 별도의 읽기용 전사를 만든다. 구조 비평기에는 음성 자체가 아니라 타임코드가 붙은 전사와 D-4의 한 줄 메시지·구간 계획을 함께 준다. 이렇게 해야 AI가 일반적인 발표 조언으로 메시지를 다시 쓰는 것을 막고, 실제 계획과 전달의 차이만 피드백하게 할 수 있다.

추천안에 동의하면 다음 단계에서 `.codex/skills/speech-rehearsal/` 형태로 입력 계약, 전사 어댑터, 결정적 분석기, 구조 비평 프롬프트, 샘플 테스트까지 설계하면 된다.
