"""인용과 좌표를 사본에 대고 검증한다.

이 게이트가 환각 인용을 전부 잡는다. 인용문이 그 경로의 그 자리에 문자열로 있는지만 보므로 판단이
들어가지 않는다. 그 인용이 주장을 함의하는지는 루브릭의 `grounding` 이 본다.

비교 전에 줄 끝을 LF 로 정규화한다. 정규화하지 않으면 사본을 어디서 받았는지에 따라 멀쩡한 인용이
탈락한다.
"""

from __future__ import annotations

import re
from pathlib import Path

MAX_QUOTES_PER_CLAIM = 3
MAX_QUOTE_LINES = 40

# 주장의 종류마다 인용으로 쓸 수 있는 출처 등급이 다르다.
#
# 사실 주장은 원문이 그렇다고 말하는 것이라 1차 출처만 근거가 된다. 선택 주장은 "대상 코드가 이 방식을
# 택했다"이므로 그 증거가 코드다. 코드를 인용에서 막으면 선택 주장이 근거를 댈 자리가 없어진다.
#
# 측정 로그는 어느 종류의 인용에도 쓸 수 없다. 측정은 "이 환경에서 이랬다"만 말하므로 주장을 함의하지
# 못하고, 전제의 좌표 자리에서만 쓸모가 있다.
CITATION_GRADES = {
    "fact": {"primary"},
    "choice": {"primary", "target"},
}

# 전제의 좌표는 더 넓다. 1차 출처가 조건을 말하고, 측정 로그가 이 환경의 값을 대고, 코드가 무엇이
# 있고 없는지를 보인다.
COORDINATE_GRADES = {"primary", "measured", "target"}

# `경로`, `경로:12`, `경로:12-19` 셋을 받는다. 범위를 막으면 여러 줄에 걸친 측정 표를 가리킬 수 없다.
COORDINATE_PATTERN = re.compile(r"^(?P<path>[^:]+?)(?::(?P<start>\d+)(?:-(?P<end>\d+))?)?$")


def normalize(text: str) -> str:
    """줄 끝을 LF 로 맞추고 각 줄의 오른쪽 공백을 떼어 낸다.

    공백을 떼는 이유는 소스와 문서 사본에서 줄 끝 공백이 보존되는 방식이 다르기 때문이다.
    줄 안의 들여쓰기는 건드리지 않는다. 인용의 위치를 가리는 정보이기 때문이다.
    """
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    return "\n".join(line.rstrip() for line in text.split("\n"))


def violation(rule: str, point: str, where: str, detail: str, verdict: str = "violation") -> dict:
    return {"rule": rule, "point": point, "where": where, "detail": detail, "verdict": verdict}


def _resolve(path_text: str, roots: dict[str, Path]) -> tuple[Path | None, str | None]:
    """인용 경로를 등급이 붙은 루트 아래로 해석한다.

    **가장 긴 루트를 고른다.** 루트가 겹칠 수 있다. 단계 1 폴더 전체가 측정 로그인데 그 안의 구현
    폴더는 대상 코드이고, 짧은 쪽을 먼저 집으면 코드가 측정으로 분류된다. 등급이 쓰임을 가르므로
    분류가 틀리면 멀쩡한 인용이 탈락하거나 막아야 할 인용이 통과한다.
    """
    best: tuple[Path, str] | None = None
    for root_name, root_path in roots.items():
        if path_text == root_name or path_text.startswith(root_name + "/"):
            if best is None or len(root_name) > len(best[1]):
                rel = path_text[len(root_name):].lstrip("/")
                best = ((root_path / rel if rel else root_path), root_name)
    return best if best else (None, None)


def check_citations(
    claims: list[dict],
    roots: dict[str, Path],
    grades: dict[str, str],
) -> dict:
    """주장마다 인용을 검증한다.

    roots  — 루트 이름에서 디스크 경로로. 스냅샷과 사본과 대상 코드를 함께 담는다
    grades — 루트 이름에서 등급으로. `primary`, `measured`, `target` 셋
    """
    violations: list[dict] = []

    for claim in claims:
        cid = claim.get("id", "?")
        allowed_grades = CITATION_GRADES.get(claim.get("kind"), {"primary"})
        quotes = claim.get("citations") or []

        if not quotes:
            violations.append(violation(
                "claim_uncovered", cid, "", "주장에 인용이 0개다"))
            continue

        if len(quotes) > MAX_QUOTES_PER_CLAIM:
            violations.append(violation(
                "quote_oversized", cid, "",
                f"인용 {len(quotes)}개로 상한 {MAX_QUOTES_PER_CLAIM}개를 넘는다"))

        for quote in quotes:
            path_text = (quote.get("path") or "").strip()
            text = quote.get("quote") or ""
            where = f"{path_text}"

            if text.count("\n") + 1 > MAX_QUOTE_LINES:
                violations.append(violation(
                    "quote_oversized", cid, where,
                    f"연속 {text.count(chr(10)) + 1}줄로 상한 {MAX_QUOTE_LINES}줄을 넘는다"))

            resolved, root_name = _resolve(path_text, roots)
            if resolved is None:
                violations.append(violation(
                    "source_out_of_scope", cid, where, "등록된 출처 루트 밖의 경로다"))
                continue

            grade = grades.get(root_name, "unknown")
            if grade not in allowed_grades:
                kind_name = {"fact": "사실", "choice": "선택"}.get(claim.get("kind"), "?")
                violations.append(violation(
                    "source_out_of_scope", cid, where,
                    f"등급이 {grade} 인 출처를 {kind_name} 주장의 인용으로 썼다. "
                    f"{kind_name} 주장은 {sorted(allowed_grades)} 만 인용할 수 있다"))
                continue

            if not resolved.exists() or not resolved.is_file():
                violations.append(violation(
                    "citation_absent", cid, where, "그 경로의 파일이 사본에 없다"))
                continue

            haystack = normalize(resolved.read_text(encoding="utf-8", errors="replace"))
            needle = normalize(text).strip()
            if not needle:
                violations.append(violation("citation_absent", cid, where, "인용문이 비어 있다"))
            elif needle not in haystack:
                violations.append(violation(
                    "citation_absent", cid, where, "인용문이 그 자리에 문자열로 없다"))
            else:
                anchor = check_anchor(claim, text)
                if anchor:
                    violations.append(anchor)

    return {"violations": violations}


def check_anchor(claim: dict, quote_text: str) -> dict | None:
    """`decides_at` 에 닿았는지 본다.

    출처 종류로 갈라 판정한다. 산문 출처에는 심볼이 없으므로 심볼 포함을 요구하면 멀쩡한 인용이
    탈락한다. 이 검사는 Gen 이 고른 앵커에 Gen 이 닿았는지까지만 본다. 앵커 자체가 얕은 것은
    `depth` 축이 본다.
    """
    decides = claim.get("decides_at") or {}
    kind = decides.get("kind")
    cid = claim.get("id", "?")

    if kind == "source":
        symbols = decides.get("symbols") or []
        if not symbols:
            return violation("anchor_absent", cid, "", "`kind: source` 인데 symbols 가 비어 있다")
        if any(symbol in quote_text for symbol in symbols):
            return None
        return violation("anchor_absent", cid, ", ".join(symbols),
                         "인용문에 decides_at 의 심볼이 없다")

    if kind == "doc":
        section = (decides.get("section") or "").strip()
        if not section:
            return violation("anchor_absent", cid, "", "`kind: doc` 인데 section 이 비어 있다")
        return None  # 절 일치는 경로 쪽에서 본다

    return violation("anchor_absent", cid, str(kind), "decides_at 의 kind 가 source 도 doc 도 아니다")


def check_coordinates(claims: list[dict], roots: dict[str, Path],
                      grades: dict[str, str]) -> dict:
    """선택 주장의 교환 항목에 붙은 좌표가 실재하는지 본다.

    게이트는 교환이 옳은지 판정할 수 없지만, 수치에 좌표가 붙어 있고 그 좌표가 실재하는지는 셀 수 있다.
    좌표가 그 항목을 뒷받침하는지는 `tradeoff_validity` 가 본다.

    좌표는 인용과 같은 루트 표로 해석한다. 전에는 저장소 뿌리에만 붙여서, 스냅샷의 경로를 가리킨 멀쩡한
    좌표가 "파일이 없다"로 탈락했다.
    """
    violations: list[dict] = []

    for claim in claims:
        if claim.get("kind") != "choice":
            continue
        cid = claim.get("id", "?")
        for item in claim.get("tradeoffs") or []:
            dim = item.get("dimension", "?")
            coord = (item.get("coordinate") or "").strip()
            if not coord:
                violations.append(violation(
                    "metric_coordinate_absent", cid, dim, "교환 항목에 좌표가 없다"))
                continue

            match = COORDINATE_PATTERN.match(coord)
            if not match:
                violations.append(violation(
                    "metric_coordinate_absent", cid, coord,
                    "좌표 형식이 아니다. `경로`, `경로:12`, `경로:12-19` 셋만 받는다"))
                continue

            rel = match.group("path")
            target, root_name = _resolve(rel, roots)
            if target is None or not target.exists():
                violations.append(violation(
                    "metric_coordinate_absent", cid, coord, "좌표의 파일이 없다"))
                continue

            grade = grades.get(root_name, "unknown")
            if grade not in COORDINATE_GRADES:
                violations.append(violation(
                    "source_out_of_scope", cid, coord,
                    f"등급이 {grade} 인 출처를 전제의 좌표로 썼다"))
                continue

            start, end = match.group("start"), match.group("end")
            if start:
                total = len(normalize(target.read_text(encoding="utf-8", errors="replace")).split("\n"))
                last = int(end or start)
                if int(start) < 1 or last > total or last < int(start):
                    violations.append(violation(
                        "metric_coordinate_absent", cid, coord,
                        f"줄 번호가 파일 범위를 넘거나 거꾸로다. 파일은 {total}줄이다"))

    return {"violations": violations}


def compare_limiting_clauses(gen_output: dict, critique_output: dict) -> dict:
    """Critique 이 가져온 제한 조항 중 Gen 이 안 가져온 것을 찾는다.

    두 단계의 인용 경로 집합의 차집합이라 판단이 들어가지 않는다. 기계적 검사는 정보 차단의 대상이
    아니므로 게이트에는 양쪽 산출을 다 준다.

    **`observation` 으로 낸다. 거절로 이어지지 않는다.** 처음에는 위반으로 뒀는데 run 을 돌려 보니
    통과를 막는 쳇바퀴가 됐다. Critique 은 Gen 뒤에 돌면서 제한 조항을 찾는 일만 하므로 Gen 이 안
    가져온 조항을 거의 항상 찾아내고, 주장당 인용 3개 상한 안에서 Gen 이 그것을 미리 다 인용할 수는
    없다. 실제로 재수집이 그 조항들을 받아들이려다 상한을 넘겨 두 번 탈락했다.

    조항을 안 가져온 것은 사실이지만 **그것이 결함인지는 그 조항이 주장을 실제로 깎는지에 달려 있고,
    그건 판단이다.** `grounding` 의 4점 조건이 "원문이 단 조건이 주장 문장에 들어 있는가"로 그것을
    본다. 게이트가 사실만 보기로 한 원칙에 따라 여기서는 열거만 하고 판정은 축에 맡긴다.

    열거를 없애지는 않는다. 재수집의 입력으로 들어가고 리포트에 실려, 사람이 먼저 열어 볼 자리를
    가리킨다. 실제로 이 열거가 17건에서 6건으로 줄었다. 장치는 작동했고 막는 자리만 틀렸다.
    """
    def paths(output: dict, key: str) -> dict[str, set[str]]:
        found: dict[str, set[str]] = {}
        for claim in output.get("claims", []):
            cid = claim.get("id", "?")
            bucket = found.setdefault(cid, set())
            for item in claim.get(key) or []:
                path_text = (item.get("path") or "").strip()
                if path_text:
                    bucket.add(path_text)
        return found

    gen = paths(gen_output, "citations")
    critique = paths(critique_output, "limiting_clauses")

    violations = []
    for cid, clause_paths in critique.items():
        missed = sorted(clause_paths - gen.get(cid, set()))
        for path_text in missed:
            violations.append(violation(
                "limiting_clause_missed", cid, path_text,
                "Critique 이 가져온 제한 조항을 Gen 이 가져오지 않았다",
                verdict="observation"))

    return {"violations": violations}


def check_claim_changes(prev: list[dict], curr: list[dict], reported: list[dict]) -> dict:
    """신고 없는 주장 삭제나 수정을 찾는다.

    지우는 것을 막지 않고 지운 사실을 숨기지 못하게 한다. 주장이 판정의 분모여서 좁히면 점수가 오르고,
    주장이 원문보다 넓었다는 발견 자체가 이 하네스의 산출물이다.
    """
    prev_by_id = {c.get("id"): c for c in prev}
    curr_by_id = {c.get("id"): c for c in curr}
    declared = {r.get("id") for r in reported}

    violations = []
    for cid, claim in prev_by_id.items():
        if cid not in curr_by_id:
            if cid not in declared:
                violations.append(violation(
                    "claim_change_unreported", cid, "", "신고 없이 주장을 지웠다"))
            continue
        if claim.get("sentence") != curr_by_id[cid].get("sentence") and cid not in declared:
            violations.append(violation(
                "claim_change_unreported", cid, "", "신고 없이 주장 문장을 고쳤다"))

    for report in reported:
        cid = report.get("id")
        if not (report.get("reason") or "").strip():
            violations.append(violation(
                "claim_change_unreported", cid or "?", "", "신고에 근거가 비어 있다"))

    return {"violations": violations}
