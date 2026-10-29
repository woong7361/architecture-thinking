"""채택·기각 판정 시트를 만든다.

`REPORT.md` 는 원문과 대조하는 데 필요한 것을 다 담아서 길다. 이 시트는 **판정 하나를 내리는 데 필요한
최소**만 담는다. 사람이 먼저 이걸 읽고, 걸리는 주장만 리포트의 그 묶음을 연다.

판정은 리포트가 아니라 이 파일에 적는다. 리포트는 하네스가 다시 쓰므로 사람이 쓴 판정을 거기 두면
다음 run 에 덮인다.

사용법:
    python make_verdict_sheet.py <run 폴더> <출력 경로>
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")

FLOORS = {"grounding": 3, "depth": 3, "tradeoff_coverage": 3, "tradeoff_validity": 3}


def load(run: Path, name: str) -> dict:
    for candidate in (run / f"{name}.json", run / f"{name}.i2.json", run / f"{name}.i1.json"):
        if candidate.exists():
            return json.loads(candidate.read_text(encoding="utf-8"))
    return {}


def build(run: Path, report_rel: str) -> str:
    claims = load(run, "claims")["claims"]
    scored = {c["id"]: c for c in load(run, "eval").get("claims", [])}
    critique = {c["id"]: c for c in load(run, "critique").get("claims", [])}
    verdict = load(run, "verdict")

    missed: dict[str, list[str]] = {}
    for item in verdict.get("observations", []):
        missed.setdefault(item["point"], []).append(item["where"])

    def key(claim: dict) -> tuple:
        axes = scored.get(claim["id"], {}).get("axes", {})
        below = sum(1 for a, f in FLOORS.items() if a in axes and axes[a]["box"] < f)
        clauses = len(critique.get(claim["id"], {}).get("limiting_clauses") or [])
        return (-below, -clauses, sum(v["box"] for v in axes.values()))

    out: list[str] = []
    add = out.append

    add("# 단계 3 — 채택 / 기각")
    add("")
    add(f"판정은 여기 적는다. [{report_rel}]({report_rel}) 는 하네스가 다시 쓰므로 거기 적으면 덮인다.")
    add("")
    add("## 무엇을 보고 정하는가")
    add("")
    add("`READY` 는 주장이 참이라는 뜻이 아니라 **대조할 자료가 갖춰졌다**는 뜻이다. 참·거짓은 이 표에서 정한다.")
    add("")
    add("주장마다 한 줄만 보면 된다. **주장과 원문의 차이**다. 게이트가 인용의 실존을 이미 확인했으므로 "
        "남은 위험은 진짜 인용으로 원문보다 넓은 주장을 뒷받침하는 것이고, 그 차이가 거기 적혀 있다.")
    add("")
    add("그 줄이 걸리면 리포트의 그 묶음을 연다. 사본에서 떠낸 자리가 줄 번호와 함께 있다.")
    add("")
    add("## 한눈에")
    add("")
    add("| id | 종류 | 축 칸 | 제한 조항 | 채택·기각 | 이유 |")
    add("| --- | --- | --- | ---: | --- | --- |")
    for claim in sorted(claims, key=key):
        axes = scored.get(claim["id"], {}).get("axes", {})
        cells = " ".join(f"{a[0].upper()}{axes[a]['box']}" for a in FLOORS if a in axes)
        clauses = len(critique.get(claim["id"], {}).get("limiting_clauses") or [])
        add(f"| [{claim['id']}](#{claim['id']}) | {claim.get('kind')} | {cells} | {clauses or ''} |  |  |")
    add("")
    add("축 칸은 `G` grounding, `D` depth, `T` tradeoff_coverage, `T` tradeoff_validity 순이다. "
        "사실 주장은 앞의 둘만 있다. `제한 조항` 은 반대 조사자가 그 주장을 깎으려고 가져온 인용의 수다.")
    add("")
    add("## 주장별")
    add("")

    for claim in sorted(claims, key=key):
        cid = claim["id"]
        add(f"### {cid}")
        add("")
        add(f"> {claim['sentence']}")
        add("")
        add(f"**차이** {claim.get('claim_delta', '—')}")
        add("")
        paths = [q["path"] for q in claim.get("citations") or []]
        add("근거 " + ", ".join(f"`{p.rsplit('/', 1)[-1]}`" for p in paths))
        add("")
        if cid in missed:
            add("반대 조사자만 가져온 조항")
            for where in missed[cid]:
                add(f"- `{where}`")
            add("")
        add("판정 — ")
        add("")
        add("이유 — ")
        add("")

    add("## 기각이 하나도 없으면")
    add("")
    add("그 기록은 \"AI가 맞다고 한다\"를 한 겹 포장한 것이다. 게이트가 환각 인용을 전부 잡고 나면 남는 "
        "위험은 진짜 인용으로 원문보다 넓은 주장을 뒷받침하는 것이고, 그것은 사람이 원문을 열어야 보인다.")
    add("")
    return "\n".join(out) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("run_dir")
    parser.add_argument("out")
    parser.add_argument("--report", default="REPORT.md")
    parser.add_argument("--force", action="store_true",
                        help="사람이 쓴 판정이 있어도 덮어쓴다")
    args = parser.parse_args()

    out = Path(args.out)
    # 사람이 쓴 판정을 덮지 않는다. 그 판정이 이 하네스의 산출물이고 다시 만들 수 없다.
    if out.exists() and not args.force:
        existing = out.read_text(encoding="utf-8")
        filled = [line for line in existing.splitlines()
                  if line.startswith("판정 — ") and line.strip() != "판정 —"]
        if filled:
            print(f"{out} 에 사람이 쓴 판정 {len(filled)}건이 있다. 덮지 않는다. "
                  f"정말 다시 만들려면 --force 를 준다.", file=sys.stderr)
            return 1

    text = build(Path(args.run_dir), args.report)
    out.write_text(text, encoding="utf-8")
    print(f"판정 시트: {args.out} ({len(text.splitlines())}줄)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
