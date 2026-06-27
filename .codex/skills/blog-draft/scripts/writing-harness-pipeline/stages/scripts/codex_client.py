from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class CodexClient:
    codex_bin: str
    project_dir: Path
    timeout_seconds: int = 600
    bypass_approvals_and_sandbox: bool = True

    def run_prompt(
        self,
        prompt: str,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
    ) -> dict | None:
        command = self.build_command(
            output_schema=output_schema,
            output_path=output_path,
            model=model,
        )

        output_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            completed = subprocess.run(
                command,
                input=prompt,
                text=True,
                capture_output=True,
                encoding="utf-8",
                timeout=self.timeout_seconds,
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
            ]
        )
        if self.bypass_approvals_and_sandbox:
            command.append("--dangerously-bypass-approvals-and-sandbox")

        command.extend(
            [
                "-C",
                str(self.project_dir),
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
