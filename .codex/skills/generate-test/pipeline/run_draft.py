from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


PIPELINE_DIR = Path(__file__).resolve().parent

MODES = ["bundled", "contract", "unit"]


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the test-generation harness pipeline for one input.")
    parser.add_argument("input", type=Path)
    parser.add_argument("--mode", choices=MODES, default="contract")
    parser.add_argument("--provider", choices=["codex", "claude"], default="codex")
    parser.add_argument("--max-iterations", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument(
        "--runs-dir",
        type=Path,
        default=None,
        help="미지정 시 runner가 mode에 따라 runs/{split,bundled}/ 로 결정.",
    )
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    input_path = args.input.resolve()
    command = [
        sys.executable,
        "-B",
        str(PIPELINE_DIR / "runner.py"),
        str(input_path),
        "--mode",
        args.mode,
        "--provider",
        args.provider,
        "--max-iterations",
        str(args.max_iterations),
        "--timeout-seconds",
        str(args.timeout_seconds),
    ]
    if args.runs_dir is not None:
        command.extend(["--runs-dir", str(args.runs_dir)])
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
        for artifact in result.get("artifacts", []):
            print(f"artifact: {artifact}")
    elif result.get("failed"):
        print(f"failed: {result.get('failed')}")

    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())
