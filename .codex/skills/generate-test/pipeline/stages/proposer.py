"""proposer.py

Slow loop 2단계: analysis.json을 받아 gen/critique/eval/refine 파이프라인으로
테스트 생성 파이프라인 수정 제안을 만들고 changelog/proposals/<analysis_id>/proposal-final.md를 출력한다.

정보 차단 (설계 docs/v1-slow-loop-design.md §8):
  - 층위 1: propose critique는 propose eval에 anchor되지 않는다.
  - 층위 2: gen은 후보 target 전체를 읽고, critique/eval/refine은 제안이 건드린 파일만 읽는다.

표본 게이트 (§5-B): sufficient_sample=false 그룹의 신호는 gen에 넣지 않는다.

고정점 (§5): slow loop 자신의 rubric(rubric_proposal.yaml)·propose_* 프롬프트·slow-loop 코드
(proposer/analyze/run_propose)는 target 후보에서 제외한다 — 사람만 바꾼다.

validator 검사 (코드, PASS/REJECT, LLM 없이):
  - 신호 실재성: cited_signals의 id가 analysis.json signals[]에 있는가
  - diff 적용성: diff.anchor가 대상 파일에 존재하는가
  - 위험 라벨 규칙: pipeline_code/agents_md/rubric → risk 반드시 "높음" (§4)
  - target_file 비어있지 않음
"""
from __future__ import annotations

import copy
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

from stages.scripts.llm_client import LLMClient

PROJECT_DIR = Path(__file__).resolve().parent.parent   # pipeline/
SKILL_DIR = PROJECT_DIR.parent                          # generate-test/
PROMPTS_DIR = PROJECT_DIR / "prompts"
SCHEMAS_DIR = PROJECT_DIR / "schemas"
RUBRICS_DIR = PROJECT_DIR / "rubrics"
STAGES_DIR = PROJECT_DIR / "stages"
PROPOSALS_DIR = PROJECT_DIR / "changelog" / "proposals"

PROPOSE_GEN_SYSTEM = PROMPTS_DIR / "propose_gen_system.md"
PROPOSE_CRITIQUE_SYSTEM = PROMPTS_DIR / "propose_critique_system.md"
PROPOSE_EVAL_SYSTEM = PROMPTS_DIR / "propose_eval_system.md"
PROPOSE_REFINE_SYSTEM = PROMPTS_DIR / "propose_refine_system.md"

PROPOSE_GEN_OUTPUT_SCHEMA = SCHEMAS_DIR / "propose_gen_output.schema.json"
PROPOSE_CRITIQUE_OUTPUT_SCHEMA = SCHEMAS_DIR / "propose_critique_output.schema.json"
PROPOSE_EVAL_OUTPUT_SCHEMA = SCHEMAS_DIR / "propose_eval_output.schema.json"

RUBRIC_PROPOSAL = PROJECT_DIR / "rubric_proposal.yaml"

# 고정점: slow loop 자신의 판정 장치·코드는 target 후보에서 제외 (§5)
FIXED_POINT_FILES = {"rubric_proposal.yaml"}
FIXED_POINT_STEMS = {"proposer", "analyze_runs", "run_propose"}
RISK_HIGH_KINDS = {"pipeline_code", "agents_md", "rubric"}  # §4

KST = timezone(timedelta(hours=9))
MAX_ITERATIONS = 2


def now_iso() -> str:
    return datetime.now(KST).isoformat(timespec="seconds")


def load_json(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"expected JSON object: {path}")
    return data


def load_rubric_proposal() -> dict:
    try:
        import yaml  # type: ignore[import-not-found]
        data = yaml.safe_load(RUBRIC_PROPOSAL.read_text(encoding="utf-8"))
    except ModuleNotFoundError:
        data = json.loads(RUBRIC_PROPOSAL.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"expected rubric object: {RUBRIC_PROPOSAL}")
    return data


# ---------------------------------------------------------------------------
# target 파일 로딩 (경로는 SKILL_DIR 기준 단일 루트)
# ---------------------------------------------------------------------------

def _rel(path: Path) -> str:
    return str(path.relative_to(SKILL_DIR))


def load_target_files() -> dict[str, str]:
    """gen 단계용: 후보 target 파일을 전부 읽어 (SKILL_DIR 상대경로)→내용 맵으로 반환한다.

    고정점(propose_* 프롬프트, rubric_proposal, slow-loop 코드)은 제외한다.
    """
    targets: dict[str, str] = {}

    for path in sorted(RUBRICS_DIR.glob("*.rubric.yaml")):
        targets[_rel(path)] = path.read_text(encoding="utf-8")

    for path in sorted(PROMPTS_DIR.glob("*.md")):
        if path.name.startswith("propose_"):
            continue  # 고정점: slow loop 자신의 프롬프트
        targets[_rel(path)] = path.read_text(encoding="utf-8")

    for gov in (SKILL_DIR / "SKILL.md", SKILL_DIR / "CLAUDE.md", PROJECT_DIR / "AGENTS.md"):
        if gov.exists():
            targets[_rel(gov)] = gov.read_text(encoding="utf-8")

    for path in sorted(STAGES_DIR.glob("*.py")):
        if path.stem in FIXED_POINT_STEMS:
            continue
        targets[_rel(path)] = path.read_text(encoding="utf-8")

    for path in sorted(PROJECT_DIR.glob("*.py")):
        if path.stem in FIXED_POINT_STEMS or path.name in FIXED_POINT_FILES:
            continue
        targets[_rel(path)] = path.read_text(encoding="utf-8")

    return targets


def load_touched_files(proposals: list[dict]) -> dict[str, str]:
    """critique/eval/refine 단계용: 제안이 건드린 파일만 읽는다 (SKILL_DIR 기준)."""
    touched: dict[str, str] = {}
    for p in proposals:
        target_file = p.get("target_file", "")
        if not target_file:
            continue
        candidate = SKILL_DIR / target_file
        if candidate.exists():
            touched[target_file] = candidate.read_text(encoding="utf-8")
    return touched


# ---------------------------------------------------------------------------
# 표본 게이트: sufficient_sample 그룹 신호만 gen에 넣는다 (§5-B)
# ---------------------------------------------------------------------------

def filter_analysis_for_gen(analysis: dict) -> dict:
    groups = analysis.get("groups", {})
    allowed = {name for name, g in groups.items() if g.get("sufficient_sample")}
    filtered = copy.deepcopy(analysis)
    filtered["signals"] = [
        s for s in analysis.get("signals", [])
        if s.get("group") is None or s.get("group") in allowed
    ]
    filtered["_allowed_groups"] = sorted(allowed)
    return filtered


# ---------------------------------------------------------------------------
# validator (코드, PASS/REJECT, LLM 없이)
# ---------------------------------------------------------------------------

def validate_proposal(proposal: dict, analysis: dict, touched_files: dict[str, str]) -> list[str]:
    errors = []
    valid_signal_ids = {s["id"] for s in analysis.get("signals", []) if isinstance(s, dict) and "id" in s}

    for sid in proposal.get("cited_signals", []):
        if sid not in valid_signal_ids:
            errors.append(f"cited_signals: '{sid}' not found in analysis.json signals")

    diff = proposal.get("diff", {})
    anchor = diff.get("anchor", "") if isinstance(diff, dict) else ""
    target_file = proposal.get("target_file", "")
    if anchor and target_file:
        content = touched_files.get(target_file, "")
        if content and anchor not in content:
            errors.append(f"diff.anchor not found in {target_file}")

    target_kind = proposal.get("target_kind", "")
    risk = proposal.get("risk", "")
    if target_kind in RISK_HIGH_KINDS and risk != "높음":
        errors.append(f"risk must be '높음' for target_kind='{target_kind}', got '{risk}'")

    if not target_file:
        errors.append("target_file is empty")

    return errors


# ---------------------------------------------------------------------------
# stage 함수들
# ---------------------------------------------------------------------------

def propose_gen(analysis, target_files, output_path, client, model):
    system = PROPOSE_GEN_SYSTEM.read_text(encoding="utf-8")
    targets_text = "\n\n".join(f"FILE: {name}\n{content}" for name, content in target_files.items())
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"TARGET_FILES:\n{targets_text}\n"
    )
    return client.run_prompt(system=system, user=user, output_schema=PROPOSE_GEN_OUTPUT_SCHEMA, output_path=output_path, model=model)


def propose_critique(analysis, proposal_data, touched_files, output_path, client, model):
    system = PROPOSE_CRITIQUE_SYSTEM.read_text(encoding="utf-8")
    touched_text = "\n\n".join(f"FILE: {name}\n{content}" for name, content in touched_files.items())
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"PROPOSAL_JSON:\n{json.dumps(proposal_data, ensure_ascii=False, indent=2)}\n\n"
        f"TOUCHED_FILES:\n{touched_text}\n"
    )
    return client.run_prompt(system=system, user=user, output_schema=PROPOSE_CRITIQUE_OUTPUT_SCHEMA, output_path=output_path, model=model)


def propose_eval(analysis, proposal_data, touched_files, rubric, output_path, client, model):
    system = PROPOSE_EVAL_SYSTEM.read_text(encoding="utf-8")
    touched_text = "\n\n".join(f"FILE: {name}\n{content}" for name, content in touched_files.items())
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"PROPOSAL_JSON:\n{json.dumps(proposal_data, ensure_ascii=False, indent=2)}\n\n"
        f"TOUCHED_FILES:\n{touched_text}\n\n"
        f"RUBRIC_JSON:\n{json.dumps(rubric, ensure_ascii=False, indent=2)}\n"
    )
    return client.run_prompt(system=system, user=user, output_schema=PROPOSE_EVAL_OUTPUT_SCHEMA, output_path=output_path, model=model)


def propose_refine(analysis, proposal_data, critique_data, touched_files, weak_axes, output_path, client, model):
    system = PROPOSE_REFINE_SYSTEM.read_text(encoding="utf-8")
    touched_text = "\n\n".join(f"FILE: {name}\n{content}" for name, content in touched_files.items())
    refine_signal = {"weak_axes": weak_axes}  # eval 총점 원문은 넘기지 않는다 (§8)
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"PROPOSAL_JSON:\n{json.dumps(proposal_data, ensure_ascii=False, indent=2)}\n\n"
        f"CRITIQUE_JSON:\n{json.dumps(critique_data, ensure_ascii=False, indent=2)}\n\n"
        f"TOUCHED_FILES:\n{touched_text}\n\n"
        f"REFINE_SIGNAL:\n{json.dumps(refine_signal, ensure_ascii=False, indent=2)}\n"
    )
    return client.run_prompt(system=system, user=user, output_schema=PROPOSE_GEN_OUTPUT_SCHEMA, output_path=output_path, model=model)


# ---------------------------------------------------------------------------
# 게이트
# ---------------------------------------------------------------------------

def get_proposal_weak_axes(eval_data: dict, rubric: dict) -> list[str]:
    min_axis = rubric.get("thresholds", {}).get("min_axis", {})
    weak = []
    for ev in eval_data.get("evaluations", []):
        scores = ev.get("rubric_scores", {}).get("scores", {})
        pid = ev.get("proposal_id", "?")
        for axis, minimum in min_axis.items():
            score = scores.get(axis)
            if isinstance(score, (int, float)) and isinstance(minimum, (int, float)) and score < minimum:
                weak.append(f"{pid}.{axis}")
    return weak


def passes_gate(eval_data: dict, rubric: dict) -> bool:
    thresholds = rubric.get("thresholds", {})
    min_total = thresholds.get("min_total", 0)
    min_axis = thresholds.get("min_axis", {})
    evals = eval_data.get("evaluations", [])
    if not evals:
        return False
    for ev in evals:
        scores_obj = ev.get("rubric_scores", {})
        total = scores_obj.get("weighted_total", 0)
        if isinstance(total, (int, float)) and total < min_total:
            return False
        scores = scores_obj.get("scores", {})
        for axis, minimum in min_axis.items():
            score = scores.get(axis)
            if isinstance(score, (int, float)) and isinstance(minimum, (int, float)) and score < minimum:
                return False
    return True


# ---------------------------------------------------------------------------
# proposal-final 문서 생성
# ---------------------------------------------------------------------------

def _all_runs(analysis: dict) -> list[str]:
    runs = []
    for g in analysis.get("groups", {}).values():
        runs += g.get("runs_included", []) + g.get("failed_runs_included", [])
    return runs


def build_changelog_draft(proposals, validator_errors_by_id, analysis, today) -> str:
    passing = [p for p in proposals if p["id"] not in validator_errors_by_id]
    if not passing:
        return ""
    run_refs = ", ".join(_all_runs(analysis))
    signal_map = {s["id"]: s["summary"] for s in analysis.get("signals", []) if isinstance(s, dict)}

    lines = ["## CHANGELOG 초안", "",
             "*(적용 시 CHANGELOG.md에 복사·수정하세요. `vN`은 현재 버전+1, commit 해시는 적용 후 기입합니다.)*", ""]
    for p in passing:
        target_file = p.get("target_file", "")
        component = Path(target_file).stem if target_file else "unknown"
        risk = p.get("risk", "?")
        target_axis = p.get("target_axis", "")
        diff = p.get("diff", {})
        change_text = diff.get("change", "") if isinstance(diff, dict) else ""
        change_summary = change_text.splitlines()[0] if change_text else "(diff 내용 확인)"
        cited = p.get("cited_signals", [])
        근거 = "; ".join(signal_map[sid] for sid in cited if sid in signal_map) or "(신호 없음)"
        lines += [
            f"### {component}:vN ({today})",
            f"- 변경: {change_summary}",
            f"- 겨냥 axis: {target_axis}",
            f"- 근거: {근거}",
            f"- 분석 run: {run_refs}",
            f"- 위험: {risk}",
            f"- commit: (적용 후 기입)",
            "",
        ]
    return "\n".join(lines)


def build_proposal_md(analysis, proposal_data, eval_data, validator_errors_by_id, analysis_id) -> str:
    lines = [
        f"# Slow Loop Proposal — {datetime.now(KST).date().isoformat()} (미검토 {analysis.get('total_unreviewed','?')}개 run 분석)",
        "",
        "## 분석 요약 (그룹별)",
    ]
    for name, g in analysis.get("groups", {}).items():
        suff = "표본충분" if g.get("sufficient_sample") else "표본부족(제안제외)"
        lines.append(f"- **{name}** [{suff}] pass={g.get('passing_count')} fail={g.get('failed_count')}")
        for axis, st in (g.get("axis_stats") or {}).items():
            if st.get("below_min_count", 0) > 0:
                lines.append(f"    - {axis}: 평균 {st['mean']} (미달 {st['below_min_count']})")
    fb = analysis.get("user_feedback", {})
    lines.append(f"- 사용자 피드백(problem.md): {fb.get('count', 0)}건")
    lines.append(f"- 신호 총 {len(analysis.get('signals', []))}개")
    lines += ["", "## 제안", ""]

    eval_map = {ev["proposal_id"]: ev for ev in eval_data.get("evaluations", [])}
    for proposal in proposal_data.get("proposals", []):
        pid = proposal["id"]
        risk = proposal.get("risk", "?")
        target_file = proposal.get("target_file", "?")
        lines.append(f"### {pid} — [위험: {risk}] {target_file}")
        lines.append(f"**진단:** {proposal.get('diagnosis', '')}")
        lines.append(f"**겨냥 axis:** {proposal.get('target_axis', '')}")
        lines.append(f"**근거 신호:** {', '.join(proposal.get('cited_signals', []))}")
        lines.append(f"**효과 경로:** {proposal.get('effect_path', '')}")
        alts = proposal.get("alternatives_considered", [])
        if alts:
            lines.append("**배제한 원인:**")
            for alt in alts:
                lines.append(f"  - {alt.get('cause', '')}: {alt.get('why_not', '')}")
        side = proposal.get("side_effects", "")
        if side:
            lines.append(f"**부작용:** {side}")
        diff = proposal.get("diff", {})
        if isinstance(diff, dict):
            lines.append("**diff:**")
            lines.append("```")
            lines.append(f"anchor: {diff.get('anchor', '')[:120]}")
            lines.append(f"change: {diff.get('change', '')}")
            lines.append("```")
        v_errors = validator_errors_by_id.get(pid, [])
        lines.append(f"**validator:** {'REJECT — ' + '; '.join(v_errors) if v_errors else 'PASS'}")
        ev = eval_map.get(pid)
        if ev:
            scores = ev.get("rubric_scores", {}).get("scores", {})
            total = ev.get("rubric_scores", {}).get("weighted_total", "?")
            lines.append(f"**eval 총점:** {total}")
            lines.append(f"**축별 점수:** {scores}")
        lines.append("")

    priority = proposal_data.get("priority_order")
    if priority:
        lines.append("## 적용 순서")
        for p in priority:
            lines.append(f"{p['rank']}. {p['proposal_id']}: {p['rationale']}")
        lines.append("")

    today = datetime.now(KST).date().isoformat()
    changelog_section = build_changelog_draft(proposal_data.get("proposals", []), validator_errors_by_id, analysis, today)
    if changelog_section:
        lines.append(changelog_section)
    lines += ["---", f"analysis_id: {analysis_id}", f"generated_at: {now_iso()}"]
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# 오케스트레이터
# ---------------------------------------------------------------------------

def run_proposal_pipeline(analysis_path: Path, client: LLMClient, model=None, progress_fn=None) -> dict:
    def log(msg: str) -> None:
        (progress_fn or (lambda m: print(f"[proposer] {m}", file=sys.stderr)))(msg)

    raw_analysis = load_json(analysis_path)
    analysis_id = raw_analysis.get("analysis_id", "unknown")
    rubric = load_rubric_proposal()
    target_files = load_target_files()

    # 표본 게이트: sufficient 그룹 신호만 gen에 넘긴다
    analysis = filter_analysis_for_gen(raw_analysis)
    allowed = analysis.get("_allowed_groups", [])
    log(f"analysis_id={analysis_id} allowed_groups={allowed} signals={len(analysis.get('signals', []))}")
    if not analysis.get("signals"):
        return {"status": "SKIP", "reason": "표본 충분한 그룹의 신호가 없음(모든 그룹 sufficient_sample=false)", "analysis_id": analysis_id}

    run_dir = PROPOSALS_DIR / analysis_id
    run_dir.mkdir(parents=True, exist_ok=True)

    iteration = "001"
    proposal_data: dict = {}
    eval_data: dict = {}
    validator_errors_by_id: dict[str, list[str]] = {}

    for iter_num in range(1, MAX_ITERATIONS + 1):
        iteration = f"{iter_num:03d}"
        iter_dir = run_dir / f"iter_{iteration}"
        iter_dir.mkdir(parents=True, exist_ok=True)

        log(f"iter {iteration} gen")
        propose_gen(analysis, target_files, iter_dir / "gen.json", client, model)
        proposal_data = load_json(iter_dir / "gen.json")

        touched_files = load_touched_files(proposal_data.get("proposals", []))
        validator_errors_by_id = {}
        for p in proposal_data.get("proposals", []):
            errs = validate_proposal(p, analysis, touched_files)
            if errs:
                validator_errors_by_id[p["id"]] = errs
                log(f"iter {iteration} validator REJECT {p['id']}: {errs}")

        passing_proposals = [p for p in proposal_data.get("proposals", []) if p["id"] not in validator_errors_by_id]
        if not passing_proposals:
            if iter_num >= MAX_ITERATIONS:
                return {"status": "FAILED", "reason": "all proposals failed validator", "analysis_id": analysis_id,
                        "run_dir": str(run_dir), "validator_errors": validator_errors_by_id}
            log(f"iter {iteration} all proposals failed validator, retry")
            continue

        passing_data = {**proposal_data, "proposals": passing_proposals}

        log(f"iter {iteration} critique")
        propose_critique(analysis, passing_data, touched_files, iter_dir / "critique.json", client, model)
        critique_data = load_json(iter_dir / "critique.json")

        log(f"iter {iteration} eval")
        propose_eval(analysis, passing_data, touched_files, rubric, iter_dir / "eval.json", client, model)
        eval_data = load_json(iter_dir / "eval.json")

        if passes_gate(eval_data, rubric):
            log(f"iter {iteration} gate PASS")
            break
        if iter_num >= MAX_ITERATIONS:
            log(f"iter {iteration} gate FAIL max_iterations")
            break

        weak_axes = get_proposal_weak_axes(eval_data, rubric)
        log(f"iter {iteration} refine weak_axes={weak_axes}")
        propose_refine(analysis, passing_data, critique_data, touched_files, weak_axes, iter_dir / "refine.json", client, model)
        proposal_data = load_json(iter_dir / "refine.json")

    proposal_path = run_dir / "proposal-final.md"
    proposal_path.write_text(
        build_proposal_md(analysis, proposal_data, eval_data, validator_errors_by_id, analysis_id),
        encoding="utf-8",
    )
    log(f"proposal written: {proposal_path}")

    status = "PASS" if passes_gate(eval_data, rubric) else "FAILED"
    return {"status": status, "analysis_id": analysis_id, "run_dir": str(run_dir),
            "proposal_path": str(proposal_path), "final_iteration": iteration,
            "validator_errors": validator_errors_by_id}
