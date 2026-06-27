from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
PIPELINE_DIR = SCRIPT_DIR / "writing-harness-pipeline"
DEFAULT_OUTPUT_DIR = PIPELINE_DIR / "inputs"
KST = timezone(timedelta(hours=9))


def read_raw_text(args: argparse.Namespace) -> str:
    chunks: list[str] = []
    for path in args.raw_text_file or []:
        chunks.append(Path(path).read_text(encoding="utf-8"))
    if args.raw_text:
        chunks.append(args.raw_text)
    raw_text = "\n\n".join(chunk.strip() for chunk in chunks if chunk.strip()).strip()
    if not raw_text:
        raise ValueError("raw text is required via --raw-text or --raw-text-file")
    return raw_text


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


def build_brief(args: argparse.Namespace, raw_text: str) -> dict:
    constraints: dict[str, object] = {}
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

    brief = {
        "topic": args.topic,
        "raw_text": raw_text,
        "piece_type": args.piece_type,
        "intent": args.intent,
        "audience": args.audience,
    }
    if constraints:
        brief["constraints"] = constraints
    return {key: value for key, value in brief.items() if value}


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
    parser.add_argument("--raw-text", help="Raw source material to preserve in brief.raw_text.")
    parser.add_argument("--raw-text-file", action="append", help="UTF-8 file containing raw source material.")
    parser.add_argument("--topic", required=True)
    parser.add_argument("--piece-type", default="retrospective")
    parser.add_argument("--intent", required=True)
    parser.add_argument("--audience", required=True)
    parser.add_argument("--tone")
    parser.add_argument("--target-length")
    parser.add_argument("--emphasis", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--must-include", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--avoid", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    raw_text = read_raw_text(args)
    brief = build_brief(args, raw_text)
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
