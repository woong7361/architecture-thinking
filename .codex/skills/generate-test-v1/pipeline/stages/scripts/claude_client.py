from __future__ import annotations

import json
import re
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

        output_data = _parse_json(completed.stdout)
        output_path.write_text(json.dumps(output_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return None

    def _build_command(self, model: str | None) -> list[str]:
        command = [self.claude_bin, "-p", "-", "--output-format", "text"]
        if model:
            command.extend(["--model", model])
        return command


def _parse_json(text: str) -> dict:
    """모델 응답에서 JSON 객체를 견고하게 뽑는다. 잡담 preamble·코드펜스가 앞뒤에 붙어도
    (1) 원문 통째로 → (2) ```json 펜스 블록 → (3) 첫 균형 {...} 순으로 시도하고,
    각 후보는 원문과 후행쉼표 제거본 둘 다 파싱을 시도한다(LLM이 흔히 내는 trailing comma 복구)."""
    text = text.strip()
    for candidate in _json_candidates(text):
        for variant in (candidate, _strip_trailing_commas(candidate)):
            try:
                return json.loads(variant)
            except json.JSONDecodeError:
                continue
    raise ValueError(f"claude response is not valid JSON (len={len(text)}): {text}")


def _strip_trailing_commas(text: str) -> str:
    """`}`·`]` 바로 앞의 후행 쉼표를 제거한다. 문자열 리터럴 안의 쉼표는 건드리지 않는다."""
    out: list[str] = []
    in_string = False
    escape = False
    length = len(text)
    for index, char in enumerate(text):
        if in_string:
            out.append(char)
            if escape:
                escape = False
            elif char == "\\":
                escape = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
            out.append(char)
            continue
        if char == ",":
            look = index + 1
            while look < length and text[look] in " \t\r\n":
                look += 1
            if look < length and text[look] in "}]":
                continue  # 후행 쉼표 — 버린다
        out.append(char)
    return "".join(out)


def _json_candidates(text: str):
    yield text
    # ```json ... ``` 또는 ``` ... ``` 펜스(위치 무관). preamble 뒤 코드블록을 잡는다.
    for match in re.finditer(r"```(?:json)?\s*\n(.*?)```", text, re.DOTALL):
        yield match.group(1).strip()
    # 펜스가 없어도 문자열-인식 중괄호 매칭으로 첫 균형 객체를 뽑는다.
    balanced = _first_balanced_object(text)
    if balanced is not None:
        yield balanced


def _first_balanced_object(text: str) -> str | None:
    start = text.find("{")
    if start == -1:
        return None
    depth = 0
    in_string = False
    escape = False
    for index in range(start, len(text)):
        char = text[index]
        if in_string:
            if escape:
                escape = False
            elif char == "\\":
                escape = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    return None
