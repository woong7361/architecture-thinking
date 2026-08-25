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
from stages.scripts.context import load_banned_terms
from stages.scripts.llm_client import create_client
from validate import content_contract_errors, validate_file, write_result


PROJECT_DIR = Path(__file__).resolve().parent
RUNS_DIR = PROJECT_DIR.parent / "runs"
RUBRIC_PATH = PROJECT_DIR / "rubric.yaml"
KST = timezone(timedelta(hours=9))

PROVIDER_CODEX = "codex"
PROVIDER_CLAUDE = "claude"

MODEL_CODEX_DEFAULT = None
MODEL_GPT_5_5 = "gpt-5.5"
MODEL_GPT_5_4 = "gpt-5.4"
MODEL_GPT_5_4_MINI = "gpt-5.4-mini"
MODEL_O3 = "o3"

MODEL_CLAUDE_DEFAULT = None
MODEL_CLAUDE_SONNET = "claude-sonnet-4-6"
MODEL_CLAUDE_OPUS = "claude-opus-4-8"

AGENT_GEN = "gen"
AGENT_CRITIQUE = "critique"
AGENT_EVAL = "eval"
AGENT_REFINE = "refine"

PROMPT_VERSIONS = {
    AGENT_GEN: "gen_system:v3",
    AGENT_CRITIQUE: "critique_system:v2",
    AGENT_EVAL: "eval_system:v2",
    AGENT_REFINE: "refine_system:v2",
}

CODEX_DEFAULT_MODELS = {
    AGENT_GEN: MODEL_GPT_5_5,
    AGENT_CRITIQUE: MODEL_GPT_5_5,
    AGENT_EVAL: MODEL_GPT_5_5,
    AGENT_REFINE: MODEL_GPT_5_5,
}

CLAUDE_DEFAULT_MODELS = {
    AGENT_GEN: MODEL_CLAUDE_DEFAULT,
    AGENT_CRITIQUE: MODEL_CLAUDE_DEFAULT,
    AGENT_EVAL: MODEL_CLAUDE_DEFAULT,
    AGENT_REFINE: MODEL_CLAUDE_DEFAULT,
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


def display_model(model: str | None, provider: str = PROVIDER_CODEX) -> str:
    if model:
        return model
    return "claude-sonnet-4-6" if provider == PROVIDER_CLAUDE else "codex-cli-default"


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
    def final_markdown_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_final.md"

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
    retried: bool = False,
) -> dict:
    metadata = {
        "prompt_version": PROMPT_VERSIONS[source_stage],
        "source_files": [f'{input_data["brief_hash"]}_input.json'],
    }
    if token_usage:
        metadata["token_usage"] = token_usage
    if retried:
        metadata["envelope_retry"] = True

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
        "prompt_version": PROMPT_VERSIONS[AGENT_CRITIQUE],
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
        "prompt_version": PROMPT_VERSIONS[AGENT_EVAL],
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


ENVELOPE_MARK = "content is a JSON envelope"


def is_envelope_only(result: dict) -> bool:
    errors = result.get("errors") or []
    return bool(errors) and all(ENVELOPE_MARK in str(error) for error in errors)


def call_stage_with_envelope_retry(call, output_path: Path, artifact: str, progress, label: str):
    """Run a generation stage, retrying once when the model double-wrapped its output.

    A JSON envelope is a formatting slip, not a quality verdict. The contract
    still refuses it, but ending a whole run over one malformed response throws
    away every stage that already succeeded. A second slip is treated as real.
    """
    token_usage = call()
    result = validate_file(output_path, artifact=artifact)
    retried = False
    if is_envelope_only(result):
        retried = True
        progress.line(f"{label} envelope detected, retrying once")
        token_usage = call()
        result = validate_file(output_path, artifact=artifact)
    progress.validation(label, result)
    ensure_pass(result)
    return token_usage, retried


def unsupported_claim_errors(critique_artifact: dict) -> list[str]:
    """Turn the critique's grounding findings into contract errors.

    The rubric alone does not catch invented material: a draft that fabricated
    pipeline internals scored higher on grounding than the same draft without
    them, because specific falsehood reads as better evidence than vague truth.
    Detection therefore belongs to the critique, which compares draft against
    brief and does not score, and the result blocks the run the way any other
    contract failure does.
    """
    claims = critique_artifact.get("unsupported_claims")
    if not isinstance(claims, list):
        return []
    errors = []
    for claim in claims:
        if not isinstance(claim, dict):
            continue
        text = str(claim.get("claim", "")).strip()
        if text:
            errors.append(f"unsupported_claim: {text[:80]}")
    return errors


TITLE_MAX_CHARS = 80
SENTENCE_ENDINGS = (".", "!", "?")


def build_final_markdown(final_artifact: dict, eval_artifact: dict, rubric: dict) -> str:
    """Render the accepted draft as Markdown for a person to read.

    The JSON artifacts are the contract between stages; a person reviewing the
    piece should not have to read escaped newlines to do it. The prose comes
    first and the scorecard sits below a rule, so the file still reads as the
    piece it is.
    """
    content = final_artifact.get("content", "")
    lines = render_body(content)
    lines.extend(render_scorecard(final_artifact, eval_artifact, rubric))
    lines.extend(render_suggestions(final_artifact))
    return "\n".join(lines) + "\n"


def render_body(content: str) -> list[str]:
    """Pass Markdown through untouched; give plain prose the structure it lacks.

    Drafts are written as Markdown now, and reformatting one would break its
    code blocks and lists. Older plain-text drafts still need their paragraphs
    separated and their opening line promoted to a title.
    """
    if any(line.startswith("#") for line in content.splitlines()):
        return [content.rstrip(), ""]

    paragraphs = [line.strip() for line in content.split("\n") if line.strip()]
    lines: list[str] = []
    if paragraphs and len(paragraphs[0]) <= TITLE_MAX_CHARS and not paragraphs[0].endswith(SENTENCE_ENDINGS):
        lines.append(f"# {paragraphs.pop(0)}")
        lines.append("")
    for paragraph in paragraphs:
        lines.append(paragraph)
        lines.append("")
    return lines


KIND_LABELS = {
    "scene": "장면",
    "evidence": "근거",
    "reference": "출처",
    "analogy": "비유",
    "diagram": "그림",
    "structure": "배치",
}


def render_suggestions(final_artifact: dict) -> list[str]:
    """Render proposals the author has to decide on, kept out of the piece.

    A stage that finds a claim needing material it does not have has three
    options: omit it, invent it, or ask. Only the third is honest, so the
    proposals live here rather than in the body, where an invented scene would
    read as something that happened.
    """
    suggestions = final_artifact.get("suggestions") or []
    if not suggestions:
        return []
    lines = [
        "---",
        "",
        "## 추가 제안",
        "",
        "본문에 넣지 않은 제안이다. 채택 여부는 저자가 정한다. "
        "[저자 필요]는 저자만 댈 수 있는 재료라 파이프라인이 채우지 않았다.",
        "",
    ]
    for item in suggestions:
        tag = "저자 필요" if item.get("needs_author") else "바로 사용"
        kind = KIND_LABELS.get(item.get("kind", ""), item.get("kind", ""))
        lines.append(f"- **[{tag}] {kind}** — {item.get('target', '')}")
        lines.append(f"  - {item.get('proposal', '')}")
    lines.append("")
    return lines


def render_scorecard(final_artifact: dict, eval_artifact: dict, rubric: dict) -> list[str]:
    snapshot = final_artifact.get("quality_snapshot", {})
    scores = snapshot.get("scores", {})
    thresholds = rubric.get("thresholds", {})
    min_axis = thresholds.get("min_axis", {})
    axes = rubric.get("axes", {})
    rationales = eval_artifact.get("axis_rationales", {})
    lineage = final_artifact.get("lineage", {})

    lines = [
        "---",
        "",
        "## 판정",
        "",
        f"- run `{lineage.get('run_id', '')}` · iteration {final_artifact.get('final_iteration', '')}"
        f" · {final_artifact.get('accepted_at', '')}",
        f"- rubric `{snapshot.get('rubric_name', '')}` · 총점 **{snapshot.get('weighted_total', '')}**"
        f" / 임계 {thresholds.get('min_total', '')}",
        f"- 분량 {len(final_artifact.get('content', ''))}자",
        "",
        "| 축 | 점수 | 임계 | 가중치 | 근거 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for axis, score in scores.items():
        weight = axes.get(axis, {}).get("weight", "")
        rationale = str(rationales.get(axis, "")).replace("|", "/")
        lines.append(f"| `{axis}` | {score} | {min_axis.get(axis, '')} | {weight} | {rationale} |")
    lines.append("")
    return lines


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
    critique_artifact: dict,
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
        "suggestions": critique_artifact.get("suggestions") or [],
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


def summarize_block(errors: list) -> str:
    """Name what actually stopped the last iteration.

    Running out of iterations says when the loop ended, not why. A draft that
    fell short on score and a draft that scored well but stated something the
    brief never contained are different failures, and reading them as one makes
    a pile of failed runs useless for deciding what to fix.
    """
    categories = {categorize_failure(str(error)) for error in errors}
    if not categories:
        return "none"
    if len(categories) == 1:
        return categories.pop()
    return "mixed"


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
        "last_blocked_by": summarize_block(last_errors),
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
            "last_blocked_by가 quality_reject이면 threshold나 생성 프롬프트를, contract_error이면 재료와 계약 검사를 본다",
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
    agent_models = resolve_agent_models(args)
    config = {
        "provider": args.provider,
        "codex_bin": args.codex_bin,
        "agent_models": agent_models,
        "iteration": args.iteration,
        "max_iterations": args.max_iterations,
        "timeout_seconds": args.timeout_seconds,
        "rubric_path": str(rubric_path),
        "rubric": rubric,
    }
    client = create_client(
        provider=args.provider,
        timeout_seconds=args.timeout_seconds,
        codex_bin=args.codex_bin,
    )
    eval_rejections: list[dict] = []
    last_refine_request_lineage: str | None = None
    progress.line(
        f"run start brief={brief_hash} iteration={args.iteration} max_iterations={args.max_iterations} "
        f"rubric={rubric.get('name', rubric_path.name)} run_id={root_context.run_id}"
    )

    try:
        stage = "prepare"
        copy_input(input_path, root_context.copied_input_path, overwrite=args.overwrite)

        banned_terms = load_banned_terms()

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

                    def run_gen():
                        with progress.step(
                            f"{iteration_label} gen model={display_model(config['agent_models'][AGENT_GEN], args.provider)}",
                            live=True,
                        ):
                            return generate(
                                input_path=root_context.copied_input_path,
                                output_path=temp_gen_output_path,
                                client=client,
                                model=config["agent_models"][AGENT_GEN],
                            )

                    stage = f"iter_{iteration}_gen_validate"
                    token_usage, gen_retried = call_stage_with_envelope_retry(
                        run_gen,
                        temp_gen_output_path,
                        "gen_output",
                        progress,
                        f"{iteration_label} gen_output_validate",
                    )
                    gen_output = load_json(temp_gen_output_path)

                stage = f"iter_{iteration}_draft_write"
                draft = build_draft(
                    input_data=input_data,
                    stage_output=gen_output,
                    iteration=iteration,
                    model_name=display_model(config["agent_models"][AGENT_GEN], args.provider),
                    token_usage=token_usage,
                    source_stage=AGENT_GEN,
                    retried=gen_retried,
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
                    f"{iteration_label} critique model={display_model(config['agent_models'][AGENT_CRITIQUE], args.provider)}",
                    live=True,
                ):
                    token_usage = critique(
                        input_path=root_context.copied_input_path,
                        draft_path=context.draft_path,
                        output_path=temp_critique_path,
                        client=client,
                        model=config["agent_models"][AGENT_CRITIQUE],
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
                model_name=display_model(config["agent_models"][AGENT_CRITIQUE], args.provider),
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
                    f"{iteration_label} eval model={display_model(config['agent_models'][AGENT_EVAL], args.provider)}",
                    live=True,
                ):
                    token_usage = evaluate(
                        input_path=root_context.copied_input_path,
                        draft_path=context.draft_path,
                        rubric=rubric,
                        output_path=temp_eval_path,
                        client=client,
                        model=config["agent_models"][AGENT_EVAL],
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
                model_name=display_model(config["agent_models"][AGENT_EVAL], args.provider),
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
            content_errors = content_contract_errors(
                draft.get("content"),
                input_data.get("brief"),
                banned_terms,
            )
            content_errors += unsupported_claim_errors(critique_artifact)
            if content_errors:
                eval_result["errors"] = list(eval_result.get("errors", [])) + content_errors
                eval_result["status"] = "REJECT"

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
                    critique_artifact=critique_artifact,
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

                stage = f"iter_{iteration}_final_markdown"
                context.final_markdown_path.write_text(
                    build_final_markdown(final_artifact, eval_artifact, rubric),
                    encoding="utf-8",
                )
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
                    "final_markdown": str(context.final_markdown_path),
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
                    f"blocked_by={summarize_block(eval_rejections[-1].get('errors', []) if eval_rejections else [])} "
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

                def run_refine():
                    with progress.step(
                        f"iter {iteration}->{to_iteration} refine model={display_model(config['agent_models'][AGENT_REFINE], args.provider)}",
                        live=True,
                    ):
                        return refine(
                            input_path=root_context.copied_input_path,
                            draft_path=context.draft_path,
                            critique_path=context.critique_path,
                            refine_request=refine_request,
                            output_path=temp_refine_output_path,
                            client=client,
                            model=config["agent_models"][AGENT_REFINE],
                        )

                stage = f"iter_{iteration}_refine_output_validate"
                token_usage, refine_retried = call_stage_with_envelope_retry(
                    run_refine,
                    temp_refine_output_path,
                    "refine_output",
                    progress,
                    f"iter {iteration}->{to_iteration} refine_output_validate",
                )
                refine_output = load_json(temp_refine_output_path)

            stage = f"iter_{to_iteration}_draft_write"
            refined_draft = build_draft(
                input_data=input_data,
                stage_output=refine_output,
                iteration=to_iteration,
                model_name=display_model(config["agent_models"][AGENT_REFINE], args.provider),
                token_usage=token_usage,
                source_stage=AGENT_REFINE,
                retried=refine_retried,
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
    defaults = CLAUDE_DEFAULT_MODELS if args.provider == PROVIDER_CLAUDE else CODEX_DEFAULT_MODELS
    models = defaults.copy()
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
    parser.add_argument("--provider", choices=[PROVIDER_CODEX, PROVIDER_CLAUDE], default=PROVIDER_CODEX)
    parser.add_argument("--codex-bin", default="codex")
    parser.add_argument("--model", help="Alias for --gen-model in the current MVP.")
    parser.add_argument("--gen-model", help="Model for the Gen agent.")
    parser.add_argument("--critique-model", help="Model for the Critique agent.")
    parser.add_argument("--eval-model", help="Model for the Eval agent.")
    parser.add_argument("--refine-model", help="Model for the Refine agent.")
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
