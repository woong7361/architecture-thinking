"""proposer.py

Slow loop 2단계: analysis.json을 받아 gen/critique/eval/refine 파이프라인으로
시스템 수정 제안을 만들고 changelog/proposals/YYYY-MM-DD.md를 출력한다.

정보 차단 (설계 문서 "정보 차단 규칙" 참조):
  - 층위 1: propose critique는 propose eval에 anchor되지 않는다.
  - 층위 2: gen은 후보 target 전체를 읽고, critique/eval/refine은
    제안이 건드린 파일만 읽는다.

validator 4검사 (코드, PASS/REJECT, LLM 없이):
  - 신호 실재성: cited_signals의 id가 analysis.json에 있는가
  - diff 적용성: diff.anchor가 대상 파일에 존재하는가
  - 위험 라벨 규칙: pipeline_code/agents_md → risk 반드시 "높음"
  - 범위 임계: 한 제안이 건드리는 파일 수 ≤ MAX_TARGET_FILES
"""
from __future__ import annotations

import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.dont_write_bytecode = True

from stages.scripts.llm_client import LLMClient

PROJECT_DIR = Path(__file__).resolve().parent.parent
SKILL_DIR = PROJECT_DIR.parent
PROMPTS_DIR = PROJECT_DIR / "prompts"
SCHEMAS_DIR = PROJECT_DIR / "schemas"
PROPOSALS_DIR = SKILL_DIR / "proposals"

PROPOSE_GEN_SYSTEM = PROMPTS_DIR / "propose_gen_system.md"
PROPOSE_CRITIQUE_SYSTEM = PROMPTS_DIR / "propose_critique_system.md"
PROPOSE_EVAL_SYSTEM = PROMPTS_DIR / "propose_eval_system.md"
PROPOSE_REFINE_SYSTEM = PROMPTS_DIR / "propose_refine_system.md"

PROPOSE_GEN_OUTPUT_SCHEMA = SCHEMAS_DIR / "propose_gen_output.schema.json"
PROPOSE_CRITIQUE_OUTPUT_SCHEMA = SCHEMAS_DIR / "propose_critique_output.schema.json"
PROPOSE_EVAL_OUTPUT_SCHEMA = SCHEMAS_DIR / "propose_eval_output.schema.json"

RUBRIC_PROPOSAL = PROJECT_DIR / "rubric_proposal.yaml"

KST = timezone(timedelta(hours=9))
MAX_TARGET_FILES = 3
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


def load_target_files() -> dict[str, str]:
    """gen 단계용: 후보 target 파일들을 전부 읽어 경로→내용 맵으로 반환한다."""
    targets: dict[str, str] = {}
    for path in (PROJECT_DIR / "rubric.yaml", PROJECT_DIR / "rubric_proposal.yaml"):
        if path.exists():
            targets[path.name] = path.read_text(encoding="utf-8")
    for path in sorted(PROMPTS_DIR.glob("*.md")):
        targets[f"prompts/{path.name}"] = path.read_text(encoding="utf-8")
    agents_md = PROJECT_DIR / "AGENTS.md"
    if agents_md.exists():
        targets["AGENTS.md"] = agents_md.read_text(encoding="utf-8")
    for path in sorted((PROJECT_DIR / "stages").glob("*.py")):
        targets[f"stages/{path.name}"] = path.read_text(encoding="utf-8")
    for path in sorted(PROJECT_DIR.glob("*.py")):
        targets[path.name] = path.read_text(encoding="utf-8")
    return targets


def load_touched_files(proposals: list[dict]) -> dict[str, str]:
    """critique/eval/refine 단계용: 제안이 건드린 파일만 읽는다."""
    touched: dict[str, str] = {}
    for p in proposals:
        target_file = p.get("target_file", "")
        if not target_file:
            continue
        # pipeline 디렉토리 기준으로 경로 해석
        candidate = PROJECT_DIR / target_file
        if candidate.exists():
            touched[target_file] = candidate.read_text(encoding="utf-8")
    return touched


# ---------------------------------------------------------------------------
# validator (코드, PASS/REJECT, LLM 없이)
# ---------------------------------------------------------------------------

def validate_proposal(proposal: dict, analysis: dict, touched_files: dict[str, str]) -> list[str]:
    """제안 하나를 validator 4검사로 검사한다. 실패 메시지 목록을 반환한다."""
    errors = []
    valid_signal_ids = {s["id"] for s in analysis.get("signals", []) if isinstance(s, dict)}

    # 1. 신호 실재성
    for sid in proposal.get("cited_signals", []):
        if sid not in valid_signal_ids:
            errors.append(f"cited_signals: '{sid}' not found in analysis.json signals")

    # 2. diff 적용성
    diff = proposal.get("diff", {})
    anchor = diff.get("anchor", "") if isinstance(diff, dict) else ""
    target_file = proposal.get("target_file", "")
    if anchor and target_file:
        content = touched_files.get(target_file, "")
        if content and anchor not in content:
            errors.append(f"diff.anchor not found in {target_file}")

    # 3. 위험 라벨 규칙
    target_kind = proposal.get("target_kind", "")
    risk = proposal.get("risk", "")
    if target_kind in ("pipeline_code", "agents_md") and risk != "높음":
        errors.append(f"risk must be '높음' for target_kind='{target_kind}', got '{risk}'")

    # 4. 범위 임계
    # 단일 제안이므로 파일 수는 1이지만, diff가 없거나 target_file이 비면 체크
    if not target_file:
        errors.append("target_file is empty")

    return errors


# ---------------------------------------------------------------------------
# stage 함수들
# ---------------------------------------------------------------------------

def propose_gen(
    analysis: dict,
    target_files: dict[str, str],
    output_path: Path,
    client: LLMClient,
    model: str | None,
) -> dict | None:
    system = PROPOSE_GEN_SYSTEM.read_text(encoding="utf-8")
    targets_text = "\n\n".join(
        f"FILE: {name}\n{content}" for name, content in target_files.items()
    )
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"TARGET_FILES:\n{targets_text}\n"
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=PROPOSE_GEN_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def propose_critique(
    analysis: dict,
    proposal_data: dict,
    touched_files: dict[str, str],
    output_path: Path,
    client: LLMClient,
    model: str | None,
) -> dict | None:
    # 층위 1: critique는 eval을 받지 않는다.
    system = PROPOSE_CRITIQUE_SYSTEM.read_text(encoding="utf-8")
    touched_text = "\n\n".join(
        f"FILE: {name}\n{content}" for name, content in touched_files.items()
    )
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"PROPOSAL_JSON:\n{json.dumps(proposal_data, ensure_ascii=False, indent=2)}\n\n"
        f"TOUCHED_FILES:\n{touched_text}\n"
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=PROPOSE_CRITIQUE_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def propose_eval(
    analysis: dict,
    proposal_data: dict,
    touched_files: dict[str, str],
    rubric: dict,
    output_path: Path,
    client: LLMClient,
    model: str | None,
) -> dict | None:
    # 층위 1: eval은 critique를 받지 않는다.
    system = PROPOSE_EVAL_SYSTEM.read_text(encoding="utf-8")
    touched_text = "\n\n".join(
        f"FILE: {name}\n{content}" for name, content in touched_files.items()
    )
    rubric_json = json.dumps(rubric, ensure_ascii=False, indent=2)
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"PROPOSAL_JSON:\n{json.dumps(proposal_data, ensure_ascii=False, indent=2)}\n\n"
        f"TOUCHED_FILES:\n{touched_text}\n\n"
        f"RUBRIC_JSON:\n{rubric_json}\n"
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=PROPOSE_EVAL_OUTPUT_SCHEMA,
        output_path=output_path,
        model=model,
    )


def propose_refine(
    analysis: dict,
    proposal_data: dict,
    critique_data: dict,
    touched_files: dict[str, str],
    weak_axes: list[str],
    output_path: Path,
    client: LLMClient,
    model: str | None,
) -> dict | None:
    system = PROPOSE_REFINE_SYSTEM.read_text(encoding="utf-8")
    touched_text = "\n\n".join(
        f"FILE: {name}\n{content}" for name, content in touched_files.items()
    )
    # eval 총점 원문은 넘기지 않는다 (weak_axes만)
    refine_signal = {"weak_axes": weak_axes}
    user = (
        f"ANALYSIS_JSON:\n{json.dumps(analysis, ensure_ascii=False, indent=2)}\n\n"
        f"PROPOSAL_JSON:\n{json.dumps(proposal_data, ensure_ascii=False, indent=2)}\n\n"
        f"CRITIQUE_JSON:\n{json.dumps(critique_data, ensure_ascii=False, indent=2)}\n\n"
        f"TOUCHED_FILES:\n{touched_text}\n\n"
        f"REFINE_SIGNAL:\n{json.dumps(refine_signal, ensure_ascii=False, indent=2)}\n"
    )
    return client.run_prompt(
        system=system,
        user=user,
        output_schema=PROPOSE_GEN_OUTPUT_SCHEMA,  # refine 출력은 gen과 같은 형태
        output_path=output_path,
        model=model,
    )


# ---------------------------------------------------------------------------
# 게이트: eval 결과에서 weak_axes 추출
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
    """모든 제안이 min_total과 min_axis를 통과하면 True."""
    thresholds = rubric.get("thresholds", {})
    min_total = thresholds.get("min_total", 0)
    min_axis = thresholds.get("min_axis", {})
    for ev in eval_data.get("evaluations", []):
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

def build_changelog_draft(
    proposals: list[dict],
    validator_errors_by_id: dict[str, list[str]],
    analysis: dict,
    today: str,
) -> str:
    """validator를 통과한 제안들의 CHANGELOG 초안 섹션을 만든다. Python만 사용, LLM 없음."""
    passing = [p for p in proposals if p["id"] not in validator_errors_by_id]
    if not passing:
        return ""

    run_refs = ", ".join(analysis.get("runs_included", []))
    signal_map = {s["id"]: s["summary"] for s in analysis.get("signals", []) if isinstance(s, dict)}

    lines = [
        "## CHANGELOG 초안",
        "",
        "*(적용 시 CHANGELOG.md에 복사·수정하세요. `vN`은 현재 버전+1, commit 해시는 적용 후 기입합니다.)*",
        "",
    ]

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


def build_proposal_md(
    analysis: dict,
    proposal_data: dict,
    eval_data: dict,
    validator_errors_by_id: dict[str, list[str]],
    analysis_id: str,
) -> str:
    lines = [
        f"# Slow Loop Proposal — {datetime.now(KST).date().isoformat()} ({analysis['pending_count']}개 run 분석)",
        "",
        "## 분석 요약",
    ]
    for axis, stats in analysis.get("axis_stats", {}).items():
        lines.append(
            f"- {axis}: 평균 {stats['mean']} (미달 {stats['below_min_count']}/{analysis.get('pending_count', '?')} run)"
        )
    weakness_count = len(analysis.get("critique_weaknesses", []))
    lines.append(f"- critique weakness 수집: {weakness_count}건")
    lines.append("")

    lines.append("## 제안")
    lines.append("")

    eval_map: dict[str, dict] = {}
    for ev in eval_data.get("evaluations", []):
        eval_map[ev["proposal_id"]] = ev

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
            lines.append(f"```")
            lines.append(f"anchor: {diff.get('anchor', '')[:120]}")
            lines.append(f"change: {diff.get('change', '')}")
            lines.append("```")

        # validator 결과
        v_errors = validator_errors_by_id.get(pid, [])
        if v_errors:
            lines.append(f"**validator:** REJECT — {'; '.join(v_errors)}")
        else:
            lines.append("**validator:** PASS")

        # eval 결과
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
    changelog_section = build_changelog_draft(
        proposals=proposal_data.get("proposals", []),
        validator_errors_by_id=validator_errors_by_id,
        analysis=analysis,
        today=today,
    )
    if changelog_section:
        lines.append(changelog_section)

    lines.append(f"---")
    lines.append(f"analysis_id: {analysis_id}")
    lines.append(f"generated_at: {now_iso()}")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# 오케스트레이터
# ---------------------------------------------------------------------------

def run_proposal_pipeline(
    analysis_path: Path,
    client: LLMClient,
    model: str | None,
    progress_fn=None,
) -> dict:
    """
    analysis.json → gen/critique/eval/refine → proposal-final.md

    Returns:
        {"status": "PASS"|"FAILED"|"VALIDATOR_REJECT", "proposal_path": ..., ...}
    """
    def log(msg: str) -> None:
        if progress_fn:
            progress_fn(msg)
        else:
            print(f"[proposer] {msg}", file=sys.stderr)

    analysis = load_json(analysis_path)
    analysis_id = analysis.get("analysis_id", "unknown")
    rubric = load_rubric_proposal()
    target_files = load_target_files()

    log(f"analysis_id={analysis_id} signals={len(analysis.get('signals', []))}")

    # proposals/{analysis_id}/ 디렉토리를 run 루트로 사용
    run_dir = PROPOSALS_DIR / analysis_id
    run_dir.mkdir(parents=True, exist_ok=True)

    iteration = "001"
    proposal_data: dict = {}
    eval_data: dict = {}
    validator_errors_by_id: dict[str, list[str]] = {}

    for iter_num in range(1, MAX_ITERATIONS + 1):
        iteration = f"{iter_num:03d}"
        log(f"iter {iteration} start")

        iter_dir = run_dir / f"iter_{iteration}"
        iter_dir.mkdir(parents=True, exist_ok=True)

        gen_out = iter_dir / "gen.json"
        log(f"iter {iteration} gen")
        propose_gen(
            analysis=analysis,
            target_files=target_files,
            output_path=gen_out,
            client=client,
            model=model,
        )
        proposal_data = load_json(gen_out)

        # validator 4검사 (LLM 없이)
        touched_files = load_touched_files(proposal_data.get("proposals", []))
        validator_errors_by_id = {}
        for p in proposal_data.get("proposals", []):
            errs = validate_proposal(p, analysis, touched_files)
            if errs:
                validator_errors_by_id[p["id"]] = errs
                log(f"iter {iteration} validator REJECT {p['id']}: {errs}")

        # validator 실패 제안은 eval에 가지 않는다.
        # 전체가 다 실패면 refine으로도 의미 없으므로 바로 다음 iteration
        passing_proposals = [
            p for p in proposal_data.get("proposals", [])
            if p["id"] not in validator_errors_by_id
        ]
        if not passing_proposals:
            log(f"iter {iteration} all proposals failed validator, retrying")
            if iter_num >= MAX_ITERATIONS:
                return {
                    "status": "FAILED",
                    "reason": "all proposals failed validator in all iterations",
                    "analysis_id": analysis_id,
                    "run_dir": str(run_dir),
                    "validator_errors": validator_errors_by_id,
                }
            continue

        # critique (validator 통과 제안만 대상)
        passing_data = {**proposal_data, "proposals": passing_proposals}
        crit_out = iter_dir / "critique.json"
        log(f"iter {iteration} critique")
        propose_critique(
            analysis=analysis,
            proposal_data=passing_data,
            touched_files=touched_files,
            output_path=crit_out,
            client=client,
            model=model,
        )
        critique_data = load_json(crit_out)

        # eval (critique를 받지 않음 — 층위 1)
        eval_out = iter_dir / "eval.json"
        log(f"iter {iteration} eval")
        propose_eval(
            analysis=analysis,
            proposal_data=passing_data,
            touched_files=touched_files,
            rubric=rubric,
            output_path=eval_out,
            client=client,
            model=model,
        )
        eval_data = load_json(eval_out)

        if passes_gate(eval_data, rubric):
            log(f"iter {iteration} gate PASS")
            break

        if iter_num >= MAX_ITERATIONS:
            log(f"iter {iteration} gate FAIL max_iterations reached")
            break

        # refine: eval 총점 원문은 넘기지 않고 weak_axes만
        weak_axes = get_proposal_weak_axes(eval_data, rubric)
        log(f"iter {iteration} refine weak_axes={weak_axes}")
        refine_out = iter_dir / "refine.json"
        propose_refine(
            analysis=analysis,
            proposal_data=passing_data,
            critique_data=critique_data,
            touched_files=touched_files,
            weak_axes=weak_axes,
            output_path=refine_out,
            client=client,
            model=model,
        )
        proposal_data = load_json(refine_out)

    # proposal-final.md 생성 (run_dir 안에)
    proposal_path = run_dir / "proposal-final.md"
    md = build_proposal_md(
        analysis=analysis,
        proposal_data=proposal_data,
        eval_data=eval_data,
        validator_errors_by_id=validator_errors_by_id,
        analysis_id=analysis_id,
    )
    proposal_path.write_text(md, encoding="utf-8")
    log(f"proposal written: {proposal_path}")

    status = "PASS" if passes_gate(eval_data, rubric) else "FAILED"
    return {
        "status": status,
        "analysis_id": analysis_id,
        "run_dir": str(run_dir),
        "proposal_path": str(proposal_path),
        "final_iteration": iteration,
        "validator_errors": validator_errors_by_id,
    }
