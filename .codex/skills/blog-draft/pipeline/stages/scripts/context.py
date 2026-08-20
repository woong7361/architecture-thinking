from __future__ import annotations

from pathlib import Path

# context.py lives at <skill_root>/pipeline/stages/scripts/context.py
SKILL_ROOT = Path(__file__).resolve().parents[3]
SOUL_PATH = SKILL_ROOT / "soul.md"
MEMORY_PATH = SKILL_ROOT / "memory.md"
CRAFT_PATH = SKILL_ROOT / "craft.md"

COMMON_SECTION = "공통"
BANNED_TERMS_SECTION = "본문 금지 용어"
PIECE_TYPE_PREFIX = "piece_type:"


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


def load_craft_context(piece_type: str | None = None, craft_path: Path = CRAFT_PATH) -> str:
    """Load genre technique rules that apply to this piece.

    Returns the common section plus the section matching `piece_type`, or an
    empty string when the file is missing. Like the persona block this goes to
    gen/refine only: technique is a means of generation, the rubric is the
    standard for the result, and injecting technique into the evaluator turns
    "used the technique" into "is a good piece".
    """
    text = _read_if_present(craft_path)
    if not text:
        return ""

    wanted = {COMMON_SECTION}
    if piece_type:
        wanted.add(f"{PIECE_TYPE_PREFIX} {piece_type}")

    sections = [
        f"## {title}\n\n{body}"
        for title, body in _split_sections(text)
        if title in wanted and body
    ]
    if not sections:
        return ""

    body = "\n\n".join(sections)
    return (
        "# CRAFT_CONTEXT\n"
        "다음은 이 글이 따르는 장르 기법이다. 글의 재료가 아니라 '어떻게 쓸지'에 대한 지침이다.\n"
        "AUTHOR_CONTEXT의 목소리와 충돌하면 AUTHOR_CONTEXT를 따른다. 기법이 저자의 목소리를 덮지 않는다.\n"
        "기법이 요구하는 재료가 brief에 없으면 그 기법을 쓰지 않는다. 재료 없이 기법만 적용하면 지어내게 된다.\n\n"
        f"{body}\n"
    )


def load_banned_terms(craft_path: Path = CRAFT_PATH) -> list[str]:
    """Technique vocabulary that must not surface in the draft body.

    Kept in craft.md next to the rules that introduce the words, but never
    injected into a prompt: naming the words in an instruction is itself a way
    of summoning them. The check is deterministic instead.
    """
    text = _read_if_present(craft_path)
    if not text:
        return []
    for title, body in _split_sections(text):
        if title != BANNED_TERMS_SECTION:
            continue
        return [
            line.lstrip("- ").strip()
            for line in body.splitlines()
            if line.strip().startswith("- ")
        ]
    return []


def _split_sections(text: str) -> list[tuple[str, str]]:
    """Split markdown into (h2 title, body) pairs. Content before the first h2 is dropped."""
    sections: list[tuple[str, str]] = []
    title: str | None = None
    lines: list[str] = []
    for line in text.splitlines():
        if line.startswith("## "):
            if title is not None:
                sections.append((title, "\n".join(lines).strip()))
            title = line[3:].strip()
            lines = []
        elif title is not None:
            lines.append(line)
    if title is not None:
        sections.append((title, "\n".join(lines).strip()))
    return sections


def _read_if_present(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8").strip()
