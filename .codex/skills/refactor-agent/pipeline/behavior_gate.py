"""behavior_gate.py — Validate의 사실 층(결정적, LLM 없음).

baseline(경계-클린)에 리팩토링 매니페스트를 격리 적용하고 동결 경계 테스트를 돌려
GREEN/RED/COMPILE_FAIL을 판정한다. 원본 트리는 안 건드린다(worktree). 테스트 코드는 안 쓴다.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


def _run(cmd, cwd=None, env=None, timeout=900):
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


def run_gate(repo_root: Path, baseline_ref: str, project_subdir: str, source_root: str,
             files: list[dict], test_cmd, java_home: str | None) -> dict:
    """매니페스트를 baseline worktree에 적용 후 경계 테스트 실행. verdict 반환."""
    wt = Path(tempfile.mkdtemp(prefix="refactor-gate-"))
    try:
        add = _run(["git", "worktree", "add", "--detach", str(wt), baseline_ref], cwd=str(repo_root))
        if add.returncode != 0:
            return {"verdict": "ERROR", "detail": f"worktree add failed: {add.stderr.strip()}"}

        src_root = wt / project_subdir / source_root
        for f in files:
            dest = src_root / _sanitize(f["path"])
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(f["content"], encoding="utf-8")

        env = dict(os.environ)
        if java_home:
            env["JAVA_HOME"] = java_home
        proj = wt / project_subdir
        tr = _run(test_cmd, cwd=str(proj), env=env)
        out = (tr.stdout or "") + "\n" + (tr.stderr or "")

        if tr.returncode == 0:
            m = re.search(r"(\d+) Scenarios \((\d+) passed\)", out)
            return {"verdict": "GREEN", "detail": m.group(0) if m else "BUILD SUCCESS"}
        if "COMPILATION ERROR" in out or "cannot find symbol" in out:
            errs = [l.strip() for l in out.splitlines()
                    if ("cannot find symbol" in l or (".java:" in l and "ERROR" in l))][:8]
            return {"verdict": "COMPILE_FAIL", "detail": "\n".join(errs) or "compilation error"}
        m = re.search(r"(\d+) Scenarios \(([^)]*)\)", out)
        return {"verdict": "RED", "detail": m.group(0) if m else "BUILD FAILURE"}
    finally:
        _run(["git", "worktree", "remove", "--force", str(wt)], cwd=str(repo_root))
        shutil.rmtree(wt, ignore_errors=True)
