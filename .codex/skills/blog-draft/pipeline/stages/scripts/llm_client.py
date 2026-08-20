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
    timeout_seconds: int,
    codex_bin: str = "codex",
) -> LLMClient:
    """Build a stage client. Stages get no working directory of their own:
    each call runs in a throwaway empty one so the prompt is the only input."""
    if provider == "claude":
        from stages.scripts.claude_client import ClaudeClient
        return ClaudeClient(timeout_seconds=timeout_seconds)
    from stages.scripts.codex_client import CodexClient
    return CodexClient(codex_bin=codex_bin, timeout_seconds=timeout_seconds)
