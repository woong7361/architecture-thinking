from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import LLMClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
GEN_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "gen_system.md"
GEN_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "gen_output.schema.json"


def generate(
    input_path: Path,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
) -> dict | None:
    brief = json.loads(input_path.read_text(encoding="utf-8"))
    system, user = build_prompt(brief)
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=GEN_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(brief: dict) -> tuple[str, str]:
    system = GEN_SYSTEM_PROMPT.read_text(encoding="utf-8")
    brief_json = json.dumps(brief, ensure_ascii=False, indent=2)
    user = f"INPUT_JSON:\n{brief_json}\n"
    return system, user
