from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

PIPELINE_DIR = Path(__file__).resolve().parent
KST = timezone(timedelta(hours=9))

HTTP_METHODS = ("get", "put", "post", "delete", "options", "head", "patch", "trace")

DEFAULT_STACK = ["Java 17", "Spring Boot 3", "Maven"]
# 층 경계 규약. 스켈레톤의 ArchUnit 규칙이 같은 말을 강제한다.
DEFAULT_LAYOUT_RULES = [
    "인바운드 어댑터(api)는 HTTP 관심사만 다룬다",
    "도메인(domain)과 유스케이스(app)는 HTTP·직렬화 타입을 참조하지 않는다",
    "api는 adapter를 참조하지 않는다",
    "domain은 api를 참조하지 않는다",
    # 판정 좌표가 오퍼레이션 식별자와 경로이므로, 그 둘을 구현이 자기 관례로 정하면 전부 어긋난다.
    "각 오퍼레이션의 식별자는 계약이 선언한 것과 같아야 한다. 메서드 이름에서 뽑히게 두지 말고 명시한다",
    "서버 URL에 있는 경로 접두사를 각 오퍼레이션의 경로에 다시 넣지 않는다",
    # 판정은 구현에서 뽑은 스펙으로 한다. 선언하지 않으면 도구의 기본값이 계약과 다른 말을 하고,
    # 구현이 실제로 그 응답을 내더라도 스펙에는 드러나지 않는다.
    "응답의 상태 코드와 필드의 필수 여부, 에러 코드 확장, 보안 요구를 구현이 명시적으로 선언한다. "
    "추출한 스펙이 계약과 같은 말을 해야 하며 도구의 기본값에 맡기지 않는다",
]


def read_text(path: Path) -> str:
    """원문을 줄끝만 정규화해 읽는다. 해시와 기준선 사본이 같은 바이트를 보게 한다."""
    raw = path.read_bytes().decode("utf-8")
    return raw.replace("\r\n", "\n").replace("\r", "\n")


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def split_values(values: list[str] | None) -> list[str]:
    if not values:
        return []
    items: list[str] = []
    for value in values:
        for part in value.split(";"):
            stripped = part.strip()
            if stripped:
                items.append(stripped)
    return items


def parse_contract(text: str, path: Path) -> dict:
    import yaml  # type: ignore[import-not-found]

    data = yaml.safe_load(text)
    if not isinstance(data, dict):
        raise ValueError(f"contract is not a mapping: {path}")
    return data


def detect_kind(document: dict, path: Path) -> str:
    if "openapi" in document:
        return "openapi"
    if "asyncapi" in document:
        return "asyncapi"
    raise ValueError(f"cannot tell the contract kind of {path}: neither 'openapi' nor 'asyncapi' is present")


def extract_operation_ids(document: dict, kind: str) -> list[str]:
    """판정 범위를 계약에서 기계로 뽑는다. 사람이 적으면 구현이 뺀 오퍼레이션을 셀 수 없다."""
    ids: list[str] = []
    if kind == "openapi":
        paths = document.get("paths", {})
        if isinstance(paths, dict):
            for path_item in paths.values():
                if not isinstance(path_item, dict):
                    continue
                for method in HTTP_METHODS:
                    operation = path_item.get(method)
                    if isinstance(operation, dict) and isinstance(operation.get("operationId"), str):
                        ids.append(operation["operationId"])
    else:
        operations = document.get("operations", {})
        if isinstance(operations, dict):
            ids.extend(str(name) for name in operations.keys())
        channels = document.get("channels", {})
        if isinstance(channels, dict):
            for channel in channels.values():
                if not isinstance(channel, dict):
                    continue
                for role in ("publish", "subscribe"):
                    operation = channel.get(role)
                    if isinstance(operation, dict) and isinstance(operation.get("operationId"), str):
                        ids.append(operation["operationId"])

    seen: set[str] = set()
    unique: list[str] = []
    for name in ids:
        if name not in seen:
            seen.add(name)
            unique.append(name)
    if not unique:
        raise ValueError("no operation ids found in the contract; scope cannot be empty")
    return unique


def build_contracts(paths: list[Path]) -> tuple[list[dict], list[str]]:
    contracts: list[dict] = []
    operation_ids: list[str] = []
    for path in paths:
        text = read_text(path)
        document = parse_contract(text, path)
        kind = detect_kind(document, path)
        contracts.append(
            {
                "kind": kind,
                "path": path.as_posix(),
                "sha256": sha256_text(text),
                "text": text,
            }
        )
        for name in extract_operation_ids(document, kind):
            if name not in operation_ids:
                operation_ids.append(name)
    return contracts, operation_ids


def build_provided_api(paths: list[Path]) -> list[dict]:
    """사람이 소유한 포트 인터페이스 원문만 싣는다. 전문을 실으면 스켈레톤을 고쳐 계약을 맞추는 길이 열린다."""
    return [{"path": path.as_posix(), "text": read_text(path)} for path in paths]


def relative_to_repo(path: Path) -> Path:
    """저장소 기준 상대경로로 적는다. 판정과 보고가 같은 이름을 쓰게 한다."""
    resolved = path.resolve()
    repo_root = PIPELINE_DIR.parents[3]
    try:
        return resolved.relative_to(repo_root)
    except ValueError:
        return resolved


def build_brief(args: argparse.Namespace) -> dict:
    contract_paths = [relative_to_repo(Path(item)) for item in args.contract]
    contracts, operation_ids = build_contracts([Path(item) for item in args.contract])
    for contract, relative in zip(contracts, contract_paths):
        contract["path"] = relative.as_posix()

    if args.operation_id:
        operation_ids = split_values(args.operation_id)

    spec_chunks = [read_text(Path(item)).strip() for item in args.spec]
    requirement_spec = "\n\n".join(chunk for chunk in spec_chunks if chunk)
    if not requirement_spec:
        raise ValueError("requirement spec is empty")

    provided_paths = [Path(item) for item in (args.provided_api or [])]
    provided_api = build_provided_api(provided_paths)
    for entry, source in zip(provided_api, provided_paths):
        entry["path"] = relative_to_repo(source).as_posix()

    brief: dict[str, object] = {
        "title": args.title,
        "contracts": contracts,
        "scope": {"operation_ids": operation_ids},
        "requirement_spec": requirement_spec,
        "implementation_contract": {
            "stack": split_values(args.stack) or DEFAULT_STACK,
            "layout_rules": split_values(args.layout_rule) or DEFAULT_LAYOUT_RULES,
            "provided_api": provided_api,
            "immutable_paths": split_values(args.immutable_path),
        },
    }
    out_of_scope = split_values(args.out_of_scope)
    if out_of_scope:
        brief["out_of_scope"] = out_of_scope
    return brief


def brief_hash(brief: dict) -> str:
    canonical = json.dumps(brief, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:8]


def write_json(path: Path, data: dict, overwrite: bool) -> None:
    incoming = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if path.exists() and not overwrite:
        if path.read_text(encoding="utf-8") == incoming:
            return
        raise FileExistsError(f"refusing to overwrite existing file: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(incoming, encoding="utf-8")


def validate_input(path: Path) -> dict:
    sys.path.insert(0, str(PIPELINE_DIR))
    from validate import validate_file  # type: ignore[import-not-found]

    return validate_file(path, artifact="input")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="계약과 요구사항 명세로 input JSON을 만든다. 판정 기준은 넣지 않는다."
    )
    parser.add_argument("--contract", action="append", required=True, help="계약 파일. 반복 지정 가능.")
    parser.add_argument("--spec", action="append", required=True, help="요구사항 명세 파일. 반복 지정 가능.")
    parser.add_argument("--title", required=True, help="구현 대상 한 줄 이름.")
    parser.add_argument(
        "--operation-id",
        action="append",
        help="판정 범위를 직접 적을 때만. 미지정이면 계약에서 기계로 뽑는다.",
    )
    parser.add_argument("--provided-api", action="append", help="스켈레톤이 제공하는 포트 인터페이스 파일.")
    parser.add_argument("--immutable-path", action="append", help="건드리면 안 되는 경로. ';'로 구분 가능.")
    parser.add_argument("--stack", action="append", help=f"미지정 시 {DEFAULT_STACK}.")
    parser.add_argument("--layout-rule", action="append", help="미지정 시 ArchUnit 규칙과 같은 말을 쓴다.")
    parser.add_argument("--out-of-scope", action="append", help="이 run이 다루지 않는 것.")
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    brief = build_brief(args)
    payload = {
        "brief_hash": brief_hash(brief),
        "created_at": datetime.now(KST).isoformat(timespec="seconds"),
        "brief": brief,
    }
    output_path = args.output_dir / f"{payload['brief_hash']}_input.json"
    write_json(output_path, payload, overwrite=args.overwrite)

    result = validate_input(output_path)
    if result["status"] != "PASS":
        print(json.dumps(result, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1

    print(
        json.dumps(
            {
                "status": "PASS",
                "input": str(output_path),
                "brief_hash": payload["brief_hash"],
                "contracts": [
                    {"kind": c["kind"], "path": c["path"], "sha256": c["sha256"]} for c in brief["contracts"]
                ],
                "operation_ids": brief["scope"]["operation_ids"],
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
