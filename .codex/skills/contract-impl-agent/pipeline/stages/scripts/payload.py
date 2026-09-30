from __future__ import annotations

import json
from pathlib import Path


def read_prompt(path: Path) -> str:
    """단계 프롬프트를 읽는다. 프롬프트는 조각 D가 소유하므로 없을 때 그 사실을 말한다."""
    if not path.exists():
        raise FileNotFoundError(
            f"stage prompt not found: {path}\n"
            "프롬프트는 pipeline/prompts/ 에 있고 다른 조각이 소유한다. 붙기 전에는 이 단계를 돌릴 수 없다."
        )
    return path.read_text(encoding="utf-8")


def block(label: str, value: object) -> str:
    """payload 한 덩이를 라벨 붙은 JSON 블록으로 만든다."""
    return f"{label}:\n{json.dumps(value, ensure_ascii=False, indent=2)}\n"


def compose(*blocks: str) -> str:
    return "\n".join(part for part in blocks if part)


# draft 안에서 다른 단계의 산출에 대한 응답을 담는 칸. `repairs`는 게이트 위반을 인용하고
# `ignored_suggestions`는 비평의 발의를 인용한다. draft를 통째로 실으면 정보 차단 표가 막은 것이
# 이 칸을 타고 새므로, 보면 안 되는 단계에 실을 때 뺀다.
GATE_ECHO = ("repairs",)
CRITIQUE_ECHO = ("ignored_suggestions",)


def draft_without(draft: dict, keys: tuple[str, ...]) -> dict:
    """draft에서 `keys`를 뺀 사본. 원본 산출물은 건드리지 않는다."""
    return {key: value for key, value in (draft or {}).items() if key not in keys}

def contract_version_paths(work_dir: Path) -> list[str]:
    """작업 폴더에 실재하는 계약 판본의 상대경로.

    이름을 짜맞추게 두면 모델이 틀린다. `<이름>-v<n>.yaml`의 `<이름>`을 확장자까지 포함한 파일명으로 읽어
    `tennis-alert-api.yaml-v1.yaml`을 선언한 run이 있었다. runner는 그 이름을 확정적으로 알고 있으므로
    추측할 것을 남기지 않는다.
    """
    directory = work_dir / "contract"
    if not directory.is_dir():
        return []
    return sorted(
        path.relative_to(work_dir).as_posix() for path in directory.rglob("*") if path.is_file()
    )


def workspace_block(work_dir: Path | None, writable: bool) -> str:
    """작업 폴더와 그 자리에서 읽으라는 한 줄. 구현은 payload가 아니라 이 자리에 있다."""
    if work_dir is None:
        return ""
    versions = contract_version_paths(Path(work_dir))
    return block(
        "WORKSPACE_JSON",
        {
            "path": str(work_dir),
            "cwd": ".",
            "access": "read-write" if writable else "read-only",
            "contract_versions": versions,
            "contract_version_note": (
                "작업 폴더에 실재하는 계약 판본의 경로다. 이 문자열을 그대로 쓴다. "
                "이름을 짜맞추지 마라. 판본을 올릴 때만 판 번호를 올린 새 이름을 만든다."
            ),
            "note": (
                "구현은 이 자리에서 읽어라. 파일 내용은 payload에 없다."
                + (" 파일은 이 폴더 아래에만 쓴다." if writable else " 이 단계는 쓰지 않는다.")
            ),
        },
    )

