"""단계별 payload 구성.

차단은 runner 가 payload 를 구성해 물리적으로 막고, 프롬프트가 "받은 것만 보고 판단하라"를 함께 적는다.
프롬프트만으로 막지 않는다. 입력에 있는 정보를 무시하라는 지시는 그 정보를 한 번 더 읽게 만든다.

`contract-impl-agent/pipeline/stages/scripts/payload.py` 의 `block`·`compose`·`read_prompt` 모양을
가져왔다. 계약 판본 전용 함수는 이 하네스에 대상이 없어 옮기지 않았다.
"""

from __future__ import annotations

import json
from pathlib import Path


def read_prompt(path: Path) -> str:
    """단계 프롬프트를 읽는다. 프롬프트는 조각 D가 소유하므로 없을 때 그 사실을 말한다."""
    if not path.exists():
        raise FileNotFoundError(
            f"stage prompt not found: {path}\n"
            "프롬프트는 pipeline/prompts/ 에 있다. 붙기 전에는 이 단계를 돌릴 수 없다."
        )
    return path.read_text(encoding="utf-8")


def block(label: str, value: object) -> str:
    """payload 한 덩이를 라벨 붙은 JSON 블록으로 만든다."""
    return f"{label}:\n{json.dumps(value, ensure_ascii=False, indent=2)}\n"


def compose(*blocks: str) -> str:
    return "\n".join(part for part in blocks if part)


# 단계마다 봐도 되는 입력. 설계 §7의 표를 그대로 옮긴 것이고, runner 가 이 표로 payload 를 만든다.
# 표에 없는 것은 전달하지 않는다. 기술적으로 넘길 수 있다는 것이 넘겨야 할 이유가 되지 않는다.
ALLOWED = {
    "gen": ("SOURCES_JSON", "CODE_JSON", "ANSWERS_JSON", "STEP1_JSON", "SITES_JSON",
            "DIMENSIONS_JSON"),
    "critique": ("SOURCES_JSON", "CODE_JSON", "CLAIMS_JSON", "SITES_JSON"),
    "eval": ("SOURCES_JSON", "CODE_JSON", "GEN_JSON", "RUBRIC_JSON"),
    "refine": ("SOURCES_JSON", "CODE_JSON", "CLAIMS_JSON", "VIOLATIONS_JSON",
               "COUNTER_JSON", "WEAK_AXES_JSON"),
}


def claims_for_critique(gen_output: dict) -> dict:
    """비평에 넘기는 주장 묶음.

    Gen 의 `claim_delta` 를 뺀다. 두 독립 판독이 생기고, 두 서술이 어긋나는 자리가 사람이 먼저 열어
    볼 자리가 된다. `source_says` 도 뺀다. 남기면 비평이 그것을 전제로 받아 확인만 하게 된다.
    """
    hidden = {"claim_delta", "source_says"}
    return {
        "claims": [
            {key: value for key, value in claim.items() if key not in hidden}
            for claim in gen_output.get("claims", [])
        ]
    }


def counter_for_refine(critique_output: dict) -> dict:
    """재수집에 넘기는 반증. 점수는 넘기지 않는다.

    점수는 방향 없는 목표라 측정값을 움직이는 가장 싼 경로를 고르게 한다. 약점 서술은 무엇을 고칠지
    지시하지만 점수는 목표만 준다.
    """
    keep = {"id", "limiting_clauses", "deeper_anchor", "amendment_proposal"}
    return {
        "claims": [
            {key: value for key, value in claim.items() if key in keep}
            for claim in critique_output.get("claims", [])
        ],
        "missing_sites": critique_output.get("missing_sites", []),
    }


def weak_axes(eval_output: dict, rubric: dict) -> dict:
    """하한에 못 미친 축의 이름만. 칸 값은 넘기지 않는다."""
    floors = {axis["id"]: axis["floor"] for axis in rubric["axes"]}
    weak: dict[str, list[str]] = {}
    for claim in eval_output.get("claims", []):
        names = [
            axis for axis, box in (claim.get("axes") or {}).items()
            if isinstance(box, dict) and box.get("box", 0) < floors.get(axis, 0)
        ]
        if names:
            weak[claim.get("id", "?")] = sorted(names)
    return {"weak_axes": weak}


def check_blocking(stage: str, keys: list[str]) -> list[str]:
    """payload 의 키 집합이 단계별 허용 표와 일치하는지 본다. 설계 §7의 검증 항목이다."""
    allowed = set(ALLOWED[stage])
    return sorted(set(keys) - allowed)
