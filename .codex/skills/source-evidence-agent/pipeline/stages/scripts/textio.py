"""외부 명령의 출력을 잃지 않고 읽는다.

이 기계의 윈도우 도구들은 한국어를 cp949로 낸다. `subprocess`를 `encoding="utf-8"`로 열면 그 바이트에서
리더 스레드가 `UnicodeDecodeError`로 죽고 호출 자체가 실패한다. 실제로 세 iteration을 돌린 run이 그렇게
끝났다. 도구의 출력 인코딩 때문에 판정이 멈추는 것은 판정의 문제가 아니다.

그래서 바이트로 받아 순서대로 풀어 본다. utf-8을 먼저 시도하고, 실패하면 cp949, 그것도 실패하면 손실을
받아들이고 `replace`로 푼다. 우선순위가 utf-8인 이유는 우리 산출물이 전부 utf-8이고 cp949는 도구 쪽
사정이기 때문이다.
"""

from __future__ import annotations

import subprocess
from dataclasses import dataclass
from pathlib import Path

# 풀어 볼 순서. 우리 산출물이 utf-8이므로 그것이 먼저다.
DECODE_ORDER = ("utf-8", "cp949")


def decode_output(raw: bytes | str | None) -> str:
    """바이트를 잃지 않고 문자열로 만든다. 어느 인코딩으로도 못 풀면 replace로 푼다."""
    if raw is None:
        return ""
    if isinstance(raw, str):
        return raw
    for encoding in DECODE_ORDER:
        try:
            return raw.decode(encoding)
        except UnicodeDecodeError:
            continue
    return raw.decode(DECODE_ORDER[0], errors="replace")


@dataclass(frozen=True)
class CommandResult:
    returncode: int
    stdout: str
    stderr: str


def run_command(
    command: list[str],
    cwd: Path | str | None = None,
    input_text: str | None = None,
    timeout: int | None = None,
    env: dict[str, str] | None = None,
) -> CommandResult:
    """명령을 돌리고 출력을 안전하게 푼다.

    `text=True`와 `encoding=`을 쓰지 않는다. 그것을 쓰면 디코딩이 리더 스레드 안에서 일어나고, 거기서 터지면
    호출자가 손쓸 수 없다. 바이트로 받아 이 자리에서 푼다.
    """
    completed = subprocess.run(
        command,
        cwd=str(cwd) if cwd is not None else None,
        input=input_text.encode("utf-8") if input_text is not None else None,
        capture_output=True,
        timeout=timeout,
        env=env,
    )
    return CommandResult(
        returncode=completed.returncode,
        stdout=decode_output(completed.stdout),
        stderr=decode_output(completed.stderr),
    )


def harden_stream(stream) -> None:
    """콘솔이 좁은 코드페이지일 때 로그 한 줄이 run을 죽이지 않게 한다.

    진행 로그에 한국어가 들어가는데 콘솔이 cp949면 `UnicodeEncodeError`가 날 수 있다. 로그를 쓰다 죽는 것은
    판정과 아무 관계가 없으므로 글자를 잃는 쪽을 고른다.
    """
    reconfigure = getattr(stream, "reconfigure", None)
    if reconfigure is None:
        return
    try:
        reconfigure(errors="replace")
    except (ValueError, OSError):
        pass


def save_raw(raw_path: Path | None, text: str) -> None:
    """모델 응답 원문을 파싱 전에 남긴다.

    실패했을 때만 남기면 실패 경로에 버그가 있을 때 아무것도 안 남는다. 그래서 성공해도 남긴다.
    원문이 없으면 응답이 잘려서 온 것인지 우리 파서가 틀린 것인지 판별할 방법이 없다.
    """
    if raw_path is None:
        return
    try:
        raw_path.parent.mkdir(parents=True, exist_ok=True)
        raw_path.write_bytes(text.encode("utf-8", errors="replace"))
    except OSError:
        pass
