# Draft Answer

결론부터 말하면, 현재 `thinking-review-gate`의 목적에는 Level 1, Level 2, Level 3로 검토 강도를 나누는 설계가 조건부로 적절하다. 여기서 “적절하다”는 뜻은 낮은 위험 답변의 속도를 유지하면서, 설계 판단과 재현 가능한 검토에는 더 강한 검토 비용을 지불하도록 경계를 나누는 것이 현재 프로젝트의 MVP 운영 방식에 맞는다는 의미다. 3단계 자체가 보편적 정답이라는 뜻은 아니다.

판단 기준은 네 가지다. 첫째, 빠른 응답을 과도하게 늦추지 않아야 한다. 둘째, 설계/추천/구현 방향처럼 실패 비용이 있는 답변은 main agent의 자기확신만으로 끝내지 않아야 한다. 셋째, review case, schema validation, runner, gate처럼 재현 가능한 기록이 필요한 경우에는 artifact가 남아야 한다. 넷째, 점수와 gate가 일반 대화의 판단을 과도하게 anchor하지 않도록 사용자-facing 흐름과 Level 3 artifact 흐름을 분리해야 한다.

이 기준에서 현재 3단계 설계는 프로젝트 문맥과 잘 맞는다. `AGENTS.md`는 설계 판단에서 문맥 확인, 사실/추정/의견 분리, 대안과 trade-off, 검증 가능한 마무리를 요구한다. `SKILL.md`는 모든 적용 답변에 Level 1을 기본 적용하고, 설계/추천/의사결정은 Level 2로, 재현 가능한 artifact와 gate가 필요한 경우는 Level 3로 올리도록 정의한다. `PROBLEM.md`는 점수 노출과 file hand-off를 일반 답변에 섞을 때 생길 수 있는 anchoring 위험을 보여주며, 현재 결정도 점수는 Level 3 artifact에만 남기는 쪽이다. `thinking-review-gate-checklist.md` 역시 단일 skill 안의 Level ladder와 Level 3 전용 JSON Schema라는 MVP 방향을 기록한다.

추천 판정 규칙은 지금처럼 `Level 3 조건 우선 -> Level 2 조건 -> Level 1 기본값`을 유지하는 것이다. Level 1은 낮은 위험의 빠른 설명, 짧은 판단, 단순 요약에서 main agent가 inline으로 claim, 근거, 불확실성을 짧게 점검하는 단계다. Level 2는 Level 3 조건이 없고, 설계/추천/의사결정/구현 방향처럼 판단 비용이 있는 경우에 sub-agent inline critique를 받아 수정하는 단계다. Level 3는 review case, file hand-off, eval, runner, schema validation, pass/fail gate, CI/hook 후보 검증처럼 재현 가능한 기록이나 판정 자체가 필요한 경우에만 쓰는 단계다.

대안과 trade-off를 보면 현재안이 가장 균형적이다. Level 1만 유지하면 빠르지만 설계 판단에서 자기검토의 blind spot을 줄이기 어렵다. Level 1과 Level 3만 유지하면 구조는 단순하지만 중간 단계가 사라져, 설계 판단마다 과도한 file hand-off를 하거나 반대로 Level 1에 머물러 검토 강도가 부족해질 수 있다. 모든 검토를 Level 3로 통일하면 기록과 재현성은 좋아지지만 일반 대화 속도와 운영 비용이 크게 나빠지고, 점수/gate가 대화 판단을 과도하게 anchor할 위험도 커진다. 현재 3단계는 Level 2가 중간 완충 역할을 하므로, 대부분의 설계 판단은 빠른 critique로 처리하고 정말 기록이 필요한 경우만 Level 3로 올릴 수 있다.

다만 유지 조건이 있다. Level 2와 Level 3의 경계는 계속 선명해야 한다. “설계/추천이면 Level 2”가 아니라 “Level 3 조건이 없고 설계/추천이면 Level 2”라고 써야 한다. Level 3도 단순히 파일을 남기는 단계가 아니라 critique/eval 분리, schema validation, runner, validation gate까지 포함하는 재현 가능한 검토 흐름으로 이해해야 한다. 또한 Level 2/3 비용은 아직 운영 데이터로 충분히 검증된 것이 아니므로, 실제 대화에서 응답 지연, 발견된 수정점 수, artifact 재사용성, gate 실패 원인 식별 가능성, 재검토 루프의 효용을 관찰해야 한다.

따라서 현재 설계 판단은 “3단계 구조 유지”가 맞다. 단, 문서에는 Level 3 우선 판정, Level 2의 조건부 적용, Level 3의 정확한 진입 조건, 점수 노출 제한, 남은 운영 검증 항목을 더 명시하는 편이 좋다.
