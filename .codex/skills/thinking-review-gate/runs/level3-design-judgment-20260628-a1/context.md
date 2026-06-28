# Project Context

- `AGENTS.md` says implementation/design judgments should check existing context first, separate facts from estimates/opinions, compare alternatives and trade-offs, and finish in a verifiable form.
- `.codex/skills/thinking-review-gate/SKILL.md` defines Level 1 as inline verifier review for default/low-risk answers, Level 2 as sub-agent inline critique review for design/recommendation/decision/implementation direction when no file artifact or pass/fail gate is needed, and Level 3 as file hand-off agentic review when review case, file hand-off, eval, gate, runner, reproducible record, schema validation, CI/hook candidate validation, or artifacted review is required.
- `.codex/skills/thinking-review-gate/SKILL.md` states the level split exists to balance answer speed/productivity and answer quality; all applied answers receive at least Level 1.
- `PROBLEM.md` has an open issue dated 2026-06-28 about verifier output and hand-off. It records unresolved questions around file hand-off, critique/eval separation, and score exposure anchoring the main agent.
- `PROBLEM.md` temporary decision: ordinary verifier output should not expose axis scores; critique gives problem points and revision suggestions; axis scores/calculation are only for debugging or review-case evaluation.
- `thinking-review-gate-checklist.md` records the intended MVP: one `thinking-review-gate` skill, not v1/v2; Level 1 inline self-review; Level 2 critique sub-agent inline review; Level 3 file hand-off agentic review; JSON Schema only for Level 3 `eval.json` and test fixtures.
- `rubric.yaml` evaluates evidence_count, evidence_quality, claim_coverage, uncertainty_boundary, consistency, and alternatives_tradeoff. Minimum weighted score is 3.6.
- `validate.py` calculates weighted_score from eval axis scores and sets `gate_result`; axes with score below 4 become `weak_axes`.
