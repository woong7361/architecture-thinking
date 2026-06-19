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
from typing import Any

sys.dont_write_bytecode = True

from stages.analyze import analyze
from stages.critique import critique
from stages.evaluate import evaluate
from stages.keyword_extract import keyword_extract
from stages.report import report
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

STAGE_KEYWORD_EXTRACT = "keyword_extract"
STAGE_ANALYZE = "analyze"
STAGE_EVALUATE = "evaluate"
STAGE_CRITIQUE = "critique"
STAGE_REPORT = "report"

AGENT_MODELS = {
    STAGE_KEYWORD_EXTRACT: MODEL_CODEX_DEFAULT,
    STAGE_ANALYZE: MODEL_CODEX_DEFAULT,
    STAGE_EVALUATE: MODEL_CODEX_DEFAULT,
    STAGE_CRITIQUE: MODEL_CODEX_DEFAULT,
    STAGE_REPORT: MODEL_CODEX_DEFAULT,
}


def format_duration(seconds: float) -> str:
    return f"{seconds:.2f}s"


def format_running_duration(seconds: float) -> str:
    return f"{int(seconds)}s"


def display_model(model: str | None) -> str:
    return model or "codex-cli-default"


def summarize_errors(errors: list[object], limit: int = 3) -> str:
    if not errors:
        return ""
    visible_errors = [str(error) for error in errors[:limit]]
    if len(errors) > limit:
        visible_errors.append(f"... +{len(errors) - limit} more")
    return "; ".join(visible_errors)


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

    def validation(self, label: str, result: dict[str, Any]) -> None:
        if result["status"] == "PASS":
            return
        error_summary = summarize_errors(result.get("errors", []))
        errors = f" errors={error_summary}" if error_summary else ""
        self.line(f"{label} {result['status']}{errors}")

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
    runs_dir: Path
    run_id: str

    @classmethod
    def create(cls, brief_hash: str, runs_dir: Path) -> "RunContext":
        today = datetime.now(KST).date().isoformat()
        return cls(
            brief_hash=brief_hash,
            runs_dir=runs_dir.resolve(),
            run_id=f"{today}_{brief_hash}",
        )

    @property
    def run_dir(self) -> Path:
        return self.runs_dir / self.run_id

    @property
    def copied_input_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_input.json"

    def keyword_path(self, batch_no: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_batch-{batch_no}_keywords.json"

    def keyword_validation_path(self, batch_no: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_batch-{batch_no}_keywords.validation.json"

    @property
    def analysis_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_analysis.json"

    @property
    def analysis_validation_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_analysis.validation.json"

    def analysis_attempt_path(self, attempt: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_analysis-attempt-{attempt}.json"

    def analysis_attempt_validation_path(self, attempt: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_analysis-attempt-{attempt}.validation.json"

    @property
    def eval_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_eval.json"

    @property
    def eval_validation_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_eval.validation.json"

    def eval_attempt_path(self, attempt: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_eval-attempt-{attempt}.json"

    def eval_attempt_validation_path(self, attempt: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_eval-attempt-{attempt}.validation.json"

    def critique_attempt_path(self, attempt: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_critique-attempt-{attempt}.json"

    def critique_attempt_validation_path(self, attempt: str) -> Path:
        return self.run_dir / f"{self.brief_hash}_critique-attempt-{attempt}.validation.json"

    @property
    def report_markdown_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_report.md"

    @property
    def failed_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_failed.json"


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as file:
        data = json.load(file)
    if not isinstance(data, dict):
        raise ValueError(f"expected JSON object: {path}")
    return data


def write_json(path: Path, data: dict[str, Any], overwrite: bool = False) -> None:
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


def ensure_pass(result: dict[str, Any], result_path: Path | None = None) -> None:
    if result["status"] == "PASS":
        return
    if result_path:
        write_result(result, result_path)
    raise RuntimeError(json.dumps(result, ensure_ascii=False, indent=2))


def now_iso() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def relative_to_run(path: Path, run_dir: Path) -> str:
    try:
        return path.relative_to(run_dir).as_posix()
    except ValueError:
        return str(path)


def batched(items: list[dict[str, Any]], batch_size: int) -> list[list[dict[str, Any]]]:
    return [items[index : index + batch_size] for index in range(0, len(items), batch_size)]


def batch_no(index: int) -> str:
    return f"{index:03d}"


def build_keyword_batch_input(input_data: dict[str, Any], postings: list[dict[str, Any]], current_batch_no: str) -> dict[str, Any]:
    return {
        "brief_hash": input_data["brief_hash"],
        "batch_no": current_batch_no,
        "analysis_goal": input_data["analysis_goal"],
        "postings": postings,
    }


def build_analysis_input(
    input_data: dict[str, Any],
    keyword_artifacts: list[dict[str, Any]],
    keyword_files: list[str],
    attempt: str,
    previous_analysis: dict[str, Any] | None = None,
    previous_critique: dict[str, Any] | None = None,
) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "brief_hash": input_data["brief_hash"],
        "analysis_goal": input_data["analysis_goal"],
        "attempt": attempt,
        "keyword_files": keyword_files,
        "keyword_artifacts": keyword_artifacts,
    }
    if previous_analysis and previous_critique:
        payload["reanalyze_context"] = {
            "previous_analysis": previous_analysis,
            "critique": previous_critique,
            "critic": previous_critique.get("critic", {}),
            "axis_critiques": previous_critique.get("axis_critiques", []),
            "signal_critiques": previous_critique.get("signal_critiques", []),
        }
    return payload


def build_eval_input(
    input_data: dict[str, Any],
    analysis_data: dict[str, Any],
    keyword_artifacts: list[dict[str, Any]],
    keyword_files: list[str],
    rubric: dict[str, Any],
    attempt: str,
) -> dict[str, Any]:
    return {
        "brief_hash": input_data["brief_hash"],
        "analysis_goal": input_data["analysis_goal"],
        "attempt": attempt,
        "keyword_files": keyword_files,
        "keyword_artifacts": keyword_artifacts,
        "analysis": analysis_data,
        "rubric": rubric,
    }


def build_report_input(
    input_data: dict[str, Any],
    analysis_data: dict[str, Any],
    eval_data: dict[str, Any],
    threshold_result: dict[str, Any],
    keyword_files: list[str],
    analysis_file: str,
    eval_file: str,
) -> dict[str, Any]:
    return {
        "brief_hash": input_data["brief_hash"],
        "analysis_goal": input_data["analysis_goal"],
        "keyword_files": keyword_files,
        "analysis_file": analysis_file,
        "eval_file": eval_file,
        "analysis": analysis_data,
        "eval": eval_data,
        "threshold_result": threshold_result,
    }


def build_critique_input(
    input_data: dict[str, Any],
    analysis_data: dict[str, Any],
    eval_data: dict[str, Any],
    threshold_result: dict[str, Any],
    keyword_artifacts: list[dict[str, Any]],
    keyword_files: list[str],
    rubric: dict[str, Any],
    attempt: str,
) -> dict[str, Any]:
    return {
        "brief_hash": input_data["brief_hash"],
        "analysis_goal": input_data["analysis_goal"],
        "attempt": attempt,
        "keyword_files": keyword_files,
        "keyword_artifacts": keyword_artifacts,
        "analysis": analysis_data,
        "eval": eval_data,
        "threshold_result": threshold_result,
        "rubric": rubric,
    }


def validate_report_markdown(path: Path) -> list[str]:
    if not path.exists():
        return [f"report markdown does not exist: {path}"]
    text = path.read_text(encoding="utf-8").strip()
    errors = []
    if not text:
        errors.append("report markdown must not be empty")
    if not text.startswith("# "):
        errors.append("report markdown must start with a level-1 heading")
    if "```" in text:
        errors.append("report markdown must not wrap the whole output in a code block")
    return errors


def validate_analysis_references(analysis_data: dict[str, Any], keyword_artifacts: list[dict[str, Any]]) -> list[str]:
    item_ids = set()
    posting_ids = set()
    for artifact in keyword_artifacts:
        for posting in artifact.get("postings", []):
            if not isinstance(posting, dict):
                continue
            posting_id = posting.get("posting_id")
            if isinstance(posting_id, str):
                posting_ids.add(posting_id)
            for item in posting.get("items", []):
                if isinstance(item, dict) and isinstance(item.get("item_id"), str):
                    item_ids.add(item["item_id"])

    errors = []
    for signal_index, signal in enumerate(analysis_data.get("signals", [])):
        if not isinstance(signal, dict):
            continue
        for item_id in signal.get("source_item_ids", []):
            if item_id not in item_ids:
                errors.append(f"signals[{signal_index}].source_item_ids references unknown item_id: {item_id}")
        evidence_distribution = signal.get("evidence_distribution", {})
        if not isinstance(evidence_distribution, dict):
            continue
        for posting_id in evidence_distribution.get("posting_ids", []):
            if posting_id not in posting_ids:
                errors.append(
                    f"signals[{signal_index}].evidence_distribution.posting_ids references unknown posting_id: {posting_id}"
                )
    return errors


def calculate_threshold_result(eval_data: dict[str, Any], rubric: dict[str, Any]) -> dict[str, Any]:
    axes = rubric.get("axes", {})
    thresholds = rubric.get("thresholds", {})
    min_total = thresholds.get("min_total", 0)
    min_axis = thresholds.get("min_axis", {})
    scores = {
        axis_score["axis"]: axis_score["score"]
        for axis_score in eval_data.get("axis_scores", [])
        if isinstance(axis_score, dict)
        and isinstance(axis_score.get("axis"), str)
        and isinstance(axis_score.get("score"), (int, float))
    }
    if isinstance(axes, dict):
        weights = {
            axis: config.get("weight", 0)
            for axis, config in axes.items()
            if isinstance(config, dict)
        }
    elif isinstance(axes, list):
        weights = {
            axis.get("id"): axis.get("weight", 0)
            for axis in axes
            if isinstance(axis, dict) and isinstance(axis.get("id"), str)
        }
    else:
        weights = {}
    weighted_total = sum(scores.get(axis, 0) * weights.get(axis, 0) for axis in weights)
    axis_failures = [
        {
            "axis": axis,
            "score": scores.get(axis, 0),
            "minimum": minimum,
        }
        for axis, minimum in min_axis.items()
        if isinstance(minimum, (int, float)) and scores.get(axis, 0) < minimum
    ]
    total_passed = not isinstance(min_total, (int, float)) or weighted_total >= min_total
    passed = total_passed and not axis_failures
    return {
        "passed": passed,
        "weighted_total": round(weighted_total, 3),
        "min_total": min_total,
        "total_passed": total_passed,
        "axis_failures": axis_failures,
    }


def validate_critique_references(critique_data: dict[str, Any], analysis_data: dict[str, Any]) -> list[str]:
    signal_ids = {
        signal["signal_id"]
        for signal in analysis_data.get("signals", [])
        if isinstance(signal, dict) and isinstance(signal.get("signal_id"), str)
    }
    errors = []
    for index, critique_item in enumerate(critique_data.get("signal_critiques", [])):
        if not isinstance(critique_item, dict):
            continue
        signal_id = critique_item.get("signal_id")
        if signal_id not in signal_ids:
            errors.append(f"signal_critiques[{index}].signal_id references unknown signal_id: {signal_id}")
    for index, weakness in enumerate(critique_data.get("critic", {}).get("weaknesses", [])):
        if not isinstance(weakness, dict):
            continue
        if weakness.get("target_type") == "signal" and weakness.get("target_id") not in signal_ids:
            errors.append(f"critic.weaknesses[{index}].target_id references unknown signal_id: {weakness.get('target_id')}")
    return errors


def load_rubric(path: Path) -> dict[str, Any]:
    data = load_json(path)
    if not isinstance(data.get("axes"), dict):
        raise ValueError(f"expected rubric axes object: {path}")
    return data


def attempt_no(index: int) -> str:
    return f"{index:03d}"


def write_failed(
    context: RunContext,
    stage: str,
    error: Exception,
    lineage: dict[str, object],
    config: dict[str, object],
) -> Path:
    payload = {
        "brief_hash": context.brief_hash,
        "run_id": context.run_id,
        "failed_at": now_iso(),
        "stage": stage,
        "error_type": type(error).__name__,
        "message": str(error),
        "config": config,
        "lineage": lineage,
    }
    write_json(context.failed_path, payload, overwrite=True)
    return context.failed_path


def run(args: argparse.Namespace) -> dict[str, Any]:
    progress = ProgressReporter()
    pipeline_started_at = time.perf_counter()
    stage = "input_validate"
    input_path = args.input.resolve()

    input_result = validate_file(input_path, artifact="input")
    progress.validation(stage, input_result)
    ensure_pass(input_result)

    input_data = load_json(input_path)
    brief_hash = input_data["brief_hash"]
    context = RunContext.create(brief_hash=brief_hash, runs_dir=args.runs_dir)
    models = resolve_agent_models(args)
    batch_size = args.batch_size or input_data["batch_size"]
    rubric = load_rubric(args.rubric.resolve())
    keyword_batches: list[str] = []
    keyword_artifacts: list[dict[str, Any]] = []
    analysis_attempts: list[str] = []
    eval_attempts: list[str] = []
    critique_attempts: list[str] = []
    lineage: dict[str, object] = {
        "input": str(context.copied_input_path),
        "keyword_batches": keyword_batches,
        "analysis_attempts": analysis_attempts,
        "eval_attempts": eval_attempts,
        "critique_attempts": critique_attempts,
    }
    config = {
        "codex_bin": args.codex_bin,
        "codex_access": "dangerously-bypass-approvals-and-sandbox",
        "agent_models": models,
        "batch_size": batch_size,
        "max_analysis_attempts": args.max_analysis_attempts,
        "rubric_path": str(args.rubric.resolve()),
        "timeout_seconds": args.timeout_seconds,
        "until_stage": args.until_stage,
    }

    progress.line(
        f"run start brief={brief_hash} postings={len(input_data['postings'])} "
        f"batch_size={batch_size} run_id={context.run_id}"
    )

    try:
        stage = "prepare"
        copy_input(input_path, context.copied_input_path, overwrite=args.overwrite)

        posting_batches = batched(input_data["postings"], batch_size)
        for index, postings in enumerate(posting_batches, start=1):
            current_batch_no = batch_no(index)
            output_path = context.keyword_path(current_batch_no)
            validation_path = context.keyword_validation_path(current_batch_no)
            batch_input = build_keyword_batch_input(input_data, postings, current_batch_no)

            with tempfile.TemporaryDirectory(prefix="recruiting-keyword-extract-") as temp_dir:
                temp_output_path = Path(temp_dir) / "keyword-extraction.json"
                stage = f"keyword_extract_batch_{current_batch_no}"
                with progress.step(
                    f"batch {current_batch_no}/{len(posting_batches):03d} keyword_extract "
                    f"model={display_model(models[STAGE_KEYWORD_EXTRACT])}",
                    live=True,
                ):
                    keyword_extract(
                        batch_input=batch_input,
                        output_path=temp_output_path,
                        codex_bin=args.codex_bin,
                        model=models[STAGE_KEYWORD_EXTRACT],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"keyword_extract_batch_{current_batch_no}_validate"
                keyword_result = validate_file(
                    temp_output_path,
                    artifact="keyword_extraction",
                    expected_brief_hash=brief_hash,
                    expected_batch_no=current_batch_no,
                )
                progress.validation(f"batch {current_batch_no} keyword_validate", keyword_result)
                ensure_pass(keyword_result, validation_path)
                keyword_output = load_json(temp_output_path)

            stage = f"keyword_extract_batch_{current_batch_no}_write"
            write_json(output_path, keyword_output, overwrite=args.overwrite)
            keyword_batches.append(relative_to_run(output_path, context.run_dir))
            keyword_artifacts.append(keyword_output)

        if args.until_stage == STAGE_KEYWORD_EXTRACT:
            progress.line(
                f"run PASS stage={STAGE_KEYWORD_EXTRACT} batches={len(keyword_batches)} "
                f"total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
            )
            return {
                "status": "PASS",
                "run_id": context.run_id,
                "input": str(context.copied_input_path),
                "keyword_batches": [str(context.run_dir / path) for path in keyword_batches],
                "stage": STAGE_KEYWORD_EXTRACT,
            }

        previous_analysis: dict[str, Any] | None = None
        previous_critique: dict[str, Any] | None = None
        for attempt_index in range(1, args.max_analysis_attempts + 1):
            attempt = attempt_no(attempt_index)
            analysis_input = build_analysis_input(
                input_data=input_data,
                keyword_artifacts=keyword_artifacts,
                keyword_files=keyword_batches,
                attempt=attempt,
                previous_analysis=previous_analysis,
                previous_critique=previous_critique,
            )
            with tempfile.TemporaryDirectory(prefix="recruiting-analyze-") as temp_dir:
                temp_analysis_path = Path(temp_dir) / "analysis.json"
                stage = f"analyze_attempt_{attempt}"
                with progress.step(
                    f"analyze attempt={attempt}/{args.max_analysis_attempts:03d} "
                    f"keyword_batches={len(keyword_batches)} model={display_model(models[STAGE_ANALYZE])}",
                    live=True,
                ):
                    analyze(
                        analysis_input=analysis_input,
                        output_path=temp_analysis_path,
                        codex_bin=args.codex_bin,
                        model=models[STAGE_ANALYZE],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"analyze_attempt_{attempt}_validate"
                analysis_result = validate_file(
                    temp_analysis_path,
                    artifact="analysis",
                    expected_brief_hash=brief_hash,
                )
                progress.validation(f"analysis attempt={attempt} validate", analysis_result)
                ensure_pass(analysis_result, context.analysis_attempt_validation_path(attempt))
                analysis_output = load_json(temp_analysis_path)

                reference_errors = validate_analysis_references(analysis_output, keyword_artifacts)
                if reference_errors:
                    reference_result = {
                        "artifact": "analysis",
                        "checked_file": str(temp_analysis_path),
                        "status": "REJECT",
                        "errors": reference_errors,
                    }
                    progress.validation(f"analysis attempt={attempt} reference_validate", reference_result)
                    ensure_pass(reference_result, context.analysis_attempt_validation_path(attempt))

            stage = f"analysis_attempt_{attempt}_write"
            analysis_attempt_path = context.analysis_attempt_path(attempt)
            write_json(analysis_attempt_path, analysis_output, overwrite=args.overwrite)
            analysis_attempts.append(relative_to_run(analysis_attempt_path, context.run_dir))

            if args.until_stage == STAGE_ANALYZE:
                write_json(context.analysis_path, analysis_output, overwrite=args.overwrite)
                lineage["analysis"] = str(context.analysis_path)
                progress.line(
                    f"run PASS stage={STAGE_ANALYZE} batches={len(keyword_batches)} "
                    f"total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
                )
                return {
                    "status": "PASS",
                    "run_id": context.run_id,
                    "input": str(context.copied_input_path),
                    "keyword_batches": [str(context.run_dir / path) for path in keyword_batches],
                    "analysis": str(context.analysis_path),
                    "stage": STAGE_ANALYZE,
                }

            eval_input = build_eval_input(
                input_data=input_data,
                analysis_data=analysis_output,
                keyword_artifacts=keyword_artifacts,
                keyword_files=keyword_batches,
                rubric=rubric,
                attempt=attempt,
            )
            with tempfile.TemporaryDirectory(prefix="recruiting-evaluate-") as temp_dir:
                temp_eval_path = Path(temp_dir) / "eval.json"
                stage = f"evaluate_attempt_{attempt}"
                with progress.step(
                    f"evaluate attempt={attempt}/{args.max_analysis_attempts:03d} "
                    f"model={display_model(models[STAGE_EVALUATE])}",
                    live=True,
                ):
                    evaluate(
                        eval_input=eval_input,
                        output_path=temp_eval_path,
                        codex_bin=args.codex_bin,
                        model=models[STAGE_EVALUATE],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"evaluate_attempt_{attempt}_validate"
                eval_result = validate_file(
                    temp_eval_path,
                    artifact="eval",
                    expected_brief_hash=brief_hash,
                )
                progress.validation(f"eval attempt={attempt} validate", eval_result)
                ensure_pass(eval_result, context.eval_attempt_validation_path(attempt))
                eval_output = load_json(temp_eval_path)

            stage = f"eval_attempt_{attempt}_write"
            eval_attempt_path = context.eval_attempt_path(attempt)
            write_json(eval_attempt_path, eval_output, overwrite=args.overwrite)
            eval_attempts.append(relative_to_run(eval_attempt_path, context.run_dir))
            threshold_result = calculate_threshold_result(eval_output, rubric)
            progress.line(
                f"eval attempt={attempt} weighted_total={threshold_result['weighted_total']} "
                f"min_total={threshold_result['min_total']} passed={threshold_result['passed']}"
            )
            if args.until_stage == STAGE_EVALUATE:
                write_json(context.analysis_path, analysis_output, overwrite=args.overwrite)
                write_json(context.eval_path, eval_output, overwrite=args.overwrite)
                lineage["analysis"] = str(context.analysis_path)
                lineage["eval"] = str(context.eval_path)
                progress.line(
                    f"run PASS stage={STAGE_EVALUATE} attempts={attempt} batches={len(keyword_batches)} "
                    f"total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
                )
                return {
                    "status": "PASS",
                    "run_id": context.run_id,
                    "input": str(context.copied_input_path),
                    "keyword_batches": [str(context.run_dir / path) for path in keyword_batches],
                    "analysis": str(context.analysis_path),
                    "eval": str(context.eval_path),
                    "stage": STAGE_EVALUATE,
                    "analysis_attempts": [str(context.run_dir / path) for path in analysis_attempts],
                    "eval_attempts": [str(context.run_dir / path) for path in eval_attempts],
                    "critique_attempts": [str(context.run_dir / path) for path in critique_attempts],
                    "threshold_result": threshold_result,
                }

            if threshold_result["passed"]:
                stage = "analysis_eval_canonical_write"
                write_json(context.analysis_path, analysis_output, overwrite=args.overwrite)
                write_json(context.eval_path, eval_output, overwrite=args.overwrite)
                lineage["analysis"] = str(context.analysis_path)
                lineage["eval"] = str(context.eval_path)

                report_input = build_report_input(
                    input_data=input_data,
                    analysis_data=analysis_output,
                    eval_data=eval_output,
                    threshold_result=threshold_result,
                    keyword_files=keyword_batches,
                    analysis_file=relative_to_run(context.analysis_path, context.run_dir),
                    eval_file=relative_to_run(context.eval_path, context.run_dir),
                )
                stage = "report"
                if context.report_markdown_path.exists() and not args.overwrite:
                    raise FileExistsError(f"refusing to overwrite existing file: {context.report_markdown_path}")
                with progress.step(
                    f"report markdown model={display_model(models[STAGE_REPORT])}",
                    live=True,
                ):
                    report(
                        report_input=report_input,
                        output_path=context.report_markdown_path,
                        codex_bin=args.codex_bin,
                        model=models[STAGE_REPORT],
                        timeout_seconds=args.timeout_seconds,
                    )
                report_errors = validate_report_markdown(context.report_markdown_path)
                if report_errors:
                    raise RuntimeError(json.dumps({"artifact": "report", "errors": report_errors}, ensure_ascii=False))
                lineage["report"] = str(context.report_markdown_path)
                progress.line(
                    f"run PASS stage={STAGE_REPORT} attempts={attempt} batches={len(keyword_batches)} "
                    f"total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
                )
                return {
                    "status": "PASS",
                    "run_id": context.run_id,
                    "input": str(context.copied_input_path),
                    "keyword_batches": [str(context.run_dir / path) for path in keyword_batches],
                    "analysis": str(context.analysis_path),
                    "eval": str(context.eval_path),
                    "report": str(context.report_markdown_path),
                    "stage": STAGE_REPORT,
                    "analysis_attempts": [str(context.run_dir / path) for path in analysis_attempts],
                    "eval_attempts": [str(context.run_dir / path) for path in eval_attempts],
                    "critique_attempts": [str(context.run_dir / path) for path in critique_attempts],
                    "threshold_result": threshold_result,
                }

            critique_input = build_critique_input(
                input_data=input_data,
                analysis_data=analysis_output,
                eval_data=eval_output,
                threshold_result=threshold_result,
                keyword_artifacts=keyword_artifacts,
                keyword_files=keyword_batches,
                rubric=rubric,
                attempt=attempt,
            )
            with tempfile.TemporaryDirectory(prefix="recruiting-critique-") as temp_dir:
                temp_critique_path = Path(temp_dir) / "critique.json"
                stage = f"critique_attempt_{attempt}"
                with progress.step(
                    f"critique attempt={attempt}/{args.max_analysis_attempts:03d} "
                    f"model={display_model(models[STAGE_CRITIQUE])}",
                    live=True,
                ):
                    critique(
                        critique_input=critique_input,
                        output_path=temp_critique_path,
                        codex_bin=args.codex_bin,
                        model=models[STAGE_CRITIQUE],
                        timeout_seconds=args.timeout_seconds,
                    )

                stage = f"critique_attempt_{attempt}_validate"
                critique_result = validate_file(
                    temp_critique_path,
                    artifact="critique",
                    expected_brief_hash=brief_hash,
                )
                progress.validation(f"critique attempt={attempt} validate", critique_result)
                ensure_pass(critique_result, context.critique_attempt_validation_path(attempt))
                critique_output = load_json(temp_critique_path)

                critique_reference_errors = validate_critique_references(critique_output, analysis_output)
                if critique_reference_errors:
                    reference_result = {
                        "artifact": "critique",
                        "checked_file": str(temp_critique_path),
                        "status": "REJECT",
                        "errors": critique_reference_errors,
                    }
                    progress.validation(f"critique attempt={attempt} reference_validate", reference_result)
                    ensure_pass(reference_result, context.critique_attempt_validation_path(attempt))

            stage = f"critique_attempt_{attempt}_write"
            critique_attempt_path = context.critique_attempt_path(attempt)
            write_json(critique_attempt_path, critique_output, overwrite=args.overwrite)
            critique_attempts.append(relative_to_run(critique_attempt_path, context.run_dir))
            progress.line(f"eval attempt={attempt} below threshold; critique written; reanalyze next")
            previous_analysis = analysis_output
            previous_critique = critique_output

        stage = "max_analysis_attempts_exceeded"
        raise RuntimeError(f"analysis did not pass evaluation after {args.max_analysis_attempts} attempts")
    except Exception as exc:
        progress.line(
            f"run ERROR stage={stage} total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)} "
            f"error={type(exc).__name__}"
        )
        failed_path = write_failed(context, stage, exc, lineage, config)
        progress.line(f"run failed artifact={failed_path}")
        raise RuntimeError(f"pipeline failed at {stage}; wrote {failed_path}") from exc


def resolve_agent_models(args: argparse.Namespace) -> dict[str, str | None]:
    models = AGENT_MODELS.copy()
    if args.model:
        models[STAGE_KEYWORD_EXTRACT] = args.model
        models[STAGE_ANALYZE] = args.model
        models[STAGE_EVALUATE] = args.model
        models[STAGE_CRITIQUE] = args.model
        models[STAGE_REPORT] = args.model
    if args.keyword_model:
        models[STAGE_KEYWORD_EXTRACT] = args.keyword_model
    if args.analyze_model:
        models[STAGE_ANALYZE] = args.analyze_model
    if args.eval_model:
        models[STAGE_EVALUATE] = args.eval_model
    if args.critique_model:
        models[STAGE_CRITIQUE] = args.critique_model
    if args.report_model:
        models[STAGE_REPORT] = args.report_model
    return models


def main() -> int:
    parser = argparse.ArgumentParser(description="Run recruiting harness pipeline.")
    parser.add_argument("input", type=Path, help="Path to an input JSON file matching input.schema.json.")
    parser.add_argument("--codex-bin", default="codex")
    parser.add_argument("--model", help="Model for all currently implemented agents.")
    parser.add_argument("--keyword-model", help="Model for the Keyword Extract agent.")
    parser.add_argument("--analyze-model", help="Model for the Frequency & Subtext Analyze agent.")
    parser.add_argument("--eval-model", help="Model for the Evaluate agent.")
    parser.add_argument("--critique-model", help="Model for the Critique agent.")
    parser.add_argument("--report-model", help="Model for the Markdown Report agent.")
    parser.add_argument("--runs-dir", type=Path, default=RUNS_DIR)
    parser.add_argument("--rubric", type=Path, default=RUBRIC_PATH)
    parser.add_argument("--batch-size", type=int, help="Override input.batch_size for this run.")
    parser.add_argument(
        "--until-stage",
        choices=[STAGE_KEYWORD_EXTRACT, STAGE_ANALYZE, STAGE_EVALUATE, STAGE_REPORT],
        default=STAGE_REPORT,
    )
    parser.add_argument("--max-analysis-attempts", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing artifacts for the same run.")
    args = parser.parse_args()

    if args.batch_size is not None and args.batch_size < 1:
        raise ValueError("--batch-size must be at least 1")
    if args.max_analysis_attempts < 1:
        raise ValueError("--max-analysis-attempts must be at least 1")

    result = run(args)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
