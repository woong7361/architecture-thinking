from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import LLMClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
EVAL_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "eval_system.md"
EVAL_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "eval_output.schema.json"


def evaluate(
    input_path: Path,
    draft_path: Path,
    rubric: dict,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
    eval_output_schema: Path | None = None,
) -> dict | None:
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    system, user = build_prompt(input_data=input_data, draft=draft, rubric=rubric)
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=eval_output_schema or EVAL_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(input_data: dict, draft: dict, rubric: dict) -> tuple[str, str]:
    system = EVAL_SYSTEM_PROMPT.read_text(encoding="utf-8")
    input_json = json.dumps(input_data, ensure_ascii=False, indent=2)
    draft_json = json.dumps(draft, ensure_ascii=False, indent=2)
    rubric_json = json.dumps(rubric, ensure_ascii=False, indent=2)
    user = f"INPUT_JSON:\n{input_json}\n\nDRAFT_JSON:\n{draft_json}\n\nRUBRIC_JSON:\n{rubric_json}\n"
    return system, user
