from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE, LLMClient
from stages.scripts.payload import block, compose, read_prompt, workspace_block


PROJECT_DIR = Path(__file__).resolve().parent.parent
CRITIQUE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "critique_impl.md"
# 모델에게는 구조화출력용(`*.codex`/`*.impl`)을 주고, 산출물 판정은 검증용 스키마가 한다.
CRITIQUE_MODEL_SCHEMA = PROJECT_DIR / "schemas" / "critique_output.codex.schema.json"


def critique(
    input_path: Path,
    draft_path: Path,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
    prose_slots: list[dict] | None = None,
    decisions: list[dict] | None = None,
    contract_changes: list[dict] | None = None,
    observations: list[dict] | None = None,
    axes: dict[str, str] | None = None,
    work_dir: Path | None = None,
    code_files: dict | None = None,
    raw_path: Path | None = None,
    on_note=None,
) -> dict | None:
    """다섯 축에 고칠 방향을 붙이고 계약 변경을 발의한다.

    받는 것은 input, prose_slots, 현재 draft, 서 있는 결정, 계약 변경 기록, 게이트가 남긴 기록,
    축의 이름과 뜻이다. 루브릭의 스케일·가중·하한과 게이트의 위반 목록과 eval은 받지 않는다.
    스케일을 주면 지적이 "3점을 4점으로 올리는 방법"이 되고, 위반 목록을 주면 이미 잡힌 것을
    다시 말하는 데 지면을 쓴다.
    """
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    system, user = build_prompt(
        input_data=input_data,
        draft=draft,
        prose_slots=prose_slots or [],
        decisions=decisions or [],
        contract_changes=contract_changes or [],
        observations=observations or [],
        axes=axes or {},
        work_dir=work_dir,
        code_files=code_files,
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=CRITIQUE_MODEL_SCHEMA,
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
    prose_slots: list[dict],
    decisions: list[dict],
    contract_changes: list[dict],
    observations: list[dict],
    axes: dict[str, str],
    work_dir: Path | None = None,
    code_files: dict | None = None,
) -> tuple[str, str]:
    system = read_prompt(CRITIQUE_SYSTEM_PROMPT)
    user = compose(
        block("INPUT_JSON", input_data),
        block("PROSE_SLOTS_JSON", prose_slots),
        block("DRAFT_JSON", draft),
        block("DRAFT_FILES_JSON", code_files or {"files": [], "omitted": []}),
        block("STANDING_DECISIONS_JSON", decisions),
        block("CONTRACT_CHANGES_JSON", contract_changes),
        block("GATE_OBSERVATIONS_JSON", observations),
        block("AXES_JSON", axes),
        workspace_block(work_dir, False),
    )
    return system, user
