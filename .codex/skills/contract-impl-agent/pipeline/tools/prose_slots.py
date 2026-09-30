"""산문 약속의 모집단을 계약에서 뽑는다.

게이트가 소비하는 키는 기계가 결정적으로 대조한다. 남는 텍스트가 산문 약속이고, 그 약속을 사람이나 모델이
매번 열거하면 같은 계약을 두고도 개수가 흔들린다. 루브릭 채점이 18개와 31개와 35개로 갈린 그 문제다.
그래서 이 추출기가 슬롯을 JSON 포인터와 함께 열거해 분모를 고정한다. 흔들리는 것은 분류뿐이다.

슬롯 하나는 이렇게 생겼다.

    {"id": "slot_1a2b3c4d",
     "pointer": "/paths/~1alerts/post/description",
     "text": "...",
     "axis": "state_continuity",
     "axis_source": "pointer"}

모집단은 **산문 키 - 소비 키**다. 산문 키는 아래 `PROSE_KEYS`이고 소비 키는 `rules/consumed_keys.yaml`이
정한다. 게이트가 새 키를 보게 되면 그 키를 소비 목록에 더하고, 그날부터 그 키는 산문에서 빠진다.
`x-requirement`를 게이트가 판정하게 되는 날이 그런 날이다.

축 배분은 모델이 아니라 포인터가 한다. 채점의 분모가 축마다 고정되어야 하기 때문이다.

    parameters · requestBody 아래            request_tolerance
    2xx 응답 아래                            response_fidelity
    4xx · 5xx 응답 아래, 에러 코드 스키마      failure_faithfulness
    오퍼레이션 수준, 상태 관련 스키마 아래      state_continuity

`components` 아래의 슬롯은 그 자리만 보면 어느 축인지 알 수 없다. 컴포넌트는 `$ref`로 여러 자리에서
쓰이므로 경로에서 출발해 `$ref`를 따라가며 각 컴포넌트가 어떤 문맥에 닿는지 모은다. 한 문맥에만 닿으면
그 축이고, 요청과 응답에 함께 닿는 공용 스키마는 포인터로 갈리지 않으므로 `axis: null`,
`axis_source: "model"`로 남겨 모델이 분류하고 신고하게 한다.

    python prose_slots.py <contract.yaml> [--json out.json] [--list]
"""

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
SKILL = HERE.parents[1]
CONSUMED_KEYS = SKILL / "rules" / "consumed_keys.yaml"

# 산문이 실리는 키. 값이 문자열이거나, 문자열의 목록이거나, 텍스트를 가진 항목의 목록이다.
# `title`은 빠진다. 이름표와 예시의 값이지 약속이 아니다.
PROSE_KEYS = ("description", "summary", "x-requirement", "x-out-of-scope", "x-rules")

METHODS = ("get", "put", "post", "delete", "patch", "head", "options", "trace")

AXES = ("request_tolerance", "response_fidelity", "failure_faithfulness", "state_continuity")

# 요청 축과 응답 축으로 갈리는 자리. 오퍼레이션 안에서 이 키를 만나면 그 아래가 그 축이다.
REQUEST_KEYS = ("parameters", "requestBody")

# 상태 관련 스키마를 이름의 성질로 고른다. 설계는 "상태 관련 스키마"라고만 적었고 무엇이 그에 드는지
# 열거하지 않았다. 판정 지점의 상태를 나르는 스키마는 이름이 상태나 진행을 가리키므로 그것으로 고른다.
STATE_SCHEMA_NAME = re.compile(r"(Status|Delivery|Progress|State|Lifecycle)$")

# 에러 코드 스키마도 이름의 성질로 고른다. 실패 축으로 간다.
ERROR_SCHEMA_NAME = re.compile(r"(ErrorCode|ErrorCodes)$")


# ── 포인터 ──────────────────────────────────────────────────────────────────

def escape(token):
    return str(token).replace("~", "~0").replace("/", "~1")


def pointer_of(path):
    return "".join("/" + escape(token) for token in path)


def slot_id(pointer):
    return "slot_" + hashlib.sha1(pointer.encode("utf-8")).hexdigest()[:8]


# ── 소비 키 ─────────────────────────────────────────────────────────────────

def load_consumed_keys(path=CONSUMED_KEYS):
    """`rules/consumed_keys.yaml`에서 키 이름만 모은다. 파일이 없으면 제외하지 않는다."""
    if not Path(path).exists():
        return set()
    doc = yaml.safe_load(Path(path).read_text(encoding="utf-8")) or {}
    return {entry["key"] for entry in doc.get("keys") or [] if entry.get("key")}


# ── 텍스트 ─────────────────────────────────────────────────────────────────

def as_text(value):
    """슬롯의 텍스트를 만든다. 텍스트가 없으면 None을 돌려 슬롯을 만들지 않는다."""
    if isinstance(value, str):
        return value.strip() or None
    if isinstance(value, list):
        parts = []
        for item in value:
            if isinstance(item, str):
                parts.append(item.strip())
            elif isinstance(item, dict):
                # {requirement, reason} 이나 {id, text} 처럼 텍스트에 이름표가 붙은 항목.
                label = item.get("id") or item.get("requirement") or item.get("name")
                body = item.get("text") or item.get("reason") or item.get("rule") or item.get("summary")
                parts.append(f"{label}: {body}" if label and body else str(body or label or item))
        parts = [p for p in parts if p]
        return "\n".join(parts) or None
    return None


# ── 컴포넌트가 어느 문맥에 닿는가 ──────────────────────────────────────────

def status_axis(status):
    code = str(status)
    if code.startswith("2"):
        return "response_fidelity"
    if code.startswith(("4", "5")):
        return "failure_faithfulness"
    return None


def refs_in(node):
    """노드 아래의 모든 지역 `$ref` 대상을 포인터로 돌려준다."""
    found = []
    if isinstance(node, dict):
        ref = node.get("$ref")
        if isinstance(ref, str) and ref.startswith("#"):
            found.append(ref[1:])
        for key, value in node.items():
            if key != "$ref":
                found.extend(refs_in(value))
    elif isinstance(node, list):
        for item in node:
            found.extend(refs_in(item))
    return found


def resolve(doc, pointer):
    node = doc
    for token in pointer.lstrip("/").split("/"):
        token = token.replace("~1", "/").replace("~0", "~")
        if isinstance(node, dict) and token in node:
            node = node[token]
        elif isinstance(node, list) and token.isdigit() and int(token) < len(node):
            node = node[int(token)]
        else:
            return None
    return node


def component_axes(doc):
    """컴포넌트 포인터마다 그것이 닿는 축의 집합을 모은다. `$ref`를 따라 번진다."""
    reached = {}

    def spread(pointer, axis, seen):
        if pointer in seen:
            return
        reached.setdefault(pointer, set()).add(axis)
        node = resolve(doc, pointer)
        if node is None:
            return
        for child in refs_in(node):
            spread(child, axis, seen | {pointer})

    def seed(node, axis):
        for child in refs_in(node):
            spread(child, axis, frozenset())

    for path, item in (doc.get("paths") or {}).items():
        if not isinstance(item, dict):
            continue
        for method, operation in item.items():
            if method not in METHODS or not isinstance(operation, dict):
                continue
            for key in REQUEST_KEYS:
                if key in operation:
                    seed(operation[key], "request_tolerance")
            for status, response in (operation.get("responses") or {}).items():
                axis = status_axis(status)
                if axis:
                    seed({"$ref": response["$ref"]} if "$ref" in response else response, axis)
                    if "$ref" in response:
                        spread(response["$ref"][1:], axis, frozenset())
    return reached


# ── 축 배분 ────────────────────────────────────────────────────────────────

def axis_for(doc, path, reached):
    """슬롯 하나의 축을 포인터로 정한다. 갈리지 않으면 None."""
    tokens = list(path)

    if tokens and tokens[0] == "paths":
        # /paths/<경로>/<메서드>/... 에서 메서드 다음 자리가 무엇인지가 축을 정한다.
        if len(tokens) >= 3 and tokens[2] in METHODS:
            rest = tokens[3:]
            if len(rest) == 1:
                return "state_continuity", "pointer"  # 오퍼레이션 수준의 약속
            if rest and rest[0] in REQUEST_KEYS:
                return "request_tolerance", "pointer"
            if len(rest) >= 2 and rest[0] == "responses":
                return status_axis(rest[1]), "pointer" if status_axis(rest[1]) else "model"
            return None, "model"
        return None, "model"  # 경로 항목 수준. 오퍼레이션이 아니라 경로 전체를 설명한다

    if tokens and tokens[0] == "components" and len(tokens) >= 3:
        section, name = tokens[1], tokens[2]
        if section == "schemas" and ERROR_SCHEMA_NAME.search(name):
            return "failure_faithfulness", "pointer"
        if section == "schemas" and STATE_SCHEMA_NAME.search(name):
            return "state_continuity", "pointer"
        axes = reached.get(pointer_of(tokens[:3]), set())
        if len(axes) == 1:
            return next(iter(axes)), "pointer"
        return None, "model"

    return None, "model"


# ── 열거 ───────────────────────────────────────────────────────────────────

def walk(node, path=()):
    if isinstance(node, dict):
        yield path, node
        for key, value in node.items():
            yield from walk(value, path + (str(key),))
    elif isinstance(node, list):
        for index, value in enumerate(node):
            yield from walk(value, path + (str(index),))


def extract(doc, consumed=None):
    """계약에서 산문 슬롯을 열거한다. 순서는 문서 순서다."""
    consumed = load_consumed_keys() if consumed is None else consumed
    prose_keys = [key for key in PROSE_KEYS if key not in consumed]
    reached = component_axes(doc)

    slots = []
    for path, node in walk(doc):
        for key in prose_keys:
            if key not in node:
                continue
            text = as_text(node[key])
            if text is None:
                continue
            here = path + (key,)
            pointer = pointer_of(here)
            axis, source = axis_for(doc, here, reached)
            slots.append({
                "id": slot_id(pointer),
                "pointer": pointer,
                "text": text,
                "axis": axis,
                "axis_source": source,
            })
    return slots


def counts(slots):
    tally = {axis: 0 for axis in AXES}
    tally[None] = 0
    for slot in slots:
        tally[slot["axis"]] = tally.get(slot["axis"], 0) + 1
    return tally


def key_counts(slots, doc):
    """슬롯을 실은 키별 개수. 설계가 적은 모집단과 대조할 때 쓴다."""
    tally = {}
    for slot in slots:
        key = slot["pointer"].rsplit("/", 1)[-1].replace("~1", "/").replace("~0", "~")
        tally[key] = tally.get(key, 0) + 1
    return tally


def main(argv=None):
    parser = argparse.ArgumentParser(description="계약에서 산문 슬롯을 열거한다.")
    parser.add_argument("contract", type=Path)
    parser.add_argument("--json", type=Path, default=None, help="슬롯 전체를 JSON으로 쓴다")
    parser.add_argument("--list", action="store_true", help="슬롯마다 포인터와 축을 한 줄로 찍는다")
    args = parser.parse_args(argv)

    doc = yaml.safe_load(args.contract.read_text(encoding="utf-8"))
    slots = extract(doc)

    if args.list:
        for slot in slots:
            print(f"{slot['id']}  {slot['axis'] or '-':<22} {slot['axis_source']:<8} {slot['pointer']}")
        print()

    tally = counts(slots)
    print(f"산문 슬롯 {len(slots)}개")
    for axis in AXES:
        print(f"  {axis:<22} {tally[axis]}")
    print(f"  {'(축 미정, 모델 분류)':<20} {tally[None]}")
    print()
    print("키별: " + ", ".join(f"{key} {count}" for key, count in sorted(key_counts(slots, doc).items())))

    if args.json:
        args.json.write_text(json.dumps(slots, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"\n{args.json}에 썼다")
    return 0


if __name__ == "__main__":
    sys.exit(main())
