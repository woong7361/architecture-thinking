from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
REFINE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "refine_system.md"
REFINE_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "gen_output.schema.json"


def refine(
    input_path: Path,
    draft_path: Path,
    critique_path: Path,
    refine_request: dict,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    critique = json.loads(critique_path.read_text(encoding="utf-8"))
    prompt = build_prompt(
        input_data=input_data,
        draft=draft,
        critique=critique,
        refine_request=refine_request,
    )
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=REFINE_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(input_data: dict, draft: dict, critique: dict, refine_request: dict) -> str:
    system_prompt = REFINE_SYSTEM_PROMPT.read_text(encoding="utf-8")
    input_json = json.dumps(input_data, ensure_ascii=False, indent=2)
    draft_json = json.dumps(draft, ensure_ascii=False, indent=2)
    critique_json = json.dumps(critique, ensure_ascii=False, indent=2)
    refine_request_json = json.dumps(refine_request, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

INPUT_JSON:
{input_json}

PREVIOUS_DRAFT_JSON:
{draft_json}

CRITIQUE_JSON:
{critique_json}

REFINE_REQUEST_JSON:
{refine_request_json}
"""
