from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
ANALYZE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "analyze_system.md"
ANALYSIS_SCHEMA = PROJECT_DIR / "schemas" / "analysis.schema.json"


def analyze(
    analysis_input: dict,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    prompt = build_prompt(analysis_input)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=ANALYSIS_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(analysis_input: dict) -> str:
    system_prompt = ANALYZE_SYSTEM_PROMPT.read_text(encoding="utf-8")
    analysis_json = json.dumps(analysis_input, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

ANALYSIS_INPUT_JSON:
{analysis_json}
"""
