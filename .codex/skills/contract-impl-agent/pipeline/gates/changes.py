"""계약이 움직였을 때 보는 것.

계약을 고친 것 자체는 위반이 아니다. 고치는 것은 막지 않고 말없이 고치는 것을 막는다. 그래서 위반은 둘뿐이다.

대조의 기준이 둘이다. `contract_version.from`이 가리키는 앞 판본과, 이 run의 기준선이다. 앞 판본만 보면
iteration 001에서 `from`과 `path`가 같은 파일이라 **자기 자신과 비교**하게 되고 어떤 변경도 드러나지 않는다.
실제로 생성기가 기준선 사본을 4만 7천 바이트에서 1만 3천 바이트로 줄였는데 게이트가 통과시켰다. 지워진 것이
산문이라 깨는 변경으로도 잡히지 않았고, 산문은 루브릭의 분모이므로 그것을 지우면 채점 대상이 사라진다.

- `change.unjustified` — 앞 판본과 이번 판본을 대조해 나온 변경 중 `contract_changes`에 기록이 없거나,
  기록의 `basis`나 `spec_anchor`가 빈 것이 있거나, `basis`가 가리키는 id가 그 iteration의 비평에 없는 것이 있다.
  **기준선 대비 차이 중 누적된 기록이 덮지 못하는 것**도 같은 규칙이 잡는다. iteration 001의 판본은 기준선과
  한 바이트도 같아야 하고, 다르면 인용할 비평이 없으므로 여기서 함께 걸린다.
- `change.basis_is_gate_verdict` — 근거로 판정 장치의 규칙 id를 적었다. 판정 결과에 맞춰 판정 기준을 고치는
  것이므로 근거가 아니다.
- `change.version_file` — 내용이 바뀌었는데 판본 파일명이 그대로다. 판본을 파일로 남기기로 했으므로 그 규약이
  깨지면 run 폴더를 보는 사람이 계약이 움직인 것을 놓친다.

`basis`를 문자열 모양으로만 보면 `critique:`을 붙인 아무 말이 근거로 통과한다. 발의와 반영을 가른 뜻이
그 자리에서 사라지므로 이 게이트가 비평을 받아 인용한 id가 실제로 있는지 대조한다. 대조할 비평이 없는
run의 계약 변경은 인용할 곳이 없다는 뜻이라 `basis`가 채워져 있어도 통과시키지 않는다.

대조의 폭은 **이 run에서 이 iteration까지 나온 비평 전부**다. 계약 변경은 iteration을 넘어 살아남으므로
iteration 하나만 보면 앞 판본의 변경을 유지한 신고가 전부 걸리고, 그것은 모델이 만족시킬 수 없는 요구다.
어느 iteration의 비평인지는 근거의 유효성과 무관하다 — 비평이 한 번 그 지적을 했으면 그것은 있었던 일이다.

나머지는 판정이 아니라 계산이므로 기록으로만 돌려준다. 달라진 좌표의 목록, 생기거나 사라진 판정 지점,
깨는 변경 여부, 모델이 적은 호환성 라벨이 대조기 판정과 어긋나는지, 파일명의 판 번호와 `info.version`이
어긋나는지다. 호환성은 대조기가 직접 판정하므로 모델의 라벨을 신뢰할 필요가 없고, 주 버전 규약은 판본이
아직 제안인 루프 안에서 강제하면 헛일이므로 승격 게이트가 본다.

기준선과 원본 대비 누적 diff도 같은 셈이다. 이 함수는 판본 두 개만 받으므로 runner가 `(기준선, 이번 판본)`과
`(원본, 이번 판본)`으로 한 번씩 더 불러 그 셈을 얻는다.

판본 사이의 대조는 `phase2/taskE/task5/gate/breaking_gate.py`의 판정을 그대로 쓴다. 구현 대조와 달리
방향을 갈아 쓸 이유가 없다. 판본 사이에서는 요청이 좁아지는 것과 응답이 사라지는 것이 모두 깨는 변경이다.
"""

import re
from pathlib import Path

import yaml

from .schema_compare import METHODS, compare_http, major
from .g1 import VALUE_FIXING, Findings, effective_security, operations, response_of
from ..tools import prose_slots

CARDS = Path(__file__).resolve().parents[2] / "rules" / "conformance_rules.yaml"

VERSION_IN_NAME = re.compile(r"-v(\d+)(?:\.[A-Za-z0-9]+)?$")

# 계약 변경의 근거는 그 변경을 발의한 비평 항목의 id다. `critique:<id>` 꼴로 적는다.
BASIS = re.compile(r"^critique:(.+)$")

# 요구사항 명세가 요구사항 id를 **선언하는** 자리. 표를 쓰면 행의 첫 칸이고 목록을 쓰면 항목의 머리다.
# 산문 중간을 지나가는 것은 선언이 아니다. 실제 명세의 머리글에 다른 과제를 가리키는 `Task E-3` 링크가 있고,
# 그것을 요구사항으로 세면 계약을 깎는 근거로 쓸 수 있는 id가 된다.
SPEC_ID = re.compile(r"^[ \t]*(?:[-*+][ \t]+|\|[ \t]*)([A-Z][A-Z0-9]*-\d+)[ \t]*(?::|\||$)", re.M)

# 기록이 적은 근거에서 id를 읽는다. **접두사와 구분자에는 관대하고 id 자체에는 엄격하다.** 실물에서
# `requirement:FR-2`와 `FR-7, FR-3`과 `contract:security`가 함께 왔고, 앞의 둘은 통과해야 하고 뒤는 아니다.
ANCHOR_ID = re.compile(r"[A-Z][A-Z0-9]*-\d+")


def basename(path):
    return str(path or "").replace("\\", "/").rsplit("/", 1)[-1]


def stem(path):
    name = basename(path)
    return name.rsplit(".", 1)[0] if "." in name else name


# ── 판본 diff ──────────────────────────────────────────────────────────────

def escape(token):
    return str(token).replace("~", "~0").replace("/", "~1")


def diff_pointers(old, new, path=()):
    """달라진 자리를 JSON 포인터로 연다. 한쪽에만 있는 하위 트리는 그 뿌리에서 멈춘다."""
    pointer = "".join("/" + escape(token) for token in path)
    if isinstance(old, dict) and isinstance(new, dict):
        found = []
        for key in old:
            if key not in new:
                found.append(("removed", pointer + "/" + escape(key)))
        for key in new:
            if key not in old:
                found.append(("added", pointer + "/" + escape(key)))
        for key in old:
            if key in new:
                found.extend(diff_pointers(old[key], new[key], path + (key,)))
        return found
    if isinstance(old, list) and isinstance(new, list):
        found = []
        for index in range(min(len(old), len(new))):
            found.extend(diff_pointers(old[index], new[index], path + (index,)))
        for index in range(len(new), len(old)):
            found.append(("removed", pointer + "/" + str(index)))
        for index in range(len(old), len(new)):
            found.append(("added", pointer + "/" + str(index)))
        return found
    if old != new:
        return [("changed", pointer)]
    return []


# ── 표현 변경과 약속 변경 ───────────────────────────────────────────────────
#
# 구현하다 계약이 잘못 고른 것을 발견하면 고치는 것이 맞다. 그런데 근거의 **출처**만 보면 필드 하나의 표기를
# 바꾸는 것과 약속을 지우는 것이 같은 문턱을 지난다. 그래서 변경의 **성질**을 기계로 가른다.
#
#   표현 변경  아래 여섯이 그대로다. 약속의 재고는 줄지 않았다
#   약속 변경  아래 여섯 중 하나라도 줄어든다
#
# 약속의 재고는 여섯이다. 판정 지점 집합, `required` 집합, 선언한 필드 집합, 인증을 요구하는 오퍼레이션 집합,
# 에러 코드 쌍, 산문 슬롯 집합이다.
#
# 산문 슬롯을 세는 것이 중요하다. 실제로 지워진 것이 정확히 산문이었고 스키마는 한 글자도 안 바뀌어서
# 깨는 변경으로 잡히지 않았다. 산문은 루브릭의 분모이므로 그것을 지우면 채점 대상이 사라진다.
#
# 선언한 필드와 인증 요구는 재고가 닿지 않아 비평 id 하나로 통과하던 자리다. `required`만 세면 계약이 "있을 수 있다"고 한
# 필드가 사라져도 재고가 줄지 않고, 인증 요구가 사라지는 것은 어느 차원에도 들지 않았다. 둘 다 약속이 줄어드는
# 자리이므로 명세 근거를 요구하는 칸에 들어간다. 재고가 닿지 않는 자리는 재고를 고쳐 메운다.
#
# 성질을 가르는 것과 근거를 묻는 것은 다르다. 표현 변경은 약속 변경의 여집합이므로 표기를 바꾼 것만 담지 않는다.
# 문장을 고친 것, 응답을 늘린 것, 어느 응답을 가리키는지 바꾼 것이 다 여기 들어오는데 그것들은 표기를 바꾼 것이
# 아니다. 그래서 닫힌 값의 표현 근거는 성질로 묻지 않고 `SHAPE_KEYS`를 고쳤는지로 묻는다.

REPRESENTATION = "representation"
PROMISE = "promise"

# 표현 변경의 근거. 닫힌 값이다. 뜻은 규칙 카드의 `representation_bases`가 고정한다.
# 도구나 언어의 기본 표현은 근거가 아니다. 그것을 인정하면 언어 타입을 그대로 노출한 것이 정당해진다.
REPRESENTATION_BASES = ("interop", "spec_implication", "contract_consistency")

# 값의 표기를 정하는 스키마 키워드. 표현 근거는 이 자리를 고친 기록에만 묻는다.
#
# 그 필드가 막는 것은 하나다 — "도구나 언어의 기본 표현이 그렇게 시켰다"는 논리. 그 논리는 도구가 값의 모양을
# 내는 자리에서만 성립한다. 자바 `int`가 `format: int32`를 부르고 시각 타입이 `{hour, minute, second, nano}`를
# 부르는 자리다. 문장을 고치는 데도, 상태 코드를 늘리는 데도, 어느 응답을 가리키는지 바꾸는 데도 탓할 도구
# 기본값이 없다. 그 자리에서 요구하면 막는 것 없이 관문만 하나 늘어난다.
#
# 닫힌 값의 어휘가 그 경계를 이미 말한다. 셋 다 표기를 말한다 — 널리 쓰이는 표기로 바꿨거나, 명세가 그 표현을
# 함의하거나, 계약의 다른 자리가 이미 그 표기를 쓴다. 문장을 명세에 맞춘 변경에 고를 값은 없고, 억지로
# `spec_implication`을 고르면 `spec_anchor`가 이미 말한 것을 두 칸에 적게 된다.
#
# 무엇이 있느냐는 표기가 아니라 약속이다. 필드·응답·경로·파라미터가 생기거나 사라지는 것은 약속의 재고와
# `basis`와 `spec_anchor`가 본다. 그래서 `properties`·`responses`·`required`처럼 자리의 있고 없음을 담는 키는
# 여기 넣지 않는다. 재고가 닿지 않는 자리가 있으면 재고를 고쳐 메우고, 표기의 근거로 메우지 않는다.
SHAPE_KEYS = frozenset({
    "type", "format", "items", "prefixItems", "additionalProperties", "propertyNames",
    "nullable", "oneOf", "anyOf", "allOf", "not", "uniqueItems", "discriminator",
}) | frozenset(VALUE_FIXING)

# 그 아래가 키워드가 아니라 사람이 지은 이름인 키. 포인터의 토큰을 어느 쪽으로 읽을지 가른다.
NAMED_CHILDREN = frozenset({
    "properties", "patternProperties", "paths", "responses", "headers", "schemas", "parameters",
    "requestBodies", "securitySchemes", "content", "examples", "links", "callbacks", "variables",
    "encoding", "mapping", "definitions", "$defs",
})


def escape_token(token):
    return str(token).replace("~", "~0").replace("/", "~1")


def pointer_of(path):
    return "".join("/" + escape_token(token) for token in path)


def walk(node, path=()):
    if isinstance(node, dict):
        yield path, node
        for key, value in node.items():
            yield from walk(value, path + (str(key),))
    elif isinstance(node, list):
        for index, value in enumerate(node):
            yield from walk(value, path + (str(index),))


def in_example(path):
    """그 자리가 예시 payload 안인가. 예시 안의 키는 약속이 아니라 약속을 보여주는 그림이다."""
    return any(token in ("example", "examples") for token in path)


def schema_properties(node):
    """스키마의 `properties`. 예시 payload가 우연히 같은 키를 담은 것을 걸러 낸다."""
    found = node.get("properties")
    if isinstance(found, dict) and all(isinstance(value, dict) for value in found.values()):
        return found
    return {}


def promise_inventory(doc):
    """계약이 약속한 것의 재고. {차원: {이름: 포인터}}.

    이름은 무엇이 사라졌는지 사람이 읽을 수 있게, 포인터는 그 자리를 덮는 기록을 찾을 수 있게 둔다.
    """
    doc = doc or {}
    points, required, pairs, auth, declared = {}, {}, {}, {}, {}

    for path, item in (doc.get("paths") or {}).items():
        if not isinstance(item, dict):
            continue
        for method, operation in item.items():
            if method not in METHODS or not isinstance(operation, dict):
                continue
            name = operation.get("operationId") or f"{method}:{path}"
            for status, response in (operation.get("responses") or {}).items():
                where = pointer_of(("paths", path, method, "responses", status))
                points[f"{name}:{status}"] = where
                for code in (response_of(doc, operation, status).get("x-error-codes") or []):
                    points[f"{name}:{code}"] = where
                    pairs[f"{name}:{status}:{code}"] = where

            # 인증을 요구한다는 선언. 계약이 401과 그 에러 코드를 약속한 자리의 근거이고, 사라지면 그 약속이
            # 무엇에 걸려 있었는지가 없어진다. 구현하기 어려운 조항을 지워 위반을 없애는 길 중 값이 가장 크다.
            #
            # 포인터는 **앞 판본에서 그 선언이 있던 자리**다. 오퍼레이션이 스스로 적었으면 그 자리이고,
            # 물려받았으면 문서 수준이다. 덮는 기록을 찾을 때 판본 diff가 내놓는 좌표와 같아야 한다.
            if effective_security(doc, operation):
                auth[name] = (pointer_of(("paths", path, method, "security"))
                              if "security" in operation else "/security")

    for path, node in walk(doc):
        if in_example(path):
            continue
        where = pointer_of(path)
        fields = node.get("required")
        if isinstance(fields, list) and all(isinstance(field, str) for field in fields):
            for field in fields:
                required[f"{where or '/'}:{field}"] = where + "/required"

        # 계약이 선언한 필드. `필수`는 "언제나 담는다"를 세고 이 차원은 "이 필드가 있다"를 센다. 둘은 다른
        # 약속이다. `required`만 세면 계약이 "있을 수 있다"고 한 필드가 사라져도 재고가 줄지 않고, 그것을 읽던
        # 쪽은 영원히 받지 못한다.
        #
        # 선택 필드만 세지 않고 전부 센다. 선택만 세면 선택을 필수로 올린 것이 이 집합에서 빠져 약속 축소로
        # 잡히고, 계약을 굳힌 초안이 벌을 받는다. 그 대가로 필수 필드를 지우면 두 차원에 함께 실리는데,
        # 그때는 실제로 약속이 둘 깨진 것이다 — 그 필드가 없어졌고 언제나 담는다는 보장도 없어졌다.
        for field in schema_properties(node):
            declared[f"{where or '/'}:{field}"] = f"{where}/properties/{escape_token(field)}"

    slots = {}
    try:
        for slot in prose_slots.extract(doc):
            slots[slot["id"]] = slot["pointer"]
    except Exception:  # noqa: BLE001
        # 산문 슬롯을 뽑지 못하면 그 차원을 판정하지 않는다. 못 본 것을 줄지 않았다고 말하지는 않는다.
        slots = None

    return {"판정 지점": points, "필수": required, "선언한 필드": declared,
            "인증 요구": auth, "에러 코드 쌍": pairs, "산문 슬롯": slots}


def reductions(old_doc, new_doc):
    """줄어든 약속. {차원: [(이름, 포인터)]}. 늘어난 것은 약속이 줄어든 것이 아니므로 세지 않는다."""
    before, after = promise_inventory(old_doc), promise_inventory(new_doc)
    found = {}
    for dimension, items in before.items():
        if items is None or after.get(dimension) is None:
            continue
        gone = [(name, items[name]) for name in items if name not in after[dimension]]
        if gone:
            found[dimension] = sorted(gone)
    return found


def change_kind(lost):
    return PROMISE if lost else REPRESENTATION


def reduction_pointers(lost):
    """줄어든 약속이 있던 자리의 포인터. 기록이 그 자리를 덮는지 보려고 쓴다."""
    return {pointer for gone in (lost or {}).values() for _, pointer in gone}


def kind_at(pointer, lost_pointers):
    """그 좌표의 변경이 약속을 줄인 것인가."""
    return PROMISE if any(matches(pointer, target) for target in lost_pointers) else REPRESENTATION


def record_kind(record, lost_pointers):
    """기록 하나의 성질. 그 기록이 덮는 자리에서 약속이 줄었으면 약속 변경이다.

    성질을 판본 전체로 한 번에 정하면, 한 판본 안에서 표기만 바꾼 기록과 약속을 지운 기록이 같은 문턱을
    지난다. 그것이 애초에 가르려던 것이므로 기록마다 정한다.
    """
    target = normalize_target(record.get("target"))
    if not target:
        return REPRESENTATION
    return kind_at(target, lost_pointers)


def last_keyword(pointer):
    """포인터에서 가장 깊은 키워드 자리의 토큰. 그 자리가 무엇을 고쳤는지 말한다.

    OpenAPI 포인터는 키워드 자리와 사람이 지은 이름 자리가 번갈아 온다. 이름을 키워드로 읽으면 `format`이라는
    이름의 필드나 `type`이라는 이름의 스키마가 표기 변경으로 잡힌다. 목록의 번호도 이름 자리다. 그래서
    `/…/properties/expiresAt/description`은 `description`이고 `/…/required/2`는 `required`다.
    """
    found, parent = None, None
    for token in str(pointer or "").split("/"):
        if not token:
            continue
        token = token.replace("~1", "/").replace("~0", "~")
        if parent not in NAMED_CHILDREN and not token.isdigit():
            found = token
        parent = token
    return found


def changes_value_notation(record, diff):
    """그 기록이 덮는 변경 중 값의 표기를 고친 것이 있는가.

    표현 근거를 묻는 문턱이다. 약속을 줄이지 않은 변경을 전부 표현 변경으로 보면 문장을 고친 것과 응답을 늘린
    것까지 표기 근거를 요구받는데, 그 자리에는 탓할 도구 기본값도 고를 수 있는 값도 없다. 그래서 성질을 가르는
    것과 근거를 묻는 것을 나눈다. 성질은 약속이 줄었는지로 갈리고, 표기 근거는 표기를 고쳤을 때만 묻는다.

    더했는지 바꿨는지는 보지 않는다. 앞서는 더한 것뿐인 기록을 면제했는데, 지운 것을 되돌린 초안을 벌하지 않으려는
    뜻이었다. 그 뜻은 고친 **키**를 보는 것으로 이미 이뤄진다. 되돌리는 것은 문장이든 필드든 응답이든 표기 키가
    아니기 때문이다. 더한 것을 면제하면 그 대가로 계약에 없던 `format`을 붙인 기록이 통과하는데, 자바 `int`에
    도구가 `int32`를 붙이는 그 자리가 이 필드를 둔 이유다. 면제가 목적을 덮고 있었다.
    """
    target = normalize_target(record.get("target"))
    return any(matches(pointer, target) and last_keyword(pointer) in SHAPE_KEYS
               for _, pointer in diff)


# ── 근거 ───────────────────────────────────────────────────────────────────

def rule_ids(rules=None):
    """규칙 카드의 id 전부. 근거가 판정 장치의 규칙을 가리키는지 가릴 때 쓴다."""
    doc = rules if isinstance(rules, dict) and rules.get("rules") else None
    if doc is None and CARDS.exists():
        doc = yaml.safe_load(CARDS.read_text(encoding="utf-8")) or {}
    doc = doc or {}
    found = set()
    for section in ("rules", "gate_rules"):
        for card in doc.get(section) or []:
            if isinstance(card, dict) and card.get("id"):
                found.add(str(card["id"]))
    return found


def spec_requirement_ids(spec):
    """요구사항 명세가 선언한 요구사항 id. 읽지 못하면 빈 집합.

    빈 집합과 못 읽은 것을 부르는 쪽이 같게 다룬다. 명세에서 id를 뽑지 못했으면 근거가 명세를 가리키는지
    판정할 수 없고, 판정하지 못한 것을 최저점으로도 세지 않는다. 없는 규약을 만들어 전부 떨어뜨리지 않는다.
    """
    if isinstance(spec, (list, tuple)):
        spec = "\n".join(str(part) for part in spec)
    if not isinstance(spec, str):
        return set()
    return set(SPEC_ID.findall(spec))


def anchored_ids(anchor, spec_ids):
    """그 근거가 가리키는 명세 요구사항 id."""
    return {found for found in ANCHOR_ID.findall(str(anchor or "")) if found in (spec_ids or ())}


def basis_cites_gate_rule(record, known):
    """근거가 판정 장치의 규칙 id를 가리키는가.

    판정 결과에 맞춰 판정 기준을 고치는 것이므로 근거가 아니다. 게이트의 위반은 고칠 대상이지 고칠 이유가 아니다.
    """
    basis = str(record.get("basis") or "").strip()
    found = BASIS.match(basis)
    cited = found.group(1).strip() if found else basis
    return bool(cited) and cited in (known or ())


def backs_a_reduction(record, spec_ids, known):
    """이 기록이 약속 축소를 받칠 수 있는가.

    둘을 함께 본다.

    **근거가 무효인 기록은 아무것도 정당화하지 않는다.** `basis`가 판정 장치의 규칙을 가리켜 이미 유죄인 기록이
    다른 규칙에서 면제를 주면 규칙 사이가 어긋난다. 실제로 인증 요구를 통째로 지운 기록이 `basis`로
    `critique:extract.blind`를 적어 한 규칙에서 걸리면서 약속 축소는 덮었다.

    **명세를 가리키지 않는 문자열은 명세가 받친 것이 아니다.** 약속을 줄이려면 명세가 그 자리를 받쳐야 하고,
    받치지 않으면 구현하기 쉬우려고 약속을 지운 것과 구별할 수 없다. 계약 자신을 가리키는 것은 특히 아니다 —
    줄이려는 그 계약이 줄이는 근거가 될 수 없다. 명세에서 id를 뽑지 못했으면 값을 대조할 수 없으므로 비어 있는지만 본다.
    """
    if basis_cites_gate_rule(record, known):
        return False
    anchor = str(record.get("spec_anchor") or "").strip()
    if not spec_ids:
        return bool(anchor)
    return bool(anchored_ids(anchor, spec_ids))


def critique_ids(critique):
    """비평이 발의한 항목의 id 집합. 대조할 비평이 하나도 없으면 None.

    비평 하나도 받고 여러 개의 목록도 받는다. 목록을 받는 이유가 이 게이트의 비정합이었다. 계약 변경은
    iteration을 넘어 살아남는다 — refine이 앞 판본에서 한 변경을 다음 판본에도 유지하려면 그 신고를 들고
    가야 하고, 그 근거가 된 비평은 지난 iteration의 것이다. 이번 iteration의 비평만 보면 그 기록이 전부
    "인용한 id가 비평에 없다"로 걸리고, **모델이 만족시킬 방법이 없다.** 지난 비평을 되살릴 길이 없기 때문이다.
    계약 diff 쪽은 `baseline`과 `accumulated`로 이미 그 자리를 막았는데 근거 대조만 iteration 하나를 보고 있었다.

    **어느 iteration의 비평인지는 근거의 유효성과 무관하다.** 비평이 한 번 그 지적을 했으면 그 지적은 있었던
    일이고, 발의가 리뷰어에게서 나왔다는 사실은 iteration이 지나도 바뀌지 않는다. 앞을 향한 경계는 부르는 쪽이
    정한다 — 이번 iteration까지 나온 비평만 넘기므로 아직 없던 비평을 근거로 삼는 길은 닫혀 있다.
    """
    found, given = set(), False
    for one in (critique if isinstance(critique, (list, tuple)) else [critique]):
        if not isinstance(one, dict):
            continue
        given = True
        for section in ("weaknesses", "contract_review"):
            for item in one.get(section) or []:
                if isinstance(item, dict) and item.get("id"):
                    found.add(str(item["id"]))
    return found if given else None


def check_representation_basis(f, record, target, kind):
    """표현 변경의 근거가 닫힌 값 안에 있는가.

    표현 변경이라도 아무 이유나 되면 안 된다. 도구나 언어의 기본 표현은 근거가 아니다. 그것을 인정하면
    언어 타입을 그대로 노출한 것이 정당해진다. 실제로 시각을 `"06:00"` 대신 `{hour, minute, second, nano}`로
    내보낸 자리가 그 경우였다.
    """
    if kind != REPRESENTATION:
        return
    given = str(record.get("representation_basis") or "").strip()
    label = record.get("id")
    if not given:
        f.add("change.unjustified", None, target or "-",
              f"기록 {label}은 표현 변경인데 representation_basis가 비었다"
              f" (닫힌 값: {', '.join(REPRESENTATION_BASES)})", kind=kind)
        return
    if given not in REPRESENTATION_BASES:
        f.add("change.unjustified", None, target or "-",
              f"기록 {label}의 representation_basis {given!r}가 닫힌 값 밖이다"
              f" ({', '.join(REPRESENTATION_BASES)}). 도구나 언어의 기본 표현은 근거가 아니다", kind=kind)


def check_basis(f, record, target, critique, known, kind=None):
    """`basis`가 그 iteration의 비평 항목을 실제로 가리키는지 본다.

    빈 `basis`는 부르는 쪽이 이미 잡았으므로 여기서는 채워진 것만 본다. 판정 장치의 규칙 id를 가리키는
    경우를 먼저 가린다. 게이트의 위반은 고칠 대상이지 계약을 고칠 이유가 아니고, 그 성질이 문장에 남아야
    사람이 리포트에서 그 시도를 알아본다.
    """
    basis = str(record.get("basis") or "").strip()
    if not basis:
        return
    label = record.get("id")
    found = BASIS.match(basis)
    cited = (found.group(1).strip() if found else basis)

    if basis_cites_gate_rule(record, known):
        f.add("change.basis_is_gate_verdict", None, target or "-",
              f"기록 {label}의 basis가 판정 장치의 규칙 {cited!r}을 가리킨다", kind=kind)
        return
    if not found:
        f.add("change.unjustified", None, target or "-",
              f"기록 {label}의 basis {basis!r}가 비평 항목을 가리키지 않는다", kind=kind)
        return

    ids = critique_ids(critique)
    if ids is None:
        f.add("change.unjustified", None, target or "-",
              f"기록 {label}이 {cited!r}를 인용했는데 이 run에는 인용할 비평이 없다", kind=kind)
        return
    if cited not in ids:
        f.add("change.unjustified", None, target or "-",
              f"기록 {label}이 인용한 {cited!r}가 이 run의 어느 비평에도 없다", kind=kind)


def normalize_target(target):
    """기록의 `target`을 포인터로 맞춘다. `#/...`과 `/...`을 같게 본다."""
    value = str(target or "").strip()
    if value.startswith("#"):
        value = value[1:]
    if value and not value.startswith("/"):
        value = "/" + value
    return value


def matches(pointer, target):
    """기록이 그 좌표를 덮는가. 굵게 적은 기록과 잘게 적은 기록을 모두 받는다."""
    if not target:
        return False
    return pointer == target or pointer.startswith(target + "/") or target.startswith(pointer + "/")


# ── 판정 지점 ──────────────────────────────────────────────────────────────

def points_of(doc):
    """계약이 요구하는 판정 지점을 연다. 좌표 규약은 `coverage_gate.py`와 같다."""
    points = set()
    for name, (_, _, operation) in operations(doc or {}).items():
        for status in (operation.get("responses") or {}):
            points.add(f"{name}:{status}")
            for code in (response_of(doc, operation, status).get("x-error-codes") or []):
                points.add(f"{name}:{code}")
    return points


# ── 게이트 ─────────────────────────────────────────────────────────────────

def check_contract_changes(prev_contract, curr_contract, changes, prev_path, curr_path,
                           rules=None, critique=None, baseline=None, accumulated=None, spec=None):
    """판본 둘과 그 사이의 신고를 대조한다.

    `spec`은 입력의 요구사항 명세다. `basis`를 비평의 id 집합과 값으로 대조하듯 `spec_anchor`도 명세의
    요구사항 id 집합과 값으로 대조하려고 받는다. 그 대조가 없으면 빈 문자열만 아니면 통과해서, 줄이려는
    계약 자신을 가리키는 근거가 약속 축소를 덮는다. 넘기지 않거나 명세에서 id를 뽑지 못하면 그 대조를 하지 않고
    `change.anchor_uncompared`로 그 사실을 남긴다. 미판정을 통과로도 최저점으로도 세지 않는다.

    `rules`는 규칙 카드다. BUILD.md의 시그니처에는 없지만 없으면 판정값을 카드에서 읽을 수 없으므로
    기본값을 두고 받는다. 넘기지 않으면 아래의 `FALLBACK`이 같은 판정값을 준다.

    `critique`는 **이 iteration까지 이 run에서 나온 비평**이다. 하나를 넘겨도 되고 목록을 넘겨도 된다.
    계약 변경의 `basis`가 가리키는 id가 그중에 실제로 있는지 대조하는 데 쓴다. 이번 iteration의 비평을
    넘기면 옳게 인용한 근거가 없는 id로 뒤집히므로, 부르는 쪽은 이번 iteration을 만든 단계가 받은 것까지 넘긴다.

    **하나만 넘기면 iteration을 넘어 살아남는 기록이 전부 걸린다.** 계약 변경은 판본과 함께 이어진다 —
    앞 판본에서 한 변경을 다음 판본에도 유지하려면 refine이 그 신고를 들고 가야 하고, 그 근거가 된 비평은
    지난 iteration의 것이다. 지난 비평을 되살릴 길이 없으므로 모델이 만족시킬 수 없는 요구가 된다.
    계약 diff 쪽은 `baseline`과 `accumulated`가 이미 그 자리를 막았고, 근거 대조도 같은 폭으로 본다.

    넘기지 않으면 인용할 비평이 없는 run으로 보고, 채워진 `basis`도 통과시키지 않는다. 발의는 리뷰어만
    하므로 인용할 비평이 없으면 계약 변경에 설 근거가 없다. iteration 001과 G0가 깨져 비평을 건너뛴
    iteration이 그 경우다.

    `baseline`은 이 run의 기준선 계약이다. 넘기면 기준선 대비 차이도 신고와 대조한다. 넘기지 않으면 그 대조를
    하지 않으므로 `from`과 `path`가 같은 iteration에는 대조가 하나도 없게 된다. 그 자리가 실제로 뚫렸던 구멍이다.

    `accumulated`는 iteration 001부터 이번까지의 `contract_changes` 전부다. 기준선 대비 차이는 한 iteration의
    기록으로 덮이지 않고 누적된 기록으로 덮이기 때문에 따로 받는다. 넘기지 않으면 `changes`를 쓰고, 그것이
    iteration 001에서는 같은 것이다.
    """
    f = Findings(rules or FALLBACK)
    prev_contract = prev_contract or {}
    curr_contract = curr_contract or {}
    records = list(changes or [])
    history = list(accumulated if accumulated is not None else records)
    known = rule_ids(rules)
    spec_ids = spec_requirement_ids(spec)

    diff = diff_pointers(prev_contract, curr_contract)
    changed = [pointer for _, pointer in diff]

    # 변경의 성질. 기준선까지 견주어 약속이 줄었는지 본다. 기준선이 없으면 앞 판본만 본다.
    lost = reductions(prev_contract, curr_contract)
    for dimension, gone in reductions(baseline or prev_contract, curr_contract).items():
        merged = dict(lost.get(dimension) or [])
        merged.update(dict(gone))
        lost[dimension] = sorted(merged.items())
    kind = change_kind(lost)
    lost_pointers = reduction_pointers(lost)

    # ── 위반 1. 내용이 바뀌었는데 판본 파일명이 그대로다
    if diff and basename(prev_path) == basename(curr_path):
        f.add("change.version_file", None, basename(curr_path) or "-",
              f"내용이 {len(diff)}곳 달라졌는데 판본 파일명이 그대로다", kind=kind)

    # ── 위반 2. 말없이 고쳤거나 근거가 비었다
    covered = set()
    for how, pointer in diff:
        hits = [r for r in records if matches(pointer, normalize_target(r.get("target")))]
        if not hits:
            f.add("change.unjustified", None, pointer,
                  f"{how}: 이 자리의 변경에 기록이 없다", kind=kind_at(pointer, lost_pointers))
            continue
        for record in hits:
            covered.add(id(record))
    for record in records:
        target = normalize_target(record.get("target"))
        # 문턱이 기록마다 갈린다. 어느 변경이든 발의한 비평을 인용해야 하고, 그 위에 약속 변경은 명세 근거를,
        # 값의 표기를 고친 변경은 닫힌 값의 표현 근거를 요구한다. 더한 것뿐인 기록은 비평까지다.
        mine = record_kind(record, lost_pointers)
        needed = ["basis", "spec_anchor"] if mine == PROMISE else ["basis"]
        missing = [key for key in needed if not str(record.get(key) or "").strip()]
        if missing:
            f.add("change.unjustified", None, target or "-",
                  f"기록 {record.get('id')}의 {missing}가 비었다", kind=mine)
        # 약속을 줄이는 기록은 명세가 그 자리를 받쳐야 한다. 채워져 있기만 한 근거는 받친 것이 아니다.
        anchor = str(record.get("spec_anchor") or "").strip()
        if mine == PROMISE and anchor and spec_ids and not anchored_ids(anchor, spec_ids):
            f.add("change.unjustified", None, target or "-",
                  f"기록 {record.get('id')}의 spec_anchor {anchor!r}가 요구사항 명세의 id를 가리키지 않는다."
                  f" 명세를 가리키지 않는 문자열은 명세가 받친 것이 아니다", kind=mine)
        check_basis(f, record, target, critique, known, mine)
        if mine == REPRESENTATION and changes_value_notation(record, diff):
            check_representation_basis(f, record, target, mine)
        if id(record) not in covered:
            f.add("change.unmatched_record", None, target or "-",
                  f"기록 {record.get('id')}이 가리키는 자리가 판본 diff에 없다", kind=mine)

    # ── 위반 3. 기준선에서 멀어졌는데 덮는 기록이 없다
    #
    # 앞 판본 대비가 아니라 기준선 대비다. `from`과 `path`가 같은 iteration에서는 이것이 유일한 대조다.
    # 좌표마다 한 건씩 내지 않고 한 건으로 모은다. 기준선 사본을 덮어쓴 것은 좌표 백 개의 일이 아니라
    # 한 가지 일이고, 그 문장 하나가 refine에게 무엇을 되돌려야 하는지 말한다. 좌표는 증거에 남는다.
    if baseline:
        drift = diff_pointers(baseline, curr_contract)
        uncovered = [pointer for _, pointer in drift
                     if not any(matches(pointer, normalize_target(r.get("target"))) for r in history)]
        if uncovered:
            f.add("change.unjustified", None, "기준선 대비",
                  f"기준선과 달라진 좌표 {len(uncovered)}개에 덮는 기록이 없다"
                  + (f" (누적 기록 {len(history)}건이 덮은 것은 {len(drift) - len(uncovered)}개)"
                     if history else " (누적 기록이 없다)"),
                  evidence=uncovered, kind=kind)
        if drift:
            f.add("change.diff", None, "기준선 대비",
                  f"기준선과 달라진 좌표 {len(drift)}개: "
                  + ", ".join(pointer for _, pointer in drift[:40]), kind=kind)

    # ── 위반 4. 약속이 줄었는데 명세 근거가 없다
    #
    # 차원마다 한 건이다. 산문 슬롯 백 개가 사라진 것은 백 가지 일이 아니라 한 가지 일이고, 사라진 것은
    # 증거에 남는다. 명세 근거가 있는 기록이 그 자리를 덮으면 잡지 않는다 — 고치는 것은 막지 않는다.
    if lost and not spec_ids:
        f.add("change.anchor_uncompared", None, "spec_anchor",
              "요구사항 명세에서 요구사항 id를 뽑지 못해 spec_anchor의 값을 대조하지 않았다."
              " 이 판정은 근거가 비어 있는지까지만 보았다", kind=PROMISE)
    for dimension, gone in sorted(lost.items()):
        unbacked = [name for name, pointer in gone
                    if not any(matches(pointer, normalize_target(r.get("target")))
                               and backs_a_reduction(r, spec_ids, known)
                               for r in history or records)]
        if not unbacked:
            continue
        f.add("change.promise_reduced", None, dimension,
              f"{dimension}이 {len(unbacked)}개 줄었는데 명세 근거를 댄 기록이 없다"
              + (f" (줄어든 것은 모두 {len(gone)}개)" if len(gone) != len(unbacked) else ""),
              evidence=unbacked, kind=PROMISE)

    # ── 기록. 판정이 아니라 셈이다
    if diff:
        f.add("change.diff", None, "*", f"달라진 좌표 {len(diff)}개: " + ", ".join(changed[:40]),
              kind=kind)

    breaking = []
    compare_http(prev_contract, curr_contract, breaking)
    for line in breaking:
        f.add("change.breaking", None, line.split(":", 1)[0], line, kind=kind)

    labelled = {str(r.get("compatibility") or "").strip() for r in records}
    if breaking and "breaking" not in labelled:
        f.add("change.compatibility_mismatch", None, "*",
              f"대조기는 깨는 변경 {len(breaking)}건을 찾았는데 기록에 breaking 라벨이 없다")
    if not breaking and "breaking" in labelled:
        f.add("change.compatibility_mismatch", None, "*",
              "기록은 breaking이라 적었는데 대조기는 깨는 변경을 찾지 못했다")

    gone = sorted(points_of(prev_contract) - points_of(curr_contract))
    born = sorted(points_of(curr_contract) - points_of(prev_contract))
    for point in gone:
        f.add("change.point_churn", point, "-", "판정 지점이 사라졌다", kind=kind)
    for point in born:
        f.add("change.point_churn", point, "-", "판정 지점이 생겼다", kind=kind)

    found = VERSION_IN_NAME.search(stem(curr_path) or "")
    if found and found.group(1) != major(curr_contract):
        f.add("change.version_label", None, basename(curr_path),
              f"파일명의 판 번호 {found.group(1)}과 계약의 주 버전 {major(curr_contract)}이 어긋난다")

    return f.result()


# 규칙 카드를 넘기지 않았을 때 쓰는 최소 판정값. 뜻은 `rules/conformance_rules.yaml`이 정본이다.
FALLBACK = {"rules": [
    {"id": "change.unjustified", "verdict": "violation"},
    {"id": "change.promise_reduced", "verdict": "violation"},
    {"id": "change.basis_is_gate_verdict", "verdict": "violation"},
    {"id": "change.version_file", "verdict": "violation"},
    {"id": "change.unmatched_record", "verdict": "observation"},
    {"id": "change.anchor_uncompared", "verdict": "observation"},
    {"id": "change.diff", "verdict": "observation"},
    {"id": "change.breaking", "verdict": "observation"},
    {"id": "change.compatibility_mismatch", "verdict": "observation"},
    {"id": "change.point_churn", "verdict": "observation"},
    {"id": "change.version_label", "verdict": "observation"},
]}
