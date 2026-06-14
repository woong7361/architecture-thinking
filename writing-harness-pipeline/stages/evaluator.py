from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
EVAL_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "eval_system.md"
EVAL_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "eval_output.schema.json"


def evaluate(
    input_path: Path,
    draft_path: Path,
    rubric: dict,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    prompt = build_prompt(input_data=input_data, draft=draft, rubric=rubric)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=EVAL_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(input_data: dict, draft: dict, rubric: dict) -> str:
    system_prompt = EVAL_SYSTEM_PROMPT.read_text(encoding="utf-8")
    input_json = json.dumps(input_data, ensure_ascii=False, indent=2)
    draft_json = json.dumps(draft, ensure_ascii=False, indent=2)
    rubric_json = json.dumps(rubric, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

INPUT_JSON:
{input_json}

DRAFT_JSON:
{draft_json}

RUBRIC_JSON:
{rubric_json}
"""
