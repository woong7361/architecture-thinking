#!/usr/bin/env python
"""Prepare and manage Thinking Review Gate Level 2 file hand-off runs."""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
SKILL_DIR = SCRIPT_DIR.parent
DEFAULT_RUNS_DIR = SKILL_DIR / "runs"
CRITIQUE_SYSTEM = SKILL_DIR / "prompts" / "level2-critique.system.md"
EVAL_SYSTEM = SKILL_DIR / "prompts" / "level2-eval.system.md"
VALIDATE = SCRIPT_DIR / "validate.py"
DEFAULT_MAX_ATTEMPTS = 3


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def slug_timestamp() -> str:
    return now_utc().strftime("%Y%m%d-%H%M%S")


def safe_run_id(value: str) -> str:
    allowed = []
    for char in value.strip():
        if char.isalnum() or char in {"-", "_"}:
            allowed.append(char)
        else:
            allowed.append("-")
    result = "".join(allowed).strip("-_")
    return result or slug_timestamp()


def ensure_under(parent: Path, child: Path) -> None:
    parent_resolved = parent.resolve()
    child_resolved = child.resolve()
    if parent_resolved != child_resolved and parent_resolved not in child_resolved.parents:
        raise ValueError(f"path escapes runs dir: {child}")


def read_value(text: str | None, file_path: Path | None) -> str:
    if file_path is not None:
        return file_path.read_text(encoding="utf-8")
    if text == "-":
        return sys.stdin.read()
    return text or ""


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def update_manifest_latest(run_dir: Path, current_attempt_dir: Path) -> None:
    manifest_path = run_dir / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest.setdefault("paths", {})["latest_attempt"] = str(current_attempt_dir)
    write(manifest_path, json.dumps(manifest, ensure_ascii=False, indent=2))


def attempts_dir(run_dir: Path) -> Path:
    return run_dir / "attempts"


def attempt_dir(run_dir: Path, attempt: int) -> Path:
    return attempts_dir(run_dir) / str(attempt)


def latest_attempt_number(run_dir: Path) -> int:
    existing = []
    for child in attempts_dir(run_dir).iterdir():
        if child.is_dir() and child.name.isdigit():
            existing.append(int(child.name))
    if not existing:
        raise FileNotFoundError(f"missing attempts under: {attempts_dir(run_dir)}")
    return max(existing)


def resolve_attempt_dir(run_dir: Path, attempt: int | None) -> Path:
    number = attempt if attempt is not None else latest_attempt_number(run_dir)
    return attempt_dir(run_dir, number)


def combined_input(input_text: str, context_text: str) -> str:
    return f"""# Original User Input

{input_text}

# Checked Context

{context_text or "TODO: capture checked project context, evidence, constraints, and review criteria."}
"""


def init_run(args: argparse.Namespace) -> int:
    runs_dir = args.runs_dir.resolve()
    run_id = safe_run_id(args.run_id or slug_timestamp())
    run_dir = runs_dir / run_id
    ensure_under(runs_dir, run_dir)

    input_text = read_value(args.input_text, args.input_file)
    context_text = read_value(args.context_text, args.context_file)
    draft_text = read_value(args.draft_text, args.draft_file)
    if not input_text.strip():
        print(
            (
                "missing original user input: pass the user's request verbatim "
                "with --input-file or --input-text. Use --input-text - to read stdin."
            ),
            file=sys.stderr,
        )
        return 2

    run_dir.mkdir(parents=True, exist_ok=False)

    first_attempt_dir = attempt_dir(run_dir, 1)
    first_attempt_dir.mkdir(parents=True, exist_ok=True)

    write(run_dir / "input.md", combined_input(input_text, context_text))
    write(first_attempt_dir / "draft.md", draft_text or "# Draft Under Review\n\nTODO: write draft answer.")

    manifest: dict[str, Any] = {
        "version": 1,
        "run_id": run_id,
        "created_at": now_utc().isoformat(),
        "model": args.model,
        "paths": {
            "run_dir": str(run_dir),
            "input": str(run_dir / "input.md"),
            "attempts_dir": str(attempts_dir(run_dir)),
            "latest_attempt": str(first_attempt_dir),
        },
        "prompt_templates": {
            "critique": str(CRITIQUE_SYSTEM),
            "eval": str(EVAL_SYSTEM),
        },
    }
    write(run_dir / "manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
    print(json.dumps(manifest, ensure_ascii=False, indent=2))
    return 0


def add_attempt(args: argparse.Namespace) -> int:
    run_dir = args.run_dir.resolve()
    manifest_path = run_dir / "manifest.json"
    if not manifest_path.exists():
        print(f"missing manifest: {manifest_path}", file=sys.stderr)
        return 2

    next_attempt = latest_attempt_number(run_dir) + 1
    if next_attempt > args.max_attempts:
        print(
            (
                f"max attempts exceeded: next_attempt={next_attempt}, "
                f"max_attempts={args.max_attempts}"
            ),
            file=sys.stderr,
        )
        return 2

    current_attempt_dir = attempt_dir(run_dir, next_attempt)
    current_attempt_dir.mkdir(parents=True, exist_ok=False)

    draft_text = read_value(args.draft_text, args.draft_file)
    write(current_attempt_dir / "draft.md", draft_text or "# Draft Under Review\n\nTODO: write revised draft answer.")
    update_manifest_latest(run_dir, current_attempt_dir)

    print(
        json.dumps(
            {
                "run_dir": str(run_dir),
                "attempt": next_attempt,
                "attempt_dir": str(current_attempt_dir),
                "draft": str(current_attempt_dir / "draft.md"),
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


def validate_run(args: argparse.Namespace) -> int:
    run_dir = args.run_dir.resolve()
    current_attempt_dir = resolve_attempt_dir(run_dir, args.attempt)
    eval_path = current_attempt_dir / "eval.json"
    validation_path = current_attempt_dir / "validation.json"
    if not eval_path.exists():
        print(f"missing eval artifact: {eval_path}", file=sys.stderr)
        return 2
    command = [
        sys.executable,
        str(VALIDATE),
        str(eval_path),
        "--output",
        str(validation_path),
    ]
    if args.no_exit_on_gate_fail:
        command.append("--no-exit-on-gate-fail")
    return subprocess.call(command)


def cleanup(args: argparse.Namespace) -> int:
    runs_dir = args.runs_dir.resolve()
    cutoff = now_utc() - timedelta(days=args.older_than_days)
    if not runs_dir.exists():
        print(json.dumps({"deleted": [], "candidates": []}, ensure_ascii=False, indent=2))
        return 0

    candidates: list[Path] = []
    for child in runs_dir.iterdir():
        if not child.is_dir():
            continue
        ensure_under(runs_dir, child)
        modified = datetime.fromtimestamp(child.stat().st_mtime, timezone.utc)
        if modified < cutoff:
            candidates.append(child)

    deleted: list[str] = []
    if args.confirm_delete:
        for candidate in candidates:
            ensure_under(runs_dir, candidate)
            shutil.rmtree(candidate)
            deleted.append(str(candidate))

    print(
        json.dumps(
            {
                "dry_run": not args.confirm_delete,
                "older_than_days": args.older_than_days,
                "candidates": [str(path) for path in candidates],
                "deleted": deleted,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Manage Level 2 critique runs")
    subparsers = parser.add_subparsers(dest="command", required=True)

    init = subparsers.add_parser("init", help="Create a Level 2 run directory")
    init.add_argument("--run-id", help="Optional stable run id")
    init.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR)
    init.add_argument("--model", default="inherit", help="Model label to record in manifest")
    init.add_argument(
        "--input-text",
        help="Verbatim original user input. Use '-' to read stdin; prefer --input-file for multilingual text on Windows.",
    )
    init.add_argument(
        "--input-file",
        type=Path,
        help="UTF-8 file containing the verbatim original user input.",
    )
    init.add_argument("--context-text")
    init.add_argument("--context-file", type=Path)
    init.add_argument("--draft-text")
    init.add_argument("--draft-file", type=Path)
    init.set_defaults(func=init_run)

    attempt = subparsers.add_parser("attempt", help="Create the next attempt for an existing run")
    attempt.add_argument("run_dir", type=Path)
    attempt.add_argument("--draft-text")
    attempt.add_argument("--draft-file", type=Path)
    attempt.add_argument(
        "--max-attempts",
        type=int,
        default=DEFAULT_MAX_ATTEMPTS,
        help=f"Maximum allowed attempts for this run. Defaults to {DEFAULT_MAX_ATTEMPTS}.",
    )
    attempt.set_defaults(func=add_attempt)

    validate = subparsers.add_parser("validate", help="Validate eval.json for a run")
    validate.add_argument("run_dir", type=Path)
    validate.add_argument("--attempt", type=int, help="Attempt number to validate. Defaults to latest.")
    validate.add_argument("--no-exit-on-gate-fail", action="store_true")
    validate.set_defaults(func=validate_run)

    clean = subparsers.add_parser("cleanup", help="List or delete old run directories")
    clean.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR)
    clean.add_argument("--older-than-days", type=int, default=7)
    clean.add_argument(
        "--confirm-delete",
        action="store_true",
        help="Actually delete candidates. Omit for dry-run.",
    )
    clean.set_defaults(func=cleanup)

    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        return args.func(args)
    except Exception as exc:  # noqa: BLE001 - CLI should report failures plainly.
        print(f"error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
