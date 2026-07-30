"""runner.py — skeleton-agent 오케스트레이터.

층을 안쪽부터 순서대로 돌린다. 층마다: Implement → Gate(4단) → 실패면 사유를 붙여 재시도.
게이트를 통과한 층의 파일은 다음 층의 토대가 되고, 그 층의 경로는 동결 경로에 합류한다.

판정도 재시도도 하지 않는 것: 수용/기각. 그건 사람이 runs/<id>/<layer>/review.md 에 적는다.

usage: python -B runner.py <input.json> [--layer L0] [--max-attempts 3]
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True

for _s in (sys.stdout, sys.stderr):        # Windows 콘솔이 MS949라 유니코드 출력이 깨진다
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

HERE = Path(__file__).resolve().parent          # pipeline/
SKILL = HERE.parent                             # skeleton-agent/
sys.path.insert(0, str(HERE))

# LLM 클라이언트 포트는 형제 스킬에서 그대로 import 한다(공급자 교체 축이 이미 둘).
_SIBLING = SKILL.parent / "refactor-agent" / "pipeline"
sys.path.insert(0, str(_SIBLING))
sys.path.insert(0, str(_SIBLING / "stages"))

from scripts.llm_client import create_client   # noqa: E402
from gate import run_gate                      # noqa: E402

PROMPTS = HERE / "prompts"
SCHEMAS = HERE / "schemas"


def log(msg: str) -> None:
    print(f"[skeleton] {msg}", file=sys.stderr, flush=True)


def git_show(repo_root: Path, ref: str, path: str) -> str:
    r = subprocess.run(["git", "show", f"{ref}:{path}"], cwd=str(repo_root),
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        raise RuntimeError(f"git show {ref}:{path} failed: {r.stderr.strip()}")
    return r.stdout


def fmt_files(d: dict) -> str:
    return "\n\n".join(f"// ===== FILE: {k} =====\n{v}" for k, v in d.items())


def load_contracts(repo_root: Path, cfg: dict, layer: dict) -> str:
    """이 층이 읽기만 하는 계약의 실제 소스. 출발선에서 읽으므로 정답 구현은 섞이지 않는다."""
    base = f"{cfg['project_subdir']}/{cfg['source_root']}"
    return fmt_files({rel: git_show(repo_root, cfg["baseline_ref"], f"{base}/{rel}")
                      for rel in layer.get("contracts", [])})


def build_user(cfg: dict, layer: dict, conventions: str, rules: str,
               contracts: str, accepted: list[dict], feedback: str) -> str:
    conf = cfg["configurations"][layer["gate"]]
    u = (
        f"CONVENTIONS:\n{conventions}\n\n"
        f"BOUNDARY_RULES:\n{rules}\n\n"
        f"CONTRACTS (읽기 전용 — 시그니처를 그대로 따른다):\n{contracts}\n\n"
    )
    if accepted:
        # 앞 층이 이미 만들어 통과시킨 것. 이 층은 그 위에 쌓으므로 타입을 알아야 한다. 수정은 금지.
        u += ("ALREADY_BUILT (앞 층이 만들어 게이트를 통과한 코드 — 읽기 전용, 고치지 마라):\n"
              + fmt_files({f["path"]: f["content"] for f in accepted}) + "\n\n")
    u += (
        f"LAYER:\n"
        f"- 책임: {layer['requirements']}\n"
        f"- 쓸 수 있는 경로: {', '.join(layer['allowed_paths'])}\n"
        f"- 이 층이 채우는 자리: {layer['promotes']}\n"
        f"- 판정: {conf['desc']}\n"
    )
    if feedback:
        u += f"\nFEEDBACK — 직전 시도가 실패했다. 사유를 고쳐라(계약·경로는 그대로):\n{feedback}\n"
    return u


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("input", type=Path)
    ap.add_argument("--layer", help="이 층만 실행(생략하면 전부 순서대로)")
    ap.add_argument("--max-attempts", type=int, default=3)
    args = ap.parse_args()

    cfg = json.loads(args.input.read_text(encoding="utf-8"))
    repo_root = Path(cfg["repo_root"]).resolve()
    run_dir = SKILL / "runs" / cfg.get("run_id", "c6")
    run_dir.mkdir(parents=True, exist_ok=True)
    (run_dir / "input.json").write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")

    client = create_client(provider=cfg.get("provider", "claude"), project_dir=SKILL,
                           timeout_seconds=cfg.get("timeout", 900))
    conventions = "\n\n".join((repo_root / p).read_text(encoding="utf-8") for p in cfg["context_files"])
    rules = (SKILL / "references" / "boundary-shortcuts.md").read_text(encoding="utf-8")

    layers = cfg["layers"]
    if args.layer:
        layers = [l for l in layers if l["id"] == args.layer]
        if not layers:
            log(f"층 없음: {args.layer}")
            return 2

    # 이전 층의 수용 결과를 쌓아 올린다. --layer 로 중간부터 시작하면 앞 층 산출물을 읽어 온다.
    accepted: list[dict] = []
    frozen = list(cfg.get("frozen_paths", []))
    for prev in cfg["layers"]:
        if prev["id"] == layers[0]["id"]:
            break
        acc = run_dir / prev["id"] / "accepted.json"
        if not acc.exists():
            log(f"앞 층 산출물이 없다: {prev['id']} — 먼저 그 층을 통과시켜라")
            return 2
        accepted += json.loads(acc.read_text(encoding="utf-8"))["files"]
        frozen += prev["allowed_paths"]

    for layer in layers:
        layer_dir = run_dir / layer["id"]
        layer_dir.mkdir(parents=True, exist_ok=True)
        contracts = load_contracts(repo_root, cfg, layer)
        feedback = ""
        verdict: dict = {"verdict": "ERROR", "detail": "no attempt ran"}

        for attempt in range(1, args.max_attempts + 1):
            att_dir = layer_dir / f"attempt_{attempt:02d}"
            att_dir.mkdir(parents=True, exist_ok=True)
            log(f"{layer['id']} attempt {attempt} implement ...")

            system = (PROMPTS / "implement_layer.md").read_text(encoding="utf-8")
            user = build_user(cfg, layer, conventions, rules, contracts, accepted, feedback)
            client.run_prompt(system=system, user=user,
                              output_schema=SCHEMAS / "implement_output.schema.json",
                              output_path=att_dir / "implement.json", model=None)
            impl = json.loads((att_dir / "implement.json").read_text(encoding="utf-8"))
            files = impl.get("files", [])

            gate_cfg = dict(cfg, frozen_paths=frozen)
            verdict = run_gate(repo_root, cfg["baseline_ref"], cfg["project_subdir"],
                               cfg["source_root"], accepted, files, layer, gate_cfg)
            (att_dir / "gate.json").write_text(json.dumps(verdict, ensure_ascii=False, indent=2),
                                               encoding="utf-8")
            for f in files:                                   # 시도한 파일을 그대로 보존(검수 자료)
                p = att_dir / "files" / f["path"]
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_text(f["content"], encoding="utf-8")

            prog = verdict.get("progress", {})
            log(f"{layer['id']} attempt {attempt} [{verdict['stage']}] {verdict['verdict']}"
                + (f" | 진행도({prog.get('configuration')}): {prog.get('verdict')}" if prog else ""))

            if verdict["verdict"] == "GREEN":
                (layer_dir / "accepted.json").write_text(
                    json.dumps({"files": files, "notes": impl.get("notes", ""),
                                "attempts": attempt, "gate": verdict}, ensure_ascii=False, indent=2),
                    encoding="utf-8")
                break
            feedback = f"[{verdict['stage']}] {verdict['verdict']}\n{verdict['detail']}"

        if verdict["verdict"] != "GREEN":
            log(f"{layer['id']}: {args.max_attempts}회 시도에도 통과 못 함 — 사람 판단 필요")
            return 1

        accepted += json.loads((layer_dir / "accepted.json").read_text(encoding="utf-8"))["files"]
        frozen += layer["allowed_paths"]                      # 뒤 층이 앞 층을 고쳐 회피하지 못하게

    log("완료. 층별 review.md 는 사람이 작성한다.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
