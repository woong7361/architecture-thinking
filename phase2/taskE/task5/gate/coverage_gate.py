"""G4 — 계약이 요구하는 판정 지점을 시나리오가 덮는지 본다.

계약에서 판정 지점을 열거하고, feature 의 태그와 대조해 덮이지 않은 지점을 센다.
열거를 사람이나 모델이 하면 같은 계약을 두고도 개수가 흔들린다(루브릭 채점에서 18·31·35개로 갈렸다).
계약에서 뽑으면 흔들리지 않고, 계약이 바뀌면 이 게이트가 저절로 따라온다.

판정 지점은 둘이다.

    <operationId>:<status>   선언된 응답 하나
    <operationId>:<CODE>     그 오퍼레이션이 낼 수 있다고 선언한 에러 코드 하나

시나리오는 자기가 판정하는 지점을 태그로 밝힌다. 예: `@createAlert:201 @createAlert:WATCHING_START`
아직 덮지 않기로 한 지점은 deferred.yaml 에 이유와 함께 적는다. 이유가 비어 있으면 실패한다.
E-4 계약이 범위 밖 요구사항을 x-out-of-scope 에 이유와 함께 적게 한 것과 같은 방식이다.

    python phase2/taskE/task5/gate/coverage_gate.py
"""

import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT.parent / "api" / "tennis-alert-api.yaml"
FEATURE_DIR = ROOT / "feature"
DEFERRED = Path(__file__).resolve().parent / "deferred.yaml"

TAG = re.compile(r"@([A-Za-z][A-Za-z0-9]*:[A-Z0-9_]+)")
SCENARIO = re.compile(r"^\s*(Scenario|Scenario Outline)\s*:\s*(.+)$")
TAG_LINE = re.compile(r"^\s*@\S")


def resolve(doc, node):
    """$ref 하나를 따라간다. 계약의 응답은 components/responses 를 가리킨다."""
    seen = 0
    while isinstance(node, dict) and "$ref" in node and seen < 10:
        target = doc
        for part in node["$ref"].lstrip("#/").split("/"):
            target = target[part]
        node = target
        seen += 1
    return node


def contract_points(doc):
    """계약이 요구하는 판정 지점을 {지점: 설명}으로 연다."""
    points = {}
    for path, methods in doc["paths"].items():
        for method, operation in methods.items():
            if method.startswith("x-") or not isinstance(operation, dict):
                continue
            name = operation["operationId"]
            for status, response in (operation.get("responses") or {}).items():
                points[f"{name}:{status}"] = f"{method.upper()} {path} → {status}"
                for code in resolve(doc, response).get("x-error-codes") or []:
                    points[f"{name}:{code}"] = f"{method.upper()} {path} → {status} {code}"
    return points


def feature_tags():
    """시나리오마다 달린 지점 태그를 모은다. {지점: [시나리오 이름]}"""
    covered = {}
    scenarios = 0
    for feature in sorted(FEATURE_DIR.glob("*.feature")):
        pending = []
        for line in feature.read_text(encoding="utf-8").splitlines():
            if TAG_LINE.match(line):
                pending.extend(TAG.findall(line))
                continue
            found = SCENARIO.match(line)
            if found:
                scenarios += 1
                title = f"{feature.name}: {found.group(2).strip()}"
                for tag in pending:
                    covered.setdefault(tag, []).append(title)
                pending = []
    return covered, scenarios


def deferred_points():
    if not DEFERRED.exists():
        return {}
    return yaml.safe_load(DEFERRED.read_text(encoding="utf-8")) or {}


def main():
    doc = yaml.safe_load(CONTRACT.read_text(encoding="utf-8"))
    points = contract_points(doc)
    covered, scenarios = feature_tags()
    deferred = deferred_points()

    failures = []
    unknown = sorted(set(covered) - set(points))
    for tag in unknown:
        failures.append(f"[G4] 계약에 없는 지점을 태그로 달았다: {tag}")

    for point, reason in sorted(deferred.items()):
        if point not in points:
            failures.append(f"[G4] 계약에 없는 지점을 미룬다고 적었다: {point}")
        elif not str(reason).strip():
            failures.append(f"[G4] 미루는 이유가 비어 있다: {point}")
        elif point in covered:
            failures.append(f"[G4] 덮으면서 미룬다고도 적었다: {point}")

    missing = sorted(set(points) - set(covered) - set(deferred))
    for point in missing:
        failures.append(f"[G4] 덮지도, 미룬다고 적지도 않은 지점: {point} ({points[point]})")

    total = len(points)
    print(f"계약 판정 지점 {total}개 · 시나리오 {scenarios}개")
    print(f"  덮음   {len(set(covered) & set(points))}")
    print(f"  미룸   {len([p for p in deferred if p in points])}")
    print(f"  누락   {len(missing)}")
    if failures:
        print()
        for failure in failures:
            print(failure)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
