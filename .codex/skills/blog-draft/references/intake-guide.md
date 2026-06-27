# Blog Draft Intake Guide

Ask only questions that materially change the draft. Infer the rest from the material.

## Default Inference

- Technical project notes usually become a developer retrospective.
- Architecture or workflow notes usually use a calm, concrete developer tone.
- If no audience is obvious, use "AI native 개발 방식에 관심 있는 개발자".
- If no length is specified, propose "3000-4000 Korean characters".
- Avoid hype, unsupported metrics, and claims not grounded in `raw_text`.

## Fields

- `topic`: a concise title-like subject.
- `raw_text`: the original material, preserved without summary.
- `piece_type`: `retrospective`, `essay`, or `technical_note`.
- `intent`: why the user wants the draft.
- `audience`: expected readers.
- `constraints.target_length`: desired length.
- `constraints.tone`: voice and distance.
- `constraints.emphasis`: what to foreground.
- `constraints.must_include`: required points.
- `constraints.avoid`: claims, tones, or topics to avoid.
