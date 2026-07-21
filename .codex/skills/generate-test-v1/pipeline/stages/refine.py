from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import LLMClient


PROJECT_DIR = Path(__file__).resolve().parent.parent
REFINE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "refine_system.md"
REFINE_OUTPUT_SCHEMA = PROJECT_DIR / "schemas" / "gen_output.schema.json"


def refine(
    input_path: Path,
    draft_path: Path,
    critique_path: Path,
    refine_request: dict,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
    refine_prompt_path: Path | None = None,
) -> dict | None:
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    critique = json.loads(critique_path.read_text(encoding="utf-8"))
    system, user = build_prompt(
        input_data=input_data,
        draft=draft,
        critique=critique,
        refine_request=refine_request,
        refine_prompt_path=refine_prompt_path,
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=REFINE_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def build_prompt(input_data: dict, draft: dict, critique: dict, refine_request: dict, refine_prompt_path: Path | None = None) -> tuple[str, str]:
    prompt_path = refine_prompt_path or REFINE_SYSTEM_PROMPT
    system = prompt_path.read_text(encoding="utf-8")
    input_json = json.dumps(input_data, ensure_ascii=False, indent=2)
    draft_json = json.dumps(draft, ensure_ascii=False, indent=2)
    critique_json = json.dumps(critique, ensure_ascii=False, indent=2)
    refine_request_json = json.dumps(refine_request, ensure_ascii=False, indent=2)
    user = (
        f"INPUT_JSON:\n{input_json}\n\n"
        f"PREVIOUS_DRAFT_JSON:\n{draft_json}\n\n"
        f"CRITIQUE_JSON:\n{critique_json}\n\n"
        f"REFINE_REQUEST_JSON:\n{refine_request_json}\n"
    )
    return system, user
