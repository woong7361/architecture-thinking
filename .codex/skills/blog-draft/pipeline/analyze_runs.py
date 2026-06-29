"""analyze_runs.py

Slow loop 1단계: pending run들의 eval/critique 원문을 수집해 analysis.json을 만든다.

패턴 판단은 하지 않는다. axis 점수 통계(deterministic)와 rationale·weakness 원문을
그대로 담아 proposer(LLM)에게 넘긴다.

대상: runs/pending/ 아래 통과한 run들의 eval.json, critique.json.
      ERROR(파이프라인 크래시)와 failed.json(max_iterations 초과)은 제외한다.
출력: proposals/{analysis_id}/analysis.json

Usage:
    python -B analyze_runs.py --runs-dir ../runs
    python -B analyze_runs.py --runs-dir ../runs --dry-run
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

PROJECT_DIR = Path(__file__).resolve().parent
SKILL_DIR = PROJECT_DIR.parent
DEFAULT_RUNS_DIR = SKILL_DIR / "runs"
PENDING_DIR_NAME = "pending"
PROPOSALS_DIR = SKILL_DIR / "proposals"

KST = timezone(timedelta(hours=9))


def now_iso() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def load_json(path: Path) -> dict | None:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else None
    except Exception:
        return None


def collect_passing_runs(pending_dir: Path) -> list[Path]:
    """pending/ 아래에서 final.json이 있는 run 디렉토리만 반환한다."""
    if not pending_dir.exists():
        return []
    runs = []
    for run_dir in sorted(pending_dir.iterdir()):
        if not run_dir.is_dir():
            continue
        if list(run_dir.glob("*_final.json")):
            runs.append(run_dir)
    return runs


def collect_evals(run_dirs: list[Path]) -> list[tuple[str, dict]]:
    """(run_dir_name, eval_data) 목록. final.json과 eval이 모두 있는 것만."""
    result = []
    for run_dir in run_dirs:
        finals = list(run_dir.glob("*_final.json"))
        if not finals:
            continue
        final = load_json(finals[0])
        if not final:
            continue
        final_iter = final.get("final_iteration")
        if not final_iter:
            continue
        eval_files = list(run_dir.glob(f"iter_{final_iter}/*_iter-{final_iter}_eval.json"))
        if not eval_files:
            continue
        eval_data = load_json(eval_files[0])
        if eval_data and isinstance(eval_data.get("rubric_scores"), dict):
            result.append((run_dir.name, eval_data))
    return result


def collect_critiques(run_dirs: list[Path]) -> list[tuple[str, dict]]:
    """(run_dir_name, critique_data) 목록. final.json 기준 최종 iteration critique."""
    result = []
    for run_dir in run_dirs:
        finals = list(run_dir.glob("*_final.json"))
        if not finals:
            continue
        final = load_json(finals[0])
        if not final:
            continue
        final_iter = final.get("final_iteration")
        if not final_iter:
            continue
        critique_files = list(run_dir.glob(f"iter_{final_iter}/*_iter-{final_iter}_critique.json"))
        if not critique_files:
            continue
        critique_data = load_json(critique_files[0])
        if critique_data and isinstance(critique_data.get("weaknesses"), list):
            result.append((run_dir.name, critique_data))
    return result


def compute_axis_stats(evals: list[tuple[str, dict]], rubric_axes: list[str], min_axis: dict) -> dict:
    """axis별 평균/최솟값/최댓값/미달 개수/미달 비율을 결정적으로 계산한다."""
    n = len(evals)
    if n == 0:
        return {}

    axis_values: dict[str, list[float]] = defaultdict(list)
    for _, ev in evals:
        scores = ev.get("rubric_scores", {}).get("scores", {})
        if not isinstance(scores, dict):
            continue
        for axis in rubric_axes:
            val = scores.get(axis)
            if isinstance(val, (int, float)):
                axis_values[axis].append(float(val))

    stats = {}
    for axis in rubric_axes:
        vals = axis_values.get(axis, [])
        if not vals:
            continue
        minimum = float(min_axis.get(axis, 0))
        below = [v for v in vals if v < minimum]
        stats[axis] = {
            "mean": round(sum(vals) / len(vals), 3),
            "min": min(vals),
            "max": max(vals),
            "below_min_count": len(below),
            "below_min_ratio": round(len(below) / n, 3),
        }
    return stats


def collect_eval_rationales(evals: list[tuple[str, dict]], rubric_axes: list[str]) -> dict:
    """axis별로 각 run의 rationale 원문을 수집한다."""
    rationales: dict[str, list[dict]] = {axis: [] for axis in rubric_axes}
    for run_name, ev in evals:
        axis_rationales = ev.get("axis_rationales", {})
        if not isinstance(axis_rationales, dict):
            continue
        for axis in rubric_axes:
            text = axis_rationales.get(axis)
            if text and isinstance(text, str):
                rationales[axis].append({"run": run_name, "text": text})
    return rationales


def collect_critique_weaknesses(critiques: list[tuple[str, dict]]) -> list[dict]:
    """모든 run의 critique weakness를 run 이름과 함께 수집한다."""
    weaknesses = []
    for run_name, cr in critiques:
        for w in cr.get("weaknesses", []):
            if isinstance(w, dict) and w.get("issue"):
                weaknesses.append({"run": run_name, **w})
    return weaknesses


def infer_rubric_name(evals: list[tuple[str, dict]]) -> str:
    for _, ev in evals:
        name = ev.get("rubric_name")
        if name and isinstance(name, str):
            return name
    return "unknown"


def infer_min_axis(evals: list[tuple[str, dict]], rubric_path: Path | None) -> dict:
    """rubric.yaml이 있으면 거기서, 없으면 eval 파일들에서 추론한다."""
    if rubric_path and rubric_path.exists():
        try:
            import yaml  # type: ignore[import-not-found]
            rubric = yaml.safe_load(rubric_path.read_text(encoding="utf-8"))
            min_axis = rubric.get("thresholds", {}).get("min_axis", {})
            if isinstance(min_axis, dict) and min_axis:
                return min_axis
        except Exception:
            pass
    for _, ev in evals:
        scores = ev.get("rubric_scores", {}).get("scores", {})
        if isinstance(scores, dict) and scores:
            return {axis: 0 for axis in scores}
    return {}


def run(args: argparse.Namespace) -> dict:
    pending_dir = args.runs_dir / PENDING_DIR_NAME
    print(f"[analyze] pending_dir={pending_dir}", file=sys.stderr)

    run_dirs = collect_passing_runs(pending_dir)
    print(f"[analyze] passing runs found={len(run_dirs)}", file=sys.stderr)

    if len(run_dirs) < args.min_runs:
        return {
            "status": "SKIP",
            "reason": f"passing runs ({len(run_dirs)}) < min_runs ({args.min_runs})",
            "pending_dir": str(pending_dir),
        }

    evals = collect_evals(run_dirs)
    critiques = collect_critiques(run_dirs)
    print(f"[analyze] evals={len(evals)} critiques={len(critiques)}", file=sys.stderr)

    rubric_path = args.rubric if args.rubric else PROJECT_DIR / "rubric.yaml"
    min_axis = infer_min_axis(evals, rubric_path)
    rubric_axes = list(min_axis.keys())

    axis_stats = compute_axis_stats(evals, rubric_axes, min_axis)
    eval_rationales = collect_eval_rationales(evals, rubric_axes)
    critique_weaknesses = collect_critique_weaknesses(critiques)
    rubric_name = infer_rubric_name(evals)

    today = datetime.now(KST).date().isoformat()
    analysis_id = f"{today}_{len(run_dirs)}runs"

    analysis = {
        "analysis_id": analysis_id,
        "generated_at": now_iso(),
        "pending_count": len(run_dirs),
        "runs_included": [r.name for r in run_dirs],
        "rubric_name": rubric_name,
        "axis_stats": axis_stats,
        "eval_rationales": eval_rationales,
        "critique_weaknesses": critique_weaknesses,
    }

    if args.dry_run:
        print(json.dumps(analysis, ensure_ascii=False, indent=2))
        return {
            "status": "DRY_RUN",
            "analysis_id": analysis_id,
            "weakness_count": len(critique_weaknesses),
        }

    out_path = PROPOSALS_DIR / analysis_id / "analysis.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(analysis, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"[analyze] wrote {out_path}", file=sys.stderr)

    return {
        "status": "OK",
        "analysis_id": analysis_id,
        "analysis_path": str(out_path),
        "run_count": len(run_dirs),
        "weakness_count": len(critique_weaknesses),
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Collect eval/critique data from passing pending runs → analysis.json for the slow loop proposer."
    )
    parser.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR, help="runs/ 루트 경로")
    parser.add_argument("--rubric", type=Path, default=None, help="writing rubric YAML 경로 (기본: pipeline/rubric.yaml)")
    parser.add_argument(
        "--min-runs",
        type=int,
        default=5,
        help="분석을 실행할 최소 통과 run 수 (기본: 5)",
    )
    parser.add_argument("--dry-run", action="store_true", help="파일 저장 없이 분석 결과를 stdout에 출력")
    args = parser.parse_args()

    result = run(args)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result.get("status") in ("OK", "DRY_RUN") else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
