## 문제 지점

- [timestamp 전제] 초안은 `gpt-transcribe -> transcript.json + timestamps`와 “단어 또는 구간 timestamp 기반 말 속도/멈춤 계산”을 실행 흐름의 확정 요소처럼 쓴다. 입력 문맥에는 “timestamp가 있으면 계산할 수 있다”는 조건만 있고, 현재 후보 전사 경로가 실제로 필요한 granularity의 timestamp를 제공한다는 근거는 없다. 이 부분은 확인된 사실이 아니라 설계 전제 또는 확인 필요 조건으로 분리해야 한다.

- [검증 가능한 근거 anchor] 초안의 주요 claim은 대체로 입력 문맥과 맞지만, “project custom agent 병렬 실행 가능”, “TOML에는 경로와 책임 경계만 둔다”, “manifest에 role/rubric 버전 또는 해시를 남긴다” 같은 설계 claim이 어떤 근거에서 나온 것인지 답변 안에서 구분되지 않는다. 사용자가 검증 가능한 답변으로 쓰려면 입력 문맥, 프로젝트 원칙, 구현 선택을 명시적으로 나눠야 한다.

- [AI 분류의 변동성 경계] “AI가 문맥을 분류하고 코드는 schema 검증과 집계를 한다”는 추천은 적절하지만, AI 분류 자체의 비결정성은 남는다. 초안은 집계 숫자의 흔들림을 줄인다고 설명하지만, 같은 transcript를 재평가할 때 label이 달라질 가능성까지는 충분히 표시하지 않는다.

- [전사 결과와 reviewer 입력 계약] `review-context.json`을 immutable하게 만든다고 했지만, 그 안에 포함될 최소 필드가 불분명하다. 특히 발표 계획, transcript, timestamp, 속도 계산 결과, reviewer가 참조할 rubric 경로 또는 버전 중 무엇이 공통 입력이고 무엇이 reviewer별 입력인지가 덜 분리되어 있다.

- [어투 평가 범위] 초안은 “언어적 어투”와 “발표 구조”를 delivery reviewer에 넣는다. 입력 문맥상 transcript-only에서 가능한 어투 평가는 단어 선택, 문장 종결, 반복, 머뭇거림 정도다. 초안도 일부 제한을 적었지만, “속도 해석”과 “어투”가 실제 음성 운율 평가처럼 오해될 수 있는 표현이 남아 있다.

## 확인 필요

- OpenAI Audio transcription API의 선택 모델과 응답 포맷이 단어 또는 구간 timestamp를 제공하는지 확인해야 한다.

- D-5 산출물이 요구하는 “필러 워드 빈도”가 확정 filler만의 빈도인지, uncertain까지 포함한 후보 빈도인지 정해야 한다.

- 발표 계획 또는 원래 발표 의도 문서가 reviewer 입력에 항상 존재하는지 확인해야 한다. 없으면 logic reviewer의 “주장-근거-예시 적합성” 평가는 transcript 내부 기준으로 제한되어야 한다.

- `.codex/agents/*.toml` custom agent와 skill 내부 prompt/rubric/schema 파일을 실제 실행 시 어떤 방식으로 전달할지 확인해야 한다. 초안은 구조를 제안하지만 실행 연결 방식은 아직 추정에 가깝다.

## 수정 제안

- `transcript.json + timestamps`는 “가능하면” 또는 “필수 확인 조건”으로 바꾸고, timestamp가 없을 때의 fallback을 추가하라. 예를 들어 말 속도와 pause 피드백은 생략하거나, transcript-only에서 계산 가능한 지표만 산출한다고 분기할 수 있다.

- 답변 앞부분에 “확정된 방향”, “확인 필요한 전제”, “추천 설계”를 나눠라. 사용자가 동의해야 할 결정과 구현 전에 확인해야 할 사실이 섞이지 않게 된다.

- AI filler 분류의 비결정성 대응을 보강하라. 예를 들어 원본 transcript와 annotation을 저장하고, 확정 빈도는 `label=filler`만 집계하며, 재실행 비교가 필요하면 같은 입력과 같은 rubric 버전의 결과 차이를 diff로 확인한다고 적으면 더 검증 가능하다.

- `review-context.json`의 최소 필드를 짧게 제시하라. 예: transcript text, utterance/segment timestamps, calculated pace metrics, presentation goal, reviewer input manifest, rubric version/hash. 단, 실제 API 응답에서 보장되지 않은 필드는 optional로 표시해야 한다.

- delivery reviewer의 평가 범위를 “transcript에서 확인 가능한 전달 방식”으로 더 좁혀 표현하라. 음높이, 음량, 강세뿐 아니라 실제 감정, 에너지, 청중 반응도 transcript-only 한계에 포함하면 오해가 줄어든다.

- “승인 후 구현 범위”는 좋지만, 실제 구현 전 확인 항목을 먼저 완료해야 한다는 순서를 분명히 하라. 특히 API key는 사용자가 제공한 환경 변수만 사용하고, `.env` 수정은 하지 않는다는 제약을 함께 표시하면 프로젝트 원칙과 더 잘 맞는다.

## 요약

초안의 큰 방향은 사용자의 제안과 프로젝트 원칙에 잘 맞는다. 다만 timestamp 제공 여부, AI 분류의 재현성, reviewer 입력 계약처럼 구현 가능성을 좌우하는 전제가 확정 사실처럼 쓰인 부분이 있다. 사실, 전제, 추천을 분리하고 transcript-only 한계를 더 명확히 표시하면 사용자가 바로 승인 여부를 판단할 수 있는 답변이 된다.