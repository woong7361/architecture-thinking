"""G1 — 구현이 자기 계약 판본을 지켰는가.

대조 상대는 그 iteration의 계약 판본이다. 계약을 고칠 수 있게 했으므로 이 게이트가 잡는 것은 구현과 문서의
어긋남이다. 계약을 고쳐 코드를 정당화했다면 문서에 그 사실이 남아 있어야 하고, 남지 않으면 여기서 걸린다.

**대조의 양쪽이 모두 선언이다.** 계약이 선언한 표면과 구현에서 추출한 선언을 견주므로 물을 것은 하나다.
구현이 자기 표면을 계약과 같은 말로 선언했는가. 그래서 판정은 **최소 포함**이다. 계약이 선언한 것이 추출
스펙에 다 있어야 하고, 추출 스펙에 더 있는 것은 계약이 그 자리를 닫았다고 말한 경우에만 위반이다.

규칙은 다섯 가족이다.

| 가족 | 무엇 | 판정 |
| --- | --- | --- |
| 없다 | 계약이 선언한 오퍼레이션·경로·상태 코드·필드·헤더·에러 코드 쌍이 추출 스펙에 없다 | 위반 |
| 다르다 | 계약이 정한 타입·형식·모양·제약·열거형·필수·보안·식별자가 다르다 | 위반 |
| 닫은 자리에 더 있다 | 계약이 `additionalProperties: false`로 닫은 객체에 계약에 없는 필드가 있다 | 위반 |
| 응답에서 좁혔다 | 계약이 응답에 허용한 값의 범위나 형식을 구현이 더 좁게 선언했다 | 기록 |
| 말하지 않은 자리에 더 있다 | 계약이 언급하지 않은 경로·상태 코드·헤더·필드가 있다 | 기록 |

차이가 좁아진 것인지 넓어진 것인지는 규칙이 아니라 **라벨**로 붙인다. 판본 대비 판본에서는 그 방향이 판정을
갈랐지만 선언 대비 선언에서는 아니다. 라벨은 그 차이가 쓰는 사람에게 어떤 뜻인지를 말하고 리포트의 정렬과
`decision_risk`에 쓰인다. 라벨의 어휘는 닫힌 값이고 뜻은 `rules/conformance_rules.yaml`의 `labels`가 고정한다.

**방향이 판정을 가르는 자리는 하나다.** 응답에서 값의 범위와 형식이 좁아진 것은 차이지만 위반이 아니다.
구현이 계약이 약속한 것을 다 지키면서 좁게 지킨 것이므로 계약대로 읽는 쪽이 손해를 보지 않고, 그것을 위반으로
세면 계약을 만족하는 구현이 떨어진다. 그 자리는 `response.value_narrowed`로 나가고 카드가 기록으로 고정한다.
좁힐 수 있는 것은 값의 범위와 형식이지 타입이 아니므로 타입이 좁아진 것은 `다르다`에 남고, 계약이 정한 형식이나
모양을 다른 것으로 바꾼 것은 좁힌 것으로 볼 수 없어 거기 남는다. 요청은 반대 방향이라 그대로다. 계약이
받겠다고 한 것을 좁게 받으면 위반이다. 나머지 모든 방향 차이는 판정을 가르지 않고 라벨로만 갈린다.

**차이는 설명할 수 있는 가장 높은 고도에서 한 건이다.** 잎에서만 세면 원인 하나가 수백 건이 된다. 실제로
한 run의 iteration에서 위반 523건이 판정 지점 33개에서 나왔고 그중 26개 지점이 446건을 만들었는데 원인은
하나였다. 에러 응답에 스키마를 주지 않아 도구가 성공 응답의 타입을 썼고, 그 한 가지가 지점마다 필드 누락
아홉 건과 초과 여덟 건으로, 같은 사실이 양쪽 방향에서 두 번 세어졌다.

그래서 위에서 아래로 내려가며 설명할 수 있는 자리에서 멈춘다. 오퍼레이션이 없으면 그 한 건이고 그 아래는
보지 않는다. 그 자리의 스키마가 계약이 정한 것과 다른 종류면 그 한 건이고 필드 차이는 그 건의 `evidence`로
붙는다. 같은 종류인데 일부가 다를 때만 필드 고도로 내려간다.

**멈추는 것과 건너뛰는 것은 다르다.** 상위를 판정했고 그 판정이 하위를 설명할 때만 멈춘다. 상위에서 짝을
찾지 못해 판정하지 못했으면 하위를 계속 본다. 오퍼레이션의 짝을 식별자로 찾다가 식별자가 어긋나 그 아래
44건을 통째로 놓친 일이 후자였다. 그래서 짝은 경로와 메서드로 찾고 식별자는 그다음에 보며, 식별자가 달라도
그 아래를 계속 판정한다.

판정 지점 좌표는 `coverage_gate.py`와 같은 `operationId:status`와 `operationId:CODE`다.
판정 범위는 `scope`가 정한다. 구현의 내부 구조, 패키지 이름, 클래스 분해는 계약이 말하지 않았으므로 보지 않는다.

추출한 스펙이 계약의 어떤 조항에 대응하는 것을 아예 갖고 있지 않으면 `extract.blind`로 위반 처리한다.
게이트 공백은 통과가 아니다. 못 본 것을 통과시키지 않는다.

이 게이트가 판정하지 못하는 것은 따로 있다. 추출한 스펙은 타입 선언을 보여주므로 그 필드가 응답에 실제로
실리는지는 말하지 않는다. 널 허용 필드의 키와 조건부 필수는 실물 응답을 보는 G3의 몫이고, 그때까지는
루브릭이 맡는다. 이 게이트는 그 자리를 판정한다고 말하지 않는다.

그 자리에 예외가 하나 있다. 사람이 소유한 설정이 널을 지우지 않도록 고정해 두었는데 생성 코드가 그것을
덮어쓰면, 계약이 널을 허용하면서 필수로 둔 필드의 키가 응답에서 사라진다. 그 덮어쓰기는 추출 스펙에
드러나지 않지만 소스에는 드러나므로 `check_serialization`이 소스를 읽어 잡는다. 실물 응답을 보는 것이
아니라 재정의가 있는지만 보므로 G3를 대신하지는 않는다.

추출 스펙에는 구현이 약속한 계약이 아닌 값이 섞인다. 문서 판본과 제목과 서버 주소가 도구의 기본값이거나
부팅에 쓴 임의 포트다. 그 자리를 대조하면 구현과 무관한 차이가 위반으로 쏟아지므로 대조 전에 지운다.
목록은 `rules/consumed_keys.yaml`의 `derived_spec_ignored`가 정한다. 왜 빠졌는지가 코드가 아니라
규칙 파일에 남게 하려는 것이다.
"""

import re
from pathlib import Path

import yaml

from .schema_compare import METHODS, TIGHTENED, deref, type_set, unwrap_nullable

SKILL = Path(__file__).resolve().parents[2]
CONSUMED_KEYS = SKILL / "rules" / "consumed_keys.yaml"

# 값을 정해 둔 키. 계약이 이 중 하나를 적어 둔 자리는 빈자리가 아니다.
VALUE_FIXING = ("enum", "const", "default", "pattern", "minimum", "maximum",
                "exclusiveMinimum", "exclusiveMaximum", "minLength", "maxLength",
                "minItems", "maxItems", "multipleOf")

# 널을 지우는 직렬화 재정의. 어노테이션과 코드 설정과 설정 파일의 세 경로가 모두 같은 일을 한다.
SERIALIZATION_OVERRIDE = (
    (re.compile(r"@JsonInclude\b"), "@JsonInclude"),
    (re.compile(r"JsonInclude\.Include\.(?!ALWAYS)\w+"), "JsonInclude.Include"),
    (re.compile(r"@JsonSerialize\s*\([^)]*include\s*="), "@JsonSerialize(include=...)"),
    (re.compile(r"setSerializationInclusion\s*\("), "setSerializationInclusion(...)"),
    (re.compile(r"serializationInclusion\s*\("), "serializationInclusion(...)"),
    (re.compile(r"NON_NULL|NON_ABSENT|NON_EMPTY|NON_DEFAULT"), "Include 상수"),
    (re.compile(r"default-property-inclusion\s*:\s*(?!always)\S+"), "default-property-inclusion"),
    (re.compile(r"default_property_inclusion\s*=\s*(?!always)\S+"), "default_property_inclusion"),
)

SOURCE_SUFFIXES = (".java", ".kt", ".yaml", ".yml", ".properties")

# 작업 폴더 안에서 소스가 아닌 것. `target/`은 빌드가 리소스를 복사해 두어 같은 위반을 두 번 세게 하고,
# `.git/`은 iteration 스냅샷이라 지난 판본의 소스를 다시 읽게 한다.
SKIP_DIRS = ("target", ".git", ".mvn", "__pycache__", "node_modules")

# 차이의 라벨. 판정이 아니라 그 차이가 쓰는 사람에게 어떤 뜻인지다. 어휘는 닫혀 있고 뜻은 규칙 카드가 고정한다.
REJECTS_REQUEST = "rejects_contracted_request"
ACCEPTS_REJECTED = "accepts_what_contract_rejects"
WITHHOLDS_RESPONSE = "withholds_promised_response"
BREAKS_READER = "breaks_reader"
HARMLESS = "harmless_to_client"
UNDECLARED = "undeclared_surface"

LABELS = (REJECTS_REQUEST, ACCEPTS_REJECTED, WITHHOLDS_RESPONSE, BREAKS_READER, HARMLESS,
          UNDECLARED)

# 이 게이트가 붙일 수 있는 규칙 id 전부. 원장이 규칙 id로 묶어 세므로 목록이 한 자리에 있어야 하고,
# 규칙 카드와 어긋나면 테스트가 그 자리에서 걸린다. 일부는 f-string으로 만들어지므로 소스를 긁어서는 못 모은다.
RULE_IDS = (
    # 없다
    "surface.path_missing", "request.operation_missing", "request.input_missing", "request.body_missing",
    "response.status_missing", "response.body_missing", "response.field_missing", "response.header_missing",
    "error.code_pair_missing",
    # 다르다
    "request.operation_id_mismatch", "request.security_differs",
    "request.schema_differs", "request.input_differs",
    "response.schema_differs", "response.field_differs",
    # 응답에서 좁혔다
    "response.value_narrowed",
    # 닫은 자리에 더 있다
    "response.field_extra_closed", "error.code_undeclared",
    # 말하지 않은 자리에 더 있다
    "surface.path_extra", "surface.status_extra", "surface.header_extra",
    "request.input_extra", "response.field_extra",
    # 스펙 대조가 아닌 판정
    "extract.blind", "response.serialization_override",
    "decision.overrides_contract", "decision.unknown_point",
)

# 값을 좁히는 쪽으로 바뀌면 받던 것을 거절하게 되는 제약. 방향은 라벨을 고르는 데만 쓴다.


class Findings:
    """규칙 카드에서 판정값을 읽어 위반과 기록으로 나눈다."""

    def __init__(self, rules):
        cards = {}
        for card in ((rules or {}).get("rules") or []):
            if card.get("id"):
                cards[card["id"]] = card
        self.cards = cards
        self.violations = []
        self.observations = []

    def add(self, rule, point, where, detail, label=None, evidence=None, kind=None):
        """판정 하나를 남긴다.

        `label`은 그 차이가 쓰는 사람에게 어떤 뜻인지이고 판정을 가르지 않는다. `evidence`는 더 높은 고도로
        접을 때 잎을 남기는 자리다. 원인 하나를 한 건으로 세는 것과 원인을 못 보는 것은 다르다.
        `kind`는 계약 변경이 표현을 바꾼 것인지 약속을 줄인 것인지다. 원장과 리포트가 그것으로 정렬한다.
        """
        card = self.cards.get(rule)
        if card is None:
            # 카드가 없는 판정은 통과시키지 않는다. 게이트 공백은 통과가 아니다.
            verdict = "violation"
            detail = f"{detail} (규칙 카드가 없다: {rule})"
        else:
            verdict = card.get("verdict", "violation")
        item = {"rule": rule, "point": point, "where": where, "detail": detail, "verdict": verdict}
        if label:
            item["label"] = label
        if kind:
            item["kind"] = kind
        if evidence:
            item["evidence"] = list(evidence)
        (self.observations if verdict == "observation" else self.violations).append(item)
        return item

    def result(self):
        return {"violations": self.violations, "observations": self.observations}


# ── 문서 읽기 ───────────────────────────────────────────────────────────────

def resolve_one(doc, node):
    """`$ref` 하나를 따라간다. 이름표를 잃지 않으려고 펼치지 않고 그 자리만 바꾼다."""
    seen = 0
    while isinstance(node, dict) and "$ref" in node and seen < 20:
        target = doc
        for part in node["$ref"].lstrip("#/").split("/"):
            part = part.replace("~1", "/").replace("~0", "~")
            if not isinstance(target, dict) or part not in target:
                return {}
            target = target[part]
        node = target
        seen += 1
    return node if isinstance(node, dict) else {}


def ref_label(node, fallback):
    """스키마의 `$ref` 마지막 조각을 좌표의 뿌리 이름으로 쓴다. 없으면 fallback."""
    if isinstance(node, dict) and isinstance(node.get("$ref"), str):
        return node["$ref"].rsplit("/", 1)[-1]
    return fallback


def routes(doc):
    """{(path, method): (operationId or None, operation)}. 경로 항목의 공용 parameters를 오퍼레이션에 합친다.

    짝을 경로와 메서드로 찾는다. 식별자로 찾으면 같은 경로와 메서드인데도 다른 것으로 보인다. 추출 스펙의
    식별자는 도구가 메서드 이름에서 뽑은 기본값일 수 있어서 계약이 정한 이름과 다르고, 그때 다섯 오퍼레이션이
    전부 누락으로 잡혀 "만들지 않았다"로 읽힌다. 계약이 약속한 것은 경로와 메서드이고 식별자는 그 자리에
    붙은 이름이므로, 먼저 자리를 맞추고 이름은 그다음에 본다.
    """
    found = {}
    for path, item in (doc.get("paths") or {}).items():
        if not isinstance(item, dict):
            continue
        shared = item.get("parameters") or []
        for method, operation in item.items():
            if method not in METHODS or not isinstance(operation, dict):
                continue
            merged = dict(operation)
            if shared:
                merged["parameters"] = list(shared) + list(operation.get("parameters") or [])
            found[(path, method)] = (operation.get("operationId"), merged)
    return found


def operations(doc):
    """{operationId: (path, method, operation)}. 판정 지점 좌표가 식별자이므로 계약 쪽은 이 색인으로 연다."""
    found = {}
    for (path, method), (name, operation) in routes(doc).items():
        found[name or f"{method}:{path}"] = (path, method, operation)
    return found


def parameters_of(doc, operation):
    """{(name, in): 파라미터}. `$ref`를 따라간 뒤 이름으로 짝짓는다."""
    found = {}
    for entry in operation.get("parameters") or []:
        resolved = resolve_one(doc, entry)
        if resolved.get("name"):
            found[(resolved["name"], resolved.get("in", "query"))] = resolved
    return found


def body_of(doc, operation):
    """요청 본문의 (선언 여부, 스키마 원본). 스키마 원본은 `$ref`를 남겨 둔다."""
    body = operation.get("requestBody")
    if body is None:
        return False, None
    resolved = resolve_one(doc, body)
    for media in (resolved.get("content") or {}).values():
        return True, media.get("schema")
    return True, None


def response_of(doc, operation, status):
    return resolve_one(doc, (operation.get("responses") or {}).get(status) or {})


def response_body(response):
    for media in (response.get("content") or {}).values():
        return media.get("schema")
    return None


def has_key_anywhere(node, key):
    if isinstance(node, dict):
        if key in node:
            return True
        return any(has_key_anywhere(value, key) for value in node.values())
    if isinstance(node, list):
        return any(has_key_anywhere(item, key) for item in node)
    return False


def load_ignored_pointers(path=CONSUMED_KEYS):
    """추출 스펙에서 비교하지 않는 자리. 목록은 규칙 파일이 정한다."""
    if not Path(path).exists():
        return []
    doc = yaml.safe_load(Path(path).read_text(encoding="utf-8")) or {}
    return [entry["pointer"] for entry in doc.get("derived_spec_ignored") or [] if entry.get("pointer")]


def strip_ignored(spec, pointers):
    """대조 전에 비교하지 않는 자리를 지운다.

    추출 스펙은 스켈레톤을 부팅해 받으므로 문서 판본과 제목과 서버 주소가 도구의 기본값이거나
    그때 쓴 임의 포트다. 구현이 약속한 계약이 아니므로 지우고 대조한다.
    """
    if not isinstance(spec, dict):
        return spec
    trimmed = dict(spec)
    for pointer in pointers:
        node = trimmed
        tokens = [t.replace("~1", "/").replace("~0", "~") for t in pointer.lstrip("/").split("/")]
        for token in tokens[:-1]:
            if not isinstance(node, dict) or token not in node:
                node = None
                break
            node[token] = dict(node[token]) if isinstance(node[token], dict) else node[token]
            node = node[token]
        if isinstance(node, dict):
            node.pop(tokens[-1], None)
    return trimmed


def check_serialization(f, project_dir):
    """생성 코드가 널을 지우는 직렬화 설정으로 사람 소유의 설정을 덮는지 본다.

    계약은 널을 허용하면서 필수로 둔 필드가 있다. 그 자리는 키가 있고 값이 널이라는 뜻인데, 직렬화가
    널을 지우면 키가 사라져 계약을 어긴다. 그 위반은 추출한 스펙에 드러나지 않는다. 스펙은 타입 선언만
    보여주므로 그 필드가 응답에 실제로 실리는지는 말하지 않는다. 그래서 여기만 소스를 읽는다.

    소스는 매니페스트가 아니라 디스크에서 읽는다. 생성기가 파일 내용을 응답에 담지 않고 작업 폴더의 실제
    프로젝트를 제자리에서 고치기 때문이다. 매니페스트는 경로 색인만 담으므로 내용은 여기서 열어 봐야 한다.
    """
    if not project_dir:
        return
    root = Path(project_dir)
    if not root.exists():
        return
    for source in sorted(root.rglob("*")):
        if not source.is_file() or source.suffix not in SOURCE_SUFFIXES:
            continue
        if any(part in SKIP_DIRS for part in source.relative_to(root).parts):
            continue
        try:
            content = source.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        hits = sorted({label for pattern, label in SERIALIZATION_OVERRIDE if pattern.search(content)})
        if hits:
            f.add("response.serialization_override", None,
                  source.relative_to(root).as_posix(),
                  f"직렬화 재정의를 찾았다: {', '.join(hits)}", label=WITHHOLDS_RESPONSE)


def effective_security(doc, operation):
    """오퍼레이션이 인증을 요구하는지. 오퍼레이션이 적지 않으면 문서 수준을 물려받는다."""
    if "security" in operation:
        return operation["security"] or []
    return doc.get("security") or []


# ── 스키마의 종류 ───────────────────────────────────────────────────────────

def flatten(doc, schema):
    """대조할 수 있는 모양으로 편다. `$ref`를 따라가고 널 허용 표기를 정규화한다."""
    node, _ = unwrap_nullable(deref(doc, schema) if schema is not None else {})
    return node if isinstance(node, dict) else {}


def properties_of(node):
    return node.get("properties") or {}


def is_leaf(node):
    """자식이 없는 자리. 값 선언만 있다."""
    return not properties_of(node) and not isinstance(node.get("items"), dict)


def comparable(old_child, new_child):
    """이름이 같은 두 필드를 견줄 수 있는가.

    이름만 같고 타입이 겹치지 않는 필드는 같은 필드가 아니다. 실제로 성공 응답의 문자열 `status`와
    문제 응답의 정수 `status`가 이름만 같았고, 그것을 겹침으로 세면 통째로 다른 스키마가 같은 종류로 보인다.
    """
    old_types, new_types = type_set(old_child), type_set(new_child)
    if not old_types or not new_types:
        return True
    return bool(old_types & new_types)


def shared_fields(c_doc, d_doc, old, new):
    """이름이 같고 견줄 수 있는 필드의 이름들."""
    old_props, new_props = properties_of(old), properties_of(new)
    found = set()
    for name in set(old_props) & set(new_props):
        if comparable(flatten(c_doc, old_props[name]), flatten(d_doc, new_props[name])):
            found.add(name)
    return found


def different_kind(c_doc, d_doc, old, new):
    """계약이 정한 것과 다른 종류인가. 다르면 왜 다른지를, 같으면 None을 돌려준다.

    겹침의 비율로 가르지 않는다. 문턱값을 두면 그 값이 판정을 흔들고 계약이 바뀔 때마다 다시 골라야 한다.
    그래서 흔들리지 않는 두 가지만 본다.

    - **최상위 타입이 겹치지 않는다.** 계약이 객체라고 한 자리에 배열이나 스칼라가 있으면 아래를 견줄 것이 없다.
    - **견줄 수 있는 필드가 하나도 없다.** 하나라도 있으면 그 객체는 계약이 말한 그 객체일 수 있고 고칠 것은
      필드다. 하나도 없으면 다른 객체가 그 자리에 온 것이다. 이름만 같고 타입이 겹치지 않는 필드는 세지 않는다.
    """
    old_types, new_types = type_set(old), type_set(new)
    if old_types and new_types and not (new_types & old_types):
        return f"계약은 {sorted(old_types)}이라고 했는데 구현은 {sorted(new_types)}이다"

    old_props, new_props = properties_of(old), properties_of(new)
    if old_props and new_props and not shared_fields(c_doc, d_doc, old, new):
        return (f"계약이 선언한 필드 {len(old_props)}개 중 구현에 견줄 수 있는 것이 하나도 없다"
                f" (구현은 다른 필드 {len(new_props)}개를 선언한다)")
    return None


def kind_evidence(c_doc, d_doc, old, new, closed):
    """다른 종류로 접을 때 잎을 증거로 남긴다. 원인 하나를 한 건으로 세는 것과 원인을 못 보는 것은 다르다."""
    old_props, new_props = properties_of(old), properties_of(new)
    evidence = [f"없다: {name}" for name in sorted(set(old_props) - set(new_props))]
    kept = "닫은 자리에 더 있다" if closed else "말하지 않은 자리에 더 있다"
    evidence += [f"{kept}: {name}" for name in sorted(set(new_props) - set(old_props))]
    for name in sorted(set(old_props) & set(new_props)):
        child_old, child_new = flatten(c_doc, old_props[name]), flatten(d_doc, new_props[name])
        if not comparable(child_old, child_new):
            evidence.append(f"이름은 같지만 견줄 수 없다: {name}"
                            f" (계약 {sorted(type_set(child_old) or [])},"
                            f" 구현 {sorted(type_set(child_new) or [])})")
    dropped = sorted(set(old.get("required") or []) - set(new.get("required") or []))
    if dropped:
        evidence.append(f"필수 보장이 없다: {dropped}")
    return evidence or ["견줄 것이 없다"]


# ── 값 선언의 차이 ─────────────────────────────────────────────────────────
#
# 차이의 종류와 방향을 갈라 둔다. 판정은 차이의 유무가 정하고, 방향은 라벨만 고른다.

NARROWER = "narrower"      # 구현이 계약보다 좁게 선언했다
WIDER = "wider"            # 구현이 계약보다 넓게 선언했다
INCOMPATIBLE = "incompatible"  # 좁고 넓음으로 볼 수 없다

# (방향, 요청인가) → 라벨. 계약은 요청에 무엇을 받고 무엇을 거절하겠다고 둘 다 말했으므로 요청의 두 방향이
# 각각 다른 약속을 깬다. 좁게 받으면 받겠다고 한 것을 거절하고, 넓게 받으면 거절하겠다고 한 것을 받아들인다.
# 응답은 넓어지면 약속한 것을 다 주지 않는 것이고, 좁아지면 대개 무해하다.
LABEL_OF = {
    (NARROWER, True): REJECTS_REQUEST,
    (WIDER, True): ACCEPTS_REJECTED,
    (INCOMPATIBLE, True): REJECTS_REQUEST,
    (NARROWER, False): BREAKS_READER,
    (WIDER, False): WITHHOLDS_RESPONSE,
    (INCOMPATIBLE, False): BREAKS_READER,
}

# 응답에서 좁아져도 계약을 어기지 않는 차이의 종류. 값의 범위와 형식이다.
#
# 구현이 계약보다 좁게 응답하면 계약대로 읽는 쪽은 자기가 받을 수 있다고 믿은 값의 부분집합을 받는다.
# 손해가 없으므로 차이이지만 위반이 아니다. 타입은 여기 없다. 타입이 좁아지면 계약이 그 자리에 올 수 있다고
# 말한 종류 하나가 아예 오지 않게 되어 그 종류를 기다리는 쪽이 부서진다.
#
# 방향은 `value_differences`가 정하고, 거기서 계약이 값을 정하지 않은 자리에 구현이 형식이나 모양을 붙인 것만
# `NARROWER`가 된다. 계약이 정한 형식을 다른 것으로 바꾼 것(`int64` → `int32`)은 `INCOMPATIBLE`이라
# 이 목록에 들어도 걸러지지 않는다. 형식 이름 사이에 넓고 좁음의 순서가 없으므로 그 자리는 판정하지 않고
# 위반으로 남겨 사람과 비평이 보게 한다.
NARROWABLE_IN_RESPONSE = ("형식", "모양", "경계", "열거형")


def narrowed_in_response(kind, way, request):
    """이 차이가 응답에서 값을 좁힌 것인가. 그 자리만 판정이 아니라 기록이다."""
    return (not request) and way == NARROWER and kind in NARROWABLE_IN_RESPONSE


def label_of(kind, way, request):
    """이 차이가 쓰는 쪽에 어떤 뜻인지. 방향만으로는 정해지지 않고 차이의 종류를 함께 본다.

    응답이 좁아지는 자리가 둘로 갈린다. 값의 범위와 형식이 좁아진 것은 계약대로 읽는 쪽이 부분집합을 받으므로
    무해하지만, 타입이 좁아진 것은 계약이 그 자리에 올 수 있다고 말한 종류 하나가 아예 오지 않게 되어 그 종류를
    기다리는 쪽이 부서진다. 그래서 응답의 `NARROWER`에 한 라벨만 달면 한쪽이 틀린다. 무해로 달면 진짜 위반이
    리포트 바닥으로 내려가고, 깨진다고 달면 기록으로 내려간 차이가 위험하게 읽힌다.
    """
    if narrowed_in_response(kind, way, request):
        return HARMLESS
    return LABEL_OF[(way, request)]


def value_differences(old, new):
    """한 자리의 값 선언이 어떻게 다른가. [(종류, 문장, 방향)] 목록."""
    found = []

    old_types, new_types = type_set(old), type_set(new)
    if old_types and new_types and old_types != new_types:
        way = (NARROWER if new_types < old_types
               else WIDER if old_types < new_types else INCOMPATIBLE)
        found.append(("타입", f"계약 {sorted(old_types)} → 구현 {sorted(new_types)}", way))

    old_format, new_format = old.get("format"), new.get("format")
    if (old_format or new_format) and old_format != new_format:
        way = NARROWER if new_format and not old_format else (WIDER if old_format and not new_format
                                                              else INCOMPATIBLE)
        found.append(("형식", f"계약 {old_format} → 구현 {new_format}", way))

    old_pattern, new_pattern = old.get("pattern"), new.get("pattern")
    if (old_pattern or new_pattern) and old_pattern != new_pattern:
        way = NARROWER if new_pattern and not old_pattern else (WIDER if old_pattern and not new_pattern
                                                                else INCOMPATIBLE)
        found.append(("모양", f"계약 {old_pattern} → 구현 {new_pattern}", way))

    for key, direction in TIGHTENED.items():
        before, after = old.get(key), new.get(key)
        if before is None and after is None:
            continue
        if before is None or after is None:
            way = NARROWER if after is not None else WIDER
            found.append(("경계", f"{key}: 계약 {before} → 구현 {after}", way))
            continue
        if before == after:
            continue
        narrowed = (after < before) if direction == "less" else (after > before)
        found.append(("경계", f"{key}: 계약 {before} → 구현 {after}",
                      NARROWER if narrowed else WIDER))

    old_enum, new_enum = old.get("enum"), new.get("enum")
    if old_enum and new_enum:
        gone = [value for value in old_enum if value not in new_enum]
        extra = [value for value in new_enum if value not in old_enum]
        if gone and extra:
            found.append(("열거형", f"계약에 없다 {extra}, 구현에 없다 {gone}", INCOMPATIBLE))
        elif gone:
            found.append(("열거형", f"구현에 없다 {gone}", NARROWER))
        elif extra:
            found.append(("열거형", f"계약에 없다 {extra}", WIDER))
    elif old_enum and not new_enum:
        found.append(("열거형", f"계약은 {old_enum}로 열거했는데 구현은 열거하지 않는다", WIDER))

    old_const, new_const = old.get("const"), new.get("const")
    if old_const is not None and new_const != old_const:
        found.append(("상수", f"계약 {old_const!r} → 구현 {new_const!r}", INCOMPATIBLE))
    return found


def worst(labels):
    """가장 센 라벨. 리포트의 정렬이 이것을 쓴다.

    약속을 깬 것이 계약 밖이 자란 것보다 위에 온다. 계약이 거절하겠다고 한 것을 받는 자리는 계약대로 부르는 쪽이
    부서지지는 않지만 계약이 한 약속 하나가 선언에서 사라진 것이라, 아무 약속도 깨지 않은 초과보다 위다.
    """
    for label in (BREAKS_READER, REJECTS_REQUEST, WITHHOLDS_RESPONSE, ACCEPTS_REJECTED,
                  UNDECLARED, HARMLESS):
        if label in labels:
            return label
    return HARMLESS


# ── 한 자리를 대조한다 ─────────────────────────────────────────────────────

MAX_DEPTH = 12


def compare_place(f, point, where, c_doc, d_doc, c_schema, d_schema, request, depth=0):
    """계약의 한 자리와 구현의 같은 자리를 견준다.

    위에서 아래로 내려가며 설명할 수 있는 자리에서 멈춘다. 그 자리가 계약이 정한 것과 다른 종류면 그 한 건이고
    아래는 증거로 붙는다. 같은 종류면 자식을 보는데, 자식의 값 선언 차이는 자식마다 세지 않고 **그 자식을 담은
    객체에서 차이의 종류마다 한 건**으로 센다. 형식 선언을 열 필드에서 잃은 것은 열 가지 일이 아니라 한 가지 일이고,
    그 문장 하나가 열 자리를 설명한다. 잃은 자리는 증거에 남는다.
    """
    if depth > MAX_DEPTH:
        return
    old = flatten(c_doc, c_schema)
    new = flatten(d_doc, d_schema)
    if not old:
        return

    family = "request" if request else "response"

    reason = different_kind(c_doc, d_doc, old, new)
    if reason:
        closed = old.get("additionalProperties") is False
        f.add(f"{family}.schema_differs", point, where, reason,
              label=REJECTS_REQUEST if request else BREAKS_READER,
              evidence=kind_evidence(c_doc, d_doc, old, new, closed))
        return

    # 이 자리에 모인 차이. 자기 자신의 값 선언과 잎 자식들의 값 선언을 함께 담는다.
    gathered = {}
    for kind, text, way in value_differences(old, new):
        gathered.setdefault(kind, []).append((text, way))

    old_props, new_props = properties_of(old), properties_of(new)
    if old_props or new_props:
        gathered = compare_object(f, point, where, c_doc, d_doc, old, new, request, depth, gathered)

    if isinstance(old.get("items"), dict) and isinstance(new.get("items"), dict):
        compare_place(f, point, f"{where}[]", c_doc, d_doc,
                      old["items"], new["items"], request, depth + 1)

    emit_differences(f, point, where, request, gathered)


def emit_differences(f, point, where, request, gathered):
    """모인 차이를 종류마다 한 건으로 낸다.

    한 종류 안에서 응답을 좁힌 것과 그 밖의 차이가 섞이면 둘로 나눠 낸다. 섞어 한 건으로 내면 판정이 한쪽에
    끌려간다. 좁힌 쪽에 붙이면 같이 들어온 위반이 기록으로 내려가 통과하고, 위반 쪽에 붙이면 무해한 차이가
    REJECT를 만든다. 나누면 각각이 자기 판정을 받고 증거도 자기 쪽에만 남는다.
    """
    differs = "request.input_differs" if request else "response.field_differs"
    for kind in sorted(gathered):
        for narrowed in (False, True):
            entries = [(text, way) for text, way in gathered[kind]
                       if narrowed_in_response(kind, way, request) is narrowed]
            if not entries:
                continue
            rule = "response.value_narrowed" if narrowed else differs
            verb = "좁혔다" if narrowed else "다르다"
            labels = [label_of(kind, way, request) for _, way in entries]
            detail = f"{kind}: " + ("; ".join(text for text, _ in entries) if len(entries) == 1
                                    else f"{len(entries)}곳에서 {verb}")
            f.add(rule, point, where, detail, label=worst(labels),
                  evidence=[f"{kind}: {text}" for text, _ in entries])


def compare_object(f, point, where, c_doc, d_doc, old, new, request, depth, gathered):
    """같은 종류의 객체를 필드 고도에서 견준다. 잎 자식의 값 차이는 이 객체로 모아 올린다."""
    old_props, new_props = properties_of(old), properties_of(new)
    missing_rule = "request.input_missing" if request else "response.field_missing"
    missing_label = REJECTS_REQUEST if request else WITHHOLDS_RESPONSE

    for name in sorted(set(old_props) - set(new_props)):
        f.add(missing_rule, point, f"{where}.{name}", "계약이 선언한 필드다", label=missing_label)

    closed = old.get("additionalProperties") is False
    for name in sorted(set(new_props) - set(old_props)):
        if request:
            f.add("request.input_extra", point, f"{where}.{name}",
                  "계약이 말하지 않은 요청 입력이다", label=UNDECLARED)
        elif closed:
            f.add("response.field_extra_closed", point, f"{where}.{name}",
                  "계약이 닫아 둔 객체다", label=UNDECLARED)
        else:
            f.add("response.field_extra", point, f"{where}.{name}",
                  "계약이 열어 둔 객체다", label=UNDECLARED)

    both = set(old_props) & set(new_props)
    old_required = set(old.get("required") or [])
    new_required = set(new.get("required") or [])
    if request:
        changed = sorted((new_required - old_required) & both)
        if changed:
            gathered.setdefault("필수", []).extend(
                (f"{name}: 계약은 선택으로 두었는데 구현이 필수로 요구한다", NARROWER) for name in changed)
    else:
        changed = sorted((old_required - new_required) & both)
        if changed:
            gathered.setdefault("필수", []).extend(
                (f"{name}: 계약이 항상 담겠다고 보장했는데 구현은 선택이다", WIDER) for name in changed)

    for name in sorted(both):
        child_old = flatten(c_doc, old_props[name])
        child_new = flatten(d_doc, new_props[name])
        if is_leaf(child_old) and is_leaf(child_new):
            reason = different_kind(c_doc, d_doc, child_old, child_new)
            if reason:
                gathered.setdefault("타입", []).append((f"{name}: {reason}", INCOMPATIBLE))
                continue
            for kind, text, way in value_differences(child_old, child_new):
                gathered.setdefault(kind, []).append((f"{name}: {text}", way))
        else:
            compare_place(f, point, f"{where}.{name}", c_doc, d_doc,
                          old_props[name], new_props[name], request, depth + 1)
    return gathered


# ── 추출 스펙의 맹점 ───────────────────────────────────────────────────────

def probe_blind(f, contract, derived, targets, c_ops):
    """계약의 한 조항 갈래에 대응하는 것이 추출 스펙에 아예 없는지 본다. 없으면 그 갈래는 미판정이다."""
    blind = set()

    declares_codes = any(
        response_of(contract, c_ops[name][2], status).get("x-error-codes")
        for name in targets if name in c_ops
        for status in (c_ops[name][2].get("responses") or {})
    )
    if declares_codes and not has_key_anywhere(derived, "x-error-codes"):
        blind.add("x-error-codes")
        f.add("extract.blind", None, "x-error-codes",
              "계약은 (상태 코드, 에러 코드) 쌍을 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다",
              label=UNDECLARED)

    if contract.get("security") is not None and not has_key_anywhere(derived, "security"):
        blind.add("security")
        f.add("extract.blind", None, "security",
              "계약은 인증 요구를 선언했는데 추출 스펙에는 그 조항에 대응하는 것이 없다",
              label=UNDECLARED)

    return blind


# ── 결정 ───────────────────────────────────────────────────────────────────

def decision_node(contract, c_ops, point, where):
    """결정의 좌표가 가리키는 계약의 스키마 노드를 찾는다. 못 찾으면 None."""
    if not point or ":" not in str(point):
        return None
    name, suffix = str(point).split(":", 1)
    if name not in c_ops:
        return None
    operation = c_ops[name][2]
    schema = None
    if suffix.isdigit():
        schema = response_body(response_of(contract, operation, suffix))
    else:
        for status in operation.get("responses") or {}:
            response = response_of(contract, operation, status)
            if suffix in (response.get("x-error-codes") or []):
                schema = response_body(response)
                break
    if schema is None:
        return None
    label = ref_label(schema, "body")
    node = deref(contract, schema)
    if not where:
        return node
    tokens = [t for t in str(where).split(".") if t]
    if tokens and tokens[0] == label:
        tokens = tokens[1:]
    for token in tokens:
        node, _ = unwrap_nullable(node if isinstance(node, dict) else {})
        node = (node.get("properties") or {}).get(token)
        if node is None:
            return None
    node, _ = unwrap_nullable(node if isinstance(node, dict) else {})
    return node


def check_decisions(f, contract, c_ops, decisions, targets):
    """결정이 계약이 정한 자리를 덮어쓰려 하는가.

    Validate가 결정에서 읽는 것은 좌표와 라벨까지다. 근거 문장을 읽으면 설명이 그럴듯한 결정이 통과한다.
    그래서 판정도 좌표로 한다. 계약이 그 자리에 값을 정해 두었으면(열거형·상수·기본값·모양·경계)
    거기는 빈자리가 아니므로 결정이 아니라 판정 대상이다.
    """
    for decision in decisions or []:
        point = decision.get("point")
        where = decision.get("where")
        name = str(point).split(":", 1)[0] if point else None
        if name not in c_ops or (targets and name not in targets):
            f.add("decision.unknown_point", point, where or "-",
                  f"계약의 판정 지점이 아니다 (결정 {decision.get('id')})", label=UNDECLARED)
            continue
        node = decision_node(contract, c_ops, point, where)
        if not isinstance(node, dict):
            continue
        fixed = [key for key in VALUE_FIXING if key in node]
        if fixed:
            f.add("decision.overrides_contract", point, where or "-",
                  f"계약이 그 자리에 {fixed}를 적어 두었다 (결정 {decision.get('id')})",
                  label=UNDECLARED)


# ── 게이트 ─────────────────────────────────────────────────────────────────

def run_g1(derived_spec, contract, scope, decisions, rules, project_dir=None):
    """구현의 추출 스펙과 그 iteration의 계약 판본을 대조한다.

    `project_dir`는 BUILD.md의 시그니처에 없던 인자다. 직렬화 재정의는 추출 스펙에 드러나지 않아
    소스를 읽어야만 잡히는데, 그 하나 때문에 게이트를 나누면 같은 규칙 카드가 두 자리로 흩어진다.
    생성기가 파일 내용을 응답에 담지 않고 작업 폴더를 제자리에서 고치므로 내용이 아니라 그 폴더를 받는다.
    `run_g0`이 빌드하는 폴더와 같은 값이고 인자 이름도 같게 뒀다. 없으면 그 검사만 건너뛴다.
    """
    f = Findings(rules)
    derived_spec = strip_ignored(derived_spec or {}, load_ignored_pointers())
    c_ops = operations(contract or {})
    d_routes = routes(derived_spec or {})
    targets = [name for name in (scope or sorted(c_ops))]

    blind = probe_blind(f, contract or {}, derived_spec or {}, targets, c_ops)

    derived_paths = set((derived_spec or {}).get("paths") or {})
    contract_paths = {c_ops[name][0] for name in targets if name in c_ops}

    for path in sorted(derived_paths - contract_paths):
        f.add("surface.path_extra", None, path, "계약에 없는 경로다", label=UNDECLARED)

    for name in targets:
        if name not in c_ops:
            f.add("extract.blind", name, "operationId",
                  "판정 범위가 계약에 없는 오퍼레이션을 가리킨다", label=UNDECLARED)
            continue
        path, method, operation = c_ops[name]

        if (path, method) not in d_routes:
            if path not in derived_paths:
                f.add("surface.path_missing", name, path, f"{method.upper()} {path}",
                      label=WITHHOLDS_RESPONSE)
            else:
                f.add("request.operation_missing", name, f"{method.upper()} {path}",
                      "경로는 있는데 그 메서드의 오퍼레이션이 없다", label=REJECTS_REQUEST)
            continue
        d_name, d_operation = d_routes[(path, method)]

        if d_name != name:
            # 자리는 맞고 이름이 다르다. 판정 지점 좌표가 식별자이므로 이 자리를 따로 가려야
            # "만들지 않았다"와 구별된다.
            f.add("request.operation_id_mismatch", name, f"{method.upper()} {path}",
                  f"계약이 선언한 식별자 {name!r}, 추출 스펙의 식별자 {d_name!r}", label=UNDECLARED)

        judge_request(f, name, contract, derived_spec, operation, d_operation, blind)
        judge_responses(f, name, contract, derived_spec, operation, d_operation, blind)

    check_serialization(f, project_dir)
    check_decisions(f, contract or {}, c_ops, decisions, set(targets))
    return f.result()


def judge_request(f, name, contract, derived, operation, d_operation, blind):
    if "security" not in blind:
        wanted = effective_security(contract, operation)
        got = effective_security(derived, d_operation)
        if bool(wanted) != bool(got):
            f.add("request.security_differs", name, "security",
                  "계약은 인증을 요구했는데 구현은 요구하지 않는다" if wanted
                  else "계약은 인증을 요구하지 않았는데 구현은 요구한다",
                  label=UNDECLARED if wanted else REJECTS_REQUEST)

    c_params = parameters_of(contract, operation)
    d_params = parameters_of(derived, d_operation)
    for key in sorted(c_params):
        parameter = c_params[key]
        where = f"parameters.{key[1]}.{key[0]}"
        if key not in d_params:
            f.add("request.input_missing", name, where, "계약이 받겠다고 선언한 파라미터다",
                  label=REJECTS_REQUEST)
            continue
        got = d_params[key]
        if got.get("required") and not parameter.get("required"):
            f.add("request.input_differs", name, where,
                  "필수: 계약은 선택으로 두었는데 구현이 필수로 요구한다", label=REJECTS_REQUEST)
        compare_place(f, name, where, contract, derived,
                      parameter.get("schema") or {}, got.get("schema") or {}, request=True)
    for key in sorted(set(d_params) - set(c_params)):
        f.add("request.input_extra", name, f"parameters.{key[1]}.{key[0]}",
              "계약이 말하지 않은 파라미터다", label=UNDECLARED)

    c_has, c_body = body_of(contract, operation)
    d_has, d_body = body_of(derived, d_operation)
    if c_has and not d_has:
        f.add("request.body_missing", name, "requestBody", "계약은 요청 본문을 받겠다고 했다",
              label=REJECTS_REQUEST)
    elif d_has and not c_has:
        f.add("request.input_extra", name, "requestBody", "계약이 말하지 않은 요청 본문이다",
              label=UNDECLARED)
    elif c_has and d_has and c_body is not None and d_body is not None:
        compare_place(f, name, ref_label(c_body, "body"), contract, derived,
                      c_body, d_body, request=True)


def judge_responses(f, name, contract, derived, operation, d_operation, blind):
    c_statuses = list((operation.get("responses") or {}))
    d_statuses = set((d_operation.get("responses") or {}))

    for status in sorted(set(d_statuses) - set(c_statuses)):
        f.add("surface.status_extra", f"{name}:{status}", "responses",
              "계약이 선언하지 않은 상태 코드다", label=UNDECLARED)

    for status in c_statuses:
        point = f"{name}:{status}"
        c_response = response_of(contract, operation, status)
        if status not in d_statuses:
            # 상태 코드가 없으면 그 아래의 본문과 헤더와 에러 코드는 그 한 건이 설명한다.
            f.add("response.status_missing", point, "responses", "계약이 선언한 상태 코드다",
                  label=WITHHOLDS_RESPONSE)
            continue
        d_response = response_of(derived, d_operation, status)

        c_body = response_body(c_response)
        d_body = response_body(d_response)
        if c_body is not None and d_body is None:
            f.add("response.body_missing", point, ref_label(c_body, "body"),
                  "계약이 선언한 응답 본문이 구현에 없다", label=WITHHOLDS_RESPONSE)
        elif c_body is not None and d_body is not None:
            compare_place(f, point, ref_label(c_body, "body"), contract, derived,
                          c_body, d_body, request=False)

        c_headers = set(c_response.get("headers") or {})
        d_headers = set(d_response.get("headers") or {})
        for header in sorted(c_headers - d_headers):
            f.add("response.header_missing", point, f"headers.{header}",
                  "계약이 싣겠다고 선언한 헤더다", label=WITHHOLDS_RESPONSE)
        for header in sorted(d_headers - c_headers):
            f.add("surface.header_extra", point, f"headers.{header}",
                  "계약이 말하지 않은 응답 헤더다", label=UNDECLARED)

        if "x-error-codes" in blind:
            continue
        c_codes = list(c_response.get("x-error-codes") or [])
        d_codes = list(d_response.get("x-error-codes") or [])
        for code in c_codes:
            if code not in d_codes:
                # 빠진 쌍은 판정 지점 하나씩이다. 지점이 다르므로 접지 않는다.
                f.add("error.code_pair_missing", f"{name}:{code}", f"responses.{status}",
                      f"계약이 선언한 쌍이다 ({status}, {code})", label=WITHHOLDS_RESPONSE)
        extra = [code for code in d_codes if code not in c_codes]
        if extra:
            # 선언 밖의 코드는 그 응답 하나의 사실이다. 코드마다 세면 응답 하나가 코드 수만큼 불어난다.
            f.add("error.code_undeclared", point, f"responses.{status}",
                  f"계약의 선언 밖에 있는 코드를 쓴다 {extra}", label=UNDECLARED,
                  evidence=[f"선언 밖: ({status}, {code})" for code in extra])
