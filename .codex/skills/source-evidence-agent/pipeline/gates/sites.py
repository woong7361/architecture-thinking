"""동시성 API 사용 지점 추출기.

커버리지 게이트의 분모를 만든다. 주장을 뽑는 쪽이 판정받는 쪽이므로, 자신 없는 자리를 주장으로 세우지
않는 것이 모든 축에서 유리하다. 이 목록이 그 길을 막는 유일한 장치다.

판정 자산이라 생성 쪽 payload 에 넣지 않는다.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from fnmatch import fnmatch
from pathlib import Path

import yaml


@dataclass(frozen=True)
class Site:
    site: str          # "<repo 기준 경로>:<줄 번호>"
    rule: str
    api: str
    line: str = field(compare=False, default="")

    def as_dict(self) -> dict:
        return {"site": self.site, "rule": self.rule, "api": self.api, "line": self.line}


def claim_sites(claim: dict) -> list[str]:
    """주장이 덮는 지점 id 목록.

    주장 하나가 지점 하나만 덮게 하면 주장 수가 지점 수를 따라가야 한다. 같은 API 결정이 여러 줄에
    흩어져 있을 때 그것을 한 주장으로 설명하는 것이 정상이므로 배열로 받는다.

    좌표 문자열이 아니라 id 로 받는다. 한 줄에 두 API 가 있으면 좌표가 같아서, 좌표로 세면 한쪽을
    덮은 주장이 다른 쪽까지 덮은 것으로 세어진다. 리허설이 이 자리를 잡았다.
    """
    sites = claim.get("sites")
    if isinstance(sites, str):
        return [sites]
    if isinstance(sites, list):
        return [s for s in sites if s]
    single = claim.get("site")
    return [single] if single else []


def load_rules(path: Path) -> dict:
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def _matches(rel: str, patterns: list[str]) -> bool:
    return any(fnmatch(rel, pattern) for pattern in patterns)


def target_files(code_roots: list[Path], rules: dict, repo_root: Path) -> list[Path]:
    targets = rules["targets"]
    found: list[Path] = []
    for root in code_roots:
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            rel = path.relative_to(repo_root).as_posix()
            if not _matches(rel, targets["include"]):
                continue
            if _matches(rel, targets.get("exclude", [])):
                continue
            found.append(path)
    return found


def extract_sites(code_roots: list[Path], rules: dict, repo_root: Path) -> list[dict]:
    compiled = [(r["id"], re.compile(r["pattern"]), r["unit"]) for r in rules["rules"]]
    sites: dict[str, Site] = {}

    for path in target_files(code_roots, rules, repo_root):
        rel = path.relative_to(repo_root).as_posix()
        text = path.read_text(encoding="utf-8", errors="replace")
        for lineno, raw in enumerate(text.splitlines(), start=1):
            line = raw.strip()
            for rule_id, pattern, unit in compiled:
                match = pattern.search(raw)
                if not match:
                    continue
                api = match.group(1) if match.groups() else match.group(0)
                api = re.sub(r"\s+", " ", api).strip()
                # unit=line 은 줄마다 지점을 센다. 같은 줄에 두 규칙이 걸리면 따로 센다.
                # unit=symbol 은 파일 안에서 같은 API 반복을 하나로 센다. 같은 호출을 세 번 쓴 것은
                # 세 개의 결정이 아니라 하나의 결정이다.
                dedupe = f"{rel}:{lineno}#{rule_id}#{api}" if unit == "line" else f"{rel}#{rule_id}#{api}"
                if dedupe in sites:
                    continue
                sites[dedupe] = Site(site=f"{rel}:{lineno}", rule=rule_id, api=api, line=line)

    ordered = sorted(sites.values(), key=lambda s: (s.site, s.rule, s.api))
    out = []
    for index, site in enumerate(ordered, start=1):
        entry = site.as_dict()
        entry["id"] = f"site-{index:02d}"
        out.append(entry)
    return out


def coverage_gap(claims: list[dict], sites: list[dict]) -> dict:
    """어느 주장도 덮지 않은 지점을 찾는다.

    주장은 `sites` 에 지점 id 를 단다. 그 id 가 지점 목록에 없으면 `claim_site_unknown`,
    지점 중 어느 주장도 달지 않은 것이 있으면 `coverage_gap` 이다.
    """
    site_keys = {s["id"] for s in sites}
    claimed: set[str] = set()
    for claim in claims:
        claimed.update(claim_sites(claim))

    violations = []
    for claim in claims:
        coords = claim_sites(claim)
        if not coords:
            violations.append({
                "rule": "claim_site_unknown", "point": claim.get("id", "?"),
                "where": "", "detail": "주장에 코드 좌표가 없다", "verdict": "violation",
            })
        for site in coords:
            if site not in site_keys:
                violations.append({
                    "rule": "claim_site_unknown", "point": claim.get("id", "?"),
                    "where": site, "detail": "지점 목록에 없는 id 다", "verdict": "violation",
                })

    for site in sites:
        if site["id"] not in claimed:
            violations.append({
                "rule": "coverage_gap", "point": site["id"],
                "where": f"{site['site']} [{site['rule']}] {site['api']}",
                "detail": "어느 주장도 이 지점을 덮지 않는다", "verdict": "violation",
            })

    return {"violations": violations, "sites": len(sites), "claimed_sites": len(claimed & site_keys)}
