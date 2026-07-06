from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from jsonschema import Draft202012Validator, FormatChecker

PROJECT_DIR = Path(__file__).resolve().parent
SCHEMA_DIR = PROJECT_DIR / "schemas"
ARTIFACT_SCHEMAS = {
    "input": SCHEMA_DIR / "input.schema.json",
    "gen_output": SCHEMA_DIR / "gen_output.schema.json",
    "refine_output": SCHEMA_DIR / "gen_output.schema.json",
    "critique_output": SCHEMA_DIR / "critique_output.schema.json",
    "eval_output": SCHEMA_DIR / "eval_output.schema.json",
    "draft": SCHEMA_DIR / "draft.schema.json",
    "critique": SCHEMA_DIR / "critique.schema.json",
    "eval": SCHEMA_DIR / "eval.schema.json",
    "final": SCHEMA_DIR / "final.schema.json",
}

DRAFT_FORBIDDEN_FIELDS = {
    "self_score",
    "self_critique",
    "verdict",
    "rubric_scores",
    "contract_errors",
}

CRITIQUE_FORBIDDEN_FIELDS = {
    "score",
    "rubric_scores",
    "weighted_total",
    "verdict",
    "rewritten_content",
}

EVAL_FORBIDDEN_FIELDS = {
    "verdict",
    "contract_errors",
    "revision_instructions",
    "rewritten_content",
}

# 검증 불가능한(모호한) 단언 금지패턴. 구체 기대값 없이 "잘 동작한다" 류로 때우는 단언을
# 결정적으로 걸러낸다. 부사+동작 조합만 골라 "정상 환불" 같은 정당한 도메인어 오탐을 피한다.
# 위반은 하드 실패가 아니라 contract_error로 refine 루프에 전달된다(runner).
FORBIDDEN_ASSERTION_PATTERNS = [
    "정상적으로 동작",
    "올바르게 동작",
    "제대로 동작",
    "잘 동작",
    "적절히 처리",
    "알맞게",
    "적당히",
    "문제없이",
]


def check_forbidden_assertions(content: str) -> list[str]:
    """생성된 테스트 본문에서 모호한 단언 금지패턴을 찾아 '줄번호: 문구' 목록으로 반환한다."""
    if not content:
        return []
    violations: list[str] = []
    for line_no, line in enumerate(content.splitlines(), start=1):
        for phrase in FORBIDDEN_ASSERTION_PATTERNS:
            if phrase in line:
                violations.append(f"line {line_no}: '{phrase}'")
    return violations


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"invalid JSON: {exc}") from exc


def validate_file(
    file_path: Path,
    artifact: str,
    expected_brief_hash: str | None = None,
    expected_iteration: str | None = None,
    rubric: dict[str, Any] | None = None,
) -> dict:
    if artifact not in ARTIFACT_SCHEMAS:
        raise ValueError(f"unknown artifact: {artifact}")

    try:
        data = load_json(file_path)
    except ValueError as exc:
        return {
            "artifact": artifact,
            "checked_file": str(file_path),
            "status": "ERROR",
            "errors": [str(exc)],
        }

    schema = load_json(ARTIFACT_SCHEMAS[artifact])
    errors = validate_schema(data, schema)

    if artifact == "draft" and isinstance(data, dict):
        errors += validate_draft_contract(data, expected_brief_hash, expected_iteration)
    if artifact == "critique" and isinstance(data, dict):
        errors += validate_critique_contract(data, expected_brief_hash, expected_iteration)
    if artifact == "eval" and isinstance(data, dict):
        errors += validate_eval_contract(data, expected_brief_hash, expected_iteration, rubric)
    if artifact == "final" and isinstance(data, dict):
        errors += validate_final_contract(data, expected_brief_hash)

    return {
        "artifact": artifact,
        "checked_file": str(file_path),
        "status": "REJECT" if errors else "PASS",
        "errors": errors,
    }


def write_result(result: dict[str, Any], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def validate_schema(data: Any, schema: dict[str, Any]) -> list[str]:
    validator = Draft202012Validator(schema, format_checker=FormatChecker())
    return [
        format_schema_error(error)
        for error in sorted(validator.iter_errors(data), key=lambda item: list(item.path))
    ]


def format_schema_error(error: Any) -> str:
    path = "$"
    if error.path:
        path += "".join(f"[{part}]" if isinstance(part, int) else f".{part}" for part in error.path)
    return f"schema {path}: {error.message}"


def validate_draft_contract(data: dict, expected_brief_hash: str | None, expected_iteration: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(DRAFT_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"draft must not include {key}")
    return errors


def validate_critique_contract(data: dict, expected_brief_hash: str | None, expected_iteration: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(CRITIQUE_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"critique must not include {key}")
    return errors


def validate_eval_contract(
    data: dict,
    expected_brief_hash: str | None,
    expected_iteration: str | None,
    rubric: dict[str, Any] | None,
) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(EVAL_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"eval must not include {key}")

    rubric_scores = data.get("rubric_scores", {})
    scores = rubric_scores.get("scores", {}) if isinstance(rubric_scores, dict) else {}
    weights = rubric_scores.get("weights", {}) if isinstance(rubric_scores, dict) else {}
    axis_rationales = data.get("axis_rationales", {})
    if rubric:
        axes = set(rubric.get("axes", {}).keys())
        if axes:
            if set(scores.keys()) != axes:
                errors.append(f"rubric score axes mismatch: expected {sorted(axes)}, actual {sorted(scores.keys())}")
            if set(weights.keys()) != axes:
                errors.append(f"rubric weight axes mismatch: expected {sorted(axes)}, actual {sorted(weights.keys())}")
            if set(axis_rationales.keys()) != axes:
                errors.append(
                    f"rubric rationale axes mismatch: expected {sorted(axes)}, actual {sorted(axis_rationales.keys())}"
                )

        thresholds = rubric.get("thresholds", {})
        min_total = thresholds.get("min_total")
        weighted_total = rubric_scores.get("weighted_total") if isinstance(rubric_scores, dict) else None
        if isinstance(min_total, (int, float)) and isinstance(weighted_total, (int, float)):
            if weighted_total < min_total:
                errors.append(f"min_total: {weighted_total} < {min_total}")

        min_axis = thresholds.get("min_axis", {})
        if isinstance(min_axis, dict) and isinstance(scores, dict):
            for axis, minimum in min_axis.items():
                score = scores.get(axis)
                if isinstance(minimum, (int, float)) and isinstance(score, (int, float)) and score < minimum:
                    errors.append(f"min_axis.{axis}: {score} < {minimum}")
    return errors


def validate_final_contract(data: dict, expected_brief_hash: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    contract_result = data.get("contract_result", {})
    if isinstance(contract_result, dict) and contract_result.get("verdict") != "PASS":
        errors.append("final contract_result.verdict must be PASS")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a pipeline JSON artifact.")
    parser.add_argument("file", type=Path)
    parser.add_argument(
        "--artifact",
        required=True,
        choices=["input", "gen_output", "refine_output", "critique_output", "eval_output", "draft", "critique", "eval", "final"],
    )
    parser.add_argument("--brief-hash")
    parser.add_argument("--iteration")
    parser.add_argument("--write-result", type=Path)
    args = parser.parse_args()

    result = validate_file(
        file_path=args.file,
        artifact=args.artifact,
        expected_brief_hash=args.brief_hash,
        expected_iteration=args.iteration,
    )

    if args.write_result:
        write_result(result, args.write_result)

    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
