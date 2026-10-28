"""확정된 주장 묶음에 읽기용 머리글을 붙인다.

파이프라인의 네 단계에 넣지 않았다. 단계는 판단 하나씩 맡고, 머리글은 판단이 아니라 표시다. 단계로
올리면 역할 프롬프트와 산출 스키마와 실패 경로가 하나씩 더 늘지만 늘어난 것이 판정을 바꾸지 않는다.

입력은 주장 문장과 차이뿐이다. **출처도 코드도 주지 않는다.** 주면 새 근거를 만들 수 있고, 그 근거는
어느 게이트도 보지 않는다.

사용법:
    python make_headlines.py <run 폴더> [--model <모델>]
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

PIPELINE_DIR = Path(__file__).resolve().parents[1]
SKILL_ROOT = PIPELINE_DIR.parent
sys.path.insert(0, str(PIPELINE_DIR))

from stages.scripts import payload as P  # noqa: E402
from stages.scripts.llm_client import SANDBOX_READ_ONLY, create_client  # noqa: E402
from validate import validate_output  # noqa: E402

for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")

SCHEMA = "headline_output.schema.json"


def load_claims(run_dir: Path) -> list[dict]:
    for name in ("claims.json", "refine.i1.json", "gen.i1.json", "gen.json"):
        path = run_dir / name
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))["claims"]
    raise FileNotFoundError(f"{run_dir} 에 주장 묶음이 없다")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("run_dir")
    parser.add_argument("--model")
    parser.add_argument("--timeout", type=int, default=900)
    args = parser.parse_args()

    run_dir = Path(args.run_dir)
    claims = load_claims(run_dir)

    # 받는 것은 문장과 차이와 종류뿐이다. 인용과 좌표와 지점은 넘기지 않는다.
    trimmed = [
        {"id": c["id"], "kind": c.get("kind"), "sentence": c["sentence"],
         "claim_delta": c.get("claim_delta", "")}
        for c in claims
    ]

    out_path = run_dir / "headline.json"
    schema_path = PIPELINE_DIR / "schemas" / SCHEMA
    client = create_client("claude", project_dir=SKILL_ROOT, timeout_seconds=args.timeout)
    client.run_prompt(
        system=P.read_prompt(PIPELINE_DIR / "prompts" / "headline.md"),
        user=P.block("CLAIMS_JSON", {"claims": trimmed}),
        output_schema=schema_path,
        output_path=out_path,
        model=args.model,
        sandbox=SANDBOX_READ_ONLY,
        raw_path=run_dir / "headline.raw.txt",
    )

    output = json.loads(out_path.read_text(encoding="utf-8"))
    result = validate_output(output, SCHEMA)
    if result["violations"]:
        for item in result["violations"]:
            print(f"{item['rule']} {item['where']} {item['detail'][:120]}", file=sys.stderr)
        return 1

    got = {c["id"] for c in output["claims"]}
    want = {c["id"] for c in claims}
    if got != want:
        print(f"주장 id 집합이 다르다. 빠진 것 {sorted(want - got)}, 더한 것 {sorted(got - want)}",
              file=sys.stderr)
        return 1

    print(f"머리글 {len(output['claims'])}개: {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
