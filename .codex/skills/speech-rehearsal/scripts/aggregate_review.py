from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import sys

from jsonschema import Draft202012Validator


SKILL_DIR = Path(__file__).resolve().parent.parent
CONTEXT_SCHEMA_PATH = SKILL_DIR / "schemas" / "review-context.schema.json"
DELIVERY_SCHEMA_PATH = SKILL_DIR / "schemas" / "delivery-output.schema.json"
LOGIC_SCHEMA_PATH = SKILL_DIR / "schemas" / "logic-output.schema.json"
CRITERION_PATTERN = re.compile(r"^  ([a-z][a-z0-9_]*):\s*$")


def load_json(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"expected JSON object: {path}")
    return value


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_schema(value: dict, schema_path: Path, artifact_name: str) -> None:
    schema = load_json(schema_path)
    errors = sorted(Draft202012Validator(schema).iter_errors(value), key=lambda error: list(error.path))
    if errors:
        details = "; ".join(error.message for error in errors[:5])
        raise ValueError(f"{artifact_name} schema validation failed: {details}")


def resolve_resource(resource: dict) -> Path:
    path = (SKILL_DIR / resource["path"]).resolve()
    if not path.is_relative_to(SKILL_DIR):
        raise ValueError(f"resource escapes skill directory: {path}")
    if not path.is_file():
        raise FileNotFoundError(f"resource not found: {path}")
    if sha256_file(path) != resource["sha256"]:
        raise ValueError(f"resource changed after context preparation: {path}")
    return path


def verify_resources(context: dict) -> dict[str, dict[str, Path]]:
    resolved: dict[str, dict[str, Path]] = {}
    for reviewer, resources in context["resources"].items():
        resolved[reviewer] = {kind: resolve_resource(resource) for kind, resource in resources.items()}
    return resolved


def rubric_criteria(path: Path) -> set[str]:
    criteria: set[str] = set()
    inside = False
    for line in path.read_text(encoding="utf-8").splitlines():
        if line == "criteria:":
            inside = True
            continue
        if inside and line and not line.startswith(" "):
            break
        if inside:
            match = CRITERION_PATTERN.match(line)
            if match:
                criteria.add(match.group(1))
    if not criteria:
        raise ValueError(f"rubric contains no criteria: {path}")
    return criteria


def verify_criterion_coverage(review: dict, expected: set[str], reviewer: str) -> None:
    actual = [finding["criterion"] for finding in review["findings"]]
    duplicates = sorted(name for name, count in Counter(actual).items() if count > 1)
    missing = sorted(expected - set(actual))
    unknown = sorted(set(actual) - expected)
    if duplicates or missing or unknown:
        raise ValueError(
            f"{reviewer} rubric coverage mismatch: duplicates={duplicates}, missing={missing}, unknown={unknown}"
        )


def verify_token_evidence(context: dict, delivery: dict, logic: dict) -> None:
    transcript = context["transcript"]["text"]
    known_tokens = {token["id"] for token in context["transcript"]["tokens"]}

    def check(token_ids: list[str], quote: str, location: str) -> None:
        unknown = sorted(set(token_ids) - known_tokens)
        if unknown:
            raise ValueError(f"{location} references unknown token IDs: {unknown}")
        if quote and quote not in transcript:
            raise ValueError(f"{location} quote is not an exact transcript substring: {quote!r}")

    used_filler_tokens: set[str] = set()
    annotation_ids: set[str] = set()
    for annotation in delivery["filler_annotations"]:
        annotation_id = annotation["annotation_id"]
        if annotation_id in annotation_ids:
            raise ValueError(f"duplicate filler annotation ID: {annotation_id}")
        annotation_ids.add(annotation_id)
        overlap = used_filler_tokens.intersection(annotation["token_ids"])
        if overlap:
            raise ValueError(f"filler annotations overlap token IDs: {sorted(overlap)}")
        used_filler_tokens.update(annotation["token_ids"])
        check(annotation["token_ids"], annotation["evidence_quote"], f"filler annotation {annotation_id}")

    for reviewer_name, review in (("delivery", delivery), ("logic", logic)):
        for finding in review["findings"]:
            for index, evidence in enumerate(finding["evidence"], start=1):
                check(evidence["token_ids"], evidence["quote"], f"{reviewer_name} {finding['criterion']} evidence {index}")

    for index, question in enumerate(logic["senior_questions"], start=1):
        evidence = question["evidence"]
        check(evidence["token_ids"], evidence["quote"], f"senior question {index}")


def filler_summary(delivery: dict, duration_seconds: float | None) -> dict:
    labels = Counter(annotation["label"] for annotation in delivery["filler_annotations"])
    filler_surfaces = Counter(
        annotation["surface"] for annotation in delivery["filler_annotations"] if annotation["label"] == "filler"
    )
    filler_count = labels.get("filler", 0)
    return {
        "confirmed_filler_count": filler_count,
        "lexical_count": labels.get("lexical", 0),
        "uncertain_count": labels.get("uncertain", 0),
        "confirmed_fillers_per_minute": round(filler_count * 60 / duration_seconds, 2) if duration_seconds else None,
        "confirmed_filler_surfaces": dict(sorted(filler_surfaces.items())),
    }


def build_feedback(context: dict, delivery: dict, logic: dict) -> dict:
    return {
        "schema_version": 1,
        "context_id": context["context_id"],
        "metrics": context["metrics"],
        "filler_summary": filler_summary(delivery, context["metrics"]["duration_seconds"]),
        "delivery_review": delivery,
        "logic_review": logic,
    }


def finding_lines(findings: list[dict]) -> list[str]:
    lines = []
    for finding in findings:
        lines.extend(
            [
                f"### {finding['criterion']} · {finding['status']}",
                "",
                finding["diagnosis"],
                "",
                f"- 개선 행동: {finding['action']}",
            ]
        )
        evidence = finding.get("evidence", [])
        if evidence:
            lines.append(f"- 근거: {evidence[0]['quote']}")
        lines.append("")
    return lines


def render_markdown(feedback: dict) -> str:
    metrics = feedback["metrics"]
    fillers = feedback["filler_summary"]
    delivery = feedback["delivery_review"]
    logic = feedback["logic_review"]
    lines = [
        "# Speech Rehearsal Feedback",
        "",
        f"- context: `{feedback['context_id']}`",
        f"- 공백 기준 발화 단위: {metrics['whitespace_token_count']}",
        f"- 녹화 길이: {metrics['duration_seconds'] if metrics['duration_seconds'] is not None else '측정 불가'}초",
        f"- 분당 발화 단위: {metrics['tokens_per_minute'] if metrics['tokens_per_minute'] is not None else '측정 불가'}",
        "",
        "## 필러 진단",
        "",
        f"- 확정 필러: {fillers['confirmed_filler_count']}회",
        f"- 의미 표현: {fillers['lexical_count']}회",
        f"- 판단 보류: {fillers['uncertain_count']}회",
        f"- 분당 확정 필러: {fillers['confirmed_fillers_per_minute'] if fillers['confirmed_fillers_per_minute'] is not None else '측정 불가'}",
        f"- 확정 필러별 빈도: {json.dumps(fillers['confirmed_filler_surfaces'], ensure_ascii=False)}",
        "",
        "## 전달 진단",
        "",
        delivery["overall_summary"],
        "",
    ]
    lines.extend(finding_lines(delivery["findings"]))
    lines.extend(["## 논리 진단", "", logic["overall_summary"], ""])
    lines.extend(finding_lines(logic["findings"]))
    lines.extend(["## 시니어 후속 질문", ""])
    for question in logic["senior_questions"]:
        lines.extend([f"- {question['question']}", f"  - 이유: {question['why']}"])
    lines.extend(["", "## 다음 리허설 행동", ""])
    for action in delivery["top_actions"] + logic["top_actions"]:
        lines.append(f"- {action}")
    limitations = delivery["limitations"] + logic["limitations"]
    if limitations:
        lines.extend(["", "## 한계", ""])
        for limitation in dict.fromkeys(limitations):
            lines.append(f"- {limitation}")
    return "\n".join(lines).rstrip() + "\n"


def write_json(path: Path, value: dict) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate and aggregate two independent speech review artifacts.")
    parser.add_argument("--context", type=Path, required=True)
    parser.add_argument("--delivery", type=Path, required=True)
    parser.add_argument("--logic", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    context = load_json(args.context.resolve())
    delivery = load_json(args.delivery.resolve())
    logic = load_json(args.logic.resolve())
    validate_schema(context, CONTEXT_SCHEMA_PATH, "review context")
    validate_schema(delivery, DELIVERY_SCHEMA_PATH, "delivery review")
    validate_schema(logic, LOGIC_SCHEMA_PATH, "logic review")

    resources = verify_resources(context)
    verify_criterion_coverage(delivery, rubric_criteria(resources["delivery"]["rubric"]), "delivery")
    verify_criterion_coverage(logic, rubric_criteria(resources["logic"]["rubric"]), "logic")
    verify_token_evidence(context, delivery, logic)

    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / "feedback.json"
    markdown_path = output_dir / "feedback.md"
    if json_path.exists() or markdown_path.exists():
        raise FileExistsError(f"refusing to overwrite existing feedback in {output_dir}")

    feedback = build_feedback(context, delivery, logic)
    write_json(json_path, feedback)
    markdown_path.write_text(render_markdown(feedback), encoding="utf-8")
    print(markdown_path)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
