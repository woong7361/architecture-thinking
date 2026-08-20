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
   - reader / guide / judgment blocks (author-only material; see "저자 영역 블록" below)
4. Show the inferred defaults and ask for confirmation in one compact message.
5. Generate an input JSON with `pipeline/intake_to_input.py`.
6. Validate and run the pipeline with `pipeline/run_draft.py`.
7. Report the final draft path, or the failed artifact path if the harness does not pass.
8. Capture user feedback on the draft (see "사용자 개인화" below).

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

독자 쪽은 제가 채울 수 없어 확인이 필요합니다.
- 독자가 원하는 것 / 그걸 막는 것 / 겉으로 드러난 문제 / 그 문제가 만드는 감정
- 같은 자리에 있었던 본인 장면, 독자가 직접 확인할 수 있는 것
- 버린 가설, 이 판단이 깨지는 조건

기본 프로필로 제안드릴까요, 아니면 직접 정해주시겠어요? 비워두면 초안에서 그 대목은 빠집니다.

나머지는 이대로 진행할까요? 바꾸고 싶은 톤, 강조점, 분량, 청자, 의도, 피할 사항이 있으면 알려주세요.
```

If the user says to proceed, use the proposed defaults. If the user gives changes, merge them into the brief.

## 저자 영역 블록

`brief.reader` / `brief.guide` / `brief.judgment`는 저자만 아는 것이다. 독자가 놓인 상황, 저자가 같은 자리에 있었던 장면, 버린 가설과 판단이 깨지는 조건이 여기 들어간다.

`raw_text`에서 추론해서 채우지 않는다. 비어 있으면 초안은 그 내용을 쓰지 않고, 그게 맞는 동작이다. 추론으로 채우면 초안의 결함이 아니라 거짓이 된다.

`piece_type`이 `essay` 또는 `retrospective`면 `reader`를 반드시 확인받는다. 기본 프로필 제안값과 각 항목의 뜻은 `references/intake-guide.md`에 있다.

## Input Contract

Use `pipeline/schemas/input.schema.json` as the canonical input contract. Do not duplicate the full JSON shape in this file.

Important contract points:

- Preserve original material in `brief.raw_text`.
- Put the full original material in `brief.raw_text` whenever possible, even when it is long.
- Do not summarize, shorten, excerpt, normalize, or omit parts of the user's material when building `brief.raw_text`.
- If context or tool limits make it difficult to include the full original material, stop and ask the user how to split or reference the source instead of silently shortening it.
- Put writing intent under `brief.intent`.
- Put audience under `brief.audience`.
- Put tone, length, emphasis, required points, and avoid rules under `brief.constraints`.
- Do not include `summary` or `intake_answers` in the MVP input.
- Put author-only material under `brief.reader`, `brief.guide`, and `brief.judgment`. Never infer these from `raw_text`.
- Validate every generated input through `pipeline/intake_to_input.py` or `pipeline/validate.py --artifact input` before running the harness.

## 피드백 캡처 (step 8)

초안 제시 후 사용자가 주목할 반응(부정/긍정)을 주면 기록한다. 근거·설계는 `docs/personalization-design.md` 참조.

1. `problem.md`의 "사용자 피드백 누적 > 항목"에 추가: `(YYYY-MM-DD, run_id, verdict=pos|neg) 반응 요약 → 교훈`. `run_id`는 `run_draft.py` 결과 JSON에서 얻는다.
2. 일반화 가능한 교훈이면 `memory.md`에 `- [neg|pos] (날짜, run_id) 교훈 한 줄` 추가.
3. 명백한 지속 취향이면 **사용자 확인 후** `soul.md`에 반영한다. 확인 없이 `soul.md`를 수정하지 않는다.

## Scripts

Use `pipeline/intake_to_input.py` to create and validate input JSON from intake values:

```bash
python -B .codex/skills/blog-draft/pipeline/intake_to_input.py \
  --raw-text-file /tmp/material.txt \
  --topic "..." \
  --intent "..." \
  --audience "..." \
  --tone "..." \
  --target-length "3000-4000 Korean characters" \
  --context-file /tmp/context.json \
  --output-dir .codex/skills/blog-draft/pipeline/inputs
```

`--context-file` takes the author-only blocks as one JSON object. Shape and asking rules live in `references/intake-guide.md`.

Use `pipeline/run_draft.py` to run the bundled harness:

```bash
python -B .codex/skills/blog-draft/pipeline/run_draft.py \
  .codex/skills/blog-draft/pipeline/inputs/a1b2c3d4_input.json \
  --provider claude   # or --provider codex (default)
```

By default, `run_draft.py` writes run artifacts to `runs/` (the sibling of `pipeline/`), and the fast loop writes each run under `runs/pending/`. Override the location with `--runs-dir` when needed.

The pipeline lives under `pipeline/`. Do not edit the original repository-level pipeline when using this skill unless the user explicitly asks to sync improvements back.
