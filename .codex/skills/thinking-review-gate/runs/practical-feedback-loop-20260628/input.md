# Original User Input

그러면 collector를 추가한다면 어떻게 할거고
느린 개선 루프는 어떻게 추가할거야

실전에서 쓸 수 있게 만들려면

# Checked Context

- .codex/skills/thinking-review-gate/SKILL.md: Level 2 file hand-off eval/gate는 runs/{run_id}/input.md와 attempts/{n}/draft.md, critique.md, eval.json, validation.json을 사용한다. Original User Input에는 사용자 원문을 그대로 저장해야 한다.
- .codex/skills/thinking-review-gate/scripts/validate.py: validation.json에 scores, score_reasons, weak_axes, weighted_score, gate_result를 저장한다.
- .codex/skills/thinking-review-gate/scripts/critique.py: run 생성, attempt 추가, validation 재실행, cleanup은 있지만 run들을 집계하는 collector는 없다.
- .codex/skills/thinking-review-gate/rubric.yaml: 평가 축은 evidence_count, evidence_quality, claim_coverage, uncertainty_boundary, consistency, alternatives_tradeoff이다.
- PROBLEM.md: 과거 verifier 출력과 hand-off 문제는 resolved로 기록되어 있으며, 현재 기준은 점수와 pass/fail을 분리하고 artifact에만 남기는 방향이다.
