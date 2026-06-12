from __future__ import annotations

import argparse
import json
import shutil
import sys
import tempfile
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

from stages.generator import generate
from validate import validate_file, write_result


PROJECT_DIR = Path(__file__).resolve().parent
RUNS_DIR = PROJECT_DIR / "runs"
KST = timezone(timedelta(hours=9))

MODEL_CODEX_DEFAULT = None
MODEL_GPT_5_5 = "gpt-5.5"
MODEL_O3 = "o3"

AGENT_GEN = "gen"
AGENT_CRITIQUE = "critique"
AGENT_EVAL = "eval"
AGENT_REFINE = "refine"

AGENT_MODELS = {
    AGENT_GEN: MODEL_GPT_5_5,
    AGENT_CRITIQUE: MODEL_CODEX_DEFAULT,
    AGENT_EVAL: MODEL_CODEX_DEFAULT,
    AGENT_REFINE: MODEL_CODEX_DEFAULT,
}


@dataclass(frozen=True)
class RunContext:
    brief_hash: str
    iteration: str
    runs_dir: Path
    run_id: str

    @classmethod
    def create(cls, brief_hash: str, iteration: str, runs_dir: Path) -> "RunContext":
        today = datetime.now(KST).date().isoformat()
        return cls(
            brief_hash=brief_hash,
            iteration=iteration,
            runs_dir=runs_dir.resolve(),
            run_id=f"{today}_{brief_hash}",
        )

    @property
    def run_dir(self) -> Path:
        return self.runs_dir / self.run_id

    @property
    def iter_dir(self) -> Path:
        return self.run_dir / f"iter_{self.iteration}"

    @property
    def copied_input_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_input.json"

    @property
    def draft_path(self) -> Path:
        return self.iter_dir / f"{self.brief_hash}_iter-{self.iteration}_draft.json"

    @property
    def draft_validation_path(self) -> Path:
        return self.iter_dir / f"{self.brief_hash}_iter-{self.iteration}_draft.validation.json"


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as file:
        data = json.load(file)
    if not isinstance(data, dict):
        raise ValueError(f"expected JSON object: {path}")
    return data


def write_json(path: Path, data: dict, overwrite: bool = False) -> None:
    if path.exists() and not overwrite:
        raise FileExistsError(f"refusing to overwrite existing file: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def copy_input(source: Path, destination: Path, overwrite: bool = False) -> None:
    if destination.exists() and not overwrite:
        current = destination.read_text(encoding="utf-8")
        incoming = source.read_text(encoding="utf-8")
        if current == incoming:
            return
        raise FileExistsError(f"input file already exists with different content: {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)


def ensure_pass(result: dict, result_path: Path | None = None) -> None:
    if result["status"] == "PASS":
        return
    if result_path:
        write_result(result, result_path)
    raise RuntimeError(json.dumps(result, ensure_ascii=False, indent=2))


def write_failed(
    run_dir: Path,
    brief_hash: str,
    run_id: str,
    stage: str,
    error: Exception,
    lineage: dict[str, str],
    config: dict[str, object],
) -> Path:
    failed_path = run_dir / f"{brief_hash}_failed.json"
    payload = {
        "brief_hash": brief_hash,
        "run_id": run_id,
        "failed_at": now_iso(),
        "stage": stage,
        "error_type": type(error).__name__,
        "message": str(error),
        "config": config,
        "lineage": lineage,
    }
    write_json(failed_path, payload, overwrite=True)
    return failed_path


def now_iso() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def build_draft(
    input_data: dict,
    gen_output: dict,
    iteration: str,
    model_name: str,
    token_usage: dict | None = None,
) -> dict:
    metadata = {
        "prompt_version": "gen_system:v1",
        "source_files": [f'{input_data["brief_hash"]}_input.json'],
    }
    if token_usage:
        metadata["token_usage"] = token_usage

    return {
        "brief_hash": input_data["brief_hash"],
        "iteration": iteration,
        "stage": "gen",
        "content": gen_output["content"],
        "generated_at": now_iso(),
        "model": model_name,
        "metadata": metadata,
    }


def run(args: argparse.Namespace) -> dict:
    stage = "input_validate"
    input_path = args.input.resolve()
    input_result = validate_file(input_path, artifact="input")
    ensure_pass(input_result)

    input_data = load_json(input_path)
    brief_hash = input_data["brief_hash"]
    context = RunContext.create(
        brief_hash=brief_hash,
        iteration=args.iteration,
        runs_dir=args.runs_dir,
    )
    lineage = {
        "input": str(context.copied_input_path),
        "draft": str(context.draft_path),
    }
    config = {
        "codex_bin": args.codex_bin,
        "codex_access": "dangerously-bypass-approvals-and-sandbox",
        "agent_models": resolve_agent_models(args),
        "iteration": args.iteration,
        "timeout_seconds": args.timeout_seconds,
    }

    try:
        stage = "prepare"
        copy_input(input_path, context.copied_input_path, overwrite=args.overwrite)
        context.iter_dir.mkdir(parents=True, exist_ok=True)
        if not args.overwrite:
            if context.draft_path.exists():
                raise FileExistsError(f"refusing to overwrite existing file: {context.draft_path}")

        with tempfile.TemporaryDirectory(prefix="writing-harness-gen-") as temp_dir:
            temp_gen_output_path = Path(temp_dir) / "gen-output.json"

            stage = "gen"
            token_usage = generate(
                input_path=context.copied_input_path,
                output_path=temp_gen_output_path,
                codex_bin=args.codex_bin,
                model=config["agent_models"][AGENT_GEN],
                timeout_seconds=args.timeout_seconds,
            )

            stage = "gen_validate"
            gen_result = validate_file(temp_gen_output_path, artifact="gen_output")
            ensure_pass(gen_result)
            gen_output = load_json(temp_gen_output_path)

        stage = "draft"
        draft = build_draft(
            input_data=input_data,
            gen_output=gen_output,
            iteration=args.iteration,
            model_name=config["agent_models"][AGENT_GEN] or "codex-cli-default",
            token_usage=token_usage,
        )
        write_json(context.draft_path, draft, overwrite=args.overwrite)

        stage = "draft_validate"
        draft_result = validate_file(
            context.draft_path,
            artifact="draft",
            expected_brief_hash=brief_hash,
            expected_iteration=args.iteration,
        )
        ensure_pass(draft_result, context.draft_validation_path)
    except Exception as exc:
        failed_path = write_failed(context.run_dir, brief_hash, context.run_id, stage, exc, lineage, config)
        raise RuntimeError(f"pipeline failed at {stage}; wrote {failed_path}") from exc

    return {
        "status": "PASS",
        "run_id": context.run_id,
        "input": str(context.copied_input_path),
        "draft": str(context.draft_path),
    }


def resolve_agent_models(args: argparse.Namespace) -> dict[str, str | None]:
    models = AGENT_MODELS.copy()
    if args.model:
        models[AGENT_GEN] = args.model
    if args.gen_model:
        models[AGENT_GEN] = args.gen_model
    return models


def main() -> int:
    parser = argparse.ArgumentParser(description="Run MVP: input.json -> gen -> draft.json -> validate.")
    parser.add_argument("input", type=Path, help="Path to an input JSON file matching input.schema.json.")
    parser.add_argument("--codex-bin", default="codex")
    parser.add_argument("--model", help="Alias for --gen-model in the current MVP.")
    parser.add_argument("--gen-model", help="Model for the Gen agent. Defaults to the official Codex recommended model.")
    parser.add_argument("--runs-dir", type=Path, default=RUNS_DIR)
    parser.add_argument("--iteration", default="001")
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing MVP artifacts for the same run.")
    args = parser.parse_args()

    if len(args.iteration) != 3 or not args.iteration.isdigit():
        raise ValueError("--iteration must use a 3-digit value such as 001")

    result = run(args)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
