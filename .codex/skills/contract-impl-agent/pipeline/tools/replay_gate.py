"""저장된 run에 게이트를 다시 돌려 예전 판정과 새 판정을 나란히 낸다.

판정 장치를 고칠 때마다 물어야 하는 것이 둘이다. 세는 단위가 원인에 가까워졌는가, 그리고 접히면서 사실을
잃지 않았는가. 유료 호출을 다시 하지 않고 답하려면 지난 run의 산출로 게이트만 다시 돌리면 된다. 추출 스펙과
계약 판본과 그때의 위반 목록이 run 폴더에 남아 있다.

구현 대조와 판본 대조를 함께 돌린다. 읽는 것은 이렇다.

    iter_00N/api-docs.json      그때 부팅해서 받은 추출 스펙
    output/contract/<판본>       `draft.json`의 `contract_version.path`가 가리키는 계약 판본
    output/contract/<앞 판본>    `contract_version.from`이 가리키는 판본
    baseline/*.yaml             이 run의 기준선. 판본 대조가 이것과도 견준다
    iter_00(N-1)/critique.json  Refine이 받은 비평. 계약 변경의 `basis`를 대조하는 데 쓴다
    iter_00N/gate.json          그때의 위반과 기록 목록

계약 판본이 작업 폴더에 남아 있지 않으면 그 iteration의 커밋에서 꺼낸다. 작업 폴더는 제자리에서 고치므로
마지막 판본만 남고, 중간 iteration의 판본은 git에만 있다.

    python replay_gate.py <run 폴더 또는 그 상위> [--contract <기준 계약>] [--json out.json] [--verbose]

`--contract`는 계약 판본을 찾지 못했을 때 쓸 대체물이다. 넘기지 않으면 그 iteration을 건너뛰고 이유를 적는다.
찾은 판본으로 돈 것과 대체물로 돈 것을 출력이 구별한다.
"""

import argparse
import json
import subprocess
import sys
from collections import Counter
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
SKILL = HERE.parents[1]
if str(SKILL) not in sys.path:
    sys.path.insert(0, str(SKILL))

from pipeline.gates import check_contract_changes, load_rules, run_g1  # noqa: E402
from pipeline.gates.changes import record_kind, reduction_pointers, reductions  # noqa: E402

SCOPE = ["getCourtAvailability", "createAlert", "listAlerts", "getAlert", "cancelAlert"]

# 게이트가 아니라 원장과 파이프라인이 내는 규칙. 대조에서 뺀다. 이 도구가 다시 돌리는 것은 G1뿐이다.
NOT_G1 = ("g0.", "change.", "manifest.", "contract.", "schema.")


def find_runs(root):
    """run 폴더를 찾는다. 넘긴 것이 run 폴더면 그것 하나, 상위면 그 아래를 모은다."""
    root = Path(root)
    if (root / "output").exists() or list(root.glob("iter_*")):
        return [root]
    found = []
    for candidate in sorted(root.rglob("iter_001")):
        run = candidate.parent
        if run not in found:
            found.append(run)
    return found


def contract_from_git(run, commit, relative):
    """그 iteration의 커밋에서 계약 판본을 꺼낸다. 작업 폴더는 제자리에서 고치므로 중간 판본은 git에만 있다."""
    output = run / "output"
    if not commit or not (output / ".git").exists():
        return None
    done = subprocess.run(["git", "-C", str(output), "show", f"{commit}:{relative}"],
                          capture_output=True)
    if done.returncode != 0:
        return None
    try:
        return yaml.safe_load(done.stdout.decode("utf-8", errors="replace"))
    except yaml.YAMLError:
        return None


def load_contract(run, draft):
    """그 iteration의 계약 판본과 어디서 얻었는지. 못 얻으면 (None, 이유)."""
    version = (draft or {}).get("contract_version") or {}
    relative = version.get("path")
    if not relative:
        return None, "draft.json에 contract_version.path가 없다"
    on_disk = run / "output" / relative
    if on_disk.exists():
        return yaml.safe_load(on_disk.read_text(encoding="utf-8")), f"작업 폴더 {relative}"
    from_git = contract_from_git(run, (draft or {}).get("commit"), relative)
    if from_git is not None:
        return from_git, f"커밋 {(draft or {}).get('commit', '')[:8]}:{relative}"
    return None, f"{relative}를 작업 폴더에도 커밋에도 없다"


def g1_only(items):
    return [item for item in items or [] if not str(item.get("rule", "")).startswith(NOT_G1)]


def changes_only(items):
    return [item for item in items or [] if str(item.get("rule", "")).startswith("change.")]


def load_spec(run):
    """이 run의 요구사항 명세. `spec_anchor`가 명세의 요구사항 id를 가리키는지 대조하는 데 쓴다.

    입력 파일 이름에 brief 해시가 붙으므로 glob으로 찾는다. 못 찾으면 그 대조를 하지 않고 게이트가
    `change.anchor_uncompared`로 그 사실을 남긴다.
    """
    for path in sorted(run.glob("*_input.json")):
        try:
            brief = (json.loads(path.read_text(encoding="utf-8")) or {}).get("brief") or {}
        except (OSError, ValueError):
            continue
        if brief.get("requirement_spec"):
            return brief["requirement_spec"]
    return None


def load_baseline(run):
    """이 run의 기준선. 판본 대조가 앞 판본과 기준선을 둘 다 본다."""
    found = sorted((run / "baseline").glob("*.yaml")) if (run / "baseline").exists() else []
    for path in found:
        if "asyncapi" not in path.name:
            return yaml.safe_load(path.read_text(encoding="utf-8")), path.name
    return None, None


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else None


def accumulated_changes(run, iteration):
    """iteration 001부터 이번까지의 계약 변경 기록 전부. 기준선 대비 차이는 누적 기록이 덮는다."""
    found = []
    for folder in sorted(run.glob("iter_*")):
        draft = read_json(folder / "draft.json") or {}
        found.extend(draft.get("contract_changes") or [])
        if folder.name == iteration.name:
            break
    return found


def previous_critique(run, iteration):
    """이 iteration까지 이 run에서 나온 비평 전부. 이번 iteration의 것은 넣지 않는다.

    계약 변경은 iteration을 넘어 살아남는다. 앞 판본에서 한 변경을 다음 판본에도 유지하려면 refine이 그
    신고를 들고 가야 하고, 그 근거가 된 비평은 지난 iteration의 것이다. 앞 iteration 하나만 넘기면 그 기록이
    전부 "인용한 id가 비평에 없다"로 걸리는데 지난 비평을 되살릴 길이 없다.

    이번 iteration의 비평은 넣지 않는다. 그것은 이 판본을 만든 뒤에 나온 것이므로 근거가 될 수 없다.
    iteration 001은 앞선 비평이 없어 빈 목록이고, 그때의 계약 변경은 인용할 곳이 없다.
    """
    folders = sorted(run.glob("iter_*"))
    names = [folder.name for folder in folders]
    if iteration.name not in names:
        return None
    found = [read_json(folder / "critique.json") for folder in folders[:names.index(iteration.name)]]
    return [one for one in found if one] or None


def replay_changes(run, iteration, rules, contract, draft):
    """판본 대조를 되돌린다. 기준선을 넘기는 것과 넘기지 않는 것을 함께 낸다."""
    version = (draft or {}).get("contract_version") or {}
    curr_path, prev_path = version.get("path"), version.get("from")
    if not curr_path:
        return None
    prev, _ = (contract, None) if prev_path == curr_path else load_contract(
        run, {"contract_version": {"path": prev_path}, "commit": (draft or {}).get("commit")})
    if prev is None:
        prev = contract
    baseline, baseline_name = load_baseline(run)
    records = (draft or {}).get("contract_changes") or []
    history = accumulated_changes(run, iteration)
    critique = previous_critique(run, iteration)

    spec = load_spec(run)
    without = check_contract_changes(prev, contract, records, prev_path, curr_path, rules, critique,
                                    spec=spec)
    with_baseline = check_contract_changes(prev, contract, records, prev_path, curr_path, rules,
                                          critique, baseline=baseline, accumulated=history,
                                          spec=spec)

    # 변경의 성질. 기록마다 정한다. 판본 전체로 한 번에 정하면 표기만 바꾼 기록과 약속을 지운 기록이 섞인다.
    lost = reductions(prev, contract)
    if baseline:
        for dimension, gone in reductions(baseline, contract).items():
            lost.setdefault(dimension, []).extend(gone)
    pointers = reduction_pointers(lost)
    return {
        "reduced": {dimension: len(gone) for dimension, gone in sorted(lost.items())},
        "record_kinds": {record.get("id") or "?": record_kind(record, pointers) for record in records},
        "self_compared": prev_path == curr_path,
        "baseline": baseline_name,
        "records": len(records),
        "accumulated": len(history),
        "critique": bool(critique),
        "without_baseline": len(without["violations"]),
        "with_baseline": len(with_baseline["violations"]),
        "items": with_baseline["violations"],
    }


def tally(items):
    return dict(sorted(Counter(item["rule"] for item in items).items()))


def points(items):
    return {item.get("point") for item in items if item.get("point")}


def leaves(items):
    """접힌 잎까지 센다. 접히면서 사실을 잃지 않았는지 보는 자리다."""
    return sum(max(1, len(item.get("evidence") or [])) for item in items)


def facts(items):
    """서로 다른 사실의 수. 판정 지점을 빼고 센다.

    계약이 같은 컴포넌트를 여러 응답에서 `$ref`하면 그 컴포넌트 하나의 결함이 지점마다 한 번씩 나온다.
    판정 지점은 판정의 좌표이므로 접지 않지만, 원인이 몇 개인지는 이 수가 말한다.
    """
    return len({(item["rule"], item["where"], item["detail"]) for item in items})


def replay_iteration(run, iteration, rules, fallback):
    spec_path = iteration / "api-docs.json"
    gate_path = iteration / "gate.json"
    draft_path = iteration / "draft.json"
    label = f"{run.parent.name}/{run.name} {iteration.name}"

    old = json.loads(gate_path.read_text(encoding="utf-8")) if gate_path.exists() else {}
    old_violations = g1_only(old.get("violations"))
    old_observations = g1_only(old.get("observations"))

    draft = json.loads(draft_path.read_text(encoding="utf-8")) if draft_path.exists() else {}
    contract, source = load_contract(run, draft)
    if contract is None and fallback is not None:
        contract, source = fallback, "대체 계약"

    if not spec_path.exists() or contract is None:
        # 구현 대조는 추출 스펙이 있어야 돌지만 판본 대조는 계약만 있으면 돈다.
        reason = ("api-docs.json이 없다 (G0가 깨진 iteration)" if not spec_path.exists() else source)
        return {"label": label, "skipped": reason,
                "old_violations": len(old_violations),
                "old_change_violations": len(changes_only(old.get("violations"))),
                "changes": replay_changes(run, iteration, rules, contract, draft)
                if contract is not None else None}

    spec = json.loads(spec_path.read_text(encoding="utf-8"))
    outcome = run_g1(spec, contract, SCOPE, draft.get("decisions") or [], rules)
    new_violations = outcome["violations"]
    new_observations = outcome["observations"]

    return {
        "label": label,
        "changes": replay_changes(run, iteration, rules, contract, draft),
        "old_change_violations": len(changes_only(old.get("violations"))),
        "contract_source": source,
        "old_violations": len(old_violations),
        "old_rules": tally(old_violations),
        "old_points": len(points(old_violations)),
        "old_observations": len(old_observations),
        "new_violations": len(new_violations),
        "new_rules": tally(new_violations),
        "new_points": len(points(new_violations)),
        "new_observations": len(new_observations),
        "new_leaves": leaves(new_violations),
        "old_facts": facts(old_violations),
        "new_facts": facts(new_violations),
        "gone_rules": sorted(set(tally(old_violations)) - set(tally(new_violations))),
        "items": new_violations,
    }


def print_report(rows, verbose=False):
    for row in rows:
        print(f"\n{row['label']}")
        if row.get("skipped"):
            print(f"   구현 대조 건너뜀: {row['skipped']}  (그때 위반 {row.get('old_violations', 0)}건)")
            print_changes(row)
            continue
        print(f"   계약 판본: {row['contract_source']}")
        print(f"   예전 {row['old_violations']}건 (규칙 {len(row['old_rules'])}종, 지점 {row['old_points']}개)"
              f"  →  새 {row['new_violations']}건 (규칙 {len(row['new_rules'])}종, 지점 {row['new_points']}개)"
              f"  · 기록 {row['old_observations']} → {row['new_observations']}")
        if row["old_violations"]:
            ratio = row["new_violations"] / row["old_violations"]
            print(f"   접힘: {ratio:.2f}배 · 증거로 남은 잎 {row['new_leaves']}개")
        print(f"   서로 다른 사실: 예전 {row['old_facts']}개 → 새 {row['new_facts']}개"
              f" (지점을 빼고 센 것. 남은 부풀림은 계약이 같은 컴포넌트를 여러 응답에서 가리켜서다)")
        print(f"   예전 규칙: {row['old_rules']}")
        print(f"   새 규칙:   {row['new_rules']}")
        if row["gone_rules"]:
            print(f"   사라진 규칙: {row['gone_rules']}")
        print("   새 판정에서 남은 것:")
        for item in row["items"][:40]:
            head = f"      {item['rule']:<32} {str(item.get('point')):<28} {item['where']}"
            print(head)
            if verbose:
                print(f"         {item['detail']}")
            for leaf in (item.get("evidence") or [])[:6 if not verbose else 100]:
                print(f"         · {leaf}")
            rest = len(item.get("evidence") or []) - (6 if not verbose else 100)
            if rest > 0:
                print(f"         · … 증거 {rest}개 더")
        if len(row["items"]) > 40:
            print(f"      … {len(row['items']) - 40}건 더")
        print_changes(row)


def print_changes(row):
    """판본 대조. 기준선을 넘기지 않으면 무엇이 안 보이는지 나란히 낸다."""
    changes = row.get("changes")
    if not changes:
        return
    print(f"   판본 대조 · 기준선 {changes['baseline']}"
          + ("  (앞 판본과 같은 파일이라 자기 자신과 비교한다)" if changes["self_compared"] else "")
          + f" · 이 iteration 기록 {changes['records']}건, 누적 {changes['accumulated']}건"
          + f", 비평 {'있다' if changes['critique'] else '없다'}")
    print(f"      그때 위반 {row.get('old_change_violations', 0)}건"
          f"  ·  기준선 없이 {changes['without_baseline']}건"
          f"  →  기준선까지 보면 {changes['with_baseline']}건")
    if changes.get("reduced"):
        print(f"      줄어든 약속의 재고: {changes['reduced']}")
    if changes.get("record_kinds"):
        tally = Counter(changes["record_kinds"].values())
        print(f"      기록의 성질: {dict(tally)}")
        for name, kind in changes["record_kinds"].items():
            print(f"         {kind:<14} {name}")
    for item in changes["items"][:8]:
        print(f"      {item['rule']:<28} [{item.get('kind') or '-'}] {item['where']}")
        print(f"         {item['detail']}")
        for leaf in (item.get("evidence") or [])[:5]:
            print(f"         · {leaf}")
        rest = len(item.get("evidence") or []) - 5
        if rest > 0:
            print(f"         · … 증거 {rest}개 더")
    if len(changes["items"]) > 8:
        print(f"      … {len(changes['items']) - 8}건 더")


def main(argv=None):
    parser = argparse.ArgumentParser(description="저장된 run에 게이트를 다시 돌린다.")
    parser.add_argument("root", type=Path)
    parser.add_argument("--contract", type=Path, default=None, help="계약 판본을 못 찾을 때 쓸 대체물")
    parser.add_argument("--json", type=Path, default=None)
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args(argv)

    rules = load_rules()
    fallback = yaml.safe_load(args.contract.read_text(encoding="utf-8")) if args.contract else None

    rows = []
    runs = find_runs(args.root)
    if not runs:
        print(f"run 폴더를 찾지 못했다: {args.root}")
        return 2
    for run in runs:
        for iteration in sorted(run.glob("iter_*")):
            rows.append(replay_iteration(run, iteration, rules, fallback))

    print_report(rows, args.verbose)

    replayed = [row for row in rows if not row.get("skipped")]
    old_total = sum(row["old_violations"] for row in replayed)
    new_total = sum(row["new_violations"] for row in replayed)
    leaf_total = sum(row["new_leaves"] for row in replayed)
    old_facts = sum(row["old_facts"] for row in replayed)
    new_facts = sum(row["new_facts"] for row in replayed)
    print(f"\n합계 · iteration {len(replayed)}개 되돌림, {len(rows) - len(replayed)}개 건너뜀")
    print(f"   위반 {old_total}건 → {new_total}건"
          + (f" ({new_total / old_total:.2f}배)" if old_total else ""))
    print(f"   서로 다른 사실 {old_facts}개 → {new_facts}개")
    print(f"   새 판정의 증거로 남은 잎 {leaf_total}개")

    if args.json:
        args.json.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"   {args.json}에 썼다")
    return 0


if __name__ == "__main__":
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main())
