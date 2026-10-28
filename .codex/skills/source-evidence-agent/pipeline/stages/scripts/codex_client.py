from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from pathlib import Path

from stages.scripts.llm_client import SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE
from stages.scripts.textio import run_command, save_raw


@dataclass(frozen=True)
class CodexClient:
    codex_bin: str
    project_dir: Path
    timeout_seconds: int = 600

    def run_prompt(
        self,
        system: str,
        user: str,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
        work_dir: Path | None = None,
        sandbox: str = SANDBOX_READ_ONLY,
        raw_path: Path | None = None,
        on_note=None,
    ) -> dict | None:
        prompt = f"{system}\n\n{user}"
        command = self.build_command(
            output_schema=output_schema,
            output_path=output_path,
            model=model,
            work_dir=work_dir,
            sandbox=sandbox,
        )

        output_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            completed = run_command(command, input_text=prompt, timeout=self.timeout_seconds)
        except subprocess.TimeoutExpired as exc:
            raise TimeoutError(
                "Codex CLI timed out in non-interactive mode.\n"
                f"command: {command}\n"
                f"timeout_seconds: {self.timeout_seconds}"
            ) from exc

        # codex는 CLI가 답을 파일로 쓴다. 그 파일이 곧 원문이므로 그대로 남긴다.
        if raw_path is not None and output_path.exists():
            save_raw(raw_path, output_path.read_text(encoding="utf-8", errors="replace"))

        if completed.returncode != 0:
            raise RuntimeError(
                "Codex CLI failed\n"
                f"command: {command}\n"
                f"stdout: {completed.stdout}\n"
                f"stderr: {completed.stderr}"
            )

        return extract_token_usage(completed.stdout)

    def build_command(
        self,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
        work_dir: Path | None = None,
        sandbox: str = SANDBOX_READ_ONLY,
    ) -> list[str]:
        """작업 루트와 샌드박스를 명시해 부른다.

        승인 우회 플래그(`--dangerously-bypass-approvals-and-sandbox`)는 쓰지 않는다. 그 플래그는
        모델이 실행하는 명령을 샌드박스 밖에서 돌리므로 저장소 전체에 쓸 수 있고, 판정 대상이 아닌 것이
        움직여도 게이트가 보지 못한다. 대신 작업 루트를 `iter_00N/files/`로 두고 쓰기를 그 아래로 제한한다.
        비평과 채점은 쓸 일이 없으므로 `read-only`로 돈다.
        """
        if sandbox not in (SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE):
            raise ValueError(f"unknown sandbox mode: {sandbox}")
        root = Path(work_dir) if work_dir is not None else self.project_dir

        command = [self.codex_bin, "exec"]
        if model:
            command.extend(["--model", model])
        command.extend(
            [
                "--ephemeral",
                "--json",
                "--sandbox",
                sandbox,
                # 작업 루트가 저장소 밖(run 폴더)일 수 있다. git 저장소가 아니어도 돌게 한다.
                "--skip-git-repo-check",
                "-C",
                str(root),
                "--output-schema",
                str(output_schema),
                "--output-last-message",
                str(output_path),
                "-",
            ]
        )
        return command


def extract_token_usage(stdout: str) -> dict | None:
    usage = None
    for line in stdout.splitlines():
        if not line.strip():
            continue
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if isinstance(event, dict) and isinstance(event.get("usage"), dict):
            usage = event["usage"]
    return usage
