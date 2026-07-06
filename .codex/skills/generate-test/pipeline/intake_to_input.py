from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path


PIPELINE_DIR = Path(__file__).resolve().parent
KST = timezone(timedelta(hours=9))


def read_source_material(args: argparse.Namespace) -> str:
    chunks: list[str] = []
    for path in args.source_material_file or []:
        chunks.append(Path(path).read_text(encoding="utf-8"))
    if args.source_material:
        chunks.append(args.source_material)
    material = "\n\n".join(chunk.strip() for chunk in chunks if chunk.strip()).strip()
    if not material:
        raise ValueError("source material is required via --source-material or --source-material-file")
    return material


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


def build_brief(args: argparse.Namespace, source_material: str) -> dict:
    brief: dict[str, object] = {
        "requirement": args.requirement,
        "source_material": source_material,
    }
    if args.title:
        brief["title"] = args.title

    policy_rules = split_values(args.policy_rule)
    if policy_rules:
        brief["policy_rules"] = policy_rules

    external = split_values(args.external_dependency)
    if external:
        brief["external_dependencies"] = external

    frozen = read_frozen_contract(args)
    if frozen:
        brief["frozen_contract"] = frozen

    constraints: dict[str, object] = {}
    for key, values in [
        ("must_include", split_values(args.must_include)),
        ("avoid", split_values(args.avoid)),
    ]:
        if values:
            constraints[key] = values
    if constraints:
        brief["constraints"] = constraints

    return brief


def read_frozen_contract(args: argparse.Namespace) -> str:
    """split-unit run용. 동결된 gherkin 계약 원문을 읽어 brief.frozen_contract로 싣는다."""
    if args.frozen_contract_file:
        return Path(args.frozen_contract_file).read_text(encoding="utf-8").strip()
    if args.frozen_contract:
        return args.frozen_contract.strip()
    return ""


def brief_hash(brief: dict) -> str:
    canonical = json.dumps(brief, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:8]


def write_json(path: Path, data: dict, overwrite: bool) -> None:
    if path.exists() and not overwrite:
        existing = path.read_text(encoding="utf-8")
        incoming = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if existing == incoming:
            return
        raise FileExistsError(f"refusing to overwrite existing file: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_input(path: Path) -> dict:
    sys.path.insert(0, str(PIPELINE_DIR))
    from validate import validate_file  # type: ignore

    return validate_file(path, artifact="input")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Build test-generation harness input JSON (정책만 담는다; 테스트 케이스는 Gen이 생성한다)."
    )
    parser.add_argument("--requirement", required=True, help="테스트로 고정할 정책/요구사항 (NL).")
    parser.add_argument("--source-material", help="설계/스펙 원문 (요약 금지).")
    parser.add_argument(
        "--source-material-file",
        action="append",
        help="원문을 담은 UTF-8 파일. 반복 지정 가능.",
    )
    parser.add_argument("--title")
    parser.add_argument(
        "--policy-rule",
        action="append",
        help="정책을 분해한 규칙(케이스 아님). 반복 지정 또는 ';'로 구분.",
    )
    parser.add_argument(
        "--external-dependency",
        action="append",
        help="scope 밖 외부 의존(PG/DB 등). 반복 지정 또는 ';'로 구분.",
    )
    parser.add_argument("--frozen-contract", help="split-unit용. 동결된 gherkin 계약 원문.")
    parser.add_argument("--frozen-contract-file", help="split-unit용. 동결된 계약 .feature 파일 경로.")
    parser.add_argument("--must-include", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--avoid", action="append", help="Repeat or separate values with semicolons.")
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    source_material = read_source_material(args)
    brief = build_brief(args, source_material)
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

    print(json.dumps({"status": "PASS", "input": str(output_path)}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
