from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


PIPELINE_DIR = Path(__file__).resolve().parent


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the contract conformance pipeline for one input.")
    parser.add_argument("input", type=Path)
    parser.add_argument("--provider", choices=["codex", "claude"], default="codex")
    parser.add_argument("--model", help="네 단계 전부의 모델. 단계별 인자가 이것을 덮는다.")
    parser.add_argument("--gen-model")
    parser.add_argument("--critique-model")
    parser.add_argument("--eval-model")
    parser.add_argument("--refine-model")
    parser.add_argument("--iteration", default="001")
    parser.add_argument("--max-iterations", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--runs-dir", type=Path, default=None, help="미지정 시 <skill>/runs/.")
    parser.add_argument("--rubric", type=Path, default=None)
    parser.add_argument("--skeleton-dir", type=Path, default=None)
    parser.add_argument("--java-home", default=None, help="JDK 17 이상의 경로. 미지정이면 환경 변수에 맡긴다.")
    parser.add_argument("--rules", type=Path, default=None)
    parser.add_argument("--max-build-attempts", type=int, default=None)
    parser.add_argument("--allow-missing-gates", action="store_true", help="개발용. 붙은 run은 통과로 읽지 않는다.")
    parser.add_argument("--prose-slots", type=Path, default=None)
    parser.add_argument("--origin", type=Path, action="append")
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    command = [
        sys.executable,
        "-B",
        str(PIPELINE_DIR / "runner.py"),
        str(args.input.resolve()),
        "--provider",
        args.provider,
        "--iteration",
        args.iteration,
        "--max-iterations",
        str(args.max_iterations),
        "--timeout-seconds",
        str(args.timeout_seconds),
    ]
    for flag, value in (
        ("--runs-dir", args.runs_dir),
        ("--rubric", args.rubric),
        ("--skeleton-dir", args.skeleton_dir),
        ("--prose-slots", args.prose_slots),
        ("--rules", args.rules),
        ("--java-home", args.java_home),
        ("--model", args.model),
        ("--gen-model", args.gen_model),
        ("--critique-model", args.critique_model),
        ("--eval-model", args.eval_model),
        ("--refine-model", args.refine_model),
        ("--max-build-attempts", args.max_build_attempts),
    ):
        if value is not None:
            command.extend([flag, str(value)])
    for origin in args.origin or []:
        command.extend(["--origin", str(origin)])
    if args.allow_missing_gates:
        command.append("--allow-missing-gates")
    if args.overwrite:
        command.append("--overwrite")

    sys.path.insert(0, str(PIPELINE_DIR))
    from stages.scripts.textio import run_command  # noqa: E402

    completed = run_command(command)
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
        print(f"contract_version: {json.dumps(result.get('contract_version'), ensure_ascii=False)}")
        print(f"decision_risk: {json.dumps(result.get('decision_risk'), ensure_ascii=False)}")
        for artifact in result.get("artifacts", []):
            print(f"artifact: {artifact}")
    elif result.get("failed"):
        print(f"failed: {result.get('failed')}")

    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())
