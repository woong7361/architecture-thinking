from __future__ import annotations

import json
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class CodexClient:
    codex_bin: str
    timeout_seconds: int = 600
    sandbox_mode: str = "read-only"

    def run_prompt(
        self,
        system: str,
        user: str,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
    ) -> dict | None:
        prompt = f"{system}\n\n{user}"
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # Every stage runs in an empty directory. The pipeline's own source is
        # not material for the piece, and a stage that can read it will use it:
        # a draft has already described this pipeline's stages and validator in
        # first person from files rather than from the brief. Keeping the
        # working directory empty is what makes brief-only actually mean
        # brief-only, and it also keeps the evaluator from reading the rubric
        # thresholds it is being judged against.
        with tempfile.TemporaryDirectory(prefix="writing-harness-workspace-") as workspace:
            command = self.build_command(
                workspace=Path(workspace),
                output_schema=output_schema,
                output_path=output_path,
                model=model,
            )
            try:
                completed = subprocess.run(
                    command,
                    input=prompt,
                    text=True,
                    capture_output=True,
                    encoding="utf-8",
                    timeout=self.timeout_seconds,
                    cwd=workspace,
                )
            except subprocess.TimeoutExpired as exc:
                raise TimeoutError(
                    "Codex CLI timed out in non-interactive mode.\n"
                    f"command: {command}\n"
                    f"timeout_seconds: {self.timeout_seconds}"
                ) from exc

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
        workspace: Path,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
    ) -> list[str]:
        command = [
            self.codex_bin,
            "exec",
        ]
        if model:
            command.extend(["--model", model])

        command.extend(
            [
                "--ephemeral",
                "--json",
                "--sandbox",
                self.sandbox_mode,
                "--skip-git-repo-check",
                "-C",
                str(workspace),
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
