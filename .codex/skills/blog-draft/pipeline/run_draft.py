from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


PIPELINE_DIR = Path(__file__).resolve().parent

SLOW_LOOP_MIN_RUNS = 5  # pending 통과 run이 이 수 이상이면 slow loop 발동


def default_runs_dir() -> Path:
    return PIPELINE_DIR.parent / "runs"


def pending_dir(runs_dir: Path) -> Path:
    return runs_dir / "pending"


def count_passing_pending(runs_dir: Path) -> int:
    """pending/ 아래에서 final.json이 있는 run 수를 센다."""
    p = pending_dir(runs_dir)
    if not p.exists():
        return 0
    return sum(1 for d in p.iterdir() if d.is_dir() and list(d.glob("*_final.json")))


def maybe_run_slow_loop(runs_dir: Path, provider: str, timeout_seconds: int) -> None:
    """pending 통과 run이 SLOW_LOOP_MIN_RUNS 이상이면 analyze_runs → proposer를 실행한다."""
    count = count_passing_pending(runs_dir)
    if count < SLOW_LOOP_MIN_RUNS:
        print(
            f"[slow-loop] pending passing runs={count} < {SLOW_LOOP_MIN_RUNS}, skip",
            file=sys.stderr,
        )
        return

    print(f"[slow-loop] pending passing runs={count} >= {SLOW_LOOP_MIN_RUNS}, starting", file=sys.stderr)

    # 1단계: analyze_runs.py → analysis.json
    analyze_cmd = [
        sys.executable,
        "-B",
        str(PIPELINE_DIR / "analyze_runs.py"),
        "--runs-dir",
        str(runs_dir),
        "--min-runs",
        str(SLOW_LOOP_MIN_RUNS),
    ]
    analyze_result = subprocess.run(analyze_cmd, text=True, capture_output=True, encoding="utf-8")
    if analyze_result.stderr:
        print(analyze_result.stderr, file=sys.stderr, end="")

    if analyze_result.returncode != 0:
        print("[slow-loop] analyze_runs failed, skip proposer", file=sys.stderr)
        return

    try:
        analyze_out = json.loads(analyze_result.stdout)
    except json.JSONDecodeError:
        print("[slow-loop] analyze_runs output parse error, skip proposer", file=sys.stderr)
        return

    if analyze_out.get("status") == "SKIP":
        print(f"[slow-loop] analyze_runs SKIP: {analyze_out.get('reason')}", file=sys.stderr)
        return

    analysis_path = analyze_out.get("analysis_path")
    if not analysis_path:
        print("[slow-loop] analysis_path missing, skip proposer", file=sys.stderr)
        return

    # 2단계: proposer (별도 스크립트로 실행)
    propose_cmd = [
        sys.executable,
        "-B",
        str(PIPELINE_DIR / "run_propose.py"),
        analysis_path,
        "--provider",
        provider,
        "--runs-dir",
        str(runs_dir),
        "--timeout-seconds",
        str(timeout_seconds),
    ]
    propose_result = subprocess.run(propose_cmd, text=True, capture_output=True, encoding="utf-8")
    if propose_result.stderr:
        print(propose_result.stderr, file=sys.stderr, end="")
    if propose_result.stdout:
        print(propose_result.stdout, end="")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the bundled writing-harness pipeline for a blog-draft input.")
    parser.add_argument("input", type=Path)
    parser.add_argument("--provider", choices=["codex", "claude"], default="codex")
    parser.add_argument("--max-iterations", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--runs-dir", type=Path, default=default_runs_dir())
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--no-slow-loop", action="store_true", help="slow loop 트리거를 건너뛴다.")
    args = parser.parse_args()

    # fast loop은 pending/ 에 쓴다
    runs_dir = args.runs_dir
    fast_loop_runs_dir = pending_dir(runs_dir)

    input_path = args.input.resolve()
    command = [
        sys.executable,
        "-B",
        str(PIPELINE_DIR / "runner.py"),
        str(input_path),
        "--provider",
        args.provider,
        "--runs-dir",
        str(fast_loop_runs_dir),
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

    # slow loop 트리거 (PASS/FAILED 무관하게, 통과 run 수 기준으로 판단)
    if not args.no_slow_loop:
        maybe_run_slow_loop(
            runs_dir=runs_dir,
            provider=args.provider,
            timeout_seconds=args.timeout_seconds,
        )

    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())
