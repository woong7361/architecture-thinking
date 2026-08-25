"""runner.py — refactor-agent C0 오케스트레이터.

C0 슬라이스: Diagnose(진단) → Implement(구현) → Validate(행위 게이트).
Critique/Eval은 C0 범위 밖. RED면 Implement를 재시도(refine)한다(설계는 그대로).

usage: python -B runner.py <input.json>
"""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True

# Windows 콘솔이 MS949라 유니코드(—, ∥ 등) 출력이 깨진다 → UTF-8로 강제.
for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

HERE = Path(__file__).resolve().parent          # pipeline/
SKILL = HERE.parent                             # refactor-agent/
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / "stages"))

from scripts.llm_client import create_client   # noqa: E402
from behavior_gate import run_gate             # noqa: E402

PROMPTS = HERE / "prompts"
SCHEMAS = HERE / "schemas"


def log(msg: str) -> None:
    print(f"[runner] {msg}", file=sys.stderr, flush=True)


def git_show(repo_root: Path, ref: str, path: str) -> str:
    r = subprocess.run(["git", "show", f"{ref}:{path}"], cwd=str(repo_root),
                        capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        raise RuntimeError(f"git show {ref}:{path} failed: {r.stderr.strip()}")
    return r.stdout


def fmt_files(d: dict) -> str:
    return "\n\n".join(f"// ===== FILE: {k} =====\n{v}" for k, v in d.items())


def materialize(run_dir: Path, files: list) -> list:
    """PASS한 리팩토링 매니페스트를 <run_dir>/artifact/ 아래 실제 파일로 persist."""
    art = run_dir / "artifact"
    written = []
    for f in files:
        rel = f["path"].replace("\\", "/").lstrip("/")
        if not rel or ".." in rel.split("/"):
            continue
        p = art / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(f["content"], encoding="utf-8")
        written.append(p)
    return written


def call(client, system_path: Path, user: str, schema_path: Path, out_path: Path):
    system = system_path.read_text(encoding="utf-8")
    client.run_prompt(system=system, user=user, output_schema=schema_path, output_path=out_path, model=None)
    return json.loads(out_path.read_text(encoding="utf-8"))


def score_eval(ev: dict, rubric: dict) -> dict | None:
    """Eval의 축 점수를 rubric 가중치로 weighted_total·threshold 판정(결정적, LLM 아님)."""
    scores = ev.get("scores", {})
    weighted = 0.0
    for axis, spec in rubric["axes"].items():
        s = scores.get(axis)
        if not isinstance(s, (int, float)):
            return None  # 축 누락
        allowed_scores = {float(value) for value in spec["scale"]}
        if float(s) not in allowed_scores:
            return None  # rubric 사다리에 없는 점수
        weighted += s * spec["weight"]
    th = rubric["thresholds"]
    weak = [a for a, m in th.get("min_axis", {}).items()
            if isinstance(scores.get(a), (int, float)) and scores[a] < m]
    passed = weighted >= th["min_total"] and not weak
    return {"weighted_total": round(weighted, 3), "passed": passed, "weak_axes": weak, "scores": scores}


def build_diag_user(cfg, reference, source, revision):
    u = (
        f"change_goal: {cfg['change_goal']}\n\n"
        f"boundary: {cfg['boundary']}\n\n"
        f"SMELL_SOLID_MAP:\n{reference}\n\n"
        f"code:\n{fmt_files(source)}\n"
    )
    if revision:  # refine 패스: Critique 약점 + 약축 이름(점수 아님) + 이전 제안
        u += f"\nREVISION_FEEDBACK:\n{json.dumps(revision, ensure_ascii=False, indent=2)}\n"
    return u


def build_impl_user(cfg, diagnosis, source, impl_feedback):
    u = (
        f"boundary: {cfg['boundary']}\n\n"
        f"proposals:\n{json.dumps(diagnosis['proposals'], ensure_ascii=False, indent=2)}\n\n"
        f"code:\n{fmt_files(source)}\n"
    )
    if impl_feedback:
        u += f"\nPREVIOUS_ATTEMPT_FAILED — 구현을 고쳐라(경계·제안은 그대로):\n{impl_feedback}\n"
    return u


def run_review(client, cfg, reference, rubric, source, diagnosis, files, iter_dir):
    """Critique ∥ Eval 병렬(서로 못 봄). eval 집계는 결정적."""
    refactored = fmt_files({f["path"]: f["content"] for f in files})
    common = (
        f"change_goal: {cfg['change_goal']}\n\n"
        f"original_code:\n{fmt_files(source)}\n\n"
        f"proposals:\n{json.dumps(diagnosis['proposals'], ensure_ascii=False, indent=2)}\n\n"
        f"refactored_code:\n{refactored}\n\n"
        f"SMELL_SOLID_MAP:\n{reference}\n"
    )
    eval_user = common + f"\nRUBRIC:\n{json.dumps(rubric, ensure_ascii=False, indent=2)}\n"
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
        fut_c = ex.submit(call, client, PROMPTS / "critique_refactor.md", common,
                          SCHEMAS / "critique_output.schema.json", iter_dir / "critique.json")
        fut_e = ex.submit(call, client, PROMPTS / "eval_refactor.md", eval_user,
                          SCHEMAS / "eval_output.schema.json", iter_dir / "eval.json")
        critique = fut_c.result()
        evaluation = fut_e.result()
    score = score_eval(evaluation, rubric)
    (iter_dir / "eval_score.json").write_text(json.dumps(score, ensure_ascii=False, indent=2), encoding="utf-8")
    return critique, score


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("input", type=Path)
    args = ap.parse_args()

    cfg = json.loads(args.input.read_text(encoding="utf-8"))
    repo_root = Path(cfg.get("repo_root", ".")).resolve()
    run_dir = SKILL / "runs" / cfg.get("run_id", "c0")
    run_dir.mkdir(parents=True, exist_ok=True)

    client = create_client(provider=cfg.get("provider", "claude"), project_dir=SKILL,
                           timeout_seconds=cfg.get("timeout", 600))
    reference = (repo_root / cfg["reference"]).read_text(encoding="utf-8")
    rubric = json.loads((HERE / "rubrics" / "refactor.rubric.json").read_text(encoding="utf-8"))
    source = {rel: git_show(repo_root, cfg["baseline_ref"], f"{cfg['project_subdir']}/{cfg['source_root']}/{rel}")
              for rel in cfg["source_files"]}

    max_design = cfg.get("max_design_iters", cfg.get("max_iterations", 2))
    max_impl = cfg.get("max_impl_iters", 2)
    revision = None          # Diagnose refine 피드백 (첫 패스엔 None)
    last: dict = {}

    for d in range(1, max_design + 1):
        iter_dir = run_dir / f"iter_{d:03d}"
        iter_dir.mkdir(parents=True, exist_ok=True)

        # 1 · Diagnose (설계) — refine 패스면 REVISION_FEEDBACK 포함
        log(f"iter {d:03d} diagnose{' (refine)' if revision else ''} ...")
        diagnosis = call(client, PROMPTS / "diagnose_refactor.md",
                         build_diag_user(cfg, reference, source, revision),
                         SCHEMAS / "diagnose_output.schema.json", iter_dir / "diagnose.json")
        log(f"iter {d:03d} violations={len(diagnosis.get('violations', []))} "
            f"proposals={len(diagnosis.get('proposals', []))}")

        # 2·3 · Implement → Validate (RED면 Implement refine)
        impl_feedback = ""
        verdict = {"verdict": "ERROR", "detail": "no impl ran"}
        files: list = []
        for i in range(1, max_impl + 1):
            log(f"iter {d:03d} impl {i} implement ...")
            impl = call(client, PROMPTS / "implement_refactor.md",
                        build_impl_user(cfg, diagnosis, source, impl_feedback),
                        SCHEMAS / "implement_output.schema.json", iter_dir / f"implement_{i:02d}.json")
            files = impl.get("files", [])
            verdict = run_gate(repo_root, cfg["baseline_ref"], cfg["project_subdir"], cfg["source_root"],
                               files, cfg["test_cmd"], cfg.get("java_home"))
            (iter_dir / f"gate_{i:02d}.json").write_text(json.dumps(verdict, ensure_ascii=False, indent=2), encoding="utf-8")
            log(f"iter {d:03d} impl {i} verdict={verdict['verdict']} :: {verdict['detail']}")
            if verdict["verdict"] == "GREEN":
                break
            impl_feedback = f"{verdict['verdict']}: {verdict['detail']}"

        if verdict["verdict"] != "GREEN":
            # 구현 refine 소진에도 RED → 설계로 에스컬레이트(제안 자체가 행위를 바꾼 것)
            log(f"iter {d:03d}: RED 미해결 → 설계 refine 에스컬레이트")
            revision = {"previous_proposals": diagnosis["proposals"], "behavior_broken": verdict["detail"]}
            last = {"stage": "validate", "verdict": verdict}
            continue

        # 4 · GREEN → Critique ∥ Eval (병렬)
        log(f"iter {d:03d} critique ∥ eval (parallel) ...")
        critique, score = run_review(client, cfg, reference, rubric, source, diagnosis, files, iter_dir)
        weaknesses = critique.get("weaknesses", [])
        log(f"iter {d:03d} critique weaknesses={len(weaknesses)}  "
            f"eval weighted={score['weighted_total'] if score else 'n/a'} passed={score['passed'] if score else 'n/a'}")

        if score and score["passed"]:
            artifacts = materialize(run_dir, files)   # 리팩토링된 코드를 .java로 persist
            final = {"status": "PASS", "design_iter": d, "behavior": verdict,
                     "quality": {"eval": score, "critique_weaknesses": weaknesses},
                     "artifact": [str(p.relative_to(run_dir)) for p in artifacts]}
            (run_dir / "final.json").write_text(json.dumps(final, ensure_ascii=False, indent=2), encoding="utf-8")
            print(json.dumps(final, ensure_ascii=False))
            return 0

        # 품질 미달 → 설계 refine (Critique 약점 + 약축 이름만; Eval 숫자는 안 넣음 = 순환성 차단)
        revision = {"previous_proposals": diagnosis["proposals"],
                    "weaknesses": weaknesses,
                    "weak_axes": score["weak_axes"] if score else []}
        last = {"stage": "eval", "eval": score, "critique_weaknesses": weaknesses}

    final = {"status": "FAILED", "reason": "max_design_iters", "last": last}
    (run_dir / "final.json").write_text(json.dumps(final, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(final, ensure_ascii=False))
    return 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"[runner] ERROR {type(exc).__name__}: {exc}", file=sys.stderr)
        sys.exit(1)
