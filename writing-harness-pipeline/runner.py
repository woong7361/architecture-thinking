from __future__ import annotations

import argparse
from contextlib import contextmanager
import json
import shutil
import sys
import tempfile
import threading
import time
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

from stages.critique import critique
from stages.evaluator import evaluate
from stages.generator import generate
from stages.refine import refine
from validate import validate_file, write_result


PROJECT_DIR = Path(__file__).resolve().parent
RUNS_DIR = PROJECT_DIR / "runs"
RUBRIC_PATH = PROJECT_DIR / "rubric.yaml"
KST = timezone(timedelta(hours=9))

MODEL_CODEX_DEFAULT = None
MODEL_GPT_5_5 = "gpt-5.5"
MODEL_GPT_5_4 = "gpt-5.4"
MODEL_GPT_5_4_MINI = "gpt-5.4-mini"
MODEL_O3 = "o3"

AGENT_GEN = "gen"
AGENT_CRITIQUE = "critique"
AGENT_EVAL = "eval"
AGENT_REFINE = "refine"

AGENT_MODELS = {
    AGENT_GEN: MODEL_GPT_5_4_MINI,
    AGENT_CRITIQUE: MODEL_GPT_5_4_MINI,
    AGENT_EVAL: MODEL_GPT_5_4_MINI,
    AGENT_REFINE: MODEL_GPT_5_4_MINI,
}

FINAL_CHECKED_RULES = ["schema", "brief_hash", "min_total", "min_axis"]


def format_duration(seconds: float) -> str:
    return f"{seconds:.2f}s"


def format_running_duration(seconds: float) -> str:
    return f"{int(seconds)}s"


def format_score(value: object) -> str:
    if isinstance(value, (int, float)):
        return f"{value:g}"
    return "n/a"


def display_model(model: str | None) -> str:
    return model or "codex-cli-default"


def summarize_errors(errors: list[object], limit: int = 3) -> str:
    if not errors:
        return ""
    visible_errors = [str(error) for error in errors[:limit]]
    if len(errors) > limit:
        visible_errors.append(f"... +{len(errors) - limit} more")
    return "; ".join(visible_errors)


def format_eval_scores(eval_artifact: dict, rubric: dict) -> str:
    rubric_scores = eval_artifact.get("rubric_scores", {})
    if not isinstance(rubric_scores, dict):
        return "total=n/a"

    total = rubric_scores.get("weighted_total")
    scale = rubric.get("scale", {})
    max_score = scale.get("max", 5) if isinstance(scale, dict) else 5
    min_total = rubric.get("thresholds", {}).get("min_total", "n/a")
    scores = rubric_scores.get("scores", {})
    axes = ""
    if isinstance(scores, dict):
        axes = " axes=" + " ".join(f"{axis}:{format_score(score)}" for axis, score in scores.items())

    return f"total={format_score(total)}/{format_score(max_score)} min={format_score(min_total)}{axes}"


class ProgressReporter:
    def __init__(self, stream=sys.stderr, refresh_seconds: float = 1.0) -> None:
        self.stream = stream
        self.refresh_seconds = refresh_seconds
        self.interactive = bool(stream.isatty())
        self._lock = threading.Lock()
        self._last_live_length = 0

    def line(self, message: str) -> None:
        with self._lock:
            self.stream.write(f"[{self._timestamp()}] {message}\n")
            self.stream.flush()

    @contextmanager
    def step(self, label: str, live: bool = False):
        start = time.perf_counter()
        live_line = _LiveProgressLine(self, label, start) if live and self.interactive else None
        if live_line:
            live_line.start()
        else:
            self.line(f"{label} start")

        try:
            yield
        except Exception as exc:
            elapsed = time.perf_counter() - start
            message = f"{label} ERROR {format_duration(elapsed)} error={type(exc).__name__}"
            if live_line:
                live_line.finish(message)
            else:
                self.line(message)
            raise
        else:
            elapsed = time.perf_counter() - start
            message = f"{label} done {format_duration(elapsed)}"
            if live_line:
                live_line.finish(message)
            else:
                self.line(message)

    def validation(self, label: str, result: dict, extra: str = "") -> None:
        if result["status"] == "PASS":
            return
        suffix = f" {extra}" if extra else ""
        error_summary = summarize_errors(result.get("errors", []))
        errors = f" errors={error_summary}" if error_summary else ""
        self.line(f"{label} {result['status']}{suffix}{errors}")

    def _write_live(self, message: str) -> None:
        with self._lock:
            padded = message.ljust(self._last_live_length)
            self.stream.write(f"\r{padded}")
            self.stream.flush()
            self._last_live_length = len(message)

    def _finish_live(self, message: str) -> None:
        with self._lock:
            padded = message.ljust(self._last_live_length)
            self.stream.write(f"\r{padded}\n")
            self.stream.flush()
            self._last_live_length = 0

    def _timestamp(self) -> str:
        return datetime.now(KST).strftime("%H:%M:%S")


class _LiveProgressLine:
    def __init__(self, reporter: ProgressReporter, label: str, started_at: float) -> None:
        self.reporter = reporter
        self.label = label
        self.started_at = started_at
        self._stop = threading.Event()
        self._thread = threading.Thread(target=self._run, daemon=True)

    def start(self) -> None:
        self._write()
        self._thread.start()

    def finish(self, message: str) -> None:
        self._stop.set()
        self._thread.join()
        self.reporter._finish_live(f"[{self.reporter._timestamp()}] {message}")

    def _run(self) -> None:
        while not self._stop.wait(self.reporter.refresh_seconds):
            self._write()

    def _write(self) -> None:
        elapsed = time.perf_counter() - self.started_at
        self.reporter._write_live(
            f"[{self.reporter._timestamp()}] {self.label} running {format_running_duration(elapsed)}"
        )


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

    @property
    def critique_path(self) -> Path:
        return self.iter_dir / f"{self.brief_hash}_iter-{self.iteration}_critique.json"

    @property
    def critique_validation_path(self) -> Path:
        return self.iter_dir / f"{self.brief_hash}_iter-{self.iteration}_critique.validation.json"

    @property
    def eval_path(self) -> Path:
        return self.iter_dir / f"{self.brief_hash}_iter-{self.iteration}_eval.json"

    @property
    def eval_validation_path(self) -> Path:
        return self.iter_dir / f"{self.brief_hash}_iter-{self.iteration}_eval.validation.json"

    @property
    def final_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_final.json"

    @property
    def failed_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_failed.json"


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


def load_rubric(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")
    try:
        import yaml  # type: ignore[import-not-found]
    except ModuleNotFoundError:
        data = json.loads(text)
    else:
        data = yaml.safe_load(text)
    if not isinstance(data, dict):
        raise ValueError(f"expected rubric YAML object: {path}")
    return data


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
    stage_output: dict,
    iteration: str,
    model_name: str,
    token_usage: dict | None = None,
    source_stage: str = "gen",
) -> dict:
    metadata = {
        "prompt_version": f"{source_stage}_system:v1",
        "source_files": [f'{input_data["brief_hash"]}_input.json'],
    }
    if token_usage:
        metadata["token_usage"] = token_usage

    return {
        "brief_hash": input_data["brief_hash"],
        "iteration": iteration,
        "stage": source_stage,
        "content": stage_output["content"],
        "generated_at": now_iso(),
        "model": model_name,
        "metadata": metadata,
    }


def build_critique(
    critique_output: dict,
    iteration: str,
    model_name: str,
    token_usage: dict | None = None,
) -> dict:
    metadata = {
        "prompt_version": "critique_system:v1",
        "source_files": [
            f'{critique_output["brief_hash"]}_input.json',
            f'{critique_output["brief_hash"]}_iter-{iteration}_draft.json',
        ],
    }
    if token_usage:
        metadata["token_usage"] = token_usage

    return {
        **critique_output,
        "critiqued_at": now_iso(),
        "model": model_name,
        "metadata": metadata,
    }


def build_eval(
    eval_output: dict,
    iteration: str,
    model_name: str,
    token_usage: dict | None = None,
) -> dict:
    metadata = {
        "prompt_version": "eval_system:v1",
        "source_files": [
            f'{eval_output["brief_hash"]}_input.json',
            f'{eval_output["brief_hash"]}_iter-{iteration}_draft.json',
        ],
    }
    if token_usage:
        metadata["token_usage"] = token_usage

    return {
        **eval_output,
        "evaluated_at": now_iso(),
        "model": model_name,
        "metadata": metadata,
    }


def get_weak_axes(eval_data: dict, rubric: dict) -> list[str]:
    scores = eval_data.get("rubric_scores", {}).get("scores", {})
    min_axis = rubric.get("thresholds", {}).get("min_axis", {})
    weak_axes = []
    if not isinstance(scores, dict) or not isinstance(min_axis, dict):
        return weak_axes
    for axis, minimum in min_axis.items():
        score = scores.get(axis)
        if isinstance(score, (int, float)) and isinstance(minimum, (int, float)) and score < minimum:
            weak_axes.append(axis)
    return weak_axes


def get_refine_contract_errors(eval_result: dict) -> list[object]:
    errors = eval_result.get("errors", [])
    if not isinstance(errors, list):
        return []
    return [error for error in errors if categorize_failure(str(error)) == "contract_error"]


def build_refine_request(
    input_data: dict,
    draft: dict,
    critique_artifact: dict,
    eval_artifact: dict,
    eval_result: dict,
    rubric: dict,
    to_iteration: str,
) -> dict:
    weak_axes = get_weak_axes(eval_artifact, rubric)
    axis_rationales = eval_artifact.get("axis_rationales", {})
    weak_axis_rationales = {
        axis: axis_rationales[axis]
        for axis in weak_axes
        if isinstance(axis_rationales, dict) and axis in axis_rationales
    }
    weaknesses = critique_artifact.get("weaknesses", [])
    revision_priority = [
        item["suggestion"]
        for item in weaknesses
        if isinstance(item, dict) and item.get("severity") == "high" and item.get("suggestion")
    ]
    revision_priority.extend(weak_axes)

    return {
        "brief_hash": input_data["brief_hash"],
        "from_iteration": draft["iteration"],
        "to_iteration": to_iteration,
        "contract_errors": get_refine_contract_errors(eval_result),
        "weak_axes": weak_axes,
        "weak_axis_rationales": weak_axis_rationales,
        "revision_priority": revision_priority,
    }


def build_final(
    context: RunContext,
    input_data: dict,
    draft: dict,
    eval_artifact: dict,
    eval_result: dict,
    rubric: dict,
    refine_request_lineage: str | None,
) -> dict:
    lineage = {
        "run_id": context.run_id,
        "input": relative_to_run(context.copied_input_path, context.run_dir),
        "draft": relative_to_run(context.draft_path, context.run_dir),
        "critique": relative_to_run(context.critique_path, context.run_dir),
        "eval": relative_to_run(context.eval_path, context.run_dir),
    }
    if refine_request_lineage:
        lineage["refine_request"] = refine_request_lineage

    rubric_scores = eval_artifact["rubric_scores"]
    return {
        "brief_hash": input_data["brief_hash"],
        "final_iteration": context.iteration,
        "content": draft["content"],
        "accepted_at": now_iso(),
        "quality_snapshot": {
            "rubric_name": eval_artifact["rubric_name"],
            "weighted_total": rubric_scores["weighted_total"],
            "scores": rubric_scores["scores"],
            "weak_axes": get_weak_axes(eval_artifact, rubric),
        },
        "contract_result": {
            "verdict": "PASS",
            "contract_errors": [],
            "checked_rules": FINAL_CHECKED_RULES,
        },
        "lineage": lineage,
    }


def relative_to_run(path: Path, run_dir: Path) -> str:
    try:
        return path.relative_to(run_dir).as_posix()
    except ValueError:
        return str(path)


def next_iteration(iteration: str) -> str:
    return f"{int(iteration) + 1:03d}"


def write_max_iteration_failed(
    context: RunContext,
    eval_rejections: list[dict],
    config: dict[str, object],
) -> Path:
    last_rejection = eval_rejections[-1] if eval_rejections else {}
    last_errors = last_rejection.get("errors", [])
    failure_counts: dict[str, int] = {}
    for rejection in eval_rejections:
        for error in rejection.get("errors", []):
            category = categorize_failure(error)
            failure_counts[category] = failure_counts.get(category, 0) + 1

    payload = {
        "brief_hash": context.brief_hash,
        "run_id": context.run_id,
        "failed_at": now_iso(),
        "terminal_reason": "max_iteration_exceeded",
        "last_iteration": context.iteration,
        "failure_counts_by_category": failure_counts,
        "last_failures": [
            {
                "category": categorize_failure(error),
                "rule": failure_rule(error),
                "severity": "high",
                "retryable": False,
                "message": error,
            }
            for error in last_errors
        ],
        "lineage": {
            "input": relative_to_run(context.copied_input_path, context.run_dir),
            "last_draft": relative_to_run(context.draft_path, context.run_dir),
            "last_critique": relative_to_run(context.critique_path, context.run_dir),
            "last_eval": relative_to_run(context.eval_path, context.run_dir),
        },
        "iteration_rejections": eval_rejections,
        "config": config,
        "next_actions": [
            "원본 brief에 구체적 사례와 제약을 보강한다",
            "반복 실패의 주된 category를 보고 rubric threshold 또는 stage prompt를 조정한다",
        ],
    }
    write_json(context.failed_path, payload, overwrite=True)
    return context.failed_path


def categorize_failure(error: str) -> str:
    if error.startswith("schema "):
        return "schema_error"
    if "must not include" in error:
        return "role_boundary_violation"
    if error.startswith("min_total") or error.startswith("min_axis"):
        return "quality_reject"
    return "contract_error"


def failure_rule(error: str) -> str:
    if ":" in error:
        return error.split(":", 1)[0]
    return error


def run(args: argparse.Namespace) -> dict:
    progress = ProgressReporter()
    pipeline_started_at = time.perf_counter()
    stage = "input_validate"
    input_path = args.input.resolve()
    input_result = validate_file(input_path, artifact="input")
    progress.validation(stage, input_result)
    ensure_pass(input_result)

    input_data = load_json(input_path)
    brief_hash = input_data["brief_hash"]
    start_iteration = int(args.iteration)
    if args.max_iterations < start_iteration:
        raise ValueError("--max-iterations must be greater than or equal to --iteration")
    rubric_path = args.rubric.resolve()
    rubric = load_rubric(rubric_path)

    root_context = RunContext.create(
        brief_hash=brief_hash,
        iteration=args.iteration,
        runs_dir=args.runs_dir,
    )
    lineage = {
        "input": str(root_context.copied_input_path),
    }
    config = {
        "codex_bin": args.codex_bin,
        "codex_access": "dangerously-bypass-approvals-and-sandbox",
        "agent_models": resolve_agent_models(args),
        "iteration": args.iteration,
        "max_iterations": args.max_iterations,
        "timeout_seconds": args.timeout_seconds,
        "rubric_path": str(rubric_path),
        "rubric": rubric,
    }
    eval_rejections: list[dict] = []
    last_refine_request_lineage: str | None = None
    progress.line(
        f"run start brief={brief_hash} iteration={args.iteration} max_iterations={args.max_iterations} "
        f"rubric={rubric.get('name', rubric_path.name)} run_id={root_context.run_id}"
    )

    try:
        stage = "prepare"
        copy_input(input_path, root_context.copied_input_path, overwrite=args.overwrite)

        for iteration_number in range(start_iteration, args.max_iterations + 1):
            iteration = f"{iteration_number:03d}"
            iteration_label = f"iter {iteration}/{args.max_iterations:03d}"
            progress.line(f"{iteration_label} start")
            context = RunContext.create(brief_hash=brief_hash, iteration=iteration, runs_dir=args.runs_dir)
            context.iter_dir.mkdir(parents=True, exist_ok=True)
            lineage.update(
                {
                    "draft": str(context.draft_path),
                    "critique": str(context.critique_path),
                    "eval": str(context.eval_path),
                }
            )

            if iteration_number == start_iteration:
                with tempfile.TemporaryDirectory(prefix="writing-harness-gen-") as temp_dir:
                    temp_gen_output_path = Path(temp_dir) / "gen-output.json"

                    stage = f"iter_{iteration}_gen"
                    with progress.step(
                        f"{iteration_label} gen model={display_model(config['agent_models'][AGENT_GEN])}",
                        live=True,
                    ):
                        token_usage = generate(
                            input_path=root_context.copied_input_path,
                            output_path=temp_gen_output_path,
                            codex_bin=args.codex_bin,
                            model=config["agent_models"][AGENT_GEN],
                            timeout_seconds=args.timeout_seconds,
                        )

                    stage = f"iter_{iteration}_gen_validate"
                    gen_result = validate_file(temp_gen_output_path, artifact="gen_output")
                    progress.validation(f"{iteration_label} gen_output_validate", gen_result)
                    ensure_pass(gen_result)
                    gen_output = load_json(temp_gen_output_path)

                stage = f"iter_{iteration}_draft_write"
                draft = build_draft(
                    input_data=input_data,
                    stage_output=gen_output,
                    iteration=iteration,
                    model_name=display_model(config["agent_models"][AGENT_GEN]),
                    token_usage=token_usage,
                    source_stage=AGENT_GEN,
                )
                write_json(context.draft_path, draft, overwrite=args.overwrite)
            elif not context.draft_path.exists():
                raise FileNotFoundError(f"expected refined draft for iteration {iteration}: {context.draft_path}")

            stage = f"iter_{iteration}_draft_validate"
            draft_result = validate_file(
                context.draft_path,
                artifact="draft",
                expected_brief_hash=brief_hash,
                expected_iteration=iteration,
            )
            progress.validation(f"{iteration_label} draft_validate", draft_result)
            ensure_pass(draft_result, context.draft_validation_path)
            draft = load_json(context.draft_path)

            with tempfile.TemporaryDirectory(prefix="writing-harness-critique-") as temp_dir:
                temp_critique_path = Path(temp_dir) / "critique.json"

                stage = f"iter_{iteration}_critique"
                with progress.step(
                    f"{iteration_label} critique model={display_model(config['agent_models'][AGENT_CRITIQUE])}",
                    live=True,
                ):
                    token_usage = critique(
                        input_path=root_context.copied_input_path,
                        draft_path=context.draft_path,
                        output_path=temp_critique_path,
                        codex_bin=args.codex_bin,
                        model=config["agent_models"][AGENT_CRITIQUE],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"iter_{iteration}_critique_output_validate"
                critique_output_result = validate_file(temp_critique_path, artifact="critique_output")
                progress.validation(f"{iteration_label} critique_output_validate", critique_output_result)
                ensure_pass(critique_output_result)
                critique_output = load_json(temp_critique_path)

            stage = f"iter_{iteration}_critique_write"
            critique_artifact = build_critique(
                critique_output=critique_output,
                iteration=iteration,
                model_name=display_model(config["agent_models"][AGENT_CRITIQUE]),
                token_usage=token_usage,
            )
            write_json(context.critique_path, critique_artifact, overwrite=args.overwrite)

            stage = f"iter_{iteration}_critique_validate"
            critique_result = validate_file(
                context.critique_path,
                artifact="critique",
                expected_brief_hash=brief_hash,
                expected_iteration=iteration,
            )
            progress.validation(f"{iteration_label} critique_validate", critique_result)
            ensure_pass(critique_result, context.critique_validation_path)

            with tempfile.TemporaryDirectory(prefix="writing-harness-eval-") as temp_dir:
                temp_eval_path = Path(temp_dir) / "eval.json"

                stage = f"iter_{iteration}_eval"
                with progress.step(
                    f"{iteration_label} eval model={display_model(config['agent_models'][AGENT_EVAL])}",
                    live=True,
                ):
                    token_usage = evaluate(
                        input_path=root_context.copied_input_path,
                        draft_path=context.draft_path,
                        rubric=rubric,
                        output_path=temp_eval_path,
                        codex_bin=args.codex_bin,
                        model=config["agent_models"][AGENT_EVAL],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"iter_{iteration}_eval_output_validate"
                eval_output_result = validate_file(temp_eval_path, artifact="eval_output")
                progress.validation(f"{iteration_label} eval_output_validate", eval_output_result)
                ensure_pass(eval_output_result)
                eval_output = load_json(temp_eval_path)

            stage = f"iter_{iteration}_eval_write"
            eval_artifact = build_eval(
                eval_output=eval_output,
                iteration=iteration,
                model_name=display_model(config["agent_models"][AGENT_EVAL]),
                token_usage=token_usage,
            )
            write_json(context.eval_path, eval_artifact, overwrite=args.overwrite)

            stage = f"iter_{iteration}_eval_validate"
            eval_result = validate_file(
                context.eval_path,
                artifact="eval",
                expected_brief_hash=brief_hash,
                expected_iteration=iteration,
                rubric=rubric,
            )
            eval_summary = format_eval_scores(eval_artifact, rubric)
            if eval_result["status"] == "PASS":
                progress.line(f"{iteration_label} eval PASS {eval_summary}")
            else:
                error_summary = summarize_errors(eval_result.get("errors", []))
                errors = f" errors={error_summary}" if error_summary else ""
                progress.line(f"{iteration_label} eval {eval_result['status']} {eval_summary}{errors}")
            if eval_result["status"] == "PASS":
                stage = f"iter_{iteration}_final_write"
                final_artifact = build_final(
                    context=context,
                    input_data=input_data,
                    draft=draft,
                    eval_artifact=eval_artifact,
                    eval_result=eval_result,
                    rubric=rubric,
                    refine_request_lineage=last_refine_request_lineage,
                )
                write_json(context.final_path, final_artifact, overwrite=args.overwrite)

                stage = f"iter_{iteration}_final_validate"
                final_result = validate_file(
                    context.final_path,
                    artifact="final",
                    expected_brief_hash=brief_hash,
                )
                progress.validation(f"{iteration_label} final_validate", final_result)
                ensure_pass(final_result)
                progress.line(
                    f"run PASS iteration={iteration} total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
                )
                return {
                    "status": "PASS",
                    "run_id": context.run_id,
                    "input": str(root_context.copied_input_path),
                    "draft": str(context.draft_path),
                    "critique": str(context.critique_path),
                    "eval": str(context.eval_path),
                    "final": str(context.final_path),
                    "iteration": iteration,
                }

            write_result(eval_result, context.eval_validation_path)
            eval_rejections.append(
                {
                    "iteration": iteration,
                    "validation": str(context.eval_validation_path),
                    "errors": eval_result.get("errors", []),
                }
            )

            if iteration_number >= args.max_iterations:
                stage = f"iter_{iteration}_max_iteration_exceeded"
                with progress.step(f"{iteration_label} max_iteration_exceeded"):
                    failed_path = write_max_iteration_failed(
                        context=context,
                        eval_rejections=eval_rejections,
                        config=config,
                    )
                progress.line(
                    "run FAILED terminal_reason=max_iteration_exceeded "
                    f"last_iteration={iteration} total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
                )
                return {
                    "status": "FAILED",
                    "run_id": context.run_id,
                    "failed": str(failed_path),
                    "terminal_reason": "max_iteration_exceeded",
                    "last_iteration": iteration,
                }

            to_iteration = next_iteration(iteration)
            with progress.step(f"iter {iteration}->{to_iteration} refine_request"):
                refine_request = build_refine_request(
                    input_data=input_data,
                    draft=draft,
                    critique_artifact=critique_artifact,
                    eval_artifact=eval_artifact,
                    eval_result=eval_result,
                    rubric=rubric,
                    to_iteration=to_iteration,
                )
            last_refine_request_lineage = f"memory:{iteration}->{to_iteration}"
            next_context = RunContext.create(brief_hash=brief_hash, iteration=to_iteration, runs_dir=args.runs_dir)
            next_context.iter_dir.mkdir(parents=True, exist_ok=True)

            with tempfile.TemporaryDirectory(prefix="writing-harness-refine-") as temp_dir:
                temp_refine_output_path = Path(temp_dir) / "refine-output.json"

                stage = f"iter_{iteration}_refine_to_{to_iteration}"
                with progress.step(
                    f"iter {iteration}->{to_iteration} refine model={display_model(config['agent_models'][AGENT_REFINE])}",
                    live=True,
                ):
                    token_usage = refine(
                        input_path=root_context.copied_input_path,
                        draft_path=context.draft_path,
                        critique_path=context.critique_path,
                        refine_request=refine_request,
                        output_path=temp_refine_output_path,
                        codex_bin=args.codex_bin,
                        model=config["agent_models"][AGENT_REFINE],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"iter_{iteration}_refine_output_validate"
                refine_result = validate_file(temp_refine_output_path, artifact="refine_output")
                progress.validation(f"iter {iteration}->{to_iteration} refine_output_validate", refine_result)
                ensure_pass(refine_result)
                refine_output = load_json(temp_refine_output_path)

            stage = f"iter_{to_iteration}_draft_write"
            refined_draft = build_draft(
                input_data=input_data,
                stage_output=refine_output,
                iteration=to_iteration,
                model_name=display_model(config["agent_models"][AGENT_REFINE]),
                token_usage=token_usage,
                source_stage=AGENT_REFINE,
            )
            write_json(next_context.draft_path, refined_draft, overwrite=args.overwrite)
    except Exception as exc:
        progress.line(
            f"run ERROR stage={stage} total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)} "
            f"error={type(exc).__name__}"
        )
        failed_path = write_failed(root_context.run_dir, brief_hash, root_context.run_id, stage, exc, lineage, config)
        progress.line(f"run failed artifact={failed_path}")
        raise RuntimeError(f"pipeline failed at {stage}; wrote {failed_path}") from exc

    raise RuntimeError("pipeline ended without PASS or FAILED status")


def resolve_agent_models(args: argparse.Namespace) -> dict[str, str | None]:
    models = AGENT_MODELS.copy()
    if args.model:
        models[AGENT_GEN] = args.model
    if args.gen_model:
        models[AGENT_GEN] = args.gen_model
    if args.critique_model:
        models[AGENT_CRITIQUE] = args.critique_model
    if args.eval_model:
        models[AGENT_EVAL] = args.eval_model
    if args.refine_model:
        models[AGENT_REFINE] = args.refine_model
    return models


def main() -> int:
    parser = argparse.ArgumentParser(description="Run pipeline: input -> gen/refine -> critique -> eval -> final/failed.")
    parser.add_argument("input", type=Path, help="Path to an input JSON file matching input.schema.json.")
    parser.add_argument("--codex-bin", default="codex")
    parser.add_argument("--model", help="Alias for --gen-model in the current MVP.")
    parser.add_argument("--gen-model", help="Model for the Gen agent. Defaults to the official Codex recommended model.")
    parser.add_argument("--critique-model", help="Model for the Critique agent. Defaults to the Codex CLI default model.")
    parser.add_argument("--eval-model", help="Model for the Eval agent. Defaults to the Codex CLI default model.")
    parser.add_argument("--refine-model", help="Model for the Refine agent. Defaults to the Codex CLI default model.")
    parser.add_argument("--runs-dir", type=Path, default=RUNS_DIR)
    parser.add_argument("--rubric", type=Path, default=RUBRIC_PATH)
    parser.add_argument("--iteration", default="001")
    parser.add_argument("--max-iterations", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing artifacts for the same run.")
    args = parser.parse_args()

    if len(args.iteration) != 3 or not args.iteration.isdigit():
        raise ValueError("--iteration must use a 3-digit value such as 001")
    if args.max_iterations < 1:
        raise ValueError("--max-iterations must be at least 1")

    result = run(args)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
