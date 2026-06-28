#!/usr/bin/env python
"""Validate Level 3 eval scores and calculate the gate result.

The eval agent writes axis scores and score reasons. This script validates the
shape, calculates the weighted total from those axis scores, and decides the
final pass/fail gate result.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


VERSION = 1
MIN_SCORE = 3.6
WEIGHTS = {
    "evidence_count": 0.15,
    "evidence_quality": 0.20,
    "claim_coverage": 0.20,
    "uncertainty_boundary": 0.15,
    "consistency": 0.20,
    "alternatives_tradeoff": 0.10,
}


def is_object(value: Any) -> bool:
    return isinstance(value, dict)


def load_json(path: Path) -> tuple[Any | None, list[str]]:
    try:
        return json.loads(path.read_text(encoding="utf-8")), []
    except Exception as exc:  # noqa: BLE001 - CLI should report any parse/read error.
        return None, [f"invalid json: {exc}"]


def validate_shape(data: Any) -> list[str]:
    errors: list[str] = []
    if not is_object(data):
        return ["root must be an object"]

    required = [
        "version",
        "run_id",
        "scores",
        "score_reasons",
    ]
    for key in required:
        if key not in data:
            errors.append(f"{key} is required")

    allowed = set(required)
    for key in data:
        if key not in allowed:
            errors.append(f"{key} is not allowed")

    if data.get("version") != VERSION:
        errors.append("version must be 1")
    if not isinstance(data.get("run_id"), str) or not data.get("run_id"):
        errors.append("run_id must be a non-empty string")

    scores = data.get("scores")
    if not is_object(scores):
        errors.append("scores must be an object")
    else:
        for key in scores:
            if key not in WEIGHTS:
                errors.append(f"scores.{key} is not allowed")
        for key in WEIGHTS:
            if scores.get(key) not in {1, 2, 3, 4, 5}:
                errors.append(f"scores.{key} must be an integer from 1 to 5")

    score_reasons = data.get("score_reasons")
    if not is_object(score_reasons):
        errors.append("score_reasons must be an object")
    else:
        for key in score_reasons:
            if key not in WEIGHTS:
                errors.append(f"score_reasons.{key} is not allowed")
        for key in WEIGHTS:
            if not isinstance(score_reasons.get(key), str) or not score_reasons.get(key):
                errors.append(f"score_reasons.{key} must be a non-empty string")

    return errors


def weighted_score(scores: dict[str, int]) -> float:
    total = sum(scores[key] * WEIGHTS[key] for key in WEIGHTS)
    return round(total, 2)


def build_validation(data: dict[str, Any], eval_path: Path, errors: list[str]) -> dict[str, Any]:
    validation: dict[str, Any] = {
        "version": VERSION,
        "run_id": data.get("run_id") if isinstance(data, dict) else None,
        "source_eval_path": str(eval_path),
        "schema_valid": not errors,
        "schema_errors": errors,
        "min_score": MIN_SCORE,
        "scores": None,
        "score_reasons": data.get("score_reasons") if isinstance(data, dict) else None,
        "weak_axes": [],
        "weighted_score": None,
        "gate_result": "fail",
        "summary": "",
    }

    if errors:
        validation["summary"] = "eval.json failed schema or consistency validation."
        return validation

    scores = data["scores"]
    score = weighted_score(scores)
    gate_result = "fail" if score < MIN_SCORE else "pass"
    weak_axes = [key for key, value in scores.items() if value < 4]

    validation.update(
        {
            "scores": scores,
            "score_reasons": data.get("score_reasons", {}),
            "weak_axes": weak_axes,
            "weighted_score": score,
            "gate_result": gate_result,
            "summary": (
                f"gate_result={gate_result}, weighted_score={score}, "
                f"weak_axes={','.join(weak_axes) or 'none'}"
            ),
        }
    )
    return validation


def write_validation(path: Path, validation: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(validation, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate Level 3 eval.json")
    parser.add_argument("eval_json", type=Path, help="Path to eval.json")
    parser.add_argument(
        "--output",
        type=Path,
        help="Path to write validation.json. Defaults to eval.json sibling.",
    )
    parser.add_argument(
        "--no-exit-on-gate-fail",
        action="store_true",
        help="Return exit code 0 when schema is valid but gate_result is fail.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    eval_path = args.eval_json.resolve()
    output_path = (args.output or eval_path.with_name("validation.json")).resolve()

    data, load_errors = load_json(eval_path)
    if load_errors:
        validation = build_validation({}, eval_path, load_errors)
        write_validation(output_path, validation)
        print(json.dumps(validation, ensure_ascii=False, indent=2))
        return 2

    errors = validate_shape(data)
    validation = build_validation(data, eval_path, errors)
    write_validation(output_path, validation)
    print(json.dumps(validation, ensure_ascii=False, indent=2))

    if errors:
        return 2
    if validation["gate_result"] == "fail" and not args.no_exit_on_gate_fail:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
