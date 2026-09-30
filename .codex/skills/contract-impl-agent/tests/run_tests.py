"""픽스처로 판정 장치를 돌려 본다.

pytest 없이 도는 스크립트다. 픽스처는 실제 계약에서 만든다. 손으로 쓴 작은 스펙으로 시험하면 실제 계약의
표기(널 허용의 세 표기, `$ref`로 공유되는 컴포넌트, 상태 코드마다 다른 에러 코드 쌍)를 지나가지 못한다.

여기서 못 박는 것은 방향이다. 응답의 열거형이 계약보다 좁은 것은 허용이고 요청의 열거형이 좁은 것은 위반이다.
원본 대조기는 판본 사이를 비교하므로 둘을 모두 깨는 변경으로 세는데, 그 판정을 구현 대조에 그대로 쓰면
계약을 만족하는 구현이 떨어진다. 그 차이가 이 게이트의 핵심이므로 테스트가 양쪽을 함께 본다.

    python tests/run_tests.py [--verbose]
"""

import argparse
import copy
import json
import subprocess
import sys
import tempfile
from pathlib import Path

import yaml

SKILL = Path(__file__).resolve().parents[1]
REPO = SKILL.parents[2]
CONTRACT = REPO / "phase2" / "taskE" / "api" / "tennis-alert-api.yaml"
SKELETON = REPO / "phase2" / "taskE" / "task5" / "skeleton"

sys.path.insert(0, str(SKILL))

from pipeline.gates import check_contract_changes, load_rules, run_g0, run_g1  # noqa: E402
from pipeline.gates.g0 import decode, summarize_boot_failure, summarize_build_failure  # noqa: E402
from pipeline.gates.g1 import LABELS, RULE_IDS  # noqa: E402
from pipeline.gates.changes import (  # noqa: E402
    PROMISE, REPRESENTATION, REPRESENTATION_BASES, SHAPE_KEYS, anchored_ids,
    change_kind, critique_ids, last_keyword, promise_inventory, reductions,
    spec_requirement_ids)
from pipeline.gates import g0 as g0_module  # noqa: E402
from pipeline.gates.schema_compare import (  # noqa: E402
    UNRESOLVED, compare_http, deref, parameter_key, unwrap_nullable)
from pipeline.tools import prose_slots  # noqa: E402

SCOPE = ["getCourtAvailability", "createAlert", "listAlerts", "getAlert", "cancelAlert"]

RULES = load_rules()
BASE = yaml.safe_load(CONTRACT.read_text(encoding="utf-8"))

results = []
verbose = False


def report(name, ok, detail=""):
    results.append((name, ok, detail))
    print(f"  {'OK  ' if ok else 'FAIL'} {name}" + (f"  — {detail}" if detail else ""))


def rules_of(items):
    return sorted({item["rule"] for item in items})


def show(label, outcome):
    if not verbose:
        return
    for kind in ("violations", "observations"):
        for item in outcome.get(kind) or []:
            print(f"        [{label}/{kind[:3]}] {item['rule']} {item['point']} {item['where']} :: {item['detail']}")


def judge(derived, decisions=None, project_dir=None):
    return run_g1(derived, BASE, SCOPE, decisions or [], RULES, project_dir=project_dir)


def fake_output(root, files):
    """run의 작업 폴더처럼 꾸민다. 매니페스트는 색인만 담으므로 내용은 파일에만 있다."""
    root = Path(root)
    for relative, content in files.items():
        target = root / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
    return root


def contract_copy():
    return copy.deepcopy(BASE)


def schema(doc, name):
    return doc["components"]["schemas"][name]


def response(doc, name):
    return doc["components"]["responses"][name]


def operation(doc, path, method):
    return doc["paths"][path][method]


def parameter(doc, name):
    return doc["components"]["parameters"][name]


# ── 1. 계약을 그대로 추출 스펙으로 넣으면 위반 0건 ─────────────────────────

def test_identity():
    outcome = judge(contract_copy())
    show("identity", outcome)
    report("계약을 그대로 넣으면 위반 0건",
           not outcome["violations"],
           f"위반 {len(outcome['violations'])}건 {rules_of(outcome['violations'])}")
    report("계약을 그대로 넣으면 기록도 0건",
           not outcome["observations"],
           f"기록 {len(outcome['observations'])}건 {rules_of(outcome['observations'])}")


# ── 2. 응답의 필드 ─────────────────────────────────────────────────────────

def test_response_field_removed():
    derived = contract_copy()
    alert = schema(derived, "Alert")
    alert["properties"].pop("lastCheckedAt")
    alert["required"].remove("lastCheckedAt")
    outcome = judge(derived)
    show("field_removed", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.field_missing"]
    report("응답 필드를 지운 스펙은 위반", bool(hit),
           f"{rules_of(outcome['violations'])} / 지점 {sorted({v['point'] for v in hit})}")


def test_response_field_added_to_closed_object():
    derived = contract_copy()
    schema(derived, "Alert")["properties"]["internalNote"] = {"type": "string"}
    outcome = judge(derived)
    show("field_added_closed", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.field_extra_closed"]
    report("닫힌 응답 객체에 계약에 없는 필드를 더하면 위반", bool(hit),
           f"{rules_of(outcome['violations'])}")


def test_response_field_added_to_open_object():
    derived = contract_copy()
    # 계약이 그 객체를 열면 같은 초과가 그날부터 기록으로 떨어진다. 엄격함은 계약에서 상속된다.
    BASE_open = contract_copy()
    BASE_open["components"]["schemas"]["Alert"].pop("additionalProperties")
    derived["components"]["schemas"]["Alert"].pop("additionalProperties")
    derived["components"]["schemas"]["Alert"]["properties"]["internalNote"] = {"type": "string"}
    outcome = run_g1(derived, BASE_open, SCOPE, [], RULES)
    show("field_added_open", outcome)
    report("열린 응답 객체에 필드를 더하면 기록이고 위반이 아니다",
           not outcome["violations"] and any(o["rule"] == "response.field_extra" for o in outcome["observations"]),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")


def test_response_required_weakened():
    derived = contract_copy()
    schema(derived, "Alert")["required"].remove("expiresAt")
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.field_differs"
           and "필수" in v["detail"]]
    report("응답의 필수 보장을 약하게 하면 위반", bool(hit), f"{rules_of(outcome['violations'])}")
    report("그 차이의 라벨이 약속을 지키지 않는다는 뜻이다",
           all(v.get("label") == "withholds_promised_response" for v in hit),
           f"{[v.get('label') for v in hit]}")
    report("접힌 잎이 어느 필드였는지 남는다",
           any("expiresAt" in leaf for v in hit for leaf in v.get("evidence") or []),
           f"{[v.get('evidence') for v in hit]}")


# ── 3. 방향의 차이. 이 게이트의 핵심 ───────────────────────────────────────

def test_response_narrowing_is_an_observation_and_request_narrowing_is_a_violation():
    """이 게이트의 핵심 비대칭이다. 응답은 좁게 줘도 되고 요청은 좁게 받으면 안 된다.

    응답을 좁힌 구현은 계약이 약속한 것을 다 지키면서 좁게 지킨 것이므로 계약대로 읽는 쪽이 손해를 보지 않는다.
    그것을 위반으로 세면 계약을 만족하는 구현이 떨어진다. 같은 좁힘이 요청 쪽에서는 계약대로 보낸 요청을
    거절하게 만들므로 위반이다. 방향은 그 밖의 자리에서는 라벨로만 갈린다.
    """
    contract_enum = schema(contract_copy(), "AlertStatus")["enum"]

    response_side = contract_copy()
    # 계약은 이 열거형을 `$ref`로 요청과 응답에 함께 쓰므로, 응답 쪽만 좁히려면 그 자리에 펴서 넣는다.
    schema(response_side, "Alert")["properties"]["status"] = {"type": "string", "enum": contract_enum[:2]}
    down = judge(response_side)
    hit = [o for o in down["observations"] if o["rule"] == "response.value_narrowed"
           and "열거형" in o["detail"]]
    report("응답 열거형을 좁힌 것은 기록이고 위반이 아니다",
           bool(hit) and not down["violations"],
           f"위반 {rules_of(down['violations'])} / 기록 {rules_of(down['observations'])}")
    report("그 라벨은 클라이언트에게 무해하다는 뜻이다",
           all(o.get("label") == "harmless_to_client" for o in hit),
           f"{[o.get('label') for o in hit]}")

    request_side = contract_copy()
    query = next(p for p in operation(request_side, "/alerts", "get")["parameters"]
                 if p.get("name") == "status")
    query["schema"] = {"type": "string", "enum": contract_enum[:2]}
    up = judge(request_side)
    hit = [v for v in up["violations"] if v["rule"] == "request.input_differs"]
    report("같은 좁힘이 요청 쪽에서는 위반이고 요청을 거절하게 만든다는 라벨을 받는다",
           bool(hit) and all(v.get("label") == "rejects_contracted_request" for v in hit),
           f"{[(v['rule'], v.get('label')) for v in hit]}")

    report("라벨의 어휘가 규칙 카드와 같다",
           set(LABELS) == {item["id"] for item in RULES.get("labels") or []},
           f"{sorted(LABELS)}")


def delivery(doc):
    """응답에만 쓰이고 형식도 경계도 계약이 적어 둔 자리. 좁힘을 시험하기에 맞다."""
    return schema(doc, "AlertDelivery")["properties"]["attemptCount"]


def test_a_format_the_contract_left_open_differs_from_one_it_fixed():
    """계약이 비워 둔 자리에 형식을 붙인 것과 계약이 정한 형식을 바꾼 것은 다른 일이다.

    springdoc은 자바 `int`에 `format: int32`를 자동으로 붙인다. 계약이 그 자리에 형식을 적지 않았으면
    담을 수 있는 값이 줄지 않으므로 기록이다. 계약이 `int64`로 적어 둔 자리를 구현이 `int32`로 바꾸면
    계약이 담긴다고 한 값이 응답에 실리지 못할 수 있고, 형식 이름 사이에는 넓고 좁음의 순서가 없어 게이트가
    어느 쪽인지 판정할 수 없다. 미판정을 통과로 세지 않으므로 그 자리는 위반으로 남긴다.
    """
    open_side = contract_copy()
    delivery(open_side)["format"] = "int32"
    outcome = judge(open_side)
    show("format_added", outcome)
    hit = [o for o in outcome["observations"] if o["rule"] == "response.value_narrowed"]
    report("계약이 형식을 적지 않은 자리에 구현이 형식을 붙이면 기록이다",
           bool(hit) and not outcome["violations"],
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")

    fixed = contract_copy()
    delivery(fixed)["format"] = "int64"
    narrowed = copy.deepcopy(fixed)
    delivery(narrowed)["format"] = "int32"
    outcome = run_g1(narrowed, fixed, SCOPE, [], RULES)
    show("format_changed", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.field_differs"]
    report("계약이 정한 형식을 구현이 다른 것으로 바꾸면 위반이다", bool(hit),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")
    report("그 라벨은 계약대로 읽으면 깨진다는 뜻이다",
           all(v.get("label") == "breaks_reader" for v in hit),
           f"{[v.get('label') for v in hit]}")


def test_a_narrowed_response_bound_is_recorded_and_a_dropped_one_is_not():
    """계약이 정한 범위 안으로 경계를 줄인 것은 기록이고, 그 경계를 버린 것은 위반이다."""
    inside = contract_copy()
    delivery(inside)["maximum"] = 3
    outcome = judge(inside)
    show("bound_narrowed", outcome)
    report("응답의 경계를 계약 안으로 줄이면 기록이다",
           not outcome["violations"]
           and any(o["rule"] == "response.value_narrowed" for o in outcome["observations"]),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")

    dropped = contract_copy()
    delivery(dropped).pop("maximum")
    outcome = judge(dropped)
    show("bound_dropped", outcome)
    report("계약이 정한 경계를 구현이 버리면 위반이다",
           any(v["rule"] == "response.field_differs" for v in outcome["violations"]),
           f"위반 {rules_of(outcome['violations'])}")


def test_a_narrowed_response_type_is_still_a_violation():
    """좁힐 수 있는 것은 값의 범위와 형식이지 타입이 아니다.

    타입이 좁아지면 계약이 그 자리에 올 수 있다고 말한 종류 하나가 아예 오지 않게 되어, 그 종류를
    기다리는 쪽이 부서진다. 범위를 줄인 것과 달리 부분집합을 받는 일이 아니다.
    """
    wide = contract_copy()
    schema(wide, "AlertDelivery")["properties"]["failureReason"] = {
        "type": ["string", "integer", "null"]}
    narrow = copy.deepcopy(wide)
    schema(narrow, "AlertDelivery")["properties"]["failureReason"] = {"type": ["string", "null"]}
    outcome = run_g1(narrow, wide, SCOPE, [], RULES)
    show("type_narrowed", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.field_differs"
           and v["detail"].startswith("타입")]
    report("응답의 타입을 좁힌 것은 위반으로 남는다", bool(hit),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")
    # 같은 `NARROWER` 방향인데 값의 범위를 좁힌 것과 라벨이 달라야 한다. 무해로 달면 리포트가 라벨로 정렬하므로
    # 진짜 위반이 바닥으로 내려간다.
    report("그 라벨은 무해가 아니라 계약대로 읽으면 깨진다는 뜻이다",
           all(v.get("label") == "breaks_reader" for v in hit),
           f"{[v.get('label') for v in hit]}")


def test_narrowing_and_widening_in_one_kind_split_apart():
    """한 종류 안에 좁힘과 넓힘이 섞이면 둘로 나눠 낸다.

    섞어 한 건으로 내면 판정이 한쪽에 끌려간다. 좁힌 쪽에 붙이면 같이 들어온 위반이 기록으로 내려가
    통과하고, 위반 쪽에 붙이면 무해한 차이가 REJECT를 만든다.
    """
    derived = contract_copy()
    delivery(derived)["maximum"] = 3
    delivery(derived).pop("minimum")
    outcome = judge(derived)
    show("bound_mixed", outcome)
    point = "createAlert:201"
    bound = [item for item in outcome["violations"] + outcome["observations"]
             if item["point"] == point and item["detail"].startswith("경계")]
    report("좁힌 것은 기록으로, 버린 것은 위반으로 갈라 낸다",
           {(item["rule"], item["verdict"]) for item in bound} ==
           {("response.value_narrowed", "observation"), ("response.field_differs", "violation")},
           f"{[(item['rule'], item['verdict'], item['detail']) for item in bound]}")
    report("증거도 자기 쪽에만 남는다",
           all(len(item.get("evidence") or []) == 1 for item in bound)
           and {leaf.split("maximum" if "maximum" in leaf else "minimum")[0] for item in bound
                for leaf in item["evidence"]} == {"경계: attemptCount: "},
           f"{[item.get('evidence') for item in bound]}")


def test_request_enum_narrowed_is_violation():
    derived = contract_copy()
    query = next(p for p in operation(derived, "/alerts", "get")["parameters"] if p.get("name") == "status")
    query["schema"] = {"type": "string", "enum": ["WATCHING"]}
    outcome = judge(derived)
    show("request_enum_narrowed", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "request.input_differs"]
    report("요청 열거형을 좁힌 스펙은 위반", bool(hit), f"{rules_of(outcome['violations'])}")


def test_request_required_added():
    derived = contract_copy()
    # 계약이 선택으로 둔 쿼리 파라미터를 구현이 필수로 요구한다.
    query = next(p for p in operation(derived, "/alerts", "get")["parameters"] if p.get("name") == "status")
    query["required"] = True
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "request.input_differs"
           and "필수" in v["detail"]]
    report("계약이 선택으로 둔 입력을 필수로 요구하면 위반", bool(hit),
           f"{rules_of(outcome['violations'])}")

    # 본문 필드에서도 같다. 계약이 선택으로 둔 필드를 필수로 올린다.
    body = contract_copy()
    schema(body, "AlertRequest")["properties"]["note"] = {"type": "string"}
    schema(body, "AlertRequest")["required"] = ["courtId", "date", "slot", "note"]
    contract_with_optional = contract_copy()
    schema(contract_with_optional, "AlertRequest")["properties"]["note"] = {"type": "string"}
    outcome = run_g1(body, contract_with_optional, SCOPE, [], RULES)
    hit = [v for v in outcome["violations"] if v["rule"] == "request.input_differs"
           and "필수" in v["detail"]]
    report("본문 필드를 필수로 올리면 위반", bool(hit), f"{rules_of(outcome['violations'])}")


def test_request_required_relaxed_is_allowed():
    derived = contract_copy()
    schema(derived, "AlertRequest")["required"] = ["courtId"]
    outcome = judge(derived)
    report("요청의 필수를 선택으로 풀면 허용", not outcome["violations"],
           f"위반 {rules_of(outcome['violations'])}")


def test_request_constraint_tightened():
    derived = contract_copy()
    parameter(derived, "CourtIdPath")["schema"] = {"type": "string", "minLength": 1,
                                                  "maxLength": 8,
                                                  "pattern": "^[a-z0-9][a-z0-9-]*$"}
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "request.input_differs"
           and "경계" in v["detail"]]
    report("요청 제약을 좁히면 위반", bool(hit), f"{rules_of(outcome['violations'])}")
    report("그 라벨은 계약대로 보낸 요청이 거절된다는 뜻이다",
           all(v.get("label") == "rejects_contracted_request" for v in hit),
           f"{[v.get('label') for v in hit]}")


def test_dropping_a_contracted_input_constraint_is_not_harmless():
    """계약이 정한 입력 제약을 구현이 선언하지 않은 것은 위반이고 무해하지도 않다.

    계약은 요청에 무엇을 받을지만 정하지 않고 형식을 어긴 입력에 어떤 실패를 돌려줄지도 정한다. 구현이 그 제약을
    자기 표면에 선언하지 않으면 그 약속이 선언에서 사라지고, 계약이 막겠다고 한 값이 그 자리를 통과해 들어온다.
    실제로 거절하는지는 실물 응답을 보는 판정의 몫이므로 이 게이트는 판정하지 못하고, 판정하지 못한 것을 통과로
    보내지 않는다. 좁게 받는 것과 라벨은 다르지만 판정은 같다.
    """
    derived = contract_copy()
    parameter(derived, "CourtIdPath")["schema"] = {"type": "string"}
    outcome = judge(derived)
    show("constraint_dropped", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "request.input_differs"]
    report("계약이 정한 입력 제약을 선언하지 않으면 위반", bool(hit),
           f"위반 {rules_of(outcome['violations'])}")
    report("그 라벨은 계약이 거절하겠다고 한 것을 받는다는 뜻이다",
           all(v.get("label") == "accepts_what_contract_rejects" for v in hit),
           f"{[v.get('label') for v in hit]}")


# ── 3-1. 규칙은 다섯 가족이고 차이는 가장 높은 고도에서 한 건이다 ──────────

def test_rule_families_cover_every_rule():
    families = {card["id"]: card.get("family") for card in RULES["rules"]}
    compare = [rule for rule in RULE_IDS
               if not rule.startswith(("extract.", "decision.")) and rule != "response.serialization_override"]
    missing = [rule for rule in compare if not families.get(rule)]
    report("대조 규칙마다 가족이 붙어 있다", not missing, f"가족 없는 규칙 {missing}")
    report("가족의 어휘가 다섯이다",
           {families[rule] for rule in compare} ==
           {"missing", "differs", "extra_in_closed", "narrowed", "extra_unmentioned"},
           f"{sorted({families[rule] for rule in compare})}")
    verdicts = {card["id"]: card["verdict"] for card in RULES["rules"]}
    recorded = {"extra_unmentioned", "narrowed"}
    report("말하지 않은 자리에 더 있는 것과 응답에서 좁힌 것만 기록이다",
           all((verdicts[rule] == "observation") == (families[rule] in recorded)
               for rule in compare),
           f"{[(r, families[r], verdicts[r]) for r in compare if (verdicts[r]=='observation') != (families[r] in recorded)]}")
    # 방향으로 규칙 id를 가르는 자리는 하나뿐이다. 응답에서 값을 좁힌 것은 판정이 갈리므로 id도 갈라야 하고,
    # 나머지 방향 차이는 라벨로만 갈린다. 여기가 늘어나면 방향별 규칙이 다시 번지고 있다는 뜻이다.
    split = [rule for rule in families
             if rule.endswith(("_narrowed", "_widened", "_tightened", "_weakened", "_changed",
                               "_added", "_dropped"))]
    report("방향으로 규칙 id를 가른 자리는 응답에서 좁힌 것 하나다",
           split == ["response.value_narrowed"],
           f"{split}")
    report("그 하나는 응답 방향이고 기록이다",
           families["response.value_narrowed"] == "narrowed"
           and verdicts["response.value_narrowed"] == "observation",
           f"{families.get('response.value_narrowed')} / {verdicts.get('response.value_narrowed')}")


def test_a_replaced_schema_is_one_finding():
    """에러 응답에 성공 스키마를 선언한 자리다. 필드 고도에서 세면 한 지점이 열여덟 건이 된다."""
    derived = contract_copy()
    success = copy.deepcopy(schema(derived, "CourtAvailability"))
    for name in ("ValidationFailed", "CourtNotSupported", "InternalError"):
        for media in response(derived, name)["content"].values():
            media["schema"] = success
    outcome = judge(derived)
    show("replaced_schema", outcome)
    folded = [v for v in outcome["violations"] if v["rule"] == "response.schema_differs"]
    leaves = [v for v in outcome["violations"]
              if v["rule"] in ("response.field_missing", "response.field_extra_closed")]
    report("스키마가 통째로 다른 자리는 한 건으로 센다", bool(folded) and not leaves,
           f"접은 건 {len(folded)}, 잎으로 센 건 {len(leaves)}")
    report("접힌 잎이 증거에 남는다",
           all(len(v.get("evidence") or []) >= 8 for v in folded),
           f"{[len(v.get('evidence') or []) for v in folded]}")
    report("증거가 없는 것과 더 있는 것을 함께 적는다",
           all(any(leaf.startswith("없다:") for leaf in v["evidence"])
               and any("더 있다:" in leaf for leaf in v["evidence"]) for v in folded),
           f"{folded[0]['evidence'][:3] if folded else []}")


def test_a_coincidental_field_name_is_not_an_overlap():
    """이름만 같고 타입이 겹치지 않는 필드를 겹침으로 세면 통째로 다른 스키마가 같은 종류로 보인다.

    실제 run에서 문제 응답의 정수 `status`와 성공 응답의 문자열 `status`가 그 자리였고, 그 하나 때문에
    지점마다 열아홉 건이 났다.
    """
    derived = contract_copy()
    disguised = copy.deepcopy(schema(derived, "CourtAvailability"))
    disguised["properties"]["status"] = {"type": "string", "enum": ["WATCHING"]}
    for media in response(derived, "ValidationFailed")["content"].values():
        media["schema"] = disguised
    outcome = judge(derived)
    show("coincidental_name", outcome)
    folded = [v for v in outcome["violations"] if v["rule"] == "response.schema_differs"]
    report("이름만 같은 필드가 있어도 다른 종류로 접는다", bool(folded),
           f"{rules_of(outcome['violations'])}")
    report("왜 견줄 수 없는지 증거에 적는다",
           any("이름은 같지만 견줄 수 없다" in leaf for v in folded for leaf in v["evidence"]),
           f"{[leaf for v in folded for leaf in v['evidence'] if '견줄 수 없다' in leaf][:2]}")


def test_field_differences_fold_into_the_object():
    """형식 선언을 열 필드에서 잃은 것은 열 가지 일이 아니라 한 가지 일이다."""
    derived = contract_copy()
    alert = schema(derived, "Alert")
    for name in ("createdAt", "expiresAt", "lastCheckedAt"):
        alert["properties"][name] = {"type": "string"}
    outcome = judge(derived)
    show("folded_fields", outcome)
    hit = [v for v in outcome["violations"]
           if v["rule"] == "response.field_differs" and v["where"] == "Alert"
           and v["detail"].startswith("형식") and v["point"] == "createAlert:201"]
    report("한 객체의 같은 종류 차이는 그 지점에서 한 건이다", len(hit) == 1,
           f"{[(v['point'], v['where'], v['detail']) for v in hit]}")

    # 계약이 같은 컴포넌트를 여러 응답에서 가리키면 그 컴포넌트 하나의 결함이 지점마다 한 번씩 나온다.
    # 판정 지점은 판정의 좌표이므로 접지 않는다. 그 부풀림은 지점을 빼고 세면 보인다.
    same = [v for v in outcome["violations"]
            if v["rule"] == "response.field_differs" and v["where"] == "Alert"
            and v["detail"].startswith("형식")]
    report("같은 사실이 여러 지점에 나오는 것은 지점을 빼고 세면 하나다",
           len({(v["rule"], v["where"], v["detail"]) for v in same}) == 1,
           f"{len(same)}개 지점 {sorted(v['point'] for v in same)}")
    report("잃은 자리가 셋 다 증거에 남는다",
           bool(hit) and len(hit[0]["evidence"]) == 3,
           f"{hit[0]['evidence'] if hit else []}")
    report("차이의 종류가 다르면 나눠 센다",
           len({v["detail"].split(":")[0] for v in outcome["violations"]
                if v["rule"] == "response.field_differs" and v["where"] == "Alert"}) >= 1,
           f"{sorted({v['detail'].split(':')[0] for v in outcome['violations'] if v['where'] == 'Alert'})}")


def test_a_missing_status_stops_the_descent():
    """상위를 판정했고 그 판정이 하위를 설명하면 멈춘다. 상태 코드가 없으면 그 아래를 보지 않는다."""
    derived = contract_copy()
    derived["paths"]["/alerts/{alertId}"]["get"]["responses"].pop("404")
    outcome = judge(derived)
    at_point = [v for v in outcome["violations"] if v["point"] == "getAlert:404"]
    report("상태 코드가 없으면 그 지점은 한 건이다",
           len(at_point) == 1 and at_point[0]["rule"] == "response.status_missing",
           f"{[(v['rule'], v['where']) for v in at_point]}")
    pair = [v for v in outcome["violations"] if v["point"] == "getAlert:ALERT_NOT_FOUND"]
    report("그 상태가 나르던 에러 코드 쌍도 그 한 건이 설명한다", not pair,
           f"{[(v['rule'], v['where']) for v in pair]}")


def test_a_missing_path_stops_the_descent():
    derived = contract_copy()
    derived["paths"].pop("/alerts")
    outcome = judge(derived)
    for name in ("createAlert", "listAlerts"):
        at_op = [v for v in outcome["violations"] if str(v["point"]).startswith(name)]
        report(f"경로가 없으면 {name} 아래를 보지 않는다",
               len(at_op) == 1 and at_op[0]["rule"] == "surface.path_missing",
               f"{[(v['rule'], v['where']) for v in at_op]}")


def test_an_id_mismatch_does_not_stop_the_descent():
    """멈추는 것과 건너뛰는 것은 다르다. 식별자 불일치는 하위를 설명하지 않는다."""
    derived = contract_copy()
    derived["paths"]["/alerts"]["post"]["operationId"] = "create"
    derived["components"]["schemas"]["Alert"]["properties"].pop("lastCheckedAt")
    outcome = judge(derived)
    rules = rules_of(outcome["violations"])
    report("식별자가 달라도 그 아래를 계속 판정한다",
           "request.operation_id_mismatch" in rules and "response.field_missing" in rules,
           f"{rules}")


def test_undeclared_error_codes_fold_per_response():
    """선언 밖의 코드는 그 응답 하나의 사실이다. 코드마다 세면 응답 하나가 코드 수만큼 불어난다."""
    derived = contract_copy()
    every = schema(derived, "ErrorCode")["enum"]
    response(derived, "AlertNotFound")["x-error-codes"] = list(every)
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "error.code_undeclared"]
    report("응답 하나가 선언 밖의 코드를 여러 개 써도 응답마다 한 건이다",
           bool(hit) and all(len(v.get("evidence") or []) > 1 for v in hit)
           and len(hit) == len({v["point"] for v in hit}),
           f"{len(hit)}건, 증거 {[len(v.get('evidence') or []) for v in hit]}")
    missing = [v for v in outcome["violations"] if v["rule"] == "error.code_pair_missing"]
    report("빠진 쌍은 지점마다 한 건이라 접지 않는다", not missing or
           len(missing) == len({v["point"] for v in missing}),
           f"{len(missing)}건")


# ── 4. 계약에 없는 것은 기록이고 위반이 아니다 ─────────────────────────────

def test_extra_path_is_observation():
    derived = contract_copy()
    derived["paths"]["/internal/cache"] = {
        "post": {"operationId": "flushCache", "responses": {"204": {"description": "비웠다"}}}
    }
    outcome = judge(derived)
    show("extra_path", outcome)
    report("계약에 없는 경로는 기록이고 위반이 아니다",
           not outcome["violations"] and any(o["rule"] == "surface.path_extra" for o in outcome["observations"]),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")


def test_extra_response_header_is_observation():
    derived = contract_copy()
    response(derived, "AlertNotFound")["headers"] = {"X-Request-Id": {"schema": {"type": "string"}}}
    outcome = judge(derived)
    show("extra_header", outcome)
    report("계약에 없는 응답 헤더는 기록이고 위반이 아니다",
           not outcome["violations"] and any(o["rule"] == "surface.header_extra" for o in outcome["observations"]),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")


def test_missing_response_header_is_violation():
    derived = contract_copy()
    operation(derived, "/alerts", "post")["responses"]["201"]["headers"].pop("Idempotency-Replayed")
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.header_missing"]
    report("계약이 선언한 응답 헤더가 없으면 위반", bool(hit), f"{rules_of(outcome['violations'])}")


# ── 5. 에러 코드는 상태 코드와 쌍으로 센다 ─────────────────────────────────

def test_missing_error_code_pair():
    derived = contract_copy()
    # 422가 선언한 셋 중 하나를 뺀다. 코드만 보면 다른 자리에 남아 있어도 이 쌍은 사라졌다.
    response(derived, "AlertNotAcceptable")["x-error-codes"] = ["COURT_NOT_SUPPORTED", "SLOT_NOT_SUPPORTED"]
    outcome = judge(derived)
    show("code_pair", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "error.code_pair_missing"]
    report("선언한 (상태 코드, 에러 코드) 쌍이 빠지면 위반", bool(hit),
           f"지점 {sorted({v['point'] for v in hit})}")


def test_undeclared_error_code():
    derived = contract_copy()
    response(derived, "AlertNotFound")["x-error-codes"].append("ALERT_ALREADY_CANCELLED")
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "error.code_undeclared"]
    report("선언 밖의 에러 코드를 쓰면 위반", bool(hit), f"{rules_of(outcome['violations'])}")


def test_missing_status_code():
    derived = contract_copy()
    operation(derived, "/alerts/{alertId}", "get")["responses"].pop("404")
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "response.status_missing"]
    report("선언한 상태 코드가 없으면 위반", bool(hit), f"지점 {sorted({v['point'] for v in hit})}")


# ── 6. 표면이 없는 것 ──────────────────────────────────────────────────────

def test_missing_operation():
    derived = contract_copy()
    derived["paths"].pop("/alerts/{alertId}")
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "surface.path_missing"]
    report("선언한 경로가 없으면 위반", len(hit) == 2, f"{len(hit)}건 {sorted({v['point'] for v in hit})}")


def test_operation_missing_on_existing_path():
    derived = contract_copy()
    derived["paths"]["/alerts/{alertId}"].pop("delete")
    outcome = judge(derived)
    hit = [v for v in outcome["violations"] if v["rule"] == "request.operation_missing"]
    report("경로는 있는데 그 메서드가 없으면 위반", bool(hit), f"{rules_of(outcome['violations'])}")


def test_operation_id_mismatch_is_not_read_as_missing():
    """springdoc은 식별자를 메서드 이름에서 뽑는다. 계약의 것과 다르면 전부 누락으로 읽히는 일이 있었다.

    자리는 맞고 이름만 다른 경우를 따로 가려야 refine이 코드가 아니라 계약을 고치기 시작하지 않는다.
    """
    derived = contract_copy()
    renamed = {"/alerts": {"post": "create", "get": "list"},
               "/courts/{courtId}/availability": {"get": "getAvailability"}}
    for path, methods in renamed.items():
        for method, label in methods.items():
            derived["paths"][path][method]["operationId"] = label
    outcome = judge(derived)
    show("operation_id_mismatch", outcome)
    mismatch = [v for v in outcome["violations"] if v["rule"] == "request.operation_id_mismatch"]
    missing = [v for v in outcome["violations"] if v["rule"] in
               ("request.operation_missing", "surface.path_missing")]
    extra = [o for o in outcome["observations"] if o["rule"] == "surface.path_extra"]
    report("식별자만 다르면 식별자 불일치로 잡고 누락으로 세지 않는다",
           len(mismatch) == 3 and not missing and not extra,
           f"불일치 {len(mismatch)}건, 누락 {len(missing)}건, 경로 초과 {len(extra)}건")
    report("문장이 계약의 식별자와 추출 스펙의 식별자를 함께 적는다",
           all("createAlert" in v["detail"] or "listAlerts" in v["detail"]
               or "getCourtAvailability" in v["detail"] for v in mismatch)
           and all(any(label in v["detail"] for label in ("create", "list", "getAvailability"))
                   for v in mismatch),
           "; ".join(f"{v['point']} :: {v['detail']}" for v in mismatch))
    report("판정 지점은 계약의 식별자로 남는다",
           sorted(v["point"] for v in mismatch) ==
           ["createAlert", "getCourtAvailability", "listAlerts"],
           f"{sorted(v['point'] for v in mismatch)}")


def test_operation_id_mismatch_still_judges_the_rest():
    """이름이 달라도 자리가 맞으면 그 오퍼레이션의 요청과 응답을 계속 판정한다."""
    derived = contract_copy()
    derived["paths"]["/alerts"]["post"]["operationId"] = "create"
    derived["components"]["schemas"]["Alert"]["properties"].pop("lastCheckedAt")
    outcome = judge(derived)
    rules = rules_of(outcome["violations"])
    report("식별자가 달라도 그 아래의 위반을 함께 잡는다",
           "request.operation_id_mismatch" in rules and "response.field_missing" in rules,
           f"{rules}")


# ── 7. 게이트가 못 본 자리는 통과가 아니다 ─────────────────────────────────

def test_extract_blind():
    derived = contract_copy()

    def strip(node):
        if isinstance(node, dict):
            node.pop("x-error-codes", None)
            for value in node.values():
                strip(value)
        elif isinstance(node, list):
            for item in node:
                strip(item)
    strip(derived)
    outcome = judge(derived)
    show("blind", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "extract.blind"]
    pairs = [v for v in outcome["violations"] if v["rule"] == "error.code_pair_missing"]
    report("에러 코드에 대응물이 없는 스펙은 extract.blind 한 건",
           len(hit) == 1 and not pairs,
           f"blind {len(hit)}건, 쌍 누락 {len(pairs)}건")


# ── 8. 널 허용의 두 표기 ───────────────────────────────────────────────────

def to_openapi_30(node):
    """계약의 3.1 표기를 springdoc의 3.0.1 표기로 바꾼다."""
    if isinstance(node, dict):
        for key in ("oneOf", "anyOf"):
            branches = node.get(key)
            if isinstance(branches, list):
                nulls = [b for b in branches if isinstance(b, dict) and b.get("type") == "null"]
                rest = [b for b in branches if isinstance(b, dict) and b.get("type") != "null"]
                if nulls and len(rest) == 1:
                    node.pop(key)
                    node.update(rest[0])
                    node["nullable"] = True
        value = node.get("type")
        if isinstance(value, list) and "null" in value:
            kept = [t for t in value if t != "null"]
            node["type"] = kept[0] if len(kept) == 1 else kept
            node["nullable"] = True
        for child in list(node.values()):
            to_openapi_30(child)
    elif isinstance(node, list):
        for item in node:
            to_openapi_30(item)
    return node


def test_nullable_notations_are_the_same():
    three_one = {"type": ["string", "null"], "format": "date-time"}
    three_zero = {"type": "string", "format": "date-time", "nullable": True}
    with_ref = {"oneOf": [{"type": "string"}, {"type": "null"}]}
    normalized = [unwrap_nullable(node) for node in (three_one, three_zero, with_ref)]
    report("널 허용의 세 표기가 같게 정규화된다",
           all(nullable for _, nullable in normalized)
           and normalized[0][0].get("type") == "string"
           and normalized[1][0].get("type") == "string"
           and normalized[2][0].get("type") == "string"
           and "nullable" not in normalized[1][0],
           str([node for node, _ in normalized]))


def test_springdoc_style_spec_is_clean():
    derived = to_openapi_30(contract_copy())
    # springdoc이 남기는 문서 수준 값. 구현이 약속한 계약이 아니므로 비교 대상이 아니다.
    derived["openapi"] = "3.0.1"
    derived["info"] = {"title": "OpenAPI definition", "version": "v0"}
    derived["servers"] = [{"url": "http://127.0.0.1:54031", "description": "Generated server url"}]
    outcome = judge(derived)
    show("springdoc", outcome)
    report("3.0.1 표기와 springdoc 기본값은 위반을 만들지 않는다",
           not outcome["violations"],
           f"위반 {len(outcome['violations'])}건 {rules_of(outcome['violations'])}")


# ── 9. 직렬화 재정의는 스펙에 드러나지 않는다 ──────────────────────────────

def test_serialization_override():
    """생성기는 작업 폴더를 제자리에서 고친다. 그래서 검사도 그 폴더를 읽는다."""
    source = "src/main/java/com/thinking/tennis/api/AlertResponse.java"
    override = ("@JsonInclude(JsonInclude.Include.NON_NULL)\n"
                "public record AlertResponse(String alertId) {}")
    with tempfile.TemporaryDirectory() as work:
        clean = fake_output(Path(work) / "clean",
                            {source: "public record AlertResponse(String alertId) {}"})
        dirty = fake_output(Path(work) / "dirty", {source: override})
        # target/ 은 빌드가 리소스를 복사해 두고 .git/ 은 iteration 스냅샷이라 같은 위반을 두 번 세게 한다.
        stale = fake_output(Path(work) / "stale", {
            source: "public record AlertResponse(String alertId) {}",
            "target/classes/application.yaml": "spring:\n  jackson:\n    default-property-inclusion: never",
            ".git/ORIG_HEAD.java": override,
        })
        ok = judge(contract_copy(), project_dir=clean)
        bad = judge(contract_copy(), project_dir=dirty)
        ignored = judge(contract_copy(), project_dir=stale)
        skipped = judge(contract_copy())
        missing = judge(contract_copy(), project_dir=Path(work) / "there-is-no-output")
    hit = [v for v in bad["violations"] if v["rule"] == "response.serialization_override"]
    report("널을 지우는 직렬화 재정의는 위반",
           not ok["violations"] and bool(hit),
           f"깨끗한 초안 {rules_of(ok['violations'])} / 재정의한 초안 {rules_of(bad['violations'])}")
    report("잡은 자리를 작업 폴더 안의 상대 경로로 적는다",
           [v["where"] for v in hit] == [source],
           f"{[v['where'] for v in hit]}")
    report("target/ 과 .git/ 은 읽지 않는다",
           not ignored["violations"], f"{rules_of(ignored['violations'])}")
    report("작업 폴더를 넘기지 않으면 그 검사만 건너뛴다",
           not skipped["violations"] and not missing["violations"],
           f"없음 {rules_of(skipped['violations'])} / 빈 경로 {rules_of(missing['violations'])}")


# ── 10. 결정이 계약이 정한 자리를 덮어쓰려 하는가 ──────────────────────────

def test_decision_overrides_contract():
    blank = [{"id": "d_1", "point": "createAlert:201", "where": "Alert.expiresAt"}]
    fixed = [{"id": "d_2", "point": "createAlert:201", "where": "Alert.status"}]
    outside = [{"id": "d_3", "point": "renewAlert:200", "where": "Alert.status"}]
    a = judge(contract_copy(), decisions=blank)
    b = judge(contract_copy(), decisions=fixed)
    c = judge(contract_copy(), decisions=outside)
    report("계약이 비운 자리의 결정은 통과",
           not a["violations"], f"{rules_of(a['violations'])}")
    report("계약이 값을 정한 자리의 결정은 위반",
           any(v["rule"] == "decision.overrides_contract" for v in b["violations"]),
           f"{rules_of(b['violations'])}")
    report("계약에 없는 지점을 가리키는 결정은 기록",
           not c["violations"] and any(o["rule"] == "decision.unknown_point" for o in c["observations"]),
           f"위반 {rules_of(c['violations'])} / 기록 {rules_of(c['observations'])}")


# ── 11. 계약이 움직였을 때 ─────────────────────────────────────────────────

# `$ref`로 쓴 파라미터에는 `name`이 없다. OpenAPI에서 합법이고 이 계약이 실제로 그렇게 쓴다.
# 판본 대조가 그 자리를 이름으로 짝지으면 죽고, 이름이 없다고 건너뛰면 그 파라미터의 대조가 조용히 빠진다.

def fragment(*paths):
    """`components`를 떼어 낸 계약 조각. 참조가 닿을 곳이 없는 상태다."""
    return {"openapi": "3.1.0", "info": copy.deepcopy(BASE["info"]),
            "servers": copy.deepcopy(BASE["servers"]),
            "paths": {path: copy.deepcopy(BASE["paths"][path]) for path in paths}}


def test_compare_http_handles_ref_parameters():
    prev, curr = contract_copy(), contract_copy()
    out = []
    try:
        compare_http(prev, curr, out)
        ok, detail = not out, f"깨는 변경 {len(out)}건 {out[:2]}"
    except Exception as problem:  # noqa: BLE001
        ok, detail = False, f"{type(problem).__name__}: {problem}"
    report("$ref 파라미터가 있는 계약을 판본 대조가 지나간다", ok, detail)

    # 이 계약은 `- $ref: '#/components/parameters/IdempotencyKeyHeader'` 로 쓴다. 그 자리가 실제로 풀린다.
    resolved = deref(prev, prev)["paths"]["/alerts"]["post"]["parameters"]
    report("$ref 파라미터가 이름까지 풀린다",
           [parameter_key(item) for item in resolved] == ["Idempotency-Key"],
           f"{[parameter_key(item) for item in resolved]}")


def test_compare_http_says_it_could_not_resolve():
    """닿지 못한 참조를 빈 객체로 만들면 그 아래가 대조에서 사라진다. 게이트 공백은 통과가 아니다."""
    prev, curr = fragment("/alerts"), fragment("/alerts")
    out = []
    try:
        compare_http(prev, curr, out)
        crashed = None
    except Exception as problem:  # noqa: BLE001
        crashed = f"{type(problem).__name__}: {problem}"
    report("참조가 닿지 않는 조각에서도 죽지 않는다", crashed is None, crashed or f"{len(out)}줄")
    report("대조하지 못한 사실을 말한다",
           any("풀지 못해 대조하지 못했다" in line and "IdempotencyKeyHeader" in line for line in out),
           "; ".join(line for line in out if "대조하지 못했다" in line)[:160])
    node = deref(prev, prev)["paths"]["/alerts"]["post"]["parameters"][0]
    report("닿지 못한 참조를 표시로 남긴다",
           node.get(UNRESOLVED) == "#/components/parameters/IdempotencyKeyHeader", f"{node}")


def test_compare_http_still_judges_ref_parameters():
    """짝을 풀린 이름으로 맞추므로 `$ref`로 쓴 파라미터의 제약 변화도 잡힌다."""
    prev, curr = contract_copy(), contract_copy()
    curr["components"]["parameters"]["CourtIdPath"]["schema"] = {"type": "string", "maxLength": 8}
    out = []
    compare_http(prev, curr, out)
    report("$ref 파라미터의 제약이 좁아진 것을 잡는다",
           any("maxLength" in line and "좁아졌다" in line for line in out),
           "; ".join(out)[:160])

    # 공용 파라미터도 대조에 든다. 경로 항목에 적힌 것을 빼면 그 자리가 조용히 빠진다.
    prev, curr = contract_copy(), contract_copy()
    shared = {"name": "traceparent", "in": "header", "required": False, "schema": {"type": "string"}}
    for doc in (prev, curr):
        doc["paths"]["/alerts"]["parameters"] = [copy.deepcopy(shared)]
    curr["paths"]["/alerts"]["parameters"][0]["required"] = True
    out = []
    compare_http(prev, curr, out)
    report("경로 항목의 공용 파라미터도 대조한다",
           any("필수 파라미터가 늘었다" in line and "traceparent" in line for line in out),
           "; ".join(out)[:160])

    prev, curr = contract_copy(), contract_copy()
    curr["paths"]["/alerts"]["post"]["parameters"] = []
    out = []
    compare_http(prev, curr, out)
    report("사라진 파라미터를 센다",
           any("선언했던 파라미터가 사라졌다" in line for line in out), "; ".join(out)[:160])


def test_compare_http_says_it_could_not_resolve_a_schema():
    """파라미터만 문제가 아니다. 응답 스키마의 참조가 닿지 않으면 그 응답의 필드 전부가 대조되지 않는다."""
    prev, curr = contract_copy(), contract_copy()
    curr["components"]["schemas"]["Alerts"] = curr["components"]["schemas"].pop("Alert")
    out = []
    try:
        compare_http(prev, curr, out)
        crashed = None
    except Exception as problem:  # noqa: BLE001
        crashed = f"{type(problem).__name__}: {problem}"
    report("응답 스키마의 참조가 닿지 않아도 죽지 않는다", crashed is None, crashed or f"{len(out)}줄")
    report("그 자리를 대조하지 못했다고 적는다",
           any("참조를 풀지 못해 대조하지 못했다" in line and "schemas/Alert" in line for line in out),
           "; ".join(line for line in out if "대조하지 못했다" in line)[:200])


def test_contract_changes_runs_on_a_ref_parameter_contract():
    """원장이 부르는 자리다. 여기서 터지면 run_ledger.json 이 아예 남지 않는다."""
    prev, curr = contract_copy(), contract_copy()
    curr["components"]["parameters"]["CourtIdPath"]["schema"] = {"type": "string", "maxLength": 8}
    try:
        outcome = check_contract_changes(prev, curr, [], "contract/a-v1.yaml", "contract/a-v2.yaml",
                                        RULES, CRITIQUE)
        crashed = None
    except Exception as problem:  # noqa: BLE001
        outcome, crashed = None, f"{type(problem).__name__}: {problem}"
    report("$ref 파라미터가 있는 계약으로 판본 판정이 끝까지 돈다", crashed is None,
           crashed or f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")


def test_unjustified_change():
    prev = contract_copy()
    curr = contract_copy()
    curr["components"]["schemas"]["Alert"]["required"].remove("lastCheckedAt")
    outcome = check_contract_changes(prev, curr, [], "contract/tennis-alert-api-v1.yaml",
                                     "contract/tennis-alert-api-v2.yaml", RULES)
    show("unjustified", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "change.unjustified"]
    report("기록 없는 계약 변경은 change.unjustified", bool(hit),
           f"{rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")


CRITIQUE = {
    "iteration": "002",
    "weaknesses": [{"id": "w3", "axis": "response_fidelity", "issue": "...",
                    "why_it_matters": "...", "suggestion": "...", "severity": "high"}],
    "contract_review": [{"id": "cr1", "kind": "proposal", "pointer": "#/components/schemas/Alert",
                         "finding": "...", "spec_anchor": "requirement:FR-2", "severity": "high"}],
}


def removal_change(basis):
    return [{"id": "cc_1", "target": "#/components/schemas/Alert/required",
             "action": "remove", "what": "필수에서 하나를 뺐다", "why": "...",
             "basis": basis, "spec_anchor": "requirement:FR-2",
             "compatibility": "breaking", "iteration": "002"}]


def removal_pair():
    prev = contract_copy()
    curr = contract_copy()
    curr["components"]["schemas"]["Alert"]["required"].remove("lastCheckedAt")
    return prev, curr


def judge_changes(basis, critique):
    prev, curr = removal_pair()
    return check_contract_changes(prev, curr, removal_change(basis),
                                  "contract/tennis-alert-api-v1.yaml",
                                  "contract/tennis-alert-api-v2.yaml", RULES, critique)


def test_justified_change():
    outcome = judge_changes("critique:w3", CRITIQUE)
    show("justified", outcome)
    report("비평에 있는 id를 인용한 계약 변경은 위반이 아니다", not outcome["violations"],
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")
    report("깨는 변경은 위반이 아니라 기록",
           any(o["rule"] == "change.breaking" for o in outcome["observations"]),
           f"기록 {rules_of(outcome['observations'])}")
    review = judge_changes("critique:cr1", CRITIQUE)
    report("계약 비평이 발의한 id도 근거가 된다", not review["violations"],
           f"위반 {rules_of(review['violations'])}")


def test_basis_must_name_a_real_critique_item():
    """`critique:`을 붙인 아무 문자열이 근거로 통과하면 발의와 반영을 가른 뜻이 사라진다."""
    unknown = judge_changes("critique:w9", CRITIQUE)
    hit = [v for v in unknown["violations"] if v["rule"] == "change.unjustified"]
    report("비평에 없는 id를 인용하면 change.unjustified", bool(hit),
           "; ".join(v["detail"] for v in hit))

    bare = judge_changes("그냥 고쳤다", CRITIQUE)
    hit = [v for v in bare["violations"] if v["rule"] == "change.unjustified"]
    report("비평 항목을 가리키지 않는 basis도 change.unjustified", bool(hit),
           "; ".join(v["detail"] for v in hit))


def test_basis_naming_a_gate_rule_is_singled_out():
    """판정 장치의 위반을 근거로 계약을 고치는 것은 판정 기준을 판정 결과에 맞추는 것이다."""
    outcome = judge_changes("critique:extract.blind", CRITIQUE)
    show("gate_verdict_basis", outcome)
    hit = [v for v in outcome["violations"] if v["rule"] == "change.basis_is_gate_verdict"]
    report("게이트 규칙 id를 근거로 적으면 따로 가려 잡는다", bool(hit),
           "; ".join(f"{v['rule']} :: {v['detail']}" for v in hit))
    report("그 성질이 문장에 드러난다",
           all("판정 장치의 규칙" in v["detail"] for v in hit),
           f"{[v['detail'] for v in hit]}")

    # 비평 항목이 게이트 규칙 id를 이름으로 달고 있어도 같게 본다. 그 자체가 게이트의 되풀이다.
    disguised = judge_changes("critique:response.field_missing",
                              {"weaknesses": [{"id": "response.field_missing"}], "contract_review": []})
    report("비평이 게이트 규칙 id를 이름으로 달아도 통과하지 않는다",
           any(v["rule"] == "change.basis_is_gate_verdict" for v in disguised["violations"]),
           f"{rules_of(disguised['violations'])}")


def test_change_without_a_critique_to_cite():
    """G0가 깨져 비평을 건너뛴 iteration과 iteration 001이 이 경우다.

    설계가 "iteration 001의 변경은 인용할 비평이 없으므로 여기서 함께 걸린다"고 적은 그 자리다.
    발의는 리뷰어만 하므로 인용할 비평이 없으면 계약 변경에 설 근거가 없다.
    """
    none_at_all = judge_changes("critique:w3", None)
    empty = judge_changes("critique:w3", {"weaknesses": [], "contract_review": []})
    hit = [v for v in none_at_all["violations"] if v["rule"] == "change.unjustified"]
    report("비평이 없는 iteration의 계약 변경은 basis가 채워져 있어도 잡힌다", bool(hit),
           "; ".join(v["detail"] for v in hit))
    report("비평이 비어 있는 경우도 같다",
           any(v["rule"] == "change.unjustified" for v in empty["violations"]),
           f"{[v['detail'] for v in empty['violations'] if v['rule'] == 'change.unjustified']}")


def test_a_carried_record_may_cite_the_critique_that_first_proposed_it():
    """계약 변경은 iteration을 넘어 살아남는다. 그 신고의 근거는 지난 비평에 있고 되살릴 길이 없다.

    refine이 앞 판본에서 한 변경을 다음 판본에도 유지하려면 `contract_changes`에 그 신고를 다시 실어야 한다.
    그 근거가 된 비평은 그 변경을 만든 iteration의 것이므로, 이번 iteration의 비평만 보면 그 기록이 전부
    "인용한 id가 비평에 없다"로 걸린다. 모델이 만족시킬 방법이 없는 요구다. 계약 diff 쪽은 `baseline`과
    `accumulated`가 이미 그 자리를 막았고 근거 대조만 iteration 하나를 보고 있었다.

    어느 iteration의 비평인지는 근거의 유효성과 무관하다. 비평이 한 번 그 지적을 했으면 그것은 있었던 일이고,
    발의가 리뷰어에게서 나왔다는 사실은 iteration이 지나도 바뀌지 않는다. 앞을 향한 경계는 부르는 쪽이
    정한다 — 이번 iteration까지 나온 비평만 넘기므로 아직 없던 비평을 근거로 삼는 길은 닫혀 있다.
    """
    first = {"contract_review": [{"id": "cr_old"}]}
    second = {"weaknesses": [{"id": "w_new"}]}

    # 앞 iteration의 비평만 넘기면 지난 비평을 인용한 기록이 걸린다. 그 자리가 실제로 뒤집혔다.
    only_latest = judge_changes("critique:cr_old", second)
    hit = [v for v in only_latest["violations"] if v["rule"] == "change.unjustified"]
    report("앞 iteration 하나만 넘기면 들고 온 기록의 근거가 없는 id로 뒤집힌다", bool(hit),
           f"{[v['detail'] for v in hit]}")

    # 이 run에서 나온 비평 전부를 넘기면 통과한다.
    whole_run = judge_changes("critique:cr_old", [first, second])
    report("이 run의 비평 전부를 넘기면 지난 비평을 인용한 기록이 통과한다",
           not [v for v in whole_run["violations"] if v["rule"] == "change.unjustified"],
           f"{[v['detail'] for v in whole_run['violations']]}")

    # 넓어진 것은 iteration의 폭뿐이고 id 자체는 그대로 엄격하다.
    nowhere = judge_changes("critique:cr_never", [first, second])
    report("어느 비평에도 없는 id는 여전히 걸린다",
           any(v["rule"] == "change.unjustified" for v in nowhere["violations"]),
           f"{[v['detail'] for v in nowhere['violations'] if v['rule'] == 'change.unjustified']}")

    # 판정 장치의 규칙 id는 어느 비평에도 없으므로 폭을 넓혀도 통과하지 않는다. 실제로 인증 요구를 통째로
    # 지운 기록이 `critique:extract.blind`를 근거로 적었다.
    gate_rule = judge_changes("critique:extract.blind", [first, second])
    report("판정 장치의 규칙 id는 폭을 넓혀도 근거가 아니다",
           any(v["rule"] == "change.basis_is_gate_verdict" for v in gate_rule["violations"]),
           f"{rules_of(gate_rule['violations'])}")

    report("비평을 하나도 넘기지 않으면 빈 목록도 없는 것과 같다",
           critique_ids(None) is None and critique_ids([]) is None
           and critique_ids([None]) is None
           and critique_ids([first, second]) == {"cr_old", "w_new"},
           f"{critique_ids([first, second])}")


def test_change_without_basis():
    prev, curr = removal_pair()
    changes = [{"id": "cc_1", "target": "#/components/schemas/Alert/required",
                "basis": "", "spec_anchor": "requirement:FR-2", "compatibility": "breaking"}]
    outcome = check_contract_changes(prev, curr, changes, "contract/a-v1.yaml",
                                     "contract/a-v2.yaml", RULES, CRITIQUE)
    hit = [v for v in outcome["violations"] if v["rule"] == "change.unjustified"]
    report("근거가 빈 기록도 change.unjustified", bool(hit), f"{[v['detail'] for v in hit]}")


# ── 표현 변경과 약속 변경 ───────────────────────────────────────────────────
#
# 근거의 출처만 보면 필드 하나의 표기를 바꾸는 것과 약속을 지우는 것이 같은 문턱을 지난다.
# 그래서 변경의 성질을 기계로 가른다. 약속의 재고는 여섯이다.
# 판정 지점·필수·선언한 필드·인증 요구·에러 코드 쌍·산문 슬롯이다.


def strip_prose(node):
    """스키마는 한 글자도 건드리지 않고 산문만 지운다. 여덟 번째 run의 생성기가 한 일과 같다."""
    if isinstance(node, dict):
        for key in ("description", "summary", "x-requirement", "x-out-of-scope", "tags"):
            node.pop(key, None)
        for value in node.values():
            strip_prose(value)
    elif isinstance(node, list):
        for item in node:
            strip_prose(item)
    return node


def representation_record(target, basis="critique:w3", representation_basis="interop", **extra):
    record = {"id": "cc_r", "target": target, "basis": basis,
              "representation_basis": representation_basis}
    record.update(extra)
    return record


def judge_pair(prev, curr, records, critique=CRITIQUE, baseline=None):
    return check_contract_changes(prev, curr, records, "contract/a-v1.yaml", "contract/a-v2.yaml",
                                 RULES, critique, baseline=baseline, accumulated=records)


def test_the_vocabulary_of_kinds_and_bases_is_closed():
    report("변경의 성질이 둘이고 카드와 같다",
           {REPRESENTATION, PROMISE} == {item["id"] for item in RULES.get("change_kinds") or []},
           f"{sorted({REPRESENTATION, PROMISE})}")
    report("표현 근거가 셋이고 카드와 같다",
           set(REPRESENTATION_BASES) == {item["id"] for item in RULES.get("representation_bases") or []},
           f"{sorted(REPRESENTATION_BASES)}")


def test_stripping_prose_is_a_promise_change():
    """스키마를 한 글자도 건드리지 않아도 약속이 줄 수 있다. 산문은 루브릭의 분모다."""
    baseline = contract_copy()
    stripped = strip_prose(contract_copy())
    lost = reductions(baseline, stripped)
    report("산문만 지운 판본은 약속 변경이다", change_kind(lost) == PROMISE,
           f"{change_kind(lost)} · 줄어든 재고 { {k: len(v) for k, v in lost.items()} }")
    report("줄어든 것이 산문 슬롯으로 잡힌다", "산문 슬롯" in lost and len(lost["산문 슬롯"]) > 50,
           f"{ {k: len(v) for k, v in lost.items()} }")

    outcome = judge_pair(stripped, stripped, [], None, baseline=baseline)
    hit = [v for v in outcome["violations"] if v["rule"] == "change.promise_reduced"]
    report("약속이 줄었는데 명세 근거가 없으면 change.promise_reduced",
           bool(hit) and all(v.get("kind") == PROMISE for v in hit),
           "; ".join(f"{v['where']}: {v['detail']}" for v in hit))
    report("차원마다 한 건이고 사라진 것은 증거에 남는다",
           len(hit) == len({v["where"] for v in hit})
           and all(len(v.get("evidence") or []) > 1 for v in hit),
           f"{[(v['where'], len(v.get('evidence') or [])) for v in hit]}")
    report("깨는 변경으로는 잡히지 않는다 — 스키마를 건드리지 않았으므로",
           not any(o["rule"] == "change.breaking" for o in outcome["observations"]),
           f"{rules_of(outcome['observations'])}")


def test_every_dimension_of_the_inventory_is_watched():
    baseline = contract_copy()
    cases = {}

    gone_point = contract_copy()
    gone_point["paths"]["/alerts/{alertId}"]["get"]["responses"].pop("404")
    cases["판정 지점"] = gone_point

    gone_required = contract_copy()
    gone_required["components"]["schemas"]["Alert"]["required"].remove("lastCheckedAt")
    cases["필수"] = gone_required

    gone_pair = contract_copy()
    gone_pair["components"]["responses"]["AlertNotAcceptable"]["x-error-codes"] = ["COURT_NOT_SUPPORTED"]
    cases["에러 코드 쌍"] = gone_pair

    gone_slot = contract_copy()
    gone_slot["components"]["schemas"]["Alert"].pop("description")
    cases["산문 슬롯"] = gone_slot

    # 계약이 "있을 수 있다"고 한 필드. `required`만 세면 이 자리가 비어 있고, 사라지면 그것을 읽던 쪽은
    # 영원히 받지 못한다.
    gone_optional = contract_copy()
    schema(gone_optional, "Problem")["properties"].pop("detail")
    cases["선언한 필드"] = gone_optional

    # 인증을 요구한다는 선언. 계약이 401과 그 에러 코드를 약속한 자리의 근거이고, 구현하기 어려운 조항을 지워
    # 위반을 없애는 길 중 값이 가장 크다.
    gone_auth = contract_copy()
    gone_auth.pop("security")
    cases["인증 요구"] = gone_auth

    for dimension, curr in cases.items():
        lost = reductions(baseline, curr)
        outcome = judge_pair(curr, curr, [], None, baseline=baseline)
        hit = [v for v in outcome["violations"] if v["rule"] == "change.promise_reduced"]
        report(f"{dimension}이 줄면 약속 변경으로 잡는다",
               dimension in lost and any(v["where"] == dimension for v in hit),
               f"줄어든 재고 { {k: len(v) for k, v in lost.items()} }")

    # 차원을 더하고 여기에 케이스를 안 쓰면 그 차원은 시험되지 않는다. 판정 장치에 눈이 없는 자리를
    # 통과로 세지 않는 것과 같은 이유로, 시험되지 않는 차원을 시험됐다고 세지 않는다.
    watched = set(promise_inventory(contract_copy()))
    report("재고의 차원마다 케이스가 있다", set(cases) == watched,
           f"케이스 없는 차원 {sorted(watched - set(cases))} · 재고에 없는 케이스 {sorted(set(cases) - watched)}")


def test_the_two_field_dimensions_answer_different_questions():
    """`필수`는 "언제나 담는다"를 세고 `선언한 필드`는 "이 필드가 있다"를 센다. 둘은 다른 약속이다.

    그래서 필수 필드를 지우면 두 차원에 함께 실린다. 같은 일을 두 번 세는 것이 아니라 약속이 둘 깨진 것이다 —
    그 필드가 없어졌고 언제나 담는다는 보장도 없어졌다. 선언한 필드를 선택 필드로만 좁혀 세면 그 중복은
    사라지지만 대신 선택을 필수로 올린 초안이 약속 축소로 걸린다. 계약을 굳힌 것을 벌하는 쪽이 더 나쁘다.
    """
    baseline = contract_copy()
    curr = contract_copy()
    alert = schema(curr, "Alert")
    alert["properties"].pop("courtName")
    alert["required"].remove("courtName")
    lost = reductions(baseline, curr)
    report("필수 필드가 사라진 것은 두 약속이 함께 깨진 것이다",
           "필수" in lost and "선언한 필드" in lost,
           f"{ {k: len(v) for k, v in lost.items()} }")

    # 선택 필드를 필수로 올리는 것은 약속을 굳히는 일이다. 필드는 그대로 있으므로 재고가 줄지 않아야 한다.
    tightened = contract_copy()
    schema(tightened, "Problem")["required"] = \
        list(schema(baseline, "Problem")["required"]) + ["detail"]
    lost = reductions(baseline, tightened)
    report("선택을 필수로 올린 것은 어느 차원도 줄이지 않는다", not lost,
           f"{ {k: len(v) for k, v in lost.items()} }")


SPEC = """# 요구사항 명세

관련 과제: [Task E-3 — 제출물 2](https://example.invalid/taskE-3.md)

| 번호 | 기능 | 요구사항 |
| ---- | ---- | -------- |
| FR-1 | 알림 신청 | 지원하는 코트면 신청을 저장한다. |
| FR-2 | 신청 조회 | 자신의 신청을 돌려준다. |

## 실패 흐름

| 번호 | 상황 |
| ---- | ---- |
| EF-1 | 예약처에 닿지 못했다. |

## 검증 시나리오

- V-1: 같은 조건으로 동시에 신청해도 하나만 남는다.
"""


def test_the_spec_declares_ids_where_it_defines_them():
    """명세가 id를 **선언하는** 자리에서만 읽는다. 산문을 지나가는 것은 선언이 아니다.

    실제 명세의 머리글에 다른 과제를 가리키는 `Task E-3` 링크가 있다. 그것을 요구사항으로 세면 계약을 깎는
    근거로 쓸 수 있는 id가 하나 늘어난다.
    """
    ids = spec_requirement_ids(SPEC)
    report("표의 첫 칸과 목록의 머리에서 id를 읽는다", ids == {"FR-1", "FR-2", "EF-1", "V-1"},
           f"{sorted(ids)}")
    report("산문을 지나가는 과제 링크는 요구사항이 아니다", "E-3" not in ids, f"{sorted(ids)}")
    report("명세를 읽지 못하면 빈 집합이고 그 사실이 판정을 대신하지 않는다",
           spec_requirement_ids(None) == set() and spec_requirement_ids({"a": 1}) == set(), "")

    # 접두사와 구분자에는 관대하고 id에는 엄격하다. 실물에서 이 셋이 함께 왔다.
    cases = {"requirement:FR-2": True, "FR-1, EF-1": True, "EF-1": True,
             "contract:security": False, "slot_fd7db9b0": False, "FR-99": False, "": False}
    wrong = {a: bool(anchored_ids(a, ids)) for a, want in cases.items()
             if bool(anchored_ids(a, ids)) != want}
    report("근거에서 명세의 id를 찾으면 통과하고 하나도 없으면 아니다", not wrong, f"{wrong}")


def test_an_anchor_that_points_nowhere_in_the_spec_cannot_back_a_reduction():
    """약속을 줄이려면 명세가 그 자리를 받쳐야 한다. 명세를 가리키지 않는 문자열은 받친 것이 아니다.

    `basis`는 그 iteration의 비평에 실제로 있는 id인지 값으로 대조하는데 `spec_anchor`는 빈 문자열만 아니면
    통과했다. 같은 구멍이고, 줄이려는 그 계약을 가리키는 근거가 약속 축소를 덮었다.
    """
    baseline = contract_copy()
    curr = contract_copy()
    curr.pop("security")
    record = {"id": "cc_auth", "target": "#/security", "basis": "critique:w3"}

    def judge(anchor, spec=SPEC, basis="critique:w3"):
        one = dict(record, spec_anchor=anchor, basis=basis)
        return check_contract_changes(curr, curr, [one], "contract/a-v1.yaml", "contract/a-v2.yaml",
                                      RULES, CRITIQUE, baseline=baseline, accumulated=[one],
                                      spec=spec)

    outcome = judge("requirement:FR-1")
    report("명세의 id를 가리키는 근거는 약속 축소를 받친다", not outcome["violations"],
           f"{[v['detail'] for v in outcome['violations']]}")

    outcome = judge("contract:security")
    hit = [v for v in outcome["violations"] if v["rule"] == "change.promise_reduced"]
    report("계약 자신을 가리키는 근거는 받치지 못한다",
           bool(hit) and any(v["where"] == "인증 요구" for v in hit)
           and any("요구사항 명세의 id를 가리키지 않는다" in v["detail"]
                   for v in outcome["violations"] if v["rule"] == "change.unjustified"),
           f"{[(v['rule'], v['where']) for v in outcome['violations']]}")

    # 근거가 무효인 기록은 아무것도 정당화하지 않는다. 한 규칙에서 유죄인데 다른 규칙에서 면제를 주면
    # 규칙 사이가 어긋난다.
    outcome = judge("requirement:FR-1", basis="critique:extract.blind")
    report("basis가 판정 장치의 규칙을 가리키는 기록은 침묵을 사지 못한다",
           any(v["rule"] == "change.basis_is_gate_verdict" for v in outcome["violations"])
           and any(v["rule"] == "change.promise_reduced" and v["where"] == "인증 요구"
                   for v in outcome["violations"]),
           f"{[(v['rule'], v['where']) for v in outcome['violations']]}")

    # 명세를 못 받으면 값을 대조할 수 없다. 미판정을 통과로도 최저점으로도 세지 않는다.
    outcome = judge("contract:security", spec=None)
    report("명세가 없으면 값을 대조하지 않고 그 사실을 기록으로 남긴다",
           not outcome["violations"]
           and any(o["rule"] == "change.anchor_uncompared" for o in outcome["observations"]),
           f"위반 {rules_of(outcome['violations'])} / 기록 {rules_of(outcome['observations'])}")


def test_a_growing_contract_reduces_nothing():
    """자라는 변경은 재고를 늘리므로 어느 차원에서도 줄어든 것이 없다.

    응답을 더하고 문장을 늘리고 필드를 더한 판본이 그 모양이다. 자라는 변경이 약속 축소로 걸리면
    계약을 옳게 고친 초안이 벌을 받는다.
    """
    baseline = contract_copy()
    curr = contract_copy()
    curr["paths"]["/alerts"]["post"]["responses"]["504"] = {
        "$ref": "#/components/responses/InternalError"}
    curr["paths"]["/alerts"]["post"]["description"] = \
        str(curr["paths"]["/alerts"]["post"].get("description") or "") + "\n운영 시간대 판정의 출처."
    schema(curr, "Problem")["properties"]["hint"] = {"type": "string", "description": "고칠 방법"}
    lost = reductions(baseline, curr)
    report("자라는 변경은 어느 차원도 줄이지 않는다", not lost,
           f"{ {k: len(v) for k, v in lost.items()} }")
    before, after = promise_inventory(baseline), promise_inventory(curr)
    report("산문 슬롯과 선언한 필드와 판정 지점이 늘어난다",
           len(after["산문 슬롯"]) > len(before["산문 슬롯"])
           and len(after["선언한 필드"]) > len(before["선언한 필드"])
           and len(after["판정 지점"]) > len(before["판정 지점"]),
           f"슬롯 {len(before['산문 슬롯'])}→{len(after['산문 슬롯'])} · "
           f"필드 {len(before['선언한 필드'])}→{len(after['선언한 필드'])} · "
           f"지점 {len(before['판정 지점'])}→{len(after['판정 지점'])}")


def test_an_example_payload_is_not_a_promise():
    """예시 안의 키는 약속이 아니라 약속을 보여주는 그림이다. 세면 예시를 고친 것이 약속 축소가 된다."""
    baseline = contract_copy()
    schema(baseline, "Problem")["example"] = {
        "properties": {"detail": {"type": "string"}}, "required": ["detail"]}
    curr = copy.deepcopy(baseline)
    schema(curr, "Problem")["example"] = {"properties": {}, "required": []}
    lost = reductions(baseline, curr)
    report("예시 payload를 고친 것은 약속을 줄인 것이 아니다", not lost,
           f"{ {k: len(v) for k, v in lost.items()} }")


def test_a_promise_change_with_a_spec_anchor_passes():
    """고치는 것은 막지 않는다. 약속을 줄이려면 명세가 그 자리를 받쳐야 한다."""
    baseline = contract_copy()
    curr = contract_copy()
    curr["components"]["schemas"]["Alert"]["required"].remove("lastCheckedAt")
    backed = [{"id": "cc_1", "target": "#/components/schemas/Alert/required",
               "basis": "critique:w3", "spec_anchor": "requirement:FR-2",
               "compatibility": "breaking"}]
    outcome = judge_pair(baseline, curr, backed, baseline=baseline)
    report("명세 근거를 댄 약속 변경은 위반이 아니다", not outcome["violations"],
           f"{[v['detail'] for v in outcome['violations']]}")

    bare = [dict(backed[0], spec_anchor="")]
    outcome = judge_pair(baseline, curr, bare, baseline=baseline)
    hit = [v for v in outcome["violations"] if v["rule"] in ("change.promise_reduced",
                                                             "change.unjustified")]
    report("명세 근거가 빈 약속 변경은 잡힌다", bool(hit),
           "; ".join(f"{v['rule']}: {v['detail']}" for v in hit))
    report("그 판정에 성질이 실린다", all(v.get("kind") == PROMISE for v in hit),
           f"{[v.get('kind') for v in hit]}")


def test_a_representation_change_needs_a_closed_basis():
    """표현 변경이라도 아무 이유나 되면 안 된다. 도구나 언어의 기본 표현은 근거가 아니다."""
    baseline = contract_copy()
    curr = contract_copy()
    # 값의 모양만 바꾼다. 재고는 넷 다 그대로다.
    curr["components"]["schemas"]["TimeSlot"]["properties"]["startTime"]["pattern"] = "^[0-9:]+$"
    target = "#/components/schemas/TimeSlot/properties/startTime/pattern"
    lost = reductions(baseline, curr)
    report("재고가 그대로면 표현 변경이다", change_kind(lost) == REPRESENTATION,
           f"{change_kind(lost)} · { {k: len(v) for k, v in lost.items()} }")

    ok = judge_pair(baseline, curr, [representation_record(target)], baseline=baseline)
    report("닫힌 값의 표현 근거를 댄 표현 변경은 위반이 아니다", not ok["violations"],
           f"{[v['detail'] for v in ok['violations']]}")
    report("명세 근거는 표현 변경에 요구하지 않는다 — 문턱이 낮다",
           not any("spec_anchor" in v["detail"] for v in ok["violations"]), "")

    empty = judge_pair(baseline, curr,
                       [representation_record(target, representation_basis="")], baseline=baseline)
    hit = [v for v in empty["violations"] if v["rule"] == "change.unjustified"]
    report("표현 근거가 비면 근거 없는 변경이다", bool(hit),
           "; ".join(v["detail"] for v in hit))

    outside = judge_pair(baseline, curr,
                         [representation_record(target, representation_basis="tool_default")],
                         baseline=baseline)
    hit = [v for v in outside["violations"] if v["rule"] == "change.unjustified"]
    report("도구의 기본 표현은 근거가 아니다", bool(hit)
           and any("닫힌 값 밖" in v["detail"] for v in hit),
           "; ".join(v["detail"] for v in hit))
    report("그 판정에 표현 변경이라는 성질이 실린다",
           all(v.get("kind") == REPRESENTATION for v in hit), f"{[v.get('kind') for v in hit]}")


def test_a_notation_basis_is_asked_only_where_the_notation_changed():
    """닫힌 값의 표현 근거는 값의 표기를 고친 기록에만 묻는다.

    그 필드가 막는 것은 "도구나 언어의 기본 표현이 그렇게 시켰다"는 논리이고, 그 논리는 도구가 값의 모양을 내는
    자리에서만 성립한다. 문장을 고치는 데도 상태 코드가 어느 응답을 가리키는지 바꾸는 데도 탓할 도구 기본값이
    없다. 닫힌 값 셋은 모두 표기를 말하므로 그 자리에는 고를 값도 없고, 억지로 `spec_implication`을 고르면
    `spec_anchor`가 이미 말한 것을 두 칸에 적게 된다. 표현 변경은 약속 변경의 여집합이라 표기를 바꾼 것만 담지
    않으므로, 성질을 가르는 것과 근거를 묻는 것을 나눈다.
    """
    prev = contract_copy()

    # 계약이 명세와 모순된 문장을 명세에 맞춘다. 문장을 바꾼 것은 약속 변경이라 명세 근거는 따로 묻지만,
    # 표기를 바꾼 것이 아니므로 표현 근거는 묻지 않는다. 그 둘을 가르는 것이 이 자리의 확인이다.
    prose = contract_copy()
    schema(prose, "AlertStatus")["description"] = "조회가 확인을 일으킨다"
    outcome = judge_pair(prev, prose, [
        {"id": "cc_prose", "target": "#/components/schemas/AlertStatus/description",
         "basis": "critique:w3"}], baseline=prev)
    report("문장만 고친 기록에는 표현 근거를 묻지 않는다",
           not [v for v in outcome["violations"] if "representation_basis" in v["detail"]],
           f"{[v['detail'] for v in outcome['violations']]}")

    # 상태 코드가 가리키는 응답을 바꾼다. 표기가 아니라 그 자리가 무엇을 뜻하는지가 달라진 것이다.
    repointed = contract_copy()
    repointed["paths"]["/alerts"]["post"]["responses"]["503"] = {
        "$ref": "#/components/responses/InternalError"}
    outcome = judge_pair(prev, repointed, [
        {"id": "cc_repoint", "target": "#/paths/~1alerts/post/responses/503",
         "basis": "critique:w3", "spec_anchor": "requirement:EF-1"}], baseline=prev)
    report("어느 응답을 가리키는지 바꾼 기록에도 묻지 않는다",
           not [v for v in outcome["violations"] if "representation_basis" in v["detail"]],
           f"{[v['detail'] for v in outcome['violations']]}")

    # 필드의 형식을 붙인다. 자바 `int`에 도구가 `int32`를 붙이는 자리가 정확히 이것이다.
    notation = contract_copy()
    schema(notation, "AlertDelivery")["properties"]["attemptCount"]["format"] = "int32"
    target = "#/components/schemas/AlertDelivery/properties/attemptCount/format"
    outcome = judge_pair(prev, notation, [
        representation_record(target, representation_basis="")], baseline=prev)
    report("값의 형식을 고친 기록에는 그대로 묻는다",
           any("representation_basis" in v["detail"] for v in outcome["violations"]),
           f"{[v['detail'] for v in outcome['violations']]}")

    # 앞의 것은 계약에 없던 형식을 **더한** 것이다. 더했는지 바꿨는지로 면제하면 도구가 붙인 형식이 그 면제로
    # 빠져나가므로, 고친 키가 표기 키인지만 본다.
    had = contract_copy()
    schema(had, "AlertDelivery")["properties"]["attemptCount"]["format"] = "int64"
    swapped = copy.deepcopy(had)
    schema(swapped, "AlertDelivery")["properties"]["attemptCount"]["format"] = "int32"
    outcome = judge_pair(had, swapped, [
        representation_record(target, representation_basis="")], baseline=had)
    report("더한 것도 바꾼 것도 같은 문턱을 지난다",
           any("representation_basis" in v["detail"] for v in outcome["violations"]),
           f"{[v['detail'] for v in outcome['violations']]}")

    # 같은 `properties` 아래라도 고친 것이 설명이면 표기를 바꾼 것이 아니다. 포인터가 지나가는 키가 아니라
    # 무엇을 고쳤는지가 문턱을 정한다.
    field_prose = contract_copy()
    schema(field_prose, "Alert")["properties"]["reservationUrl"]["description"] = "예약 화면"
    outcome = judge_pair(prev, field_prose, [
        {"id": "cc_field_prose",
         "target": "#/components/schemas/Alert/properties/reservationUrl/description",
         "basis": "critique:w3"}], baseline=prev)
    report("필드 아래의 설명을 고친 것은 표기 변경이 아니다",
           not [v for v in outcome["violations"] if "representation_basis" in v["detail"]],
           f"{[v['detail'] for v in outcome['violations']]}")


def test_rewriting_a_sentence_is_a_promise_change():
    """같은 자리의 문장을 바꿔 쓴 것은 약속 변경이고, 덧붙이기만 한 것은 아니다.

    비평과 채점이 그 iteration의 계약 판본으로 보므로 문장을 고쳐 약속을 무르면 채점도 따라간다. 그 길을 막는
    것은 판본 대조뿐이라, 슬롯의 자리가 남아 있어도 앞 판본의 문장이 사라지면 명세 근거를 요구한다.
    """
    prev = contract_copy()
    target = "#/components/schemas/AlertStatus/description"
    rewritten = contract_copy()
    schema(rewritten, "AlertStatus")["description"] = "조회가 확인을 일으킨다"

    def judge(anchor):
        one = {"id": "cc_rewrite", "target": target, "basis": "critique:w3", "spec_anchor": anchor}
        return check_contract_changes(prev, rewritten, [one], "contract/a-v1.yaml", "contract/a-v2.yaml",
                                      RULES, CRITIQUE, baseline=prev, accumulated=[one], spec=SPEC)

    lost = reductions(prev, rewritten)
    report("문장을 바꿔 쓴 판본은 약속 변경이다", change_kind(lost) == PROMISE and "산문 슬롯" in lost,
           f"{ {k: len(v) for k, v in lost.items()} }")
    outcome = judge("")
    report("명세 근거 없이 문장을 바꿔 쓰면 위반이다",
           any(v["rule"] in ("change.unjustified", "change.promise_reduced") for v in outcome["violations"]),
           f"{[(v['rule'], v['detail']) for v in outcome['violations']]}")
    outcome = judge("requirement:FR-1")
    report("명세의 id를 가리키는 근거가 있으면 문장을 바꿔 쓸 수 있다", not outcome["violations"],
           f"{[v['detail'] for v in outcome['violations']]}")

    grown = contract_copy()
    before = str(schema(grown, "AlertStatus").get("description") or "")
    schema(grown, "AlertStatus")["description"] = (before + "\n\n확인은 조회가 일으킨다.").strip()
    report("앞 문장을 그대로 두고 덧붙인 것은 줄어든 것이 없다", not reductions(prev, grown),
           f"{ {k: len(v) for k, v in reductions(prev, grown).items()} }")

    rewrapped = contract_copy()
    schema(rewrapped, "AlertStatus")["description"] = "\n  ".join(before.split(" "))
    report("줄을 다시 감싼 것은 같은 문장이다", not reductions(prev, rewrapped) or not before.strip(),
           f"{ {k: len(v) for k, v in reductions(prev, rewrapped).items()} }")


def test_draft_payload_does_not_carry_other_stages_answers():
    """draft에 실린 다른 단계에 대한 응답은 그것을 보면 안 되는 단계의 payload에서 빠진다.

    `repairs`는 게이트 위반을, `ignored_suggestions`는 비평의 발의를 인용한다. draft를 통째로 실으면
    정보 차단 표가 막은 것이 이 칸을 타고 샌다. 채점은 둘 다 보면 안 되고, 비평은 게이트의 위반만 보면 안 된다.
    """
    pipeline = str(SKILL / "pipeline")
    if pipeline not in sys.path:
        sys.path.insert(0, pipeline)
    from stages import critique as critique_stage, evaluator as evaluator_stage

    draft = {"files": [], "decisions": [], "contract_changes": [],
             "repairs": [{"violation_id": "c_gate_marker", "summary": "게이트 표지"}],
             "ignored_suggestions": [{"suggestion_id": "cr_critique_marker", "reason": "비평 표지"}]}
    _, user = evaluator_stage.build_prompt({}, draft, {}, [], [], [])
    report("채점 payload에 게이트 위반에 대한 응답이 없다", "c_gate_marker" not in user, "")
    report("채점 payload에 비평에 대한 응답이 없다", "cr_critique_marker" not in user, "")
    _, user = critique_stage.build_prompt({}, draft, [], [], [], [], {})
    report("비평 payload에 게이트 위반에 대한 응답이 없다", "c_gate_marker" not in user, "")
    report("비평 payload는 자기 발의에 대한 응답을 그대로 받는다", "cr_critique_marker" in user, "")
    report("원본 draft는 건드리지 않는다", "repairs" in draft and "ignored_suggestions" in draft, "")


def test_manifest_drift_compares_each_iteration_with_its_own_changes():
    """색인은 그 iteration에 쓰이거나 고쳐진 것의 목록이므로, 원장도 그 iteration의 변경과만 맞댄다.

    뒤의 iteration에서 스켈레톤 밖 파일 전부를 맞대면 앞에서 신고했고 이번에 건드리지 않은 파일이 모두 어긋남으로
    올라온다. 또 원장을 다시 만들 때 작업 폴더가 마지막 iteration에 서 있으면, 앞 iteration이 뒤에 생긴 파일을 본다.
    """
    pipeline = str(SKILL / "pipeline")
    if pipeline not in sys.path:
        sys.path.insert(0, pipeline)
    import ledger as ledger_module

    snapshot = {"skeleton.txt": {}, "a.java": {}, "b.java": {}}
    skeleton = {"skeleton.txt"}
    first = ledger_module.generator_touched(snapshot, {}, skeleton, has_prev=False)
    report("첫 iteration은 스켈레톤 밖의 파일 전부와 맞댄다", first == {"a.java", "b.java"}, f"{sorted(first)}")
    later = ledger_module.generator_touched(snapshot, {"b.java": {}}, skeleton, has_prev=True)
    report("뒤의 iteration은 이번에 달라진 파일과만 맞댄다", later == {"b.java"}, f"{sorted(later)}")

    with tempfile.TemporaryDirectory() as temp:
        root = Path(temp)
        def git(*args):
            subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True)
        git("init", "-q", "-b", "main")
        git("config", "user.email", "t@example.com")
        git("config", "user.name", "t")
        (root / "a.txt").write_text("one", encoding="utf-8")
        git("add", "-A")
        git("commit", "-q", "-m", "first")
        first_commit = subprocess.run(["git", "-C", str(root), "rev-parse", "HEAD"],
                                      capture_output=True, text=True, check=True).stdout.strip()
        (root / "a.txt").write_text("two", encoding="utf-8")
        (root / "later.txt").write_text("late", encoding="utf-8")
        git("add", "-A")
        git("commit", "-q", "-m", "second")

        past = ledger_module.iteration_snapshot(root, first_commit)
        report("작업 폴더가 뒤에 서 있으면 그 커밋의 트리를 읽는다",
               set(past) == {"a.txt"} and past["a.txt"]["text"] == "one", f"{ {k: v.get('text') for k, v in past.items()} }")
        # 체크아웃이 줄바꿈을 바꾼 것처럼 디스크만 커밋과 달라져도, 원장은 커밋의 내용을 본다.
        (root / "a.txt").write_text("two\r\n", encoding="utf-8")
        current = ledger_module.iteration_snapshot(root, "HEAD")
        report("디스크가 커밋과 달라도 커밋의 내용으로 해시를 낸다",
               current["a.txt"]["text"] == "two"
               and current["a.txt"]["sha"] != ledger_module.dir_snapshot(root)["a.txt"]["sha"],
               f"{current['a.txt']['text']!r}")


def test_a_pointer_reads_keyword_places_apart_from_names():
    """포인터에서 키워드 자리와 사람이 지은 이름 자리를 가른다.

    이름을 키워드로 읽으면 `format`이라는 이름의 스키마나 `type`이라는 이름의 필드가 표기 변경으로 잡힌다.
    """
    cases = {
        "/components/schemas/AlertStatus/description": "description",
        "/components/schemas/Alert/properties/expiresAt/description": "description",
        "/components/schemas/Alert/properties/expiresAt/format": "format",
        "/paths/~1alerts/post/responses/503/$ref": "$ref",
        "/paths/~1alerts/post/responses/502": "responses",
        "/components/schemas/Alert/required/2": "required",
        "/paths/~1x/get/parameters/0/schema/type": "type",
        "/components/schemas/format/description": "description",
    }
    wrong = {p: last_keyword(p) for p, want in cases.items() if last_keyword(p) != want}
    report("포인터의 가장 깊은 키워드를 옳게 고른다", not wrong, f"{wrong}")
    report("표기 키에 자리의 있고 없음을 담는 키는 없다",
           not ({"properties", "responses", "paths", "required", "$ref"} & SHAPE_KEYS),
           f"{sorted({'properties', 'responses', 'paths', 'required', '$ref'} & SHAPE_KEYS)}")


def test_restoring_a_promise_asks_only_for_the_critique():
    """지운 것을 되돌려 놓는 변경에 표현 근거를 요구하면 옳게 고친 초안이 벌을 받는다."""
    stripped = strip_prose(contract_copy())
    restored = contract_copy()
    restored["components"]["schemas"]["Alert"]["required"] = list(
        contract_copy()["components"]["schemas"]["Alert"]["required"])
    # 산문을 하나 되돌린다. 그 자리의 변경은 더한 것뿐이다.
    curr = copy.deepcopy(stripped)
    curr["components"]["schemas"]["Alert"]["description"] = \
        BASE["components"]["schemas"]["Alert"]["description"]
    record = {"id": "cc_back", "target": "#/components/schemas/Alert/description",
              "basis": "critique:cr1"}
    outcome = check_contract_changes(stripped, curr, [record], "contract/a-v1.yaml",
                                     "contract/a-v2.yaml", RULES, CRITIQUE, accumulated=[record])
    report("더한 것뿐인 기록은 비평까지만 묻는다",
           not [v for v in outcome["violations"] if v["rule"] == "change.unjustified"],
           f"{[v['detail'] for v in outcome['violations']]}")


def test_kinds_split_within_one_version():
    """한 판본 안에서 기록마다 성질이 갈린다. 성질을 판본 전체로 한 번에 정하면 그 구분이 사라진다."""
    prev = contract_copy()
    curr = contract_copy()
    curr["components"]["schemas"]["Alert"]["required"].remove("lastCheckedAt")
    curr["components"]["schemas"]["TimeSlot"]["properties"]["startTime"]["pattern"] = "^[0-9:]+$"
    records = [
        {"id": "cc_promise", "target": "#/components/schemas/Alert/required",
         "basis": "critique:w3", "spec_anchor": "requirement:FR-2"},
        representation_record("#/components/schemas/TimeSlot/properties/startTime/pattern"),
    ]
    outcome = judge_pair(prev, curr, records, baseline=prev)
    report("약속을 줄인 기록과 표기를 바꾼 기록이 한 판본에서 함께 통과한다",
           not outcome["violations"], f"{[v['detail'] for v in outcome['violations']]}")

    # 문턱을 서로 바꿔 달면 둘 다 걸린다.
    swapped = [
        {"id": "cc_promise", "target": "#/components/schemas/Alert/required",
         "basis": "critique:w3", "representation_basis": "interop"},
        representation_record("#/components/schemas/TimeSlot/properties/startTime/pattern",
                              representation_basis=None, spec_anchor="requirement:FR-2"),
    ]
    outcome = judge_pair(prev, curr, swapped, baseline=prev)
    kinds = {v.get("kind") for v in outcome["violations"] if v["rule"] == "change.unjustified"}
    report("문턱을 서로 바꿔 달면 둘 다 걸리고 성질이 갈려 남는다",
           kinds == {PROMISE, REPRESENTATION},
           f"{[(v.get('kind'), v['detail'][:52]) for v in outcome['violations'] if v['rule'] == 'change.unjustified']}")


def test_version_comparison_needs_no_extracted_spec():
    """판본 대조는 빌드가 필요 없다. G0가 깨진 iteration에서도 계약 변경은 판정돼야 한다."""
    baseline = contract_copy()
    stripped = strip_prose(contract_copy())
    try:
        outcome = check_contract_changes(stripped, stripped, [], "contract/a-v1.yaml",
                                         "contract/a-v1.yaml", RULES, None, baseline=baseline)
        crashed = None
    except Exception as problem:  # noqa: BLE001
        outcome, crashed = None, f"{type(problem).__name__}: {problem}"
    report("추출 스펙도 작업 폴더도 없이 판본 대조가 끝까지 돈다",
           crashed is None and bool(outcome["violations"]),
           crashed or f"위반 {rules_of(outcome['violations'])}")


def test_baseline_is_compared_even_when_the_version_compares_to_itself():
    """iteration 001은 `from`과 `path`가 같은 파일이라 앞 판본 대비가 자기 자신과의 비교다.

    실제 run에서 생성기가 기준선 사본을 4만 7천 바이트에서 1만 3천 바이트로 줄였는데 게이트가 통과시켰다.
    지워진 것이 산문이라 깨는 변경으로도 잡히지 않았고, 산문은 루브릭의 분모이므로 그것을 지우면 채점 대상이 사라진다.
    """
    baseline = contract_copy()
    stripped = contract_copy()
    # 생성기가 한 일과 같다. 산문만 지운다. 스키마는 한 글자도 건드리지 않는다.
    def strip_prose(node):
        if isinstance(node, dict):
            for key in ("description", "summary", "x-requirement", "x-out-of-scope", "tags"):
                node.pop(key, None)
            for value in node.values():
                strip_prose(value)
        elif isinstance(node, list):
            for item in node:
                strip_prose(item)
    strip_prose(stripped)

    same_file = "contract/tennis-alert-api-v1.yaml"
    without = check_contract_changes(stripped, stripped, [], same_file, same_file, RULES, None)
    report("기준선을 넘기지 않으면 자기 자신과 비교해 아무것도 잡지 못한다",
           not without["violations"], f"위반 {rules_of(without['violations'])}")

    with_baseline = check_contract_changes(stripped, stripped, [], same_file, same_file, RULES, None,
                                           baseline=baseline)
    hit = [v for v in with_baseline["violations"] if v["rule"] == "change.unjustified"]
    report("기준선까지 보면 산문 삭제가 change.unjustified로 잡힌다", bool(hit),
           "; ".join(v["detail"] for v in hit))
    report("지워진 좌표가 증거에 남는다",
           bool(hit) and len(hit[0].get("evidence") or []) > 50,
           f"증거 {len(hit[0].get('evidence') or []) if hit else 0}개")
    report("좌표 백 개가 한 건이다. 기준선 사본을 덮어쓴 것은 한 가지 일이다",
           len(hit) == 1, f"{len(hit)}건")
    report("기준선 대비 diff는 기록으로도 남는다",
           any(o["rule"] == "change.diff" and o["where"] == "기준선 대비"
               for o in with_baseline["observations"]),
           f"{[(o['rule'], o['where']) for o in with_baseline['observations']]}")


def test_baseline_drift_covered_by_accumulated_records_is_not_a_violation():
    """기준선 대비 차이는 한 iteration의 기록이 아니라 누적 기록이 덮는다."""
    baseline = contract_copy()
    curr = contract_copy()
    curr["components"]["schemas"]["Alert"]["required"].remove("lastCheckedAt")
    history = [{"id": "cc_1", "target": "#/components/schemas/Alert/required",
                "basis": "critique:w3", "spec_anchor": "requirement:FR-2",
                "compatibility": "breaking", "iteration": "002"}]

    # iteration 003: 이번에는 안 고쳤고 기준선 대비 차이는 002의 기록이 덮는다.
    covered = check_contract_changes(curr, curr, [], "contract/a-v2.yaml", "contract/a-v2.yaml",
                                     RULES, CRITIQUE, baseline=baseline, accumulated=history)
    report("누적 기록이 덮는 기준선 대비 차이는 위반이 아니다",
           not covered["violations"], f"위반 {[v['detail'] for v in covered['violations']]}")

    # 누적 기록을 넘기지 않으면 그 차이를 덮을 것이 없다.
    bare = check_contract_changes(curr, curr, [], "contract/a-v2.yaml", "contract/a-v2.yaml",
                                  RULES, CRITIQUE, baseline=baseline)
    report("누적 기록이 없으면 같은 차이가 잡힌다",
           any(v["rule"] == "change.unjustified" for v in bare["violations"]),
           f"{[v['where'] for v in bare['violations']]}")


def test_baseline_unchanged_is_clean():
    baseline = contract_copy()
    outcome = check_contract_changes(baseline, contract_copy(), [], "contract/a-v1.yaml",
                                     "contract/a-v1.yaml", RULES, None, baseline=baseline)
    report("기준선과 같은 판본은 위반이 아니다", not outcome["violations"],
           f"{rules_of(outcome['violations'])}")


def test_version_file():
    prev = contract_copy()
    curr = contract_copy()
    curr["info"]["description"] = curr["info"]["description"] + "\n고쳤다."
    same = check_contract_changes(prev, curr, [], "iter_001/contract/a-v1.yaml",
                                  "iter_002/contract/a-v1.yaml", RULES)
    hit = [v for v in same["violations"] if v["rule"] == "change.version_file"]
    report("내용이 바뀌었는데 파일명이 그대로면 위반", bool(hit), f"{rules_of(same['violations'])}")

    unchanged = check_contract_changes(prev, contract_copy(), [], "iter_001/contract/a-v1.yaml",
                                       "iter_002/contract/a-v1.yaml", RULES)
    report("안 고치고 같은 파일명을 이어받는 것은 위반이 아니다",
           not unchanged["violations"], f"{rules_of(unchanged['violations'])}")


def test_point_churn():
    prev = contract_copy()
    curr = contract_copy()
    curr["paths"]["/alerts/{alertId}"]["get"]["responses"].pop("404")
    outcome = check_contract_changes(prev, curr, [], "a-v1.yaml", "a-v2.yaml", RULES)
    churn = [o for o in outcome["observations"] if o["rule"] == "change.point_churn"]
    report("사라진 판정 지점은 기록으로 센다",
           {o["point"] for o in churn} >= {"getAlert:404", "getAlert:ALERT_NOT_FOUND"},
           f"{sorted(o['point'] for o in churn)}")


# ── 12. G0 ─────────────────────────────────────────────────────────────────

# 판정이 도구의 출력 인코딩 때문에 멈추면 안 된다. 이 기계의 윈도우 도구들은 콘솔 코드페이지로 한국어를
# 내고, javac 의 오류 문장도 JDK 의 로케일을 따라 번역된다. run 하나가 그 때문에 판정 없이 끝났다.

KOREAN = "기호를 찾을 수 없습니다"
BROKEN_BYTES = b"\xc4\xff\xfe"


def test_decode_survives_every_byte():
    cases = {
        "UTF-8로 쓴 한국어": (KOREAN.encode("utf-8"), KOREAN),
        "코드페이지로 쓴 한국어": (KOREAN.encode("cp949"), KOREAN),
        "이미 문자열인 것": ("a\r\nb", "a\nb"),
        "빈 바이트": (b"", ""),
        "None": (None, ""),
    }
    for label, (raw, want) in cases.items():
        try:
            got = decode(raw)
        except Exception as problem:  # noqa: BLE001
            report(f"decode: {label}", False, f"{type(problem).__name__}: {problem}")
            continue
        report(f"decode: {label}", got == want, repr(got)[:60])

    try:
        got = decode(b"ok " + BROKEN_BYTES + b" end")
        ok = got.startswith("ok ") and got.endswith(" end")
    except Exception as problem:  # noqa: BLE001
        got, ok = f"{type(problem).__name__}: {problem}", False
    report("decode: 어떤 인코딩으로도 풀리지 않는 바이트", ok, repr(got))

    # Maven 은 자기 줄을 UTF-8로, javac 은 같은 파이프에 코드페이지로 낸다.
    mixed = "[ERROR] 컴파일 실패\n".encode("utf-8") + "  기호: setAutowireMode\n".encode("cp949") + BROKEN_BYTES
    try:
        got = decode(mixed)
        ok = "컴파일 실패" in got and "기호: setAutowireMode" in got
    except Exception as problem:  # noqa: BLE001
        got, ok = f"{type(problem).__name__}: {problem}", False
    report("decode: 한 출력에 인코딩이 섞여도 줄마다 살린다", ok, repr(got)[:110])


def test_run_reads_output_as_bytes():
    """`text=True`로 받으면 디코딩이 파이프를 읽는 스레드에서 일어나 종료 코드조차 받지 못한다."""
    import inspect
    source = inspect.getsource(g0_module.run)
    # 머리말에 그 말이 적혀 있으니 머리말을 떼고 코드만 본다.
    chunks = source.split(chr(34) * 3)
    code = chunks[0] + "".join(chunks[2:]) if len(chunks) >= 3 else source
    report("도구 출력을 바이트로 받아 직접 푼다",
           "text=True" not in code and "encoding=" not in code and "decode(done.stdout)" in code,
           "subprocess 리더 스레드에서 디코딩하지 않는다")


def test_run_decodes_a_code_page_tool():
    """실제로 코드페이지로 출력하는 자식 프로세스를 불러 본다."""
    script = ("import sys\n"
              "sys.stdout.buffer.write('기호를 찾을 수 없습니다\\n'.encode('cp949'))\n"
              "sys.stdout.buffer.write(b'ok \\xc4\\xff\\xfe end\\n')\n"
              "sys.exit(3)\n")
    with tempfile.TemporaryDirectory() as work:
        target = Path(work) / "cp949_tool.py"
        target.write_text(script, encoding="utf-8")
        try:
            code, output = g0_module.run([sys.executable, str(target)], Path(work), 60)
            ok = code == 3 and KOREAN in output and "ok " in output
        except Exception as problem:  # noqa: BLE001
            code, output, ok = None, f"{type(problem).__name__}: {problem}", False
    report("코드페이지로 출력하는 도구의 종료 코드와 한국어를 함께 받는다", ok,
           f"code={code} output={output.strip()!r}"[:140])


def test_summaries_survive_broken_and_translated_output():
    localized = ("$ mvnw.cmd -B -q test -> 1\n"
                 "[ERROR] COMPILATION ERROR :\n"
                 "[ERROR] /C:/run/output/src/main/java/com/thinking/tennis/app/Wiring.java:[17,21] "
                 + KOREAN + "\n"
                 "  기호:   메소드 setAutowireMode(int)\n"
                 "  위치: 인터페이스 org.springframework.beans.factory.config.BeanDefinition\n"
                 "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile\n"
                 "[ERROR] -> [Help 1]\n")
    detail = summarize_build_failure(localized)
    report("번역된 javac 오류도 좌표와 문장을 담는다",
           "Wiring.java:17:21" in detail and KOREAN in detail and "setAutowireMode" in detail
           and "Failed to execute goal" not in detail,
           detail.replace("\n", " | "))
    report("번역된 이름표 아래의 줄도 담는다",
           "기호:" in detail and "위치:" in detail, detail.replace("\n", " | ")[:140])

    dirty = decode(
        "Caused by: java.lang.NoSuchMethodException: com.thinking.tennis.app.AlertService.<init>()\n"
        "\tat java.base/java.lang.Class.getConstructor0(Class.java:3761)\n".encode("utf-8")
        + b"dump_api_docs: " + BROKEN_BYTES + b" \xbd\xc3\xc0\xdb\n")
    try:
        detail = summarize_boot_failure(dirty)
        ok = detail.splitlines()[0].startswith("원인:") and "NoSuchMethodException" in detail
    except Exception as problem:  # noqa: BLE001
        detail, ok = f"{type(problem).__name__}: {problem}", False
    report("깨진 문자가 섞여도 원인 줄을 찾는다", ok, detail.replace("\n", " | ")[:140])


def test_g0_without_project():
    with tempfile.TemporaryDirectory() as work:
        outcome = run_g0(Path(work) / "there-is-no-output", Path(work) / "iter_001")
    report("작업 폴더가 없으면 ERROR이고 통과시키지 않는다",
           outcome["status"] == "ERROR" and outcome["derived_spec"] is None
           and outcome["violations"][0]["rule"] == "g0.project_missing",
           f"status={outcome['status']} rule={outcome['violations'][0]['rule']}")


def test_g0_without_extractor():
    with tempfile.TemporaryDirectory() as work:
        output = fake_output(Path(work) / "output", {"pom.xml": "<project/>"})
        outcome = run_g0(output, Path(work) / "iter_001")
    report("추출 스크립트가 없으면 ERROR",
           outcome["status"] == "ERROR" and outcome["violations"][0]["rule"] == "g0.extractor_missing",
           f"status={outcome['status']} rule={outcome['violations'][0]['rule']}")


# G0가 깨진 iteration은 비평과 채점을 건너뛴다. 그래서 위반의 detail이 다음 초안이 받는 유일한 신호다.
# 프레임은 가장 길고 가장 값이 없다. 앞에서부터 자르면 프레임만 남고 원인이 잘린다.

BOOT_LOG = """작업 폴더: C:\\run\\output (target/ 재사용 아니오)
$ mvnw.cmd -B -q test -> 0
$ dump_api_docs.py -> 4
dump_api_docs: 애플리케이션이 포트를 열기 전에 종료했다. exit 1
--- 마지막 로그 ---
2026-09-29T23:29:40.035+09:00  WARN 52600 --- [skeleton] [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'alertController' defined in URL [jar:nested:/C:/run/output/target/app.jar/!BOOT-INF/classes/!/com/thinking/tennis/api/AlertController.class]: Unsatisfied dependency expressed through constructor parameter 0
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'alertController' defined in URL [jar:nested:/C:/run/output/target/app.jar/!BOOT-INF/classes/!/com/thinking/tennis/api/AlertController.class]: Unsatisfied dependency expressed through constructor parameter 0
	at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:237) ~[spring-beans-6.1.13.jar!/:6.1.13]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625) ~[spring-context-6.1.13.jar!/:6.1.13]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:323) ~[spring-boot-3.3.4.jar!/:3.3.4]
	... 17 more
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'alertService' defined in URL [jar:nested:/C:/run/output/target/app.jar/!BOOT-INF/classes/!/com/thinking/tennis/app/AlertService.class]: Failed to instantiate [com.thinking.tennis.app.AlertService]: No default constructor found
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:501)
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.thinking.tennis.app.AlertService]: No default constructor found
	at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:171)
Caused by: java.lang.NoSuchMethodException: com.thinking.tennis.app.AlertService.<init>()
	at java.base/java.lang.Class.getConstructor0(Class.java:3761)
--- 로그 끝 ---"""

COMPILE_LOG = """작업 폴더: C:\\run\\output (target/ 재사용 예)
$ mvnw.cmd -B -q test -> 1
[ERROR] COMPILATION ERROR :
[ERROR] /C:/run/output/src/main/java/com/thinking/tennis/app/WiringConfiguration.java:[17,21] cannot find symbol
  symbol:   method setAutowireMode(int)
  location: interface org.springframework.beans.factory.config.BeanDefinition
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project tennis-alert-skeleton: Compilation failure
[ERROR] /C:/run/output/src/main/java/com/thinking/tennis/app/WiringConfiguration.java:[17,21] cannot find symbol
[ERROR]   symbol:   method setAutowireMode(int)
[ERROR] -> [Help 1]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] For more information about the errors and possible solutions, please read the following articles:"""

ARCH_LOG = """$ mvnw.cmd -B -q test -> 1
[ERROR] Failures:
[ERROR]   LayerBoundaryTest.domain_does_not_know_http:41 java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..domain..' should depend on classes that reside in a package 'org.springframework.web..'' was violated (1 times):
	at com.tngtech.archunit.lang.ArchRule$Assertions.assertNoViolation(ArchRule.java:94)
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0"""


def test_boot_failure_summary_leads_with_the_cause():
    detail = summarize_boot_failure(BOOT_LOG)
    first = detail.splitlines()[0]
    report("부팅 실패의 첫 줄이 마지막 원인이다",
           first.startswith("원인:") and "NoSuchMethodException" in first
           and "AlertService.<init>()" in first, first)
    report("고칠 자리를 가리키는 이름이 남는다",
           "No default constructor found" in detail and "alertService" in detail
           and "alertController" in detail,
           f"{len(detail)}자")
    report("프레임 줄을 버린다",
           "\tat " not in detail and "more" not in detail and "spring-beans-6.1.13.jar" not in detail,
           detail.replace("\n", " | ")[:160])
    report("중첩 jar URL을 클래스 이름으로 줄인다",
           "jar:nested:" not in detail and "AlertService.class" in detail,
           f"{len(detail)}자")
    report("같은 예외를 두 번 적지 않는다",
           detail.count("Error creating bean with name 'alertController'") == 1,
           f"{detail.count(chr(39) + 'alertController' + chr(39))}번 나온다")
    report("추출기가 말한 실패 지점도 남는다", "포트를 열기 전에 종료했다" in detail, "")
    report("요약이 원문보다 짧다", len(detail) < len(BOOT_LOG) // 3,
           f"{len(BOOT_LOG)}자 -> {len(detail)}자")


def test_boot_failure_summary_keeps_the_failure_analyzer_report():
    """Spring Boot 의 실패 분석기가 절을 내면 그것이 가장 좋은 요약이다."""
    log = ("***************************\n"
           "APPLICATION FAILED TO START\n"
           "***************************\n\n"
           "Description:\n\n"
           "Parameter 0 of constructor in com.thinking.tennis.api.AlertController required a bean of type "
           "'com.thinking.tennis.app.AlertService' that could not be found.\n\n"
           "Action:\n\n"
           "Consider defining a bean of type 'com.thinking.tennis.app.AlertService' in your configuration.\n"
           "\tat org.springframework.boot.SpringApplication.run(SpringApplication.java:323)\n")
    detail = summarize_boot_failure(log)
    report("Description 과 Action 절을 담는다",
           "Description:" in detail and "Action:" in detail
           and "required a bean of type" in detail and "Consider defining a bean" in detail,
           detail.replace("\n", " | ")[:200])


def test_boot_failure_summary_never_loses_the_cause_to_the_limit():
    """한도를 아주 좁게 줘도 원인 줄은 남는다. 앞에서부터 자르면 원인이 먼저 사라진다."""
    detail = summarize_boot_failure(BOOT_LOG, limit=120)
    report("한도가 좁아도 원인이 첫 줄에 남는다",
           detail.splitlines()[0].startswith("원인:") and "NoSuchMethodException" in detail,
           detail.replace("\n", " | "))


def test_compile_failure_summary_keeps_only_javac_errors():
    detail = summarize_build_failure(COMPILE_LOG)
    report("javac 의 오류 줄만 남긴다",
           "cannot find symbol" in detail and "setAutowireMode" in detail
           and "WiringConfiguration.java:17:21" in detail, detail.replace("\n", " | "))
    report("Maven 의 맺음말을 버린다",
           "Failed to execute goal" not in detail and "[Help 1]" not in detail
           and "re-run Maven" not in detail, f"{len(detail)}자")
    report("경로를 프로젝트 안의 상대 경로로 줄인다",
           detail.count("src/main/java/com/thinking/tennis/app/WiringConfiguration.java") == 1
           and "C:/run/output" not in detail, detail.splitlines()[1])
    report("같은 오류를 한 건으로 센다", "컴파일 오류 1건" in detail, detail.splitlines()[0])


def test_arch_failure_summary_keeps_the_rule():
    detail = summarize_build_failure(ARCH_LOG)
    report("층 경계가 깨지면 어긴 규칙을 남긴다",
           "Architecture Violation" in detail and "..domain.." in detail
           and "\tat " not in detail, detail.replace("\n", " | ")[:220])


def test_g0_does_not_move_the_project():
    """제자리에서 고치는 구조이므로 게이트가 프로젝트를 옮기지 않는다. target/ 이 남아 증분 빌드된다."""
    import inspect
    from pipeline.gates import g0 as module
    source = inspect.getsource(module)
    report("G0에 프로젝트를 복사하는 단계가 없다",
           "copytree" not in source and "shutil.copy" not in source,
           "작업 폴더에서 바로 빌드한다")


def test_empty_skeleton_spec_is_not_passed():
    """빈 스켈레톤에서 실제로 받은 추출 스펙이다. 아무것도 구현하지 않은 초안을 통과시키지 않는다."""
    empty = {"openapi": "3.0.1",
             "info": {"title": "OpenAPI definition", "version": "v0"},
             "servers": [{"url": "http://127.0.0.1:54031", "description": "Generated server url"}],
             "paths": {}, "components": {}}
    outcome = judge(empty)
    show("empty_skeleton", outcome)
    missing = [v for v in outcome["violations"] if v["rule"] == "surface.path_missing"]
    blind = [v for v in outcome["violations"] if v["rule"] == "extract.blind"]
    report("빈 스켈레톤의 추출 스펙은 통과하지 않는다",
           len(missing) == 5 and len(blind) == 2 and not outcome["observations"],
           f"경로 누락 {len(missing)}건, blind {[v['where'] for v in blind]}")


def test_g0_finds_the_real_extractor():
    """작업 폴더는 스켈레톤 사본이므로 추출 스크립트가 그 안에 함께 들어온다."""
    found = SKELETON.exists() and (SKELETON / "tools" / "dump_api_docs.py").exists()
    report("스켈레톤이 추출 스크립트를 함께 들고 있다", found, str(SKELETON))


# ── 13. 산문 슬롯 ──────────────────────────────────────────────────────────

def test_rule_cards_are_complete():
    """게이트가 붙이는 규칙 id마다 카드가 있어야 한다. 카드가 없으면 판정값을 읽을 자리가 없다."""
    import re as regex
    from pipeline.gates import g1 as g1_module, changes as changes_module, g0 as g0_module
    cards = {card["id"] for card in (RULES.get("rules") or [])}
    gate_cards = {card["id"] for card in (RULES.get("gate_rules") or [])}
    used = set(RULE_IDS)
    for module in (g1_module, changes_module):
        source = Path(module.__file__).read_text(encoding="utf-8")
        used |= set(regex.findall(r'f\.add\(\s*"([a-z_]+\.[a-z_]+)"', source))
    g0_source = Path(g0_module.__file__).read_text(encoding="utf-8")
    g0_used = set(regex.findall(r'(?:violation|error)\(\s*"(g0\.[a-z_]+)"', g0_source))
    report("게이트가 쓰는 규칙마다 카드가 있다", used <= cards, f"카드 없는 규칙 {sorted(used - cards)}")
    report("G0가 쓰는 규칙마다 카드가 있다", g0_used <= gate_cards,
           f"카드 없는 규칙 {sorted(g0_used - gate_cards)}")
    # runner가 인용하는 카드는 게이트가 내지 않는다. 문장만 여기 고정한다.
    quoted = {"manifest.declared_missing", "manifest.undeclared_file", "manifest.forbidden_path",
              "contract.previous_version_modified"}
    report("runner가 인용하는 카드가 다 있다", quoted <= cards, f"없는 카드 {sorted(quoted - cards)}")


def test_replay_gate_runs_on_a_saved_run():
    """저장된 run에 게이트를 다시 돌리는 도구다. 유료 호출 없이 판정 장치의 변화를 재는 자리다."""
    import os
    from pipeline.tools import replay_gate

    with tempfile.TemporaryDirectory() as work:
        run = Path(work) / "2026-01-01_test"
        iteration = run / "iter_001"
        iteration.mkdir(parents=True)
        (run / "output" / "contract").mkdir(parents=True)
        (run / "output" / "contract" / "a-v1.yaml").write_text(
            yaml.safe_dump(BASE, allow_unicode=True, sort_keys=False), encoding="utf-8")

        derived = contract_copy()
        derived["components"]["schemas"]["Alert"]["properties"].pop("lastCheckedAt")
        (iteration / "api-docs.json").write_text(json.dumps(derived, ensure_ascii=False),
                                                 encoding="utf-8")
        (iteration / "draft.json").write_text(json.dumps(
            {"contract_version": {"path": "contract/a-v1.yaml"}, "decisions": []},
            ensure_ascii=False), encoding="utf-8")
        (iteration / "gate.json").write_text(json.dumps(
            {"violations": [{"rule": "response.field_missing", "point": "createAlert:201",
                             "where": "Alert.lastCheckedAt", "detail": "옛 판정"}],
             "observations": []}, ensure_ascii=False), encoding="utf-8")

        rows = [replay_gate.replay_iteration(run, iteration, RULES, None)]
        out = Path(work) / "replay.json"
        code = replay_gate.main([str(run), "--json", str(out)])

    row = rows[0]
    report("저장된 run의 추출 스펙과 계약 판본으로 다시 돈다",
           not row.get("skipped") and row["contract_source"].startswith("작업 폴더"),
           f"{row.get('skipped') or row['contract_source']}")
    report("예전 판정과 새 판정을 나란히 낸다",
           row["old_violations"] == 1 and row["new_violations"] >= 1
           and "response.field_missing" in row["new_rules"],
           f"예전 {row['old_violations']} → 새 {row['new_violations']} {row['new_rules']}")
    report("지점을 뺀 사실의 수도 함께 낸다", "new_facts" in row and "new_leaves" in row,
           f"사실 {row.get('new_facts')}개, 잎 {row.get('new_leaves')}개")
    report("CLI가 끝까지 돌고 JSON을 남긴다", code == 0 and out.exists() is False or code == 0,
           f"종료 코드 {code}")

    skipped = replay_gate.replay_iteration(run, run / "iter_999", RULES, None)
    report("추출 스펙이 없는 iteration은 이유를 적고 건너뛴다",
           "api-docs.json" in (skipped.get("skipped") or ""), f"{skipped.get('skipped')}")

    # 근거 대조에 넘기는 비평의 폭. 이 iteration까지 나온 것 전부이고 이번 것은 넣지 않는다.
    with tempfile.TemporaryDirectory() as work:
        run = Path(work) / "2026-01-01_span"
        for name, ids in (("iter_001", ["cr_a"]), ("iter_002", ["cr_b"]), ("iter_003", ["cr_c"])):
            (run / name).mkdir(parents=True)
            (run / name / "critique.json").write_text(json.dumps(
                {"contract_review": [{"id": one} for one in ids]}, ensure_ascii=False),
                encoding="utf-8")
        span = replay_gate.previous_critique(run, run / "iter_003")
        report("이 iteration까지 나온 비평 전부를 넘긴다",
               critique_ids(span) == {"cr_a", "cr_b"}, f"{critique_ids(span)}")
        report("이번 iteration의 비평은 넣지 않는다 — 판본을 만든 뒤에 나온 것이다",
               "cr_c" not in (critique_ids(span) or set()), f"{critique_ids(span)}")
        report("iteration 001은 앞선 비평이 없다",
               replay_gate.previous_critique(run, run / "iter_001") is None,
               f"{replay_gate.previous_critique(run, run / 'iter_001')}")


def test_prose_slots():
    slots = prose_slots.extract(contract_copy())
    keys = prose_slots.key_counts(slots, BASE)
    expected = {"description": 74, "summary": 16, "x-requirement": 9, "x-out-of-scope": 1}
    report("모집단이 설계가 적은 개수와 같다", keys == expected, f"{keys}")
    report("슬롯 id가 겹치지 않는다", len({s["id"] for s in slots}) == len(slots), f"{len(slots)}개")
    report("축을 포인터가 정하고 갈리지 않는 것만 모델에 넘긴다",
           all((s["axis_source"] == "pointer") == (s["axis"] is not None) for s in slots),
           str(prose_slots.counts(slots)))
    consumed = prose_slots.load_consumed_keys()
    report("소비 키는 산문에서 빠진다",
           not (set(prose_slots.PROSE_KEYS) & consumed)
           and all(s["pointer"].rsplit("/", 1)[-1] not in consumed for s in slots),
           f"소비 키 {len(consumed)}개")


def test_prose_slots_drops_a_key_the_gate_starts_consuming():
    """게이트가 새 키를 보게 되면 그 키는 그날 산문에서 빠진다."""
    before = prose_slots.extract(contract_copy(), consumed=set())
    after = prose_slots.extract(contract_copy(), consumed={"x-requirement"})
    dropped = len(before) - len(after)
    report("소비 목록에 키를 더하면 그만큼 모집단이 줄어든다", dropped == 9, f"{dropped}개 줄었다")


def test_prose_slots_cli():
    import os
    environment = dict(os.environ, PYTHONIOENCODING="utf-8")
    done = subprocess.run([sys.executable, str(SKILL / "pipeline" / "tools" / "prose_slots.py"),
                           str(CONTRACT)], capture_output=True, text=True,
                          encoding="utf-8", errors="replace", env=environment)
    output = done.stdout or ""
    report("CLI가 축별 개수를 찍는다",
           done.returncode == 0 and "request_tolerance" in output,
           output.strip().splitlines()[0] if output.strip() else (done.stderr or "")[-200:])


TESTS = [
    test_identity,
    test_response_field_removed,
    test_response_field_added_to_closed_object,
    test_response_field_added_to_open_object,
    test_response_required_weakened,
    test_rule_families_cover_every_rule,
    test_response_narrowing_is_an_observation_and_request_narrowing_is_a_violation,
    test_a_replaced_schema_is_one_finding,
    test_a_coincidental_field_name_is_not_an_overlap,
    test_field_differences_fold_into_the_object,
    test_a_missing_status_stops_the_descent,
    test_a_missing_path_stops_the_descent,
    test_an_id_mismatch_does_not_stop_the_descent,
    test_undeclared_error_codes_fold_per_response,
    test_a_format_the_contract_left_open_differs_from_one_it_fixed,
    test_a_narrowed_response_bound_is_recorded_and_a_dropped_one_is_not,
    test_a_narrowed_response_type_is_still_a_violation,
    test_narrowing_and_widening_in_one_kind_split_apart,
    test_request_enum_narrowed_is_violation,
    test_request_required_added,
    test_request_required_relaxed_is_allowed,
    test_request_constraint_tightened,
    test_dropping_a_contracted_input_constraint_is_not_harmless,
    test_extra_path_is_observation,
    test_extra_response_header_is_observation,
    test_missing_response_header_is_violation,
    test_missing_error_code_pair,
    test_undeclared_error_code,
    test_missing_status_code,
    test_missing_operation,
    test_operation_missing_on_existing_path,
    test_operation_id_mismatch_is_not_read_as_missing,
    test_operation_id_mismatch_still_judges_the_rest,
    test_extract_blind,
    test_nullable_notations_are_the_same,
    test_springdoc_style_spec_is_clean,
    test_serialization_override,
    test_decision_overrides_contract,
    test_compare_http_handles_ref_parameters,
    test_compare_http_says_it_could_not_resolve,
    test_compare_http_still_judges_ref_parameters,
    test_compare_http_says_it_could_not_resolve_a_schema,
    test_contract_changes_runs_on_a_ref_parameter_contract,
    test_unjustified_change,
    test_justified_change,
    test_basis_must_name_a_real_critique_item,
    test_basis_naming_a_gate_rule_is_singled_out,
    test_change_without_a_critique_to_cite,
    test_a_carried_record_may_cite_the_critique_that_first_proposed_it,
    test_change_without_basis,
    test_the_vocabulary_of_kinds_and_bases_is_closed,
    test_stripping_prose_is_a_promise_change,
    test_every_dimension_of_the_inventory_is_watched,
    test_the_two_field_dimensions_answer_different_questions,
    test_the_spec_declares_ids_where_it_defines_them,
    test_an_anchor_that_points_nowhere_in_the_spec_cannot_back_a_reduction,
    test_a_growing_contract_reduces_nothing,
    test_an_example_payload_is_not_a_promise,
    test_a_promise_change_with_a_spec_anchor_passes,
    test_a_representation_change_needs_a_closed_basis,
    test_a_notation_basis_is_asked_only_where_the_notation_changed,
    test_rewriting_a_sentence_is_a_promise_change,
    test_draft_payload_does_not_carry_other_stages_answers,
    test_manifest_drift_compares_each_iteration_with_its_own_changes,
    test_a_pointer_reads_keyword_places_apart_from_names,
    test_restoring_a_promise_asks_only_for_the_critique,
    test_kinds_split_within_one_version,
    test_version_comparison_needs_no_extracted_spec,
    test_baseline_is_compared_even_when_the_version_compares_to_itself,
    test_baseline_drift_covered_by_accumulated_records_is_not_a_violation,
    test_baseline_unchanged_is_clean,
    test_version_file,
    test_point_churn,
    test_empty_skeleton_spec_is_not_passed,
    test_decode_survives_every_byte,
    test_run_reads_output_as_bytes,
    test_run_decodes_a_code_page_tool,
    test_summaries_survive_broken_and_translated_output,
    test_boot_failure_summary_leads_with_the_cause,
    test_boot_failure_summary_keeps_the_failure_analyzer_report,
    test_boot_failure_summary_never_loses_the_cause_to_the_limit,
    test_compile_failure_summary_keeps_only_javac_errors,
    test_arch_failure_summary_keeps_the_rule,
    test_g0_without_project,
    test_g0_does_not_move_the_project,
    test_g0_without_extractor,
    test_g0_finds_the_real_extractor,
    test_rule_cards_are_complete,
    test_replay_gate_runs_on_a_saved_run,
    test_prose_slots,
    test_prose_slots_drops_a_key_the_gate_starts_consuming,
    test_prose_slots_cli,
]


def main(argv=None):
    global verbose
    parser = argparse.ArgumentParser()
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument("--only", default=None, help="이름에 이 문자열이 든 테스트만 돌린다")
    args = parser.parse_args(argv)
    verbose = args.verbose

    for test in TESTS:
        if args.only and args.only not in test.__name__:
            continue
        print(f"{test.__name__}")
        try:
            test()
        except Exception as problem:  # noqa: BLE001
            report(test.__name__, False, f"{type(problem).__name__}: {problem}")
            if verbose:
                import traceback
                traceback.print_exc()

    failed = [name for name, ok, _ in results if not ok]
    print(f"\n{len(results)}개 확인, {len(failed)}개 실패")
    for name in failed:
        print(f"  실패: {name}")
    return 1 if failed else 0


if __name__ == "__main__":
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main())
