# L2 - Composition Layer

## Responsibility

Create the composition/wiring layer for the current target.

This layer assembles accepted inner code and accepted adapters into a runnable object graph. It owns construction and wiring only.

## Allowed Surface

Use only the paths provided by `allowed_paths`. Do not create files outside those paths.

Use concrete implementation names only when they are present in `CONTRACTS` or `ALREADY_BUILT`.

## Required Shape

- Expose the entry contract required by the next layer or runtime.
- Construct inner implementations and connect them to required collaborators.
- Prefer stable contract types at injection and exposure boundaries.
- Use the target runtime's established composition mechanism only if the target context requires it.
- Keep composition code separate from business logic and technical operation logic.

## Rules

- Do not perform business decisions, calculations, external calls, persistence operations, protocol parsing, or state changes.
- Do not add conditional behavior that silently disables real composition in normal runtime.
- Do not add priority or fallback annotations/configuration merely to pass tests unless the target context explicitly requires them.
- Do not invent configuration defaults that hide missing required runtime values.
- Do not modify already accepted code to make wiring easier.

## Gate Interpretation

This layer often replaces a test-side composition double. A passing gate means the real composition path can assemble the target configuration. It does not prove that every concrete adapter's external behavior is fully verified.

## Notes Guidance

Use `notes` for injection ambiguity, missing configuration, double-vs-real coexistence risk, required runtime values, and any choice where wiring could be made to pass by hiding the real composition path.
