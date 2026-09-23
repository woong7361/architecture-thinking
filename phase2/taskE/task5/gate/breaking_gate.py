"""G2 — 계약이 호환을 깨는 방향으로 바뀌었는지 본다.

기준선과 지금 계약을 대조해 깨는 변경을 찾는다. 무엇을 깨는 것으로 볼지는 계약 본문이 정했다.

    요청에 필수를 더한다        받던 요청을 거절하게 된다
    응답에서 필드를 뺀다        읽던 값이 사라진다
    열거형에서 값을 뺀다        분기하던 값이 사라진다 (에러 코드 제거가 여기 든다)
    타입이나 형식을 좁힌다      들어오던 값이 거절되거나 나가던 값이 달라진다

계약은 깨는 변경을 주 버전을 올려야만 할 수 있다고 적었다. 그래서 깨는 변경을 찾으면
주 버전이 함께 올랐는지 본다. 올랐으면 통과시키고 올리지 않았으면 REJECT 한다.

    python phase2/taskE/task5/gate/breaking_gate.py
    python phase2/taskE/task5/gate/breaking_gate.py --baseline <경로> --current <경로>
"""

import argparse
import re
import sys
from pathlib import Path

import yaml

GATE = Path(__file__).resolve().parent
API = GATE.parents[1] / "api"
BASELINE = GATE / "baseline"

# 값이 좁아지면 받던 것을 거절하게 되는 제약. (키, 좁아지는 방향)
TIGHTENED = {
    "maxLength": "less", "maximum": "less", "exclusiveMaximum": "less", "maxItems": "less",
    "minLength": "more", "minimum": "more", "exclusiveMinimum": "more", "minItems": "more",
}


def deref(doc, node, seen=()):
    """$ref 를 따라간다. 순환은 그 자리에서 멈춘다."""
    while isinstance(node, dict) and "$ref" in node:
        ref = node["$ref"]
        if ref in seen:
            return {}
        seen = seen + (ref,)
        target = doc
        for part in ref.lstrip("#/").split("/"):
            target = target.get(part, {}) if isinstance(target, dict) else {}
        node = target
    if isinstance(node, dict):
        return {key: deref(doc, value, seen) for key, value in node.items()}
    if isinstance(node, list):
        return [deref(doc, item, seen) for item in node]
    return node


def type_set(schema):
    value = schema.get("type")
    if value is None:
        return None
    return {value} if isinstance(value, str) else set(value)


def compare_schema(where, old, new, out, request):
    if not isinstance(old, dict) or not isinstance(new, dict):
        return

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

            before_required = {p["name"] for p in operation.get("parameters") or [] if p.get("required")}
            after_required = {p["name"] for p in after.get("parameters") or [] if p.get("required")}
            added = after_required - before_required
            if added:
                out.append(f"{name}: 요청에 필수 파라미터가 늘었다 {sorted(added)}")
            for parameter in after.get("parameters") or []:
                match = next((p for p in operation.get("parameters") or [] if p["name"] == parameter["name"]), None)
                if match:
                    compare_schema(f"{name}.{parameter['name']}", match.get("schema") or {},
                                   parameter.get("schema") or {}, out, request=True)

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


def compare_messages(old_doc, new_doc, out):
    old, new = deref(old_doc, old_doc), deref(new_doc, new_doc)
    old_schemas = ((old.get("components") or {}).get("schemas") or {})
    new_schemas = ((new.get("components") or {}).get("schemas") or {})
    for name, schema in old_schemas.items():
        if name not in new_schemas:
            out.append(f"message.{name}: 스키마가 사라졌다")
            continue
        compare_schema(f"message.{name}", schema, new_schemas[name], out, request=False)


def major(doc, url_key="servers"):
    """HTTP 는 경로의 /v1 이 주 버전이고 메시지는 schemaVersion 이 그 일을 한다."""
    servers = doc.get(url_key) or []
    if servers and isinstance(servers, list) and isinstance(servers[0], dict):
        found = re.search(r"/v(\d+)", servers[0].get("url", ""))
        if found:
            return found.group(1)
    version = str((doc.get("info") or {}).get("version", ""))
    return version.split(".")[0]


def schema_major(doc):
    node = ((doc.get("components") or {}).get("schemas") or {}).get("SchemaVersion") or {}
    return str(node.get("const") or node.get("minimum") or "")


def run(pairs):
    failures = []
    for label, baseline_path, current_path, compare, version_of in pairs:
        if not baseline_path.exists():
            print(f"[G2] 기준선이 없다: {baseline_path}")
            return 1
        old = yaml.safe_load(baseline_path.read_text(encoding="utf-8"))
        new = yaml.safe_load(current_path.read_text(encoding="utf-8"))
        breaking = []
        compare(old, new, breaking)
        bumped = version_of(old) != version_of(new)
        print(f"{label}: 깨는 변경 {len(breaking)}건, 주 버전 {version_of(old)} → {version_of(new)}")
        for line in breaking:
            print(f"  - {line}")
        if breaking and not bumped:
            failures.append(f"[G2] {label}: 주 버전을 올리지 않고 호환을 깼다 ({len(breaking)}건)")
    for failure in failures:
        print(failure)
    return 1 if failures else 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, default=None)
    parser.add_argument("--current", type=Path, default=None)
    args = parser.parse_args()

    if args.baseline and args.current:
        return run([("계약", args.baseline, args.current, compare_http, major)])
    return run([
        ("HTTP 계약", BASELINE / "tennis-alert-api.yaml", API / "tennis-alert-api.yaml", compare_http, major),
        ("메시지 계약", BASELINE / "notification-asyncapi.yaml", API / "notification-asyncapi.yaml",
         compare_messages, schema_major),
    ])


if __name__ == "__main__":
    sys.exit(main())
