from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path

# 샌드박스 정책. 생성기는 파일을 쓰는 도구를 들고 돌기 때문에 저장소를 고칠 수 있다.
# 쓰기를 run 안에 가두는 것은 프롬프트의 약속이 아니라 이 값이 한다.
SANDBOX_READ_ONLY = "read-only"
SANDBOX_WORKSPACE_WRITE = "workspace-write"


class LLMClient(ABC):
    @abstractmethod
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
    ) -> dict | None: ...


def create_client(
    provider: str,
    project_dir: Path,
    timeout_seconds: int,
    codex_bin: str = "codex",
) -> LLMClient:
    if provider == "claude":
        from stages.scripts.claude_client import ClaudeClient
        return ClaudeClient(project_dir=project_dir, timeout_seconds=timeout_seconds)
    from stages.scripts.codex_client import CodexClient
    return CodexClient(codex_bin=codex_bin, project_dir=project_dir, timeout_seconds=timeout_seconds)
