from __future__ import annotations

import json
from pathlib import Path

from stages.scripts.llm_client import SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE, LLMClient
from stages.scripts.payload import block, compose, read_prompt, workspace_block


PROJECT_DIR = Path(__file__).resolve().parent.parent
REFINE_SYSTEM_PROMPT = PROJECT_DIR / "prompts" / "refine_impl.md"
# 모델에게는 구조화출력용(`*.codex`/`*.impl`)을 주고, 산출물 판정은 검증용 스키마가 한다.
REFINE_MODEL_SCHEMA = PROJECT_DIR / "schemas" / "gen_output.codex.schema.json"


def refine(
    input_path: Path,
    draft_path: Path,
    critique_path: Path | None,
    refine_request: dict,
    output_path: Path,
    client: LLMClient,
    model: str | None = None,
    work_dir: Path | None = None,
    raw_path: Path | None = None,
    on_note=None,
) -> dict | None:
    """위반과 지적을 코드와 계약 판본에 반영한다.

    받는 것은 input, 이전 draft, critique, refine_request다. eval 총점 원문과 사람 테스트는 받지 않는다.
    열린 위반·규칙 카드·서 있는 결정·약한 축은 runner가 refine_request로 계산해 넘긴다.
    G0가 깨져 비평을 건너뛴 iteration에서는 critique_path가 없고, 그때는 게이트 위반만 반영한다.
    """
    input_data = json.loads(input_path.read_text(encoding="utf-8"))
    draft = json.loads(draft_path.read_text(encoding="utf-8"))
    critique = json.loads(critique_path.read_text(encoding="utf-8")) if critique_path else {}
    system, user = build_prompt(
        input_data=input_data,
        draft=draft,
        critique=critique,
        refine_request=refine_request,
        work_dir=work_dir,
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=REFINE_MODEL_SCHEMA,
        output_path=output_path,
        model=model,
        work_dir=work_dir,
        sandbox=SANDBOX_WORKSPACE_WRITE,
        raw_path=raw_path,
        on_note=on_note,
    )


def build_prompt(
    input_data: dict,
    draft: dict,
    critique: dict,
    refine_request: dict,
    work_dir: Path | None = None,
) -> tuple[str, str]:
    system = read_prompt(REFINE_SYSTEM_PROMPT)
    user = compose(
        block("INPUT_JSON", input_data),
        block("PREVIOUS_DRAFT_JSON", draft),
        block("CRITIQUE_JSON", critique),
        block("REFINE_REQUEST_JSON", refine_request),
        workspace_block(work_dir, True),
    )
    return system, user
