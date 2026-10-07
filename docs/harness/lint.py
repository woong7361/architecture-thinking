# -*- coding: utf-8 -*-
"""카드가 자기 작성 규칙을 지키는지 검사한다. 읽어서 판단하지 않고 기계로 센다."""
from pathlib import Path
import re, sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ORDER = ["무엇을 막는가", "원리", "메커니즘", "예", "경계", "검증"]
MAX_LINES = 60
# 돌리는 쪽의 선언으로 통과되는 항목
SELF_REPORT = ["말할 수 있는가", "판단할 수 있는가", "알 수 있는가", "확인했는가", "충분한가", "적절한가"]
# 판정 대상이 될 수 있는 명사
ANCHOR = ["payload", "스키마", "목록", "필드", "개수", "수가", "수와", "수는", "기록", "이력", "분류",
          "경로", "산출물", "인자", "표", "코드", "태그", "표본", "파일", "커밋", "값", "횟수", "상태",
          "분산", "분포", "근거", "점수", "키", "run", "trace", "앵커", "식별자", "이유", "사유",
          "카운터", "통계", "프롬프트", "단계", "테스트", "입력", "규칙"]
VAGUE = ["적절", "충분", "자연스럽", "제대로", "되도록", "가능한 한", "적당", "어느 정도", "바람직"]

def main():
    root = Path(__file__).parent
    cards = sorted(p for p in root.glob("*.md") if p.name != "index.md")
    names = {p.stem for p in cards} | {"index"}
    fail = []
    for p in cards:
        t = p.read_text(encoding="utf-8")
        lines = t.splitlines()
        heads = [l[3:].strip() for l in lines if l.startswith("## ")]
        if heads != ORDER:
            fail.append(f"{p.name}: 슬롯이 {ORDER} 순서가 아니다: {heads}")
        if len(lines) > MAX_LINES:
            fail.append(f"{p.name}: {len(lines)}줄로 상한 {MAX_LINES}을 넘었다")
        for m in re.findall(r"\[\[([^\]]+)\]\]", t):
            if m not in names:
                fail.append(f"{p.name}: 없는 카드를 가리킨다: [[{m}]]")
        for item in re.findall(r"(?m)^- \[ \] (.+)$", t):
            if any(s in item for s in SELF_REPORT):
                fail.append(f"{p.name}: 선언으로 통과되는 항목: {item[:50]}")
            if any(v in item for v in VAGUE):
                fail.append(f"{p.name}: 모호한 표현: {item[:50]}")
            if not any(a in item for a in ANCHOR):
                fail.append(f"{p.name}: 판정 대상이 없는 항목: {item[:50]}")
            if re.fullmatch(r"[^.]*\[\[[^\]]+\]\][^.]*\.?", item):
                fail.append(f"{p.name}: 다른 카드를 가리키는 포인터는 항목이 아니다: {item[:50]}")
    print("\n".join(fail) if fail else f"카드 {len(cards)}장 통과")
    return 1 if fail else 0

if __name__ == "__main__":
    sys.exit(main())
