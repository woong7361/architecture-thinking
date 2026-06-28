# Project Context

- `AGENTS.md`: 이 프로젝트는 AI 작업 환경을 점진적으로 개선하는 것을 목표로 한다. 구현 전 기존 문맥을 확인하고, 사실/추정/의견을 구분하며, 설계/추천 판단에서는 대안과 trade-off를 고려하고, 작업 결과를 검증 가능한 형태로 마무리해야 한다.
- `.codex/skills/thinking-review-gate/SKILL.md`: 모든 적용 답변은 최소 Level 1 inline verifier review를 거친다. Level 2는 설계, 추천, 의사결정, 구현 방향, 대안 비교, 최신성/정책/API 변경 가능성처럼 여러 claim이 얽혔지만 file artifact나 pass/fail gate가 필요하지 않은 경우에 사용한다. Level 3는 사용자가 review case, file hand-off, eval, gate, runner, 재현 가능한 기록, schema validation, CI/hook 후보 검증을 명시하거나 artifact가 필요한 경우에 사용한다. 분류 규칙은 Level 3 조건 우선, 그다음 Level 2, 둘 다 아니면 Level 1이다.
- `.codex/skills/thinking-review-gate/prompts/verifier.md`: Level 2 verifier는 점수, 가중 평균, pass/fail gate를 만들지 않고 Markdown critique만 제공한다.
- `.codex/skills/thinking-review-gate/rubric.yaml`: Level 3 eval은 evidence_count, evidence_quality, claim_coverage, uncertainty_boundary, consistency, alternatives_tradeoff 축을 1-5로 평가하고, validate.py가 weighted_score와 gate_result를 계산한다.
- `PROBLEM.md`: 현재 결정은 Level 2에서 점수를 노출하지 않고, Level 3에서만 `eval.json`과 `validation.json`으로 점수와 gate를 기록하는 것이다. 남은 해결 조건은 실제 review case 2개 이상에서 verifier output을 확인하고, 점수 노출이 main agent 응답을 과도하게 anchor하지 않는지 판단하는 것이다.
- `thinking-review-gate-checklist.md`: 단일 skill 안에서 Level 1/2/3 ladder를 만들고, JSON Schema는 Level 3 `eval.json`에만 적용한다는 MVP 방향은 체크되어 있다. 다만 Level 1/2 실제 대화 리허설, claim 경계 디버깅, Markdown 응답 템플릿 자연스러움 확인 등은 아직 남아 있다.
- 기존 `level3-design-judgment-20260628-a2` run은 gate_result=pass였지만 critique에서 적절성 기준, PROBLEM.md 근거 성격, Level 3 진입 경계, 운영 비용 검증 항목을 더 명확히 하라고 지적했다.
