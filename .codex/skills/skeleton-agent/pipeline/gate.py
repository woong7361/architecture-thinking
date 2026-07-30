"""gate.py — 층 판정의 사실 층(결정적, LLM 없음).

출발선 worktree에 지금까지 수용된 층 + 이번 층의 매니페스트를 적용하고 4단으로 채점한다.
원본 트리는 건드리지 않는다. 테스트 코드는 절대 쓰지 않는다.

    ① 경로 규약  매니페스트가 허용 경로 안인가, 동결 경로를 안 건드렸나   (밀리초)
    ② 컴파일     빌드가 되나                                              (초)
    ③ 경계 규칙  의존 방향 정적 검사                                       (초)
    ④ 인수테스트 그 층의 구성으로 판정                                      (분)

싼 검출기를 먼저 돌려 비싼 실행을 아낀다. 앞 단이 실패하면 뒤는 돌지 않는다.

refactor-agent/pipeline/behavior_gate.py 의 worktree 적용 패턴을 가져와 개조했다.
차이: 판정이 "회귀 없음"이 아니라 "새 GREEN"이고, 단이 4개이며, 진행도 관측이 붙는다.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

TEST_PATH_HINTS = ("src/test", "/test/", "test/java")


def _run(cmd, cwd=None, env=None, timeout=1800):
    shell = isinstance(cmd, str)
    return subprocess.run(
        cmd, cwd=cwd, env=env, shell=shell,
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout,
    )


def _sanitize(rel: str) -> str:
    rel = rel.strip().replace("\\", "/").lstrip("/")
    parts = [p for p in rel.split("/") if p not in ("", ".")]
    if not parts or any(p == ".." for p in parts):
        raise ValueError(f"unsafe manifest path: {rel!r}")
    return "/".join(parts)


def check_paths(files: list[dict], allowed: list[str], frozen: list[str]) -> list[str]:
    """① 경로 규약. 가장 싼 검출기이자 경계 위반의 1차 방어.

    잡는 것: 안쪽 층 파일을 고쳐 문제를 풀려는 시도, 계약 변경, 심판 매수.
    """
    violations = []
    for f in files:
        try:
            rel = _sanitize(f.get("path", ""))
        except ValueError as e:
            violations.append(str(e))
            continue
        if any(hint in rel for hint in TEST_PATH_HINTS):
            violations.append(f"테스트 경로에 쓰려 함(심판은 수정 불가): {rel}")
            continue
        if any(rel.startswith(fp.rstrip("/") + "/") or rel == fp for fp in frozen):
            violations.append(f"동결 경로를 수정하려 함: {rel}")
            continue
        if not any(rel.startswith(ap.rstrip("/") + "/") or rel == ap for ap in allowed):
            violations.append(f"허용 경로 밖: {rel} (허용: {', '.join(allowed)})")
    return violations


def _classify(returncode: int, out: str) -> tuple[str, str]:
    """실행 결과를 판정으로 옮긴다. 컴파일 실패와 테스트 실패를 구분한다."""
    if returncode == 0:
        m = re.search(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+)", out)
        return "GREEN", m.group(0) if m else "BUILD SUCCESS"
    if "COMPILATION ERROR" in out or "cannot find symbol" in out:
        errs = [l.strip() for l in out.splitlines()
                if ("cannot find symbol" in l or (".java:" in l and "ERROR" in l))][:10]
        return "COMPILE_FAIL", "\n".join(errs) or "compilation error"
    fails = [l.strip() for l in out.splitlines()
             if l.strip().startswith("[ERROR]") and ("Tests run" in l or "<<<" in l)][:10]
    m = re.search(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+)", out)
    detail = "\n".join(fails) or (m.group(0) if m else "BUILD FAILURE")
    return "RED", detail


def _exec(cmd_template: list[str], project_dir: Path, env: dict) -> tuple[str, str]:
    cmd = [c.replace("{project}", project_dir.as_posix()) for c in cmd_template]
    r = _run(cmd, cwd=str(project_dir), env=env)
    return _classify(r.returncode, (r.stdout or "") + "\n" + (r.stderr or ""))


def run_gate(repo_root: Path, baseline_ref: str, project_subdir: str, source_root: str,
             accepted: list[dict], new_files: list[dict], layer: dict, cfg: dict) -> dict:
    """4단 채점 + 진행도 관측. worktree는 매번 새로 따고 끝나면 지운다."""
    paths = check_paths(new_files, layer["allowed_paths"], cfg.get("frozen_paths", []))
    if paths:
        return {"stage": "paths", "verdict": "PATH_VIOLATION", "detail": "\n".join(paths)}

    wt = Path(tempfile.mkdtemp(prefix="skeleton-gate-", dir=cfg.get("worktree_parent") or None))
    try:
        add = _run(["git", "worktree", "add", "--detach", str(wt), baseline_ref], cwd=str(repo_root))
        if add.returncode != 0:
            return {"stage": "setup", "verdict": "ERROR", "detail": add.stderr.strip()}

        proj = wt / project_subdir
        src = proj / source_root
        for f in accepted + new_files:                       # 앞 층 결과 위에 이번 층을 쌓는다
            dest = src / _sanitize(f["path"])
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(f["content"], encoding="utf-8")

        env = dict(os.environ)
        if cfg.get("java_home"):
            env["JAVA_HOME"] = cfg["java_home"]

        for stage, cmd in (("compile", cfg["compile_cmd"]), ("boundary", cfg["boundary_cmd"])):
            verdict, detail = _exec(cmd, proj, env)
            if verdict != "GREEN":
                return {"stage": stage, "verdict": verdict, "detail": detail}

        conf = cfg["configurations"][layer["gate"]]
        verdict, detail = _exec(conf["cmd"], proj, env)
        result = {"stage": "acceptance", "verdict": verdict, "detail": detail,
                  "configuration": layer["gate"]}

        # 진행도 관측 — 판정이 아니라 기록. 최종 구성은 대역이 없어 속지 않는다.
        final_id = cfg.get("final_configuration")
        if final_id and final_id != layer["gate"]:
            fv, fd = _exec(cfg["configurations"][final_id]["cmd"], proj, env)
            result["progress"] = {"configuration": final_id, "verdict": fv, "detail": fd}
        return result
    finally:
        _run(["git", "worktree", "remove", "--force", str(wt)], cwd=str(repo_root))
        shutil.rmtree(wt, ignore_errors=True)
