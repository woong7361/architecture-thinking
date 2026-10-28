"""지점 추출기를 혼자 돌려 본다. BUILD.md 의 조각 C 확인 방법이다.

사용법:
    python list_sites.py                 # 지점 목록을 사람이 읽는 표로
    python list_sites.py --json <경로>   # 게이트가 읽는 형식으로 저장
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from gates.sites import extract_sites, load_rules  # noqa: E402

for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")

SKILL_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = SKILL_ROOT.parents[2]
RULES = SKILL_ROOT / "rules" / "concurrency_sites.yaml"

# 대상 코드. F-2 의 서비스와 F-3 의 락 두 벌, 그리고 단계 1 이 낸 구현이다.
CODE_ROOTS = [
    REPO_ROOT / "phase2/taskF/alert-kata/src/main/java",
    REPO_ROOT / "phase2/taskF/alert-kata/src/test/resources",
    REPO_ROOT / "phase2/taskF/task4/step1/run3/impl",
]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json")
    args = parser.parse_args()

    rules = load_rules(RULES)
    missing = [r for r in CODE_ROOTS if not r.exists()]
    if missing:
        for root in missing:
            print(f"대상 코드 경로가 없다: {root}", file=sys.stderr)
        return 2

    sites = extract_sites(CODE_ROOTS, rules, REPO_ROOT)

    if args.json:
        Path(args.json).write_text(
            json.dumps({"sites": sites}, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"{len(sites)}개 지점을 {args.json} 에 썼다")
        return 0

    by_rule = Counter(s["rule"] for s in sites)
    print(f"지점 {len(sites)}개, 규칙 {len(by_rule)}개\n")
    for rule, count in sorted(by_rule.items(), key=lambda kv: (-kv[1], kv[0])):
        print(f"{count:4d}  {rule}")
    print()
    for site in sites:
        print(f"{site['site']}  [{site['rule']}] {site['api']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
