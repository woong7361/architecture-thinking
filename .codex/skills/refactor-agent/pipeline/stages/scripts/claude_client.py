from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from pathlib import Path

CLAUDE_DEFAULT_MODEL = "claude-sonnet-4-6"


@dataclass(frozen=True)
class ClaudeClient:
    project_dir: Path
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

        # 진단용 raw 저장(파싱 실패해도 남는다)
        output_path.with_suffix(".raw.txt").write_text(completed.stdout, encoding="utf-8")
        output_data = _parse_json(completed.stdout)
        output_path.write_text(json.dumps(output_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return None

    def _build_command(self, model: str | None) -> list[str]:
        command = [self.claude_bin, "-p", "-", "--output-format", "text"]
        if model:
            command.extend(["--model", model])
        return command


def _parse_json(text: str) -> dict:
    text = text.strip()
    # strip markdown code block wrapper if present (```json ... ```)
    if text.startswith("```"):
        lines = text.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines).strip()
    try:
        return json.loads(text, strict=False)
    except json.JSONDecodeError:
        # fallback: 서문·후문을 무시하고 첫 {부터 마지막 }까지 균형 객체를 추출
        start, end = text.find("{"), text.rfind("}")
        if start != -1 and end > start:
            try:
                return json.loads(text[start:end + 1], strict=False)
            except json.JSONDecodeError as exc:
                raise ValueError(f"claude response is not valid JSON: {text[:300]}") from exc
        raise ValueError(f"claude response is not valid JSON: {text[:300]}")
