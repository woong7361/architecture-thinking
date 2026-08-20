"""run_propose.py

Slow loop 2단계 진입점: analysis.json → proposer 파이프라인 → proposal-final.md

Usage:
    python -B run_propose.py path/to/analysis_2026-07-05_5runs.json --provider claude
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.dont_write_bytecode = True

PIPELINE_DIR = Path(__file__).resolve().parent



def main() -> int:
    parser = argparse.ArgumentParser(description="Run the slow-loop proposer pipeline.")
    parser.add_argument("analysis", type=Path, help="analysis.json 경로")
    parser.add_argument("--provider", choices=["codex", "claude"], default="codex")
    parser.add_argument("--runs-dir", type=Path, default=None)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    args = parser.parse_args()

    analysis_path = args.analysis.resolve()
    if not analysis_path.exists():
        print(f"analysis file not found: {analysis_path}", file=sys.stderr)
        return 1

    sys.path.insert(0, str(PIPELINE_DIR))

    from stages.scripts.llm_client import create_client
    from stages.proposer import run_proposal_pipeline

    client = create_client(
        provider=args.provider,
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
    return 0 if result.get("status") == "PASS" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
