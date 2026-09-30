from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from pathlib import Path

from stages.scripts.llm_client import SANDBOX_READ_ONLY, SANDBOX_WORKSPACE_WRITE
from stages.scripts.textio import run_command, save_raw

CLAUDE_DEFAULT_MODEL = "claude-sonnet-4-6"

# 셸. 두 단계 모두에서 뺀다. 빌드는 하네스가 돌리므로 모델이 셸을 쓸 일이 없고, 열어 두면 편집 승인과
# 무관하게 작업 폴더 밖을 건드릴 길이 생긴다.
SHELL_TOOLS = ("Bash", "BashOutput", "KillShell")
# 읽기 단계에서 빼는 것. 쓰기 도구와 셸을 아예 목록에서 없앤다.
WRITE_TOOLS = ("Write", "Edit", "NotebookEdit", *SHELL_TOOLS)


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
        work_dir: Path | None = None,
        sandbox: str = SANDBOX_READ_ONLY,
        raw_path: Path | None = None,
        on_note=None,
    ) -> dict | None:
        schema_content = output_schema.read_text(encoding="utf-8")
        user_with_schema = f"{user}\nOUTPUT_SCHEMA (follow this exactly, output only valid JSON matching this schema):\n{schema_content}"
        prompt = f"{system}\n\n{user_with_schema}"
        command = self._build_command(model, sandbox)
        cwd = Path(work_dir) if work_dir is not None else self.project_dir

        output_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            completed = run_command(command, cwd=cwd, input_text=prompt, timeout=self.timeout_seconds)
        except subprocess.TimeoutExpired as exc:
            raise TimeoutError(
                f"claude CLI timed out\ncommand: {command}\ntimeout_seconds: {self.timeout_seconds}"
            ) from exc

        if completed.returncode != 0:
            raise RuntimeError(
                f"claude CLI failed\ncommand: {command}\nstdout: {completed.stdout}\nstderr: {completed.stderr}"
            )

        # 파싱 전에 원문을 남긴다. 성공해도 남긴다. 고칠 때도 원문은 손대지 않는다.
        save_raw(raw_path, completed.stdout)
        output_data, repairs = _parse_json(completed.stdout)
        for repair in repairs:
            if on_note is not None:
                on_note(f"응답의 문법 흠을 고쳐 통과시켰다: {repair} (원문은 그대로 남아 있다)")
        output_path.write_text(json.dumps(output_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return None

    def _build_command(self, model: str | None, sandbox: str) -> list[str]:
        """권한을 단계에 맞게 좁힌다.

        claude CLI는 OS 샌드박스가 아니라 권한 체계로 막으므로 codex의 `--sandbox`보다 약하다.
        그래도 구조로 막는 것은 같다. 작업 디렉터리를 작업 폴더로 두고, 프롬프트가 물어봐야 하는 일은
        `--permission-prompts none`이 자동으로 거절한다. 그래서 작업 폴더 밖 쓰기는 통과하지 못한다.
        읽기 단계에서는 쓰기 도구와 셸을 아예 뺀다. 우회 플래그(`--dangerously-skip-permissions`)는 쓰지 않는다.
        """
        command = [self.claude_bin, "-p", "-", "--output-format", "text", "--permission-prompts", "none"]
        if model:
            command.extend(["--model", model])
        # 편집은 자동 승인하고 그 밖의 경로는 물어봐야 하므로 거절된다. 읽기 도구는 두 단계 모두에서
        # 프롬프트 없이 되어야 한다 — 채점자가 더 보고 싶은 것을 볼 수 있어야 하고, `manual`로 두면
        # 읽기까지 거절될 수 있다. 무엇을 못 하게 하는가는 권한 모드가 아니라 도구 목록이 정한다.
        command.extend(["--permission-mode", "acceptEdits"])
        # 셸은 두 단계 모두에서 뺀다. 빌드는 하네스가 돌리고, 파일은 Read로 읽고, 계약 경로는 payload가
        # 준다. 셸을 열어 두면 편집 승인과 무관하게 작업 폴더 밖을 건드릴 길이 생긴다.
        disallowed = list(SHELL_TOOLS) if sandbox == SANDBOX_WORKSPACE_WRITE else list(WRITE_TOOLS)
        command.extend(["--disallowed-tools", *disallowed])
        return command


def _balanced_objects(text: str) -> list[str]:
    """문자열에서 균형 잡힌 최상위 JSON 객체를 찾아 나온 순서대로 돌려준다.

    따옴표 안의 중괄호와 역슬래시 이스케이프를 세지 않는다. 그러지 않으면 본문에 `}`가 있는 자바 코드가
    괄호 셈을 망친다.
    """
    objects: list[str] = []
    depth = 0
    start = -1
    in_string = False
    escaped = False
    for index, char in enumerate(text):
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "{":
            if depth == 0:
                start = index
            depth += 1
        elif char == "}":
            if depth > 0:
                depth -= 1
                if depth == 0 and start >= 0:
                    objects.append(text[start : index + 1])
                    start = -1
    return objects


def _brace_balance(text: str) -> tuple[int, int]:
    """문자열 밖의 중괄호 여닫이 개수. 잘린 응답인지 가리는 근거다."""
    opens = closes = 0
    in_string = False
    escaped = False
    for char in text:
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "{":
            opens += 1
        elif char == "}":
            closes += 1
    return opens, closes


def strip_trailing_commas(text: str) -> tuple[str, int]:
    """닫는 괄호 앞의 쉼표를 뗀다. 뗀 개수를 함께 돌려준다.

    끝쉼표는 뜻을 바꾸지 않는 문법 흠이다. 모델이 객체 마지막 항목 뒤에 쉼표 하나를 남긴 것으로 9분과
    한 번의 호출을 버린 일이 있었다. 그것은 이 하네스가 재려는 것과 무관하다. 문자열 안의 쉼표는 세지 않는다.
    """
    result: list[str] = []
    removed = 0
    in_string = False
    escaped = False
    for index, char in enumerate(text):
        if in_string:
            result.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
            result.append(char)
            continue
        if char == ",":
            rest = text[index + 1 :]
            stripped_rest = rest.lstrip()
            if stripped_rest[:1] in ("}", "]"):
                removed += 1
                continue
        result.append(char)
    return "".join(result), removed


def response_reason(text: str) -> str:
    """응답이 왜 JSON이 아닌지 이름을 붙인다.

    넷을 가른다. `ok`는 건져지는 것, `truncated`는 끝까지 오지 않은 것, `absent`는 JSON이 아예 없는 것,
    `syntax`는 균형은 맞고 끝까지 오는데 문법이 깨진 것이다. 하나로 부르면 다음에 같은 것을 보고도 무엇인지
    모른다. 끝쉼표가 그 자리였다 — 잘린 것으로 읽고 파서를 더 만질 뻔했다.
    """
    if _attempt_parse(text) is not None:
        return "ok"
    stripped = text.strip()
    opens, closes = _brace_balance(stripped)
    if stripped.startswith("{") and opens > closes:
        return "truncated"
    if not _balanced_objects(text):
        return "absent"
    return "syntax"


def describe_response(text: str, head: int = 500, tail: int = 500) -> str:
    """응답을 진단할 수 있게 적는다.

    앞부분만 실으면 잘린 응답인지 알 수 없다. 잘렸는지는 끝과 괄호 셈이 말한다.
    """
    stripped = text.strip()
    opens, closes = _brace_balance(stripped)
    return (
        f"reason={response_reason(text)} length={len(stripped)} "
        f"braces open={opens} close={closes} unbalanced={opens - closes}\n"
        f"--- head {head}\n{stripped[:head]}\n"
        f"--- tail {tail}\n{stripped[-tail:]}"
    )


def _attempt_parse(text: str) -> dict | None:
    """그대로, 그리고 균형 잡힌 객체를 뒤에서부터 시도한다.

    산문이 앞에 붙는 경우가 더 흔하고 마지막 객체가 답일 가능성이 높으므로 뒤에서부터 본다.
    """
    stripped = text.strip()
    try:
        parsed = json.loads(stripped)
    except json.JSONDecodeError:
        pass
    else:
        if isinstance(parsed, dict):
            return parsed

    for candidate in reversed(_balanced_objects(text)):
        try:
            parsed = json.loads(candidate)
        except json.JSONDecodeError:
            continue
        if isinstance(parsed, dict):
            return parsed
    return None


def _parse_json(text: str) -> tuple[dict, list[str]]:
    """응답에서 JSON 객체를 건져낸다. 고친 것이 있으면 무엇을 고쳤는지 함께 돌려준다.

    codex는 `--output-schema`로 구조를 강제하지만 claude 경로에는 그 장치가 없다. 순수 JSON이 오는 것이
    보통이어도 앞뒤에 한 줄이나 끝쉼표 하나가 붙는 것만으로 한 iteration을 버리게 된다.

    고치는 것은 뜻을 바꾸지 않는 문법 흠까지다. **원문은 손대지 않고** 사본으로 파싱하며, 고친 것은
    기록으로 남긴다. 건지지 못하면 무엇이 왔는지 진단할 수 있게 적어 던진다.
    """
    parsed = _attempt_parse(text)
    if parsed is not None:
        return parsed, []

    repaired, removed = strip_trailing_commas(text)
    if removed:
        parsed = _attempt_parse(repaired)
        if parsed is not None:
            return parsed, [f"trailing_comma x{removed}"]

    raise ValueError("claude response has no usable JSON object.\n" + describe_response(text))
