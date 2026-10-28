"""산출 스키마 검증.

금지 산출물은 프롬프트에만 적지 않고 스키마에 박는다. 역할 침범이 자연어 판단이 아니라 구조 위반으로
잡히게 하려는 것이고, 한 필드로 슬쩍 들어온 자기 평가는 읽는 쪽이 지적하지 않으면 그대로 다음 단계로
넘어간다.

역할 침범을 다른 실패와 섞어 적지 않는다. 섞이면 집계되지 않는다.
"""

from __future__ import annotations

import json
from pathlib import Path

from jsonschema import Draft202012Validator
from jsonschema.validators import RefResolver

SCHEMA_DIR = Path(__file__).resolve().parent / "schemas"

# 선언하지 않은 필드가 들어온 것은 역할 침범 후보다. 다른 구조 오류와 분류를 가른다.
ROLE_BREACH_VALIDATORS = {"additionalProperties", "unevaluatedProperties"}


def load_schema(name: str) -> dict:
    return json.loads((SCHEMA_DIR / name).read_text(encoding="utf-8"))


def _validator(name: str) -> Draft202012Validator:
    schema = load_schema(name)
    store = {}
    for path in SCHEMA_DIR.glob("*.schema.json"):
        store[path.name] = json.loads(path.read_text(encoding="utf-8"))
    resolver = RefResolver(base_uri="", referrer=schema, store=store)
    return Draft202012Validator(schema, resolver=resolver)


def validate_output(instance: dict, schema_name: str) -> dict:
    """산출물 하나를 검증한다.

    반환은 게이트와 같은 모양이다. `verdict` 가 셋이라 미판정을 위반과 다른 상태로 적을 수 있다.
    """
    validator = _validator(schema_name)
    violations = []
    for error in sorted(validator.iter_errors(instance), key=lambda e: list(e.absolute_path)):
        where = "/".join(str(part) for part in error.absolute_path) or "(root)"
        rule = "role_breach" if error.validator in ROLE_BREACH_VALIDATORS else "schema_violation"
        violations.append({
            "rule": rule,
            "point": schema_name,
            "where": where,
            "detail": error.message,
            "verdict": "violation",
        })
    return {"violations": violations}


def check_kind_consistency(claims: list[dict]) -> dict:
    """종류와 트레이드오프 칸의 짝을 본다.

    스키마는 `tradeoffs` 를 선택 필드로 두므로, fact 주장에 교환이 붙거나 choice 주장에 교환이 없는
    것은 스키마가 잡지 못한다. 둘 다 구조 문제이므로 채점으로 보내지 않는다.
    """
    violations = []
    for claim in claims:
        cid = claim.get("id", "?")
        kind = claim.get("kind")
        has = bool(claim.get("tradeoffs"))
        if kind == "fact" and has:
            violations.append({
                "rule": "role_breach", "point": cid, "where": "tradeoffs",
                "detail": "fact 주장에 트레이드오프가 붙었다", "verdict": "violation",
            })
        if kind == "choice" and not has:
            violations.append({
                "rule": "schema_violation", "point": cid, "where": "tradeoffs",
                "detail": "choice 주장에 트레이드오프가 없다", "verdict": "violation",
            })
    return {"violations": violations}


def check_dimension_sync(rubric: dict, schema_name: str = "gen_output.schema.json") -> dict:
    """루브릭의 차원 목록과 스키마의 enum 이 같은지 본다.

    목록이 두 자리에 있으면 갈라진다. 갈라지면 루브릭이 세는 분모와 산출이 쓸 수 있는 값이 달라져서,
    차원 하나가 영원히 명시되지 않는다.
    """
    schema = load_schema(schema_name)
    enum = (schema["properties"]["claims"]["items"]["properties"]["tradeoffs"]["items"]
            ["properties"]["dimension"]["enum"])
    declared = [d["id"] for d in rubric.get("dimensions", [])]
    if sorted(enum) == sorted(declared):
        return {"violations": []}
    return {"violations": [{
        "rule": "judge_asset_drift", "point": schema_name, "where": "dimension",
        "detail": f"루브릭은 {declared}, 스키마는 {enum}", "verdict": "violation",
    }]}
