from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

from jsonschema import Draft202012Validator, FormatChecker


PROJECT_DIR = Path(__file__).resolve().parent
SCHEMA_DIR = PROJECT_DIR / "schemas"
ARTIFACT_SCHEMAS = {
    "input": SCHEMA_DIR / "input.schema.json",
    "keyword_extraction": SCHEMA_DIR / "keyword_extraction.schema.json",
    "analysis": SCHEMA_DIR / "analysis.schema.json",
    "eval": SCHEMA_DIR / "eval.schema.json",
    "critique": SCHEMA_DIR / "critique.schema.json",
}

KEYWORD_FORBIDDEN_FIELDS = {
    "inferred_expectation",
    "confidence",
    "reasoning",
    "alternative_reading",
    "capability_type",
    "company_size_claim",
    "domain_claim",
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
    expected_batch_no: str | None = None,
) -> dict[str, Any]:
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

    if artifact == "keyword_extraction" and isinstance(data, dict):
        errors += validate_keyword_extraction_contract(data, expected_brief_hash, expected_batch_no)
    elif artifact == "analysis" and isinstance(data, dict):
        errors += validate_analysis_contract(data, expected_brief_hash)
    elif artifact == "eval" and isinstance(data, dict):
        errors += validate_eval_contract(data, expected_brief_hash)
    elif artifact == "critique" and isinstance(data, dict):
        errors += validate_critique_contract(data, expected_brief_hash)
    elif artifact == "input" and isinstance(data, dict):
        errors += validate_input_contract(data, expected_brief_hash)

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


def validate_input_contract(data: dict[str, Any], expected_brief_hash: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")

    posting_ids = []
    for index, posting in enumerate(data.get("postings", [])):
        if not isinstance(posting, dict):
            continue
        posting_id = posting.get("posting_id")
        if isinstance(posting_id, str):
            posting_ids.append(posting_id)
        for key in ("company_size", "domain"):
            if posting.get(key) in (None, ""):
                errors.append(f"postings[{index}].{key} must be provided; use unknown if needed")

    duplicate_ids = sorted({posting_id for posting_id in posting_ids if posting_ids.count(posting_id) > 1})
    for posting_id in duplicate_ids:
        errors.append(f"duplicate posting_id: {posting_id}")
    return errors


def validate_keyword_extraction_contract(
    data: dict[str, Any],
    expected_brief_hash: str | None,
    expected_batch_no: str | None,
) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_batch_no and data.get("batch_no") != expected_batch_no:
        errors.append("batch_no mismatch")
    if data.get("stage") != "keyword_extract":
        errors.append("stage must be keyword_extract")
    batch_no = data.get("batch_no")
    if isinstance(batch_no, str) and not re.fullmatch(r"[0-9]{3}", batch_no):
        errors.append("batch_no must use a 3-digit value such as 001")

    item_ids = []
    for posting_index, posting in enumerate(data.get("postings", [])):
        if not isinstance(posting, dict):
            continue
        for key in ("posting_id", "company_name", "company_size", "domain", "role_title", "reference_link"):
            if not non_empty_string(posting.get(key)):
                errors.append(f"postings[{posting_index}].{key} must be a non-empty string")
        for item_index, item in enumerate(posting.get("items", [])):
            if not isinstance(item, dict):
                continue
            item_id = item.get("item_id")
            if isinstance(item_id, str):
                item_ids.append(item_id)
                if not re.fullmatch(r".+-k[0-9]{3}", item_id):
                    errors.append(f"postings[{posting_index}].items[{item_index}].item_id must look like p001-k001")
            for key in sorted(KEYWORD_FORBIDDEN_FIELDS & item.keys()):
                errors.append(f"postings[{posting_index}].items[{item_index}] must not include {key}")
            source_spans = item.get("source_spans", [])
            if not source_spans:
                errors.append(f"postings[{posting_index}].items[{item_index}].source_spans must not be empty")
            for span_index, span in enumerate(source_spans):
                if isinstance(span, dict) and not non_empty_string(span.get("text")):
                    errors.append(
                        f"postings[{posting_index}].items[{item_index}].source_spans[{span_index}].text "
                        "must be a non-empty string"
                    )
            for term_index, term in enumerate(item.get("terms", [])):
                if not non_empty_string(term):
                    errors.append(f"postings[{posting_index}].items[{item_index}].terms[{term_index}] must be non-empty")

    duplicate_ids = sorted({item_id for item_id in item_ids if item_ids.count(item_id) > 1})
    for item_id in duplicate_ids:
        errors.append(f"duplicate item_id: {item_id}")
    return errors


def validate_analysis_contract(data: dict[str, Any], expected_brief_hash: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if data.get("stage") != "analyze":
        errors.append("stage must be analyze")

    signal_ids = []
    reading_ids = []
    errors += validate_non_negative_counts(data)

    for signal_index, signal in enumerate(data.get("signals", [])):
        if isinstance(signal, dict) and isinstance(signal.get("signal_id"), str):
            signal_ids.append(signal["signal_id"])
            if not re.fullmatch(r"s[0-9]{3}", signal["signal_id"]):
                errors.append(f"signals[{signal_index}].signal_id must look like s001")
        if not isinstance(signal, dict):
            continue
        if not signal.get("source_item_ids"):
            errors.append(f"signals[{signal_index}].source_item_ids must not be empty")
        evidence_distribution = signal.get("evidence_distribution", {})
        if isinstance(evidence_distribution, dict) and not evidence_distribution.get("posting_ids"):
            errors.append(f"signals[{signal_index}].evidence_distribution.posting_ids must not be empty")
        for key in ("surface_pattern", "inferred_expectation", "reasoning", "alternative_reading"):
            if not non_empty_string(signal.get(key)):
                errors.append(f"signals[{signal_index}].{key} must be a non-empty string")

    signal_id_set = set(signal_ids)
    for reading_index, reading in enumerate(data.get("subtext_readings", [])):
        if isinstance(reading, dict) and isinstance(reading.get("reading_id"), str):
            reading_ids.append(reading["reading_id"])
            if not re.fullmatch(r"r[0-9]{3}", reading["reading_id"]):
                errors.append(f"subtext_readings[{reading_index}].reading_id must look like r001")
        if not isinstance(reading, dict):
            continue
        if not reading.get("source_item_ids"):
            errors.append(f"subtext_readings[{reading_index}].source_item_ids must not be empty")
        evidence_distribution = reading.get("evidence_distribution", {})
        if isinstance(evidence_distribution, dict) and not evidence_distribution.get("posting_ids"):
            errors.append(f"subtext_readings[{reading_index}].evidence_distribution.posting_ids must not be empty")
        if not reading.get("representative_surface_phrases"):
            errors.append(f"subtext_readings[{reading_index}].representative_surface_phrases must not be empty")
        for key in (
            "surface_phrase_group",
            "plain_translation",
            "possible_team_context",
            "candidate_opportunity",
            "reasoning",
            "alternative_reading",
        ):
            if not non_empty_string(reading.get(key)):
                errors.append(f"subtext_readings[{reading_index}].{key} must be a non-empty string")
        for signal_id in reading.get("linked_signal_ids", []):
            if signal_id not in signal_id_set:
                errors.append(
                    f"subtext_readings[{reading_index}].linked_signal_ids references unknown signal_id: {signal_id}"
                )

    duplicate_ids = sorted({signal_id for signal_id in signal_ids if signal_ids.count(signal_id) > 1})
    for signal_id in duplicate_ids:
        errors.append(f"duplicate signal_id: {signal_id}")
    duplicate_reading_ids = sorted({reading_id for reading_id in reading_ids if reading_ids.count(reading_id) > 1})
    for reading_id in duplicate_reading_ids:
        errors.append(f"duplicate reading_id: {reading_id}")
    return errors


def validate_eval_contract(data: dict[str, Any], expected_brief_hash: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if data.get("stage") != "evaluate":
        errors.append("stage must be evaluate")

    expected_axes = {
        "evidence_linkage",
        "inference_discipline",
        "subtext_quality",
        "pattern_quality",
        "alternative_reading",
        "distribution_awareness",
    }
    axes = [
        score.get("axis")
        for score in data.get("axis_scores", [])
        if isinstance(score, dict) and isinstance(score.get("axis"), str)
    ]
    if set(axes) != expected_axes:
        errors.append(f"axis_scores axes mismatch: expected {sorted(expected_axes)}, actual {sorted(axes)}")
    duplicate_axes = sorted({axis for axis in axes if axes.count(axis) > 1})
    for axis in duplicate_axes:
        errors.append(f"duplicate axis_score: {axis}")

    for index, axis_score in enumerate(data.get("axis_scores", [])):
        if not isinstance(axis_score, dict):
            continue
        score = axis_score.get("score")
        if not isinstance(score, (int, float)):
            errors.append(f"axis_scores[{index}].score must be a number")
        elif score < 0 or score > 5:
            errors.append(f"axis_scores[{index}].score must be between 0 and 5")
        if not non_empty_string(axis_score.get("rationale")):
            errors.append(f"axis_scores[{index}].rationale must be a non-empty string")
    return errors


def validate_critique_contract(data: dict[str, Any], expected_brief_hash: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if data.get("stage") != "critique":
        errors.append("stage must be critique")

    critic = data.get("critic", {})
    weaknesses = critic.get("weaknesses", []) if isinstance(critic, dict) else []
    revision_instructions = critic.get("revision_instructions", []) if isinstance(critic, dict) else []
    if not weaknesses:
        errors.append("critique must include critic.weaknesses")
    if not revision_instructions:
        errors.append("critique must include critic.revision_instructions")
    weakness_ids = [
        weakness.get("weakness_id")
        for weakness in weaknesses
        if isinstance(weakness, dict) and isinstance(weakness.get("weakness_id"), str)
    ]
    duplicate_weakness_ids = sorted({weakness_id for weakness_id in weakness_ids if weakness_ids.count(weakness_id) > 1})
    for weakness_id in duplicate_weakness_ids:
        errors.append(f"duplicate weakness_id: {weakness_id}")
    return errors


def validate_non_negative_counts(value: Any, path: str = "$") -> list[str]:
    errors = []
    if isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{path}.{key}"
            if key.endswith("_count") or key == "count" or key.endswith("_counts"):
                errors += validate_count_value(child, child_path)
            else:
                errors += validate_non_negative_counts(child, child_path)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            errors += validate_non_negative_counts(child, f"{path}[{index}]")
    return errors


def validate_count_value(value: Any, path: str) -> list[str]:
    if isinstance(value, int):
        return [] if value >= 0 else [f"{path} must be non-negative"]
    if isinstance(value, dict):
        errors = []
        for key, child in value.items():
            errors += validate_count_value(child, f"{path}.{key}")
        return errors
    return []


def non_empty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a recruiting harness JSON artifact.")
    parser.add_argument("file", type=Path)
    parser.add_argument("--artifact", required=True, choices=sorted(ARTIFACT_SCHEMAS))
    parser.add_argument("--brief-hash")
    parser.add_argument("--batch-no")
    parser.add_argument("--write-result", type=Path)
    args = parser.parse_args()

    result = validate_file(
        file_path=args.file,
        artifact=args.artifact,
        expected_brief_hash=args.brief_hash,
        expected_batch_no=args.batch_no,
    )

    if args.write_result:
        write_result(result, args.write_result)

    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
