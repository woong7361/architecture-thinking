from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
EVALUATE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "evaluate_system.md"
EVAL_SCHEMA = PROJECT_DIR / "schemas" / "eval.schema.json"


def evaluate(
    eval_input: dict,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    prompt = build_prompt(eval_input)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_schema=EVAL_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(eval_input: dict) -> str:
    system_prompt = EVALUATE_SYSTEM_PROMPT.read_text(encoding="utf-8")
    eval_json = json.dumps(eval_input, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

EVAL_INPUT_JSON:
{eval_json}
"""
