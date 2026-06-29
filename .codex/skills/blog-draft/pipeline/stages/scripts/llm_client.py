from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path


class LLMClient(ABC):
    @abstractmethod
    def run_prompt(
        self,
        system: str,
        user: str,
        output_schema: Path,
        output_path: Path,
        model: str | None = None,
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
