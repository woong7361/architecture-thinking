"""대조기. `phase2/taskE/task5/gate/breaking_gate.py`에서 가져왔다.

가져온 것은 `deref`, `type_set`, `TIGHTENED`, `compare_schema`, `body_schema`, `response_schema`,
`compare_http`, `major`다. 원본은 계약의 두 판본을 비교하는 게이트이고, 이 하네스는 그것을 두 방향으로 쓴다.

- **계약 판본 대조** (`pipeline/gates/changes.py`): 원본의 판정을 그대로 쓴다. 판본 사이에서는 요청이
  좁아지는 것과 응답이 사라지는 것이 모두 호환을 깨는 변경이므로 원본의 방향 판정이 맞다.
- **계약과 구현 대조** (`pipeline/gates/g1.py`): 대조기만 가져오고 판정표는 새로 쓴다. 구현은 계약보다
  좁은 응답 값을 줄 수 있고(열거형·범위·형식) 계약보다 넓은 요청을 받을 수 있다. 원본을 그대로 쓰면
  `열거형에서 값이 사라졌다`가 응답에서도 위반이 되어 계약을 만족하는 구현을 떨어뜨린다.

그래서 원본의 `compare_schema`는 문자열 목록을 쌓지만 여기서는 규칙 id가 붙은 항목을 쌓는 함수를
G1이 따로 쓴다. 이 파일은 원본 그대로의 동작이 필요한 자리(판본 대조)를 위해 남는다.

원본에 없던 것이 둘이다.

- 계약이 널 허용을 `oneOf: [X, {type: 'null'}]`로 적으므로 그 모양을 X로 펴 주는 `unwrap_nullable`.
  원본은 `oneOf`를 따라 들어가지 않아 그 아래가 대조되지 않는다.
- 파라미터의 짝을 `p["name"]`으로 맞추지 않는다. `$ref`로 쓴 파라미터에는 `name`이 없어서 원본은 그 자리에서
  `KeyError`로 죽고, `p.get("name")`으로 고치면 짝을 못 찾아 그 파라미터의 대조가 조용히 빠진다. 그래서
  `$ref`를 먼저 풀어 이름을 얻고, 풀지 못했으면 참조 문자열로 짝을 맞추고 풀지 못했다는 사실을 적는다.
  같이 딸려 온 두 공백도 닫았다. 경로 항목의 공용 파라미터를 합치고, 사라진 파라미터를 센다.
"""

# 값이 좁아지면 받던 것을 거절하게 되는 제약. (키, 좁아지는 방향)
TIGHTENED = {
    "maxLength": "less", "maximum": "less", "exclusiveMaximum": "less", "maxItems": "less",
    "minLength": "more", "minimum": "more", "exclusiveMinimum": "more", "minItems": "more",
}

METHODS = ("get", "put", "post", "delete", "patch", "head", "options", "trace")


# 풀지 못한 참조를 남기는 자리. 원본은 그 자리를 빈 객체로 만들어 지웠고, 그러면 그 아래가 대조에서
# 조용히 빠진다. 게이트 공백은 통과가 아니므로 참조를 남겨 대조기가 그 사실을 말할 수 있게 한다.
UNRESOLVED = "x-unresolved-ref"


def resolve_pointer(doc, ref):
    """문서 안의 `$ref`를 따라간다. 닿지 못하면 None. 빈 객체와 없는 것을 구별해야 하므로 None을 쓴다."""
    if not isinstance(ref, str) or not ref.startswith("#"):
        return None
    target = doc
    for part in ref[1:].lstrip("/").split("/"):
        if part == "":
            continue
        part = part.replace("~1", "/").replace("~0", "~")
        if isinstance(target, dict) and part in target:
            target = target[part]
        elif isinstance(target, list) and part.isdigit() and int(part) < len(target):
            target = target[int(part)]
        else:
            return None
    return target


def deref(doc, node, seen=()):
    """$ref 를 따라간다. 순환은 그 자리에서 멈추고, 닿지 못한 참조는 표시를 남긴다.

    닿지 못한 참조를 빈 객체로 만들면 그 아래가 대조에서 사라진다. 계약이 컴포넌트를 지우거나 이름을
    바꿨는데 경로가 아직 그것을 가리키고 있을 때 실제로 그 일이 일어나고, 대조기는 아무 말도 하지 않는다.
    그 자리를 표시로 남기면 `compare_http`가 "대조하지 못했다"고 적을 수 있다.
    """
    while isinstance(node, dict) and "$ref" in node:
        ref = node["$ref"]
        if ref in seen:
            return {}
        seen = seen + (ref,)
        target = resolve_pointer(doc, ref)
        if target is None:
            return {UNRESOLVED: ref}
        node = target
    if isinstance(node, dict):
        return {key: deref(doc, value, seen) for key, value in node.items()}
    if isinstance(node, list):
        return [deref(doc, item, seen) for item in node]
    return node


def parameter_key(parameter):
    """파라미터의 짝을 맞출 열쇠.

    `$ref`로 쓴 파라미터에는 `name`이 없다. OpenAPI에서 합법이고 이 계약도 그렇게 쓴다. 그래서 짝은
    풀린 이름으로 맞추고, 풀지 못했으면 참조 문자열로 맞춘다. 이름을 얻지 못했다고 건너뛰면 그 파라미터의
    대조가 조용히 빠져 게이트 공백이 된다.
    """
    if not isinstance(parameter, dict):
        return None
    if parameter.get("name"):
        return str(parameter["name"])
    ref = parameter.get(UNRESOLVED) or parameter.get("$ref")
    return f"$ref {ref}" if ref else None


def parameters_by_key(doc, path_item, operation):
    """{열쇠: 파라미터}. 경로 항목의 공용 파라미터를 오퍼레이션의 것과 합친다.

    공용 파라미터를 빼면 경로 항목에 적힌 파라미터가 대조에서 빠진다. 이름이 겹치면 오퍼레이션의 것이 이긴다.
    """
    found = {}
    for source in ((path_item or {}).get("parameters") or [], operation.get("parameters") or []):
        for parameter in source:
            resolved = deref(doc, parameter)
            key = parameter_key(resolved)
            if key:
                found[key] = resolved
    return found


def type_set(schema):
    value = schema.get("type")
    if value is None:
        return None
    return {value} if isinstance(value, str) else set(value)


def unwrap_nullable(schema):
    """널 허용의 세 표기를 같게 만들고 (널을 뺀 스키마, 널 허용 여부)를 돌려준다.

    같은 약속이 문서 판본에 따라 다르게 적힌다. 계약은 OpenAPI 3.1의 `type: [X, 'null']`과
    `oneOf: [{$ref: X}, {type: 'null'}]`을 쓰고, springdoc의 3.0.1 출력은 같은 것을 `nullable: true`로 낸다.
    정규화하지 않으면 널 허용 필드마다 타입이 다르다고 잡혀 헛 REJECT가 쏟아진다. 표기 목록은
    `rules/consumed_keys.yaml`의 `nullable_notations`에 있다.

    널 자체의 판정, 곧 키가 응답에 실제로 실리는지는 실물 응답을 보는 G3의 몫이라 여기서는 펴기만 한다.
    원본 대조기는 `oneOf`를 따라 들어가지 않아 그 아래가 대조되지 않았다. 그것도 여기서 펴진다.
    """
    if not isinstance(schema, dict):
        return schema, False

    nullable = schema.get("nullable") is True
    node = schema

    for key in ("oneOf", "anyOf"):
        branches = node.get(key)
        if not isinstance(branches, list):
            continue
        nulls = [b for b in branches if isinstance(b, dict) and b.get("type") == "null"]
        rest = [b for b in branches if isinstance(b, dict) and b.get("type") != "null"]
        if nulls and len(rest) == 1:
            merged = {k: v for k, v in node.items() if k != key}
            merged.update(rest[0])
            node, nullable = merged, True
            break

    value = node.get("type")
    if isinstance(value, list) and "null" in value:
        rest = [t for t in value if t != "null"]
        node = dict(node)
        if len(rest) == 1:
            node["type"] = rest[0]
        elif rest:
            node["type"] = rest
        else:
            node.pop("type", None)
        nullable = True

    if nullable and node.get("nullable") is True:
        node = {k: v for k, v in node.items() if k != "nullable"}
    return node, nullable


def compare_schema(where, old, new, out, request):
    """원본의 판정을 그대로 쓴다. 판본 대조용이다."""
    if not isinstance(old, dict) or not isinstance(new, dict):
        return
    old, old_nullable = unwrap_nullable(old)
    new, new_nullable = unwrap_nullable(new)

    # 닿지 못한 참조는 그 아래가 비어 보인다. 조용히 지나가면 그 응답의 필드 전부가 대조되지 않은 채
    # 통과하므로, 대조하지 못했다고 적고 멈춘다.
    unresolved = new.get(UNRESOLVED) or old.get(UNRESOLVED)
    if unresolved:
        side = "이번 판본" if new.get(UNRESOLVED) else "앞 판본"
        out.append(f"{where}: {side}의 참조를 풀지 못해 대조하지 못했다 {unresolved}")
        return

    # 정규화가 타입 집합에서 널을 뺐으므로 널 허용이 사라진 것을 여기서 따로 센다.
    # 판본 대조에서 널 허용을 없애는 것은 받던 값을 거절하게 되는 변경이다.
    if old_nullable and not new_nullable:
        out.append(f"{where}: 널 허용이 사라졌다")

    old_types, new_types = type_set(old), type_set(new)
    if old_types and new_types and not old_types <= new_types:
        out.append(f"{where}: 타입이 좁아졌다 {sorted(old_types)} → {sorted(new_types)}")

    if old.get("format") != new.get("format") and new.get("format"):
        out.append(f"{where}: 형식이 좁아졌다 {old.get('format')} → {new['format']}")

    if "pattern" in new and new.get("pattern") != old.get("pattern"):
        out.append(f"{where}: 값의 모양을 좁혔다 {old.get('pattern')} → {new['pattern']}")

    for key, direction in TIGHTENED.items():
        before, after = old.get(key), new.get(key)
        if before is None or after is None:
            continue
        if (after < before) if direction == "less" else (after > before):
            out.append(f"{where}: {key} 가 좁아졌다 {before} → {after}")

    old_enum, new_enum = old.get("enum"), new.get("enum")
    if old_enum and new_enum:
        gone = [value for value in old_enum if value not in new_enum]
        if gone:
            out.append(f"{where}: 열거형에서 값이 사라졌다 {gone}")

    old_props = old.get("properties") or {}
    new_props = new.get("properties") or {}
    if request:
        added = set(new.get("required") or []) - set(old.get("required") or [])
        if added:
            out.append(f"{where}: 요청에 필수가 늘었다 {sorted(added)}")
    else:
        for name in old_props:
            if name not in new_props:
                out.append(f"{where}: 응답에서 필드가 사라졌다 {name}")
        dropped = set(old.get("required") or []) - set(new.get("required") or [])
        if dropped:
            out.append(f"{where}: 응답에서 필수 보장이 사라졌다 {sorted(dropped)}")

    for name, old_child in old_props.items():
        if name in new_props:
            compare_schema(f"{where}.{name}", old_child, new_props[name], out, request)

    if isinstance(old.get("items"), dict) and isinstance(new.get("items"), dict):
        compare_schema(f"{where}[]", old["items"], new["items"], out, request)


def body_schema(operation):
    content = ((operation.get("requestBody") or {}).get("content") or {})
    for media in content.values():
        return media.get("schema") or {}
    return {}


def response_schema(response):
    for media in (response.get("content") or {}).values():
        return media.get("schema") or {}
    return {}


def compare_http(old_doc, new_doc, out):
    """원본 그대로. 계약 판본 사이의 깨는 변경을 찾는다."""
    old, new = deref(old_doc, old_doc), deref(new_doc, new_doc)
    for path, methods in (old.get("paths") or {}).items():
        for method, operation in methods.items():
            if not isinstance(operation, dict) or "operationId" not in operation:
                continue
            name = operation["operationId"]
            after = ((new.get("paths") or {}).get(path) or {}).get(method)
            if not isinstance(after, dict):
                out.append(f"{name}: 오퍼레이션이 사라졌다 ({method.upper()} {path})")
                continue

            before_params = parameters_by_key(old, methods, operation)
            after_params = parameters_by_key(new, (new.get("paths") or {}).get(path) or {}, after)

            added = {key for key, p in after_params.items() if p.get("required")} \
                - {key for key, p in before_params.items() if p.get("required")}
            if added:
                out.append(f"{name}: 요청에 필수 파라미터가 늘었다 {sorted(added)}")

            for key, parameter in after_params.items():
                if UNRESOLVED in parameter:
                    # 이 자리는 대조하지 못했다. 건너뛰면 그 사실이 어디에도 남지 않는다.
                    out.append(f"{name}: 파라미터 참조를 풀지 못해 대조하지 못했다 {parameter[UNRESOLVED]}")
                    continue
                match = before_params.get(key)
                if match and UNRESOLVED not in match:
                    compare_schema(f"{name}.{key}", match.get("schema") or {},
                                   parameter.get("schema") or {}, out, request=True)
            for key, parameter in before_params.items():
                if key not in after_params:
                    out.append(f"{name}: 선언했던 파라미터가 사라졌다 {key}")

            compare_schema(f"{name}.body", body_schema(operation), body_schema(after), out, request=True)

            for status, response in (operation.get("responses") or {}).items():
                target = (after.get("responses") or {}).get(status)
                if target is None:
                    out.append(f"{name}: 선언했던 응답이 사라졌다 {status}")
                    continue
                compare_schema(f"{name}.{status}", response_schema(response), response_schema(target), out, request=False)
                gone = [c for c in response.get("x-error-codes") or [] if c not in (target.get("x-error-codes") or [])]
                if gone:
                    out.append(f"{name}.{status}: 선언했던 에러 코드가 사라졌다 {gone}")


def major(doc, url_key="servers"):
    """HTTP 는 경로의 /v1 이 주 버전이고 메시지는 schemaVersion 이 그 일을 한다."""
    import re
    servers = doc.get(url_key) or []
    if servers and isinstance(servers, list) and isinstance(servers[0], dict):
        found = re.search(r"/v(\d+)", servers[0].get("url", ""))
        if found:
            return found.group(1)
    version = str((doc.get("info") or {}).get("version", ""))
    return version.split(".")[0]
