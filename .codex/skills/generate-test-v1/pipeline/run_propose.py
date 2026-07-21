"""run_propose.py

Slow loop 2단계 진입점: analysis.json → proposer 파이프라인 → proposal-final.md

proposal이 PASS로 생성되면, 분석에 반영된 run들에 `.reviewed` 마커를 기록해(§7·§9 step 4)
다음 사이클에서 다시 집계되지 않게 한다(폴더 이동 없음). 기본은 마킹, --no-mark로 끈다.

Usage:
    python -B run_propose.py path/to/analysis.json --provider codex
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.dont_write_bytecode = True

PIPELINE_DIR = Path(__file__).resolve().parent
DEFAULT_RUNS_DIR = PIPELINE_DIR.parent / "runs"


def _analyzed_run_names(analysis: dict) -> list[str]:
    names: list[str] = []
    for g in analysis.get("groups", {}).values():
        names += g.get("runs_included", []) + g.get("failed_runs_included", [])
    return names


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the slow-loop proposer pipeline.")
    parser.add_argument("analysis", type=Path, help="analysis.json 경로")
    parser.add_argument("--provider", choices=["codex", "claude"], default="codex")
    parser.add_argument("--runs-dir", type=Path, default=DEFAULT_RUNS_DIR)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--no-mark", action="store_true", help="PASS여도 .reviewed 마킹을 하지 않는다")
    args = parser.parse_args()

    analysis_path = args.analysis.resolve()
    if not analysis_path.exists():
        print(f"analysis file not found: {analysis_path}", file=sys.stderr)
        return 1

    sys.path.insert(0, str(PIPELINE_DIR))

    from stages.scripts.llm_client import create_client
    from stages.proposer import run_proposal_pipeline
    from analyze_runs import mark_reviewed_by_names

    client = create_client(
        provider=args.provider,
        project_dir=PIPELINE_DIR,
        timeout_seconds=args.timeout_seconds,
    )

    result = run_proposal_pipeline(
        analysis_path=analysis_path,
        client=client,
        model=None,
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))

    if result.get("status") == "PASS":
        print(f"proposal: {result.get('proposal_path')}")
        if not args.no_mark:
            analysis = json.loads(analysis_path.read_text(encoding="utf-8"))
            marked = mark_reviewed_by_names(args.runs_dir, _analyzed_run_names(analysis))
            print(f"marked reviewed ({len(marked)}): {', '.join(marked)}")
    return 0 if result.get("status") == "PASS" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
