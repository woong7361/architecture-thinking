# L1 - Outbound Adapter Layer

## Responsibility

Create outbound adapters for the current target.

This layer implements outbound contracts using concrete external mechanisms. It translates between stable inner contracts and external technical details. It must not change the inner contracts.

## Allowed Surface

Use only the paths provided by `allowed_paths`. Do not create files outside those paths.

Implement only the outbound contracts listed in `CONTRACTS` or required by already accepted inner code. Do not create new contracts to make the adapter convenient.

## Required Shape

- Implement the outbound contracts using concrete technologies or external systems specified by the target context.
- Keep technology-specific models, clients, annotations, schemas, and SDK types inside this layer.
- If inner and external models are distinct, perform mapping inside this layer.
- Register produced implementations using the local project's established mechanism when runtime wiring requires it.
- Return absence, failure, and conflict states exactly as the outbound contract expects.

## External Contract Handling

- Use external endpoint names, schema names, field names, status meanings, and configuration keys only when they are explicitly provided by `CONTRACTS`, target context, or already accepted code.
- Do not infer external protocol details from common conventions.
- Do not treat transport success as business success unless the target context explicitly defines that equivalence.
- If a concrete integration is not exercised by the configured gate, state that gap in `notes`.

## Persistence Or State Adapter Rules

When this layer persists or loads state:

- Keep storage models inside the adapter layer.
- Do not expose storage models through inner contracts.
- Preserve identity, versioning, ordering, deletion, and conflict semantics required by the contract.
- Do not overwrite concurrent or externally changed state unless the contract explicitly allows it.
- Translate storage-specific errors into the contract's error surface or report the missing mapping in `notes`.

## External Call Adapter Rules

When this layer calls an external service:

- Use configured base values or clients from the target context.
- Do not invent defaults that hide missing configuration.
- Treat unknown or failed calls conservatively according to the contract.
- Keep request and response DTOs inside the adapter layer unless the contract explicitly exposes them.

## Notes Guidance

Use `notes` for unverified external assumptions, contract gaps, concurrency risks, missing timeout/retry/cancel semantics, and adapter code not covered by the current gate.
