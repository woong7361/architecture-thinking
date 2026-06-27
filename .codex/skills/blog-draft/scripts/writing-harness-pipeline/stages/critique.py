from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
CRITIQUE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "critique_system.md"
CRITIQUE_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "critique_output.schema.json"


def critique(
    input_path: Path,
    draft_path: Path,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    prompt = build_prompt(input_data=input_data, draft=draft)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=CRITIQUE_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(input_data: dict, draft: dict) -> str:
    system_prompt = CRITIQUE_SYSTEM_PROMPT.read_text(encoding="utf-8")
    input_json = json.dumps(input_data, ensure_ascii=False, indent=2)
    draft_json = json.dumps(draft, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

INPUT_JSON:
{input_json}

DRAFT_JSON:
{draft_json}
"""
