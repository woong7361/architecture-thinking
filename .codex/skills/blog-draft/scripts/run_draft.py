from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
PIPELINE_DIR = SCRIPT_DIR / "writing-harness-pipeline"


def default_runs_dir() -> Path:
    if Path("writing-harness-pipeline").is_dir():
        return Path("writing-harness-pipeline") / "runs"
    return Path("runs")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the bundled writing-harness pipeline for a blog-draft input.")
    parser.add_argument("input", type=Path)
    parser.add_argument("--provider", choices=["codex", "claude"], default="codex")
    parser.add_argument("--max-iterations", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--runs-dir", type=Path, default=default_runs_dir())
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    input_path = args.input.resolve()
    command = [
        sys.executable,
        "-B",
        str(PIPELINE_DIR / "runner.py"),
        str(input_path),
        "--provider",
        args.provider,
        "--runs-dir",
        str(args.runs_dir),
        "--max-iterations",
        str(args.max_iterations),
        "--timeout-seconds",
        str(args.timeout_seconds),
    ]
    if args.overwrite:
        command.append("--overwrite")

    completed = subprocess.run(
        command,
        text=True,
        capture_output=True,
        encoding="utf-8",
    )
    if completed.stderr:
        print(completed.stderr, file=sys.stderr, end="")
    if completed.stdout:
        print(completed.stdout, end="")

    if completed.returncode != 0:
        return completed.returncode

    try:
        result = json.loads(completed.stdout)
    except json.JSONDecodeError:
        return completed.returncode

    if result.get("status") == "PASS":
        print(f"final: {result.get('final')}")
    elif result.get("failed"):
        print(f"failed: {result.get('failed')}")
    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())
