## 문제 지점

- [근거 앵커] 초안은 `.codex/skills/thinking-review-gate/scripts/critique.py`, `runner.py`, `validate.py`, `SKILL.md`에서 확인된 사실과 새로 제안하는 설계를 섞어 설명하지만, 각 claim 옆에 어떤 파일/컨텍스트에 근거한 것인지 표시하지 않는다. 사용자가 "이미 구현된 동작"과 "제안된 설계"를 구분하기 어렵다.

- [구현 여부 경계] `collect.py`, `propose_improvements.py`, `reports/latest/summary.json`, `reports/history/{timestamp}/...`, `--collect`, `--history`, `--auto-propose`가 실제로 존재하는 것처럼 읽힐 수 있다. 입력 컨텍스트에는 collector/proposal 구현이 아직 없다고 되어 있으므로, 초안은 이 항목들이 MVP 설계안 또는 제안 명령이라는 점을 명확히 해야 한다.

- [사용자 질문 해석] "병합된건을 감지하는걸 고려한거야?"를 초안은 attempt 병합과 중복 run 병합으로 해석했다. 이 해석은 유용하지만, 사용자가 말한 "병합된 건"이 proposal 적용/규칙 파일 반영/PR merge/attempt merge 중 무엇인지 확정되어 있지 않다. 초안은 이 해석 가정을 드러내지 않는다.

- [대안과 trade-off] collector 실행 시점, latest/history 저장 방식, proposal manual/auto 옵션에 대한 추천은 있으나, 각 선택지의 trade-off가 충분히 분리되어 있지 않다. 예를 들어 "runner 기본 동작에 넣기"와 "`--collect` 옵션으로 두기"의 장단점, "latest only"와 "history 보존"의 비용/효익이 더 명확해야 한다.

- [수집 스키마 구체성] axis와 critique 문제점을 수집한다고 답하지만, 최소 summary 구조가 없다. `run_id`, `attempts`, `final_attempt`, `weak_axes`, `score_reasons`, `critique_sections`, `duplicate_candidates` 같은 필드가 제안인지 예시인지도 불명확하다.

- [일관성/범위] "같은 입력 run 집합이면 같은 latest report"라고 하면서 "필요하면 timestamped report도 같이 남긴다", "기본은 latest만 쓰고 --history를 주면 history도 남긴다"고 설명한다. 큰 모순은 아니지만, 기본 동작과 옵션 동작이 한 문단 안에서 섞여 있어 결정 기준이 흐려진다.

- [자동 루프 안전장치] proposal은 manual이라고 결론 내리면서 `--auto-propose`를 제안한다. 이 옵션이 "proposal 파일 생성까지만"이라는 안전 경계를 말하긴 하지만, 자동 루프가 언제 실행되어도 되는지, 기준 파일 수정은 왜 별도 apply 단계여야 하는지 근거가 더 필요하다.

## 확인 필요

- "병합된 건"이 같은 run의 여러 attempt를 말하는지, 동일 입력으로 반복 생성된 run을 말하는지, 아니면 proposal이 실제 규칙 파일에 반영된 상태를 말하는지 확인해야 한다.

- collector/proposal을 지금 구현하려는 것인지, 설계 방향만 정하려는 것인지 확인해야 한다. 초안은 구현 명령과 파일명을 제안하므로 구현 범위에 따라 답변 톤이 달라져야 한다.

- report history를 기본으로 남길지, 옵션으로 남길지에 대한 사용자 선호가 필요하다. 회고/추적성이 중요하면 history 기본값이 달라질 수 있다.

- critique.md 파싱 실패 시 원문 전체 보존, 부분 파싱, run 제외 중 어떤 정책을 쓸지 정해야 한다.

## 수정 제안

- 답변 앞부분에 "확인된 현재 상태"와 "추천 설계"를 분리하라. 예: 현재는 Level 2 artifact와 validate 결과 구조만 있고 collector/proposal은 미구현이라는 점을 먼저 명시한다.

- `collect.py`, `propose_improvements.py`, `--collect`, `--history`, `--auto-propose`는 모두 "제안하는 MVP 인터페이스"라고 표시하라. 실제 존재하는 명령처럼 단정하지 말라.

- 사용자 질문별로 직접 답하는 구조로 바꾸라. 예: `collector는 언제 도나`, `여러 번 돌면 파일은 어떻게 되나`, `axis/critique를 수집하나`, `병합 감지는 어떻게 보나`, `proposal은 자동인가 수동인가`.

- "병합 감지" 섹션에는 해석 가정을 명시하라. 같은 run의 attempts 병합은 기본 정책으로 제안하고, 동일 input_hash 중복 run은 자동 병합하지 않고 후보 표시만 하는 이유를 trade-off와 함께 설명하라.

- 최소 report 구조 예시를 짧게 추가하라. 전체 JSON을 길게 쓰기보다 핵심 필드 목록으로 충분하다.

- proposal 자동화는 3단계로 구분하라: `manual propose`, `collector 후 proposal 파일 자동 생성`, `기준 파일 자동 apply`. 추천은 앞의 두 단계까지만 허용하고, 자동 apply는 사용자 승인 전 금지로 명확히 하라.

- 불확실한 부분에는 "제안", "MVP 기준", "확인 필요" 표현을 붙여 사실 claim과 설계 선택을 구분하라.

## 요약

초안은 사용자의 질문 대부분에 답하고 추천 방향도 실용적이지만, 구현된 사실과 제안 설계가 섞여 있고 "병합된 건"의 의미를 가정한 채 단정한다. 현재 미구현 상태를 먼저 밝히고, 질문별 직접 답변과 최소 스키마, 병합/자동화 정책의 trade-off를 보강하면 검증 가능한 답변에 가까워진다.