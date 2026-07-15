"""runner.py — refactor-agent C0 오케스트레이터.

C0 슬라이스: Diagnose(진단) → Implement(구현) → Validate(행위 게이트).
Critique/Eval은 C0 범위 밖. RED면 Implement를 재시도(refine)한다(설계는 그대로).

usage: python -B runner.py <input.json>
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True

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


def call(client, system_path: Path, user: str, schema_path: Path, out_path: Path):
    system = system_path.read_text(encoding="utf-8")
    client.run_prompt(system=system, user=user, output_schema=schema_path, output_path=out_path, model=None)
    return json.loads(out_path.read_text(encoding="utf-8"))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("input", type=Path)
    args = ap.parse_args()

    cfg = json.loads(args.input.read_text(encoding="utf-8"))
    repo_root = Path(cfg.get("repo_root", ".")).resolve()
    run_dir = SKILL / "runs" / "c0"
    run_dir.mkdir(parents=True, exist_ok=True)

    client = create_client(provider=cfg.get("provider", "claude"), project_dir=SKILL,
                           timeout_seconds=cfg.get("timeout", 600))
    reference = (repo_root / cfg["reference"]).read_text(encoding="utf-8")
    source = {rel: git_show(repo_root, cfg["baseline_ref"], f"{cfg['project_subdir']}/{cfg['source_root']}/{rel}")
              for rel in cfg["source_files"]}

    # 1 · Diagnose (설계만)
    diag_user = (
        f"change_goal: {cfg['change_goal']}\n\n"
        f"boundary: {cfg['boundary']}\n\n"
        f"SMELL_SOLID_MAP:\n{reference}\n\n"
        f"code:\n{fmt_files(source)}\n"
    )
    log("diagnose ...")
    diagnosis = call(client, PROMPTS / "diagnose_refactor.md", diag_user,
                     SCHEMAS / "diagnose_output.schema.json", run_dir / "diagnose.json")
    log(f"violations={len(diagnosis.get('violations', []))} proposals={len(diagnosis.get('proposals', []))}")

    # 2·3 · Implement → Validate (RED면 refine)
    max_iter = cfg.get("max_iterations", 2)
    feedback = ""
    verdict = {"verdict": "ERROR", "detail": "no iteration ran"}
    for it in range(1, max_iter + 1):
        impl_user = (
            f"boundary: {cfg['boundary']}\n\n"
            f"proposals:\n{json.dumps(diagnosis['proposals'], ensure_ascii=False, indent=2)}\n\n"
            f"code:\n{fmt_files(source)}\n"
            + (f"\nPREVIOUS_ATTEMPT_FAILED — 구현을 고쳐라(경계·제안은 그대로):\n{feedback}\n" if feedback else "")
        )
        log(f"iter {it} implement ...")
        impl = call(client, PROMPTS / "implement_refactor.md", impl_user,
                    SCHEMAS / "implement_output.schema.json", run_dir / f"implement_{it}.json")
        files = impl.get("files", [])
        log(f"iter {it} gate ({len(files)} files) ...")
        verdict = run_gate(repo_root, cfg["baseline_ref"], cfg["project_subdir"], cfg["source_root"],
                           files, cfg["test_cmd"], cfg.get("java_home"))
        (run_dir / f"gate_{it}.json").write_text(json.dumps(verdict, ensure_ascii=False, indent=2), encoding="utf-8")
        log(f"iter {it} verdict={verdict['verdict']} :: {verdict['detail']}")
        if verdict["verdict"] == "GREEN":
            print(json.dumps({"status": "PASS", "iteration": it, "detail": verdict["detail"]}, ensure_ascii=False))
            return 0
        feedback = f"{verdict['verdict']}: {verdict['detail']}"

    print(json.dumps({"status": "FAILED", "last_verdict": verdict}, ensure_ascii=False))
    return 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"[runner] ERROR {type(exc).__name__}: {exc}", file=sys.stderr)
        sys.exit(1)
