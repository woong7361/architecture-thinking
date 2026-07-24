# Original User Input

그러면 언제 분리하고 언제 합치는 그런 결정적인 게이트가 있을까? 

AI가 명시적으로 세거나 할 수 있는 기준으로 

예를들어 단순 CRUD가 아니라 도메인 불변식이 한개 이상 존재한다거나 하는 식의 예시로


# Checked Context

# Checked project context

- `task3/assignments/taskC-1.md` identifies the primary problem as domain policy depending on external technology and its model, causing test cost and change propagation.
- Task C-1 says the practical decision is whether the value of protected business rules exceeds the added boundary and mapping cost.
- `task3/assignments/taskC-2.md` asks when the mapping cost of separating Domain and JPA Entity is worth paying and explicitly rejects always-separate and always-combine answers.
- The latest recheck concluded that JPA dependency harm is the primary cause, model divergence is an amplifier, and mapping is the cost.
- No open `PROBLEM.md` item directly applies.
- This turn asks for a countable, deterministic AI gate, not an assignment file edit.

# Design constraints

- A single business invariant should identify protection value but must not automatically force separation.
- Distinguish simple input/data validation from a domain invariant.
- Distinguish source dependency from demonstrated runtime/semantic dependency harm.
- The gate must be inspectable from code, tests, architecture rules, mappings, and documented adapters. It must not infer undocumented future changes.
- Missing scope must produce `NOT_EVALUABLE`, not a fabricated zero.
- The gate is a project policy proposal that needs calibration; it is not a universal industry law.
- The decision scope is per aggregate or bounded feature, not the whole application.
