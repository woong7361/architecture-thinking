#!/usr/bin/env python
"""Prepare and manage Thinking Review Gate Level 3 file hand-off runs."""

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
CRITIQUE_SYSTEM = SKILL_DIR / "prompts" / "level3-critique.system.md"
EVAL_SYSTEM = SKILL_DIR / "prompts" / "level3-eval.system.md"
RUBRIC = SKILL_DIR / "rubric.yaml"
SCHEMA = SKILL_DIR / "schemas" / "level3-eval.schema.json"
VALIDATE = SCRIPT_DIR / "validate.py"


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
    return text or ""


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def artifact_prompt(system_path: Path, run_dir: Path, output_name: str) -> str:
    return f"""# Level 3 Artifact Task

Use the system instructions in:

```text
{system_path}
```

Read these inputs:

```text
{run_dir / "input.md"}
{run_dir / "context.md"}
{run_dir / "draft.md"}
```

Additional references:

```text
rubric: {RUBRIC}
schema: {SCHEMA}
```

Artifact output path:

```text
{run_dir / output_name}
```

Your final response is the complete artifact content. The caller saves that final
message to the artifact output path with Codex CLI `--output-last-message`.

Do not write a separate receipt. Do not wrap the artifact in Markdown fences
unless the artifact format itself is Markdown.
"""


def init_run(args: argparse.Namespace) -> int:
    runs_dir = args.runs_dir.resolve()
    run_id = safe_run_id(args.run_id or slug_timestamp())
    run_dir = runs_dir / run_id
    ensure_under(runs_dir, run_dir)
    run_dir.mkdir(parents=True, exist_ok=False)

    input_text = read_value(args.input_text, args.input_file)
    context_text = read_value(args.context_text, args.context_file)
    draft_text = read_value(args.draft_text, args.draft_file)

    write(run_dir / "input.md", input_text or "# User Input\n\nTODO: capture user request.")
    write(run_dir / "context.md", context_text or "# Project Context\n\nTODO: capture checked context.")
    write(run_dir / "draft.md", draft_text or "# Draft Answer\n\nTODO: write draft answer.")
    write(run_dir / "critique.prompt.md", artifact_prompt(CRITIQUE_SYSTEM, run_dir, "critique.md"))
    write(run_dir / "eval.prompt.md", artifact_prompt(EVAL_SYSTEM, run_dir, "eval.json"))

    manifest: dict[str, Any] = {
        "version": 1,
        "run_id": run_id,
        "created_at": now_utc().isoformat(),
        "model": args.model,
        "paths": {
            "run_dir": str(run_dir),
            "input": str(run_dir / "input.md"),
            "context": str(run_dir / "context.md"),
            "draft": str(run_dir / "draft.md"),
            "critique_prompt": str(run_dir / "critique.prompt.md"),
            "eval_prompt": str(run_dir / "eval.prompt.md"),
            "critique": str(run_dir / "critique.md"),
            "eval": str(run_dir / "eval.json"),
            "validation": str(run_dir / "validation.json"),
        },
    }
    write(run_dir / "manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
    print(json.dumps(manifest, ensure_ascii=False, indent=2))
    return 0


def validate_run(args: argparse.Namespace) -> int:
    run_dir = args.run_dir.resolve()
    eval_path = run_dir / "eval.json"
    validation_path = run_dir / "validation.json"
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
    parser = argparse.ArgumentParser(description="Manage Level 3 critique runs")
    subparsers = parser.add_subparsers(dest="command", required=True)

    init = subparsers.add_parser("init", help="Create a Level 3 run directory")
    init.add_argument("--run-id", help="Optional stable run id")
    init.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR)
    init.add_argument("--model", default="inherit", help="Model label to record in manifest")
    init.add_argument("--input-text")
    init.add_argument("--input-file", type=Path)
    init.add_argument("--context-text")
    init.add_argument("--context-file", type=Path)
    init.add_argument("--draft-text")
    init.add_argument("--draft-file", type=Path)
    init.set_defaults(func=init_run)

    validate = subparsers.add_parser("validate", help="Validate eval.json for a run")
    validate.add_argument("run_dir", type=Path)
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
