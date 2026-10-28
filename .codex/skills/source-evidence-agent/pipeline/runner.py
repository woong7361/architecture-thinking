"""소스 근거 하네스 runner.

단계 그래프는 설계 §7이다.

    코드·답안·단계1 산출 → gen → 게이트 ─┬ 위반 → refine → (게이트 재실행)
                                          └ 통과 → critique → eval → 합산 → READY
                                                                      └ 미달 → refine

게이트 위반이 있으면 critique 와 eval 을 건너뛴다. 존재하지 않는 인용에는 반증할 것도 채점할 것도
없으므로 유료 호출 두 번을 아낀다. 건너뛴 이유는 원장에 남긴다.

`contract-impl-agent/pipeline/runner.py` 를 복사하지 않았다. 그쪽은 계약 판본과 git 커밋과 스켈레톤
복사에 묶여 있고 이 하네스에는 그 대상이 없다. 복사해서 걷어내면 복사한 파일을 고치지 않는다는 규약이
깨지므로, 도메인 중립인 잎 파일만 복사하고 오케스트레이션은 새로 썼다. 그 결정은 BUILD.md 에 적는다.
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import yaml

PIPELINE_DIR = Path(__file__).resolve().parent
SKILL_ROOT = PIPELINE_DIR.parent
REPO_ROOT = SKILL_ROOT.parents[2]
sys.path.insert(0, str(PIPELINE_DIR))

from gates.citation import (  # noqa: E402
    check_citations,
    check_claim_changes,
    check_coordinates,
    compare_limiting_clauses,
)
from gates.sites import coverage_gap, extract_sites, load_rules  # noqa: E402
from stages.scripts import payload as P  # noqa: E402
from stages.scripts.llm_client import SANDBOX_READ_ONLY, create_client  # noqa: E402
from validate import (  # noqa: E402
    check_dimension_sync,
    check_kind_consistency,
    validate_output,
)

for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")

ITERATION_CAP = 2
STAGES = ("gen", "critique", "eval", "refine")

SOURCES_DIR = SKILL_ROOT / "sources"
RULES_DIR = SKILL_ROOT / "rules"
RUBRIC_PATH = PIPELINE_DIR / "rubrics" / "claim.rubric.yaml"
RUNS_DIR = SKILL_ROOT / "runs"

SCHEMA_BY_STAGE = {
    "gen": "gen_output.schema.json",
    "critique": "critique_output.schema.json",
    "eval": "eval_output.schema.json",
    "refine": "refine_output.schema.json",
}


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def read_yaml(path: Path) -> dict:
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


class Ledger:
    """run 하나의 원장. 실패는 분류와 규칙 id 를 붙여 남긴다.

    iteration 마다 주장 집합을 남긴다. 신고 대조가 그 이력에 걸려 있고, 철회된 주장이 리포트에 실린다.
    """

    def __init__(self, path: Path) -> None:
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)

    def append(self, kind: str, **fields) -> None:
        record = {"at": now(), "kind": kind, **fields}
        with self.path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")

    def records(self) -> list[dict]:
        if not self.path.exists():
            return []
        return [json.loads(line) for line in self.path.read_text(encoding="utf-8").splitlines() if line]


def source_roots(manifest: dict) -> tuple[dict[str, Path], dict[str, str]]:
    roots: dict[str, Path] = {}
    grades: dict[str, str] = {}
    for spec in manifest["roots"]:
        roots[spec["root"]] = SOURCES_DIR / spec["root"]
        grades[spec["root"]] = spec["grade"]
    roots["copies"] = SKILL_ROOT / "copies"
    grades["copies"] = manifest["copies"]["grade"]
    # 측정 로그와 대상 코드는 저장소 안에 있고 경로가 그대로 루트 이름이 된다. 등급이 쓰임을 가른다.
    for section in ("measured", "target"):
        for rel in manifest[section]["roots"]:
            name = rel.rstrip("/")
            roots[name] = REPO_ROOT / name
            grades[name] = manifest[section]["grade"]
    return roots, grades


def input_blocks(sites: list[dict], rubric: dict, manifest: dict, code_roots: list[Path]) -> dict:
    """단계들이 공유하는 입력 블록. 어느 단계에 무엇을 줄지는 ALLOWED 가 정한다."""
    return {
        "SOURCES_JSON": {
            "roots": [
                {"root": s["root"], "artifact": s["artifact"], "grade": s["grade"],
                 "path": str(SOURCES_DIR / s["root"])}
                for s in manifest["roots"]
            ],
            "copies": {"path": str(SKILL_ROOT / "copies"),
                       "grade": manifest["copies"]["grade"],
                       "allowed_hosts": manifest["copies"]["allowed_hosts"]},
            "measured": manifest["measured"],
            "note": "인용은 이 루트 아래 경로에서만 가져온다. 파일 내용은 payload 에 없다. 그 자리에서 읽어라.",
        },
        "CODE_JSON": {
            "roots": [str(p) for p in code_roots],
            "note": "대상 코드다. 이 자리에서 읽어라.",
        },
        "ANSWERS_JSON": {
            "paths": [
                str(REPO_ROOT / "phase2/taskF/assignments/taskF-1.md"),
                str(REPO_ROOT / "phase2/taskF/assignments/taskF-2.md"),
                str(REPO_ROOT / "phase2/taskF/assignments/taskF-3.md"),
            ],
            "note": "사람이 쓴 답안의 단정 문장이다. 주장 후보다.",
        },
        "STEP1_JSON": {
            "paths": [str(REPO_ROOT / "phase2/taskF/task4/step1/findings.md")],
            "note": "단계 1의 자기 평가와 테스트 결과를 묶은 기록이다.",
        },
        "SITES_JSON": {"sites": sites},
        "DIMENSIONS_JSON": {"dimensions": rubric["dimensions"]},
    }


def run_gates(claims: list[dict], sites: list[dict], roots: dict[str, Path],
              grades: dict[str, str]) -> list[dict]:
    violations: list[dict] = []
    violations += check_kind_consistency(claims)["violations"]
    violations += coverage_gap(claims, sites)["violations"]
    violations += check_citations(claims, roots, grades)["violations"]
    violations += check_coordinates(claims, roots, grades)["violations"]
    return violations


STAGE_ATTEMPTS = 3


def call_stage(client, stage: str, prompt: str, blocks: dict, out_path: Path,
               raw_path: Path, model: str | None, ledger: "Ledger") -> dict:
    """단계 하나를 부른다. 전송 실패는 반복을 태우지 않고 그 자리에서 다시 부른다.

    반복 상한은 품질 거절과 계약 위반에만 쓴다. 실행 환경 실패를 같이 세면 산출물을 고칠 기회가 환경
    문제로 소진되고, 상한 초과의 원인이 품질인지 환경인지도 구분되지 않는다.
    """
    keys = [k for k in blocks]
    leaked = P.check_blocking(stage, keys)
    if leaked:
        raise RuntimeError(f"{stage} payload 에 허용 표 밖의 키가 있다: {leaked}")

    schema_path = PIPELINE_DIR / "schemas" / SCHEMA_BY_STAGE[stage]
    body = P.compose(*(P.block(key, value) for key, value in blocks.items()))

    last: Exception | None = None
    for attempt in range(1, STAGE_ATTEMPTS + 1):
        try:
            client.run_prompt(
                system=prompt,
                user=body,
                output_schema=schema_path,
                output_path=out_path,
                model=model,
                sandbox=SANDBOX_READ_ONLY,
                raw_path=raw_path.with_suffix(f".attempt{attempt}.txt"),
            )
            return read_json(out_path)
        except (RuntimeError, TimeoutError, ValueError) as exc:
            last = exc
            ledger.append("stage_failure", stage=stage, attempt=attempt,
                          detail=str(exc)[:600])
            print(f"{stage} {attempt}번째 호출이 실패했다. 전송 실패는 반복을 태우지 않는다.",
                  file=sys.stderr)
    raise SystemExit(f"{stage} 호출이 {STAGE_ATTEMPTS}번 모두 실패했다: {last}")


def stage_blocks(stage: str, shared: dict, state: dict) -> dict:
    """설계 §7의 표를 집행한다. 표에 없는 것은 담지 않는다."""
    if stage == "gen":
        return {k: shared[k] for k in ("SOURCES_JSON", "CODE_JSON", "ANSWERS_JSON",
                                       "STEP1_JSON", "SITES_JSON", "DIMENSIONS_JSON")}
    if stage == "critique":
        return {
            "SOURCES_JSON": shared["SOURCES_JSON"],
            "CODE_JSON": shared["CODE_JSON"],
            "SITES_JSON": shared["SITES_JSON"],
            "CLAIMS_JSON": P.claims_for_critique(state["gen"]),
        }
    if stage == "eval":
        return {
            "SOURCES_JSON": shared["SOURCES_JSON"],
            "CODE_JSON": shared["CODE_JSON"],
            "GEN_JSON": state["gen"],
            "RUBRIC_JSON": state["rubric"],
        }
    if stage == "refine":
        return {
            "SOURCES_JSON": shared["SOURCES_JSON"],
            "CODE_JSON": shared["CODE_JSON"],
            "CLAIMS_JSON": {"claims": state["gen"]["claims"]},
            "VIOLATIONS_JSON": {"violations": state.get("violations", [])},
            "COUNTER_JSON": P.counter_for_refine(state.get("critique") or {}),
            "WEAK_AXES_JSON": P.weak_axes(state.get("eval") or {}, state["rubric"]),
        }
    raise ValueError(stage)


def readiness(claims: list[dict], eval_output: dict, rubric: dict,
              violations: list[dict]) -> dict:
    """하한 판정은 코드가 한다. 점수는 재료이고 통과는 채점자가 정하지 않는다."""
    floors = {axis["id"]: axis for axis in rubric["axes"]}
    kind_by_id = {c["id"]: c.get("kind") for c in claims}

    unmeasured: list[str] = []
    below: list[dict] = []
    for scored in eval_output.get("claims", []):
        cid = scored.get("id", "?")
        if scored.get("unreadable"):
            unmeasured.append(cid)
            continue
        axes = scored.get("axes") or {}
        for axis_id, axis in floors.items():
            if kind_by_id.get(cid) not in axis["applies_to"]:
                continue
            box = (axes.get(axis_id) or {}).get("box")
            if box is None:
                unmeasured.append(f"{cid}/{axis_id}")
            elif box < axis["floor"]:
                below.append({"claim": cid, "axis": axis_id, "box": box, "floor": axis["floor"]})

    scored_ids = {s.get("id") for s in eval_output.get("claims", [])}
    unscored = sorted({c["id"] for c in claims} - scored_ids)
    unmeasured += [f"{cid}/전체" for cid in unscored]

    status = "READY" if not violations and not unmeasured and not below else "NOT_READY"
    return {"status": status, "violations": violations, "unmeasured": unmeasured, "below": below}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=STAGES, help="단계 하나만 돌린다")
    parser.add_argument("--run-id", help="이어서 돌릴 run 폴더 이름")
    parser.add_argument("--provider", default="claude")
    parser.add_argument("--model")
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument("--dry-run", action="store_true",
                        help="LLM 을 부르지 않고 payload 와 입력만 만들어 본다")
    args = parser.parse_args()

    manifest = read_yaml(RULES_DIR / "sources.yaml")
    rubric = read_yaml(RUBRIC_PATH)

    drift = check_dimension_sync(rubric)["violations"]
    if drift:
        for item in drift:
            print(f"판정 자산이 갈라졌다: {item['detail']}", file=sys.stderr)
        return 2

    site_rules = load_rules(RULES_DIR / "concurrency_sites.yaml")
    code_roots = [
        REPO_ROOT / "phase2/taskF/alert-kata/src/main/java",
        REPO_ROOT / "phase2/taskF/alert-kata/src/test/resources",
        REPO_ROOT / "phase2/taskF/task4/step1/run3/impl",
    ]
    sites = extract_sites(code_roots, site_rules, REPO_ROOT)
    roots, grades = source_roots(manifest)

    run_id = args.run_id or datetime.now().strftime("%Y-%m-%d_%H%M%S")
    run_dir = RUNS_DIR / run_id
    run_dir.mkdir(parents=True, exist_ok=True)
    ledger = Ledger(run_dir / "ledger.jsonl")

    shared = input_blocks(sites, rubric, manifest, code_roots)
    write_json(run_dir / "sites.json", {"sites": sites})
    state: dict = {"rubric": rubric}

    if args.dry_run:
        for stage in (args.stage,) if args.stage else STAGES:
            if stage in ("critique", "eval", "refine"):
                state.setdefault("gen", {"claims": []})
            blocks = stage_blocks(stage, shared, state)
            leaked = P.check_blocking(stage, list(blocks))
            print(f"{stage:9s} 키 {sorted(blocks)} 누출 {leaked or '없음'}")
        print(f"\n지점 {len(sites)}개, 출처 루트 {len(roots)}개, 축 {len(rubric['axes'])}개")
        print(f"run 폴더 {run_dir}")
        return 0

    # 읽기 뿌리는 저장소다. 대상 코드와 답안이 저장소에 있고 출처 스냅샷도 그 아래에 있다.
    client = create_client(args.provider, project_dir=REPO_ROOT, timeout_seconds=args.timeout)

    def one(stage: str, iteration: int = 1) -> dict:
        out_path = run_dir / f"{stage}.i{iteration}.json"
        raw_path = run_dir / f"{stage}.i{iteration}.raw.txt"

        # 이미 있는 산출은 다시 부르지 않는다. 전송 실패로 멈춘 run 을 이어 돌릴 때, 끝난 단계를
        # 다시 부르면 그 호출값이 달라져 앞 단계의 판정이 가리키는 산출물이 사라진다.
        if out_path.exists():
            output = read_json(out_path)
            if not validate_output(output, SCHEMA_BY_STAGE[stage])["violations"]:
                ledger.append("reuse", stage=stage, iteration=iteration, path=str(out_path))
                state[stage] = output
                return output

        prompt = P.read_prompt(PIPELINE_DIR / "prompts" / f"{stage}.md")
        blocks = stage_blocks(stage, shared, state)
        output = call_stage(client, stage, prompt, blocks, out_path, raw_path, args.model, ledger)
        result = validate_output(output, SCHEMA_BY_STAGE[stage])
        if result["violations"]:
            ledger.append("schema", stage=stage, violations=result["violations"])
            raise SystemExit(f"{stage} 산출이 스키마를 통과하지 못했다. 원장을 보라.")
        ledger.append("stage", stage=stage, iteration=iteration, path=str(out_path))
        state[stage] = output
        return output

    if args.stage:
        for prior in STAGES[:STAGES.index(args.stage)]:
            path = run_dir / f"{prior}.i1.json"
            if path.exists():
                state[prior] = read_json(path)
        one(args.stage)
        print(f"{args.stage} 산출: {run_dir / (args.stage + '.i1.json')}")
        return 0

    iteration = 1
    while True:
        claims = one("gen", 1)["claims"] if iteration == 1 else state["gen"]["claims"]
        violations = run_gates(claims, sites, roots, grades)
        ledger.append("gate", iteration=iteration, violations=violations)

        if violations:
            if iteration > ITERATION_CAP:
                ledger.append("terminal", status="FAILED", reason="iteration_cap")
                print("FAILED — 반복 상한까지 게이트 위반이 남았다")
                return 1
            ledger.append("skip", stages=["critique", "eval"],
                          reason="게이트 위반이 있어 반증과 채점을 건너뛴다")
            state["violations"] = violations
            refined = one("refine", iteration)
            changes = check_claim_changes(claims, refined["claims"], refined["claim_changes"])
            if changes["violations"]:
                ledger.append("gate", iteration=iteration, violations=changes["violations"])
                print("FAILED — 신고 없는 주장 변경이 있다")
                return 1
            state["gen"] = {"claims": refined["claims"]}
            iteration += 1
            continue

        critique = one("critique", iteration)
        missed = compare_limiting_clauses(state["gen"], critique)["violations"]
        ledger.append("gate", iteration=iteration, violations=missed)
        evaluation = one("eval", iteration)
        # 관찰은 거절로 이어지지 않는다. 그 조항이 주장을 깎는지는 `grounding` 이 본다.
        blocking = [v for v in missed if v.get("verdict") == "violation"]
        verdict = readiness(claims, evaluation, rubric, blocking)
        verdict["observations"] = missed
        ledger.append("verdict", iteration=iteration, **verdict)

        if verdict["status"] == "READY":
            write_json(run_dir / "verdict.json", verdict)
            write_json(run_dir / "claims.json", {"claims": claims})
            print(f"READY — 주장 {len(claims)}개. {run_dir}")
            return 0

        if iteration > ITERATION_CAP:
            ledger.append("terminal", status="FAILED", reason="iteration_cap")
            print("FAILED — 반복 상한까지 미달이 남았다")
            return 1

        state["violations"] = missed
        refined = one("refine", iteration)
        changes = check_claim_changes(claims, refined["claims"], refined["claim_changes"])
        if changes["violations"]:
            ledger.append("gate", iteration=iteration, violations=changes["violations"])
            print("FAILED — 신고 없는 주장 변경이 있다")
            return 1
        state["gen"] = {"claims": refined["claims"]}
        iteration += 1


if __name__ == "__main__":
    raise SystemExit(main())
