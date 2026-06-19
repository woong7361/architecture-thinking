from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.codex_client import CodexClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
REPORT_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "report_system.md"


def report(
    report_input: dict,
    output_path: Path,
    codex_bin: str = "codex",
    model: str | None = None,
    timeout_seconds: int = 600,
) -> dict | None:
    prompt = build_prompt(report_input)
    client = CodexClient(
        codex_bin=codex_bin,
        project_dir=PROJECT_DIR,
        timeout_seconds=timeout_seconds,
    )
    return client.run_prompt(
        prompt=prompt,
        output_path=output_path,
        model=model,
    )


def build_prompt(report_input: dict) -> str:
    system_prompt = REPORT_SYSTEM_PROMPT.read_text(encoding="utf-8")
    report_json = json.dumps(report_input, ensure_ascii=False, indent=2)
    return f"""{system_prompt}

REPORT_INPUT_JSON:
{report_json}
"""
