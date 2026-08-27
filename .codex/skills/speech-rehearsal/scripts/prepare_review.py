from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import sys

from jsonschema import Draft202012Validator


SKILL_DIR = Path(__file__).resolve().parent.parent
CONTEXT_SCHEMA_PATH = SKILL_DIR / "schemas" / "review-context.schema.json"
TRANSCRIPT_SCHEMA_PATH = SKILL_DIR / "schemas" / "transcript.schema.json"
RESOURCE_PATHS = {
    "delivery": {
        "role": SKILL_DIR / "references" / "roles" / "delivery-reviewer.md",
        "rubric": SKILL_DIR / "references" / "rubrics" / "delivery.yaml",
        "output_schema": SKILL_DIR / "schemas" / "delivery-output.schema.json",
    },
    "logic": {
        "role": SKILL_DIR / "references" / "roles" / "senior-logic-reviewer.md",
        "rubric": SKILL_DIR / "references" / "rubrics" / "logic.yaml",
        "output_schema": SKILL_DIR / "schemas" / "logic-output.schema.json",
    },
}
TOKEN_PATTERN = re.compile(r"\S+")


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def load_json(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"expected JSON object: {path}")
    return value


def load_transcript(path: Path) -> tuple[str, list[dict], float | None, dict]:
    if path.suffix.lower() == ".json":
        value = load_json(path)
        validate_value(value, TRANSCRIPT_SCHEMA_PATH, "transcript")
        text = value.get("text")
        if not isinstance(text, str) or not text.strip():
            raise ValueError("transcript JSON requires non-empty text")
        segments = value.get("segments", [])
        if not isinstance(segments, list):
            raise ValueError("transcript segments must be an array")
        duration = numeric_duration(value.get("duration_seconds"))
        source = value.get("source") if isinstance(value.get("source"), dict) else {}
        source = {**source, "transcript_path": str(path.resolve()), "transcript_sha256": sha256_file(path)}
        return text.strip(), normalize_segments(segments), duration, source

    text = path.read_text(encoding="utf-8").strip()
    if not text:
        raise ValueError("transcript text is empty")
    source = {"transcript_path": str(path.resolve()), "transcript_sha256": sha256_file(path)}
    return text, [], None, source


def numeric_duration(value: object) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or value <= 0:
        return None
    return float(value)


def normalize_segments(segments: list[object]) -> list[dict]:
    normalized = []
    for index, segment in enumerate(segments, start=1):
        if not isinstance(segment, dict):
            continue
        text = str(segment.get("text", "")).strip()
        start = segment.get("start_seconds", segment.get("start"))
        end = segment.get("end_seconds", segment.get("end"))
        normalized.append(
            {
                "id": f"s{index:04d}",
                "text": text,
                "start_seconds": numeric_nonnegative(start),
                "end_seconds": numeric_nonnegative(end),
            }
        )
    return normalized


def numeric_nonnegative(value: object) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or value < 0:
        return None
    return float(value)


def infer_duration(segments: list[dict]) -> float | None:
    ends = [segment["end_seconds"] for segment in segments if segment.get("end_seconds") is not None]
    return max(ends) if ends else None


def tokenize(text: str) -> list[dict]:
    tokens = []
    for index, match in enumerate(TOKEN_PATTERN.finditer(text), start=1):
        tokens.append(
            {
                "id": f"t{index:04d}",
                "text": match.group(0),
                "char_start": match.start(),
                "char_end": match.end(),
            }
        )
    return tokens


def calculate_segment_pace(segments: list[dict]) -> list[dict]:
    results = []
    for segment in segments:
        start = segment.get("start_seconds")
        end = segment.get("end_seconds")
        duration = end - start if start is not None and end is not None else None
        token_count = len(TOKEN_PATTERN.findall(segment.get("text", "")))
        rate = round(token_count * 60 / duration, 2) if duration and duration > 0 else None
        results.append(
            {
                "segment_id": segment["id"],
                "start_seconds": start,
                "end_seconds": end,
                "whitespace_token_count": token_count,
                "tokens_per_minute": rate,
            }
        )
    return results


def resource_manifest() -> dict:
    resources = {}
    for reviewer, paths in RESOURCE_PATHS.items():
        resources[reviewer] = {}
        for kind, path in paths.items():
            if not path.is_file():
                raise FileNotFoundError(f"missing {kind} resource: {path}")
            resources[reviewer][kind] = {
                "path": path.relative_to(SKILL_DIR).as_posix(),
                "sha256": sha256_file(path),
            }
    return resources


def presentation_plan(path: Path | None) -> dict:
    if path is None:
        return {"available": False, "text": None, "source": None}
    resolved = path.resolve()
    text = resolved.read_text(encoding="utf-8")
    return {
        "available": True,
        "text": text,
        "source": {"path": str(resolved), "sha256": sha256_file(resolved)},
    }


def canonical_json(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def validate_context(context: dict) -> None:
    validate_value(context, CONTEXT_SCHEMA_PATH, "review context")


def validate_value(value: dict, schema_path: Path, artifact_name: str) -> None:
    schema = load_json(schema_path)
    errors = sorted(Draft202012Validator(schema).iter_errors(value), key=lambda error: list(error.path))
    if errors:
        details = "; ".join(error.message for error in errors[:5])
        raise ValueError(f"{artifact_name} schema validation failed: {details}")


def write_json(path: Path, value: dict) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def build_context(transcript_path: Path, duration_override: float | None, plan_path: Path | None) -> dict:
    text, segments, transcript_duration, source = load_transcript(transcript_path)
    tokens = tokenize(text)
    if not tokens:
        raise ValueError("transcript has no whitespace-delimited tokens")

    duration = duration_override or transcript_duration or infer_duration(segments)
    token_count = len(tokens)
    metrics = {
        "whitespace_token_count": token_count,
        "duration_seconds": duration,
        "tokens_per_minute": round(token_count * 60 / duration, 2) if duration else None,
        "segment_pace": calculate_segment_pace(segments),
    }
    body = {
        "schema_version": 1,
        "transcript": {"text": text, "tokens": tokens, "segments": segments, "source": source},
        "metrics": metrics,
        "presentation_plan": presentation_plan(plan_path),
        "resources": resource_manifest(),
    }
    context_id = sha256_bytes(canonical_json(body))[:12]
    context = {"schema_version": 1, "context_id": context_id, **{key: value for key, value in body.items() if key != "schema_version"}}
    validate_context(context)
    return context


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prepare immutable, shared input for both speech reviewers.")
    parser.add_argument("transcript", type=Path)
    parser.add_argument("--duration-seconds", type=float)
    parser.add_argument("--plan", type=Path)
    parser.add_argument("--output-dir", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    transcript_path = args.transcript.resolve()
    if not transcript_path.is_file():
        raise FileNotFoundError(f"transcript not found: {transcript_path}")
    if args.duration_seconds is not None and args.duration_seconds <= 0:
        raise ValueError("duration-seconds must be positive")
    if args.plan is not None and not args.plan.is_file():
        raise FileNotFoundError(f"presentation plan not found: {args.plan}")

    output_dir = args.output_dir.resolve()
    context_path = output_dir / "review-context.json"
    manifest_path = output_dir / "manifest.json"
    if context_path.exists() or manifest_path.exists():
        raise FileExistsError(f"refusing to overwrite an existing review input in {output_dir}")
    output_dir.mkdir(parents=True, exist_ok=True)

    context = build_context(transcript_path, args.duration_seconds, args.plan)
    write_json(context_path, context)
    manifest = {
        "schema_version": 1,
        "context_id": context["context_id"],
        "created_at": datetime.now(timezone.utc).isoformat(),
        "context_path": str(context_path),
        "context_sha256": sha256_file(context_path),
        "reviewers": ["delivery_reviewer", "senior_logic_reviewer"],
        "execution": "parallel-independent",
        "resources": context["resources"],
    }
    write_json(manifest_path, manifest)
    print(context_path)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
