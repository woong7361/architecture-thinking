"""Cross-document consistency checks for the two API contracts.

The schema linters only see one file each. These checks compare the contracts
against the requirements document, against each other, and against the error
table in the answer, so that a mismatch between documents fails instead of
being noticed by eye.

A failure is tagged with the axis it belongs to: A for source traceability,
B for schema agreement, C for the error scheme, G for documentation, and
H for message delivery.

Run all three after editing a contract:

    npx --yes @redocly/cli@latest lint --config phase2/taskE/api/redocly.yaml         phase2/taskE/api/tennis-alert-api.yaml
    npx --yes @asyncapi/cli@latest validate phase2/taskE/api/notification-asyncapi.yaml
    python phase2/taskE/api/check_contract.py
"""

import re
import sys
from datetime import datetime
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator, FormatChecker



ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "api" / "tennis-alert-api.yaml"
ASYNC = ROOT / "api" / "notification-asyncapi.yaml"
PLAN = ROOT / "tennis-court-service-plan.md"
ANSWER = ROOT / "assignments" / "taskE-4.md"

# `\b` would not match at the end of "EF-3이": a Korean particle is a word
# character too, so an id followed by one has to be recognised explicitly.
REQUIREMENT_ID = re.compile(r"(?<![A-Za-z0-9])((?:FR|NFR|EF|V)-\d+)(?!\d)")

# jsonschema checks a format only when its optional dependency is installed, and
# it silently passes everything otherwise. Both contracts fix these two shapes
# themselves, so they are checked here rather than left to the environment.
RFC3339_UTC = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z$")
ABSOLUTE_URI = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.\-]*://\S+$")

FORMATS = FormatChecker()


@FORMATS.checks("date-time")
def is_rfc3339_utc(value):
    if not isinstance(value, str):
        return True
    if not RFC3339_UTC.match(value):
        return False
    try:
        datetime.strptime(value[:19], "%Y-%m-%dT%H:%M:%S")
    except ValueError:
        return False
    return True


@FORMATS.checks("uri")
def is_absolute_uri(value):
    return not isinstance(value, str) or bool(ABSOLUTE_URI.match(value))

failures = []
checks_run = 0


def check(item, ok, message):
    global checks_run
    checks_run += 1
    if not ok:
        failures.append(f"[{item}] {message}")


def walk(node, path=()):
    """Yield every (path, mapping) pair in the document."""
    if isinstance(node, dict):
        yield path, node
        for key, value in node.items():
            yield from walk(value, path + (str(key),))
    elif isinstance(node, list):
        for index, value in enumerate(node):
            yield from walk(value, path + (str(index),))


def resolve_ref(doc, ref):
    if not ref.startswith("#/"):
        return None
    node = doc
    for part in ref[2:].split("/"):
        part = part.replace("~1", "/").replace("~0", "~")
        if not isinstance(node, dict) or part not in node:
            return None
        node = node[part]
    return node


def load():
    doc = yaml.safe_load(CONTRACT.read_text(encoding="utf-8"))
    events = yaml.safe_load(ASYNC.read_text(encoding="utf-8"))
    plan = PLAN.read_text(encoding="utf-8")
    answer = ANSWER.read_text(encoding="utf-8") if ANSWER.exists() else ""
    return doc, events, plan, answer


def inline_refs(doc, node, seen=()):
    """Replace every local reference with its target so the schema stands alone."""
    if isinstance(node, list):
        return [inline_refs(doc, item, seen) for item in node]
    if not isinstance(node, dict):
        return node
    ref = node.get("$ref")
    if isinstance(ref, str):
        if ref in seen:
            return {}
        return inline_refs(doc, resolve_ref(doc, ref) or {}, seen + (ref,))
    return {key: inline_refs(doc, value, seen) for key, value in node.items()}


def validator_for(doc, schema):
    return Draft202012Validator(inline_refs(doc, schema), format_checker=FORMATS)


def collect_examples(doc, media, where):
    """Return (label, value) pairs declared under one media type object."""
    found = []
    if "example" in media:
        found.append((where, media["example"]))
    for name, wrapper in (media.get("examples") or {}).items():
        if isinstance(wrapper, dict) and "value" in wrapper:
            found.append((f"{where}#{name}", wrapper["value"]))
    return found


def check_examples(doc):
    """B2: every example satisfies the schema it is declared under."""
    for path, node in walk(doc):
        if not isinstance(node, dict) or "schema" not in node:
            continue
        if not any(p in ("content",) for p in path):
            continue
        schema = node["schema"]
        where = "/".join(path)
        for label, value in collect_examples(doc, node, where):
            errors = sorted(validator_for(doc, schema).iter_errors(value), key=str)
            check(
                "B2",
                not errors,
                f"example does not satisfy its schema at {label}: "
                + "; ".join(f"{list(e.absolute_path)}: {e.message}" for e in errors[:3]),
            )


def check_enum_documentation(doc):
    """B4: every enum documents each of its values."""
    for path, node in walk(doc):
        if not isinstance(node, dict) or "enum" not in node:
            continue
        where = "/".join(path)
        description = node.get("description", "")
        missing = [v for v in node["enum"] if isinstance(v, str) and f"`{v}`" not in description]
        # Enums reused through $ref are documented at their definition site.
        if not description and where.startswith("paths"):
            continue
        check("B4", not missing, f"enum values without a description at {where}: {missing}")


def check_refs(doc):
    """B7: no dangling reference and no unused component."""
    refs = set()
    for path, node in walk(doc):
        if isinstance(node, dict) and isinstance(node.get("$ref"), str):
            refs.add(node["$ref"])
    for ref in sorted(refs):
        check("B7", resolve_ref(doc, ref) is not None, f"dangling reference: {ref}")

    security_schemes = {name for entry in doc.get("security", []) for name in entry}
    for section, entries in (doc.get("components") or {}).items():
        for name in entries:
            if section == "securitySchemes":
                check("B7", name in security_schemes, f"unused security scheme: {name}")
                continue
            ref = f"#/components/{section}/{name}"
            check("B7", ref in refs, f"unused component: {ref}")


def operations(doc):
    methods = {"get", "put", "post", "delete", "patch", "head", "options", "trace"}
    for path, item in (doc.get("paths") or {}).items():
        for method, operation in item.items():
            if method in methods:
                yield f"{method.upper()} {path}", operation


def check_error_codes(doc):
    """C1 and C2: declared codes, response codes and examples agree."""
    declared = set(doc["components"]["schemas"]["ErrorCode"]["enum"])
    documented = set(re.findall(r"- `([A-Z][A-Z0-9_]+)`", doc["components"]["schemas"]["ErrorCode"]["description"]))
    check("C1", declared == documented, f"ErrorCode enum and its description differ: {declared ^ documented}")

    used = set()
    for name, response in (doc["components"].get("responses") or {}).items():
        codes = response.get("x-error-codes")
        check("C2", bool(codes), f"response component without x-error-codes: {name}")
        for code in codes or []:
            check("C2", code in declared, f"unknown error code {code} in response {name}")
            used.add(code)
        for media in (response.get("content") or {}).values():
            for label, value in collect_examples(doc, media, name):
                check(
                    "C2",
                    value.get("code") in (codes or []),
                    f"example code {value.get('code')} is not declared by response {label}",
                )
                check(
                    "C5",
                    ("retryAfterSeconds" in value) <= bool(value.get("retryable")),
                    f"retryAfterSeconds on a non-retryable example at {label}",
                )
    check("C1", declared == used, f"declared error codes never used in a response: {sorted(declared - used)}")

    for name, operation in operations(doc):
        for status, response in (operation.get("responses") or {}).items():
            if not status.startswith(("4", "5")):
                continue
            target = resolve_ref(doc, response["$ref"]) if "$ref" in response else response
            for media in (target.get("content") or {}).values():
                for label, value in collect_examples(doc, media, f"{name} {status}"):
                    check(
                        "C2",
                        str(value.get("status")) == status,
                        f"example status {value.get('status')} does not match response status {status} at {label}",
                    )


def check_constrained_params(doc):
    """C2: an operation whose parameters can fail validation must declare 400."""
    for name, operation in operations(doc):
        constrained = []
        for param in operation.get("parameters") or []:
            target = resolve_ref(doc, param["$ref"]) if "$ref" in param else param
            schema = inline_refs(doc, target.get("schema") or {})
            if set(schema) - {"type", "description", "examples", "example"}:
                constrained.append(target.get("name"))
        if constrained:
            check(
                "C2",
                "400" in (operation.get("responses") or {}),
                f"{name} constrains {constrained} but declares no 400 response",
            )


def check_retry_hint(doc):
    """C4: a retryable failure always says how long to wait."""
    for name, response in (doc["components"].get("responses") or {}).items():
        for media in (response.get("content") or {}).values():
            for label, value in collect_examples(doc, media, name):
                if value.get("retryable"):
                    check("C4", "retryAfterSeconds" in value, f"retryable example without retryAfterSeconds at {label}")
                    check("C4", "Retry-After" in (response.get("headers") or {}), f"retryable response without a Retry-After header: {name}")


def check_trace_id(doc):
    """C8: a failure the client cannot fix carries a trace id, one it can fix does not."""
    for name, response in (doc["components"].get("responses") or {}).items():
        for media in (response.get("content") or {}).values():
            for label, value in collect_examples(doc, media, name):
                status = str(value.get("status", ""))
                if status.startswith("5"):
                    check("C8", "traceId" in value, f"5xx example without traceId at {label}")
                elif status.startswith("4"):
                    check("C8", "traceId" not in value, f"4xx example carries traceId at {label}")


# A concept the two contracts share, and where each one defines it. The message
# schema names a property; the HTTP contract names either a component schema or
# a property of one.
NOTIFICATION = "AlertNotification"

SHARED_CONCEPTS = {
    "alertId": ("schema", "AlertId"),
    "courtId": ("schema", "CourtId"),
    "courtName": ("schema", "CourtName"),
    "slot": ("schema", "TimeSlot"),
    "date": ("property", "Alert", "date"),
    "reservationUrl": ("property", "Alert", "reservationUrl"),
    "expiresAt": ("property", "Alert", "expiresAt"),
    "confirmedAt": ("property", "CourtAvailability", "confirmedAt"),
}

DOCUMENTATION_KEYS = {"description", "example", "examples", "title", "default"}


def fingerprint(node):
    """Strip prose from a schema so only its constraints remain comparable."""
    if isinstance(node, dict):
        return {
            key: fingerprint(value)
            for key, value in sorted(node.items())
            if key not in DOCUMENTATION_KEYS and not key.startswith("x-")
        }
    if isinstance(node, list):
        return [fingerprint(item) for item in node]
    return node


def contract_side(doc, target):
    schemas = doc["components"]["schemas"]
    if target[0] == "schema":
        return inline_refs(doc, schemas.get(target[1]) or {})
    owner = inline_refs(doc, schemas.get(target[1]) or {})
    return (owner.get("properties") or {}).get(target[2]) or {}


def message_payload(events, name):
    """Return the payload schema of one message with its references inlined."""
    message = (events["components"]["messages"] or {}).get(name) or {}
    payload = message.get("payload") or {}
    schema = payload.get("schema") if "schema" in payload else payload
    return inline_refs(events, schema or {})


def check_message_schema(doc, events):
    """B2, B3 and B5 across the two contracts."""
    for name, message in (events["components"]["messages"] or {}).items():
        schema = message_payload(events, name)
        check("B3", schema.get("additionalProperties") is False, f"message {name} accepts unknown fields")
        properties = schema.get("properties") or {}
        for field in schema.get("required") or []:
            check("B3", field in properties, f"message {name} requires an undeclared field: {field}")

        examples = message.get("examples") or []
        check("B2", bool(examples), f"message {name} declares no example")
        validator = Draft202012Validator(schema, format_checker=FORMATS)
        for index, example in enumerate(examples):
            errors = sorted(validator.iter_errors(example.get("payload")), key=str)
            check(
                "B2",
                not errors,
                f"example {index} of message {name} does not satisfy its payload schema: "
                + "; ".join(f"{list(e.absolute_path)}: {e.message}" for e in errors[:3]),
            )

    notification = message_payload(events, NOTIFICATION)
    properties = notification.get("properties") or {}
    for name, target in SHARED_CONCEPTS.items():
        mine = properties.get(name)
        check("B5", mine is not None, f"the notification message is missing the shared concept {name}")
        if mine is None:
            continue
        theirs = contract_side(doc, target)
        check(
            "B5",
            fingerprint(mine) == fingerprint(theirs),
            f"{name} is shaped differently in the two contracts: "
            f"{fingerprint(mine)} vs {fingerprint(theirs)}",
        )


# What a message contract has to say that no schema can enforce. The contract
# tags each guarantee with the item it answers, and every item needs an answer.
REQUIRED_CHECKLIST_ITEMS = {
    "H2": "whether the same message can arrive twice, and what the receiver filters on",
    "H3": "whether arrival order is guaranteed",
    "H4": "when a message stops being useful, and what the receiver does then",
    "H5": "how far a sent mark actually reaches",
}

CORRELATION_LOCATION = re.compile(r"^\$message\.payload#/(.+)$")


def check_async_refs(events):
    """B7: no dangling reference and no unused definition in the message contract."""
    refs = set()
    for path, node in walk(events):
        if isinstance(node, dict) and isinstance(node.get("$ref"), str):
            refs.add(node["$ref"])
    for ref in sorted(refs):
        check("B7", resolve_ref(events, ref) is not None, f"dangling reference: {ref}")
    for section, entries in (events.get("components") or {}).items():
        for name in entries:
            ref = f"#/components/{section}/{name}"
            check("B7", ref in refs, f"unused component: {ref}")


def check_async_contract(events, plan):
    """H1 to H6: what a schema alone cannot say has a declared place here."""
    plan_ids = set(REQUIREMENT_ID.findall(plan))
    channels = events.get("channels") or {}
    check("H1", bool(channels), "the message contract declares no channel")

    for name, channel in channels.items():
        messages = channel.get("messages") or {}
        check("H1", bool(messages), f"channel {name} carries no message")
        for label, ref in messages.items():
            check("B7", resolve_ref(events, ref.get("$ref", "")) is not None,
                  f"dangling message reference at channel {name}/{label}")

    # H1: every hop says who acts, in which direction, and on which channel.
    for name, operation in (events.get("operations") or {}).items():
        check("H1", operation.get("action") in ("send", "receive"),
              f"operation {name} declares no action")
        ref = (operation.get("channel") or {}).get("$ref", "")
        check("H1", resolve_ref(events, ref) is not None,
              f"operation {name} points at an unknown channel: {ref}")
        check("A1", bool(operation.get("x-requirement")),
              f"operation without x-requirement: {name}")
        for message in operation.get("messages") or []:
            check("B7", resolve_ref(events, message.get("$ref", "")) is not None,
                  f"dangling message reference at operation {name}")

    for name, message in (events["components"]["messages"] or {}).items():
        location = (message.get("correlationId") or {}).get("location", "")
        match = CORRELATION_LOCATION.match(location)
        check("H2", bool(match), f"message {name} declares no correlation id in its payload: {location!r}")
        if match:
            fields = (message_payload(events, name).get("properties") or {})
            check("H2", match.group(1) in fields,
                  f"the correlation id of message {name} points at a field it does not have: {match.group(1)}")

    guarantees = events["info"].get("x-delivery-guarantee") or []
    check("H1", bool(guarantees), "the contract states no delivery guarantee")
    ids = [g.get("id") for g in guarantees]
    check("H1", len(ids) == len(set(ids)), f"delivery guarantees share an id: {ids}")
    for guarantee in guarantees:
        where = guarantee.get("id")
        check("G5", bool((guarantee.get("summary") or "").strip()),
              f"delivery guarantee without a summary: {where}")
        cited = guarantee.get("requirement") or []
        # A guarantee the requirements did not decide has to say why the contract did.
        check(
            "H7",
            bool(cited) != bool((guarantee.get("rationale") or "").strip()),
            f"delivery guarantee {where} must cite a requirement or give the reason the contract decided it",
        )
        for rid in cited:
            check("A1", rid in plan_ids, f"unknown requirement id {rid} in delivery guarantee {where}")

    promised = set()
    for guarantee in guarantees:
        channel = guarantee.get("channel")
        check("H1", channel in channels,
              f"delivery guarantee {guarantee.get('id')} names an unknown channel: {channel}")
        promised.add(channel)
    silent = sorted(set(channels) - promised)
    check("H1", not silent, f"channels with no delivery guarantee: {silent}")

    answered = {g.get("x-checklist") for g in guarantees}
    missing = sorted(set(REQUIRED_CHECKLIST_ITEMS) - answered)
    check(
        "H1",
        not missing,
        "delivery rules with no guarantee answering them: "
        + "; ".join(f"{item} ({REQUIRED_CHECKLIST_ITEMS[item]})" for item in missing),
    )

    rules = events["info"].get("x-consumer-rule") or []
    check("H6", bool(rules), "the contract lists nothing the receiver has to do")
    fields = (message_payload(events, NOTIFICATION).get("properties") or {})
    for rule in rules:
        check("H6", bool((rule.get("rule") or "").strip()), f"empty consumer rule: {rule}")
        field = rule.get("field")
        check("H6", field is None or field in fields,
              f"consumer rule names a field the message does not have: {field}")
        for rid in rule.get("requirement") or []:
            check("A1", rid in plan_ids, f"unknown requirement id {rid} in a consumer rule")


def check_answer_table(doc, answer):
    """C1: the error table in the answer lists exactly the declared codes."""
    if "| 코드" not in answer and "|코드" not in answer:
        check("C1", False, "no error code table found in the answer document")
        return
    declared = set(doc["components"]["schemas"]["ErrorCode"]["enum"])
    table = set()
    for line in answer.splitlines():
        if line.startswith("|") and "`" in line:
            table.update(re.findall(r"`([A-Z][A-Z0-9_]{3,})`", line))
    check("C1", table == declared, f"answer table and contract differ: {sorted(table ^ declared)}")


def check_traceability(doc, events, plan):
    """A1 and A2: every operation cites a requirement, every requirement lands."""
    plan_ids = set(REQUIREMENT_ID.findall(plan))
    check("A1", bool(plan_ids), "no requirement ids found in the plan")

    cited = set()
    # A requirement may be realised by either contract, so both are searched.
    for source, label in ((doc, "http"), (events, "message")):
        for path, node in walk(source):
            if isinstance(node, dict) and isinstance(node.get("x-requirement"), list):
                for rid in node["x-requirement"]:
                    check("A1", rid in plan_ids, f"unknown requirement id {rid} at {label}:{'/'.join(path)}")
                    cited.add(rid)
    for guarantee in events["info"].get("x-delivery-guarantee") or []:
        cited.update(guarantee.get("requirement") or [])
    for rule in events["info"].get("x-consumer-rule") or []:
        cited.update(rule.get("requirement") or [])

    for name, operation in operations(doc):
        check("A1", bool(operation.get("x-requirement")), f"operation without x-requirement: {name}")

    out_of_scope = {}
    for entry in doc["info"].get("x-out-of-scope") or []:
        out_of_scope[entry["requirement"]] = entry.get("reason", "")
    for rid, reason in out_of_scope.items():
        check("A2", rid in plan_ids, f"unknown requirement id in x-out-of-scope: {rid}")
        check("A2", bool(reason.strip()), f"x-out-of-scope entry without a reason: {rid}")
        check("A2", rid not in cited, f"{rid} is both cited and declared out of scope")

    missing = sorted(plan_ids - cited - set(out_of_scope))
    check("A2", not missing, f"requirements neither realised nor declared out of scope: {missing}")


def main():
    doc, events, plan, answer = load()
    check_examples(doc)
    check_enum_documentation(doc)
    check_refs(doc)
    check_error_codes(doc)
    check_constrained_params(doc)
    check_retry_hint(doc)
    check_trace_id(doc)
    check_message_schema(doc, events)
    check_async_refs(events)
    check_async_contract(events, plan)
    check_answer_table(doc, answer)
    check_traceability(doc, events, plan)

    for failure in failures:
        print(failure)
    print(f"\n{checks_run} checks, {len(failures)} failed")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
