from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import stat
import subprocess
import sys
import tempfile
import re
import threading
import time
import traceback
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

from stages.critique import critique
from stages.evaluator import evaluate
from stages.generator import generate
from stages.refine import refine
from stages.scripts.llm_client import create_client
from stages.scripts.textio import harden_stream, run_command
from validate import check_schema_pairs, normalize_model_output, validate_file, write_result


PROJECT_DIR = Path(__file__).resolve().parent
SKILL_ROOT = PROJECT_DIR.parent
REPO_ROOT = PROJECT_DIR.parents[3]
RUNS_DIR = SKILL_ROOT / "runs"
PROMPTS_DIR = PROJECT_DIR / "prompts"
RUBRICS_DIR = PROJECT_DIR / "rubrics"
SCHEMAS_DIR = PROJECT_DIR / "schemas"
RULES_DIR = SKILL_ROOT / "rules"
KST = timezone(timedelta(hours=9))

PROVIDER_CODEX = "codex"
PROVIDER_CLAUDE = "claude"

GEN_PROMPT_PATH = PROMPTS_DIR / "gen_impl.md"
RUBRIC_PATH = RUBRICS_DIR / "impl.rubric.yaml"
EVAL_MODEL_SCHEMA = SCHEMAS_DIR / "eval_output.impl.schema.json"
CONFORMANCE_RULES_PATH = RULES_DIR / "conformance_rules.yaml"
DEFAULT_SKELETON_DIR = REPO_ROOT / "phase2" / "taskE" / "task5" / "skeleton"

# 기본값을 이름으로 못 박는다. None으로 두면 CLI의 그날 기본값으로 돌고 config.agent_models에도 None이
# 남아, 나중에 이 run이 어떤 모델로 돌았는지 기록만 보고 말할 수 없다. 더 센 모델은 --model로 준다.
MODEL_CLAUDE_DEFAULT = "claude-sonnet-4-6"
MODEL_GPT_5_5 = "gpt-5.5"

AGENT_GEN = "gen"
AGENT_CRITIQUE = "critique"
AGENT_EVAL = "eval"
AGENT_REFINE = "refine"

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

FINAL_CHECKED_RULES = ["schema", "brief_hash", "manifest", "gate_g0", "gate_g1", "min_total", "min_axis"]

STATUS_SKIPPED = "SKIPPED"
STATUS_PASS = "PASS"
STATUS_REJECT = "REJECT"
STATUS_ERROR = "ERROR"

GATE_FUNCTIONS = ("run_g0", "run_g1", "check_contract_changes", "load_rules")


class GateEnvironmentError(RuntimeError):
    """판정 장치나 실행 환경이 없어서 판정 자체를 하지 못한 상태.

    초안의 잘못이 아니므로 REJECT가 아니다. REJECT로 적으면 refine이 고칠 수 없는 것을 고치려 들어
    iteration만 태운다. 게이트 공백은 통과도 아니다. 그래서 run을 ERROR로 끝낸다.
    """

# Refine이 받는 지시의 순위. 지시가 어긋날 수 있으므로 못 박는다.
REFINE_PRIORITY_ORDER = ["gate_violations", "contract_version", "standing_decisions", "code_instructions"]


# --- 진행 로그 (1-1에서 그대로) ---------------------------------------------


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
    return "claude-cli-default" if provider == PROVIDER_CLAUDE else "codex-cli-default"


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
        harden_stream(stream)
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
        if result["status"] == STATUS_PASS:
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


# --- run 폴더 ---------------------------------------------------------------


@dataclass(frozen=True)
class RunContext:
    """run 폴더의 좌표. 계약을 판본으로 들고 가므로 origin·baseline·iter·artifact 넷을 가진다."""

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

    @classmethod
    def at(cls, brief_hash: str, iteration: str, run_dir: Path) -> "RunContext":
        """이미 있는 run 폴더를 그대로 집는다. run_stage가 단계 하나만 돌릴 때 쓴다."""
        resolved = run_dir.resolve()
        return cls(
            brief_hash=brief_hash,
            iteration=iteration,
            runs_dir=resolved.parent,
            run_id=resolved.name,
        )

    def with_iteration(self, iteration: str) -> "RunContext":
        return RunContext(
            brief_hash=self.brief_hash,
            iteration=iteration,
            runs_dir=self.runs_dir,
            run_id=self.run_id,
        )

    @property
    def run_dir(self) -> Path:
        return self.runs_dir / self.run_id

    @property
    def origin_dir(self) -> Path:
        return self.run_dir / "origin"

    @property
    def baseline_dir(self) -> Path:
        return self.run_dir / "baseline"

    @property
    def output_dir(self) -> Path:
        """작업 폴더. iteration마다 새로 만들지 않고 제자리에서 고친다. 스냅샷은 git이 맡는다."""
        return self.run_dir / "output"

    @property
    def iter_dir(self) -> Path:
        return self.run_dir / f"iter_{self.iteration}"

    @property
    def copied_input_path(self) -> Path:
        return self.run_dir / f"{self.brief_hash}_input.json"

    def _iter_file(self, name: str) -> Path:
        return self.iter_dir / name

    @property
    def draft_path(self) -> Path:
        return self._iter_file("draft.json")

    @property
    def draft_validation_path(self) -> Path:
        return self._iter_file("draft.validation.json")

    @property
    def gate_path(self) -> Path:
        return self._iter_file("gate.json")

    @property
    def critique_path(self) -> Path:
        return self._iter_file("critique.json")

    @property
    def critique_validation_path(self) -> Path:
        return self._iter_file("critique.validation.json")

    @property
    def eval_path(self) -> Path:
        return self._iter_file("eval.json")

    @property
    def eval_validation_path(self) -> Path:
        return self._iter_file("eval.validation.json")

    @property
    def refine_request_path(self) -> Path:
        return self._iter_file("refine-request.json")

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


def now_iso() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def normalize_newlines(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\r", "\n")


def sha256_text(text: str) -> str:
    """계약 원문의 해시. 줄끝을 정규화해 플랫폼이 판정을 흔들지 못하게 한다."""
    return hashlib.sha256(normalize_newlines(text).encode("utf-8")).hexdigest()


def load_rubric_optional(path: Path) -> dict | None:
    """루브릭은 다른 조각이 소유한다. 없으면 None을 돌려 부르는 쪽이 판단하게 한다."""
    if not path.exists():
        return None
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


def load_structured(path: Path) -> dict:
    """계약 판본 파일(YAML 또는 JSON)을 읽는다."""
    text = path.read_text(encoding="utf-8")
    if path.suffix.lower() in (".yaml", ".yml"):
        import yaml  # type: ignore[import-not-found]

        data = yaml.safe_load(text)
    else:
        data = json.loads(text)
    if not isinstance(data, dict):
        raise ValueError(f"expected contract mapping: {path}")
    return data


def rubric_axes(rubric: dict | None) -> dict[str, str]:
    """축의 이름과 뜻만 뽑는다. Critique는 스케일·가중·하한을 받지 않는다."""
    if not rubric:
        return {}
    axes = rubric.get("axes", {})
    if not isinstance(axes, dict):
        return {}
    meanings: dict[str, str] = {}
    for name, body in axes.items():
        if isinstance(body, dict):
            meanings[name] = str(body.get("description") or body.get("what") or "")
        else:
            meanings[name] = str(body)
    return meanings


def copy_input(source: Path, destination: Path, overwrite: bool = False) -> None:
    if destination.exists() and not overwrite:
        current = destination.read_text(encoding="utf-8")
        incoming = source.read_text(encoding="utf-8")
        if current == incoming:
            return
        raise FileExistsError(f"input file already exists with different content: {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(source.read_bytes())


def ensure_pass(result: dict, result_path: Path | None = None) -> None:
    if result["status"] == STATUS_PASS:
        return
    if result_path:
        write_result(result, result_path)
    raise RuntimeError(json.dumps(result, ensure_ascii=False, indent=2))


def write_readonly(path: Path, text: str) -> None:
    """기준선을 읽기전용으로 잠근다. 잠긴 파일이 있으면 먼저 풀고 같은 내용으로 다시 쓴다."""
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        os.chmod(path, stat.S_IWRITE | stat.S_IREAD)
    path.write_bytes(normalize_newlines(text).encode("utf-8"))
    os.chmod(path, stat.S_IREAD)


def prepare_contract_copies(context: RunContext, input_data: dict, origin_paths: list[Path]) -> dict[str, list[str]]:
    """기준선과 원본을 run 폴더에 복사하고 읽기전용으로 잠근다.

    기준선은 input에 실린 원문이다. 원본은 사람이 확정한 판이고, 주어지지 않으면 첫 run이므로
    기준선과 같다. 둘을 구분하지 않으면 run을 여러 번 돌린 뒤에 계약이 원래 무엇을 약속했는지 알 수 없다.
    """
    lineage: dict[str, list[str]] = {"baseline": [], "origin": []}
    origin_by_name = {path.name: path for path in origin_paths}
    for contract in input_data["brief"]["contracts"]:
        name = Path(contract["path"]).name
        baseline_path = context.baseline_dir / name
        write_readonly(baseline_path, contract["text"])
        lineage["baseline"].append(relative_to_run(baseline_path, context.run_dir))

        origin_source = origin_by_name.get(name)
        origin_text = (
            normalize_newlines(origin_source.read_bytes().decode("utf-8"))
            if origin_source is not None
            else contract["text"]
        )
        origin_path = context.origin_dir / name
        write_readonly(origin_path, origin_text)
        lineage["origin"].append(relative_to_run(origin_path, context.run_dir))
    return lineage


def verify_contract_hashes(input_data: dict, progress: ProgressReporter) -> list[str]:
    """input의 sha256이 저장소의 계약 파일과 다른지 본다. 다르면 손으로 고친 판이라는 뜻이다."""
    drifted: list[str] = []
    for contract in input_data["brief"]["contracts"]:
        declared = contract["sha256"]
        computed = sha256_text(contract["text"])
        if declared != computed:
            raise ValueError(
                f"input contract sha256 mismatch for {contract['path']}: declared {declared}, computed {computed}"
            )
        source = REPO_ROOT / contract["path"]
        if not source.exists():
            continue
        on_disk = sha256_text(source.read_bytes().decode("utf-8"))
        if on_disk != declared:
            drifted.append(contract["path"])
            progress.line(
                f"baseline drift path={contract['path']} — input이 저장소 파일과 다르다. check_contract.py 대상이다."
            )
    return drifted


# --- 결정적 장치 (다른 조각이 소유한다. 없으면 SKIPPED) -----------------------


def import_pipeline_module(name: str) -> tuple[object | None, str]:
    """pipeline 안의 모듈을 가져온다.

    `pipeline.<name>`을 먼저 시도한다. 조각 사이에서 상대 import(`from ..tools import ...`)를 쓰는 모듈이
    있고 그것은 `pipeline`이 패키지로 잡혀야 풀린다. top-level import만 하면 그 모듈이 "없는 것"으로 보여
    판정 장치가 있는데도 run이 멈춘다. 실패한 이유를 함께 돌려주어 없는 것과 깨진 것을 가른다.
    """
    import importlib

    # `pipeline`이 패키지로 잡히려면 그 부모(스킬 루트)가 sys.path에 있어야 한다.
    root = str(SKILL_ROOT)
    if root not in sys.path:
        sys.path.insert(0, root)
    reasons: list[str] = []
    for candidate in (f"pipeline.{name}", name):
        try:
            return importlib.import_module(candidate), ""
        except ImportError as exc:
            reasons.append(f"{candidate}: {exc}")
    return None, "; ".join(reasons)


def load_gates_module(allow_missing: bool = True):
    """게이트는 다른 조각이 소유한다. 없는데 허용하지 않으면 그 자리에서 ERROR다."""
    gates, reason = import_pipeline_module("gates")
    if gates is None:
        if allow_missing:
            return None
        raise GateEnvironmentError(
            f"gates module not importable ({reason}). "
            "판정 장치 없이 돌린 run은 통과로 읽을 수 없다. 개발 중에만 --allow-missing-gates 로 건너뛴다."
        )
    absent = [name for name in GATE_FUNCTIONS if not hasattr(gates, name)]
    if absent:
        if allow_missing:
            return None
        raise GateEnvironmentError(f"gates module is incomplete: missing {absent}")
    return gates


def load_gate_rules(rules_path: Path | None, allow_missing: bool, progress: ProgressReporter) -> dict | None:
    """규칙 카드를 게이트에서 읽는다. 세 함수가 같은 카드를 봐야 판정값이 한 파일에 모인다."""
    gates = load_gates_module(allow_missing=allow_missing)
    if gates is None:
        progress.line(f"gates {STATUS_SKIPPED} reason=module_missing allow_missing_gates=true")
        return None
    target = rules_path or CONFORMANCE_RULES_PATH
    try:
        rules = gates.load_rules(str(target))
    except Exception as exc:
        if allow_missing:
            progress.line(f"rule cards {STATUS_SKIPPED} reason={type(exc).__name__} path={target}")
            return None
        raise GateEnvironmentError(
            f"rule cards not loadable: {target} ({type(exc).__name__}: {exc})\n"
            "카드가 없으면 어떤 초과를 위반으로 보고 어떤 초과를 기록으로 볼지가 정해지지 않는다."
        ) from exc
    if not rules or not rules.get("rules"):
        if allow_missing:
            progress.line(f"rule cards {STATUS_SKIPPED} reason=empty path={target}")
            return None
        raise GateEnvironmentError(f"rule cards are empty: {target}")
    return rules


PROSE_SLOT_EXTRACTORS = ("extract_prose_slots", "extract")


def load_prose_slots(baseline_contract: Path | None, override: Path | None) -> tuple[list[dict], str]:
    """산문 슬롯을 기준선에서 뽑는다. 슬롯은 기준선에서만 뽑는다 — 분모가 흔들리면 채점도 흔들린다.

    추출기는 다른 조각이 소유하므로 import를 지연시키고, 없으면 빈 목록과 SKIPPED를 돌려준다.
    """
    if override is not None:
        data = json.loads(override.read_text(encoding="utf-8"))
        slots = data.get("prose_slots", data) if isinstance(data, dict) else data
        return (slots if isinstance(slots, list) else []), f"file:{override}"
    if baseline_contract is None or not baseline_contract.exists():
        return [], STATUS_SKIPPED
    prose_slots, _ = import_pipeline_module("tools.prose_slots")
    if prose_slots is None:
        return [], STATUS_SKIPPED
    for name in PROSE_SLOT_EXTRACTORS:
        extractor = getattr(prose_slots, name, None)
        if callable(extractor):
            slots = extractor(load_structured(baseline_contract))
            return (slots if isinstance(slots, list) else []), f"tools.prose_slots.{name}"
    return [], STATUS_SKIPPED


def load_ledger_writer():
    module, _ = import_pipeline_module("ledger")
    return getattr(module, "update_ledger", None) if module is not None else None


def load_report_writer():
    module, _ = import_pipeline_module("report")
    return getattr(module, "write_report", None) if module is not None else None


# --- 매니페스트와 판본 --------------------------------------------------------


def sanitize_rel_path(raw: str) -> str:
    """색인 경로를 작업 폴더 안에 안전하게 가둔다(절대경로·.. 이탈 차단)."""
    rel = raw.strip().replace("\\", "/").lstrip("/")
    parts = [p for p in rel.split("/") if p not in ("", ".")]
    if not parts or any(p == ".." for p in parts):
        raise ValueError(f"unsafe path in manifest: {raw!r}")
    return "/".join(parts)


def normalize_manifest(files: list) -> list[dict]:
    """gen/refine가 낸 색인을 정돈한다(경로 정리, 중복 방지). 내용은 응답에 없고 작업 폴더에 있다."""
    normalized: list[dict] = []
    seen: set[str] = set()
    for entry in files:
        rel = sanitize_rel_path(entry["path"])
        if rel in seen:
            raise ValueError(f"duplicate path in manifest: {rel}")
        seen.add(rel)
        normalized.append({"path": rel, "role": entry["role"]})
    return normalized


# --- 작업 폴더와 git ---------------------------------------------------------

GIT_IGNORE = "target/\n.idea/\n*.class\n"

# 작업 저장소에만 거는 설정. 판정이 기계의 git 설정에 흔들리지 않게 한다.
# autocrlf가 켜져 있으면 커밋이 줄끝을 바꿔 기준선 계약의 바이트가 달라진다. 해시로 못 박은 뜻이 사라진다.
# 전역 excludesFile이 파일을 숨기면 `git status`에 안 나타나 undeclared_file을 놓친다.
# longpaths는 run 폴더가 깊을 때 add가 실패하는 것을 막는다.
GIT_LOCAL_CONFIG = {
    "core.autocrlf": "false",
    "core.safecrlf": "false",
    "core.longpaths": "true",
    "core.excludesFile": "",
    "core.fileMode": "false",
}
GIT_ENV_OVERRIDES = {
    "GIT_CONFIG_NOSYSTEM": "1",
    "GIT_TERMINAL_PROMPT": "0",
    "GIT_AUTHOR_NAME": "contract-impl-agent",
    "GIT_AUTHOR_EMAIL": "harness@localhost",
    "GIT_COMMITTER_NAME": "contract-impl-agent",
    "GIT_COMMITTER_EMAIL": "harness@localhost",
}


def git(args: list[str], cwd: Path, check: bool = True) -> str:
    """작업 폴더의 git을 부른다. 이 저장소는 프로젝트 저장소와 무관한 별도의 것이다."""
    completed = run_command(["git", *args], cwd=cwd, env={**os.environ, **GIT_ENV_OVERRIDES})
    if check and completed.returncode != 0:
        raise RuntimeError(
            f"git {' '.join(args)} failed in {cwd}\nstdout: {completed.stdout}\nstderr: {completed.stderr}"
        )
    return completed.stdout


def copy_skeleton(skeleton_dir: Path, target: Path) -> int:
    """스켈레톤을 작업 폴더로 복사한다. 빌드 산출물과 남의 git 이력은 옮기지 않는다."""
    skipped = ("target", ".git", ".idea", "__pycache__")

    def ignore(_directory: str, names: list[str]) -> list[str]:
        return [name for name in names if name in skipped]

    shutil.copytree(skeleton_dir, target, ignore=ignore, dirs_exist_ok=True)
    return sum(1 for path in target.rglob("*") if path.is_file())


def init_output(
    context: RunContext,
    input_data: dict,
    skeleton_dir: Path,
    progress: ProgressReporter,
) -> dict:
    """작업 폴더를 만들고 첫 커밋을 남긴다.

    그 커밋이 iteration 001의 대조 기준이다. 기준선 계약 사본도 여기 들어간다 — 생성기가 읽는 계약과
    게이트가 대조하는 계약이 한 바이트도 달라지지 않게 하는 장치의 마지막 자리다.
    """
    output = context.output_dir
    if (output / ".git").exists():
        head = git(["rev-parse", "HEAD"], output).strip()
        progress.line(f"output reused commit={head[:10]} path={output}")
        return {"commit": head, "created": False}

    output.mkdir(parents=True, exist_ok=True)
    copied = copy_skeleton(skeleton_dir, output) if skeleton_dir.exists() else 0
    if not copied:
        progress.line(f"skeleton not copied path={skeleton_dir} — 작업 폴더가 비어 시작한다")
    (output / ".gitignore").write_bytes(GIT_IGNORE.encode("utf-8"))

    for contract in input_data["brief"]["contracts"]:
        name = Path(contract["path"]).stem
        suffix = Path(contract["path"]).suffix
        seeded = output / "contract" / f"{name}-v1{suffix}"
        seeded.parent.mkdir(parents=True, exist_ok=True)
        seeded.write_bytes(normalize_newlines(contract["text"]).encode("utf-8"))

    git(["init", "-q", "-b", "main"], output)
    for key, value in GIT_LOCAL_CONFIG.items():
        git(["config", key, value], output)
    git(["add", "-A"], output)
    git(["commit", "-q", "-m", "run start: skeleton and baseline contract"], output)
    locked = lock_contract_versions(output)
    head = git(["rev-parse", "HEAD"], output).strip()
    if locked:
        progress.line(f"contract baseline locked read-only: {locked}")
    progress.line(f"output init commit={head[:10]} files={copied} path={output}")
    return {"commit": head, "created": True}


def changed_paths(output_dir: Path) -> set[str]:
    """커밋되지 않은 변화의 경로. 이것이 선언과 대조할 실물이다.

    `git status`를 쓰므로 `.gitignore`가 빼는 빌드 산출물은 들어오지 않는다. 생성기가 신고하지 않고
    고친 것까지 여기 나타나므로, 매니페스트를 들여다보는 것보다 세다.
    """
    paths: set[str] = set()
    for line in git(["status", "--porcelain", "-uall"], output_dir).splitlines():
        if not line.strip():
            continue
        entry = line[3:].strip()
        if " -> " in entry:  # 이름이 바뀐 것은 양쪽을 다 센다
            before, after = entry.split(" -> ", 1)
            paths.add(before.strip().strip('"'))
            paths.add(after.strip().strip('"'))
            continue
        paths.add(entry.strip('"'))
    return paths


def head_commit(output_dir: Path) -> str:
    return git(["rev-parse", "HEAD"], output_dir).strip()


def commit_iteration(output_dir: Path, iteration: str, stage: str) -> str:
    """iteration 하나를 커밋한다.

    커밋을 남기는 것은 runner의 일이다. 생성기가 git을 만지면 원장의 iteration 경계가 흐려진다.
    바뀐 것이 없어도 빈 커밋을 남겨 이력이 끊기지 않게 한다.
    """
    git(["add", "-A"], output_dir)
    git(["commit", "-q", "--allow-empty", "-m", f"iter {iteration} ({stage})"], output_dir)
    lock_contract_versions(output_dir)
    return head_commit(output_dir)


def detect_generator_commits(output_dir: Path, expected_head: str) -> list[str]:
    """생성기가 스스로 남긴 커밋을 찾는다.

    스냅샷과 되돌리기는 runner의 일이므로 생성기는 커밋하지 않는다. 그래도 남겼으면 막지 않고 기록한다.
    막으면 그 iteration의 산출을 버려야 하는데, 커밋 자체는 판정 대상이 아니다.
    """
    if not expected_head:
        return []
    current = head_commit(output_dir)
    if current == expected_head:
        return []
    listed = git(["rev-list", f"{expected_head}..{current}"], output_dir).strip()
    return [line.strip() for line in listed.splitlines() if line.strip()]


CONTRACT_DIR = "contract"

# 규칙 카드 id의 모양. `manifest.undeclared_file`처럼 점으로 이어진 소문자 이름이다.
RULE_ID_PATTERN = re.compile(r"^[a-z][a-z0-9_]*\.[a-z][a-z0-9_]*$")


def contract_version_files(output_dir: Path) -> list[str]:
    """작업 폴더에 쌓인 계약 판본 파일 목록."""
    directory = output_dir / CONTRACT_DIR
    if not directory.exists():
        return []
    return sorted(
        path.relative_to(output_dir).as_posix() for path in directory.rglob("*") if path.is_file()
    )


def lock_contract_versions(output_dir: Path) -> list[str]:
    """작업 폴더에 있는 계약 판본 전부를 읽기전용으로 잠근다.

    기준선 사본까지 잠근다. runner가 `init_output`에서 그 파일을 놓으므로 생성기가 다시 쓸 이유가 없고,
    4만 7천 자를 옮겨 적으라고 하면 모델은 요약한다. 여덟 run 중 일곱에서 gen이 계약 사본을 쓰면서
    `description`과 `summary`와 `x-requirement`를 지웠다. 지시로 막을 성질이 아니라 권한으로 막는다.

    판본을 올리는 것은 막지 않는다. 판 번호가 오른 사본은 아직 없는 파일이므로 만드는 데 걸리지 않는다.
    같은 이름으로 내용을 바꾸는 길만 닫힌다.
    """
    locked: list[str] = []
    for rel in contract_version_files(output_dir):
        try:
            os.chmod(output_dir / rel, stat.S_IREAD)
        except OSError:
            continue
        locked.append(rel)
    return locked


def unlock_path(path: Path) -> None:
    if path.exists():
        os.chmod(path, stat.S_IWRITE | stat.S_IREAD)


def check_previous_versions(changed: set[str], current_version: str | None) -> list[dict]:
    """잠긴 앞 판본이 고쳐진 흔적을 찾는다.

    잠금은 구조로 막는 장치이고 이 검사는 그것이 뚫렸을 때 드러내는 자리다. 앞 판본이 감사 기준이므로
    그것이 움직이면 사슬이 어디서 출발했는지 말할 수 없게 된다.
    """
    current = normalize_newlines(current_version or "").strip()
    violations: list[dict] = []
    for rel in sorted(changed):
        normalized = rel.replace("\\", "/")
        if not normalized.startswith(f"{CONTRACT_DIR}/") or normalized == current:
            continue
        violations.append(
            {
                "rule": "contract.previous_version_modified",
                "point": None,
                "where": normalized,
                "detail": "앞 계약 판본이 고쳐졌다. 판본은 쌓이고 앞 판본은 감사 기준이다",
                "verdict": "violation",
            }
        )
    return violations


# payload에 싣는 코드의 상한. 지금 자바 파일 23개라 여유가 크지만, 넘는 날 조용히 빠지지 않게 한다.
CODE_PAYLOAD_TOTAL_LIMIT = 1_500_000
CODE_PAYLOAD_FILE_LIMIT = 200_000


def collect_code_files(
    output_dir: Path,
    first_commit: str,
    index_paths: list[str],
    total_limit: int = CODE_PAYLOAD_TOTAL_LIMIT,
    file_limit: int = CODE_PAYLOAD_FILE_LIMIT,
) -> dict:
    """비평과 채점에 실어 보낼 코드를 모은다.

    읽기를 모델의 도구 성공에 맡기지 않는다. 채점자가 코드를 열지 못하면 "대지 못하는 슬롯은 미충족"
    규칙 때문에 도구 실패가 최저점이 된다. 실제로 그 일이 있었다. 측정하지 못한 것은 판정이 아니므로
    측정 자체를 runner가 보장한다.

    대상은 색인이 선언한 파일과 첫 커밋 이후 쌓인 생성 코드 전부다. 첫 커밋은 스켈레톤과 기준선이므로
    그 뒤의 diff가 곧 생성된 것이다. 상한을 넘겨 빠진 것은 `omitted`에 적는다 — 조용히 빠지면
    채점자가 그것을 미충족으로 센다.
    """
    wanted: list[str] = []
    for rel in list(index_paths) + (
        [line.strip().strip('"') for line in git(["diff", "--name-only", first_commit, "HEAD"], output_dir).splitlines()]
        if first_commit
        else []
    ):
        normalized = rel.strip().replace("\\", "/")
        if normalized and normalized not in wanted:
            wanted.append(normalized)

    files: list[dict] = []
    omitted: list[dict] = []
    total = 0
    for rel in sorted(wanted):
        path = output_dir / rel
        if not path.is_file():
            omitted.append({"path": rel, "reason": "not present in the work tree", "bytes": 0})
            continue
        size = path.stat().st_size
        if size > file_limit:
            omitted.append({"path": rel, "reason": f"larger than the per-file limit {file_limit}", "bytes": size})
            continue
        if total + size > total_limit:
            omitted.append({"path": rel, "reason": f"total limit {total_limit} reached", "bytes": size})
            continue
        try:
            content = path.read_bytes().decode("utf-8")
        except UnicodeDecodeError:
            omitted.append({"path": rel, "reason": "not utf-8 text", "bytes": size})
            continue
        files.append({"path": rel, "content": content})
        total += size

    return {
        "note": (
            "이 iteration의 구현 전문이다. 파일마다 path가 머리다. omitted가 비어 있지 않으면 그 파일은 "
            "여기 없으니 작업 디렉터리에서 직접 읽어라. 없는 것을 미충족으로 세지 마라."
        ),
        "files": files,
        "omitted": omitted,
        "total_bytes": total,
        "truncated": bool(omitted),
    }


class TransportFailure(RuntimeError):
    """모델 응답의 형식이 깨져 산출을 받지 못한 상태.

    채점 결과가 아니라 전송 실패다. 응답이 잘려 오거나 JSON이 아닌 것이 왔을 때이고 초안의 품질과 아무
    관계가 없다. 그래서 판정으로 세지 않는다.
    """


def call_stage(label: str, attempts: int, progress: ProgressReporter, call) -> tuple[object, Exception | None]:
    """단계를 부르고 형식 파싱 실패에만 재시도한다.

    `ValueError`는 응답 형식이 깨진 것이므로 다시 부른다. 그 밖의 예외는 그대로 올린다 — CLI가 죽은 것이나
    우리 코드의 버그를 재시도로 덮으면 안 된다.
    """
    last: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            return call(attempt), None
        except ValueError as exc:
            last = exc
            progress.line(
                f"{label} attempt={attempt}/{attempts} 응답 형식이 깨졌다 — {str(exc).splitlines()[0][:160]}"
            )
    return None, last


def unmeasured_eval(brief_hash: str, iteration: str, rubric: dict | None, reason: str) -> dict:
    """채점을 받지 못했을 때 그 사실을 적은 eval 산출.

    응답 형식이 깨진 것은 채점 결과가 아니라 전송 실패다. 그때 필요한 것은 run을 죽이는 것이 아니라
    "판정하지 못했다"를 적고 나아가는 것이다. 다섯 축을 모두 측정 불가로 적으므로 이 채점은 하한 대조에
    쓰이지 않고 PASS도 주지 않는다. 점수는 자리를 채우는 값이고 판정에 들어가지 않는다.
    """
    axes = sorted(rubric_axes(rubric)) or [
        "request_tolerance",
        "response_fidelity",
        "failure_faithfulness",
        "state_continuity",
        "judgement_disclosure",
    ]
    weights = {}
    for axis in axes:
        body = (rubric or {}).get("axes", {}).get(axis)
        weights[axis] = float(body.get("weight", 0)) if isinstance(body, dict) else 0.0
    return {
        "brief_hash": brief_hash,
        "iteration": iteration,
        "rubric_name": (rubric or {}).get("name", "impl:v1"),
        "rubric_scores": {"scores": {axis: 0 for axis in axes}, "weights": weights, "weighted_total": 0},
        "axis_rationales": {axis: f"측정하지 못했다: {reason}" for axis in axes},
        "slot_classifications": [],
        "unmeasured_axes": axes,
        "calibration_note": f"채점을 받지 못해 runner가 측정 불가로 적었다. {reason}",
    }


def load_source_critique(context: RunContext) -> tuple[list[dict] | None, str]:
    """이 iteration 전까지 이 run이 낸 비평 전부를 모은다.

    계약 변경은 iteration을 넘어 살아남는다. iteration 002에서 한 변경을 003이 들고 가면 그 근거가 된 비평은
    001이나 002의 것이므로, 앞 iteration 하나만 넘기면 그 기록이 전부 "인용한 id가 비평에 없다"로 걸린다.
    지난 비평을 되살릴 길이 없으니 **모델이 만족시킬 수 없는 요구**가 된다. 계약 diff 쪽은 `baseline`과
    `accumulated`로 같은 자리를 막았고 근거 대조도 같은 범위여야 한다.

    **이번 iteration의 비평은 넣지 않는다.** 그것은 이 판본이 만들어진 뒤에 나온 것이므로, 넣으면 결정이
    내려질 때 없던 정보로 그 결정을 정당화하게 된다.

    아무것도 못 모으면 `None`이다. 비평이 있는데 인용할 id가 없는 것과 대조할 비평이 아예 없는 것은 다른
    일이므로 게이트가 그 둘을 가른다 — `None`으로 돌려야 앞의 것이 아니라 뒤의 것으로 읽힌다.
    """
    current = int(context.iteration)
    if current <= 1:
        return None, "none:first_iteration"
    found: list[dict] = []
    names: list[str] = []
    for number in range(1, current):
        earlier = context.with_iteration(f"{number:03d}")
        if not earlier.critique_path.exists():
            continue
        try:
            found.append(load_json(earlier.critique_path))
        except (OSError, ValueError):
            continue
        names.append(f"iter_{earlier.iteration}")
    if not found:
        return None, f"none:no_critique_before_iter_{context.iteration}"
    return found, "+".join(names)


def violations_from_validation(result: dict, rule: str, where: str) -> list[dict]:
    """검증 REJECT를 위반 목록의 모양으로 옮긴다.

    나쁜 초안은 루프가 다루라고 만든 것이다. 규약 위반으로 run을 끝내면 그 iteration에 남은 예산까지 버린다.
    규칙 id는 검증이 낸 것을 그대로 쓴다 — 같은 위반이 자리마다 다른 이름으로 남으면 집계가 안 된다.
    """
    violations: list[dict] = []
    for error in result.get("errors", []):
        text = str(error)
        prefix = text.split(":", 1)[0].strip()
        # 규칙 카드의 id 모양일 때만 그대로 쓴다. 스키마 오류는 좌표가 매번 달라 id로 쓰면
        # 같은 성질의 실패가 run마다 다른 이름으로 흩어져 원장이 묶어 세지 못한다.
        rule_id = prefix if RULE_ID_PATTERN.match(prefix) else rule
        violations.append(
            {
                "rule": rule_id,
                "point": None,
                "where": where,
                "detail": text,
                "verdict": "violation",
            }
        )
    return violations


def derive_index_from_commit(output_dir: Path, base_commit: str, commit: str) -> list[dict]:
    """커밋 사이의 diff로 색인을 만든다.

    생성기가 낸 색인이 규약을 어겼을 때 쓴다. 실제로 무엇이 움직였는지는 diff가 말하므로, 그것을 색인으로
    쓰면 다음 iteration이 사실 위에서 시작한다. 어긋났다는 사실 자체는 위반으로 이미 남는다.
    """
    listed = git(["diff", "--name-only", base_commit, commit], output_dir).splitlines()
    index: list[dict] = []
    for line in listed:
        rel = line.strip().strip('"')
        if not rel:
            continue
        role = "contract" if rel.replace("\\", "/").startswith(f"{CONTRACT_DIR}/") else "code"
        index.append({"path": rel.replace("\\", "/"), "role": role})
    return index


def build_recovery_draft(
    input_data: dict,
    previous_draft: dict,
    stage_output: dict,
    iteration: str,
    model_name: str,
    commit: str,
    base_commit: str,
    output_dir: Path,
) -> dict:
    """규약을 어긴 refine 산출을 다음 iteration이 다룰 수 있는 초안으로 만든다.

    색인은 diff에서 다시 세우고, 계약 판본 포인터와 판단 기록은 쓸 수 있는 것만 이어받는다.
    판정을 무르는 것이 아니다 — 위반은 다음 iteration의 입력으로 함께 간다.
    """
    index = derive_index_from_commit(output_dir, base_commit, commit)
    version = stage_output.get("contract_version")
    if not (isinstance(version, dict) and isinstance(version.get("path"), str)
            and (output_dir / sanitize_rel_path(version["path"])).is_file()):
        version = previous_draft["contract_version"]
    declared = {entry["path"] for entry in index}
    if version["path"] not in declared:
        index.append({"path": version["path"], "role": "contract"})

    def carried(key: str) -> list:
        value = stage_output.get(key)
        if isinstance(value, list) and all(isinstance(item, dict) for item in value):
            return value
        return previous_draft.get(key, [])

    return {
        "brief_hash": input_data["brief_hash"],
        "iteration": iteration,
        "stage": AGENT_REFINE,
        "files": index,
        "contract_version": version,
        "claims": carried("claims"),
        "decisions": carried("decisions"),
        "contract_changes": carried("contract_changes"),
        "commit": commit,
        "generated_at": now_iso(),
        "model": model_name,
        "metadata": {
            "prompt_version": f"{AGENT_REFINE}_impl:v1",
            "source_files": [f'{input_data["brief_hash"]}_input.json'],
            "index_source": "git-diff-recovery",
        },
    }


def tag_pass(output_dir: Path, iteration: str) -> str:
    """PASS한 iteration의 커밋에 태그를 단다. 그것이 이 run의 산출이다."""
    tag = f"pass-iter-{iteration}"
    git(["tag", "-f", tag], output_dir)
    return tag


def relative_to_run(path: Path, run_dir: Path) -> str:
    try:
        return path.relative_to(run_dir).as_posix()
    except ValueError:
        return str(path)


def next_iteration(iteration: str) -> str:
    return f"{int(iteration) + 1:03d}"


def resolve_contract_file(context: RunContext, rel_path: str) -> Path | None:
    """판본 경로를 실제 파일로 옮긴다. 판본이 작업 폴더에 쌓이므로 앞 판본도 같은 자리에 있다."""
    rel = sanitize_rel_path(rel_path)
    for candidate in (context.output_dir / rel, context.baseline_dir / Path(rel).name):
        if candidate.exists():
            return candidate
    return None


# --- 산출물 조립 --------------------------------------------------------------


def build_draft(
    input_data: dict,
    stage_output: dict,
    iteration: str,
    model_name: str,
    token_usage: dict | None = None,
    source_stage: str = AGENT_GEN,
    commit: str | None = None,
) -> dict:
    metadata = {
        "prompt_version": f"{source_stage}_impl:v1",
        "source_files": [f'{input_data["brief_hash"]}_input.json'],
    }
    if token_usage:
        metadata["token_usage"] = token_usage

    files = normalize_manifest(stage_output["files"])
    draft = {
        "brief_hash": input_data["brief_hash"],
        "iteration": iteration,
        "stage": source_stage,
        "files": files,
        "contract_version": stage_output["contract_version"],
        "claims": stage_output.get("claims", []),
        "decisions": stage_output.get("decisions", []),
        "contract_changes": stage_output.get("contract_changes", []),
        "generated_at": now_iso(),
        "model": model_name,
        "metadata": metadata,
    }
    if commit:
        draft["commit"] = commit
    for optional in ("repairs", "ignored_suggestions"):
        if stage_output.get(optional):
            draft[optional] = stage_output[optional]
    return draft


def mark_unjustified_proposals(critique_output: dict) -> dict:
    """근거가 빈 계약 발의를 표시한다. Refine에 넘기지 않고 리포트에만 남긴다."""
    reviewed = []
    for item in critique_output.get("contract_review", []):
        if not isinstance(item, dict):
            continue
        entry = dict(item)
        if item.get("kind") == "proposal" and not str(item.get("spec_anchor", "")).strip():
            entry["unjustified_proposal"] = True
        reviewed.append(entry)
    return {**critique_output, "contract_review": reviewed}


def build_critique(
    critique_output: dict,
    iteration: str,
    model_name: str,
    token_usage: dict | None = None,
) -> dict:
    metadata = {
        "prompt_version": "critique_impl:v1",
        "source_files": [
            f'{critique_output["brief_hash"]}_input.json',
            f'{critique_output["brief_hash"]}_iter-{iteration}_draft.json',
        ],
    }
    if token_usage:
        metadata["token_usage"] = token_usage

    return {
        **mark_unjustified_proposals(critique_output),
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
        "prompt_version": "eval_impl:v1",
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


def unmeasured_axes(eval_data: dict, rubric: dict | None) -> list[str]:
    """채점자가 재지 못했다고 신고한 축. 루브릭에 있는 이름만 센다.

    측정하지 못한 것은 판정이 아니다. 게이트 공백을 통과로 세지 않는 것과 같은 이유로, 보지 못한 것을
    최저점으로도 세지 않는다. 도구가 실패해 코드를 못 읽은 것이 1점이 되면 점수가 사실과 어긋난다.
    """
    declared = eval_data.get("unmeasured_axes")
    if not isinstance(declared, list):
        return []
    axes = set((rubric or {}).get("axes", {}).keys())
    return [axis for axis in declared if isinstance(axis, str) and (not axes or axis in axes)]


def is_unmeasured_threshold_error(error: str, unmeasured: list[str]) -> bool:
    """이 하한 오류가 측정하지 못한 축에서 나온 것인가.

    `min_axis.<축>`은 그 축이 측정 불가일 때만 뺀다. 잰 축이 하한을 못 넘긴 것은 진짜 판정이다.
    `min_total`은 측정하지 못한 축의 점수까지 섞어 계산한 값이라 어느 축이 끌어내렸는지 가를 수 없으므로
    측정 불가가 하나라도 있으면 뺀다.
    """
    if not unmeasured:
        return False
    if error.startswith("min_total"):
        return True
    if error.startswith("min_axis."):
        axis = error[len("min_axis."):].split(":", 1)[0].strip()
        return axis in unmeasured
    return False


def get_weak_axes(eval_data: dict, rubric: dict | None) -> list[str]:
    scores = eval_data.get("rubric_scores", {}).get("scores", {})
    min_axis = (rubric or {}).get("thresholds", {}).get("min_axis", {})
    weak_axes = []
    if not isinstance(scores, dict) or not isinstance(min_axis, dict):
        return weak_axes
    for axis, minimum in min_axis.items():
        score = scores.get(axis)
        if isinstance(score, (int, float)) and isinstance(minimum, (int, float)) and score < minimum:
            weak_axes.append(axis)
    return weak_axes


def compute_decision_risk(
    decisions: list[dict],
    contract_changes: list[dict],
    extra_reasons: list[str] | None = None,
) -> dict:
    """통과한 run이 어떤 판단 위에 서 있는지 드러내는 표시. 게이트가 아니다.

    되돌리는 비용은 자유 문장이라 기계로 크기를 재지 못하므로 여기서 쓰지 않고 리포트가 보여 준다.
    기계로 읽히는 것은 신뢰도가 낮은 결정과 깨는 계약 변경이다.
    """
    reasons = list(extra_reasons or [])
    breaking = [c.get("id") for c in contract_changes if isinstance(c, dict) and c.get("compatibility") == "breaking"]
    if breaking:
        reasons.append(f"breaking contract changes: {breaking}")
    low_confidence = [d.get("id") for d in decisions if isinstance(d, dict) and d.get("confidence") == "low"]
    if low_confidence:
        reasons.append(f"low confidence decisions: {low_confidence}")

    if reasons:
        level = "high"
    elif contract_changes:
        level = "medium"
    elif decisions:
        level = "low"
    else:
        level = "none"
    return {"level": level, "reasons": reasons}


def split_critique_signals(critique_artifact: dict) -> dict:
    """비평을 구현 지시와 계약 발의로 가른다. 갈라 두지 않으면 계약 발의가 구현 지시로 읽힌다."""
    code_instructions: list[dict] = []
    contract_proposals: list[dict] = []
    for item in critique_artifact.get("weaknesses", []):
        if not isinstance(item, dict):
            continue
        target = item.get("target")
        scope = target.get("scope") if isinstance(target, dict) else None
        entry = {
            "id": item.get("id"),
            "axis": item.get("axis"),
            "issue": item.get("issue"),
            "suggestion": item.get("suggestion"),
            "severity": item.get("severity"),
            "target": target,
        }
        if scope == "contract":
            contract_proposals.append(entry)
        else:
            code_instructions.append(entry)

    for item in critique_artifact.get("contract_review", []):
        if not isinstance(item, dict) or item.get("kind") != "proposal":
            continue
        if item.get("unjustified_proposal"):
            continue
        contract_proposals.append(
            {
                "id": item.get("id"),
                "pointer": item.get("pointer"),
                "finding": item.get("finding"),
                "proposed_change": item.get("proposed_change"),
                "action": item.get("action"),
                "spec_anchor": item.get("spec_anchor"),
                "severity": item.get("severity"),
            }
        )
    return {"code_instructions": code_instructions, "contract_proposals": contract_proposals}


def index_rule_cards(rules: dict | None) -> dict[str, dict]:
    """규칙 카드를 id로 색인한다. 카드는 `rules: [{id, meaning, direction, verdict, handling}]` 목록이다."""
    cards: dict[str, dict] = {}
    for card in ((rules or {}).get("rules") or []):
        if isinstance(card, dict) and card.get("id"):
            cards[card["id"]] = card
    return cards


def cause_id(rule: object, where: object, detail: object) -> str:
    """원인의 안정된 id. `(rule, where, detail)`에서 결정적으로 뽑는다.

    같은 원인이 다음 iteration에도 같은 id를 받아야 원장이 "이 원인이 두 iteration 연속 열려 있다"를
    말할 수 있다. 그래서 iteration마다 다시 세는 순번을 쓰지 않는다. 모델은 이 값을 옮기기만 하면 되고
    조립할 것이 없다 — 조립하는 자리는 틀리는 자리다.
    """
    material = "\x00".join("" if part is None else str(part) for part in (rule, where, detail))
    return f"c_{hashlib.sha256(material.encode('utf-8')).hexdigest()[:8]}"


def stamp_cause_ids(items: list[dict]) -> list[dict]:
    """판정 항목마다 그것이 속한 원인의 id를 붙인다. 원장과 refine이 같은 이름으로 부르게 한다."""
    for item in items:
        if isinstance(item, dict):
            item["cause_id"] = cause_id(item.get("rule"), item.get("where"), item.get("detail"))
    return items


def group_by_cause(violations: list[dict]) -> list[dict]:
    """판정 지점 단위의 위반을 원인 단위로 묶는다.

    판정과 집계는 지점 단위로 본다. 계약이 같은 컴포넌트를 여러 응답에서 참조하면 그 하나의 결함이 지점마다
    한 번씩 나오는데, 그것을 그대로 refine에 넘기면 신호가 잡음이 된다. 같은 규칙과 같은 자리와 같은 차이는
    한 원인이므로 `(rule, where, detail)`로 묶고 닿는 지점의 목록을 함께 싣는다.
    원장에는 지점 단위 그대로 간다 — 어느 지점이 아직 열려 있는지는 지점 단위로만 말할 수 있다.
    """
    causes: dict[tuple, dict] = {}
    for violation in violations:
        if not isinstance(violation, dict):
            continue
        key = (violation.get("rule"), violation.get("where"), violation.get("detail"))
        cause = causes.get(key)
        if cause is None:
            cause = {
                "id": cause_id(*key),
                "rule": violation.get("rule"),
                "where": violation.get("where"),
                "detail": violation.get("detail"),
                "points": [],
                "point_count": 0,
                "evidence_count": 0,
            }
            if violation.get("label"):
                cause["label"] = violation["label"]
            causes[key] = cause
        point = violation.get("point")
        if point is not None and point not in cause["points"]:
            cause["points"].append(point)
        cause["point_count"] += 1
        cause["evidence_count"] += max(1, len(violation.get("evidence") or []))
    return sorted(causes.values(), key=lambda item: (-item["point_count"], str(item["rule"]), str(item["where"])))


def collect_rule_cards(violations: list[dict], rules: dict | None) -> dict[str, str]:
    """위반에 붙은 규칙 id의 고정 문장만 뽑는다. 위반 문장을 매번 새로 쓰면 집계가 안 된다."""
    cards = index_rule_cards(rules)
    selected: dict[str, str] = {}
    for violation in violations:
        rule_id = violation.get("rule") if isinstance(violation, dict) else None
        if not rule_id or rule_id in selected:
            continue
        card = cards.get(rule_id)
        if card:
            selected[rule_id] = str(card.get("meaning") or "")
    return selected


def build_refine_request(
    input_data: dict,
    draft: dict,
    critique_artifact: dict | None,
    eval_artifact: dict | None,
    gate_result: dict,
    rubric: dict | None,
    rules: dict,
    to_iteration: str,
) -> dict:
    weak_axes = get_weak_axes(eval_artifact or {}, rubric)
    axis_rationales = (eval_artifact or {}).get("axis_rationales", {})
    weak_axis_rationales = {
        axis: axis_rationales[axis]
        for axis in weak_axes
        if isinstance(axis_rationales, dict) and axis in axis_rationales
    }
    signals = split_critique_signals(critique_artifact or {})
    violations = gate_result.get("violations", [])
    causes = group_by_cause(violations)
    # 원인은 id 하나로만 부른다. 두 이름으로 부르면 모델이 어느 것을 옮겨야 하는지 몰라 다시 조립한다.
    priority = [cause["id"] for cause in causes]
    priority += [
        f"contract:{item.get('id')}" for item in signals["contract_proposals"] if isinstance(item, dict)
    ]
    priority += [
        f"code:{item.get('id')}"
        for item in signals["code_instructions"]
        if isinstance(item, dict) and item.get("severity") == "high"
    ]
    priority += [f"axis:{axis}" for axis in weak_axes]

    return {
        "brief_hash": input_data["brief_hash"],
        "from_iteration": draft["iteration"],
        "to_iteration": to_iteration,
        "priority_order": REFINE_PRIORITY_ORDER,
        # 열린 위반은 원인 단위다. 지점 단위 목록은 원장이 들고 간다.
        "open_causes": causes,
        "open_cause_count": len(causes),
        "open_point_count": sum(cause["point_count"] for cause in causes),
        "rule_cards": collect_rule_cards(violations, rules),
        "gate_observations": gate_result.get("observations", []),
        "gate_coverage": {
            "g0": gate_result.get("g0", STATUS_SKIPPED),
            "g1": gate_result.get("g1", STATUS_SKIPPED),
            "contract_changes": gate_result.get("changes", STATUS_SKIPPED),
            "allow_missing_gates": bool(gate_result.get("allow_missing_gates")),
        },
        "current_contract_version": draft["contract_version"],
        "standing_decisions": draft.get("decisions", []),
        "contract_changes": draft.get("contract_changes", []),
        "contested_decisions": (critique_artifact or {}).get("contested_decisions", []),
        "code_instructions": signals["code_instructions"],
        "contract_proposals": signals["contract_proposals"],
        "weak_axes": weak_axes,
        "weak_axis_rationales": weak_axis_rationales,
        "priority": priority,
    }


def build_repair_request(
    input_data: dict,
    draft: dict,
    failures: list[dict],
    rules: dict | None,
    iteration: str,
    attempt: int,
    max_attempts: int,
) -> dict:
    """빌드 수리 시도의 입력. 빌드 실패 문장뿐이다.

    계약 적합성은 아직 판정되지 않았으므로 그 자리에서 고칠 것이 없고, 빌드를 고치면서 구현을 크게 바꾸면
    무엇이 무엇을 고쳤는지 원장이 가를 수 없다. 그래서 열린 적합성 위반과 비평과 약한 축은 넣지 않는다.
    현재 계약 판본 포인터만 함께 준다 — 색인이 그것을 선언해야 하고, 수리가 계약을 건드릴 일은 없다.
    """
    return {
        "brief_hash": input_data["brief_hash"],
        "iteration": iteration,
        "purpose": "build_repair",
        "attempt": attempt,
        "max_attempts": max_attempts,
        "scope": "build_only",
        "instruction": (
            "컴파일과 부팅만 고쳐라. 계약 적합성은 아직 판정되지 않았으므로 그 자리를 건드리지 마라. "
            "구현을 크게 바꾸지 말고 빌드 실패의 원인만 고쳐라. 계약 판본은 그대로 둔다."
        ),
        "priority_order": ["gate_violations"],
        "open_causes": group_by_cause(failures),
        "rule_cards": collect_rule_cards(failures, rules),
        "current_contract_version": draft["contract_version"],
    }


def amend_draft_for_repair(
    draft: dict,
    stage_output: dict,
    commit: str,
    base_commit: str,
    output_dir: Path,
    refine_valid: bool,
    attempts: int,
) -> dict:
    """수리한 것을 같은 iteration의 초안에 합친다.

    수리는 iteration 안의 일이므로 새 iteration을 열지 않고 이 iteration의 색인에 더한다. 색인이 규약을
    어겼으면 실제로 무엇이 움직였는지는 diff가 말하므로 거기서 세운다.
    """
    if refine_valid and isinstance(stage_output.get("files"), list):
        added = normalize_manifest(stage_output["files"])
    else:
        added = derive_index_from_commit(output_dir, base_commit, commit)

    merged: list[dict] = list(draft.get("files", []))
    declared = {entry["path"] for entry in merged}
    for entry in added:
        if entry["path"] not in declared:
            merged.append(entry)
            declared.add(entry["path"])

    amended = {**draft, "files": merged, "commit": commit}
    metadata = dict(amended.get("metadata", {}))
    metadata["build_attempts"] = attempts
    if not refine_valid:
        metadata["index_source"] = "git-diff-recovery"
    amended["metadata"] = metadata
    return amended


def build_final(
    context: RunContext,
    input_data: dict,
    draft: dict,
    eval_artifact: dict,
    gate_result: dict,
    rubric: dict | None,
    contract_lineage: dict[str, list[str]],
    refine_request_lineage: str | None,
) -> dict:
    lineage = {
        "run_id": context.run_id,
        "input": relative_to_run(context.copied_input_path, context.run_dir),
        "origin": contract_lineage.get("origin", []),
        "baseline": contract_lineage.get("baseline", []),
        "draft": relative_to_run(context.draft_path, context.run_dir),
        "critique": relative_to_run(context.critique_path, context.run_dir),
        "eval": relative_to_run(context.eval_path, context.run_dir),
        "gate": relative_to_run(context.gate_path, context.run_dir),
        "output": relative_to_run(context.output_dir, context.run_dir),
    }
    if refine_request_lineage:
        lineage["refine_request"] = refine_request_lineage

    rubric_scores = eval_artifact["rubric_scores"]
    return {
        "brief_hash": input_data["brief_hash"],
        "final_iteration": context.iteration,
        "files": draft["files"],
        "commit": draft.get("commit", ""),
        "tag": f"pass-iter-{context.iteration}",
        "contract_version": draft["contract_version"],
        "decisions": draft.get("decisions", []),
        "contract_changes": draft.get("contract_changes", []),
        "decision_risk": compute_decision_risk(
            draft.get("decisions", []),
            draft.get("contract_changes", []),
        ),
        "accepted_at": now_iso(),
        "quality_snapshot": {
            "rubric_name": eval_artifact["rubric_name"],
            "weighted_total": rubric_scores["weighted_total"],
            "scores": rubric_scores["scores"],
            "weak_axes": get_weak_axes(eval_artifact, rubric),
        },
        "gate_result": {
            "g0": gate_result.get("g0", STATUS_SKIPPED),
            "g1": gate_result.get("g1", STATUS_SKIPPED),
            "changes": gate_result.get("changes", STATUS_SKIPPED),
            "allow_missing_gates": bool(gate_result.get("allow_missing_gates")),
            "violations": gate_result.get("violations", []),
            "observations": gate_result.get("observations", []),
        },
        "contract_result": {
            "verdict": STATUS_PASS,
            "contract_errors": [],
            "checked_rules": FINAL_CHECKED_RULES,
        },
        "lineage": lineage,
    }


# --- 게이트 호출 -------------------------------------------------------------


def baseline_contract_path(context: RunContext, input_data: dict) -> Path | None:
    """이 run의 기준선 계약 파일. 판본 대조의 분모다."""
    contracts = input_data["brief"]["contracts"]
    if not contracts:
        return None
    candidate = context.baseline_dir / Path(contracts[0]["path"]).name
    return candidate if candidate.exists() else None


def accumulated_contract_changes(context: RunContext, draft: dict) -> list[dict]:
    """iteration 001부터 이번까지의 계약 변경 기록 전부.

    기준선 대비 차이는 한 iteration의 기록으로 덮이지 않는다. iteration 002가 기준선에서 멀어진 자리를
    001이 신고했으면 그 신고가 여전히 근거이므로, 누적으로 넘겨야 옳게 신고한 변경이 위반으로 뒤집히지 않는다.
    """
    history: list[dict] = []
    current = int(context.iteration)
    for number in range(1, current):
        draft_path = context.with_iteration(f"{number:03d}").draft_path
        if not draft_path.exists():
            continue
        try:
            earlier = load_json(draft_path)
        except (OSError, ValueError):
            continue
        for record in earlier.get("contract_changes", []) or []:
            if isinstance(record, dict):
                history.append(record)
    for record in draft.get("contract_changes", []) or []:
        if isinstance(record, dict):
            history.append(record)
    return history


def skip_or_fail(
    result: dict,
    label: str,
    reason: str,
    allow_missing: bool,
    progress: ProgressReporter,
    iteration: str,
) -> None:
    """판정 장치가 없는 자리를 처리한다. 허용하지 않으면 미판정은 통과가 아니라 ERROR다."""
    if not allow_missing:
        raise GateEnvironmentError(f"{label} cannot judge: {reason}")
    result[label] = STATUS_SKIPPED
    result["log"].append(f"{label} {STATUS_SKIPPED}: {reason}")
    progress.line(f"iter {iteration} {label} {STATUS_SKIPPED} reason={reason}")


def run_gate_stage(
    context: RunContext,
    draft: dict,
    input_data: dict,
    rules: dict | None,
    java_home: str | None,
    allow_missing_gates: bool,
    progress: ProgressReporter,
) -> dict:
    """G0 → G1 → 판본 대조를 차례로 부른다. 셋이 같은 규칙 카드를 본다.

    판정 장치가 없으면 기본은 ERROR다. 게이트 공백은 통과가 아니다. `--allow-missing-gates`를 붙인
    개발용 run에서만 SKIPPED로 진행하고, 그 사실을 결과에 남겨 통과로 읽히지 않게 한다.
    """
    result: dict = {
        "iteration": context.iteration,
        "g0": STATUS_SKIPPED,
        "g1": STATUS_SKIPPED,
        "changes": STATUS_SKIPPED,
        "violations": [],
        "observations": [],
        "errors": [],
        "derived_spec": None,
        "allow_missing_gates": allow_missing_gates,
        "log": [],
    }

    output_dir = context.output_dir

    gates = load_gates_module(allow_missing=allow_missing_gates)
    if gates is None:
        result["log"].append("gates module not importable or incomplete")
        progress.line(f"iter {context.iteration} gate {STATUS_SKIPPED} reason=gates_module_missing")
        return result
    if rules is None:
        result["log"].append("rule cards unavailable — 판정값을 카드에서 읽지 못한다")

    if output_dir.exists() or not allow_missing_gates:
        # project_dir은 작업 폴더다. 제자리에서 빌드하므로 target/이 남아 다음 iteration이 증분 빌드된다.
        # 추출 스펙은 work_dir(iter_00N/)에 떨어진다. output/target/에 두면 mvn clean이 지우거나
        # 다음 iteration이 덮어서 원장이 iteration마다 되짚을 수 없다.
        g0 = gates.run_g0(
            project_dir=output_dir,
            work_dir=context.iter_dir,
            java_home=java_home,
        )
        result["g0"] = g0.get("status", STATUS_ERROR)
        result["derived_spec"] = str(g0.get("derived_spec")) if g0.get("derived_spec") else None
        result["log"].append(str(g0.get("log", "")))
        if result["g0"] == STATUS_ERROR:
            # 환경 문제는 초안의 잘못이 아니다. 위반으로 세지 않고 run을 ERROR로 끝낸다.
            result["errors"] += list(g0.get("violations", []))
            return result
        result["violations"] += list(g0.get("violations", []))
    else:
        skip_or_fail(
            result, "g0", f"work tree not found: {output_dir}", allow_missing_gates, progress, context.iteration
        )

    contract_rel = draft["contract_version"]["path"]
    contract_file = resolve_contract_file(context, contract_rel)
    if contract_file is None:
        raise GateEnvironmentError(
            f"contract version file not found: {contract_rel} — 대조할 판본이 없으면 판정할 수 없다"
        )
    contract = load_structured(contract_file)

    derived_spec_path = result.get("derived_spec")
    if result["g0"] == STATUS_REJECT:
        # 빌드가 깨지면 추출 스펙이 없으므로 G1은 돌 수 없다. 판정 장치가 없는 것이 아니라 대조할 것이
        # 없는 것이라서 ERROR가 아니다. 판본 대조는 빌드가 필요 없으니 아래에서 그대로 돈다 —
        # 빌드가 깨진 초안이 계약을 고쳐도 그 변경은 판정되어야 한다.
        result["g1"] = STATUS_SKIPPED
        result["log"].append(f"g1 {STATUS_SKIPPED}: g0_reject (추출 스펙이 없다)")
        progress.line(f"iter {context.iteration} g1 {STATUS_SKIPPED} reason=g0_reject")
    elif derived_spec_path:
        g1 = gates.run_g1(
            derived_spec=load_json(Path(derived_spec_path)),
            contract=contract,
            scope=input_data["brief"]["scope"]["operation_ids"],
            decisions=draft.get("decisions", []),
            rules=rules or {},
            project_dir=output_dir,
        )
        g1_violations = list(g1.get("violations", []))
        result["violations"] += g1_violations
        result["observations"] += list(g1.get("observations", []))
        result["g1"] = STATUS_REJECT if g1_violations else STATUS_PASS
    else:
        skip_or_fail(result, "g1", "derived_spec unavailable", allow_missing_gates, progress, context.iteration)

    prev_rel = draft["contract_version"]["from"]
    prev_file = resolve_contract_file(context, prev_rel)
    if prev_file is not None:
        # 넘기는 비평은 **이 판본이 만들어지기 전까지 이 run이 낸 것 전부**다. 계약 변경은 iteration을
        # 넘어 살아남으므로 iter_002의 변경을 003이 들고 가면 그 근거는 001이나 002의 비평에 있다.
        # 앞 하나만 넘기면 그 기록이 전부 "이 iteration의 비평에 없다"로 뒤집혀, 정직하게 근거를 댄 쪽이
        # 위반이 된다. 반대로 같은 iteration의 비평까지 넣으면 결정이 내려질 때 없던 정보로 그 결정을
        # 정당화하게 되므로 이번 것은 빼고, iteration 001과 비평이 하나도 없는 run은 None이다.
        baseline_path = baseline_contract_path(context, input_data)
        baseline_contract = load_structured(baseline_path) if baseline_path is not None else None
        accumulated = accumulated_contract_changes(context, draft)
        result["baseline_compared"] = baseline_contract is not None
        result["accumulated_changes"] = len(accumulated)
        if baseline_contract is None:
            # 기준선을 못 읽으면 from과 path가 같은 iteration에 대조가 하나도 없게 된다. 그 자리가 뚫렸던 구멍이다.
            progress.line(f"iter {context.iteration} contract_changes baseline 없음 — 기준선 대비 대조를 못 한다")
        source_critique, source_label = load_source_critique(context)
        result["critique_source"] = source_label
        progress.line(f"iter {context.iteration} contract_changes critique_source={source_label}")
        changes = gates.check_contract_changes(
            prev_contract=load_structured(prev_file),
            curr_contract=contract,
            changes=draft.get("contract_changes", []),
            prev_path=prev_rel,
            curr_path=contract_rel,
            rules=rules,
            critique=source_critique,
            baseline=baseline_contract,
            accumulated=accumulated,
            # 명세 원문을 넘겨 `spec_anchor`가 실재하는 요구사항 id를 가리키는지 값으로 대조하게 한다.
            # 넘기지 않으면 빈 문자열만 아니면 통과하는 옛 동작으로 떨어지고, 줄이려는 계약이 줄이는 근거가
            # 되는 문자열(`contract:security` 같은 것)이 지나간다. 판정하지 못한 것이 통과로 보이는 자리다.
            spec=input_data.get("brief", {}).get("requirement_spec"),
        )
        change_violations = list(changes.get("violations", []))
        result["violations"] += change_violations
        result["observations"] += list(changes.get("observations", []))
        result["changes"] = STATUS_REJECT if change_violations else STATUS_PASS
    else:
        skip_or_fail(
            result,
            "changes",
            f"previous contract version not found: {prev_rel}",
            allow_missing_gates,
            progress,
            context.iteration,
        )

    return result


def repair_build(
    context: RunContext,
    draft: dict,
    gate_result: dict,
    input_data: dict,
    rules: dict | None,
    args: argparse.Namespace,
    client,
    agent_models: dict,
    immutable_paths: list[str],
    progress: ProgressReporter,
) -> tuple[dict, dict, list[dict]]:
    """G0가 깨진 iteration에서 빌드만 고치는 시도를 몇 번 한다.

    이 시도는 `max_iterations`에서 세지 않는다. iteration 예산은 계약 적합성을 좁히는 데 쓰는 것이고
    컴파일을 맞추는 데 쓰는 것이 아니다. 하나를 고치면 다음 하나가 드러나는 모양이라, 밖으로 돌리지 않으면
    적합성 판정에 한 번도 닿지 못한다.

    시도마다 커밋을 남겨 이력이 끊기지 않게 하고 실패 문장을 기록한다. 수리에 몇 번이 걸렸는지가
    초안의 품질 신호다. 정한 횟수를 넘기면 G0가 깨진 그대로 돌려주어 부르는 쪽이 그 iteration을 닫는다.
    """
    attempts: list[dict] = []
    label = f"iter {context.iteration}"
    while gate_result.get("g0") == STATUS_REJECT and len(attempts) < args.max_build_attempts:
        attempt = len(attempts) + 1
        failures = [
            violation
            for violation in gate_result.get("violations", [])
            if str(violation.get("rule", "")).startswith("g0.")
        ]
        summary = "; ".join(str(v.get("detail", ""))[:160] for v in failures) or "(빌드 실패 문장이 없다)"
        progress.line(f"{label} build repair attempt={attempt}/{args.max_build_attempts} {summary[:200]}")

        request = build_repair_request(
            input_data=input_data,
            draft=draft,
            failures=failures,
            rules=rules,
            iteration=context.iteration,
            attempt=attempt,
            max_attempts=args.max_build_attempts,
        )
        write_json(context.iter_dir / f"build-repair-{attempt}.request.json", request, overwrite=True)

        head_before = head_commit(context.output_dir)
        with tempfile.TemporaryDirectory(prefix="contract-impl-build-repair-") as temp_dir:
            temp_output = Path(temp_dir) / "refine-output.json"
            with progress.step(
                f"{label} build repair {attempt} model={display_model(agent_models[AGENT_REFINE], args.provider)}",
                live=True,
            ):
                refine(
                    input_path=context.copied_input_path,
                    draft_path=context.draft_path,
                    critique_path=None,
                    refine_request=request,
                    output_path=temp_output,
                    client=client,
                    model=agent_models[AGENT_REFINE],
                    work_dir=context.output_dir,
                    raw_path=context.iter_dir / f"build-repair-{attempt}.raw.txt",
                    on_note=lambda note: progress.line(note),
                )
            normalize_and_log(
                temp_output, "repair_output", f"{label} build repair {attempt}", progress,
                work_dir=context.output_dir,
            )
            repair_result = validate_file(
                temp_output,
                # 수리 산출은 부분 색인이다. 고친 파일만 적으면 된다.
                artifact="repair_output",
                immutable_paths=immutable_paths,
                work_dir=context.output_dir,
                changed_paths=changed_paths(context.output_dir),
            )
            progress.validation(f"{label} build_repair_{attempt}_validate", repair_result)
            try:
                repair_output = load_json(temp_output)
            except Exception:
                repair_output = {}

        for hash_value in detect_generator_commits(context.output_dir, head_before):
            progress.line(f"{label} generator_commit {hash_value[:10]} — 커밋은 runner의 일이다")
        commit = commit_iteration(context.output_dir, context.iteration, f"build-repair-{attempt}")
        draft = amend_draft_for_repair(
            draft=draft,
            stage_output=repair_output,
            commit=commit,
            base_commit=head_before,
            output_dir=context.output_dir,
            refine_valid=repair_result["status"] == STATUS_PASS,
            attempts=attempt,
        )
        write_json(context.draft_path, draft, overwrite=True)
        lock_contract_versions(context.output_dir)

        attempts.append(
            {
                "attempt": attempt,
                "commit": commit,
                "index_declared": repair_result["status"] == STATUS_PASS,
                "index_errors": repair_result.get("errors", []),
                "failures": failures,
            }
        )

        gate_result = run_gate_stage(
            context=context,
            draft=draft,
            input_data=input_data,
            rules=rules,
            java_home=args.java_home,
            allow_missing_gates=args.allow_missing_gates,
            progress=progress,
        )
        gate_result["build_attempts"] = attempts
        stamp_cause_ids(gate_result.get("violations", []))
        stamp_cause_ids(gate_result.get("observations", []))
        write_json(context.gate_path, gate_result, overwrite=True)
        raise_on_gate_error(gate_result, context.gate_path)
        progress.line(
            f"{label} build repair attempt={attempt} -> g0={gate_result['g0']} "
            f"violations={len(gate_result.get('violations', []))}"
        )

    if attempts:
        gate_result["build_attempts"] = attempts
        if gate_result.get("g0") == STATUS_REJECT:
            progress.line(
                f"{label} build repair 한도 {args.max_build_attempts}회를 넘겼다 — 이 iteration을 REJECT로 닫는다"
            )
    return gate_result, draft, attempts


def gate_verdict(gate_result: dict) -> str:
    if gate_result.get("errors"):
        return STATUS_ERROR
    return STATUS_REJECT if gate_result.get("violations") else STATUS_PASS


def raise_on_gate_error(gate_result: dict, gate_path: Path) -> None:
    """G0가 환경 문제로 ERROR를 냈으면 run을 거기서 끝낸다. 고칠 수 없는 것을 refine에 보내지 않는다."""
    errors = gate_result.get("errors") or []
    if not errors:
        return
    summary = "; ".join(f"{e.get('rule')}@{e.get('where')}: {e.get('detail')}" for e in errors if isinstance(e, dict))
    raise GateEnvironmentError(f"gate could not judge (see {gate_path}): {summary}")


def record_ledger_and_report(
    context: RunContext,
    payload: dict,
    progress: ProgressReporter,
) -> dict | None:
    """원장과 리포트는 다른 조각이 소유한다. 없거나 터지면 건너뛴다.

    원장과 리포트는 기록 장치다. 판정은 이미 게이트와 루브릭이 끝냈으므로, 기록하다 터진 것으로 판정된 run을
    버리지 않는다. 그래서 여기서만 예외를 삼키고 그 사실을 로그와 결과에 남긴다. 삼킨 예외의 트레이스백도
    함께 적어 어디가 터졌는지 알 수 있게 한다 — 기록 장치의 고장도 고쳐야 할 것이다.
    """
    ledger = None
    errors: list[dict] = []

    update_ledger = load_ledger_writer()
    if update_ledger is None:
        progress.line(f"iter {context.iteration} ledger {STATUS_SKIPPED} reason=ledger_module_missing")
    else:
        try:
            ledger = update_ledger(run_dir=context.run_dir, iteration=context.iteration, payload=payload)
        except Exception as exc:  # noqa: BLE001 — 기록의 실패가 판정을 버리게 하지 않는다
            errors.append({"writer": "ledger", "error_type": type(exc).__name__, "message": str(exc)})
            progress.line(
                f"iter {context.iteration} ledger ERROR {type(exc).__name__}: {str(exc)[:160]} "
                "— 기록은 건너뛰고 판정은 그대로 간다"
            )

    write_report = load_report_writer()
    if write_report is None:
        progress.line(f"iter {context.iteration} report {STATUS_SKIPPED} reason=report_module_missing")
    else:
        try:
            write_report(run_dir=context.run_dir, ledger=ledger or {}, result=payload)
        except Exception as exc:  # noqa: BLE001
            errors.append({"writer": "report", "error_type": type(exc).__name__, "message": str(exc)})
            progress.line(
                f"iter {context.iteration} report ERROR {type(exc).__name__}: {str(exc)[:160]} "
                "— 기록은 건너뛰고 판정은 그대로 간다"
            )

    if errors:
        write_json(
            context.iter_dir / "record.errors.json",
            {
                "iteration": context.iteration,
                "recorded_at": now_iso(),
                "errors": errors,
                "traceback": traceback.format_exc(),
            },
            overwrite=True,
        )
    return ledger


# --- 실패 기록 ---------------------------------------------------------------



def normalize_and_log(
    path: Path,
    artifact: str,
    label: str,
    progress: ProgressReporter,
    work_dir: Path | None = None,
) -> list[dict]:
    """모델 산출을 검증용 표현으로 맞추고 고친 자리를 로그에 남긴다.

    고친 기록을 돌려주어 원장과 리포트가 "무엇을 자동으로 고쳤는지" 말할 수 있게 한다. 위반이 아니라
    정규화이므로 판정에는 들어가지 않는다.
    """
    records = normalize_model_output(path, artifact, work_dir)
    for record in records:
        if record["kind"] == "version_path_repaired":
            progress.line(
                f"{label} normalized {record['pointer']}: {record['from']!r} -> {record['to']!r} "
                "(판본 경로의 철자를 실재하는 파일로 맞췄다)"
            )
    pruned = [record["pointer"] for record in records if record["kind"] == "null_pruned"]
    if pruned:
        progress.line(f"{label} normalized null fields: {pruned[:6]}" + (" ..." if len(pruned) > 6 else ""))
    return records


def failure_counts(rejections: list[dict]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for rejection in rejections:
        for error in rejection.get("errors", []):
            category = categorize_failure(str(error))
            counts[category] = counts.get(category, 0) + 1
    return counts


def iteration_progress(run_dir: Path) -> list[dict]:
    """run 폴더에 남은 iteration별 진행 상황.

    단계 실행이 중간에 멈추면 그 iteration은 `rejections`에 들어가지 않는다. 그때 어디까지 갔는지 알려면
    iteration마다 남은 `gate.json`을 읽어야 하는데, 그 일을 사람이 손으로 하게 두면 실패 기록의 값이 없다.
    """
    progress_rows: list[dict] = []
    for iter_dir in sorted(run_dir.glob("iter_*")):
        gate_path = iter_dir / "gate.json"
        if not gate_path.exists():
            progress_rows.append({"iteration": iter_dir.name.removeprefix("iter_"), "gate": "not reached"})
            continue
        try:
            gate = json.loads(gate_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        row = {
            "iteration": gate.get("iteration", iter_dir.name.removeprefix("iter_")),
            "g0": gate.get("g0"),
            "g1": gate.get("g1"),
            "changes": gate.get("changes"),
            "violations": len(gate.get("violations", [])),
            "observations": len(gate.get("observations", [])),
            "build_attempts": len(gate.get("build_attempts", [])),
            "rules": sorted({str(v.get("rule")) for v in gate.get("violations", [])}),
        }
        for name, key in (("critique.json", "critique"), ("eval.json", "eval")):
            row[key] = (iter_dir / name).exists()
        progress_rows.append(row)
    return progress_rows


def write_failed(
    run_dir: Path,
    brief_hash: str,
    run_id: str,
    stage: str,
    error: Exception,
    lineage: dict[str, str],
    config: dict[str, object],
    last_iteration: str | None = None,
    rejections: list[dict] | None = None,
) -> Path:
    failed_path = run_dir / f"{brief_hash}_failed.json"
    payload = {
        "brief_hash": brief_hash,
        "run_id": run_id,
        "failed_at": now_iso(),
        "terminal_reason": "gate_environment" if isinstance(error, GateEnvironmentError) else "stage_error",
        "stage": stage,
        "last_iteration": last_iteration,
        "error_type": type(error).__name__,
        "message": str(error),
        # 진단할 수 없는 실패 기록은 값이 없다. 어느 줄에서 멈췄는지 여기서만 알 수 있다.
        "traceback": "".join(traceback.format_exception(type(error), error, error.__traceback__)),
        "failure_counts_by_category": failure_counts(rejections or []),
        "iteration_progress": iteration_progress(run_dir),
        "iteration_rejections": rejections or [],
        "config": config,
        "lineage": lineage,
    }
    write_json(failed_path, payload, overwrite=True)
    return failed_path


def categorize_failure(error: str) -> str:
    if error.startswith("schema "):
        return "schema_error"
    if error.startswith("manifest."):
        return "manifest_violation"
    if error.startswith("gate:"):
        return "gate_violation"
    if "must not include" in error:
        return "role_boundary_violation"
    if error.startswith("min_total") or error.startswith("min_axis"):
        return "quality_reject"
    return "contract_error"


def failure_rule(error: str) -> str:
    if ":" in error:
        return error.split(":", 1)[0]
    return error


def write_max_iteration_failed(
    context: RunContext,
    rejections: list[dict],
    config: dict[str, object],
    decision_risk: dict,
) -> Path:
    last_rejection = rejections[-1] if rejections else {}
    last_errors = last_rejection.get("errors", [])

    payload = {
        "brief_hash": context.brief_hash,
        "run_id": context.run_id,
        "failed_at": now_iso(),
        "terminal_reason": "max_iteration_exceeded",
        "last_iteration": context.iteration,
        "decision_risk": decision_risk,
        "failure_counts_by_category": failure_counts(rejections),
        "iteration_progress": iteration_progress(context.run_dir),
        "last_failures": [
            {
                "category": categorize_failure(str(error)),
                "rule": failure_rule(str(error)),
                "severity": "high",
                "retryable": False,
                "message": str(error),
            }
            for error in last_errors
        ],
        "lineage": {
            "input": relative_to_run(context.copied_input_path, context.run_dir),
            "last_draft": relative_to_run(context.draft_path, context.run_dir),
            "last_gate": relative_to_run(context.gate_path, context.run_dir),
            "last_critique": relative_to_run(context.critique_path, context.run_dir),
            "last_eval": relative_to_run(context.eval_path, context.run_dir),
        },
        "iteration_rejections": rejections,
        "config": config,
        "next_actions": [
            "게이트 위반이 남았으면 계약이 그 자리를 무엇이라 말하는지 먼저 확인한다",
            "반복 실패의 주된 category를 보고 프롬프트나 규칙 카드를 조정한다",
        ],
    }
    write_json(context.failed_path, payload, overwrite=True)
    return context.failed_path


# --- 파이프라인 --------------------------------------------------------------


def resolve_agent_models(args: argparse.Namespace) -> dict[str, str | None]:
    defaults = CLAUDE_DEFAULT_MODELS if args.provider == PROVIDER_CLAUDE else CODEX_DEFAULT_MODELS
    models = defaults.copy()
    # --model은 네 단계를 한꺼번에 덮고, 단계별 인자가 그 위를 덮는다. 무엇으로 돌았는지는
    # config.agent_models에 단계별로 펼쳐 남으므로 나중에 인자를 몰라도 읽을 수 있다.
    if getattr(args, "model", None):
        for agent in (AGENT_GEN, AGENT_CRITIQUE, AGENT_EVAL, AGENT_REFINE):
            models[agent] = args.model
    for agent, attribute in (
        (AGENT_GEN, "gen_model"),
        (AGENT_CRITIQUE, "critique_model"),
        (AGENT_EVAL, "eval_model"),
        (AGENT_REFINE, "refine_model"),
    ):
        value = getattr(args, attribute, None)
        if value:
            models[agent] = value
    return models


def run(args: argparse.Namespace) -> dict:
    progress = ProgressReporter()
    pipeline_started_at = time.perf_counter()
    stage = "input_validate"
    input_path = args.input.resolve()
    input_result = validate_file(input_path, artifact="input")
    progress.validation(stage, input_result)
    ensure_pass(input_result)

    # 모델용과 검증용이 어긋나면 생성물이 검증에서 떨어지거나 조용히 통과한다. 유료 호출 전에 대조한다.
    stage = "schema_pairs"
    pair_result = check_schema_pairs()
    progress.validation(stage, pair_result)
    ensure_pass(pair_result)

    input_data = load_json(input_path)
    brief_hash = input_data["brief_hash"]
    immutable_paths = input_data["brief"]["implementation_contract"]["immutable_paths"]
    start_iteration = int(args.iteration)
    if args.max_iterations < start_iteration:
        raise ValueError("--max-iterations must be greater than or equal to --iteration")

    runs_dir = args.runs_dir or RUNS_DIR
    rubric_path = (args.rubric or RUBRIC_PATH).resolve()
    rubric = load_rubric_optional(rubric_path)
    if rubric is None:
        raise FileNotFoundError(
            f"rubric not found: {rubric_path}\n"
            "루브릭은 pipeline/rubrics/ 에 있고 다른 조각이 소유한다. 전체 run은 하한이 없으면 판정할 수 없다."
        )
    # 판정 장치는 유료 호출 전에 확인한다. 게이트가 없는데 gen을 먼저 돌리면 돈만 쓰고 판정하지 못한다.
    rules = load_gate_rules(args.rules, args.allow_missing_gates, progress)
    if args.allow_missing_gates:
        progress.line("allow_missing_gates=true — 이 run은 개발용이고 통과로 읽지 않는다")

    root_context = RunContext.create(brief_hash=brief_hash, iteration=args.iteration, runs_dir=runs_dir)
    lineage = {"input": str(root_context.copied_input_path)}
    agent_models = resolve_agent_models(args)
    config = {
        "provider": args.provider,
        "codex_bin": args.codex_bin,
        "agent_models": agent_models,
        "iteration": args.iteration,
        "max_iterations": args.max_iterations,
        "timeout_seconds": args.timeout_seconds,
        "gen_prompt_path": str(GEN_PROMPT_PATH),
        "rubric_path": str(rubric_path),
        "rubric": rubric,
        "skeleton_dir": str(args.skeleton_dir),
        "java_home": args.java_home,
        "rules_path": str(args.rules or CONFORMANCE_RULES_PATH),
        "allow_missing_gates": args.allow_missing_gates,
        "max_build_attempts": args.max_build_attempts,
        "max_format_attempts": args.max_format_attempts,
    }
    client = create_client(
        provider=args.provider,
        project_dir=PROJECT_DIR,
        timeout_seconds=args.timeout_seconds,
        codex_bin=args.codex_bin,
    )
    rejections: list[dict] = []
    # 앞 단계가 낸 규약 위반을 다음 iteration의 판정에 합류시키는 통로.
    # 나쁜 초안은 루프가 다룬다. 규약 위반으로 run을 끝내면 남은 예산까지 버린다.
    pending_violations: list[dict] = []
    # 단계 실행이 중간에 멈췄을 때 어느 iteration이었는지 실패 기록에 남기기 위한 것.
    current_iteration: str | None = None
    last_refine_request_lineage: str | None = None
    progress.line(
        f"run start brief={brief_hash} iteration={args.iteration} max_iterations={args.max_iterations} "
        f"rubric={rubric.get('name', rubric_path.name)} run_id={root_context.run_id}"
    )

    try:
        stage = "prepare"
        copy_input(input_path, root_context.copied_input_path, overwrite=args.overwrite)
        drifted = verify_contract_hashes(input_data, progress)
        contract_lineage = prepare_contract_copies(root_context, input_data, args.origin or [])
        baseline_contract = root_context.baseline_dir / Path(input_data["brief"]["contracts"][0]["path"]).name
        output_state = init_output(root_context, input_data, args.skeleton_dir, progress)
        config["output_first_commit"] = output_state["commit"]
        prose_slots, prose_source = load_prose_slots(baseline_contract, args.prose_slots)
        progress.line(f"prose_slots count={len(prose_slots)} source={prose_source}")
        config["baseline_drift"] = drifted

        for iteration_number in range(start_iteration, args.max_iterations + 1):
            iteration = f"{iteration_number:03d}"
            current_iteration = iteration
            iteration_label = f"iter {iteration}/{args.max_iterations:03d}"
            progress.line(f"{iteration_label} start")
            context = root_context.with_iteration(iteration)
            context.iter_dir.mkdir(parents=True, exist_ok=True)
            lineage.update(
                {
                    "draft": str(context.draft_path),
                    "gate": str(context.gate_path),
                    "critique": str(context.critique_path),
                    "eval": str(context.eval_path),
                }
            )

            generator_commits: list[str] = []
            # 자동으로 고친 자리. 판정이 아니라 정규화이므로 원장에만 간다.
            normalizations: list[dict] = []
            # 응답 형식이 깨져 산출을 못 받은 자리. 판정이 아니므로 위반으로 세지 않는다.
            transport_failures: list[dict] = []
            if iteration_number == start_iteration:
                head_before_gen = head_commit(root_context.output_dir)
                with tempfile.TemporaryDirectory(prefix="contract-impl-gen-") as temp_dir:
                    temp_gen_output_path = Path(temp_dir) / "gen-output.json"

                    stage = f"iter_{iteration}_gen"
                    with progress.step(
                        f"{iteration_label} gen model={display_model(agent_models[AGENT_GEN], args.provider)}",
                        live=True,
                    ):
                        token_usage, gen_failure = call_stage(
                            f"{iteration_label} gen",
                            args.max_format_attempts,
                            progress,
                            lambda attempt: generate(
                                input_path=root_context.copied_input_path,
                                output_path=temp_gen_output_path,
                                client=client,
                                model=agent_models[AGENT_GEN],
                                gen_prompt_path=GEN_PROMPT_PATH,
                                prose_slots=prose_slots,
                                work_dir=root_context.output_dir,
                                raw_path=context.iter_dir / f"gen.attempt-{attempt}.raw.txt",
                                on_note=lambda note: progress.line(note),
                            ),
                        )
                    if gen_failure is not None:
                        # 초안이 없으면 판정할 것이 없다. 원문은 iter 폴더에 남아 있다.
                        raise TransportFailure(
                            f"gen의 응답 형식이 {args.max_format_attempts}번 깨졌다. "
                            f"원문은 {context.iter_dir} 의 gen.attempt-*.raw.txt 에 있다.\n{gen_failure}"
                        )

                    stage = f"iter_{iteration}_gen_validate"
                    normalizations += normalize_and_log(
                        temp_gen_output_path, "gen_output", f"{iteration_label} gen", progress,
                        work_dir=root_context.output_dir,
                    )
                    gen_changed = changed_paths(root_context.output_dir)
                    gen_result = validate_file(
                        temp_gen_output_path,
                        artifact="gen_output",
                        immutable_paths=immutable_paths,
                        work_dir=root_context.output_dir,
                        changed_paths=gen_changed,
                    )
                    progress.validation(f"{iteration_label} gen_output_validate", gen_result)
                    ensure_pass(gen_result)
                    gen_output = load_json(temp_gen_output_path)
                    pending_violations += check_previous_versions(
                        gen_changed, gen_output.get("contract_version", {}).get("path")
                    )

                stage = f"iter_{iteration}_commit"
                generator_commits = detect_generator_commits(root_context.output_dir, head_before_gen)
                if generator_commits:
                    progress.line(
                        f"{iteration_label} generator_commit 생성기가 커밋을 남겼다 "
                        f"count={len(generator_commits)} — 커밋은 runner의 일이다"
                    )
                commit = commit_iteration(root_context.output_dir, iteration, AGENT_GEN)
                progress.line(f"{iteration_label} commit={commit[:10]}")

                stage = f"iter_{iteration}_draft_write"
                draft = build_draft(
                    input_data=input_data,
                    stage_output=gen_output,
                    iteration=iteration,
                    model_name=display_model(agent_models[AGENT_GEN], args.provider),
                    token_usage=token_usage,
                    source_stage=AGENT_GEN,
                    commit=commit,
                )
                write_json(context.draft_path, draft, overwrite=args.overwrite)
                locked = lock_contract_versions(root_context.output_dir)
                if locked:
                    progress.line(f"{iteration_label} contract versions locked: {locked}")
            elif not context.draft_path.exists():
                raise FileNotFoundError(f"expected refined draft for iteration {iteration}: {context.draft_path}")

            stage = f"iter_{iteration}_draft_validate"
            draft_result = validate_file(
                context.draft_path,
                artifact="draft",
                expected_brief_hash=brief_hash,
                expected_iteration=iteration,
                immutable_paths=immutable_paths,
                work_dir=root_context.output_dir,
            )
            progress.validation(f"{iteration_label} draft_validate", draft_result)
            # 스키마가 깨진 초안은 읽을 수 없으므로 ERROR다. 색인 규약 위반은 판정이므로 이 iteration의
            # 위반으로 합류시켜 루프가 다루게 한다. 초안의 잘못으로 run을 끝내지 않는다.
            if draft_result["status"] != STATUS_PASS:
                write_result(draft_result, context.draft_validation_path)
                if any(str(error).startswith("schema ") for error in draft_result.get("errors", [])):
                    ensure_pass(draft_result)
                pending_violations += violations_from_validation(
                    draft_result, "schema.invalid_output", f"iter_{iteration} draft"
                )
                progress.line(
                    f"{iteration_label} draft 규약 위반 {len(draft_result['errors'])}건을 이 iteration의 위반으로 다룬다"
                )
            draft = load_json(context.draft_path)

            stage = f"iter_{iteration}_gate"
            with progress.step(f"{iteration_label} gate", live=True):
                gate_result = run_gate_stage(
                    context=context,
                    draft=draft,
                    input_data=input_data,
                    rules=rules,
                    java_home=args.java_home,
                    allow_missing_gates=args.allow_missing_gates,
                    progress=progress,
                )
            build_attempts: list[dict] = []
            if gate_result.get("g0") == STATUS_REJECT and args.max_build_attempts > 0:
                stage = f"iter_{iteration}_build_repair"
                gate_result, draft, build_attempts = repair_build(
                    context=context,
                    draft=draft,
                    gate_result=gate_result,
                    input_data=input_data,
                    rules=rules,
                    args=args,
                    client=client,
                    agent_models=agent_models,
                    immutable_paths=immutable_paths,
                    progress=progress,
                )

            stamp_cause_ids(gate_result.get("violations", []))
            stamp_cause_ids(gate_result.get("observations", []))
            if pending_violations:
                progress.line(
                    f"{iteration_label} carried violations={len(pending_violations)} "
                    + ", ".join(sorted({str(v.get("rule")) for v in pending_violations}))
                )
                gate_result["violations"] = list(gate_result["violations"]) + stamp_cause_ids(pending_violations)
                gate_result["carried_violations"] = pending_violations
                pending_violations = []
            write_json(context.gate_path, gate_result, overwrite=True)
            raise_on_gate_error(gate_result, context.gate_path)
            gate_status = gate_verdict(gate_result)
            progress.line(
                f"{iteration_label} gate {gate_status} g0={gate_result['g0']} g1={gate_result['g1']} "
                f"changes={gate_result['changes']} violations={len(gate_result['violations'])} "
                f"observations={len(gate_result['observations'])}"
            )

            code_files = collect_code_files(
                output_dir=root_context.output_dir,
                first_commit=output_state["commit"],
                index_paths=[entry["path"] for entry in draft["files"]],
            )
            progress.line(
                f"{iteration_label} code payload files={len(code_files['files'])} "
                f"bytes={code_files['total_bytes']} omitted={len(code_files['omitted'])}"
                + (f" -> {[item['path'] for item in code_files['omitted']][:5]}" if code_files["omitted"] else "")
            )

            critique_artifact: dict | None = None
            eval_artifact: dict | None = None
            eval_result: dict = {"status": STATUS_PASS, "errors": []}
            skipped_reason: str | None = None
            # 채점이 판정에 쓰일 수 있는가. G0가 깨져 건너뛴 iteration과 측정 불가는 둘 다 아니다.
            eval_judges = False
            unmeasured: list[str] = []

            if gate_result["g0"] == STATUS_REJECT:
                # 컴파일되지 않는 초안에는 비평할 것도 채점할 것도 없다. 유료 호출 두 번을 아낀다.
                skipped_reason = "g0_reject"
                progress.line(f"{iteration_label} critique/eval {STATUS_SKIPPED} reason=g0_reject")
            else:
                stage = f"iter_{iteration}_critique"
                with tempfile.TemporaryDirectory(prefix="contract-impl-critique-") as temp_dir:
                    temp_critique_path = Path(temp_dir) / "critique.json"
                    with progress.step(
                        f"{iteration_label} critique model={display_model(agent_models[AGENT_CRITIQUE], args.provider)}",
                        live=True,
                    ):
                        token_usage, critique_failure = call_stage(
                            f"{iteration_label} critique",
                            args.max_format_attempts,
                            progress,
                            lambda attempt: critique(
                                input_path=root_context.copied_input_path,
                                draft_path=context.draft_path,
                                output_path=temp_critique_path,
                                client=client,
                                model=agent_models[AGENT_CRITIQUE],
                                prose_slots=prose_slots,
                                decisions=draft.get("decisions", []),
                                contract_changes=draft.get("contract_changes", []),
                                observations=gate_result.get("observations", []),
                                axes=rubric_axes(rubric),
                                work_dir=root_context.output_dir,
                                code_files=code_files,
                                raw_path=context.iter_dir / f"critique.attempt-{attempt}.raw.txt",
                                on_note=lambda note: progress.line(note),
                            ),
                        )

                    critique_output = None
                    if critique_failure is None:
                        stage = f"iter_{iteration}_critique_output_validate"
                        normalize_and_log(temp_critique_path, "critique_output", f"{iteration_label} critique", progress)
                        critique_output_result = validate_file(temp_critique_path, artifact="critique_output")
                        progress.validation(f"{iteration_label} critique_output_validate", critique_output_result)
                        ensure_pass(critique_output_result)
                        critique_output = load_json(temp_critique_path)
                    else:
                        # 비평은 판정자가 아니다. 없으면 refine이 게이트 위반만 받고 계속 간다.
                        transport_failures.append(
                            {"stage": "critique", "attempts": args.max_format_attempts, "message": str(critique_failure)}
                        )
                        write_json(
                            context.iter_dir / "critique.transport-failure.json",
                            {
                                "stage": "critique",
                                "attempts": args.max_format_attempts,
                                "message": str(critique_failure),
                            },
                            overwrite=True,
                        )
                        progress.line(
                            f"{iteration_label} critique 형식 실패 — 비평 없이 진행한다(판정자가 아니다)"
                        )

                if critique_output is not None:
                    stage = f"iter_{iteration}_critique_write"
                    critique_artifact = build_critique(
                        critique_output=critique_output,
                        iteration=iteration,
                        model_name=display_model(agent_models[AGENT_CRITIQUE], args.provider),
                        token_usage=token_usage,
                    )
                    write_json(context.critique_path, critique_artifact, overwrite=args.overwrite)

                    stage = f"iter_{iteration}_critique_validate"
                    critique_result = validate_file(
                        context.critique_path,
                        artifact="critique",
                        expected_brief_hash=brief_hash,
                        expected_iteration=iteration,
                        rubric=rubric,
                    )
                    progress.validation(f"{iteration_label} critique_validate", critique_result)
                    ensure_pass(critique_result, context.critique_validation_path)

                stage = f"iter_{iteration}_eval"
                # 측정하지 못한 것은 판정이 아니다. 채점자가 코드를 읽지 못해 축을 재지 못했으면
                # 그 점수를 쓰지 않고 한 번 다시 부른다. 두 번째도 못 재면 사실만 남기고 게이트로 간다.
                for attempt in (1, 2):
                    with tempfile.TemporaryDirectory(prefix="contract-impl-eval-") as temp_dir:
                        temp_eval_path = Path(temp_dir) / "eval.json"
                        with progress.step(
                            f"{iteration_label} eval attempt={attempt} "
                            f"model={display_model(agent_models[AGENT_EVAL], args.provider)}",
                            live=True,
                        ):
                            token_usage, eval_failure = call_stage(
                                f"{iteration_label} eval attempt={attempt}",
                                1,
                                progress,
                                lambda _: evaluate(
                                    input_path=root_context.copied_input_path,
                                    draft_path=context.draft_path,
                                    rubric=rubric,
                                    output_path=temp_eval_path,
                                    client=client,
                                    model=agent_models[AGENT_EVAL],
                                    eval_output_schema=EVAL_MODEL_SCHEMA,
                                    prose_slots=prose_slots,
                                    decisions=draft.get("decisions", []),
                                    contract_changes=draft.get("contract_changes", []),
                                    work_dir=root_context.output_dir,
                                    code_files=code_files,
                                    raw_path=context.iter_dir / f"eval.attempt-{attempt}.raw.txt",
                                    on_note=lambda note: progress.line(note),
                                ),
                            )

                        if eval_failure is not None:
                            # 응답 형식이 깨진 것은 채점 결과가 아니라 전송 실패다. 한 번 더 부르고,
                            # 그래도 안 되면 측정 불가로 적고 나아간다. run을 죽이지 않는다.
                            if attempt < 2:
                                continue
                            reason = str(eval_failure).splitlines()[0][:200]
                            transport_failures.append({"stage": "eval", "attempts": 2, "message": str(eval_failure)})
                            write_json(
                                context.iter_dir / "eval.transport-failure.json",
                                {"stage": "eval", "attempts": 2, "message": str(eval_failure)},
                                overwrite=True,
                            )
                            progress.line(
                                f"{iteration_label} eval 형식 실패 — 다섯 축을 측정 불가로 적고 게이트 판정만으로 간다"
                            )
                            eval_output = unmeasured_eval(brief_hash, iteration, rubric, reason)
                            break

                        stage = f"iter_{iteration}_eval_output_validate"
                        normalize_and_log(temp_eval_path, "eval_output", f"{iteration_label} eval", progress)
                        eval_output_result = validate_file(temp_eval_path, artifact="eval_output")
                        progress.validation(f"{iteration_label} eval_output_validate", eval_output_result)
                        ensure_pass(eval_output_result)
                        eval_output = load_json(temp_eval_path)

                    unmeasured = unmeasured_axes(eval_output, rubric)
                    if not unmeasured or attempt == 2:
                        break
                    progress.line(
                        f"{iteration_label} eval 측정 불가 {unmeasured} — 그 채점은 쓰지 않고 한 번 다시 부른다"
                    )
                    write_json(
                        context.iter_dir / f"eval.unmeasured-{attempt}.json",
                        eval_output,
                        overwrite=True,
                    )

                stage = f"iter_{iteration}_eval_write"
                eval_artifact = build_eval(
                    eval_output=eval_output,
                    iteration=iteration,
                    model_name=display_model(agent_models[AGENT_EVAL], args.provider),
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
                unmeasured = unmeasured_axes(eval_artifact, rubric)
                if unmeasured:
                    # 측정하지 못한 축은 하한 대조에서 뺀다. 재지 못한 자리에 1점이 들어가 판정을 가르면
                    # 그것은 판정이 아니라 측정 실패다. 총점은 그 축들을 포함해 계산되므로 함께 뺀다.
                    # 잰 축이 하한을 못 넘긴 것은 진짜 판정이므로 남긴다.
                    kept = [
                        error
                        for error in eval_result.get("errors", [])
                        if not is_unmeasured_threshold_error(str(error), unmeasured)
                    ]
                    dropped = len(eval_result.get("errors", [])) - len(kept)
                    eval_result = {
                        **eval_result,
                        "status": STATUS_REJECT if kept else STATUS_PASS,
                        "errors": kept,
                    }
                    # 잰 축만으로는 품질을 다 말하지 못하므로 PASS도 줄 수 없다.
                    eval_judges = False
                    progress.line(
                        f"{iteration_label} eval 측정 불가 {unmeasured} — 하한 대조에서 {dropped}건을 뺐고 "
                        "이 채점으로 PASS를 주지 않는다"
                    )
                else:
                    eval_judges = True
                eval_summary = format_eval_scores(eval_artifact, rubric)
                if eval_result["status"] == STATUS_PASS:
                    progress.line(f"{iteration_label} eval PASS {eval_summary}")
                else:
                    error_summary = summarize_errors(eval_result.get("errors", []))
                    errors = f" errors={error_summary}" if error_summary else ""
                    progress.line(f"{iteration_label} eval {eval_result['status']} {eval_summary}{errors}")
                    write_result(eval_result, context.eval_validation_path)

            # 합산. 점수는 게이트를 뒤집지 못한다. 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.
            stage = f"iter_{iteration}_verdict"
            gate_errors = [
                f"gate:{v.get('rule')}@{v.get('point')}: {v.get('detail', '')}"
                for v in gate_result.get("violations", [])
                if isinstance(v, dict)
            ]
            rubric_errors = [str(error) for error in eval_result.get("errors", [])]
            decision_risk = compute_decision_risk(draft.get("decisions", []), draft.get("contract_changes", []))
            passed = (
                gate_status == STATUS_PASS
                and eval_result["status"] == STATUS_PASS
                and skipped_reason is None
                and eval_judges
            )

            record_ledger_and_report(
                context=context,
                payload={
                    "iteration": iteration,
                    "status": STATUS_PASS if passed else STATUS_REJECT,
                    "draft": relative_to_run(context.draft_path, context.run_dir),
                    "gate": gate_result,
                    "critique": critique_artifact,
                    "eval": eval_artifact,
                    "decision_risk": decision_risk,
                    "build_attempts": build_attempts,
                    "normalizations": normalizations,
                    "transport_failures": transport_failures,
                    "eval_judges": eval_judges,
                    "unmeasured_axes": unmeasured,
                    "code_payload": {
                        "files": len(code_files["files"]),
                        "omitted": code_files["omitted"],
                        "total_bytes": code_files["total_bytes"],
                    },
                    "commit": draft.get("commit"),
                    "output": relative_to_run(root_context.output_dir, context.run_dir),
                    "generator_commits": generator_commits,
                    "gate_errors": gate_errors,
                    "rubric_errors": rubric_errors,
                    "skipped_reason": skipped_reason,
                },
                progress=progress,
            )

            if passed and eval_artifact is not None:
                stage = f"iter_{iteration}_final_write"
                final_artifact = build_final(
                    context=context,
                    input_data=input_data,
                    draft=draft,
                    eval_artifact=eval_artifact,
                    gate_result=gate_result,
                    rubric=rubric,
                    contract_lineage=contract_lineage,
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

                stage = f"iter_{iteration}_tag"
                tag = tag_pass(root_context.output_dir, iteration)
                progress.line(f"{iteration_label} tagged {tag} at {final_artifact['commit'][:10]}")
                progress.line(
                    f"run PASS iteration={iteration} decision_risk={final_artifact['decision_risk']['level']} "
                    f"total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)}"
                )
                return {
                    "status": STATUS_PASS,
                    "run_id": context.run_id,
                    "input": str(root_context.copied_input_path),
                    "draft": str(context.draft_path),
                    "gate": str(context.gate_path),
                    "critique": str(context.critique_path),
                    "eval": str(context.eval_path),
                    "final": str(context.final_path),
                    "output": str(root_context.output_dir),
                    "commit": final_artifact["commit"],
                    "tag": tag,
                    "contract_version": final_artifact["contract_version"],
                    "decision_risk": final_artifact["decision_risk"],
                    "gate_coverage_incomplete": STATUS_SKIPPED
                    in (gate_result["g0"], gate_result["g1"], gate_result["changes"]),
                    "iteration": iteration,
                }

            rejections.append(
                {
                    "iteration": iteration,
                    "gate": relative_to_run(context.gate_path, context.run_dir),
                    "skipped_reason": skipped_reason,
                    "build_attempts": len(build_attempts),
                    "unmeasured_axes": unmeasured,
                    "errors": gate_errors + rubric_errors,
                }
            )

            if iteration_number >= args.max_iterations:
                stage = f"iter_{iteration}_max_iteration_exceeded"
                with progress.step(f"{iteration_label} max_iteration_exceeded"):
                    failed_path = write_max_iteration_failed(
                        context=context,
                        rejections=rejections,
                        config=config,
                        decision_risk=decision_risk,
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
                    "decision_risk": decision_risk,
                }

            to_iteration = next_iteration(iteration)
            stage = f"iter_{iteration}_refine_request"
            with progress.step(f"iter {iteration}->{to_iteration} refine_request"):
                refine_request = build_refine_request(
                    input_data=input_data,
                    draft=draft,
                    critique_artifact=critique_artifact,
                    eval_artifact=eval_artifact,
                    gate_result=gate_result,
                    rubric=rubric,
                    rules=rules,
                    to_iteration=to_iteration,
                )
                write_json(context.refine_request_path, refine_request, overwrite=True)
            last_refine_request_lineage = relative_to_run(context.refine_request_path, context.run_dir)

            next_context = root_context.with_iteration(to_iteration)
            next_context.iter_dir.mkdir(parents=True, exist_ok=True)

            head_before_refine = head_commit(root_context.output_dir)
            with tempfile.TemporaryDirectory(prefix="contract-impl-refine-") as temp_dir:
                temp_refine_output_path = Path(temp_dir) / "refine-output.json"

                stage = f"iter_{iteration}_refine_to_{to_iteration}"
                with progress.step(
                    f"iter {iteration}->{to_iteration} refine model={display_model(agent_models[AGENT_REFINE], args.provider)}",
                    live=True,
                ):
                    token_usage, refine_failure = call_stage(
                        f"iter {iteration}->{to_iteration} refine",
                        args.max_format_attempts,
                        progress,
                        lambda attempt: refine(
                            input_path=root_context.copied_input_path,
                            draft_path=context.draft_path,
                            critique_path=context.critique_path if critique_artifact is not None else None,
                            refine_request=refine_request,
                            output_path=temp_refine_output_path,
                            client=client,
                            model=agent_models[AGENT_REFINE],
                            work_dir=root_context.output_dir,
                            raw_path=next_context.iter_dir / f"refine.attempt-{attempt}.raw.txt",
                            on_note=lambda note: progress.line(note),
                        ),
                    )
                if refine_failure is not None:
                    # 다음 iteration의 초안이 없으면 루프가 이어지지 않는다. 원문은 iter 폴더에 남아 있다.
                    transport_failures.append(
                        {"stage": "refine", "attempts": args.max_format_attempts, "message": str(refine_failure)}
                    )
                    raise TransportFailure(
                        f"refine의 응답 형식이 {args.max_format_attempts}번 깨졌다. "
                        f"원문은 {next_context.iter_dir} 의 refine.attempt-*.raw.txt 에 있다.\n{refine_failure}"
                    )

                stage = f"iter_{iteration}_refine_output_validate"
                normalizations += normalize_and_log(
                    temp_refine_output_path, "refine_output", f"iter {iteration}->{to_iteration} refine", progress,
                    work_dir=root_context.output_dir,
                )
                refine_changed = changed_paths(root_context.output_dir)
                refine_result = validate_file(
                    temp_refine_output_path,
                    artifact="refine_output",
                    immutable_paths=immutable_paths,
                    work_dir=root_context.output_dir,
                    changed_paths=refine_changed,
                )
                progress.validation(f"iter {iteration}->{to_iteration} refine_output_validate", refine_result)
                try:
                    refine_output = load_json(temp_refine_output_path)
                except Exception:
                    refine_output = {}
                refine_violations = check_previous_versions(
                    refine_changed, refine_output.get("contract_version", {}).get("path")
                )
                if refine_result["status"] != STATUS_PASS:
                    # 규약을 어긴 산출은 다음 iteration의 위반으로 간다. run을 끝내지 않는다.
                    refine_violations += violations_from_validation(
                        refine_result, "schema.invalid_output", f"iter_{to_iteration} refine_output"
                    )
                    write_result(refine_result, next_context.iter_dir / "refine-output.validation.json")

            stage = f"iter_{to_iteration}_commit"
            # 아무것도 바꾸지 않는 것은 있을 수 있는 일이다. 고칠 것이 없다고 봤거나, 지시를 이해하지
            # 못했거나, 실패했을 때다. 어느 쪽이든 앞 iteration과 같은 상태이므로 게이트가 같은 위반을
            # 다시 내고 루프가 그것을 다룬다. 빈 커밋을 남겨 iteration마다 스냅샷이 하나씩 있게 한다.
            no_change = not refine_changed
            if no_change:
                progress.line(
                    f"iter {iteration}->{to_iteration} refine이 작업 폴더를 바꾸지 않았다 — "
                    "빈 커밋을 남기고 같은 상태로 다음 iteration을 돈다"
                )
            for hash_value in detect_generator_commits(root_context.output_dir, head_before_refine):
                progress.line(f"iter {to_iteration} generator_commit {hash_value[:10]} — 커밋은 runner의 일이다")
            refined_commit = commit_iteration(root_context.output_dir, to_iteration, AGENT_REFINE)
            progress.line(f"iter {to_iteration} commit={refined_commit[:10]}")

            stage = f"iter_{to_iteration}_draft_write"
            if refine_violations:
                pending_violations += refine_violations
                progress.line(
                    f"iter {iteration}->{to_iteration} refine 규약 위반 {len(refine_violations)}건을 "
                    f"iteration {to_iteration}의 위반으로 넘긴다"
                )
            if refine_result["status"] == STATUS_PASS:
                refined_draft = build_draft(
                    input_data=input_data,
                    stage_output=refine_output,
                    iteration=to_iteration,
                    model_name=display_model(agent_models[AGENT_REFINE], args.provider),
                    token_usage=token_usage,
                    source_stage=AGENT_REFINE,
                    commit=refined_commit,
                )
            else:
                refined_draft = build_recovery_draft(
                    input_data=input_data,
                    previous_draft=draft,
                    stage_output=refine_output,
                    iteration=to_iteration,
                    model_name=display_model(agent_models[AGENT_REFINE], args.provider),
                    commit=refined_commit,
                    base_commit=head_before_refine,
                    output_dir=root_context.output_dir,
                )
            if no_change:
                refined_draft.setdefault("metadata", {})["no_change"] = True
            write_json(next_context.draft_path, refined_draft, overwrite=args.overwrite)
            locked = lock_contract_versions(root_context.output_dir)
            if locked:
                progress.line(f"iter {to_iteration} contract versions locked: {locked}")
    except Exception as exc:
        progress.line(
            f"run ERROR stage={stage} total_elapsed={format_duration(time.perf_counter() - pipeline_started_at)} "
            f"error={type(exc).__name__}"
        )
        failed_path = write_failed(
            run_dir=root_context.run_dir,
            brief_hash=brief_hash,
            run_id=root_context.run_id,
            stage=stage,
            error=exc,
            lineage=lineage,
            config=config,
            last_iteration=current_iteration,
            rejections=rejections,
        )
        progress.line(f"run failed artifact={failed_path}")
        raise RuntimeError(f"pipeline failed at {stage}; wrote {failed_path}") from exc

    raise RuntimeError("pipeline ended without PASS or FAILED status")


def add_common_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--provider", choices=[PROVIDER_CODEX, PROVIDER_CLAUDE], default=PROVIDER_CODEX)
    parser.add_argument("--codex-bin", default="codex")
    parser.add_argument("--model", help="네 단계 전부의 모델. 단계별 인자가 이것을 덮는다.")
    parser.add_argument("--gen-model")
    parser.add_argument("--critique-model")
    parser.add_argument("--eval-model")
    parser.add_argument("--refine-model")
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument(
        "--rubric",
        type=Path,
        default=None,
        help="미지정 시 rubrics/impl.rubric.yaml.",
    )
    parser.add_argument(
        "--skeleton-dir",
        type=Path,
        default=DEFAULT_SKELETON_DIR,
        help="G0가 draft를 떨어뜨릴 Spring 스켈레톤.",
    )
    parser.add_argument(
        "--java-home",
        default=None,
        help="스켈레톤을 빌드할 JDK 17 이상의 경로. 미지정이면 CONTRACT_IMPL_JAVA_HOME → JAVA_HOME 순으로 환경에 맡긴다.",
    )
    parser.add_argument(
        "--rules",
        type=Path,
        default=None,
        help="규칙 카드. 미지정 시 <skill>/rules/conformance_rules.yaml.",
    )
    parser.add_argument(
        "--max-format-attempts",
        type=int,
        default=2,
        help="응답 형식이 깨졌을 때 같은 단계를 다시 부르는 횟수. 형식 실패는 채점이 아니라 전송 실패다.",
    )
    parser.add_argument(
        "--max-build-attempts",
        type=int,
        default=3,
        help="G0가 깨진 iteration에서 빌드만 고치는 시도 횟수. 이 시도는 max_iterations에서 세지 않는다. 0이면 끈다.",
    )
    parser.add_argument(
        "--allow-missing-gates",
        action="store_true",
        help="개발용. 게이트나 규칙 카드가 없을 때 ERROR 대신 SKIPPED로 진행한다. 그 run은 통과로 읽지 않는다.",
    )
    parser.add_argument(
        "--prose-slots",
        type=Path,
        default=None,
        help="산문 슬롯 JSON. 미지정이면 추출기를 부르고, 추출기가 없으면 빈 목록이다.",
    )
    parser.add_argument(
        "--origin",
        type=Path,
        action="append",
        help="사람이 확정한 계약 원본. 미지정이면 첫 run으로 보고 기준선과 같게 둔다. 반복 지정 가능.",
    )
    parser.add_argument("--overwrite", action="store_true", help="같은 run의 산출물을 다시 만들 때만 붙인다.")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Run the contract conformance pipeline: input -> gen -> G0 -> G1 -> critique -> eval -> final/failed."
    )
    parser.add_argument("input", type=Path, help="Path to an input JSON file matching input.schema.json.")
    parser.add_argument("--runs-dir", type=Path, default=None, help="미지정 시 <skill>/runs/.")
    parser.add_argument("--iteration", default="001")
    parser.add_argument("--max-iterations", type=int, default=3)
    add_common_arguments(parser)
    args = parser.parse_args()

    if len(args.iteration) != 3 or not args.iteration.isdigit():
        raise ValueError("--iteration must use a 3-digit value such as 001")
    if args.max_iterations < 1:
        raise ValueError("--max-iterations must be at least 1")

    result = run(args)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == STATUS_PASS else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
