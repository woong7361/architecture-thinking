#!/usr/bin/env python
"""Run Level 3 critique and eval artifact agents in parallel."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
SKILL_DIR = SCRIPT_DIR.parent
SCHEMA = SKILL_DIR / "schemas" / "level3-eval.schema.json"
VALIDATE = SCRIPT_DIR / "validate.py"


@dataclass(frozen=True)
class AgentJob:
    name: str
    prompt_path: Path
    output_path: Path
    output_schema: Path | None
    model: str | None


@dataclass(frozen=True)
class AgentResult:
    name: str
    output_path: Path
    returncode: int
    elapsed_seconds: float


def timestamp() -> str:
    return datetime.now().strftime("%H:%M:%S")


def line(message: str) -> None:
    print(f"[{timestamp()}] {message}", flush=True)


def ensure_file(path: Path, label: str) -> None:
    if not path.exists():
        raise FileNotFoundError(f"missing {label}: {path}")


def ensure_can_write(path: Path, overwrite: bool) -> None:
    if path.exists() and not overwrite:
        raise FileExistsError(f"refusing to overwrite existing artifact: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)


def build_command(args: argparse.Namespace, job: AgentJob, workspace: Path) -> list[str]:
    command = [args.codex_bin]
    if args.approval:
        command.extend(["--ask-for-approval", args.approval])
    if args.search:
        command.append("--search")

    command.append("exec")
    if args.ephemeral:
        command.append("--ephemeral")
    if job.model:
        command.extend(["--model", job.model])
    if args.sandbox:
        command.extend(["--sandbox", args.sandbox])

    command.extend(["-C", str(workspace)])
    if job.output_schema is not None:
        command.extend(["--output-schema", str(job.output_schema)])
    command.extend(["--output-last-message", str(job.output_path), "-"])
    return command


def run_job(args: argparse.Namespace, job: AgentJob, workspace: Path) -> AgentResult:
    ensure_file(job.prompt_path, f"{job.name} prompt")
    ensure_can_write(job.output_path, args.overwrite)

    prompt = job.prompt_path.read_text(encoding="utf-8")
    command = build_command(args, job, workspace)

    if args.dry_run:
        line(f"{job.name} dry-run command={' '.join(command)}")
        return AgentResult(job.name, job.output_path, 0, 0.0)

    started = time.perf_counter()
    line(f"{job.name} start output={job.output_path}")
    try:
        completed = subprocess.run(
            command,
            input=prompt,
            text=True,
            capture_output=True,
            encoding="utf-8",
            timeout=args.timeout_seconds,
        )
    except subprocess.TimeoutExpired as exc:
        elapsed = time.perf_counter() - started
        raise TimeoutError(
            f"{job.name} timed out after {elapsed:.2f}s; output={job.output_path}"
        ) from exc

    elapsed = time.perf_counter() - started
    if completed.returncode != 0:
        raise RuntimeError(
            f"{job.name} failed returncode={completed.returncode}\n"
            f"command={command}\nstdout={completed.stdout}\nstderr={completed.stderr}"
        )

    return AgentResult(job.name, job.output_path, completed.returncode, elapsed)


def validate_eval(run_dir: Path, no_exit_on_gate_fail: bool) -> int:
    eval_path = run_dir / "eval.json"
    validation_path = run_dir / "validation.json"
    command = [
        sys.executable,
        str(VALIDATE),
        str(eval_path),
        "--output",
        str(validation_path),
    ]
    if no_exit_on_gate_fail:
        command.append("--no-exit-on-gate-fail")
    return subprocess.call(command)


def load_gate_result(run_dir: Path) -> str | None:
    validation_path = run_dir / "validation.json"
    if not validation_path.exists():
        return None
    try:
        validation = json.loads(validation_path.read_text(encoding="utf-8"))
    except Exception:  # noqa: BLE001 - runner summary should not hide validation exit code.
        return None
    gate_result = validation.get("gate_result")
    return gate_result if gate_result in {"pass", "fail"} else None


def run_parallel(args: argparse.Namespace) -> int:
    run_dir = args.run_dir.resolve()
    workspace = (args.workspace or Path.cwd()).resolve()
    ensure_file(run_dir / "manifest.json", "manifest")

    critique_model = args.critique_model or args.model
    eval_model = args.eval_model or args.model
    jobs = [
        AgentJob(
            name="critique",
            prompt_path=run_dir / "critique.prompt.md",
            output_path=run_dir / "critique.md",
            output_schema=None,
            model=critique_model,
        ),
        AgentJob(
            name="eval",
            prompt_path=run_dir / "eval.prompt.md",
            output_path=run_dir / "eval.json",
            output_schema=SCHEMA,
            model=eval_model,
        ),
    ]

    line(f"run start run_dir={run_dir}")
    results: list[AgentResult] = []
    with ThreadPoolExecutor(max_workers=2) as executor:
        future_to_job = {
            executor.submit(run_job, args, job, workspace): job for job in jobs
        }
        for future in as_completed(future_to_job):
            job = future_to_job[future]
            try:
                result = future.result()
            except Exception as exc:  # noqa: BLE001 - CLI should report agent failures plainly.
                line(f"{job.name} failed error={type(exc).__name__}")
                print(str(exc), file=sys.stderr)
                return 2
            results.append(result)
            line(
                f"{result.name} done elapsed={result.elapsed_seconds:.2f}s "
                f"artifact={result.output_path}"
            )

    if args.dry_run:
        print(
            json.dumps(
                {
                    "run_result": "dry-run",
                    "run_dir": str(run_dir),
                    "artifacts": {result.name: str(result.output_path) for result in results},
                },
                ensure_ascii=False,
                indent=2,
            )
        )
        return 0

    if args.skip_validate:
        run_result = "artifacts-written"
        gate_result = None
        validation_code = None
    else:
        line("validate start")
        validation_code = validate_eval(run_dir, args.no_exit_on_gate_fail)
        gate_result = load_gate_result(run_dir)
        run_result = "completed" if validation_code == 0 else "validation-failed"
        line(
            "validate done "
            f"run_result={run_result} gate_result={gate_result or 'unknown'} "
            f"exit_code={validation_code}"
        )

    print(
        json.dumps(
            {
                "run_result": run_result,
                "gate_result": gate_result,
                "run_dir": str(run_dir),
                "artifacts": {result.name: str(result.output_path) for result in results},
                "validation_exit_code": validation_code,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return validation_code or 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run Level 3 critique and eval Codex CLI agents in parallel."
    )
    parser.add_argument("run_dir", type=Path, help="Level 3 run directory")
    parser.add_argument("--workspace", type=Path, help="Workspace passed to Codex CLI -C")
    parser.add_argument("--codex-bin", default="codex")
    parser.add_argument("--model", help="Model for both agents")
    parser.add_argument("--critique-model", help="Model for critique agent")
    parser.add_argument("--eval-model", help="Model for eval agent")
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--sandbox", default="danger-full-access")
    parser.add_argument("--approval", default="never")
    parser.add_argument("--search", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--ephemeral", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--skip-validate", action="store_true")
    parser.add_argument("--no-exit-on-gate-fail", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def main() -> int:
    try:
        return run_parallel(parse_args())
    except Exception as exc:  # noqa: BLE001 - CLI should report failures plainly.
        print(f"error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
