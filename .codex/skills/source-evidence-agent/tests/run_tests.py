"""게이트 리허설. 파이프라인을 붙이기 전에 양방향으로 고정한다.

정답 묶음 1건이 통과하고, 규칙마다 그 규칙만 깨뜨린 입력이 그 규칙으로만 탈락하는지 본다.
리허설 입력이 없는 규칙은 공허한지 확인된 적이 없는 규칙이다.

사용법:
    python tests/run_tests.py
"""

from __future__ import annotations

import copy
import json
import sys
from pathlib import Path

import yaml

SKILL_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SKILL_ROOT / "pipeline"))

from gates.citation import (  # noqa: E402
    check_citations,
    check_claim_changes,
    check_coordinates,
    compare_limiting_clauses,
)
from gates.sites import coverage_gap, extract_sites, load_rules  # noqa: E402
from stages.scripts.payload import check_blocking, claims_for_critique  # noqa: E402
from validate import (  # noqa: E402
    check_dimension_sync,
    check_kind_consistency,
    validate_output,
)

for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace")

REPO_ROOT = SKILL_ROOT.parents[2]
SOURCES = SKILL_ROOT / "sources"

ROOTS = {
    "spring-framework-6.2.19": SOURCES / "spring-framework-6.2.19",
    "jdk-25.0.4.1": SOURCES / "jdk-25.0.4.1",
    "hibernate-orm-6.6.53.Final": SOURCES / "hibernate-orm-6.6.53.Final",
    "evidence": REPO_ROOT / "phase2/taskF/alert-kata/evidence",
}
GRADES = {
    "spring-framework-6.2.19": "primary",
    "jdk-25.0.4.1": "primary",
    "hibernate-orm-6.6.53.Final": "primary",
    "evidence": "measured",
    "phase2/taskF/alert-kata/evidence": "measured",
    "phase2/taskF/task4/step1": "measured",
    "phase2/taskF/task4/step1/run3/impl": "target",
}
ROOTS.update({
    "phase2/taskF/alert-kata/evidence": REPO_ROOT / "phase2/taskF/alert-kata/evidence",
    "phase2/taskF/task4/step1": REPO_ROOT / "phase2/taskF/task4/step1",
    "phase2/taskF/task4/step1/run3/impl": REPO_ROOT / "phase2/taskF/task4/step1/run3/impl",
})
MEASURED_COORD_RANGE = "phase2/taskF/alert-kata/evidence/lock-comparison-results.md:14-18"
CODE_PATH = "phase2/taskF/task4/step1/run3/impl/AlertRepository.java"

SPRING_TX_PATH = "spring-framework-6.2.19/org/springframework/jdbc/datasource/DataSourceUtils.java"
JDK_LOCK_PATH = "jdk-25.0.4.1/java.base/java/util/concurrent/locks/ReentrantLock.java"
MEASURED_COORD = "phase2/taskF/alert-kata/evidence/lock-comparison-results.md:14"


def golden_claims() -> list[dict]:
    """사람이 손으로 쓴 정답 묶음. 사실 주장 하나와 선택 주장 하나."""
    return [
        {
            "id": "claim-01",
            "kind": "fact",
            "sentence": "Spring은 트랜잭션을 열 때 커넥션의 격리 수준을 지정된 값으로 바꾼다.",
            "sites": ["site-19"],
            "citations": [
                {
                    "path": SPRING_TX_PATH,
                    "quote": "con.setTransactionIsolation(isolationLevel);",
                }
            ],
            "decides_at": {"kind": "source", "symbols": ["setTransactionIsolation"]},
        },
        {
            "id": "claim-02",
            "kind": "choice",
            "sentence": "같은 사용자 행을 PESSIMISTIC_WRITE로 먼저 잠그는 방식을 택했다.",
            "sites": ["site-15"],
            "citations": [
                {
                    "path": JDK_LOCK_PATH,
                    "quote": "method. If you want a timed {@code tryLock} that does permit barging on",
                }
            ],
            "decides_at": {"kind": "source", "symbols": ["tryLock"]},
            "tradeoffs": [
                {
                    "dimension": "latency",
                    "statement": "잠금 대기가 생기지만 재시도가 없다.",
                    "coordinate": MEASURED_COORD,
                },
                {
                    "dimension": "complexity",
                    "statement": "이 차원에서는 교환이 없다. 잠금 한 줄로 끝나 재시도 코드가 없기 때문이다.",
                    "coordinate": MEASURED_COORD,
                },
            ],
        },
    ]


def rule_ids(result: dict) -> set[str]:
    return {v["rule"] for v in result["violations"]}


# 규칙마다 그 규칙만 깨뜨리는 입력을 만든다. 반환값은 (규칙 id, 변형 함수) 쌍이다.

def _break_claim_uncovered(claims: list[dict]) -> list[dict]:
    claims[0]["citations"] = []
    return claims


def _break_citation_absent_path(claims: list[dict]) -> list[dict]:
    claims[0]["citations"][0]["path"] = "spring-framework-6.2.19/org/springframework/없는파일.java"
    return claims


def _break_citation_absent_text(claims: list[dict]) -> list[dict]:
    claims[0]["citations"][0]["quote"] = "con.setTransactionIsolation(존재하지않는인자);"
    return claims


def _break_source_out_of_scope_root(claims: list[dict]) -> list[dict]:
    claims[0]["citations"][0]["path"] = "https://example.com/blog/post"
    return claims


def _break_source_out_of_scope_grade(claims: list[dict]) -> list[dict]:
    claims[0]["citations"][0]["path"] = "evidence/lock-comparison-results.md"
    claims[0]["citations"][0]["quote"] = "#"
    return claims


def _break_anchor_absent(claims: list[dict]) -> list[dict]:
    claims[0]["decides_at"]["symbols"] = ["존재하지않는심볼"]
    return claims


def _break_quote_oversized_count(claims: list[dict]) -> list[dict]:
    quote = claims[0]["citations"][0]
    claims[0]["citations"] = [dict(quote) for _ in range(4)]
    return claims


def _break_quote_oversized_lines(claims: list[dict]) -> list[dict]:
    """사본에 실제로 있는 41줄을 인용한다.

    없는 줄을 이어 붙이면 `citation_absent` 가 함께 걸려서 이 규칙만 깨뜨린 입력이 되지 않는다.
    리허설 입력은 겨냥한 규칙 하나만 깨뜨려야 한다.
    """
    source = ROOTS["spring-framework-6.2.19"] / "org/springframework/jdbc/datasource/DataSourceUtils.java"
    lines = [line.rstrip() for line in source.read_text(encoding="utf-8").replace("\r\n", "\n").split("\n")]
    anchor = next(i for i, line in enumerate(lines) if "con.setTransactionIsolation" in line)
    claims[0]["citations"][0]["quote"] = "\n".join(lines[anchor:anchor + 41])
    return claims


def _break_citation_grade_for_fact(claims: list[dict]) -> list[dict]:
    """사실 주장이 대상 코드를 인용했다. 코드는 선택 주장의 증거이고 사실의 근거가 아니다."""
    claims[0]["citations"][0]["path"] = CODE_PATH
    claims[0]["citations"][0]["quote"] = "public class AlertRepository {"
    claims[0]["decides_at"]["symbols"] = ["AlertRepository"]
    return claims


CITATION_CASES = [
    ("claim_uncovered", _break_claim_uncovered),
    ("citation_absent", _break_citation_absent_path),
    ("citation_absent", _break_citation_absent_text),
    ("source_out_of_scope", _break_source_out_of_scope_root),
    ("source_out_of_scope", _break_source_out_of_scope_grade),
    ("anchor_absent", _break_anchor_absent),
    ("quote_oversized", _break_quote_oversized_count),
    ("quote_oversized", _break_quote_oversized_lines),
    ("source_out_of_scope", _break_citation_grade_for_fact),
]


def _break_coordinate_missing(claims: list[dict]) -> list[dict]:
    claims[1]["tradeoffs"][0]["coordinate"] = ""
    return claims


def _break_coordinate_absent_file(claims: list[dict]) -> list[dict]:
    claims[1]["tradeoffs"][0]["coordinate"] = "phase2/taskF/alert-kata/evidence/없는로그.md:3"
    return claims


def _break_coordinate_out_of_range(claims: list[dict]) -> list[dict]:
    claims[1]["tradeoffs"][0]["coordinate"] = "phase2/taskF/alert-kata/evidence/lock-comparison-results.md:99999"
    return claims


def _break_coordinate_reversed_range(claims: list[dict]) -> list[dict]:
    """거꾸로 된 범위. 범위를 받기로 한 뒤 생긴 자리라 리허설을 함께 둔다."""
    claims[1]["tradeoffs"][0]["coordinate"] = "phase2/taskF/alert-kata/evidence/lock-comparison-results.md:18-14"
    return claims


COORDINATE_CASES = [
    ("metric_coordinate_absent", _break_coordinate_missing),
    ("metric_coordinate_absent", _break_coordinate_absent_file),
    ("metric_coordinate_absent", _break_coordinate_out_of_range),
    ("metric_coordinate_absent", _break_coordinate_reversed_range),
]


def run() -> int:
    failures: list[str] = []

    def check(name: str, ok: bool, detail: str = "") -> None:
        print(f"{'OK  ' if ok else 'FAIL'}  {name}{(' — ' + detail) if detail and not ok else ''}")
        if not ok:
            failures.append(name)

    # 양성 방향: 정답 묶음은 통과해야 한다.
    golden = golden_claims()
    citation_result = check_citations(golden, ROOTS, GRADES)
    check("정답 묶음 / 인용 게이트", not citation_result["violations"],
          str(rule_ids(citation_result)))

    coord_result = check_coordinates(golden, ROOTS, GRADES)
    check("정답 묶음 / 좌표 게이트", not coord_result["violations"],
          str(rule_ids(coord_result)))

    # 음성 방향: 규칙마다 그 규칙으로만 탈락해야 한다.
    for rule, mutate in CITATION_CASES:
        claims = mutate(copy.deepcopy(golden_claims()))
        result = check_citations(claims, ROOTS, GRADES)
        ids = rule_ids(result)
        check(f"{rule} / {mutate.__name__}", ids == {rule}, f"나온 규칙 {ids or '없음'}")

    for rule, mutate in COORDINATE_CASES:
        claims = mutate(copy.deepcopy(golden_claims()))
        result = check_coordinates(claims, ROOTS, GRADES)
        ids = rule_ids(result)
        check(f"{rule} / {mutate.__name__}", ids == {rule}, f"나온 규칙 {ids or '없음'}")

    # 커버리지 게이트. 지점 목록을 실제 대상에서 뽑아 쓴다.
    rules = load_rules(SKILL_ROOT / "rules" / "concurrency_sites.yaml")
    code_roots = [
        REPO_ROOT / "phase2/taskF/alert-kata/src/main/java",
        REPO_ROOT / "phase2/taskF/alert-kata/src/test/resources",
        REPO_ROOT / "phase2/taskF/task4/step1/run3/impl",
    ]
    sites = extract_sites(code_roots, rules, REPO_ROOT)
    check("지점 추출 / 비어 있지 않다", len(sites) > 0, f"{len(sites)}개")

    all_covered = [{"id": "claim-all", "sites": [s["id"] for s in sites]}]
    result = coverage_gap(all_covered, sites)
    check("정답 묶음 / 커버리지 게이트", not result["violations"], str(rule_ids(result)))

    one_short = [{"id": "claim-all", "sites": [s["id"] for s in sites[1:]]}]
    result = coverage_gap(one_short, sites)
    check("coverage_gap / 지점 하나를 비운 입력",
          rule_ids(result) == {"coverage_gap"} and len(result["violations"]) == 1,
          str(rule_ids(result)))

    unknown = [{"id": "claim-x", "sites": ["site-99"] + [s["id"] for s in sites]}]
    result = coverage_gap(unknown, sites)
    check("claim_site_unknown / 목록 밖 좌표",
          rule_ids(result) == {"claim_site_unknown"}, str(rule_ids(result)))

    # 제한 조항 대조.
    gen = {"claims": [{"id": "claim-01", "citations": [{"path": SPRING_TX_PATH}]}]}
    critique_same = {"claims": [{"id": "claim-01",
                                 "limiting_clauses": [{"path": SPRING_TX_PATH}]}]}
    result = compare_limiting_clauses(gen, critique_same)
    check("정답 묶음 / 제한 조항 대조", not result["violations"], str(rule_ids(result)))

    critique_extra = {"claims": [{"id": "claim-01",
                                  "limiting_clauses": [{"path": JDK_LOCK_PATH}]}]}
    result = compare_limiting_clauses(gen, critique_extra)
    check("limiting_clause_missed / Critique 에만 있는 조항",
          rule_ids(result) == {"limiting_clause_missed"}, str(rule_ids(result)))
    check("놓친 조항은 관찰이고 거절이 아니다",
          all(v["verdict"] == "observation" for v in result["violations"]),
          str([v["verdict"] for v in result["violations"]]))

    # 주장 변경 신고.
    prev = [{"id": "claim-01", "sentence": "A"}, {"id": "claim-02", "sentence": "B"}]
    result = check_claim_changes(prev, prev, [])
    check("정답 묶음 / 변경 신고", not result["violations"], str(rule_ids(result)))

    result = check_claim_changes(prev, prev[:1], [])
    check("claim_change_unreported / 신고 없이 삭제",
          rule_ids(result) == {"claim_change_unreported"}, str(rule_ids(result)))

    result = check_claim_changes(prev, prev[:1],
                                [{"id": "claim-02", "reason": "원문보다 넓어 철회한다"}])
    check("신고한 삭제는 통과", not result["violations"], str(rule_ids(result)))

    result = check_claim_changes(prev, prev[:1], [{"id": "claim-02", "reason": "  "}])
    check("claim_change_unreported / 근거 없는 신고",
          rule_ids(result) == {"claim_change_unreported"}, str(rule_ids(result)))

    # 고친 동작의 양성 방향. 결함을 고친 뒤에는 멀쩡한 입력이 통과하는 것까지 확인해야 한다.
    ranged = copy.deepcopy(golden_claims())
    ranged[1]["tradeoffs"][0]["coordinate"] = MEASURED_COORD_RANGE
    result = check_coordinates(ranged, ROOTS, GRADES)
    check("범위 좌표가 통과한다", not result["violations"], str(rule_ids(result)))

    snap = copy.deepcopy(golden_claims())
    snap[1]["tradeoffs"][0]["coordinate"] = SPRING_TX_PATH + ":218-222"
    result = check_coordinates(snap, ROOTS, GRADES)
    check("스냅샷 경로 좌표가 통과한다", not result["violations"], str(rule_ids(result)))

    coded = copy.deepcopy(golden_claims())
    coded[1]["citations"] = [{"path": CODE_PATH, "quote": "public class AlertRepository {"}]
    coded[1]["decides_at"] = {"kind": "source", "symbols": ["AlertRepository"]}
    result = check_citations(coded, ROOTS, GRADES)
    check("선택 주장이 대상 코드를 인용할 수 있다", not result["violations"], str(rule_ids(result)))

    from gates.citation import _resolve  # noqa: E402
    _, picked = _resolve(CODE_PATH, ROOTS)
    check("겹치는 루트에서 긴 쪽이 이긴다",
          picked == "phase2/taskF/task4/step1/run3/impl", str(picked))

    # 판정 자산. 스키마와 루브릭도 판정 장치이므로 리허설한다.
    rubric_yaml = yaml.safe_load((SKILL_ROOT / "pipeline/rubrics/claim.rubric.yaml").read_text(encoding="utf-8"))
    check("루브릭과 스키마의 차원 목록이 같다",
          not check_dimension_sync(rubric_yaml)["violations"],
          str(check_dimension_sync(rubric_yaml)["violations"]))

    axes = {a["id"] for a in rubric_yaml["axes"]}
    check("축 넷이 모두 하한을 갖는다",
          axes == {"grounding", "depth", "tradeoff_coverage", "tradeoff_validity"}
          and all("floor" in a for a in rubric_yaml["axes"]), str(axes))
    check("루브릭이 합산하지 않는다", rubric_yaml.get("aggregate") == "none",
          str(rubric_yaml.get("aggregate")))

    good_gen = {"claims": [{
        "id": "claim-01", "kind": "fact", "sentence": "원문이 그렇다고 말하는 한 문장이다.",
        "sites": ["site-01"], "decides_at": {"kind": "source", "symbols": ["x"]},
        "citations": [{"path": SPRING_TX_PATH, "quote": "con.setTransactionIsolation"}],
        "source_says": "원문이 말하는 것을 범위로 적는다.", "claim_delta": "차이 없음",
    }]}
    result = validate_output(good_gen, "gen_output.schema.json")
    check("정답 묶음 / gen 스키마", not result["violations"], str(rule_ids(result)))

    breach = copy.deepcopy(good_gen)
    breach["claims"][0]["supported"] = True
    result = validate_output(breach, "gen_output.schema.json")
    check("role_breach / 선언하지 않은 자기 평가 필드",
          rule_ids(result) == {"role_breach"}, str(rule_ids(result)))

    missing = copy.deepcopy(good_gen)
    del missing["claims"][0]["claim_delta"]
    result = validate_output(missing, "gen_output.schema.json")
    check("schema_violation / 필수 칸 누락",
          rule_ids(result) == {"schema_violation"}, str(rule_ids(result)))

    result = check_kind_consistency([{"id": "claim-01", "kind": "fact",
                                      "tradeoffs": [{"dimension": "latency"}]}])
    check("role_breach / fact 주장에 붙은 트레이드오프",
          rule_ids(result) == {"role_breach"}, str(rule_ids(result)))

    result = check_kind_consistency([{"id": "claim-02", "kind": "choice"}])
    check("schema_violation / choice 주장에 없는 트레이드오프",
          rule_ids(result) == {"schema_violation"}, str(rule_ids(result)))

    # payload 차단 표. 단계마다 허용 표 밖의 키가 들어오면 잡혀야 한다.
    check("payload 차단 / gen 에 점수를 넘기려 하면 잡힌다",
          check_blocking("gen", ["SOURCES_JSON", "RUBRIC_JSON"]) == ["RUBRIC_JSON"],
          str(check_blocking("gen", ["SOURCES_JSON", "RUBRIC_JSON"])))
    check("payload 차단 / eval 에 비평을 넘기려 하면 잡힌다",
          check_blocking("eval", ["GEN_JSON", "COUNTER_JSON"]) == ["COUNTER_JSON"],
          str(check_blocking("eval", ["GEN_JSON", "COUNTER_JSON"])))
    check("payload 차단 / critique 가 Gen 의 판독을 받지 않는다",
          "claim_delta" not in json.dumps(
              claims_for_critique({"claims": [{"id": "claim-01", "claim_delta": "x",
                                               "source_says": "y"}]}), ensure_ascii=False),
          "claim_delta 가 넘어갔다")

    print()
    if failures:
        print(f"{len(failures)}건 실패: {', '.join(failures)}")
        return 1
    print("전부 통과")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
