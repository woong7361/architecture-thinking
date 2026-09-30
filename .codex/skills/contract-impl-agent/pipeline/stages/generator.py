from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE, LLMClient
from stages.scripts.payload import block, compose, read_prompt, workspace_block


PROJECT_DIR = Path(__file__).resolve().parent.parent
GEN_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "gen_impl.md"
# 모델에게는 구조화출력용(`*.codex`/`*.impl`)을 주고, 산출물 판정은 검증용 스키마가 한다.
GEN_MODEL_SCHEMA = PROJECT_DIR / "schemas" / "gen_output.codex.schema.json"


def generate(
    input_path: Path,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
    gen_prompt_path: Path | None = None,
    prose_slots: list[dict] | None = None,
    work_dir: Path | None = None,
    raw_path: Path | None = None,
    on_note=None,
) -> dict | None:
    """기준선 계약대로 구현을 쓴다. 게이트 결과·critique·eval·사람 테스트는 보지 않는다."""
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    system, user = build_prompt(
        input_data=input_data,
        gen_prompt_path=gen_prompt_path,
        prose_slots=prose_slots or [],
        work_dir=work_dir,
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=GEN_MODEL_SCHEMA,
        output_path=output_path,
        model=model,
        work_dir=work_dir,
        sandbox=SANDBOX_WORKSPACE_WRITE,
        raw_path=raw_path,
        on_note=on_note,
    )


def build_prompt(
    input_data: dict,
    gen_prompt_path: Path | None = None,
    prose_slots: list[dict] | None = None,
    work_dir: Path | None = None,
) -> tuple[str, str]:
    system = read_prompt(gen_prompt_path or GEN_SYSTEM_PROMPT)
    user = compose(
        block("INPUT_JSON", input_data),
        block("PROSE_SLOTS_JSON", prose_slots or []),
        workspace_block(work_dir, True),
    )
    return system, user
