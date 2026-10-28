"""사람이 읽는 리포트.

사람에게 남은 일은 채택·기각 하나다. 그 하나를 싸게 만드는 것이 이 파일이 하는 일이고, 핵심은 사본에서
그 줄 범위를 줄 번호와 함께 떠내는 것이다. 에이전트가 적은 인용문만 보여 주면 대조할 것이 없다.

**리포트는 그 run 이 낸 마지막 제안만 다룬다.** 반복 중에 철회되거나 고쳐진 주장은 원장이 갖는다.
리포트에 이력을 실으면 사람이 지금 판정할 것과 지나간 것을 가려 읽어야 한다.

정렬은 루브릭이 정한다. 하한 미달 축이 많은 순, 같으면 칸 합이 낮은 순이다. 주장 전부의 근거를
정독하는 것과 미달 주장부터 정독하는 것은 비용이 다르다.
"""

from __future__ import annotations

import json
from pathlib import Path

from gates.citation import normalize

NEWLINE = chr(10)


def excerpt(path: Path, span: tuple[int, int], pad: int = 2) -> list[str]:
    """사본에서 그 줄 범위를 줄 번호와 함께 떠낸다.

    이것이 대조의 오른쪽 칸이다. 에이전트가 적은 인용문만 보여 주면 대조할 것이 없다. 게이트가 문자열
    일치를 이미 확인했으므로 사람이 볼 것은 **그 줄이 사본에서 어떤 문맥에 있는가**다. 앞뒤 두 줄을
    함께 떠서 인용이 잘려 뜻이 바뀌었는지 보이게 한다.
    """
    lines = normalize(path.read_text(encoding="utf-8", errors="replace")).split("\n")
    start = max(1, span[0] - pad)
    end = min(len(lines), span[1] + pad)
    out = []
    for number in range(start, end + 1):
        mark = ">" if span[0] <= number <= span[1] else " "
        out.append(f"{mark} {number:5d} | {lines[number - 1]}")
    return out


def locate(path: Path, quote: str) -> tuple[int, int] | None:
    """인용문이 파일의 몇 번째 줄부터 몇 번째 줄까지인지.

    게이트가 이미 실존을 확인했으므로 여기서는 자리만 찾는다. 못 찾으면 None 을 돌려주고 리포트가
    그 사실을 적는다.
    """
    if not path.exists():
        return None
    lines = normalize(path.read_text(encoding="utf-8", errors="replace")).split("\n")
    needle = normalize(quote).strip().split("\n")
    if not needle:
        return None
    for index in range(len(lines) - len(needle) + 1):
        if [line.strip() for line in lines[index:index + len(needle)]] == \
           [line.strip() for line in needle]:
            return index + 1, index + len(needle)
    return None


def resolve(path_text: str, roots: dict[str, Path]) -> Path | None:
    for name, root in roots.items():
        if path_text == name or path_text.startswith(name + "/"):
            rel = path_text[len(name):].lstrip("/")
            return root / rel if rel else root
    return None


def axis_cells(scored: dict, rubric: dict, kind: str) -> dict[str, object]:
    floors = {axis["id"]: axis for axis in rubric["axes"]}
    axes = scored.get("axes") or {}
    cells: dict[str, object] = {}
    for axis_id, axis in floors.items():
        if kind not in axis["applies_to"]:
            cells[axis_id] = "해당없음"
            continue
        box = (axes.get(axis_id) or {}).get("box")
        if box is None:
            cells[axis_id] = "미판정"
        else:
            cells[axis_id] = f"{box}{'' if box >= axis['floor'] else ' ↓'}"
    return cells


def sort_key(claim: dict, scored: dict, rubric: dict) -> tuple[int, int]:
    """미달 축 수가 많은 순, 같으면 칸 합이 낮은 순."""
    floors = {axis["id"]: axis for axis in rubric["axes"]}
    axes = scored.get("axes") or {}
    below = 0
    total = 0
    for axis_id, axis in floors.items():
        if claim.get("kind") not in axis["applies_to"]:
            continue
        box = (axes.get(axis_id) or {}).get("box")
        if box is None:
            below += 1
            continue
        total += box
        if box < axis["floor"]:
            below += 1
    return (-below, total)


def site_lines(claim: dict, sites: dict[str, dict]) -> list[str]:
    """이 주장이 걸리는 코드 자리. 지점 목록에서 그대로 떠온다.

    주장 문장이 길고 기술 용어가 많아 그것만으로는 무엇에 대한 말인지 잡히지 않는다. 코드의 어느 줄에
    걸리는지를 함께 보이면 "내 코드의 이 자리에 대한 말"로 읽힌다.
    """
    out = []
    for site_id in claim.get("sites") or []:
        site = sites.get(site_id)
        if not site:
            continue
        where = site["site"].rsplit("/", 1)[-1]
        out.append(f"- `{where}` — {site['line']}")
    return out


def write_report(
    run_dir: Path,
    claims: list[dict],
    evaluation: dict,
    critique: dict,
    verdict: dict,
    rubric: dict,
    roots: dict[str, Path],
    sites: list[dict] | None = None,
    headlines: dict[str, dict] | None = None,
) -> Path:
    scored_by_id = {s.get("id"): s for s in evaluation.get("claims", [])}
    critique_by_id = {c.get("id"): c for c in critique.get("claims", [])}
    site_by_id = {s["id"]: s for s in (sites or [])}
    heads = headlines or {}
    ordered = sorted(
        claims, key=lambda c: sort_key(c, scored_by_id.get(c["id"], {}), rubric)
    )

    out: list[str] = []
    add = out.append

    add("# 소스 근거 리포트")
    add("")
    add("대상 코드가 기대고 있는 전제를 주장으로 뽑고, 그 주장의 근거를 라이브러리 소스와 공식 문서에서")
    add("찾아온 기록이다. 주장마다 **원문을 그대로 떠낸 자리**가 붙어 있어 소스를 따로 열지 않아도 된다.")
    add("")
    add(f"상태 **{verdict['status']}**. 이 상태는 주장이 참이라는 뜻이 **아니다.** 사람이 원문과 대조해")
    add("판정할 자료가 갖춰졌다는 뜻이고, 참·거짓은 사람이 채택·기각으로 정한다.")
    add("")
    add("읽는 순서는 셋이다.")
    add("")
    add("1. 아래 목록에서 무엇에 대한 주장들인지 훑는다.")
    add("2. 주장마다 `의심할 자리` 한 줄을 읽는다. 거기에 판정의 재료가 있다.")
    add("3. 걸리는 주장만 그 아래 `근거`의 원문을 본다.")
    add("")
    add("축 칸과 채점 근거 전체는 리포트에 싣지 않는다. 하한 판정에 이미 쓰였고 사람이 다시 읽을 것이")
    add("아니다. 필요하면 `eval.json` 에 있다. 다만 `grounding` 의 근거는 주장 문장의 어느 성분이")
    add("인용으로 덮이지 않는지를 적으므로 주장마다 따로 싣는다.")
    add("")

    # 1. 한눈에
    add("## 1. 주장 목록")
    add("")
    for claim in ordered:
        cid = claim["id"]
        head = heads.get(cid, {})
        kind = "사실" if claim.get("kind") == "fact" else "선택"
        clauses = len(critique_by_id.get(cid, {}).get("limiting_clauses") or [])
        label = head.get("headline") or claim["sentence"][:70] + "…"
        add(f"**[{cid}](#{cid})** · {kind}" + (f" · 제한 조항 {clauses}건" if clauses else ""))
        add("")
        add(f"{label}")
        add("")

    add("`사실` 은 원문이 그렇다고 말하는 것이고, `선택` 은 대상 코드가 그 방식을 택했다는 것이다.")
    add("둘은 묻는 것이 다르다. 사실은 원문이 정말 그렇게 말하는지, 선택은 무엇을 치르고 얻었는지다.")
    add("")
    add("`제한 조항` 은 그 주장을 깎으려고 반대 조사자가 따로 가져온 인용의 수다. 숫자가 큰 주장부터 읽는다.")
    add("")

    # 2. 주장마다
    add("## 2. 주장별")
    add("")
    for claim in ordered:
        cid = claim["id"]
        head = heads.get(cid, {})
        cells = axis_cells(scored_by_id.get(cid, {}), rubric, claim.get("kind", ""))
        shown = " ".join(f"{name[0].upper()}{value}" for name, value in cells.items()
                         if value != "해당없음")
        clauses = len(critique_by_id.get(cid, {}).get("limiting_clauses") or [])
        kind = "사실" if claim.get("kind") == "fact" else "선택"

        add("---")
        add("")
        add(f"### {cid}")
        add("")
        if head.get("headline"):
            add(f"**{head['headline']}**")
            add("")
        add(f"{kind} 주장 · 축 칸 {shown}" + (f" · 제한 조항 {clauses}건" if clauses else ""))
        add("")

        if head.get("bites"):
            add(f"무엇이 걸리는가 — {head['bites']}")
            add("")

        lines = site_lines(claim, site_by_id)
        if lines:
            add("**이 주장이 걸리는 코드 자리**")
            add("")
            for line in lines:
                add(line)
            add("")

        add("**주장 그대로**")
        add("")
        add(f"> {claim['sentence']}")
        add("")

        add("**의심할 자리**")
        add("")
        add(claim.get("claim_delta", "—"))
        add("")

        grounding = ((scored_by_id.get(cid, {}).get("axes") or {}).get("grounding") or {})
        if grounding.get("basis"):
            add("**채점자가 독립으로 읽은 것**")
            add("")
            add(grounding["basis"])
            add("")
            add("위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 "
                "않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.")
            add("")

        add("**원문이 말하는 것**")
        add("")
        add(claim.get("source_says", "—"))
        add("")

        add("**근거**")
        add("")
        add("`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.")
        add("")
        for quote in claim.get("citations") or []:
            target = resolve(quote["path"], roots)
            span = locate(target, quote["quote"]) if target else None
            if span:
                add(f"{quote['path']}:{span[0]}-{span[1]}")
                add("")
                add("```")
                for line in excerpt(target, span):
                    add(line)
                add("```")
            else:
                add(f"{quote['path']} — 사본에서 자리를 못 찾았다")
                add("")
                add("```")
                add(normalize(quote["quote"]).strip())
                add("```")
            add("")

        if claim.get("kind") == "choice":
            add("**무엇을 치르고 얻었는가**")
            add("")
            for item in claim.get("tradeoffs") or []:
                add(f"*{item['dimension']}* — {item['statement']}")
                add("")
                if item.get("premise"):
                    add(f"- 이 교환이 성립하는 전제: {item['premise']}")
                if item.get("inverts_at"):
                    add(f"- 뒤집히는 값: {item['inverts_at']}")
                add(f"- 좌표: `{item['coordinate']}`")
                add("")

        counter_claim = critique_by_id.get(cid, {})
        if counter_claim.get("limiting_clauses"):
            add("**반대 조사자가 따로 가져온 제한 조항**")
            add("")
            for item in counter_claim["limiting_clauses"]:
                add(f"- {item['limits']}")
                add(f"  - `{item['path']}`")
            add("")
        theirs = (counter_claim.get("source_says") or "").strip()
        if theirs:
            add("**반대 조사자의 독립 판독**")
            add("")
            add(theirs)
            add("")
            add("조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 "
                "`원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.")
            add("")
        if counter_claim.get("code_mismatch"):
            add(f"**코드 불일치 신고** {counter_claim['code_mismatch']}")
            add("")
        if counter_claim.get("amendment_proposal"):
            proposal = counter_claim["amendment_proposal"]
            add("**주장을 이렇게 고치자는 발의**")
            add("")
            add(f"> {proposal['sentence']}")
            add("")
            add(f"근거 — {proposal['because']}")
            add("")


    path = run_dir / "REPORT.md"
    path.write_text(NEWLINE.join(out) + NEWLINE, encoding="utf-8")
    return path


def main() -> int:
    import argparse
    import sys

    import yaml

    parser = argparse.ArgumentParser()
    parser.add_argument("run_dir")
    args = parser.parse_args()

    run_dir = Path(args.run_dir)
    pipeline_dir = Path(__file__).resolve().parent
    skill_root = pipeline_dir.parent
    repo_root = skill_root.parents[2]

    rubric = yaml.safe_load((pipeline_dir / "rubrics/claim.rubric.yaml").read_text(encoding="utf-8"))
    manifest = yaml.safe_load((skill_root / "rules/sources.yaml").read_text(encoding="utf-8"))
    roots = {s["root"]: skill_root / "sources" / s["root"] for s in manifest["roots"]}
    roots["copies"] = skill_root / "copies"

    def load(name: str) -> dict:
        path = run_dir / f"{name}.json"
        return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}

    gen = load("gen")
    if not gen:
        print("gen.json 이 없다", file=sys.stderr)
        return 2

    verdict = load("verdict") or {"status": "NOT_READY", "violations": [], "unmeasured": [], "below": []}
    path = write_report(
        run_dir=run_dir,
        claims=gen["claims"],
        evaluation=load("eval"),
        critique=load("critique"),
        verdict=verdict,
        rubric=rubric,
        roots=roots,
        sites=(json.loads((run_dir / "sites.json").read_text(encoding="utf-8"))["sites"]
               if (run_dir / "sites.json").exists() else None),
        headlines=({c["id"]: c for c in load("headline").get("claims", [])}),
    )
    print(f"리포트: {path}")
    return 0


if __name__ == "__main__":
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    raise SystemExit(main())
