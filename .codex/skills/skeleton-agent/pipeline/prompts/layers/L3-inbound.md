# L3 - Inbound Adapter Layer

## Responsibility

Create the inbound adapter layer for the current target.

This layer receives an external request or trigger, translates it into the inbound contract's input model, invokes the inbound contract, and translates the result or error back to the external representation.

## Allowed Surface

Use only the paths provided by `allowed_paths`. Do not create files outside those paths.

Use protocol names, routes, message names, field names, status mappings, and media formats only when they are explicitly provided by `CONTRACTS` or target context.

## Required Shape

- Depend on the inbound contract, not the concrete use-case implementation.
- Keep external request/trigger models inside this layer unless the contract explicitly reuses them.
- Translate external input into the inbound contract input model.
- Invoke exactly the intended inbound operation.
- Translate success output into the external success representation required by the target context.
- Translate contract/domain errors into the external error representation required by the target context.

## Rules

- Do not call outbound contracts or outbound adapters directly.
- Do not perform business rule decisions in this layer.
- Do not perform persistence, external side effects, or domain state changes directly.
- Do not swallow errors and return success.
- Do not invent protocol routes, status codes, message names, or error formats.
- Do not couple the external representation to the inner output model unless the target context explicitly requires it.

## Error Mapping

- Use the target context's explicit error mapping.
- If a required mapping is missing, do not guess silently.
- If a technical conflict from an inner adapter can cross this boundary, map it only when the target context or existing accepted code makes that path observable.
- Keep protocol error formatting in this layer.

## Gate Interpretation

This layer often completes the full external-to-inner path. A passing gate means the configured entry path reaches the accepted inner system and returns expected externally observable results. It does not prove unrelated protocol routes or untested validation branches.

## Notes Guidance

Use `notes` for response-model coupling, missing input validation rules, unverified error handlers, ambiguous protocol negotiation, and any error mapping inferred from incomplete context.
