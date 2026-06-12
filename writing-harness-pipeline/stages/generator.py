from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
GEN_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "gen_system.md"
GEN_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "gen_output.schema.json"


def generate(
    input_path: Path,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    brief = json.loads(input_path.read_text(encoding="utf-8"))
    prompt = build_prompt(brief)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=GEN_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(brief: dict) -> str:
    system_prompt = GEN_SYSTEM_PROMPT.read_text(encoding="utf-8")
    brief_json = json.dumps(brief, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

INPUT_JSON:
{brief_json}
"""
