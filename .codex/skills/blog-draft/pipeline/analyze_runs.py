"""analyze_runs.py

Slow loop 1단계: pending run들에서 결정적 신호를 집계해 analysis.json을 만든다.

대상: runs/pending/ 아래 통과한 run들의 eval.json, critique.json.
      ERROR(파이프라인 크래시)와 failed.json(max_iterations 초과)은 제외한다.
출력: pipeline/changelog/analysis_{analysis_id}.json

Usage:
    python -B analyze_runs.py --runs-dir ../runs
    python -B analyze_runs.py --runs-dir ../runs --threshold 0.6 --dry-run
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
DEFAULT_RUNS_DIR = PROJECT_DIR.parent / "runs"
PENDING_DIR_NAME = "pending"
CHANGELOG_DIR = PROJECT_DIR / "changelog"

KST = timezone(timedelta(hours=9))

# 신호 후보로 올리는 임계값: pending의 이 비율 이상에서 min_axis 미달이면 신호화
DEFAULT_BELOW_THRESHOLD = 0.6


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
        finals = list(run_dir.glob("*_final.json"))
        if finals:
            runs.append(run_dir)
    return runs


def latest_iter_files(run_dir: Path, suffix: str) -> list[Path]:
    """run_dir 아래 iter_* 에서 suffix로 끝나는 파일을 iteration 오름차순으로 반환한다."""
    files = []
    for iter_dir in sorted(run_dir.glob("iter_*")):
        matched = list(iter_dir.glob(f"*{suffix}"))
        if matched:
            files.append(matched[0])
    return files


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


def extract_signals(
    evals: list[tuple[str, dict]],
    critiques: list[tuple[str, dict]],
    axis_stats: dict,
    min_axis: dict,
    below_threshold: float,
) -> list[dict]:
    """결정적 패턴 룰로 신호 후보를 추출한다. 각 신호는 stable id를 가진다."""
    signals = []
    signal_counter = 1

    # 신호 종류 1: axis_below_threshold
    # min_axis 미달 비율이 below_threshold 이상인 axis
    for axis, stats in axis_stats.items():
        if stats["below_min_ratio"] >= below_threshold:
            run_refs = [
                run_name
                for run_name, ev in evals
                if isinstance(ev.get("rubric_scores", {}).get("scores", {}).get(axis), (int, float))
                and float(ev["rubric_scores"]["scores"][axis]) < float(min_axis.get(axis, 0))
            ]
            # rationale 원문 발췌
            quotes = []
            for _, ev in evals:
                rationale = ev.get("axis_rationales", {}).get(axis)
                if rationale and isinstance(rationale, str):
                    quotes.append(rationale[:200])

            strength = _signal_strength(stats["below_min_ratio"])
            signals.append({
                "id": f"S{signal_counter}",
                "kind": "axis_below_threshold",
                "axis": axis,
                "summary": (
                    f"{axis}: {len(run_refs)}/{len(evals)} run에서 min_axis({min_axis.get(axis, '?')}) 미달 "
                    f"(비율={stats['below_min_ratio']:.0%}, 평균={stats['mean']})"
                ),
                "strength": strength,
                "occurrence_count": len(run_refs),
                "run_refs": run_refs,
                "example_quotes": quotes[:3],
            })
            signal_counter += 1

    # 신호 종류 2: critique_repeat
    # 같은 axis나 키워드가 여러 run의 critique weakness에 반복 등장하는 패턴
    issue_run_map: dict[str, list[str]] = defaultdict(list)
    issue_text_map: dict[str, list[str]] = defaultdict(list)
    for run_name, cr in critiques:
        weaknesses = cr.get("weaknesses", [])
        if not isinstance(weaknesses, list):
            continue
        for w in weaknesses:
            if not isinstance(w, dict):
                continue
            issue = w.get("issue", "")
            if not issue:
                continue
            # 명사구 레벨 클러스터링 없이 첫 12자를 키로 단순 집계
            # (deterministic, LLM 없이)
            key = issue[:40].strip().lower()
            issue_run_map[key].append(run_name)
            issue_text_map[key].append(issue)

    n = len(critiques)
    for key, run_refs in issue_run_map.items():
        unique_runs = list(dict.fromkeys(run_refs))  # 순서 유지 중복 제거
        ratio = len(unique_runs) / n if n > 0 else 0
        if ratio >= below_threshold:
            strength = _signal_strength(ratio)
            signals.append({
                "id": f"S{signal_counter}",
                "kind": "critique_repeat",
                "summary": (
                    f"critique 반복 지적: '{issue_text_map[key][0][:60]}' "
                    f"({len(unique_runs)}/{n} run, 비율={ratio:.0%})"
                ),
                "strength": strength,
                "occurrence_count": len(unique_runs),
                "run_refs": unique_runs,
                "example_quotes": list(dict.fromkeys(issue_text_map[key]))[:3],
            })
            signal_counter += 1

    return signals


def _signal_strength(ratio: float) -> str:
    if ratio >= 0.8:
        return "high"
    if ratio >= 0.6:
        return "medium"
    return "low"


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
    # rubric 없으면 eval에서 첫 번째 scores 키들로 축 목록만 구성, threshold는 0
    for _, ev in evals:
        scores = ev.get("rubric_scores", {}).get("scores", {})
        if isinstance(scores, dict) and scores:
            return {axis: 0 for axis in scores}
    return {}


def build_analysis(
    run_dirs: list[Path],
    evals: list[tuple[str, dict]],
    critiques: list[tuple[str, dict]],
    axis_stats: dict,
    signals: list[dict],
    rubric_name: str,
    analysis_id: str,
) -> dict:
    return {
        "analysis_id": analysis_id,
        "generated_at": now_iso(),
        "pending_count": len(run_dirs),
        "runs_included": [r.name for r in run_dirs],
        "rubric_name": rubric_name,
        "axis_stats": axis_stats,
        "signals": signals,
    }


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
    signals = extract_signals(evals, critiques, axis_stats, min_axis, args.threshold)
    rubric_name = infer_rubric_name(evals)

    today = datetime.now(KST).date().isoformat()
    analysis_id = f"{today}_{len(run_dirs)}runs"

    analysis = build_analysis(
        run_dirs=run_dirs,
        evals=evals,
        critiques=critiques,
        axis_stats=axis_stats,
        signals=signals,
        rubric_name=rubric_name,
        analysis_id=analysis_id,
    )

    if args.dry_run:
        print(json.dumps(analysis, ensure_ascii=False, indent=2))
        return {"status": "DRY_RUN", "analysis_id": analysis_id, "signal_count": len(signals)}

    out_path = CHANGELOG_DIR / f"analysis_{analysis_id}.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(analysis, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"[analyze] wrote {out_path}", file=sys.stderr)

    return {
        "status": "OK",
        "analysis_id": analysis_id,
        "analysis_path": str(out_path),
        "run_count": len(run_dirs),
        "signal_count": len(signals),
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Collect signals from passing pending runs → analysis.json for the slow loop proposer."
    )
    parser.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR, help="runs/ 루트 경로")
    parser.add_argument("--rubric", type=Path, default=None, help="writing rubric YAML 경로 (기본: pipeline/rubric.yaml)")
    parser.add_argument(
        "--threshold",
        type=float,
        default=DEFAULT_BELOW_THRESHOLD,
        help=f"신호 후보 임계값: pending의 이 비율 이상에서 미달이면 신호화 (기본: {DEFAULT_BELOW_THRESHOLD})",
    )
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
