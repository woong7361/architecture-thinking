# Project Context

- Repository root: C:\Users\이현웅\Desktop\project\thinking
- AGENTS.md requires checking project context first, using UTF-8 for text files, avoiding unsupported assumptions, and not editing rule/problem files without user confirmation.
- `.codex/skills/thinking-review-gate/SKILL.md` defines a three-level review model:
  - Level 1: inline self-review for default/low-risk answers.
  - Level 2: inline critique review for design, recommendation, decision, implementation direction, alternatives, latest/policy/API-change-sensitive answers when no file artifact or pass/fail gate is needed.
  - Level 3: file hand-off agentic review when the user explicitly asks for review case, file hand-off, eval, gate, runner, reproducible record, schema validation, CI/hook candidate validation, or when artifacted review is needed.
- The SKILL.md rationale says the levels exist to balance answer speed/productivity and answer quality. It says all applied answers get at least Level 1, Level 2 is used for heavier decision/recommendation cases, and Level 3 is reserved for artifact/gate needs.
- `PROBLEM.md` has an open issue dated 2026-06-28 about thinking-review-gate verifier output and hand-off. It records unresolved questions: whether to use file hand-off, whether to separate critique and eval, and whether score exposure anchors the main agent.
- The temporary decision in `PROBLEM.md` is: normal verifier output should not expose axis scores; critique should provide problem points and revision suggestions; axis scores and calculation are only for debugging or review-case evaluation.
- `rubric.yaml` evaluates evidence count, evidence quality, claim coverage, uncertainty boundary, consistency, and alternatives/tradeoff. Minimum weighted score is 3.6.
- `validate.py` calculates weighted score and pass/fail from eval.json; axes with score below 4 become weak_axes.
