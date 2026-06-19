from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
KEYWORD_EXTRACT_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "keyword_extract_system.md"
KEYWORD_EXTRACTION_SCHEMA = PROJECT_DIR / "schemas" / "keyword_extraction.schema.json"


def keyword_extract(
    batch_input: dict,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    prompt = build_prompt(batch_input)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=KEYWORD_EXTRACTION_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(batch_input: dict) -> str:
    system_prompt = KEYWORD_EXTRACT_SYSTEM_PROMPT.read_text(encoding="utf-8")
    batch_json = json.dumps(batch_input, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

BATCH_INPUT_JSON:
{batch_json}
"""
