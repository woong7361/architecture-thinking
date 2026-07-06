from __future__ import annotations

from pathlib import Path

# context.py lives at <skill_root>/pipeline/stages/scripts/context.py
SKILL_ROOT = Path(__file__).resolve().parents[3]
SOUL_PATH = SKILL_ROOT / "soul.md"
MEMORY_PATH = SKILL_ROOT / "memory.md"


def load_persona_context(
    soul_path: Path = SOUL_PATH,
    memory_path: Path = MEMORY_PATH,
) -> str:
    """Load durable author identity (soul) and accumulated lessons (memory).

    Returns a delimited AUTHOR_CONTEXT block to prepend to the gen/refine
    system prompt, or an empty string when neither file exists. critique and
    eval never receive this block, so the evaluator stays anchored to the
    rubric only.
    """
    sections: list[str] = []
    soul = _read_if_present(soul_path)
    if soul:
        sections.append(f"## soul.md — 저자 정체성\n\n{soul}")
    memory = _read_if_present(memory_path)
    if memory:
        sections.append(f"## memory.md — 누적 교훈\n\n{memory}")
    if not sections:
        return ""

    body = "\n\n".join(sections)
    return (
        "# AUTHOR_CONTEXT\n"
        "다음은 이 글을 쓰는 저자의 지속적 목소리·취향(soul)과 과거 피드백에서 얻은 교훈(memory)이다.\n"
        "이것은 글의 재료(raw_text)가 아니라 '어떻게 쓸지'에 대한 지침이다. 요약·인용 대상이 아니다.\n\n"
        f"{body}\n"
    )


def _read_if_present(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8").strip()
