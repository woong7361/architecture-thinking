from __future__ import annotations

import json
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path

CLAUDE_DEFAULT_MODEL = "claude-sonnet-4-6"


# 스테이지가 읽을 수 있는 것은 프롬프트뿐이어야 한다. 파일 도구가 열려 있으면
# brief에 없는 재료가 초안에 들어오고, 평가자는 자기가 대조당하는 임계값을 읽을 수 있다.
BLOCKED_TOOLS = ("Read", "Glob", "Grep", "Bash", "Edit", "Write", "NotebookEdit", "WebFetch", "WebSearch", "Task")


@dataclass(frozen=True)
class ClaudeClient:
    timeout_seconds: int = 600
    claude_bin: str = "claude"

    def run_prompt(
        self,
        system: str,
        user: str,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
    ) -> dict | None:
        schema_content = output_schema.read_text(encoding="utf-8")
        user_with_schema = f"{user}\nOUTPUT_SCHEMA (follow this exactly, output only valid JSON matching this schema):\n{schema_content}"
        prompt = f"{system}\n\n{user_with_schema}"
        command = self._build_command(model)

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
                f"claude CLI timed out\ncommand: {command}\ntimeout_seconds: {self.timeout_seconds}"
            ) from exc

        if completed.returncode != 0:
            raise RuntimeError(
                f"claude CLI failed\ncommand: {command}\nstdout: {completed.stdout}\nstderr: {completed.stderr}"
            )

        output_data = _parse_json(completed.stdout)
        output_path.write_text(json.dumps(output_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return None

    def _build_command(self, model: str | None) -> list[str]:
        command = [self.claude_bin, "-p", "-", "--output-format", "text"]
        command.extend(["--disallowed-tools", *BLOCKED_TOOLS])
        if model:
            command.extend(["--model", model])
        return command


def _parse_json(text: str) -> dict:
    text = text.strip()
    # strip markdown code block wrapper if present
    if text.startswith("```"):
        lines = text.splitlines()
        if lines[-1].strip() == "```":
            text = "\n".join(lines[1:-1]).strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError as exc:
        raise ValueError(f"claude response is not valid JSON: {text[:300]}") from exc
