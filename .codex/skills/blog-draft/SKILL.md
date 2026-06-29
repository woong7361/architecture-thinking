---
name: blog-draft
description: Turn raw notes, files, or conversation material into a blog draft by preserving raw_text, asking only the missing intent/tone/audience/length/emphasis/avoidance questions, generating a valid writing-harness input.json, and running the bundled writing-harness pipeline. Use when the user asks for a blog draft, essay draft, developer retrospective draft, or says "이 재료로 블로그 초안 작성해줘".
---

# Blog Draft

## Overview

Create a blog draft from raw material through a short intake conversation and the bundled writing harness. Preserve the user's original material in `brief.raw_text`; do not replace it with a summary.

## Workflow

1. Read the user's material from the conversation or referenced UTF-8 text files.
2. Infer draft defaults from the material before asking questions.
3. Ask only for missing or high-impact choices:
   - provider (codex / claude) — which LLM backend to run the pipeline with
   - tone
   - emphasis
   - target length
   - audience
   - intent
   - avoid list
4. Show the inferred defaults and ask for confirmation in one compact message.
5. Generate an input JSON with `scripts/intake_to_input.py`.
6. Validate and run the pipeline with `scripts/run_draft.py`.
7. Report the final draft path, or the failed artifact path if the harness does not pass.

## Intake Rules

Use inference first. If the material strongly implies a value, propose it as the default instead of asking an open-ended question.

Good intake message:

```text
재료를 읽어보니 개발자 회고 초안이 잘 맞아 보입니다.

제가 잡은 기본값은 이렇습니다.
- 톤: 차분하고 구체적인 개발자 회고
- 청자: AI native 개발 방식에 관심 있는 개발자
- 분량: 3000-4000자
- 강조점: skill이 의도를 질문하고 raw_text를 보존한다는 점
- 피할 것: AI가 모든 것을 자동으로 해결한다는 과장

이대로 진행할까요? 바꾸고 싶은 톤, 강조점, 분량, 청자, 의도, 피할 사항이 있으면 알려주세요.
```

If the user says to proceed, use the proposed defaults. If the user gives changes, merge them into the brief.

## Input Contract

Use `scripts/writing-harness-pipeline/schemas/input.schema.json` as the canonical input contract. Do not duplicate the full JSON shape in this file.

Important contract points:

- Preserve original material in `brief.raw_text`.
- Put the full original material in `brief.raw_text` whenever possible, even when it is long.
- Do not summarize, shorten, excerpt, normalize, or omit parts of the user's material when building `brief.raw_text`.
- If context or tool limits make it difficult to include the full original material, stop and ask the user how to split or reference the source instead of silently shortening it.
- Put writing intent under `brief.intent`.
- Put audience under `brief.audience`.
- Put tone, length, emphasis, required points, and avoid rules under `brief.constraints`.
- Do not include `summary` or `intake_answers` in the MVP input.
- Validate every generated input through `scripts/intake_to_input.py` or `validate.py --artifact input` before running the harness.

## Scripts

Use `scripts/intake_to_input.py` to create and validate input JSON from intake values:

```bash
python -B .codex/skills/blog-draft/scripts/intake_to_input.py \
  --raw-text-file /tmp/material.txt \
  --topic "..." \
  --intent "..." \
  --audience "..." \
  --tone "..." \
  --target-length "3000-4000 Korean characters" \
  --output-dir .codex/skills/blog-draft/scripts/writing-harness-pipeline/inputs
```

Use `scripts/run_draft.py` to run the bundled harness:

```bash
python -B .codex/skills/blog-draft/scripts/run_draft.py \
  .codex/skills/blog-draft/scripts/writing-harness-pipeline/inputs/a1b2c3d4_input.json \
  --provider claude   # or --provider codex (default)
```

By default, `run_draft.py` writes run artifacts to `writing-harness-pipeline/runs` under the current workspace when that directory exists. Override this with `--runs-dir` when needed.

The pipeline copy lives under `scripts/writing-harness-pipeline/`. Do not edit the original repository-level pipeline when using this skill unless the user explicitly asks to sync improvements back.
