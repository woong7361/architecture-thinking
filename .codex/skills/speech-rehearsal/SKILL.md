---
name: speech-rehearsal
description: "Transcribe a speech recording when needed, then diagnose filler usage, pace, transcript-visible delivery, structure, and reasoning with two independent reviewers. Use for speech rehearsal, presentation rehearsal, interview-answer review, or requests for transcript-based delivery and senior-level logic feedback."
---

# Speech Rehearsal

Turn one recording or transcript into an evidence-backed rehearsal diagnosis. Preserve the speaker's message and reasoning; diagnose them without silently rewriting them.

## Inputs

Accept either:

- an audio file supported by the OpenAI transcription endpoint;
- an audio-bearing container accepted by that endpoint;
- a UTF-8 transcript text file;
- a transcript JSON matching `schemas/transcript.schema.json`.

A known recording duration improves pace feedback. A presentation plan or intended outline is optional. If it is absent, reviewers must judge only what is present in the transcript.

Do not describe `gpt-transcribe` as understanding video. It transcribes audio. If a supplied video container is rejected, ask for an extracted audio file rather than inventing a conversion path.

## Workflow

1. If the user supplied media rather than a transcript, run `scripts/transcribe.py`. It reads `OPENAI_API_KEY` from the environment. Never create a key or edit `.env`.
2. Run `scripts/prepare_review.py` with the transcript, optional duration, and optional presentation plan. Treat the resulting `review-context.json` as immutable input for both reviewers.
3. Start the project custom agents `delivery_reviewer` and `senior_logic_reviewer` in parallel. Give both the same `review-context.json`. Tell each agent only where to return its JSON result; its role file, rubric, and output schema are the authoritative instructions.
4. Wait for both reviewers. Save their returned JSON without paraphrasing it as `delivery-review.json` and `logic-review.json`.
5. Run `scripts/aggregate_review.py`. It validates both outputs, verifies filler token references, computes counts from the AI labels, and renders `feedback.json` plus `feedback.md`.
6. Report transcript-only limitations. Do not claim to have assessed pitch, volume, emphasis, emotion, energy, or audience reaction unless a separate audio-aware evaluator actually inspected those signals.

The two reviewers must remain independent. Do not show one reviewer's output to the other before both have finished.

## Prompt ownership

Prompt-like judgment instructions have exactly one owner per reviewer:

- Delivery role: `references/roles/delivery-reviewer.md`
- Senior logic role: `references/roles/senior-logic-reviewer.md`

Evaluation criteria have exactly one owner per reviewer:

- Delivery rubric: `references/rubrics/delivery.yaml`
- Logic rubric: `references/rubrics/logic.yaml`

Output shape has exactly one owner per artifact under `schemas/`.

Scripts may load, hash, validate, aggregate, and render these resources. Do not add role text, evaluation criteria, examples of desired judgments, or hidden prompt suffixes to scripts, agent TOML, or runtime artifacts.

## Commands

Transcribe media:

```powershell
python -B .codex/skills/speech-rehearsal/scripts/transcribe.py recording.m4a --output transcript.json --language ko
```

Prepare one immutable review input:

```powershell
python -B .codex/skills/speech-rehearsal/scripts/prepare_review.py transcript.json --duration-seconds 260 --plan task4/assignments/taskD-4.md --output-dir .codex/skills/speech-rehearsal/runs/my-rehearsal
```

Validate and aggregate returned reviews:

```powershell
python -B .codex/skills/speech-rehearsal/scripts/aggregate_review.py --context .codex/skills/speech-rehearsal/runs/my-rehearsal/review-context.json --delivery .codex/skills/speech-rehearsal/runs/my-rehearsal/delivery-review.json --logic .codex/skills/speech-rehearsal/runs/my-rehearsal/logic-review.json --output-dir .codex/skills/speech-rehearsal/runs/my-rehearsal
```

If live transcription cannot run because the environment has no user-provided key, stop only the transcription step. The transcript-based review path remains usable.
