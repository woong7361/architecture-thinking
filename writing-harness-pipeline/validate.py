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
    "critique_output": SCHEMA_DIR / "critique_output.schema.json",
    "draft": SCHEMA_DIR / "draft.schema.json",
    "critique": SCHEMA_DIR / "critique.schema.json",
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


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a pipeline JSON artifact.")
    parser.add_argument("file", type=Path)
    parser.add_argument("--artifact", required=True, choices=["input", "gen_output", "critique_output", "draft", "critique"])
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
