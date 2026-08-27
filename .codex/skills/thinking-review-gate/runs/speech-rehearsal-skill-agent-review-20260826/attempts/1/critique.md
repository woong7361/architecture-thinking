## 문제 지점

- [custom agent 설정 claim] `.codex/agents/speech-coach.toml`에 `sandbox_mode = "read-only"`를 둔다는 제안은 입력 근거에 직접 연결되어 있지 않다. 제공된 공식 문서 근거에는 custom agent의 필수 필드가 `name`, `description`, `developer_instructions`라는 점과 좁은 역할 권장만 있다. `sandbox_mode`나 부모 model 상속이 해당 TOML에서 실제 지원되는지는 확인된 사실인지, 구현 시 확인할 사항인지 구분해야 한다.

- [전사 API 옵션 claim] `gpt-transcribe`에 `keywords`, `prompt`, `chunking_strategy=auto`, `verbose_json`, word/segment timestamp를 함께 쓰는 흐름은 방향은 좋지만, 초안에서 일부가 실행 가능한 확정 사양처럼 보인다. 입력의 핵심 불확실성에는 “실제 `gpt-transcribe` 조합은 live smoke test로 확인해야 한다”가 있으므로, 지원 확인 전에는 필수 구현 계약이 아니라 adapter 후보와 fallback 조건으로 표현하는 편이 안전하다.

- [근거 추적성] 초안은 프로젝트 파일, 공식 문서, 기존 관례를 잘 반영하지만 사용자가 바로 따라갈 수 있는 evidence anchor가 부족하다. 예를 들어 `task4/assignments/taskD-5.md`, `task4/assignments/taskD-4.md`, OpenAI skill 문서, Codex custom agent 문서, transcription API 문서가 입력에는 있으나 초안 본문에는 확인 경로나 링크가 거의 드러나지 않는다.

- [구현 범위와 승인 경계] 초안 마지막의 “승인 후 구현 범위”는 적절하지만, 어떤 항목이 이번 답변의 설계 제안이고 어떤 항목이 사용자 승인 후 실제 생성 대상인지 더 분명히 나눌 필요가 있다. 특히 `skill`, `custom agent`, `rubric`, `scripts`, `tests`를 모두 만들겠다는 범위는 과제 산출물로는 타당해 보이나, 첫 구현 단계를 너무 크게 잡을 가능성이 있다.

- [rubric 범위] “rubric은 구조 코칭에만 쓴다”는 결론은 강점이지만, 초안의 구조 rubric에는 `근거 추적성` 축이 포함되어 있다. 이 축은 발표 구조 품질이라기보다 에이전트 평가 출력의 품질에 가깝다. rubric을 “발표 구조 rubric”과 “코치 출력 검증 조건”으로 분리할지 검토가 필요하다.

## 확인 필요

- Codex custom agent TOML에서 `sandbox_mode`와 model 상속 설정을 실제로 지원하는지 확인해야 한다.

- `gpt-transcribe`가 현재 API에서 `keywords`, `prompt`, `chunking_strategy=auto`, `verbose_json`, word/segment timestamp를 어떤 조합으로 지원하는지 smoke test 또는 공식 문서 재확인이 필요하다.

- D-5 과제에서 “agent를 만들라”는 요구가 Codex custom agent 파일까지 요구하는지, 아니면 skill 내부 역할 프롬프트와 실행 스크립트만으로 충분한지 확인하면 구현 범위를 더 줄일 수 있다.

- `그` 필러 판별을 사람이 검토하는 후보로 남기는 정책이 과제 평가 기준에 충분히 맞는지 확인이 필요하다. 정확성을 높이는 선택이지만 자동 피드백 산출물의 단순성은 낮아진다.

## 수정 제안

- custom agent 설정 관련 문장은 “문서 확인 후 지원되면” 또는 “지원되지 않으면 role prompt만 분리”처럼 조건부로 바꿔라. 확인된 필수 필드와 추정 설정을 분리하면 좋다.

- 전사기 섹션에서 API 옵션을 `필수`, `가능하면`, `smoke test 후 확정`으로 나누어라. timestamp가 없을 때의 fallback은 이미 잘 적혀 있으므로, 옵션 목록 자체에도 같은 불확실성 표시를 붙이면 된다.

- 결론 또는 검증 섹션에 근거 링크를 짧게 추가하라. 최소한 프로젝트 과제 파일 2개와 공식 문서 3개가 어떤 claim을 뒷받침하는지 연결하면 사용자가 검증하기 쉬워진다.

- 첫 구현 단계를 더 작게 제안하라. 예를 들어 1차는 transcript mode와 deterministic metrics fixture test, 2차는 media mode와 `gpt-transcribe`, 3차는 custom agent 구조 평가로 나누면 승인 후 작업 범위가 명확해진다.

- rubric을 두 층으로 정리하라. 발표 구조 평가는 `도입`, `핵심 메시지`, `전개`, `마무리`에 두고, `근거 추적성`은 coach output schema 또는 quality gate로 옮기는 편이 더 일관적이다.

## 요약

초안은 사용자의 실제 결정 지점에 잘 답하고, role만 또는 rubric만 두는 대안보다 `skill + deterministic metrics + 구조 코치` 구성을 추천한 점이 타당하다. 다만 custom agent 설정과 transcription API 옵션 일부가 확인된 사실처럼 읽힐 수 있고, 근거 anchor와 단계적 구현 범위가 조금 더 명확해야 한다.