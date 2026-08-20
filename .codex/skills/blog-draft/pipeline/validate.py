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

    if artifact in {"gen_output", "refine_output"} and isinstance(data, dict):
        errors += validate_content_contract(data.get("content"), artifact)
    if artifact == "draft" and isinstance(data, dict):
        errors += validate_draft_contract(data, expected_brief_hash, expected_iteration)
    if artifact == "critique" and isinstance(data, dict):
        errors += validate_critique_contract(data, expected_brief_hash, expected_iteration)
    notes: list[str] = []
    if artifact == "eval" and isinstance(data, dict):
        errors += validate_eval_contract(data, expected_brief_hash, expected_iteration, rubric, notes)
    if artifact == "final" and isinstance(data, dict):
        errors += validate_final_contract(data, expected_brief_hash)

    result = {
        "artifact": artifact,
        "checked_file": str(file_path),
        "status": "REJECT" if errors else "PASS",
        "errors": errors,
    }
    if notes:
        result["notes"] = notes
    return result


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


QUOTE_PAIRS = (("“", "”"), ("「", "」"), ('"', '"'))
MIN_QUOTE_LEN = 15
WEIGHTED_TOTAL_TOLERANCE = 0.01
MIN_CHARS_FOR_SECTIONS = 1500


def content_contract_errors(content: Any, brief: Any, banned_terms: Any = ()) -> list[str]:
    """Deterministic checks on draft prose against the brief.

    These are the parts of the request that can be verified by reading, not by
    judging. Everything semantic — the avoid list, the must-include list — stays
    with the rubric, because those values are descriptions rather than literals
    and substring matching would produce nothing but false negatives.
    """
    if not isinstance(content, str) or not isinstance(brief, dict):
        return []
    constraints = brief.get("constraints") or {}
    errors: list[str] = []
    errors += target_length_errors(content, constraints.get("target_length"))
    errors += forbidden_phrase_errors(content, constraints.get("forbidden_phrases"))
    errors += banned_term_errors(content, banned_terms)
    errors += unsourced_quote_errors(content, brief.get("raw_text"))
    errors += section_heading_errors(content)
    return errors


def section_heading_errors(content: str) -> list[str]:
    """Require section headings once a piece is long enough to need them.

    A blog post delivered as one undivided block gives the reader no place to
    pause and no way to see the argument before reading all of it. Only the
    headings are required: forcing a code block or a quote into a piece that
    does not need one would be worse than the wall of text.
    """
    if len(content) < MIN_CHARS_FOR_SECTIONS:
        return []
    if any(line.startswith("## ") for line in content.splitlines()):
        return []
    return [f"markdown_structure: {len(content)} chars with no section heading"]


def target_length_errors(content: str, target_length: Any) -> list[str]:
    """Enforce a stated length range. Free-form targets without a range are skipped."""
    if not isinstance(target_length, str):
        return []
    numbers = [int(value) for value in re.findall("[0-9]+", target_length)]
    if len(numbers) < 2:
        return []
    low, high = min(numbers), max(numbers)
    count = len(content)
    if low <= count <= high:
        return []
    return [f"target_length: {count} chars outside {low}-{high}"]


def forbidden_phrase_errors(content: str, phrases: Any) -> list[str]:
    if not isinstance(phrases, list):
        return []
    return [
        f"forbidden_phrase: {phrase}"
        for phrase in phrases
        if isinstance(phrase, str) and phrase and phrase in content
    ]


def banned_term_errors(content: str, terms: Any) -> list[str]:
    if not terms:
        return []
    return [f"craft_term_leaked: {term}" for term in terms if term and term in content]


ATTRIBUTION_AFTER = ("라고", "라는", "라며", "이라고", "라 했", "라 적", "라 썼", "고 말")
ATTRIBUTION_BEFORE = ("말했", "말한", "썼다", "적었", "적혀", "인용", "따르면", "의 말")


def unsourced_quote_errors(content: str, raw_text: Any) -> list[str]:
    """Flag attributed quotations that do not appear in the source material.

    A prompt already forbids inventing quotations and it has still happened, so
    the check is textual. Only quotes carrying an attribution marker count:
    quotation marks around a coined phrase or a paraphrase are ordinary
    emphasis, and treating those as citations produces nothing but churn.
    """
    if not isinstance(raw_text, str):
        return []
    errors: list[str] = []
    for opening, closing in QUOTE_PAIRS:
        pattern = f"{re.escape(opening)}([^{re.escape(closing)}]+){re.escape(closing)}"
        for match in re.finditer(pattern, content):
            passage = match.group(1).strip()
            if len(passage) < MIN_QUOTE_LEN or passage in raw_text:
                continue
            if not is_attributed(content, match.start(), match.end()):
                continue
            error = f"unsourced_quote: {passage[:40]}"
            if error not in errors:
                errors.append(error)
    return errors


def is_attributed(content: str, start: int, end: int) -> bool:
    """True when the quote is presented as someone's words rather than emphasis."""
    if content[end:end + 12].lstrip().startswith(ATTRIBUTION_AFTER):
        return True
    before = content[max(0, start - 25):start]
    return any(marker in before for marker in ATTRIBUTION_BEFORE)


def validate_content_contract(content: Any, artifact: str) -> list[str]:
    """Reject a content field that carries a JSON envelope instead of prose.

    Models sometimes wrap their answer twice, leaving `content` holding the
    text `{"content": "..."}`. Schema validation passes because it is still a
    string, and an LLM evaluator has scored such a draft as a passing essay,
    so this check has to be deterministic rather than a prompt instruction.
    """
    if not isinstance(content, str):
        return []
    stripped = content.strip()
    if not (stripped.startswith("{") or stripped.startswith("[")):
        return []
    try:
        parsed = json.loads(stripped)
    except json.JSONDecodeError:
        return []
    if isinstance(parsed, (dict, list)):
        return [f"{artifact} content is a JSON envelope, not prose"]
    return []


def validate_draft_contract(data: dict, expected_brief_hash: str | None, expected_iteration: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(DRAFT_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"draft must not include {key}")
    errors += validate_content_contract(data.get("content"), "draft")
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
    notes: list[str] | None = None,
) -> list[str]:
    errors = []
    if notes is None:
        notes = []
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

        # 가중치와 총점은 모델이 보고하지만 판정은 rubric에서 다시 계산한다.
        # 모델이 낸 총점을 그대로 쓰면 합격선이 평가자의 산수에 의존한다.
        axis_weights = {axis: spec.get("weight") for axis, spec in rubric.get("axes", {}).items()}
        for axis in sorted(axis_weights):
            expected = axis_weights[axis]
            actual = weights.get(axis) if isinstance(weights, dict) else None
            if isinstance(expected, (int, float)) and actual != expected:
                errors.append(f"weight mismatch.{axis}: reported {actual}, rubric {expected}")

        computed_total = None
        if axes and isinstance(scores, dict) and set(scores) == axes:
            try:
                computed_total = sum(float(scores[axis]) * float(axis_weights[axis]) for axis in axes)
            except (TypeError, ValueError):
                computed_total = None

        thresholds = rubric.get("thresholds", {})
        min_total = thresholds.get("min_total")
        reported_total = rubric_scores.get("weighted_total") if isinstance(rubric_scores, dict) else None
        if computed_total is not None and isinstance(reported_total, (int, float)):
            if abs(computed_total - reported_total) > WEIGHTED_TOTAL_TOLERANCE:
                # 초안의 결함이 아니라 평가자의 결함이므로 판정을 막지 않고 기록만 한다.
                notes.append(
                    f"weighted_total mismatch: reported {reported_total}, computed {round(computed_total, 4)}"
                )

        effective_total = computed_total if computed_total is not None else reported_total
        if isinstance(min_total, (int, float)) and isinstance(effective_total, (int, float)):
            if effective_total < min_total:
                errors.append(f"min_total: {round(effective_total, 4)} < {min_total}")

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
