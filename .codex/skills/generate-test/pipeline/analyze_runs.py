"""analyze_runs.py

Slow loop 1단계: 미검토 run들의 eval/critique/failed 신호를 결정적으로 집계해 analysis.json을 만든다.

blog-draft analyze_runs.py를 generate-test로 적응한 것. 핵심 차이(설계 docs/v1-slow-loop-design.md):
- **pending/ 폴더 없음**: 상태는 제자리 `.reviewed` 마커. 미검토 = run 폴더에 `.reviewed`가 없는 것. (§7)
- **(mode × rubric_name) 분할**: contract:v1 / unit:v1 / bundled:v1를 한 통계에 섞지 않고 그룹별로 집계. (§2-1)
- **중첩 run 구조**: split은 runs/split/<group>/{contract,unit}/<run_id>/, bundled는 runs/bundled/<run_id>/.
  rglob으로 *_final.json / *_failed.json을 찾아 그 부모를 run 폴더로 본다.
- **problem.md 앵커**: 사용자 피드백 항목을 analysis에 실어 proposer에게 넘긴다(유일한 사람발 앵커). (§5)
- **표본 충분성**: 그룹 total < min_group_sample이면 sufficient_sample=false → proposer가 그 그룹은 제안 안 함. (§5-B)

패턴 판단은 하지 않는다. 신호·후보만 담고 진단은 proposer(LLM), 결정은 사람.

Usage:
    python -B analyze_runs.py --dry-run
    python -B analyze_runs.py --min-runs 5
    python -B analyze_runs.py --mark-reviewed   # (오케스트레이터 전용) 분석한 run에 .reviewed 기록
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

PROJECT_DIR = Path(__file__).resolve().parent          # pipeline/
SKILL_DIR = PROJECT_DIR.parent                         # generate-test/
DEFAULT_RUNS_DIR = SKILL_DIR / "runs"
RUBRICS_DIR = PROJECT_DIR / "rubrics"
PROBLEM_MD = SKILL_DIR / "problem.md"
PROPOSALS_DIR = PROJECT_DIR / "changelog" / "proposals"
REVIEWED_MARKER = ".reviewed"

KST = timezone(timedelta(hours=9))


def now_iso() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def load_json(path: Path) -> dict | None:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else None
    except Exception:
        return None


# ---------------------------------------------------------------------------
# run 탐색 (pending/ 대신 .reviewed 마커 기반)
# ---------------------------------------------------------------------------

def find_run_dirs(runs_dir: Path) -> tuple[list[Path], list[Path]]:
    """runs/ 를 재귀 탐색해 (통과 run 폴더, 실패 run 폴더)를 반환한다.

    run 폴더 = *_final.json(통과) 또는 *_failed.json(실패)을 직접 담은 디렉토리.
    final.json이 있으면 통과로 보고 실패 표본에서 제외(이중 집계 방지).
    """
    if not runs_dir.exists():
        return [], []
    passing = {p.parent for p in runs_dir.rglob("*_final.json")}
    failed = {p.parent for p in runs_dir.rglob("*_failed.json") if p.parent not in passing}
    return sorted(passing), sorted(failed)


def is_unreviewed(run_dir: Path) -> bool:
    return not (run_dir / REVIEWED_MARKER).exists()


def mark_reviewed_by_names(runs_dir: Path, names: list[str]) -> list[str]:
    """이름(run 폴더 basename)에 해당하는 run에 .reviewed 마커를 기록한다.

    오케스트레이터(run_propose.py)가 proposal 생성 성공 후 호출한다. 폴더를 옮기지 않고
    제자리에 마커만 남긴다(§7). 마킹된 run 이름 목록을 반환한다.
    """
    passing, failed = find_run_dirs(runs_dir)
    by_name = {d.name: d for d in (passing + failed)}
    marked = []
    for name in names:
        d = by_name.get(name)
        if d is not None:
            (d / REVIEWED_MARKER).write_text(now_iso() + "\n", encoding="utf-8")
            marked.append(name)
    return marked


def run_rubric_name(run_dir: Path) -> str:
    """run의 rubric_name을 eval.json에서 뽑는다(최종/최근 iteration 우선)."""
    eval_files = sorted(run_dir.glob("iter_*/*_eval.json"))
    for ev_path in reversed(eval_files):
        data = load_json(ev_path)
        if data and isinstance(data.get("rubric_name"), str):
            return data["rubric_name"]
    return "unknown"


def mode_of(rubric_name: str) -> str:
    return rubric_name.split(":", 1)[0] if rubric_name else "unknown"


def rubric_file_for(mode: str) -> Path:
    return RUBRICS_DIR / f"{mode}.rubric.yaml"


def load_min_axis(mode: str) -> dict:
    path = rubric_file_for(mode)
    if not path.exists():
        return {}
    try:
        import yaml  # type: ignore[import-not-found]
        rubric = yaml.safe_load(path.read_text(encoding="utf-8"))
        min_axis = (rubric.get("thresholds") or {}).get("min_axis", {})
        return min_axis if isinstance(min_axis, dict) else {}
    except Exception:
        return {}


# ---------------------------------------------------------------------------
# 신호 집계 (blog-draft 헬퍼 이식)
# ---------------------------------------------------------------------------

def final_eval(run_dir: Path) -> dict | None:
    finals = list(run_dir.glob("*_final.json"))
    if not finals:
        return None
    final = load_json(finals[0])
    if not final:
        return None
    it = final.get("final_iteration")
    if not it:
        return None
    ev = list(run_dir.glob(f"iter_{it}/*_iter-{it}_eval.json"))
    return load_json(ev[0]) if ev else None


def final_critique(run_dir: Path) -> dict | None:
    finals = list(run_dir.glob("*_final.json"))
    if not finals:
        return None
    final = load_json(finals[0])
    if not final:
        return None
    it = final.get("final_iteration")
    if not it:
        return None
    cr = list(run_dir.glob(f"iter_{it}/*_iter-{it}_critique.json"))
    return load_json(cr[0]) if cr else None


def compute_axis_stats(evals: list[tuple[str, dict]], axes: list[str], min_axis: dict) -> dict:
    n = len(evals)
    if n == 0:
        return {}
    axis_values: dict[str, list[float]] = defaultdict(list)
    for _, ev in evals:
        scores = (ev.get("rubric_scores") or {}).get("scores", {})
        if not isinstance(scores, dict):
            continue
        for axis in axes:
            val = scores.get(axis)
            if isinstance(val, (int, float)):
                axis_values[axis].append(float(val))
    stats = {}
    for axis in axes:
        vals = axis_values.get(axis, [])
        if not vals:
            continue
        minimum = float(min_axis.get(axis, 0))
        below = [v for v in vals if v < minimum]
        stats[axis] = {
            "mean": round(sum(vals) / len(vals), 3),
            "min": min(vals),
            "max": max(vals),
            "min_axis": minimum,
            "below_min_count": len(below),
            "below_min_ratio": round(len(below) / len(vals), 3),
        }
    return stats


def collect_eval_rationales(evals: list[tuple[str, dict]], axes: list[str]) -> dict:
    rationales: dict[str, list[dict]] = {axis: [] for axis in axes}
    for run_name, ev in evals:
        ax = ev.get("axis_rationales", {})
        if not isinstance(ax, dict):
            continue
        for axis in axes:
            text = ax.get(axis)
            if isinstance(text, str) and text:
                rationales[axis].append({"run": run_name, "text": text})
    return rationales


def collect_critique_weaknesses(critiques: list[tuple[str, dict]]) -> list[dict]:
    weaknesses = []
    for run_name, cr in critiques:
        for w in cr.get("weaknesses", []):
            if isinstance(w, dict) and w.get("issue"):
                weaknesses.append({"run": run_name, **w})
    return weaknesses


def compute_failure_signals(failed_dirs: list[Path]) -> dict:
    included: list[str] = []
    by_terminal: dict[str, int] = defaultdict(int)
    by_category: dict[str, int] = defaultdict(int)
    rule_runs: dict[str, int] = defaultdict(int)

    for run_dir in failed_dirs:
        files = list(run_dir.glob("*_failed.json"))
        if not files:
            continue
        data = load_json(files[0])
        if not data:
            continue
        included.append(run_dir.name)
        tr = data.get("terminal_reason")
        if isinstance(tr, str) and tr:
            by_terminal[tr] += 1
        counts = data.get("failure_counts_by_category")
        if isinstance(counts, dict):
            for cat, val in counts.items():
                if isinstance(val, (int, float)):
                    by_category[cat] += int(val)
        rules: set[str] = set()
        for f in data.get("last_failures", []):
            if isinstance(f, dict) and isinstance(f.get("rule"), str) and f["rule"]:
                rules.add(f["rule"])
        for rej in data.get("iteration_rejections", []):
            if not isinstance(rej, dict):
                continue
            for err in rej.get("errors", []):
                if isinstance(err, str):
                    rule = err.split(":", 1)[0].strip()
                    if rule:
                        rules.add(rule)
        for r in rules:
            rule_runs[r] += 1

    n = len(included)
    rule_run_counts = {
        rule: {"runs": cnt, "ratio": round(cnt / n, 3)}
        for rule, cnt in sorted(rule_runs.items(), key=lambda kv: (-kv[1], kv[0]))
    } if n else {}
    return {
        "failed_count": n,
        "by_terminal_reason": dict(by_terminal),
        "by_category": dict(by_category),
        "rule_run_counts": rule_run_counts,
        "runs_included": included,
    }


# ---------------------------------------------------------------------------
# problem.md 앵커
# ---------------------------------------------------------------------------

_ENTRY_RE = re.compile(r"^-\s*\(.*verdict=(pos|neg)", re.IGNORECASE)


def collect_user_feedback() -> dict:
    """problem.md의 '## 항목' 헤더 아래 verdict 항목만 원문으로 수집한다.

    형식 예시(상단 코드블록의 `verdict=pos|neg` 템플릿)를 잡지 않도록 '## 항목' 이후만 스캔한다.
    """
    if not PROBLEM_MD.exists():
        return {"source": "problem.md", "present": False, "entries": [], "count": 0}
    entries = []
    in_items = False
    for line in PROBLEM_MD.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if s.startswith("## "):
            in_items = s.lstrip("# ").strip() == "항목"
            continue
        if in_items and _ENTRY_RE.match(s):
            entries.append(s)
    return {"source": "problem.md", "present": True, "entries": entries, "count": len(entries)}


# ---------------------------------------------------------------------------
# signals[] 합성 — 그룹 통계를 id 붙은 평평한 색인으로 (proposer 인용·검증용, §8)
# ---------------------------------------------------------------------------

def _clip(text: str, n: int = 140) -> str:
    text = " ".join(str(text).split())
    return text if len(text) <= n else text[: n - 1] + "…"


def synthesize_signals(groups: dict, user_feedback: dict) -> list[dict]:
    """groups + user_feedback를 citable 신호 목록으로 펼친다.

    각 신호는 안정적 id를 가진다. gen이 cited_signals로 인용하고 validator가 실재성을 검사한다.
    새 사실이 아니라 그룹 통계의 평평한 색인(인덱스)이다.
    """
    signals: list[dict] = []

    for name, g in groups.items():
        # 1) axis 미달 (통과 표본에서 min_axis 밑)
        for axis, st in (g.get("axis_stats") or {}).items():
            if st.get("below_min_count", 0) > 0:
                signals.append({
                    "id": f"{name}/axis/{axis}",
                    "kind": "axis", "group": name,
                    "summary": f"{name} {axis} 평균 {st['mean']}, {st['below_min_count']}개 run이 min_axis({st['min_axis']}) 미달",
                })
        # 2) critique 반복 지적
        for i, w in enumerate(g.get("critique_weaknesses") or []):
            sev = w.get("severity", "?")
            signals.append({
                "id": f"{name}/weakness/{i}",
                "kind": "critique", "group": name,
                "summary": _clip(f"{w.get('issue','')} (severity={sev})"),
            })
        # 3) 실패 신호 (terminal_reason / rule)
        fs = g.get("failure_signals") or {}
        for reason, cnt in (fs.get("by_terminal_reason") or {}).items():
            signals.append({
                "id": f"{name}/terminal/{reason}",
                "kind": "failure", "group": name,
                "summary": f"{name}에서 {cnt}개 run이 수렴 못 하고 {reason}으로 실패",
            })
        for rule, info in (fs.get("rule_run_counts") or {}).items():
            signals.append({
                "id": f"{name}/failure/{rule}",
                "kind": "failure", "group": name,
                "summary": f"{name} {info.get('runs')}개 run이 {rule}(으)로 반려",
            })

    # 4) 사람 피드백 (problem.md, 유일한 사람발 앵커)
    for i, entry in enumerate(user_feedback.get("entries") or []):
        signals.append({
            "id": f"feedback/{i}",
            "kind": "user_feedback", "group": None,
            "summary": _clip(entry),
        })

    return signals


# ---------------------------------------------------------------------------
# 그룹별 분석
# ---------------------------------------------------------------------------

def analyze_group(rubric_name: str, passing: list[Path], failed: list[Path], min_group: int) -> dict:
    mode = mode_of(rubric_name)
    min_axis = load_min_axis(mode)
    axes = list(min_axis.keys())

    evals = [(d.name, e) for d in passing if (e := final_eval(d))]
    critiques = [(d.name, c) for d in passing if (c := final_critique(d))]
    total = len(passing) + len(failed)

    return {
        "mode": mode,
        "rubric_file": str(rubric_file_for(mode).relative_to(SKILL_DIR)) if rubric_file_for(mode).exists() else None,
        "passing_count": len(passing),
        "failed_count": len(failed),
        "total": total,
        "sufficient_sample": total >= min_group,
        "runs_included": [d.name for d in passing],
        "failed_runs_included": [d.name for d in failed],
        "axis_stats": compute_axis_stats(evals, axes, min_axis),
        "eval_rationales": collect_eval_rationales(evals, axes),
        "critique_weaknesses": collect_critique_weaknesses(critiques),
        "failure_signals": compute_failure_signals(failed),
    }


def run(args: argparse.Namespace) -> dict:
    runs_dir = args.runs_dir
    passing_all, failed_all = find_run_dirs(runs_dir)
    passing = [d for d in passing_all if is_unreviewed(d)]
    failed = [d for d in failed_all if is_unreviewed(d)]
    total = len(passing) + len(failed)
    print(f"[analyze] runs_dir={runs_dir} unreviewed: passing={len(passing)} failed={len(failed)}", file=sys.stderr)

    if total < args.min_runs:
        return {
            "status": "SKIP",
            "reason": f"unreviewed runs ({total} = passing {len(passing)} + failed {len(failed)}) < min_runs ({args.min_runs})",
        }

    # (rubric_name) 그룹핑
    groups_passing: dict[str, list[Path]] = defaultdict(list)
    groups_failed: dict[str, list[Path]] = defaultdict(list)
    for d in passing:
        groups_passing[run_rubric_name(d)].append(d)
    for d in failed:
        groups_failed[run_rubric_name(d)].append(d)

    all_names = sorted(set(groups_passing) | set(groups_failed))
    groups = {
        name: analyze_group(name, groups_passing.get(name, []), groups_failed.get(name, []), args.min_group)
        for name in all_names
    }

    today = datetime.now(KST).date().isoformat()
    analysis_id = f"{today}_{total}runs"
    user_feedback = collect_user_feedback()
    analysis = {
        "analysis_id": analysis_id,
        "generated_at": now_iso(),
        "total_unreviewed": total,
        "min_group_sample": args.min_group,
        "groups": groups,
        "user_feedback": user_feedback,
        "signals": synthesize_signals(groups, user_feedback),
    }

    if args.dry_run:
        print(json.dumps(analysis, ensure_ascii=False, indent=2))
        return {"status": "DRY_RUN", "analysis_id": analysis_id, "groups": list(groups)}

    if args.mark_reviewed:
        for d in passing + failed:
            (d / REVIEWED_MARKER).write_text(now_iso() + "\n", encoding="utf-8")
        print(f"[analyze] marked {total} runs reviewed", file=sys.stderr)

    out_path = PROPOSALS_DIR / analysis_id / "analysis.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(analysis, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"[analyze] wrote {out_path}", file=sys.stderr)
    return {"status": "OK", "analysis_id": analysis_id, "analysis_path": str(out_path), "groups": list(groups)}


def main() -> int:
    p = argparse.ArgumentParser(description="미검토 run 신호를 (mode×rubric_name)별로 집계 → analysis.json")
    p.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR, help="runs/ 루트")
    p.add_argument("--min-runs", type=int, default=5, help="분석 발동 최소 미검토 run 수 (기본 5, §5-B)")
    p.add_argument("--min-group", type=int, default=3, help="그룹 제안 허용 최소 표본 (기본 3, §5-B)")
    p.add_argument("--mark-reviewed", action="store_true", help="(오케스트레이터) 분석한 run에 .reviewed 기록")
    p.add_argument("--dry-run", action="store_true", help="저장 없이 stdout 출력")
    args = p.parse_args()
    result = run(args)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result.get("status") in ("OK", "DRY_RUN", "SKIP") else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
