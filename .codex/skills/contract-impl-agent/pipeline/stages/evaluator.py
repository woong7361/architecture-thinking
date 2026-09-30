from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE, LLMClient
from stages.scripts.payload import CRITIQUE_ECHO, GATE_ECHO, block, compose, draft_without, read_prompt, workspace_block


PROJECT_DIR = Path(__file__).resolve().parent.parent
EVAL_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "eval_impl.md"
# 모델에게는 구조화출력용(`*.codex`/`*.impl`)을 주고, 산출물 판정은 검증용 스키마가 한다.
EVAL_MODEL_SCHEMA = PROJECT_DIR / "schemas" / "eval_output.impl.schema.json"


def evaluate(
    input_path: Path,
    draft_path: Path,
    rubric: dict,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
    eval_output_schema: Path | None = None,
    prose_slots: list[dict] | None = None,
    decisions: list[dict] | None = None,
    contract_changes: list[dict] | None = None,
    work_dir: Path | None = None,
    code_files: dict | None = None,
    raw_path: Path | None = None,
    on_note=None,
) -> dict | None:
    """초안이 서 있는 계약 판본을 분모로 루브릭을 매긴다. critique와 게이트 결과와 사람 테스트는 보지 않는다."""
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    system, user = build_prompt(
        input_data=input_data,
        draft=draft,
        rubric=rubric,
        prose_slots=prose_slots or [],
        decisions=decisions or [],
        contract_changes=contract_changes or [],
        work_dir=work_dir,
        code_files=code_files,
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=eval_output_schema or EVAL_MODEL_SCHEMA,
        output_path=output_path,
        model=model,
        work_dir=work_dir,
        sandbox=SANDBOX_READ_ONLY,
        raw_path=raw_path,
        on_note=on_note,
    )


def build_prompt(
    input_data: dict,
    draft: dict,
    rubric: dict,
    prose_slots: list[dict],
    decisions: list[dict],
    contract_changes: list[dict],
    work_dir: Path | None = None,
    code_files: dict | None = None,
) -> tuple[str, str]:
    system = read_prompt(EVAL_SYSTEM_PROMPT)
    user = compose(
        block("INPUT_JSON", input_data),
        block("PROSE_SLOTS_JSON", prose_slots),
        block("DRAFT_JSON", draft_without(draft, GATE_ECHO + CRITIQUE_ECHO)),
        block("DRAFT_FILES_JSON", code_files or {"files": [], "omitted": []}),
        block("RUBRIC_JSON", rubric),
        block("STANDING_DECISIONS_JSON", decisions),
        block("CONTRACT_CHANGES_JSON", contract_changes),
        workspace_block(work_dir, False),
    )
    return system, user
