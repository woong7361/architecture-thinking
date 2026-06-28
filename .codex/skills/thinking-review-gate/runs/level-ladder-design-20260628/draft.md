# Draft Answer

결론부터 말하면, thinking-review-gate에서 Level 1, Level 2, Level 3를 나누는 설계는 현재 프로젝트 목적에는 적절하다. 이유는 이 프로젝트가 "정확성 보장" 자체보다 "사용자가 검증 가능한 사고 표면"을 만드는 데 초점을 두고 있고, 모든 답변에 같은 무게의 검토를 강제하면 응답성과 생산성이 떨어지기 때문이다.

추천 구조는 다음과 같다.

- Level 1: 기본값. 빠른 inline verifier/self-review로 claim, 근거, 불확실성, 모순 여부만 짧게 본다.
- Level 2: 설계, 추천, 구현 방향, 대안 비교처럼 판단 비용이 있는 답변. sub-agent 또는 분리된 critique reviewer가 Markdown critique를 만들고, main agent가 문제 지점만 반영한다.
- Level 3: file hand-off, eval, gate, schema validation, runner, 재현 가능한 기록이 필요한 경우. critique와 eval을 분리하고 `validation.json.gate_result`로 pass/fail을 판단한다.

이 분리는 `ai-answer-accuracy-gate.md`와 현재 `SKILL.md`의 방향과 일치한다. 특히 Level 2에서 점수를 노출하지 않고 critique만 받으며, Level 3에서만 `eval.json`과 `validation.json`을 사용하는 점은 `PROBLEM.md`의 임시 결정과도 맞다. 점수가 일반 응답 수정 과정에 직접 노출되면 main agent가 실제 문제보다 점수 맞추기에 anchor될 위험이 있으므로, 점수는 Level 3 artifact로 제한하는 편이 낫다.

다만 초안은 두 가지를 보완해야 한다. 첫째, Level 1을 "self-review"라고 부를지 "inline verifier review"라고 부를지 용어를 맞춰야 한다. 현재 문서에는 두 표현이 섞여 있어 운영 규칙을 읽는 agent가 다르게 해석할 수 있다. 둘째, Level 3의 트리거를 "artifact와 gate가 필요한 검토"로만 쓰면 조금 넓다. review case, runner, schema validation, CI/hook 후보 검증, 재현 가능한 기록처럼 무거운 검토가 실제로 필요한 조건을 명시해야 한다.

대안은 세 가지다.

1. 모든 답변 Level 1만 적용한다. 빠르지만 설계/추천 답변에서 숨은 가정과 근거 누락을 놓칠 가능성이 크다.
2. 설계/추천부터 모두 Level 3로 보낸다. 재현성은 좋지만 대화 속도가 크게 느려지고, 검토 시스템 자체를 쓰는 비용이 커진다.
3. 현재처럼 Level 1/2/3 ladder를 둔다. 레벨 선택 규칙을 관리해야 하지만, 답변 속도와 검토 품질 사이의 균형이 가장 좋다.

따라서 현재 설계는 유지하되, 문장 하나를 이렇게 바꾸는 것이 좋다.

"Level 1은 모든 thinking-review-gate 적용 답변의 최소 inline verifier review, Level 2는 설계/추천/구현 방향처럼 판단 비용이 있는 답변에 대한 분리된 critique review, Level 3는 review case, file hand-off, eval, schema validation, runner, CI/hook 후보처럼 artifact와 pass/fail gate 또는 재현 가능한 기록이 필요한 경우에만 사용한다."

남은 불확실성은 실제 운영 데이터다. 이 설계가 비용 대비 효과적인지는 Level 1/2/3 실제 대화 사례를 2개 이상 모아, Level 2가 과하게 자주 호출되는지와 Level 3 artifact가 실제로 재작업을 줄이는지 확인해야 한다.
