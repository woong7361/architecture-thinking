# L0 - Use Case Layer

## Responsibility

Create the application/use-case layer for the current target.

This layer implements the inbound contract and coordinates the operation by calling domain objects and outbound contracts. It owns orchestration, not business rule definitions and not technical I/O details.

## Allowed Surface

Use only the paths provided by `allowed_paths`. Do not create files outside those paths.

Use exact type names, method names, constructor signatures, input models, output models, and error types from `CONTRACTS`. If a required name is not present in `CONTRACTS`, do not invent it.

## Required Shape

- Implement the inbound contract named in `CONTRACTS`.
- Receive collaborators through constructor parameters or the local project's established dependency style.
- Depend on contracts and domain types, not concrete technical implementations.
- Do not add framework annotations unless this layer's contract explicitly requires them.
- Do not import adapter, protocol, persistence, runtime container, or external SDK types.

## Behavior

- Follow the externally visible behavior implied by `CONTRACTS`, acceptance criteria, and target context.
- Preserve the required operation order when the order changes observable behavior.
- Delegate business decisions and state transitions to domain objects or policies.
- Use outbound contracts for external reads, writes, calls, notifications, or other side effects.
- Return the output model required by the inbound contract.
- Throw or return errors using the contract's existing error surface.

## Rule Placement

- Put invariant checks, state transitions, and domain calculations in the domain model or domain policy when those abstractions exist.
- Put sequencing, transaction-sized workflow, and result assembly in the use-case layer.
- Do not duplicate domain rules as field-level condition checks in the use-case layer.
- Do not perform technical mapping, persistence mapping, protocol mapping, or wire formatting in this layer.

## Notes Guidance

Use `notes` for contract gaps, ambiguous absence semantics, missing compensation/cancel/retry contracts, ordering risks, and assumptions the gate cannot verify. Do not change frozen contracts to resolve those issues.
