from __future__ import annotations

import argparse
from copy import deepcopy
import hashlib
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path


PIPELINE_DIR = Path(__file__).resolve().parent
KST = timezone(timedelta(hours=9))

# Structured brief values arrive as a file instead of CLI flags. The author-only
# blocks contain multi-sentence prose, and section_plan contains nested objects;
# shell quoting makes both forms unnecessarily fragile.
BRIEF_CONTEXT_KEYS = ("reader", "guide", "judgment", "spine", "section_plan")


def read_base_brief(path: str | None) -> dict:
    """Load a prior validated brief when intake is refining an existing run."""
    if not path:
        return {}
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    brief = data.get("brief") if isinstance(data, dict) else None
    if not isinstance(brief, dict):
        raise ValueError("base input must contain a brief object")
    return brief


def read_raw_text(args: argparse.Namespace, base_brief: dict) -> str:
    chunks: list[str] = []
    for path in args.raw_text_file or []:
        chunks.append(Path(path).read_text(encoding="utf-8"))
    if args.raw_text:
        chunks.append(args.raw_text)
    if chunks:
        raw_text = "\n\n".join(chunk.strip() for chunk in chunks if chunk.strip()).strip()
    else:
        raw_text = base_brief.get("raw_text", "")
    if not raw_text:
        raise ValueError("raw text is required via --raw-text, --raw-text-file, or --base-input")
    return raw_text


def read_context(path: str | None) -> dict:
    """Load structured brief values supplied or approved during intake.

    Shape is enforced by input.schema.json, not here. This only rejects
    unknown top-level keys so a typo fails with a readable message instead of
    silently dropping material.
    """
    if not path:
        return {}
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("context file must contain a JSON object")
    unknown = sorted(set(data) - set(BRIEF_CONTEXT_KEYS))
    if unknown:
        allowed = ", ".join(BRIEF_CONTEXT_KEYS)
        raise ValueError(f"unknown context blocks: {', '.join(unknown)} (allowed: {allowed})")
    return data


def split_values(values: list[str] | None) -> list[str]:
    if not values:
        return []
    items: list[str] = []
    for value in values:
        for part in value.split(";"):
            stripped = part.strip()
            if stripped:
                items.append(stripped)
    return items


def build_brief(args: argparse.Namespace, raw_text: str, context: dict, base_brief: dict) -> dict:
    brief = deepcopy(base_brief)
    constraints: dict[str, object] = deepcopy(brief.get("constraints", {}))
    if args.target_length:
        constraints["target_length"] = args.target_length
    if args.tone:
        constraints["tone"] = args.tone

    for key, values in [
        ("emphasis", split_values(args.emphasis)),
        ("must_include", split_values(args.must_include)),
        ("avoid", split_values(args.avoid)),
    ]:
        if values:
            constraints[key] = values

    brief["raw_text"] = raw_text
    for key in ("topic", "piece_type", "intent", "audience"):
        value = getattr(args, key)
        if value:
            brief[key] = value
    brief.setdefault("piece_type", "retrospective")

    for block, value in context.items():
        brief[block] = value
    if "section_plan" in context:
        brief.pop("spine", None)
    if "spine" in context:
        brief.pop("section_plan", None)
    if constraints:
        brief["constraints"] = constraints
    missing = [key for key in ("topic", "intent", "audience") if not brief.get(key)]
    if missing:
        raise ValueError(f"missing required intake values: {', '.join(missing)}")
    return brief


def brief_hash(brief: dict) -> str:
    canonical = json.dumps(brief, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:8]


def write_json(path: Path, data: dict, overwrite: bool) -> None:
    if path.exists() and not overwrite:
        existing = path.read_text(encoding="utf-8")
        incoming = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if existing == incoming:
            return
        raise FileExistsError(f"refusing to overwrite existing file: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_input(path: Path) -> dict:
    sys.path.insert(0, str(PIPELINE_DIR))
    from validate import validate_file  # type: ignore

    return validate_file(path, artifact="input")


def main() -> int:
    parser = argparse.ArgumentParser(description="Build writing-harness input JSON from blog-draft intake values.")
    parser.add_argument(
        "--base-input",
        help="Existing input JSON whose brief is preserved unless an intake value overrides it.",
    )
    parser.add_argument("--raw-text", help="Raw source material to preserve in brief.raw_text.")
    parser.add_argument("--raw-text-file", action="append", help="UTF-8 file containing raw source material.")
    parser.add_argument("--topic")
    parser.add_argument("--piece-type")
    parser.add_argument("--intent")
    parser.add_argument("--audience")
    parser.add_argument(
        "--context-file",
        help="UTF-8 JSON file holding structured brief values such as reader, judgment, and section_plan.",
    )
    parser.add_argument("--tone")
    parser.add_argument("--target-length")
    parser.add_argument("--emphasis", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--must-include", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--avoid", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    base_brief = read_base_brief(args.base_input)
    raw_text = read_raw_text(args, base_brief)
    context = read_context(args.context_file)
    brief = build_brief(args, raw_text, context, base_brief)
    payload = {
        "brief_hash": brief_hash(brief),
        "created_at": datetime.now(KST).isoformat(timespec="seconds"),
        "brief": brief,
    }
    output_path = args.output_dir / f"{payload['brief_hash']}_input.json"
    write_json(output_path, payload, overwrite=args.overwrite)

    result = validate_input(output_path)
    if result["status"] != "PASS":
        print(json.dumps(result, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1

    print(json.dumps({"status": "PASS", "input": str(output_path)}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
