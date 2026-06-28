# Draft Answer

현재 `thinking-review-gate`의 목적과 이 프로젝트의 운영 제약을 기준으로 보면, Level 1/2/3으로 검토 강도를 나누는 설계는 적절하다. 다만 이 판단은 "3단계가 항상 정답"이라는 뜻이 아니라, 현재 MVP 단계에서 답변 속도와 검증 가능성 사이의 비용을 조절하기에 가장 현실적인 구조라는 조건부 판단이다.

근거는 네 가지다.

1. `AGENTS.md`는 설계/판단 답변에서 기존 문맥 확인, 사실/추정/의견 분리, 대안과 trade-off 비교, 검증 가능한 마무리를 요구한다. 따라서 검토 장치 자체는 필요하다.
2. `.codex/skills/thinking-review-gate/SKILL.md`는 Level 1을 기본 inline verifier review, Level 2를 sub-agent inline critique review, Level 3를 file hand-off agentic review로 정의한다. 이 구분은 검토 강도와 비용을 단계화한다.
3. `PROBLEM.md`에는 file hand-off, critique/eval 분리, score exposure anchoring 문제가 아직 open으로 남아 있다. 따라서 모든 검토를 무조건 Level 3로 올리기보다는, 일반 답변에서는 점수 노출을 피하고 필요한 경우에만 artifact/gate를 쓰는 경계가 필요하다.
4. `thinking-review-gate-checklist.md`도 단일 skill 안에서 Level 1/2/3 ladder를 만들고, JSON Schema는 Level 3 `eval.json`과 테스트 fixture에만 적용한다는 MVP 방향을 기록한다.

추천하는 판정 규칙은 `Level 3 조건 우선 -> Level 2 조건 -> Level 1 기본값`이다.

- Level 1: 낮은 위험의 빠른 설명, 짧은 판단, 단순 요약에 적용한다. main agent가 inline으로 claim, 근거, 불확실성을 짧게 점검한다.
- Level 2: 설계, 추천, 의사결정, 구현 방향처럼 판단 비용이 있지만 file artifact나 pass/fail gate가 필요하지 않은 경우에 적용한다. 여기서는 sub-agent critique를 받아 문제 지점과 수정 제안을 반영한다.
- Level 3: review case, file hand-off, eval, runner, schema validation, 재현 가능한 기록, CI/hook 후보 검증처럼 artifact 자체가 품질 판단의 일부가 되는 경우에만 적용한다.

대안과 trade-off는 다음과 같다.

- Level 1만 유지: 빠르고 단순하지만, 설계/추천/구현 방향처럼 claim이 얽힌 답변에서 main agent의 자기확신을 걸러내기 어렵다.
- Level 1과 Level 3만 유지: 구조는 단순하지만 중간 지대가 없다. 설계 판단마다 file hand-off로 올라가면 운영 비용이 커지고, 그렇다고 Level 1에 머물면 검토 강도가 부족할 수 있다.
- 현재 3단계 유지: Level 2가 중간 완충 역할을 한다. 대부분의 설계/추천 판단은 빠른 critique로 처리하고, 기록과 gate가 필요한 경우만 Level 3로 올릴 수 있다. 현재 프로젝트의 "가벼운 구조부터 시작하고 반복 개선한다"는 방향과 가장 잘 맞는다.

따라서 초안의 핵심 방향은 맞지만, 표현은 더 정밀해야 한다. 특히 "설계/추천/구현 방향은 Level 2"라고만 쓰면 Level 3 조건과 충돌할 수 있으므로, Level 3 조건을 먼저 판정한다고 명시해야 한다. 또한 Level 2는 단순한 critique가 아니라 sub-agent inline critique이고, Level 3는 단순 artifact 저장이 아니라 eval, validation, runner, gate를 포함하는 재현 가능한 검토 흐름이다.

남은 불확실성도 있다. Level 2 sub-agent 호출 비용이 실제 대화 흐름에서 과하지 않은지, Level 3 file hand-off가 충분한 디버깅 이득을 주는지, 점수 노출 제한이 main agent의 score anchoring을 줄이는지는 실제 review case를 2개 이상 돌려 확인해야 한다. 따라서 지금은 3단계 설계를 유지하되, Level 2/3 진입 조건과 사용자-facing score 노출 정책을 계속 관찰하는 것이 좋다.
