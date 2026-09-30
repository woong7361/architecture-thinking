"""리포트. `<run_dir>/REPORT.md`를 쓴다.

사람이 훑는 문서다. 원장이 기계가 읽는 판이고 이것이 사람이 읽는 판이다. 절은 여섯이고 위험이 큰 것이 위로
온다. 요약, 계약을 이렇게 고쳤다, 그 변경이 무엇을 바꾸는가, 계약이 정하지 않아 내가 고른 것, 고친 것,
주장과 판정이 어긋난 곳이다. 통과한 run에서도 나온다. 통과 여부만 알려 주면 판단을 기록한 뜻이 없다.

2절과 3절이 사람이 받는 실물이다. 계약 판본 하나와 무엇이 왜 바뀌었는지의 목록이 함께 있으므로 사람은
읽고 받아들일지만 정한다. 계약을 열어 자리를 찾아 고치는 일이 빠진다.

절 안의 순서도 위험이 정한다. 계약 변경은 깨는 변경이 위로 오고, 결정은 신뢰도가 낮거나 파급이 넓은 것이
위로 온다. 표를 쓰되 표만으로 끝내지 않고 각 절의 첫 줄에 그 절이 답하는 것을 한 문장으로 적는다. 표는
좌표와 라벨을 나란히 놓을 때만 쓰고, 이유와 흐름은 문장으로 쓴다.

`write_report(run_dir, ledger, result)`를 부른다. `ledger`는 `ledger.update_ledger`가 돌려준 것이고
`result`는 runner가 합산 자리에서 만든 payload다. 원장이 없으면 리포트는 요약만 낸다.
"""

from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone
from pathlib import Path

KST = timezone(timedelta(hours=9))
REPORT_NAME = "REPORT.md"

CONFIDENCE_ORDER = {"low": 0, "medium": 1, "high": 2}

# 계약 변경의 순서. 약속을 줄인 변경이 표현만 바꾼 변경보다 위에 온다.
#
# 깨는 변경보다 약속 축소를 위에 두는 이유는 둘이 답하는 질문이 다르기 때문이다. 깨는 변경은 지금 쓰는 쪽이
# 부서지는지를 말하고, 약속 축소는 계약이 무엇을 보장하기를 그만두었는지를 말한다. 앞의 것은 이행으로 감당할 수
# 있고 뒤의 것은 감당할 방법이 없다 — 지운 약속은 아무 장치도 다시 보지 않는다. 그래서 종류를 1순위,
# 호환성을 2순위로 둔다.
KIND_ORDER = {"promise": 0, "representation": 1}
VERDICT_ORDER = {"breaking": 0, "uncompared": 1}

# 차이의 라벨을 위험한 순서로 둔다. 읽는 쪽이 깨지는 것과 계약대로 부를 수 없는 것이 먼저다.
#
# 순서는 게이트의 `worst()`와 같게 맞춘다. 같은 어휘에 두 가지 심각도가 있으면 게이트가 한 건으로 접을 때 고른
# 라벨과 리포트가 위로 올리는 라벨이 어긋난다. 약속이 깨진 것이 계약 밖이 자란 것보다 위인데, 뒤의 것은 아무
# 약속도 깨지 않는다. `accepts_what_contract_rejects`는 계약대로 부르는 쪽을 막지는 않지만 계약이 막겠다고 한
# 값이 그 자리로 들어오므로, 아무 약속도 깨지 않는 초과보다 위다.
LABEL_ORDER = {
    "breaks_reader": 0,
    "rejects_contracted_request": 1,
    "withholds_promised_response": 2,
    "accepts_what_contract_rejects": 3,
    "undeclared_surface": 4,
    "harmless_to_client": 5,
}

# 이 표에 없는 라벨의 자리. 맨 위다.
#
# 모르는 라벨을 맨 아래로 떨어뜨리면 게이트가 라벨을 새로 만들 때마다 그 위반이 조용히 바닥으로 간다. 실제로
# 그렇게 됐다 — 새 라벨이 붙은 위반 43건이 `harmless_to_client` 아래에 깔렸다. 모르는 것을 무해한 것보다
# 아래에 두는 것은 미판정을 통과로 세는 것과 같은 잘못이다. 위로 올리고 이름을 적어 사람이 보게 한다.
UNKNOWN_LABEL_RANK = -1


def label_rank(label) -> int:
    """정렬에 쓰는 라벨의 자리. 모르는 라벨은 맨 위, 라벨이 없는 항목은 맨 아래다."""
    if not label:
        return len(LABEL_ORDER)
    return LABEL_ORDER.get(str(label), UNKNOWN_LABEL_RANK)


def unknown_labels(items: list[dict]) -> list[str]:
    """이 리포트가 순서를 모르는 라벨. 게이트의 어휘가 늘었다는 신호다."""
    return sorted({str(i.get("label")) for i in items
                   if i.get("label") and str(i["label"]) not in LABEL_ORDER})
STATUS_ORDER = {"stuck": 0, "regressed": 1, "open": 2, "closed": 3}
SECTION_TITLES = [
    "1. 요약",
    "2. 계약을 이렇게 고쳤다",
    "3. 그 변경이 무엇을 바꾸는가",
    "4. 계약이 정하지 않아 내가 고른 것",
    "5. 고친 것",
    "6. 주장과 판정이 어긋난 곳",
]


# ── 표 ──────────────────────────────────────────────────────────────────────

def cell(value) -> str:
    """표 한 칸. 줄바꿈과 파이프가 표를 깨뜨리지 못하게 한다."""
    if value is None or value == "":
        return "—"
    if isinstance(value, (list, tuple, set)):
        value = ", ".join(str(v) for v in value) or "—"
    text = str(value).replace("\r\n", " ").replace("\n", " ").replace("|", "\\|").strip()
    return text or "—"


def table(headers: list[str], rows: list[list]) -> list[str]:
    if not rows:
        return []
    lines = ["| " + " | ".join(headers) + " |", "| " + " | ".join("---" for _ in headers) + " |"]
    lines += ["| " + " | ".join(cell(c) for c in row) + " |" for row in rows]
    return lines


def bullet_list(items: list[str]) -> list[str]:
    return [f"- {item}" for item in items]


# ── 절 ──────────────────────────────────────────────────────────────────────

def summary_section(ledger: dict, result: dict) -> list[str]:
    """이 run이 어디까지 갔는가."""
    impact = ledger.get("impact") or {}
    violations = ledger.get("violations") or []
    decisions = [d for d in (ledger.get("decisions") or []) if d.get("status") == "standing"]
    changes = ledger.get("contract_changes") or []
    iterations = ledger.get("iterations") or []

    by_code = [v for v in violations if v.get("closed_by") == "code"]
    by_contract = [v for v in violations if v.get("closed_by") == "contract"]
    still_open = [v for v in violations if v.get("status") in ("open", "regressed", "stuck")]
    risk = (result.get("decision_risk") or impact.get("decision_risk") or {})

    causes = {cause_key(v) for v in violations}

    lines = ["이 run이 무엇을 남겼는지 한 자리에서 본다.", ""]
    lines += pass_bar_lines(ledger)
    lines.append("")
    lines.append(
        f"판정은 **{result.get('status') or '—'}**이고 iteration {result.get('iteration') or '—'}까지 왔다. "
        f"위반 {len(violations)}건 가운데 {len(by_code)}건을 코드로 닫고 {len(by_contract)}건을 계약으로 닫았으며 "
        f"{len(still_open)}건이 아직 열려 있다. 서 있는 결정은 {len(decisions)}건, 계약 변경 기록은 {len(changes)}건이다."
    )
    if violations:
        lines.append("")
        lines.append(
            f"위반은 지점 단위로 센 것이고 그 뒤의 원인은 {len(causes)}개다. 고칠 일의 개수는 원인 쪽이므로 "
            "5절을 원인으로 묶어 함께 낸다."
        )
    version = impact.get("contract_version") or {}
    if version.get("path"):
        lines.append("")
        lines.append(
            f"계약은 기준선 `{impact.get('baseline_contract') or '—'}`에서 출발해 `{version['path']}`까지 왔다. "
            f"바로 앞 판본은 `{version.get('from') or '—'}`이다."
        )
    if risk:
        lines.append("")
        reasons = risk.get("reasons") or []
        tail = (" 근거는 " + "; ".join(str(r) for r in reasons) + "다.") if reasons else " 올릴 근거는 없다."
        lines.append(f"`decision_risk`는 **{risk.get('level') or '—'}** 수준이다.{tail}")

    if by_contract:
        lines.append("")
        lines.append(
            "계약으로 닫힌 위반이 있으므로 그 자리는 고친 것이 아니라 요구를 옮긴 것이다. "
            "2절과 5절을 함께 읽어야 무엇이 사라졌는지 보인다."
        )

    gate_errors = result.get("gate_errors") or []
    rubric_errors = result.get("rubric_errors") or []
    if gate_errors or rubric_errors:
        lines.append("")
        lines.append("REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.")
        lines.append("")
        if gate_errors:
            lines.append("게이트:")
            lines += bullet_list([str(e) for e in gate_errors])
        if rubric_errors:
            lines.append("")
            lines.append("루브릭:")
            lines += bullet_list([str(e) for e in rubric_errors])

    drift = [o for o in (ledger.get("observations") or []) if o.get("kind") == "manifest_drift"]
    if drift:
        lines.append("")
        lines.append(
            f"색인과 실물이 어긋난 경로가 {len(drift)}곳이다. 게이트는 색인이 선언한 것을 판정하므로 "
            "선언 밖에 놓인 파일은 판정되지 않은 표면이 된다."
        )
        lines.append("")
        lines += table(["경로", "iteration", "관찰"],
                       [[o.get("where"), o.get("iteration"), o.get("detail")] for o in drift])

    no_commit = [o for o in (ledger.get("observations") or []) if o.get("kind") == "commit_missing"]
    if no_commit:
        lines.append("")
        lines.append(
            f"커밋 해시가 없어 작업 폴더의 앞 상태를 git에서 되짚지 못한 자리가 {len(no_commit)}건이다. "
            "변경의 셈은 해시 색인으로 그대로 나오고 발췌 diff만 빠진다."
        )
        lines.append("")
        lines += table(["iteration", "관찰"], [[o.get("iteration"), o.get("detail")] for o in no_commit])

    unjudged = [o for o in (ledger.get("observations") or []) if o.get("kind") == "gate_unjudged"]
    if unjudged or result.get("skipped_reason"):
        lines.append("")
        names = sorted({str(o.get("where")) for o in unjudged})
        note = f"판정되지 않은 검사가 있다: {', '.join(names)}." if names else ""
        skipped = f" 건너뛴 단계의 이유는 {result['skipped_reason']}다." if result.get("skipped_reason") else ""
        lines.append(f"{note}{skipped} 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.".strip())

    if iterations:
        lines.append("")
        lines.append(
            "합격선은 두 문장이 함께 참인 상태다. **판정했다**와 **그리고 위반이 없다**를 같은 칸에 넣지 않는다. "
            "뒤의 문장만 적으면 게이트를 부수는 것이 게이트를 통과하는 가장 쉬운 길이 된다."
        )
        lines.append("")
        lines += table(
            ["iteration", "판정", "판정했다", "위반 없다", "G0", "G1", "판본 대조", "위반", "기록", "총점"],
            [[
                e.get("iteration"), e.get("status"),
                _mark((e.get("machine_verdict") or {}).get("judged")),
                _mark((e.get("machine_verdict") or {}).get("no_violation")),
                (e.get("gate") or {}).get("g0"), (e.get("gate") or {}).get("g1"),
                (e.get("gate") or {}).get("changes"),
                (e.get("gate") or {}).get("violations"), (e.get("gate") or {}).get("observations"),
                ((e.get("eval") or {}) or {}).get("weighted_total"),
            ] for e in iterations],
        )

        gaps = [(e.get("iteration"), gap)
                for e in iterations
                for gap in (list((e.get("machine_verdict") or {}).get("judged_gaps") or [])
                            + list((e.get("machine_verdict") or {}).get("violation_gaps") or []))]
        if gaps:
            lines.append("")
            lines.append("합격선을 넘지 못한 자리마다 무엇에 걸렸는지 적는다.")
            lines.append("")
            lines += table(["iteration", "무엇에 걸렸는가"], [[name, gap] for name, gap in gaps])
    return lines


def _mark(value) -> str:
    """세 값이다. 참, 거짓, 그리고 말할 수 없음."""
    if value is True:
        return "예"
    if value is False:
        return "아니다"
    return "말할 수 없다"


def pass_bar_lines(ledger: dict) -> list[str]:
    """리포트를 처음 읽는 사람이 찾는 한 줄. 기계적으로 깨끗한 iteration이 있었는가."""
    bar = ledger.get("pass_bar") or {}
    if not bar:
        return ["기계 판정의 합격선을 계산하지 못했다. 원장에 iteration이 없다."]
    clean = bar.get("clean_iterations") or []
    if clean:
        return [
            f"**기계 판정이 깨끗한 iteration이 있다: {', '.join(clean)}.** 그 iteration에서 G0와 G1과 판본 대조가 "
            "모두 판정되었고 위반이 없었다. v0의 합격선은 최종 판정 PASS가 아니라 이것이다."
        ]

    blocked = bar.get("blocked") or {}
    judged = bar.get("judged_but_violating") or []
    if judged:
        head = (f"**기계 판정이 깨끗한 iteration은 없다.** 다만 {', '.join(judged)}은 판정까지는 갔고 "
                "위반이 남아 걸렸다.")
    else:
        head = "**기계 판정이 깨끗한 iteration은 없고, 판정 자체가 끝난 iteration도 없다.**"
    first = next(iter(blocked.items()), None)
    if first and first[1]:
        name, reasons = first
        head += f" iteration {name}은 여기서 걸렸다: {reasons[0]}."
    return [head]


def contract_changes_section(ledger: dict) -> list[str]:
    """계약을 어디서 어떻게 고쳤는가."""
    changes = list(ledger.get("contract_changes") or [])
    if not changes:
        return ["계약 판본은 이 run에서 움직이지 않았다. 고칠 자리를 찾지 못했거나 고칠 필요가 없었다는 뜻이다."]

    def rank(change: dict) -> tuple:
        # 약속을 줄인 변경이 맨 위다. 그 안에서 깨는 변경이, 그다음 판정하지 못한 변경이 온다.
        # 미판정은 안전한 쪽이 아니므로 호환되는 변경 위에 둔다.
        kind = KIND_ORDER.get(str(change.get("kind")), 2)
        verdict = VERDICT_ORDER.get(str(change.get("compatibility_verdict")), 2)
        claimed = 0 if change.get("compatibility_claimed") == "breaking" else 1
        return (kind, verdict, claimed, str(change.get("iteration") or ""), str(change.get("id")))

    changes.sort(key=rank)
    breaking = [c for c in changes if c.get("compatibility_verdict") == "breaking"]
    uncompared = [c for c in changes if c.get("compatibility_verdict") == "uncompared"]

    promise = [c for c in changes if c.get("kind") == "promise"]

    lines = ["계약이 어느 좌표에서 무엇 때문에 움직였는지 적는다. 약속을 줄인 변경이 위로 온다.", ""]
    tail = (
        f" 대조기가 참조를 풀지 못해 판정하지 못한 변경이 {len(uncompared)}건 있고, 그 자리는 깨는 변경으로도 "
        "호환되는 변경으로도 세지 않는다."
    ) if uncompared else ""
    lines.append(
        f"변경은 {len(changes)}건이고 그중 {len(promise)}건이 계약의 약속을 줄였다. "
        f"대조기 판정으로 깨는 변경은 {len(breaking)}건이다. "
        f"호환성은 모델의 라벨이 아니라 대조기가 정하므로 두 값을 나란히 싣는다.{tail}"
    )
    lines.append("")
    lines.append(
        "약속 변경과 표현 변경을 가르는 것은 판정 지점과 필수와 에러 코드 쌍과 산문 슬롯이다. "
        "그 가운데 하나라도 줄면 약속 변경이고, 이름과 설명만 달라진 것은 표현 변경이다. "
        "지운 약속은 이후 어느 장치도 다시 보지 않으므로 이 절의 맨 위에 둔다."
    )
    lines.append("")
    lines += table(
        ["id", "종류", "좌표", "무엇", "왜", "발의", "명세 근거", "라벨", "대조기", "iteration"],
        [[
            c.get("id"), c.get("kind"), c.get("target"), c.get("what"), c.get("why"),
            c.get("basis"), c.get("spec_anchor"),
            c.get("compatibility_claimed"), c.get("compatibility_verdict"), c.get("iteration"),
        ] for c in changes],
    )

    for change in promise:
        lines.append("")
        lines.append(f"`{change.get('id')}`이 줄인 약속은 이렇다.")
        lines += bullet_list([str(line) for line in (change.get("promise_lines") or [])]) or ["- —"]

    for change in breaking:
        lines.append("")
        lines.append(f"`{change.get('id')}`이 깨는 변경인 이유는 대조기가 다음을 찾았기 때문이다.")
        lines += bullet_list([str(line) for line in (change.get("breaking_lines") or [])]) or ["- —"]
        if change.get("migration"):
            lines.append(f"이행 계획은 이렇게 적혀 있다. {change['migration']}")

    for change in uncompared:
        lines.append("")
        lines.append(f"`{change.get('id')}`은 대조기가 다음 자리를 보지 못해 호환성을 판정하지 못했다.")
        lines += bullet_list([str(line) for line in (change.get("uncompared_lines") or [])]) or ["- —"]

    closed_by_contract = [v for v in (ledger.get("violations") or []) if v.get("closed_by") == "contract"]
    if closed_by_contract:
        lines.append("")
        lines.append("이 변경들 때문에 닫힌 위반이 있다. 고친 것이 아니라 요구가 사라진 자리다.")
        lines.append("")
        lines += table(
            ["위반", "규칙", "판정 지점", "좌표", "왜 계약으로 닫혔다고 보는가"],
            [[v.get("id"), v.get("rule"), v.get("point"), v.get("where"), v.get("closed_because")]
             for v in closed_by_contract],
        )
    return lines


def impact_section(ledger: dict) -> list[str]:
    """그 변경이 판정 지점과 두 분모에 무엇을 했는가."""
    impact = ledger.get("impact") or {}
    baseline = impact.get("baseline_diff") or {}
    origin = impact.get("origin_diff") or {}
    step = impact.get("step_diff") or {}

    lines = ["계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.", ""]
    lines.append(
        f"이 run의 기준선 `{impact.get('baseline_contract') or '—'}` 대비로는 좌표 "
        f"{len(baseline.get('pointers') or [])}곳이 달라지고 깨는 변경 {len(baseline.get('breaking') or [])}건이 나왔다. "
        f"사람이 확정한 원본 `{impact.get('origin_contract') or '—'}` 대비 누적으로는 좌표 "
        f"{len(origin.get('pointers') or [])}곳이 달라지고 깨는 변경 {len(origin.get('breaking') or [])}건이다. "
        "run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다."
    )
    lines.append("")
    lines += table(
        ["분모", "달라진 좌표", "깨는 변경", "대조하지 못한 자리", "사라진 판정 지점"],
        [
            ["앞 판본", len(step.get("pointers") or []), len(step.get("breaking") or []),
             len(step.get("uncompared") or []), "—"],
            ["이 run의 기준선", len(baseline.get("pointers") or []), len(baseline.get("breaking") or []),
             len(baseline.get("uncompared") or []), impact.get("points_gone") or "—"],
            ["사람이 확정한 원본", len(origin.get("pointers") or []), len(origin.get("breaking") or []),
             len(origin.get("uncompared") or []), origin.get("points_gone") or "—"],
        ],
    )

    unjudged = [o for o in (ledger.get("observations") or []) if o.get("kind") == "contract_uncompared"]
    failures = [o for o in (ledger.get("observations") or []) if o.get("kind") == "comparator_failed"]
    if unjudged or failures:
        lines.append("")
        lines.append(
            "대조기가 보지 못한 자리가 있다. 깨는 변경이 아니라 미판정이므로 위의 수에 섞지 않았다. "
            "참조가 가리키는 컴포넌트가 사라졌거나 이름이 달라졌을 때 이렇게 되고, 그 아래의 약속은 이 run에서 "
            "대조되지 않았다."
        )
        lines.append("")
        lines += table(["좌표", "iteration", "관찰"],
                       [[o.get("where"), o.get("iteration"), o.get("detail")] for o in unjudged + failures])

    gone = impact.get("points_gone") or []
    born = impact.get("points_born") or []
    if gone or born:
        lines.append("")
        lines.append(
            f"판정 지점은 {len(gone)}개가 사라지고 {len(born)}개가 생겼다. 사라진 지점은 앞으로 아무 장치도 보지 않는 자리다."
        )
        if gone:
            lines.append("")
            lines.append("사라진 지점: " + ", ".join(f"`{p}`" for p in gone))
        if born:
            lines.append("")
            lines.append("생긴 지점: " + ", ".join(f"`{p}`" for p in born))

    pointers = baseline.get("pointers") or []
    if pointers:
        lines.append("")
        lines.append("기준선 대비 달라진 좌표는 이렇다.")
        lines.append("")
        head = pointers[:30]
        lines += bullet_list([f"`{p}`" for p in head])
        if len(pointers) > len(head):
            lines.append(f"- 그 밖에 {len(pointers) - len(head)}곳")

    oscillation = [o for o in (ledger.get("observations") or []) if o.get("kind") == "contract_oscillation"]
    if oscillation:
        latest: dict[str, dict] = {}
        for item in oscillation:
            latest[str(item.get("where"))] = item
        lines.append("")
        lines.append("같은 좌표를 여러 번 흔든 자리가 있다. 계약의 공백일 수 있으므로 slow loop의 재료가 된다.")
        lines.append("")
        lines += table(["좌표", "관찰"], [[k, v.get("detail")] for k, v in sorted(latest.items())])

    mislabeled = [o for o in (ledger.get("observations") or []) if o.get("kind") == "mislabeled_change"]
    if mislabeled:
        lines.append("")
        lines.append("모델이 붙인 호환성 라벨이 대조기 판정과 어긋난 자리다. 판정은 대조기를 따른다.")
        lines.append("")
        lines += table(["좌표", "관찰", "iteration"],
                       [[o.get("where"), o.get("detail"), o.get("iteration")] for o in mislabeled])
    return lines


def decisions_section(ledger: dict) -> list[str]:
    """계약이 비운 자리에서 무엇을 골랐는가."""
    decisions = [d for d in (ledger.get("decisions") or []) if d.get("status") != "withdrawn"]
    if not decisions:
        return ["계약이 비운 자리에서 고른 것을 신고한 기록이 없다. 고른 것이 없었거나 신고하지 않았다는 뜻이다."]

    def rank(decision: dict) -> tuple:
        confidence = CONFIDENCE_ORDER.get(str(decision.get("confidence")), 3)
        return (confidence, -len(decision.get("blast_radius") or []), str(decision.get("id")))

    decisions.sort(key=rank)
    low = [d for d in decisions if d.get("confidence") == "low"]

    lines = ["계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.", ""]
    lines.append(
        f"신고된 결정은 {len(decisions)}건이고 그중 {len(low)}건이 신뢰도 낮음이다. "
        "이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다."
    )
    lines.append("")
    lines += table(
        ["id", "질문", "고른 것", "근거 종류", "신뢰도", "되돌리는 비용", "닿는 판정 지점", "상태"],
        [[
            d.get("id"), d.get("question"), d.get("chosen"), d.get("basis"),
            d.get("confidence"), d.get("reversal_cost"), d.get("blast_radius"), d.get("status"),
        ] for d in decisions],
    )

    for decision in decisions:
        if decision.get("rationale"):
            lines.append("")
            lines.append(f"`{decision.get('id')}` — {decision['rationale']}")
    return lines


def cause_key(violation: dict) -> tuple:
    """원인 하나를 가리키는 좌표. 규칙과 자리와 세부가 같은 위반은 같은 결함이다."""
    return (str(violation.get("rule") or ""), str(violation.get("where") or ""),
            str(violation.get("detail") or ""))


def group_by_cause(violations: list[dict]) -> list[dict]:
    """지점 단위의 위반을 원인 단위로 묶는다.

    계약이 같은 컴포넌트를 다섯 응답에서 참조하면 그 컴포넌트 하나의 결함이 다섯 지점에서 잡힌다. 지점 단위로만
    보여 주면 사람이 "위반 143건"을 143개의 일로 읽는다. 원장은 지점 단위를 그대로 들고 간다 — 어느 지점이
    열려 있는지는 지점 단위로만 말할 수 있다. 사람이 읽는 판만 원인으로 접는다.
    """
    groups: dict[tuple, dict] = {}
    for violation in violations:
        key = cause_key(violation)
        group = groups.setdefault(key, {
            "rule": violation.get("rule"),
            "family": violation.get("family"),
            "where": violation.get("where"),
            "detail": violation.get("detail"),
            "why_it_matters": violation.get("why_it_matters"),
            "label": violation.get("label"),
            "label_meaning": violation.get("label_meaning"),
            "ids": [],
            "points": [],
            "statuses": set(),
            "closed_by": set(),
            "evidence": [],
            "claimed": [],
            "files": [],
            "diff": "",
            "note": "",
        })
        group["ids"].append(str(violation.get("id")))
        if violation.get("point"):
            group["points"].append(str(violation["point"]))
        group["statuses"].add(str(violation.get("status")))
        if violation.get("closed_by"):
            group["closed_by"].add(str(violation["closed_by"]))
        for item in (violation.get("evidence") or []):
            if item not in group["evidence"]:
                group["evidence"].append(item)
        if violation.get("claimed_fix") and violation["claimed_fix"] not in group["claimed"]:
            group["claimed"].append(str(violation["claimed_fix"]))
        verified = violation.get("verified_fix") or {}
        for path in (verified.get("files") or []):
            if path not in group["files"]:
                group["files"].append(path)
        # 발췌는 한 번만 싣는다. 같은 원인의 지점마다 같은 diff를 반복하면 원인으로 묶은 뜻이 없다.
        if verified.get("diff") and not group["diff"]:
            group["diff"] = verified["diff"]
        if verified.get("note") and not group["note"]:
            group["note"] = str(verified["note"])
    for group in groups.values():
        group["open"] = sorted(group["statuses"] - {"closed"})
        group["status"] = min(group["statuses"], key=lambda s: STATUS_ORDER.get(s, 4))
    return list(groups.values())


def repairs_section(ledger: dict) -> list[str]:
    """무엇이 닫혔고 무엇이 아직 열려 있는가."""
    violations = list(ledger.get("violations") or [])
    if not violations:
        return ["게이트가 잡은 위반이 없다. 판정 장치가 다 돌았다면 이 run은 위반 없이 왔다는 뜻이다."]

    violations.sort(key=lambda v: (STATUS_ORDER.get(str(v.get("status")), 4),
                                   label_rank(v.get("label")),
                                   0 if v.get("closed_by") == "contract" else 1,
                                   str(v.get("id"))))
    closed = [v for v in violations if v.get("status") == "closed"]
    stuck = [v for v in violations if v.get("status") == "stuck"]

    groups = group_by_cause(violations)
    groups.sort(key=lambda g: (STATUS_ORDER.get(str(g.get("status")), 4),
                               label_rank(g.get("label")),
                               -len(g["points"]), str(g.get("rule"))))
    wide = [g for g in groups if len(g["points"]) > 1]

    lines = ["위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.", ""]
    lines.append(
        f"위반 {len(violations)}건 중 {len(closed)}건이 닫혔다. 굳은 것은 {len(stuck)}건이다. "
        "닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다."
    )
    labels = {}
    for group in groups:
        if group.get("label"):
            labels[str(group["label"])] = str(group.get("label_meaning") or "")
    if labels:
        lines.append("")
        lines.append(
            "라벨은 그 차이가 쓰는 사람에게 어떤 뜻인지다. 판정을 가르지 않고 이 절의 순서만 정한다. "
            "위반에 붙는 라벨만 여기 온다 — 응답의 값을 계약보다 좁게 선언한 것처럼 손해가 없는 자리는 위반이 "
            "아니라 기록이고 6절에 있다."
        )
        lines.append("")
        lines += table(["라벨", "뜻"],
                       [[name, labels[name]] for name in
                        sorted(labels, key=label_rank)])

    unknown = unknown_labels(groups)
    if unknown:
        lines.append("")
        lines.append(
            f"이 리포트가 순서를 모르는 라벨이 있다: {', '.join(f'`{name}`' for name in unknown)}. "
            "게이트의 어휘가 늘었다는 뜻이므로 맨 위에 두었다. 모르는 것을 무해한 것 아래에 깔면 새 라벨이 붙은 "
            "위반이 조용히 바닥으로 간다."
        )

    lines.append("")
    lines.append(
        f"그 {len(violations)}건은 원인 {len(groups)}개에서 나왔다. "
        + (f"그중 {len(wide)}개가 여러 판정 지점에 걸쳐 {sum(len(g['points']) for g in wide)}건을 만들었다. "
           "계약이 같은 컴포넌트를 여러 응답에서 참조하면 결함 하나가 지점 수만큼 세어지므로, 고칠 일의 개수는 "
           "위반의 개수가 아니라 원인의 개수다."
           if wide else "한 원인이 한 지점에만 걸렸으므로 고칠 일의 개수가 위반의 개수와 같다.")
    )
    lines.append("")
    lines += table(
        ["원인", "규칙", "family", "좌표", "라벨", "닿는 지점", "상태", "닫은 방법"],
        [[
            ", ".join(g["ids"][:3]) + (f" 외 {len(g['ids']) - 3}건" if len(g["ids"]) > 3 else ""),
            g.get("rule"), g.get("family"), g.get("where"), g.get("label"),
            len(g["points"]), g.get("status"), sorted(g["closed_by"]) or "—",
        ] for g in groups],
    )

    folded = [g for g in groups if g["evidence"]]
    if folded:
        lines.append("")
        lines.append(
            f"접힌 잎이 있는 원인은 {len(folded)}개이고 잎은 모두 {sum(len(g['evidence']) for g in folded)}개다. "
            "게이트가 한 자리를 통째로 다르다고 보고 아래를 접은 것이므로, 대조기가 보지 못한 자리와 다르다. "
            "보지 못한 자리는 3절에 따로 있다."
        )

    for group in groups:
        lines.append("")
        head = ", ".join(f"`{p}`" for p in group["points"][:8])
        more = f" 외 {len(group['points']) - 8}곳" if len(group["points"]) > 8 else ""
        lines.append(f"### `{group.get('rule')}` @ `{group.get('where')}`")
        lines.append("")
        detail = str(group.get("detail") or "").strip()
        if detail and not detail.endswith((".", "다", "?", "!")):
            detail += "."
        elif detail.endswith("다"):
            detail += "."
        lines.append(f"{detail or '—'} 닿는 판정 지점은 {len(group['points'])}곳이다: {head}{more}.")
        lines.append("")
        lines.append(f"왜 문제인가 — {group.get('why_it_matters') or '—'}")
        lines.append("")
        claimed = "; ".join(group["claimed"]) + "이다." if group["claimed"] else "없다."
        verified = (", ".join(f"`{f}`" for f in group["files"]) + "이다.") if group["files"]             else ((group["note"] + ".") if group["note"] else "없다.")
        lines.append(f"주장한 수정은 {claimed} 확인된 수정은 {verified}")
        if group["evidence"]:
            lines.append("")
            lines.append(
                f"게이트가 한 건으로 접은 잎 {len(group['evidence'])}개다. 접은 것과 보지 못한 것은 다르다 — "
                "여기 적힌 것은 게이트가 보고 접은 것이다."
            )
            lines += bullet_list([str(item) for item in group["evidence"]])
        if group["diff"]:
            lines.append("")
            lines.append("작업 폴더가 앞 iteration과 달라진 자리는 이렇다.")
            lines.append("")
            lines.append("```diff")
            lines.append(group["diff"])
            lines.append("```")

    lines.append("")
    lines.append("판정은 지점 단위로 한다. 어느 지점이 아직 열려 있는지는 지점으로만 말할 수 있다.")
    lines.append("")
    lines += table(
        ["id", "규칙", "판정 지점", "좌표", "라벨", "상태", "닫은 방법", "재발", "처음 본 iteration", "닫힌 iteration"],
        [[
            v.get("id"), v.get("rule"), v.get("point"), v.get("where"), v.get("label"), v.get("status"),
            v.get("closed_by"), v.get("recurrence"),
            v.get("first_seen_iteration"), v.get("closed_in_iteration"),
        ] for v in violations],
    )

    unverified = [v for v in violations if v.get("status") == "closed" and v.get("closed_by_verified") is False]
    if unverified:
        lines.append("")
        lines.append(
            f"닫은 방법을 확인하지 못한 위반이 {len(unverified)}건이다. 계약 판본 하나를 읽지 못해 대조가 성립하지 "
            "않았으므로 `code`로 적힌 것을 코드로 고쳤다는 뜻으로 읽지 않는다."
        )
        lines.append("")
        lines += table(["id", "왜 확인하지 못했는가"],
                       [[v.get("id"), v.get("closed_because")] for v in unverified])

    collateral = [o for o in (ledger.get("observations") or []) if o.get("kind") == "collateral_change"]
    if collateral:
        lines.append("")
        lines.append("열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.")
        lines.append("")
        lines += table(["파일", "iteration", "관찰"],
                       [[o.get("where"), o.get("iteration"), o.get("detail")] for o in collateral])
    return lines


def disagreement_section(ledger: dict) -> list[str]:
    """모델이 한 말과 장치가 낸 판정이 어디서 갈렸는가."""
    bucket = ledger.get("disagreements") or {}
    labels = [
        ("claim_mismatch", "충족 주장과 게이트 판정", "주장은 판정이 아니다. 어긋난 자리만 남긴다."),
        ("silent_reversal", "말없는 되돌림", "서 있는 결정을 바꾸려면 `supersedes`에 이전 id와 이유를 적어야 한다."),
        ("unjustified_proposal", "근거 없는 계약 발의", "Refine에 넘기지 않고 여기에만 남는다."),
        ("axis_disagreement", "축 판정의 어긋남",
         "Critique가 심각하다고 한 축에 Eval이 높은 점수를 줬다. 방향을 함께 적는다 — 누가 더 엄한지가 "
         "읽는 사람에게 필요하다. 게이트까지 셋이 갈린 자리가 자기 평가와 계약 테스트 결과가 어긋난 지점의 "
         "후보다."),
        ("machine_checkable_restatement", "게이트가 세는 것을 다시 말한 지적",
         "막지 않고 센다. 여러 run에 반복되면 프롬프트를 고칠 신호다. 겹친다고 말하려면 같은 좌표를 가리켜야 "
         "하므로 겹친 좌표를 함께 적는다."),
    ]
    total = sum(len(bucket.get(key) or []) for key, _, _ in labels)

    lines = ["모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.", ""]
    if total == 0:
        # 어긋난 자리가 없어도 게이트가 남긴 기록은 낸다. 그 기록은 어긋남이 아니라 관찰이고, 여기서 빼면
        # 「좁혔다」 가족처럼 위반이 아니라 기록으로만 나오는 자리가 리포트 어디에도 남지 않는다.
        lines.append("어긋난 자리가 없다. 다만 판정되지 않은 검사가 있으면 그 자리는 어긋남이 없는 것이 아니라 보지 않은 것이다.")
    else:
        lines.append(f"어긋난 자리는 모두 {total}건이다.")
    for key, title, note in labels:
        items = bucket.get(key) or []
        if not items:
            continue
        lines.append("")
        lines.append(f"### {title} ({len(items)}건)")
        lines.append("")
        lines.append(note)
        lines.append("")
        keys = [k for k in ("iteration", "point", "axis", "decision_id", "critique_id", "id",
                            "pointer", "rule", "shared", "gate_verdict", "eval_score", "direction")
                if any(k in item for item in items)]
        lines += table([*keys, "관찰"],
                       [[item.get(k) for k in keys] + [item.get("detail") or item.get("finding")
                                                       or item.get("issue") or item.get("note")]
                        for item in items])

    if not (bucket.get("machine_checkable_restatement") or []):
        lines.append("")
        lines.append(
            "게이트가 세는 자리를 비평이 다시 말한 곳은 없다. 같은 좌표를 가리키지 않으면 겹침이 아니므로 "
            "축 이름이나 규칙이 하나뿐인 것을 근거로 짝짓지 않는다. 겹치지 않았다고 적는 것이 지어낸 겹침보다 낫다."
        )

    observed = [o for o in (ledger.get("observations") or []) if str(o.get("kind", "")).startswith("gate:")]
    if observed:
        lines.append("")
        lines.append("### 게이트가 남긴 기록 (" + str(len(observed)) + "건)")
        lines.append("")
        lines.append(
            "게이트가 보고 위반으로 세지 않은 차이다. 계약이 말하지 않은 자리가 자란 것과 응답의 값을 계약보다 "
            "좁게 선언한 것이 여기 온다. REJECT 사유가 아니고 뜻은 Critique가 붙인다."
        )
        lines.append("")
        lines += table(["규칙", "가족", "라벨", "판정 지점", "좌표", "iteration", "관찰"],
                       [[o.get("kind"), o.get("family"), o.get("label"), o.get("point"), o.get("where"),
                         o.get("iteration"), o.get("detail")] for o in observed])
        observed_labels = {str(o.get("label")): str(o.get("label_meaning") or "")
                           for o in observed if o.get("label")}
        if observed_labels:
            lines.append("")
            lines.append(
                "기록에 붙은 라벨의 뜻이다. 손해가 없는 차이는 위반이 아니라 여기 온다 — 판정은 그대로 남기고 "
                "리포트에서 뒤로 보낼 뿐이다."
            )
            lines.append("")
            lines += table(["라벨", "뜻"],
                           [[name, observed_labels[name]]
                            for name in sorted(observed_labels, key=label_rank)])
    return lines


# ── 조립 ────────────────────────────────────────────────────────────────────

def build_report(ledger: dict, result: dict) -> str:
    ledger = ledger or {}
    result = result or {}
    sections = [
        summary_section(ledger, result),
        contract_changes_section(ledger),
        impact_section(ledger),
        decisions_section(ledger),
        repairs_section(ledger),
        disagreement_section(ledger),
    ]
    lines = [
        f"# 계약 적합성 run 리포트 — {ledger.get('run_id') or '—'}",
        "",
        "이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. "
        "판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.",
        "",
        f"작성 시각은 {datetime.now(KST).isoformat(timespec='seconds')}이고 기계가 읽는 판은 `run_ledger.json`이다.",
    ]
    for title, body in zip(SECTION_TITLES, sections):
        lines += ["", f"## {title}", ""]
        lines += body
    return "\n".join(lines).rstrip() + "\n"


def write_report(run_dir: Path, ledger: dict, result: dict) -> Path:
    """리포트를 쓴다. iteration마다 다시 계산하는 파생물이므로 덮어쓴다."""
    run_dir = Path(run_dir)
    if not ledger:
        ledger = _read_ledger(run_dir)
    path = run_dir / REPORT_NAME
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(build_report(ledger, result), encoding="utf-8")
    return path


def _read_ledger(run_dir: Path) -> dict:
    path = Path(run_dir) / "run_ledger.json"
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return data if isinstance(data, dict) else {}
