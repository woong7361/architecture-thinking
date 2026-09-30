from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

from jsonschema import Draft202012Validator, FormatChecker

PROJECT_DIR = Path(__file__).resolve().parent
SCHEMA_DIR = PROJECT_DIR / "schemas"
ARTIFACT_SCHEMAS = {
    "input": SCHEMA_DIR / "input.schema.json",
    "gen_output": SCHEMA_DIR / "gen_output.schema.json",
    "refine_output": SCHEMA_DIR / "gen_output.schema.json",
    # 빌드 수리 산출. 색인은 고친 파일만 담는다. 계약 항목은 어느 산출에서도 요구하지 않는다.
    "repair_output": SCHEMA_DIR / "gen_output.schema.json",
    "critique_output": SCHEMA_DIR / "critique_output.schema.json",
    "eval_output": SCHEMA_DIR / "eval_output.schema.json",
    "draft": SCHEMA_DIR / "draft.schema.json",
    "critique": SCHEMA_DIR / "critique.schema.json",
    "eval": SCHEMA_DIR / "eval.schema.json",
    "final": SCHEMA_DIR / "final.schema.json",
}

ARTIFACTS_WITH_MANIFEST = ("gen_output", "refine_output", "repair_output", "draft")

# 모델에게 주는 구조화출력 스키마. 검증용과 갈라 두는 이유는 두 쪽이 요구하는 것이 다르기 때문이다.
# 구조화출력은 모든 객체의 required가 properties의 모든 키를 담아야 하고 pattern·길이 제약을 받지 않는다.
# 판정의 엄격함을 그 제약에 맞춰 낮추지 않으려면 검증용을 따로 둬야 한다.
MODEL_SCHEMAS = {
    "gen_output": SCHEMA_DIR / "gen_output.codex.schema.json",
    "refine_output": SCHEMA_DIR / "gen_output.codex.schema.json",
    "repair_output": SCHEMA_DIR / "gen_output.codex.schema.json",
    "critique_output": SCHEMA_DIR / "critique_output.codex.schema.json",
    "eval_output": SCHEMA_DIR / "eval_output.impl.schema.json",
}

# 구조화출력이 받지 않는 키워드. 모델용 스키마에 남아 있으면 400이 떨어진다.
STRICT_FORBIDDEN_KEYWORDS = (
    "pattern",
    "minLength",
    "maxLength",
    "minItems",
    "maxItems",
    "minProperties",
    "maxProperties",
    "format",
)

# 역할 경계. Gen과 Refine은 구현자이므로 자기 채점과 판정을 내지 못한다.
DRAFT_FORBIDDEN_FIELDS = {
    "self_score",
    "self_critique",
    "verdict",
    "rubric_scores",
    "contract_errors",
}

# Critique는 리뷰어다. 점수도 판정도 재작성도 내지 못한다. 발의만 한다.
CRITIQUE_FORBIDDEN_FIELDS = {
    "score",
    "scores",
    "rubric_scores",
    "weighted_total",
    "verdict",
    "rewritten_content",
    "rewritten_files",
    "files",
}

# Eval은 판정자다. 채점만 하고 판정도 개선 지시도 내지 못한다.
EVAL_FORBIDDEN_FIELDS = {
    "verdict",
    "contract_errors",
    "revision_instructions",
    "revision_directions",
    "suggestions",
    "weaknesses",
    "rewritten_content",
}


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"invalid JSON: {exc}") from exc


def validate_file(
    file_path: Path,
    artifact: str,
    expected_brief_hash: str | None = None,
    expected_iteration: str | None = None,
    rubric: dict[str, Any] | None = None,
    immutable_paths: list[str] | None = None,
    work_dir: Path | None = None,
    changed_paths: set[str] | None = None,
) -> dict:
    if artifact not in ARTIFACT_SCHEMAS:
        raise ValueError(f"unknown artifact: {artifact}")

    try:
        data = load_json(file_path)
    except ValueError as exc:
        return {
            "artifact": artifact,
            "checked_file": str(file_path),
            "status": "ERROR",
            "errors": [str(exc)],
        }

    schema = load_json(ARTIFACT_SCHEMAS[artifact])
    errors = validate_schema(data, schema)

    if artifact in ARTIFACTS_WITH_MANIFEST and isinstance(data, dict):
        errors += validate_manifest(data, immutable_paths, work_dir, changed_paths)
    if artifact == "draft" and isinstance(data, dict):
        errors += validate_draft_contract(data, expected_brief_hash, expected_iteration)
    if artifact == "critique" and isinstance(data, dict):
        errors += validate_critique_contract(data, expected_brief_hash, expected_iteration, rubric)
    if artifact == "eval" and isinstance(data, dict):
        errors += validate_eval_contract(data, expected_brief_hash, expected_iteration, rubric)
    if artifact == "final" and isinstance(data, dict):
        errors += validate_final_contract(data, expected_brief_hash)

    return {
        "artifact": artifact,
        "checked_file": str(file_path),
        "status": "REJECT" if errors else "PASS",
        "errors": errors,
    }


def write_result(result: dict[str, Any], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def validate_schema(data: Any, schema: dict[str, Any]) -> list[str]:
    validator = Draft202012Validator(schema, format_checker=FormatChecker())
    return [
        format_schema_error(error)
        for error in sorted(validator.iter_errors(data), key=lambda item: list(item.path))
    ]


def format_schema_error(error: Any) -> str:
    path = "$"
    if error.path:
        path += "".join(f"[{part}]" if isinstance(part, int) else f".{part}" for part in error.path)
    return f"schema {path}: {error.message}"


def normalize_path(raw: str) -> str:
    """경로 비교용 정규화. 구분자와 선행 슬래시만 맞추고 내용은 바꾸지 않는다."""
    return raw.strip().replace("\\", "/").lstrip("/")


def path_escapes(rel: str) -> bool:
    parts = [part for part in rel.split("/") if part not in ("", ".")]
    return not parts or any(part == ".." for part in parts)


def is_forbidden_path(rel: str, immutable_paths: list[str]) -> str | None:
    """매니페스트 경로가 immutable_paths에 걸리는지 본다. 걸린 규칙을 돌려준다."""
    for raw in immutable_paths:
        forbidden = normalize_path(raw)
        if not forbidden:
            continue
        if rel == forbidden:
            return raw
        if forbidden.endswith("/") and rel.startswith(forbidden):
            return raw
        if rel.startswith(forbidden + "/"):
            return raw
    return None


def validate_manifest(
    data: dict,
    immutable_paths: list[str] | None,
    work_dir: Path | None = None,
    changed_paths: set[str] | None = None,
) -> list[str]:
    """색인을 작업 폴더 안에 가두고, 금지 경로와 선언·실물의 어긋남을 REJECT한다.

    매니페스트가 색인이 된 뒤로는 선언과 실물이 일치해야 한다. 게이트는 draft가 무엇인지 알아야 판정할 수
    있고, 판정 대상 밖에 조용히 놓인 파일은 판정되지 않은 표면이 된다.

    실물은 두 가지로 본다. 선언한 경로가 작업 폴더에 있는지는 파일 존재로, 무엇이 바뀌었는지는
    `changed_paths`(runner가 `git status`로 낸 것)로 본다. 바뀐 것을 기준으로 삼는 이유는 생성기가
    신고하지 않고 고친 것까지 잡기 때문이다. 매니페스트를 들여다보는 것보다 세다.
    """
    errors: list[str] = []
    files = data.get("files")
    if not isinstance(files, list):
        return errors

    seen: set[str] = set()
    manifest_paths: set[str] = set()
    contract_paths: list[str] = []
    for entry in files:
        if not isinstance(entry, dict) or not isinstance(entry.get("path"), str):
            continue
        raw = entry["path"]
        rel = normalize_path(raw)
        if path_escapes(rel):
            errors.append(f"manifest.unsafe_path: {raw!r} escapes the run directory")
            continue
        if rel in seen:
            errors.append(f"manifest.duplicate_path: {rel}")
            continue
        seen.add(rel)
        manifest_paths.add(rel)
        if entry.get("role") == "contract":
            contract_paths.append(rel)
        hit = is_forbidden_path(rel, immutable_paths or [])
        if hit:
            errors.append(f"manifest.forbidden_path: {rel} is covered by immutable_paths entry {hit!r}")

    # 색인은 계약 판본 항목을 적을 의무가 없다. 기준선 사본은 runner가 놓고 읽기전용으로 잠그므로
    # 생성기가 쓴 파일이 아니고, 판본을 올린 경우에는 그 파일이 diff에 나타나 undeclared_file이 잡는다.
    # 포인터가 가리키는 파일이 실물로 있는지만 본다.
    contract_version = data.get("contract_version")
    declared_version: str | None = None
    if isinstance(contract_version, dict) and isinstance(contract_version.get("path"), str):
        declared_version = normalize_path(contract_version["path"])

    # 계약 역할로 적은 것이 있으면 그것은 이번 판본이어야 한다. 적지 않는 것은 허용이다.
    for rel in contract_paths:
        if declared_version is not None and rel != declared_version:
            errors.append(
                f"manifest.contract_version_role: {rel} is declared as role=contract "
                f"but contract_version.path is {declared_version}"
            )

    if work_dir is not None:
        for rel in sorted(manifest_paths):
            if not (work_dir / rel).is_file():
                errors.append(f"manifest.declared_missing: {rel} is declared but not present in {work_dir.name}/")
        # 포인터도 선언이다. 가리키는 판본이 없으면 판정의 기준이 없다.
        if declared_version is not None and not (work_dir / declared_version).is_file():
            errors.append(
                f"manifest.declared_missing: {declared_version} is named by contract_version "
                f"but not present in {work_dir.name}/"
            )

    if changed_paths is not None:
        touched = {normalize_path(rel) for rel in changed_paths}
        for rel in sorted(touched - manifest_paths):
            hit = is_forbidden_path(rel, immutable_paths or [])
            if hit:
                errors.append(
                    f"manifest.forbidden_path: {rel} changed in the work tree and is covered by "
                    f"immutable_paths entry {hit!r}"
                )
            else:
                errors.append(f"manifest.undeclared_file: {rel} changed in the work tree but is not declared")
    return errors


def validate_draft_contract(
    data: dict,
    expected_brief_hash: str | None,
    expected_iteration: str | None,
) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(DRAFT_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"draft must not include {key}")
    return errors


def validate_critique_contract(
    data: dict,
    expected_brief_hash: str | None,
    expected_iteration: str | None,
    rubric: dict[str, Any] | None,
) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(CRITIQUE_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"critique must not include {key}")
    errors += validate_critique_axes(data, rubric)
    return errors


def validate_critique_axes(data: dict, rubric: dict[str, Any] | None) -> list[str]:
    """weaknesses[].axis가 루브릭의 축 집합에 있는지 대조한다. 축이 어긋나면 Refine이 신호를 묶지 못한다."""
    if not rubric:
        return []
    axes = set(rubric.get("axes", {}).keys())
    if not axes:
        return []
    errors = []
    weaknesses = data.get("weaknesses", [])
    if isinstance(weaknesses, list):
        for index, item in enumerate(weaknesses):
            if not isinstance(item, dict):
                continue
            axis = item.get("axis")
            if axis not in axes:
                errors.append(f"critique weaknesses[{index}].axis unknown: {axis!r} not in {sorted(axes)}")
    return errors


def validate_eval_contract(
    data: dict,
    expected_brief_hash: str | None,
    expected_iteration: str | None,
    rubric: dict[str, Any] | None,
) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    if expected_iteration and data.get("iteration") != expected_iteration:
        errors.append("iteration mismatch")
    for key in sorted(EVAL_FORBIDDEN_FIELDS & data.keys()):
        errors.append(f"eval must not include {key}")

    rubric_scores = data.get("rubric_scores", {})
    scores = rubric_scores.get("scores", {}) if isinstance(rubric_scores, dict) else {}
    weights = rubric_scores.get("weights", {}) if isinstance(rubric_scores, dict) else {}
    axis_rationales = data.get("axis_rationales", {})
    if rubric:
        axes = set(rubric.get("axes", {}).keys())
        if axes:
            if set(scores.keys()) != axes:
                errors.append(f"rubric score axes mismatch: expected {sorted(axes)}, actual {sorted(scores.keys())}")
            if set(weights.keys()) != axes:
                errors.append(f"rubric weight axes mismatch: expected {sorted(axes)}, actual {sorted(weights.keys())}")
            if set(axis_rationales.keys()) != axes:
                errors.append(
                    f"rubric rationale axes mismatch: expected {sorted(axes)}, actual {sorted(axis_rationales.keys())}"
                )

        thresholds = rubric.get("thresholds", {})
        min_total = thresholds.get("min_total")
        weighted_total = rubric_scores.get("weighted_total") if isinstance(rubric_scores, dict) else None
        if isinstance(min_total, (int, float)) and isinstance(weighted_total, (int, float)):
            if weighted_total < min_total:
                errors.append(f"min_total: {weighted_total} < {min_total}")

        min_axis = thresholds.get("min_axis", {})
        if isinstance(min_axis, dict) and isinstance(scores, dict):
            for axis, minimum in min_axis.items():
                score = scores.get(axis)
                if isinstance(minimum, (int, float)) and isinstance(score, (int, float)) and score < minimum:
                    errors.append(f"min_axis.{axis}: {score} < {minimum}")
    return errors


def validate_final_contract(data: dict, expected_brief_hash: str | None) -> list[str]:
    errors = []
    if expected_brief_hash and data.get("brief_hash") != expected_brief_hash:
        errors.append("brief_hash mismatch")
    contract_result = data.get("contract_result", {})
    if isinstance(contract_result, dict) and contract_result.get("verdict") != "PASS":
        errors.append("final contract_result.verdict must be PASS")
    gate_result = data.get("gate_result", {})
    if isinstance(gate_result, dict) and gate_result.get("violations"):
        errors.append("final gate_result.violations must be empty")
    return errors


def object_nodes(schema: Any, pointer: str = "$"):
    """스키마를 훑어 객체 노드를 좌표와 함께 내놓는다. properties와 items만 따라간다."""
    if not isinstance(schema, dict):
        return
    node_type = schema.get("type")
    types = node_type if isinstance(node_type, list) else [node_type]
    if "object" in types and isinstance(schema.get("properties"), dict):
        yield pointer, schema
    for key, child in (schema.get("properties") or {}).items():
        yield from object_nodes(child, f"{pointer}.{key}")
    items = schema.get("items")
    if isinstance(items, dict):
        yield from object_nodes(items, f"{pointer}[]")


def check_strict_object_schema(schema: Any, label: str) -> list[str]:
    """구조화출력 규약을 기계로 훑는다.

    모든 객체의 `required`가 `properties`의 모든 키를 담아야 하고, `additionalProperties`가 닫혀야 하며,
    pattern·길이·개수 제약이 없어야 한다. 선택 필드를 required에서 빼는 방식은 통하지 않으므로 선택은
    널 허용 타입으로 표현한다. 이 검사가 없으면 스키마를 고칠 때마다 같은 400에서 다시 막힌다.
    """
    errors: list[str] = []
    for pointer, node in object_nodes(schema):
        properties = set(node.get("properties") or {})
        required = node.get("required")
        if not isinstance(required, list):
            errors.append(f"strict {label} {pointer}: required is missing")
            continue
        missing = sorted(properties - set(required))
        if missing:
            errors.append(f"strict {label} {pointer}: required is missing {missing}")
        extra = sorted(set(required) - properties)
        if extra:
            errors.append(f"strict {label} {pointer}: required names absent properties {extra}")
        if node.get("additionalProperties") is not False:
            errors.append(f"strict {label} {pointer}: additionalProperties must be false")
    errors += check_forbidden_keywords(schema, label)
    return errors


def check_forbidden_keywords(schema: Any, label: str, pointer: str = "$") -> list[str]:
    errors: list[str] = []
    if isinstance(schema, dict):
        for keyword in STRICT_FORBIDDEN_KEYWORDS:
            if keyword in schema:
                errors.append(f"strict {label} {pointer}: {keyword} is not supported by structured output")
        for key, child in schema.items():
            if key in ("properties", "$defs", "definitions") and isinstance(child, dict):
                for name, sub in child.items():
                    errors += check_forbidden_keywords(sub, label, f"{pointer}.{name}")
            elif key == "items":
                errors += check_forbidden_keywords(child, label, f"{pointer}[]")
    return errors


def allows_null(schema: Any) -> bool:
    if not isinstance(schema, dict):
        return False
    node_type = schema.get("type")
    if isinstance(node_type, list):
        return "null" in node_type
    return node_type == "null"


def check_nullability(
    model_schema: Any,
    validation_schema: Any,
    validation_requires: bool,
    label: str,
    pointer: str,
) -> list[str]:
    """모델이 널로 낼 수 있는 자리를 검증용이 받을 수 있는지 본다.

    모델용은 "없음"을 널로 표현하고 검증용은 필드를 빼는 것으로 표현한다. 두 표현이 만나는 자리에서
    `normalize_model_output`이 널을 지워 하나로 모으므로, 검증용이 그 필드를 required로 두면 지울 수가 없다.
    검증용이 널을 직접 허용하는 것도 맞다. 둘 다 아니면 모델이 낸 것이 판정에서 떨어진다.
    """
    if not allows_null(model_schema) or allows_null(validation_schema):
        return []
    if validation_requires:
        return [
            f"schema_pair {label} {pointer}: model allows null but the validation schema requires the field "
            "and does not allow null"
        ]
    return []


def compare_schema_pair(model_schema: Any, validation_schema: Any, label: str, pointer: str = "$") -> list[str]:
    """모델용에 있는 것이 검증용에 있는지 두 스키마를 나란히 걸으며 대조한다.

    모델이 낼 수 있는 필드나 닫힌 값이 검증용에 없으면 그 산출물은 검증에서 떨어지거나 조용히 통과한다.
    두 벌로 가른 대가로 이 대조를 둔다. 반대 방향은 보지 않는다 — 검증용이 더 엄격한 것은 의도다.
    검증용이 축을 열거하지 않고 `additionalProperties`로 열어 둔 자리는 모델이 이름을 박아도 맞는 것으로 본다.
    축 집합이 루브릭과 같은지는 validate_eval_contract가 따로 대조한다.
    """
    errors: list[str] = []
    if not isinstance(model_schema, dict):
        return errors
    if not isinstance(validation_schema, dict):
        errors.append(f"schema_pair {label} {pointer}: absent in the validation schema")
        return errors

    model_enum = model_schema.get("enum")
    validation_enum = validation_schema.get("enum")
    if isinstance(model_enum, list) and isinstance(validation_enum, list):
        extra = [value for value in model_enum if value is not None and value not in validation_enum]
        if extra:
            errors.append(f"schema_pair {label} {pointer}: model-only enum values {extra}")

    model_properties = model_schema.get("properties")
    if isinstance(model_properties, dict):
        validation_properties = validation_schema.get("properties")
        open_map = validation_schema.get("additionalProperties")
        validation_required = validation_schema.get("required") or []
        for name, child in model_properties.items():
            if isinstance(validation_properties, dict) and name in validation_properties:
                counterpart = validation_properties[name]
                errors += check_nullability(
                    child, counterpart, name in validation_required, label, f"{pointer}.{name}"
                )
                errors += compare_schema_pair(child, counterpart, label, f"{pointer}.{name}")
            elif isinstance(open_map, dict):
                errors += check_nullability(child, open_map, False, label, f"{pointer}.{name}")
                errors += compare_schema_pair(child, open_map, label, f"{pointer}.{name}")
            else:
                errors.append(f"schema_pair {label} {pointer}.{name}: model-only field")

    model_items = model_schema.get("items")
    if isinstance(model_items, dict):
        validation_items = validation_schema.get("items")
        if isinstance(validation_items, dict):
            errors += compare_schema_pair(model_items, validation_items, label, f"{pointer}[]")
        else:
            errors.append(f"schema_pair {label} {pointer}[]: model-only array items")
    return errors


def prune_nulls(data: Any, schema: Any, pointer: str = "$") -> tuple[Any, list[str]]:
    """검증용이 널을 허용하지 않는 자리의 널을 지운다.

    모델용은 선택 필드를 널로 표현하고 검증용은 필드가 없는 것으로 표현한다. 둘은 같은 뜻이므로
    산출물에는 한 가지 표현만 남긴다. 그러지 않으면 원장과 리포트가 널과 없음을 따로 다뤄야 한다.
    """
    removed: list[str] = []
    if isinstance(data, dict) and isinstance(schema, dict):
        properties = schema.get("properties") or {}
        open_map = schema.get("additionalProperties")
        result: dict = {}
        for key, value in data.items():
            child = properties.get(key) if key in properties else (open_map if isinstance(open_map, dict) else None)
            if value is None and isinstance(child, dict) and not allows_null(child):
                removed.append(f"{pointer}.{key}")
                continue
            if isinstance(child, dict):
                value, inner = prune_nulls(value, child, f"{pointer}.{key}")
                removed += inner
            result[key] = value
        return result, removed
    if isinstance(data, list) and isinstance(schema, dict) and isinstance(schema.get("items"), dict):
        result_list = []
        for index, item in enumerate(data):
            item, inner = prune_nulls(item, schema["items"], f"{pointer}[{index}]")
            removed += inner
            result_list.append(item)
        return result_list, removed
    return data, removed


VERSION_SUFFIX = re.compile(r"-v(\d+)$")


def find_version_twin(declared: str, work_dir: Path) -> str | None:
    """선언한 경로가 없을 때 같은 판 번호를 가진 실재 파일을 찾는다.

    포인터의 철자는 이 하네스가 재려는 것이 아니다. 게이트가 재는 것은 구현이 계약을 지켰는지이고,
    19분과 비용을 파일명 한 줄에 태우는 것은 그 대상이 아니다. 그래서 판 번호가 같은 후보가 정확히
    하나일 때만 고친다. 판 번호를 못 뽑거나 후보가 둘 이상이면 고치지 않는다 — 어느 것을 뜻했는지
    기계가 말할 수 없는 자리에서 짐작하면 판정의 대상이 바뀐다.
    """
    rel = normalize_path(declared)
    target = Path(rel)
    match = VERSION_SUFFIX.search(target.stem)
    if not match:
        return None
    version = match.group(1)

    directories = []
    declared_parent = work_dir / target.parent
    if declared_parent.is_dir():
        directories.append(declared_parent)
    fallback = work_dir / "contract"
    if fallback.is_dir() and fallback not in directories:
        directories.append(fallback)

    candidates: list[str] = []
    for directory in directories:
        for candidate in sorted(directory.iterdir()):
            if not candidate.is_file() or candidate.suffix != target.suffix:
                continue
            found = VERSION_SUFFIX.search(candidate.stem)
            if found and found.group(1) == version:
                relative = candidate.relative_to(work_dir).as_posix()
                if relative not in candidates:
                    candidates.append(relative)
        if candidates:
            break
    return candidates[0] if len(candidates) == 1 else None


def repair_version_paths(data: Any, work_dir: Path) -> list[dict]:
    """판본 경로의 철자를 실재하는 파일로 맞춘다. 고친 자리를 돌려준다.

    위반이 아니라 정규화다. 고친 사실은 기록으로 남아 원장과 리포트가 "무엇을 자동으로 고쳤는지" 말한다.
    """
    records: list[dict] = []

    def repair(container: dict, key: str, pointer: str) -> None:
        declared = container.get(key)
        if not isinstance(declared, str) or not declared.strip():
            return
        rel = normalize_path(declared)
        if (work_dir / rel).is_file():
            return
        twin = find_version_twin(declared, work_dir)
        if twin is None or twin == rel:
            return
        container[key] = twin
        records.append({"kind": "version_path_repaired", "pointer": pointer, "from": declared, "to": twin})

    if not isinstance(data, dict):
        return records
    version = data.get("contract_version")
    if isinstance(version, dict):
        repair(version, "path", "$.contract_version.path")
        repair(version, "from", "$.contract_version.from")
    files = data.get("files")
    if isinstance(files, list):
        for index, entry in enumerate(files):
            if isinstance(entry, dict):
                repair(entry, "path", f"$.files[{index}].path")
    return records


def normalize_model_output(path: Path, artifact: str, work_dir: Path | None = None) -> list[dict]:
    """모델 산출물을 검증용 표현으로 맞춘다. 고친 자리를 돌려주어 로그와 원장에 남게 한다."""
    if artifact not in ARTIFACT_SCHEMAS:
        raise ValueError(f"unknown artifact: {artifact}")
    try:
        data = load_json(path)
    except ValueError:
        return []
    data, removed = prune_nulls(data, load_json(ARTIFACT_SCHEMAS[artifact]))
    records: list[dict] = [{"kind": "null_pruned", "pointer": pointer} for pointer in removed]
    if work_dir is not None and artifact in ARTIFACTS_WITH_MANIFEST:
        records += repair_version_paths(data, work_dir)
    if records:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return records


def check_schema_pairs() -> dict:
    """모델용 스키마 전부를 규약과 검증용에 대조한다. runner가 유료 호출 전에 부른다."""
    errors: list[str] = []
    checked: list[str] = []
    for artifact, model_path in MODEL_SCHEMAS.items():
        if not model_path.exists():
            errors.append(f"schema_pair {artifact}: model schema not found: {model_path}")
            continue
        if artifact in checked:
            continue
        checked.append(artifact)
        model_schema = load_json(model_path)
        errors += check_strict_object_schema(model_schema, model_path.name)
        errors += compare_schema_pair(model_schema, load_json(ARTIFACT_SCHEMAS[artifact]), model_path.name)
    return {
        "artifact": "schema_pairs",
        "checked_file": str(SCHEMA_DIR),
        "status": "REJECT" if errors else "PASS",
        "errors": errors,
    }


def git_changed_paths(work_dir: Path) -> set[str]:
    """CLI에서 쓰는 얇은 래퍼. 파이프라인에서는 runner가 같은 것을 계산해 넘긴다."""
    sys.path.insert(0, str(PROJECT_DIR))
    from stages.scripts.textio import run_command  # noqa: E402

    completed = run_command(["git", "status", "--porcelain", "-uall"], cwd=work_dir)
    paths: set[str] = set()
    for line in completed.stdout.splitlines():
        entry = line[3:].strip() if len(line) > 3 else ""
        if not entry:
            continue
        if " -> " in entry:
            before, after = entry.split(" -> ", 1)
            paths.add(before.strip().strip(chr(34)))
            paths.add(after.strip().strip(chr(34)))
        else:
            paths.add(entry.strip(chr(34)))
    return paths


def immutable_paths_from_input(input_path: Path) -> list[str]:
    data = load_json(input_path)
    if not isinstance(data, dict):
        return []
    contract = data.get("brief", {}).get("implementation_contract", {})
    paths = contract.get("immutable_paths", []) if isinstance(contract, dict) else []
    return [item for item in paths if isinstance(item, str)]


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a pipeline JSON artifact.")
    parser.add_argument("file", type=Path, nargs="?")
    parser.add_argument(
        "--artifact",
        choices=sorted(ARTIFACT_SCHEMAS.keys()),
    )
    parser.add_argument(
        "--schema-pairs",
        action="store_true",
        help="모델용 스키마가 구조화출력 규약을 지키고 검증용과 어긋나지 않는지만 본다. 파일 인자가 필요 없다.",
    )
    parser.add_argument("--brief-hash")
    parser.add_argument("--iteration")
    parser.add_argument(
        "--against-input",
        type=Path,
        help="매니페스트 금지 경로 검사에 쓸 input JSON. brief.implementation_contract.immutable_paths를 읽는다.",
    )
    parser.add_argument(
        "--work-dir",
        type=Path,
        help="색인과 대조할 작업 폴더(runs/<run_id>/output). 주면 declared_missing을 본다.",
    )
    parser.add_argument(
        "--use-git-status",
        action="store_true",
        help="--work-dir의 git status로 바뀐 경로를 얻어 undeclared_file·forbidden_path까지 본다.",
    )
    parser.add_argument("--write-result", type=Path)
    args = parser.parse_args()

    if args.schema_pairs:
        result = check_schema_pairs()
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0 if result["status"] == "PASS" else 1

    if args.file is None or args.artifact is None:
        parser.error("file and --artifact are required unless --schema-pairs is given")

    immutable_paths = immutable_paths_from_input(args.against_input) if args.against_input else None

    result = validate_file(
        file_path=args.file,
        artifact=args.artifact,
        expected_brief_hash=args.brief_hash,
        expected_iteration=args.iteration,
        immutable_paths=immutable_paths,
        work_dir=args.work_dir,
        changed_paths=git_changed_paths(args.work_dir) if (args.use_git_status and args.work_dir) else None,
    )

    if args.write_result:
        write_result(result, args.write_result)

    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
