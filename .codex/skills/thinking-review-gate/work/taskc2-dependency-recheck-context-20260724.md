# Checked project context

- `task3/assignments/taskC-1.md` concludes that the core limitation is not the number of layers but domain policy depending on external technology and its model. Keeping that direction while merely splitting files does not reduce test cost or change propagation.
- Task C-1 also says DDD decides what is worth protecting, and Hexagonal Architecture and DIP are methods to stop DB, HTTP, and external APIs from determining the domain model's shape and tests.
- Task C-1's final trade-off criterion is whether the value of the business rules being protected is greater than the added ports, adapters, boundaries, and mapping cost.
- `task3/assignments/taskC-2.md` asks specifically whether the mapping cost of separating Domain and JPA Entity is worthwhile.
- The previous answer summarized the criterion as whether the database and business models change differently enough that isolation value exceeds mapping cost.
- No `PROBLEM.md` open item directly applies.

# Decision to review

Determine whether the previous summary omitted the dependency problem from Task C-1 and replace it with a criterion consistent with Task C-1.

# Evidence and constraints

- Primary evidence is the checked Task C-1 conclusion in the project.
- Distinguish source dependency on JPA APIs from semantic/runtime dependency on proxy, lazy loading, persistence context, managed/detached state, and dirty checking.
- Do not claim that any `@Entity` annotation automatically makes separation worthwhile.
- Model/schema mismatch is a useful signal and benefit amplifier, but it is not necessary for dependency leakage and is not sufficient to justify a rich separate Domain model.
- This turn is a re-evaluation and explanation, not an assignment file edit.
