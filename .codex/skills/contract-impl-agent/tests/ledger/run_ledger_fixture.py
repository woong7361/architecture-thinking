"""원장과 리포트를 픽스처로 돌려 본다. LLM은 부르지 않는다.

pytest 없이 도는 스크립트다. run 폴더를 손으로 짜고 runner가 넘기는 것과 같은 모양의 payload를 만들어
`ledger.update_ledger`와 `report.write_report`를 iteration 순서대로 부른다.

생성기가 작업 폴더 `output/`을 제자리에서 고치고 매니페스트는 `{path, role}` 색인만 담으므로 이 픽스처도 그대로
짠다. 파일 내용은 디스크에 있고 draft JSON에는 없다. `output/`을 git 저장소로 만들고 iteration마다 커밋 하나를
남겨 runner의 흉내를 낸다. 그래서 확인의 핵심은 셋이다. 앞 상태의 변경 탐지가 원장의 해시 색인에서 나오는지,
발췌 diff의 옛 본문이 git에서 나오는지, 커밋 해시가 없어도 셈이 그대로 도는지다. 여기서 못 박는 것은 다섯이다.

1. 열려 있던 위반이 **코드를 고쳐** 사라지면 `closed_by: "code"`다.
2. 같은 위반을 **계약의 required에서 필드를 빼서** 없애면 `closed_by: "contract"`다.
   두 판을 같은 위반 id로 돌려 이 갈림이 계약 판본 대조에서만 나오는 것을 보인다.
3. 닫혔던 위반이 다시 열리면 `regressed`, 그다음 재발에서 `stuck`이다.
4. 서 있는 결정이 `supersedes` 없이 사라지면 `silent_reversal`이 기록된다.
5. `REPORT.md`에 설계가 정한 여섯 절이 그 순서로 다 있다.

계약 판본은 `alert-api-v1.yaml`과 `alert-api-v2.yaml`이다. 실제 계약은 원장 하나를 확인하기엔 크므로
같은 표기만 남긴 작은 조각을 쓴다. 판본을 고치는 갈래에서도 앞 판본을 지우지 않는다.

`alert-api-ref-v1.yaml`과 `alert-api-ref-v2.yaml`은 `$ref` 파라미터가 있고 참조가 닿지 않는 판본이다.
대조기가 "대조하지 못했다"고 적는 줄과 "선언했던 파라미터가 사라졌다"고 적는 줄이 한 대조에서 함께 나오므로,
원장이 미판정을 깨는 변경으로 세지 않는 것을 이 둘로 확인한다.

    python tests/ledger/run_ledger_fixture.py [--out <dir>]
        [--print-report code|contract|recurrence|drift|nocommit|unresolved|cause|unjudged]
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
SKILL = HERE.parents[1]
PIPELINE = SKILL / "pipeline"
sys.path.insert(0, str(PIPELINE))

# 리포트와 원장이 한국어이므로 콘솔 코드페이지가 무엇이든 UTF-8로 낸다.
for stream in (sys.stdout, sys.stderr):
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8")

from ledger import update_ledger, violation_id  # noqa: E402
from report import REPORT_NAME, SECTION_TITLES, write_report  # noqa: E402

# 지금 짜고 있는 run 폴더. `payload_of`가 추출 스펙을 그 아래에 실물로 놓는 데 쓴다.
CURRENT_RUN: Path | None = None

BRIEF_HASH = "a1b2c3d4"
V1 = "alert-api-v1.yaml"
V2 = "alert-api-v2.yaml"
REF_V1 = "alert-api-ref-v1.yaml"
REF_V2 = "alert-api-ref-v2.yaml"
CONTRACT_DIR = "contract"
OUTPUT = "output"

CONTROLLER = "src/main/java/com/thinking/tennis/api/AlertController.java"
MAPPER = "src/main/java/com/thinking/tennis/api/ErrorMapper.java"

# 위반 하나. 규칙 id와 판정 지점과 좌표가 위반 id를 만든다. `label`과 `evidence`는 게이트가 붙이는 키다.
FIELD_MISSING = {
    "rule": "response.field_missing",
    "point": "createAlert:201",
    "where": "Alert.lastCheckedAt",
    "detail": "계약이 201 응답에 담는다고 한 lastCheckedAt이 추출 스펙에 없다",
    "verdict": "violation",
    "label": "withholds_promised_response",
}


def differs_at(point: str) -> dict:
    """같은 컴포넌트의 결함이 여러 판정 지점에서 잡힌 모양.

    계약이 `Alert`를 세 응답에서 참조하면 그 스키마 하나의 차이가 세 건으로 세어진다. 규칙과 좌표와 세부가
    같으므로 원인은 하나다. 리포트가 이것을 원인으로 묶는지가 이 픽스처로 확인된다.
    """
    return {
        "rule": "response.schema_differs",
        "point": point,
        "where": "Alert",
        "detail": "계약의 Alert와 구현의 응답 타입이 다르다",
        "verdict": "violation",
        "label": "breaks_reader",
        "evidence": ["없다: Alert.lastCheckedAt", "타입이 다르다: Alert.status", "닫은 자리에 더 있다: courtId"],
    }


# 새 라벨이 붙는 위반. 계약이 파라미터에 정한 제약을 구현이 선언하지 않아 계약이 거절하겠다고 한 입력을 받는다.
# 전에는 이것이 `harmless_to_client`였고 그래서 리포트가 이것을 무해한 것과 같은 칸에 넣었다.
INPUT_DIFFERS = {
    "rule": "request.input_differs",
    "point": "getAlert:200",
    "where": "getAlert.alertId:path",
    "detail": "계약이 정한 maxLength를 구현이 선언하지 않았다",
    "verdict": "violation",
    "label": "accepts_what_contract_rejects",
}

# 「좁혔다」 가족의 기록. 응답의 값을 계약보다 좁게 선언한 것이고 그건 허용이라 위반이 아니다.
VALUE_NARROWED = {
    "rule": "response.value_narrowed",
    "point": "listAlerts:200",
    "where": "Alert.status",
    "detail": "구현이 선언한 열거형이 계약보다 좁다",
    "verdict": "observation",
    "label": "harmless_to_client",
}

# 계약이 형식을 정하지 않은 자리에 구현이 형식을 선언한 기록. 좌표는 `Problem`이다.
FORMAT_NARROWED = {
    "rule": "response.value_narrowed",
    "point": "createAlert:422",
    "where": "Problem",
    "detail": "계약이 형식을 정하지 않은 integer에 구현이 int32를 선언했다",
    "verdict": "observation",
    "label": "harmless_to_client",
}

# 관찰 규칙이 한 종류뿐인 run에서 약점 셋이 그 하나에 전부 짝지어졌다. 세 약점 모두 코드의 다른 자리를 말한다.
# 좌표가 만나지 않으므로 겹침이 아니다.
FALSE_OVERLAP_CRITIQUE = {
    "brief_hash": BRIEF_HASH,
    "iteration": "001",
    "summary": "구현이 계약이 말하지 않은 조건을 스스로 정한 자리가 있다.",
    "strengths": [],
    "weaknesses": [
        {
            "id": "w_check_delayed_on_terminal_alerts",
            "axis": "response_fidelity",
            "issue": "AlertViewFactory.of()가 신청의 상태를 보지 않고 checkDelayed를 계산한다",
            "why_it_matters": "취소·만료된 신청도 확인이 60초를 넘기면 checkDelayed가 켜진다",
            "suggestion": "종료 상태의 신청은 checkDelayed를 계산하지 않는다",
            "severity": "high",
        },
        {
            "id": "w_cancel_contention_from_sweeper",
            "axis": "failure_faithfulness",
            "issue": "CancelAlertUseCase.cancel()이 tryLock()에 실패하면 바로 503을 낸다",
            "why_it_matters": "계약은 그 상황을 경합으로 보지 않는다",
            "suggestion": "짧게 다시 시도한 뒤에도 실패하면 그때 503을 낸다",
            "severity": "medium",
        },
        {
            "id": "w_create_check_not_disclosed",
            "axis": "judgement_disclosure",
            "issue": "요청이 외부 예약처 확인을 트리거한다는 사실이 decisions에 없다",
            "why_it_matters": "신고하지 않은 채 고른 자리다",
            "suggestion": "그 선택을 결정으로 신고한다",
            "severity": "high",
        },
    ],
    "revision_directions": [],
    "contract_review": [],
    "contested_decisions": [],
}

BODY_MISSING = {
    "rule": "response.body_missing",
    "point": "cancelAlert:204",
    "where": "cancelAlert",
    "detail": "계약이 본문을 주겠다고 했는데 구현의 그 응답에 본문이 없다",
    "verdict": "violation",
    "label": "withholds_promised_response",
}

CONTROLLER_WITHOUT = """package com.thinking.tennis.api;

class AlertController {
    AlertResponse createAlert(AlertRequest request) {
        return new AlertResponse(request.courtId(), "ACTIVE");
    }
}
"""

CONTROLLER_WITH = """package com.thinking.tennis.api;

import java.time.OffsetDateTime;

class AlertController {
    AlertResponse createAlert(AlertRequest request) {
        OffsetDateTime lastCheckedAt = clock.lastCheckedAt(request.courtId());
        return new AlertResponse(request.courtId(), "ACTIVE", lastCheckedAt);
    }
}
"""

MAPPER_V1 = """package com.thinking.tennis.api;

class ErrorMapper {
    String code(Exception cause) {
        return "INVALID_BODY";
    }
}
"""

MAPPER_V2 = """package com.thinking.tennis.api;

class ErrorMapper {
    String code(Exception cause) {
        return cause instanceof IllegalStateException ? "CONFLICT" : "INVALID_BODY";
    }
}
"""

DECISION = {
    "id": "d_7c1e04",
    "point": "createAlert:201",
    "where": "Alert.lastCheckedAt",
    "question": "계약은 lastCheckedAt에 널을 허용하는데 아직 확인한 적이 없을 때 무엇을 담을지 정하지 않았다",
    "considered": [
        {"choice": "널을 담는다", "consequence": "키는 있고 값이 비어 소비자가 분기해야 한다"},
        {"choice": "신청 시각을 담는다", "consequence": "확인한 적이 없는데 확인했다고 읽힌다"},
    ],
    "chosen": "확인한 적이 없으면 널을 담는다",
    "basis": "contract_analogy",
    "rationale": "계약이 이 필드에만 널을 허용했으므로 널이 '아직 없음'을 뜻하는 자리로 읽었다",
    "confidence": "low",
    "blast_radius": ["createAlert:201", "listAlerts:200"],
    "reversal_cost": "응답 조립과 소비자 분기를 함께 고쳐야 한다",
    "supersedes": None,
}

CONTRACT_CHANGE = {
    "id": "cc_9a04b1",
    "target": "#/components/schemas/Alert/required",
    "action": "remove",
    "what": "lastCheckedAt을 필수에서 뺐다",
    "why": "확인한 적이 없는 신청에 담을 값이 없는데 계약이 키를 요구해 널을 강제했다",
    "basis": "critique:w3",
    "spec_anchor": "requirement:FR-2",
    "compatibility": "breaking",
    "migration": None,
    "iteration": "002",
}


# ── run 폴더 짜기 ───────────────────────────────────────────────────────────

def git(output_dir: Path, *args: str) -> str:
    done = subprocess.run(["git", "-C", str(output_dir), *args],
                          capture_output=True, check=True, text=True,
                          encoding="utf-8", errors="replace")
    return (done.stdout or "").strip()


def make_run(out: Path, name: str, baseline: str = V1) -> Path:
    """run 폴더를 짠다. 작업 폴더 `output/`은 git 저장소로 두고 첫 커밋을 남긴다.

    그 첫 커밋이 iteration 001의 대조 기준이다. run 폴더는 프로젝트 저장소 이력과 무관한 별도의 저장소다.
    """
    global CURRENT_RUN
    run_dir = out / name
    CURRENT_RUN = run_dir
    if run_dir.exists():
        shutil.rmtree(run_dir)
    for sub in ("baseline", "origin"):
        write_file(run_dir / sub / baseline, (HERE / baseline).read_text(encoding="utf-8"))

    output = run_dir / OUTPUT
    output.mkdir(parents=True)
    # 스켈레톤 사본과 기준선 사본. 무시 규칙도 실물처럼 둔다 — 원장의 파일 목록이 git의 목록과 같아야 한다
    write_file(output / ".gitignore", "target/\n")
    write_file(output / "pom.xml", "<project><artifactId>tennis-alert</artifactId></project>\n")
    write_file(output / f"{CONTRACT_DIR}/{baseline}", (HERE / baseline).read_text(encoding="utf-8"))
    git(output, "init", "-q")
    git(output, "config", "user.email", "fixture@example.test")
    git(output, "config", "user.name", "ledger fixture")
    git(output, "add", "-A")
    git(output, "commit", "-q", "-m", "skeleton")
    return run_dir


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def write_draft(run_dir: Path, iteration: str, contract: str, from_contract: str,
                files: dict[str, str], *, claims=None, decisions=None,
                contract_changes=None, repairs=None, undeclared: dict[str, str] | None = None,
                record_commit: bool = True, build_noise: bool = True) -> str:
    """작업 폴더를 제자리에서 고치고 커밋 하나를 남긴 뒤, 색인만 담은 draft를 쓴다.

    매니페스트에는 `{path, role}`만 들어간다. 파일 내용은 디스크에 있고 원장은 그것을 대조한다.
    `record_commit`이 거짓이면 draft에 커밋 해시를 적지 않는다 — 그때도 원장이 깨지지 않아야 한다.
    `build_noise`는 `target/` 아래에 빌드 산출물을 남긴다. 무시된 파일이 변경으로 새지 않는 것을 보이는 자리다.
    돌려주는 것은 draft JSON의 run 폴더 기준 상대경로다.
    """
    output = run_dir / OUTPUT
    contract_rel = f"{CONTRACT_DIR}/{contract}"

    declared = dict(files)
    declared[contract_rel] = (HERE / contract).read_text(encoding="utf-8")
    for path, content in declared.items():
        write_file(output / path, content)

    # 색인에 적지 않고 작업 폴더에만 남긴 파일. 판정 대상 밖에 조용히 놓인 표면이다
    for path, content in (undeclared or {}).items():
        write_file(output / path, content)

    if build_noise:
        write_file(output / "target/classes/AlertController.class", f"binary-ish {iteration}\n")

    manifest = [{"path": path, "role": "contract" if path == contract_rel else "code"}
                for path in declared]

    git(output, "add", "-A")
    git(output, "commit", "-q", "--allow-empty", "-m", f"iter {iteration}")
    commit = git(output, "rev-parse", "HEAD")

    draft = {
        "brief_hash": BRIEF_HASH,
        "iteration": iteration,
        "stage": "gen" if iteration == "001" else "refine",
        "files": manifest,
        "contract_version": {"path": contract_rel, "from": f"{CONTRACT_DIR}/{from_contract}"},
        "claims": claims or [],
        "decisions": decisions or [],
        "contract_changes": contract_changes or [],
    }
    if record_commit:
        draft["commit"] = commit
    if repairs:
        draft["repairs"] = repairs
    path = run_dir / f"iter_{iteration}" / "draft.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(draft, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return path.relative_to(run_dir).as_posix()


def payload_of(iteration: str, draft_rel: str, violations: list[dict], *,
               observations=None, critique=None, scores=None, decision_risk=None,
               status=None, gate_status=None, derived_spec: bool = True,
               unmeasured=None, run_dir: Path | None = None) -> dict:
    """runner가 합산 자리에서 원장에 넘기는 payload와 같은 키를 쓴다.

    `gate_status`를 주면 세 판정을 그 값으로 덮는다. 판정이 `SKIPPED`인데 위반 목록이 빈 run을 만들어,
    합격선의 뒤 문장만으로는 그것이 통과로 읽히는 것을 보이는 데 쓴다.
    """
    spec_rel = f"iter_{iteration}/g0/derived.json"
    target = run_dir or CURRENT_RUN
    if derived_spec and target is not None:
        write_file(target / spec_rel, "{}\n")
    gate = {
        "iteration": iteration,
        "g0": gate_status or "PASS",
        "g1": gate_status or ("REJECT" if violations else "PASS"),
        "changes": gate_status or "PASS",
        "violations": violations,
        "observations": observations or [],
        "errors": [],
        "derived_spec": spec_rel if derived_spec else None,
        "log": [],
    }
    gate_errors = [f"gate:{v['rule']}@{v['point']}: {v['detail']}" for v in violations]
    return {
        "iteration": iteration,
        "status": status or ("REJECT" if violations else "PASS"),
        "draft": draft_rel,
        "gate": gate,
        "critique": critique,
        "eval": {
            "brief_hash": BRIEF_HASH,
            "iteration": iteration,
            "rubric_name": "impl:v1",
            "rubric_scores": {
                "scores": scores or {},
                "weights": {},
                "weighted_total": round(sum((scores or {}).values()) / len(scores), 2) if scores else 0,
            },
            "axis_rationales": {},
        } if scores else None,
        "unmeasured_axes": list(unmeasured or []),
        "decision_risk": decision_risk or {"level": "none", "reasons": []},
        "gate_errors": gate_errors,
        "rubric_errors": [],
        "skipped_reason": None,
    }


CRITIQUE = {
    "brief_hash": BRIEF_HASH,
    "iteration": "001",
    "summary": "응답의 값이 계약이 말한 뜻과 어긋나는 자리가 있다.",
    "strengths": [],
    "weaknesses": [
        {
            "id": "w3",
            "axis": "response_fidelity",
            "issue": "createAlert:201의 Alert.lastCheckedAt에 담을 값이 없다",
            "why_it_matters": "소비자가 확인 시각으로 읽는 자리에 아무 값도 오지 않는다",
            "suggestion": "계약이 이 필드를 필수로 두는 것이 맞는지 다시 본다",
            "severity": "high",
            "target": {"pointer": "#/components/schemas/Alert/required", "action": "remove", "scope": "contract"},
        }
    ],
    "revision_directions": [],
    "contract_review": [
        {
            "id": "c1",
            "kind": "proposal",
            "pointer": "#/components/schemas/Alert/required",
            "finding": "확인한 적이 없는 신청에 담을 값이 없는데 계약이 키를 요구한다",
            "proposed_change": "lastCheckedAt을 필수에서 뺀다",
            "action": "remove",
            "spec_anchor": "requirement:FR-2",
            "severity": "high",
        },
        {
            "id": "c2",
            "kind": "proposal",
            "pointer": "#/components/schemas/AlertRequest/properties/note",
            "finding": "note가 무엇을 담는지 계약이 말하지 않는다",
            "proposed_change": "note를 지운다",
            "action": "remove",
            "spec_anchor": "",
            "severity": "low",
            "unjustified_proposal": True,
        },
    ],
    "contested_decisions": [],
}


# ── 세 갈래 ─────────────────────────────────────────────────────────────────

def scenario_code(out: Path) -> dict:
    """001에서 위반이 열리고 002에서 코드를 고쳐 사라진다. 계약 판본은 움직이지 않는다."""
    run_dir = make_run(out, "code")
    ledger = {}

    draft = write_draft(run_dir, "001", V1, V1, {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V1},
                        claims=[{"point": "createAlert:201", "note": "201 응답을 계약대로 조립했다"}],
                        decisions=[DECISION])
    ledger = update_ledger(run_dir, "001", payload_of("001", draft, [FIELD_MISSING],
                                                      critique=CRITIQUE,
                                                      scores={"response_fidelity": 4.2},
                                                      decision_risk={"level": "high",
                                                                     "reasons": ["low confidence decisions: ['d_7c1e04']"]}))
    write_report(run_dir, ledger, payload_of("001", draft, [FIELD_MISSING]))

    draft = write_draft(run_dir, "002", V1, V1, {CONTROLLER: CONTROLLER_WITH, MAPPER: MAPPER_V1},
                        claims=[{"point": "createAlert:201", "note": "lastCheckedAt을 담았다"}],
                        decisions=[DECISION],
                        repairs=[{"violation_id": violation_id(FIELD_MISSING),
                                  "summary": "응답 조립에 lastCheckedAt을 넣었다"}])
    payload = payload_of("002", draft, [], scores={"response_fidelity": 4.6}, status="PASS")
    ledger = update_ledger(run_dir, "002", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


def scenario_contract(out: Path) -> dict:
    """001에서 같은 위반이 열리고 002에서 계약의 required에서 그 필드를 빼서 사라진다."""
    run_dir = make_run(out, "contract")

    draft = write_draft(run_dir, "001", V1, V1, {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V1},
                        claims=[{"point": "createAlert:201", "note": "201 응답을 계약대로 조립했다"}],
                        decisions=[DECISION])
    payload = payload_of("001", draft, [FIELD_MISSING], critique=CRITIQUE,
                         observations=[{"rule": "surface.extra_header", "point": "createAlert:201",
                                        "where": "X-Request-Id", "detail": "계약에 없는 응답 헤더가 있다",
                                        "verdict": "observation"}],
                         scores={"response_fidelity": 4.2, "judgement_disclosure": 4.0},
                         decision_risk={"level": "high",
                                        "reasons": ["low confidence decisions: ['d_7c1e04']"]})
    ledger = update_ledger(run_dir, "001", payload)
    write_report(run_dir, ledger, payload)

    # 코드는 그대로 두고 계약만 고친다. 위반이 사라지는 이유가 계약뿐인 것을 보이기 위해서다.
    draft = write_draft(run_dir, "002", V2, V1, {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V1},
                        claims=[{"point": "createAlert:201", "note": "201 응답을 계약대로 조립했다"}],
                        decisions=[DECISION], contract_changes=[CONTRACT_CHANGE],
                        repairs=[{"violation_id": violation_id(FIELD_MISSING),
                                  "summary": "응답 조립에 lastCheckedAt을 넣었다"}])
    payload = payload_of("002", draft, [], scores={"response_fidelity": 4.6, "judgement_disclosure": 4.4},
                         status="PASS",
                         decision_risk={"level": "high",
                                        "reasons": ["breaking contract changes: ['cc_9a04b1']",
                                                    "low confidence decisions: ['d_7c1e04']"]})
    ledger = update_ledger(run_dir, "002", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger, "payload": payload}


def scenario_recurrence(out: Path) -> dict:
    """닫혔던 위반이 다시 열리는 것을 두 번 보이고, 서 있는 결정이 말없이 사라지는 것을 함께 본다."""
    run_dir = make_run(out, "recurrence")
    files_without = {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V1}
    files_with = {CONTROLLER: CONTROLLER_WITH, MAPPER: MAPPER_V1}
    # 003은 열린 위반과 무관한 파일을 함께 바꾼다. 곁가지 변경이 기록되는지 보기 위해서다.
    files_collateral = {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V2}

    plan = [
        ("001", files_without, [FIELD_MISSING], [DECISION]),
        ("002", files_with, [], [DECISION]),
        ("003", files_collateral, [FIELD_MISSING], []),          # 재발 + 말없는 되돌림 + 곁가지 변경
        ("004", files_with, [], []),
        ("005", files_without, [FIELD_MISSING], []),             # 두 번째 재발
    ]
    ledger = {}
    for iteration, files, violations, decisions in plan:
        draft = write_draft(run_dir, iteration, V1, V1, files, decisions=decisions)
        payload = payload_of(iteration=iteration, draft_rel=draft,
                             violations=violations, scores={"response_fidelity": 4.1})
        ledger = update_ledger(run_dir, iteration, payload)
        write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


UNRESOLVED_CHANGE = {
    "id": "cc_4d21f0",
    "target": "#/components/schemas/Alert",
    "action": "rename",
    "what": "Alert 스키마의 이름을 Alerts로 바꿨다",
    "why": "목록 응답과 단건 응답이 같은 이름을 쓰는 것이 헷갈렸다",
    "basis": "critique:w7",
    "spec_anchor": "requirement:FR-4",
    "compatibility": "compatible",
    "migration": "소비자의 스키마 이름 참조를 함께 고친다",
    "iteration": "002",
}


def scenario_unjudged(out: Path) -> dict:
    """세 판정이 모두 `SKIPPED`인데 위반 목록은 비어 있는 run. 합격선의 반례다.

    "위반이 없다"만으로 합격을 읽으면 이 run이 통과로 보인다. 게이트를 부수는 것이 게이트를 통과하는 가장
    쉬운 길이 되는 자리이므로, 리포트가 판정 여부를 따로 적어 이것을 합격으로 읽지 않는지 확인한다.
    001은 추출 스펙도 없고 Eval도 돌지 않았고, 002는 판정까지는 갔지만 재지 못한 축이 있다.
    """
    run_dir = make_run(out, "unjudged")
    draft = write_draft(run_dir, "001", V1, V1, {CONTROLLER: CONTROLLER_WITHOUT})
    payload = payload_of("001", draft, [], gate_status="SKIPPED", derived_spec=False, status="REJECT")
    payload["eval"] = None
    ledger = update_ledger(run_dir, "001", payload)
    write_report(run_dir, ledger, payload)

    draft = write_draft(run_dir, "002", V1, V1, {CONTROLLER: CONTROLLER_WITH})
    payload = payload_of("002", draft, [], scores={"response_fidelity": 4.6}, status="PASS",
                         unmeasured=["state_continuity"])
    ledger = update_ledger(run_dir, "002", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


def scenario_cause(out: Path) -> dict:
    """한 원인이 여러 판정 지점에서 잡히는 run. 새 규칙 id와 새 키를 함께 쓴다.

    001에서 `response.schema_differs`가 세 지점에, `response.body_missing`이 한 지점에 열린다. 002에서 계약의
    required에서 `lastCheckedAt`을 빼면 앞의 셋은 계약으로 닫히고 뒤의 하나는 좌표가 그대로이므로 코드로 닫힌다.
    새 규칙 id로도 `closed_by`의 두 갈래가 갈리는 것을 이 갈래가 보인다.
    """
    run_dir = make_run(out, "cause")
    findings = ([differs_at(p) for p in ("createAlert:201", "listAlerts:200", "getAlert:200")]
                + [BODY_MISSING, INPUT_DIFFERS])

    draft = write_draft(run_dir, "001", V1, V1, {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V1},
                        decisions=[DECISION])
    payload = payload_of("001", draft, findings, observations=[VALUE_NARROWED, FORMAT_NARROWED],
                         critique=FALSE_OVERLAP_CRITIQUE,
                         scores={"response_fidelity": 4.2, "failure_faithfulness": 3.0,
                                 "judgement_disclosure": 4.0})
    ledger = update_ledger(run_dir, "001", payload)
    write_report(run_dir, ledger, payload)

    # 계약을 고치고 본문 자리는 코드로 고친다
    draft = write_draft(run_dir, "002", V2, V1, {CONTROLLER: CONTROLLER_WITH, MAPPER: MAPPER_V1},
                        decisions=[DECISION], contract_changes=[CONTRACT_CHANGE])
    payload = payload_of("002", draft, [], status="PASS",
                         critique={**FALSE_OVERLAP_CRITIQUE, "iteration": "002", "weaknesses": [{
                             "id": "w_unopened_date_rejected",
                             "axis": "request_tolerance",
                             "issue": "아직 열리지 않은 날짜의 신청을 구현이 거절한다",
                             "why_it_matters": "계약은 그 날짜를 받겠다고 했다",
                             "suggestion": "계약이 받겠다고 한 범위를 그대로 받는다",
                             "severity": "high",
                         }]},
                         scores={"request_tolerance": 5.0})
    ledger = update_ledger(run_dir, "002", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


def scenario_unresolved(out: Path) -> dict:
    """대조기가 참조를 풀지 못한 자리와 실제로 깨는 변경이 한 대조에서 함께 나오는 run.

    002의 판본은 `Alert` 스키마의 이름을 바꿔 응답의 참조를 끊고 `$ref` 파라미터 선언을 뺐다. 앞의 것은
    미판정이고 뒤의 것은 깨는 변경이다. 원장이 둘을 같은 수에 섞으면 리포트가 계약이 깨졌다고 말하게 된다.
    """
    run_dir = make_run(out, "unresolved", baseline=REF_V1)
    draft = write_draft(run_dir, "001", REF_V1, REF_V1, {CONTROLLER: CONTROLLER_WITHOUT},
                        decisions=[DECISION])
    payload = payload_of("001", draft, [FIELD_MISSING], scores={"response_fidelity": 4.1})
    ledger = update_ledger(run_dir, "001", payload)
    write_report(run_dir, ledger, payload)

    draft = write_draft(run_dir, "002", REF_V2, REF_V1, {CONTROLLER: CONTROLLER_WITHOUT},
                        decisions=[DECISION], contract_changes=[UNRESOLVED_CHANGE])
    payload = payload_of("002", draft, [], scores={"response_fidelity": 4.4}, status="PASS")
    ledger = update_ledger(run_dir, "002", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


def scenario_nocommit(out: Path) -> dict:
    """커밋 해시를 draft에 적지 않은 run. 발췌 diff만 빠지고 변경의 셈은 해시 색인으로 그대로 돈다."""
    run_dir = make_run(out, "nocommit")
    draft = write_draft(run_dir, "001", V1, V1, {CONTROLLER: CONTROLLER_WITHOUT, MAPPER: MAPPER_V1},
                        decisions=[DECISION], record_commit=False)
    payload = payload_of("001", draft, [FIELD_MISSING], scores={"response_fidelity": 4.1})
    ledger = update_ledger(run_dir, "001", payload)
    write_report(run_dir, ledger, payload)

    draft = write_draft(run_dir, "002", V1, V1, {CONTROLLER: CONTROLLER_WITH, MAPPER: MAPPER_V1},
                        record_commit=False,
                        repairs=[{"violation_id": violation_id(FIELD_MISSING),
                                  "summary": "응답 조립에 lastCheckedAt을 넣었다"}])
    payload = payload_of("002", draft, [], scores={"response_fidelity": 4.6}, status="PASS")
    ledger = update_ledger(run_dir, "002", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


def scenario_drift(out: Path) -> dict:
    """색인에 적지 않은 파일이 작업 폴더에 있으면 원장이 그 경로를 센다.

    판정은 게이트의 몫이다. 원장은 어느 경로에서 색인과 실물이 어긋났는지만 기록한다.
    """
    run_dir = make_run(out, "drift")
    draft = write_draft(run_dir, "001", V1, V1, {CONTROLLER: CONTROLLER_WITHOUT},
                        undeclared={"src/main/java/com/thinking/tennis/api/Hidden.java": MAPPER_V1})
    payload = payload_of("001", draft, [], scores={"response_fidelity": 4.1}, status="PASS")
    ledger = update_ledger(run_dir, "001", payload)
    write_report(run_dir, ledger, payload)
    return {"run_dir": run_dir, "ledger": ledger}


# ── 확인 ────────────────────────────────────────────────────────────────────

results: list[tuple[bool, str]] = []


def check(name: str, ok: bool, detail: str = "") -> None:
    results.append((ok, f"{name}{(' — ' + detail) if detail else ''}"))


def one(ledger: dict, vid: str | None = None) -> dict:
    violations = ledger.get("violations") or []
    if vid:
        return next(v for v in violations if v["id"] == vid)
    assert len(violations) == 1, f"위반이 하나가 아니다: {[v['id'] for v in violations]}"
    return violations[0]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=None)
    parser.add_argument("--print-report", default="contract",
                        choices=["code", "contract", "recurrence", "drift", "nocommit",
                                 "unresolved", "cause", "unjudged", "none"])
    args = parser.parse_args()
    out = args.out or Path(tempfile.mkdtemp(prefix="ledger-fixture-"))
    out.mkdir(parents=True, exist_ok=True)
    print(f"run 폴더: {out}\n")

    code = scenario_code(out)
    contract = scenario_contract(out)
    recurrence = scenario_recurrence(out)
    drift = scenario_drift(out)
    nocommit = scenario_nocommit(out)
    unresolved = scenario_unresolved(out)
    cause = scenario_cause(out)
    unjudged_run = scenario_unjudged(out)

    # 1. 코드로 닫았다
    v_code = one(code["ledger"])
    check("closed_by code", v_code["closed_by"] == "code",
          f"{v_code['id']} status={v_code['status']} closed_by={v_code['closed_by']} "
          f"because={v_code['closed_because']}")
    check("claimed_fix는 refine의 문장이고 verified_fix는 작업 폴더 대조의 결과다",
          v_code["claimed_fix"] == "응답 조립에 lastCheckedAt을 넣었다"
          and CONTROLLER in ((v_code.get("verified_fix") or {}).get("files") or []),
          f"claimed_fix={v_code['claimed_fix']!r} verified_fix.files={(v_code.get('verified_fix') or {}).get('files')}")

    # 발췌 diff의 옛 본문이 git에서 나온다. 작업 폴더에는 지금 상태만 있으므로 git 없이는 이 줄이 나오지 않는다
    excerpt = (v_code.get("verified_fix") or {}).get("diff") or ""
    check("발췌 diff의 옛 본문이 git에서 나온다",
          '-        return new AlertResponse(request.courtId(), "ACTIVE");' in excerpt
          and "+        OffsetDateTime lastCheckedAt" in excerpt,
          f"발췌 {len(excerpt.splitlines())}줄, 지운 줄 "
          f"{len([l for l in excerpt.splitlines() if l.startswith('-') and not l.startswith('---')])}개")

    # 앞 상태의 변경 탐지는 원장이 적어 둔 해시 색인에서 나온다
    hashes = [len((e.get("file_hashes") or {})) for e in code["ledger"]["iterations"]]
    check("앞 상태를 해시 색인으로 들고 간다", all(n > 0 for n in hashes) and len(hashes) == 2,
          f"iteration별 해시 색인 크기 {hashes}, 커밋 "
          f"{[str(e.get('commit'))[:8] for e in code['ledger']['iterations']]}")

    # 무시된 빌드 산출물이 변경으로 새지 않는다. 원장의 파일 목록이 git의 목록과 같기 때문이다
    target_paths = [o for o in recurrence["ledger"]["observations"]
                    if str(o.get("where") or "").startswith("target/")]
    tracked_target = [p for e in recurrence["ledger"]["iterations"] for p in (e.get("file_hashes") or {})
                      if p.startswith("target/")]
    check("무시된 빌드 산출물이 변경으로 새지 않는다",
          not target_paths and not tracked_target,
          f"target/을 가리키는 기록 {len(target_paths)}건, 해시 색인에 든 target/ 파일 {len(tracked_target)}개")

    # 2. 계약으로 닫았다
    v_contract = one(contract["ledger"])
    check("closed_by contract", v_contract["closed_by"] == "contract",
          f"{v_contract['id']} status={v_contract['status']} closed_by={v_contract['closed_by']} "
          f"because={v_contract['closed_because']}")
    check("두 갈래가 같은 위반 id를 쓴다", v_code["id"] == v_contract["id"],
          f"{v_code['id']} == {v_contract['id']}")
    impact = contract["ledger"]["impact"]
    check("계약으로 닫힌 것이 영향 절에 모인다",
          v_contract["id"] in impact["closed_by_contract"] and not impact["closed_by_code"],
          f"closed_by_contract={impact['closed_by_contract']} closed_by_code={impact['closed_by_code']}")
    change = contract["ledger"]["contract_changes"][0]
    check("호환성은 대조기가 판정한다",
          change["compatibility_verdict"] == "breaking" and change["breaking_lines"],
          f"claimed={change['compatibility_claimed']} verdict={change['compatibility_verdict']} "
          f"lines={change['breaking_lines']}")

    # 계약 판본 파일의 변경이 코드 변경으로 새지 않는가. 이 갈래는 코드를 한 줄도 고치지 않았다
    contract_observations = [o for o in contract["ledger"]["observations"]
                             if str(o.get("where") or "").startswith(f"{CONTRACT_DIR}/")]
    check("계약 판본 파일이 코드 변경으로 새지 않는다",
          not ((v_contract.get("verified_fix") or {}).get("files") or []) and not contract_observations,
          f"verified_fix.files={(v_contract.get('verified_fix') or {}).get('files')} "
          f"note={(v_contract.get('verified_fix') or {}).get('note')!r} "
          f"contract 경로를 가리키는 기록={len(contract_observations)}건")
    # 앞 판본이 작업 폴더에서 사라져도 git에서 꺼내 같은 판정을 낸다
    import ledger as ledger_module
    v1_path = contract["run_dir"] / OUTPUT / CONTRACT_DIR / V1
    prev_commit = contract["ledger"]["iterations"][0]["commit"]
    moved = v1_path.with_suffix(".yaml.moved")
    v1_path.rename(moved)
    from_git, source = ledger_module.contract_doc(contract["run_dir"], f"{CONTRACT_DIR}/{V1}", prev_commit)
    moved.rename(v1_path)
    check("앞 판본이 작업 폴더에 없으면 git에서 꺼낸다",
          source.startswith("git:") and "lastCheckedAt" in (
              (from_git.get("components", {}).get("schemas", {}).get("Alert", {}) or {}).get("required") or []),
          f"source={source} Alert.required={(from_git.get('components', {}).get('schemas', {}).get('Alert', {}) or {}).get('required')}")

    drifted = [o for o in drift["ledger"]["observations"] if o["kind"] == "manifest_drift"]
    check("색인에 없는 파일이 작업 폴더에 있으면 센다", bool(drifted),
          "; ".join(f"{o['where']}: {o['detail']}" for o in drifted))
    check("스켈레톤 사본은 색인에 없어도 어긋남이 아니다",
          not [o for o in contract["ledger"]["observations"] if o["kind"] == "manifest_drift"],
          "; ".join(f"{e['iteration']} 색인 {e['declared_files']}개 실물 {e['files_on_disk']}개 "
                    f"커밋 {str(e['commit'])[:8]}" for e in contract["ledger"]["iterations"]))

    # 3. 재발과 굳음
    v_rec = one(recurrence["ledger"])
    check("stuck", v_rec["status"] == "stuck" and v_rec["recurrence"] == 2,
          f"{v_rec['id']} status={v_rec['status']} recurrence={v_rec['recurrence']}")
    collateral = [o for o in recurrence["ledger"]["observations"] if o["kind"] == "collateral_change"]
    check("곁가지 변경이 기록된다", bool(collateral),
          "; ".join(f"{o['iteration']} {o['where']}" for o in collateral))

    # 4. 말없는 되돌림
    reversals = recurrence["ledger"]["disagreements"]["silent_reversal"]
    check("silent_reversal", bool(reversals),
          "; ".join(f"{r['iteration']} {r['detail']}" for r in reversals))

    # 불일치 절의 나머지
    dis = contract["ledger"]["disagreements"]
    check("claims와 게이트 판정의 대조", bool(dis["claim_mismatch"]),
          "; ".join(f"{c['iteration']} {c['point']} {c['detail']}" for c in dis["claim_mismatch"]))
    check("unjustified_proposal", bool(dis["unjustified_proposal"]),
          "; ".join(str(p["id"]) for p in dis["unjustified_proposal"]))
    check("axis_disagreement", bool(dis["axis_disagreement"]),
          "; ".join(f"{a['axis']}={a['eval_score']}" for a in dis["axis_disagreement"]))
    check("machine_checkable_restatement", bool(dis["machine_checkable_restatement"]),
          "; ".join(f"{m['critique_id']}→{m['rule']}" for m in dis["machine_checkable_restatement"]))

    # runner가 부르는 자리와 시그니처가 맞는가
    import runner as runner_module
    loaded_ledger = runner_module.load_ledger_writer()
    loaded_report = runner_module.load_report_writer()
    # 같은 iteration을 다시 넘긴다. runner는 게이트 결과를 매 실행 다시 계산하므로 원장이 두 번 세지 않아야 한다.
    run_dir = contract["run_dir"]
    probe = contract["payload"]
    reloaded = loaded_ledger(run_dir=run_dir, iteration="002", payload=probe)
    loaded_report(run_dir=run_dir, ledger=reloaded, result=probe)
    check("runner의 로더가 두 함수를 찾고 키워드 인자로 부를 수 있다",
          loaded_ledger is not None and loaded_report is not None
          and reloaded["violations"][0]["closed_by"] == "contract",
          f"update_ledger={getattr(loaded_ledger, '__name__', None)} "
          f"write_report={getattr(loaded_report, '__name__', None)} "
          f"iterations={[e['iteration'] for e in reloaded['iterations']]} "
          f"oscillation={[o['detail'] for o in reloaded['observations'] if o['kind'] == 'contract_oscillation']}")

    # 커밋 해시가 없어도 원장이 깨지지 않는다
    v_nocommit = one(nocommit["ledger"])
    missing = [o for o in nocommit["ledger"]["observations"] if o["kind"] == "commit_missing"]
    nc_excerpt = (v_nocommit.get("verified_fix") or {}).get("diff") or ""
    check("커밋 해시가 없어도 변경은 세고 발췌만 빠진다",
          v_nocommit["closed_by"] == "code"
          and CONTROLLER in ((v_nocommit.get("verified_fix") or {}).get("files") or [])
          and "앞 본문을 가져오지 못해" in nc_excerpt
          and bool(missing),
          f"closed_by={v_nocommit['closed_by']} files={(v_nocommit.get('verified_fix') or {}).get('files')} "
          f"발췌={nc_excerpt.strip()!r} commit_missing={len(missing)}건")

    # 대조하지 못한 자리를 깨는 변경으로 세지 않는다
    u_step = unresolved["ledger"]["impact"]["step_diff"]
    u_change = unresolved["ledger"]["contract_changes"][0]
    u_records = [o for o in unresolved["ledger"]["observations"] if o["kind"] == "contract_uncompared"]
    check("참조를 풀지 못한 자리를 깨는 변경과 갈라 센다",
          len(u_step["breaking"]) == 1 and "파라미터가 사라졌다" in u_step["breaking"][0]
          and len(u_step["uncompared"]) == 1 and "대조하지 못했다" in u_step["uncompared"][0]
          and not any("대조하지 못했다" in line for line in u_step["breaking"]),
          f"breaking={u_step['breaking']} uncompared={u_step['uncompared']}")
    check("대조하지 못한 변경은 호환성을 판정하지 않는다",
          u_change["compatibility_verdict"] == "uncompared"
          and not u_change["breaking_lines"] and u_change["uncompared_lines"]
          and bool(u_records),
          f"claimed={u_change['compatibility_claimed']} verdict={u_change['compatibility_verdict']} "
          f"uncompared_lines={u_change['uncompared_lines']} 기록={len(u_records)}건")
    check("$ref 파라미터가 있는 계약에서 contract_diff가 터지지 않는다",
          u_step.get("error") is None
          and all((e.get("eval") or {}) is not None for e in unresolved["ledger"]["iterations"])
          and (unresolved["run_dir"] / "run_ledger.json").exists(),
          f"error={u_step.get('error')!r} iterations={[e['iteration'] for e in unresolved['ledger']['iterations']]}")
    u_report = (unresolved["run_dir"] / REPORT_NAME).read_text(encoding="utf-8")
    check("리포트가 깨는 변경 수에 미판정을 섞지 않는다",
          "대조기 판정으로 깨는 변경은 0건이다" in u_report
          and "대조하지 못한 자리" in u_report
          and "대조기가 참조를 풀지 못해 판정하지 못한 변경이 1건" in u_report,
          "; ".join(line for line in u_report.splitlines()
                    if "깨는 변경은" in line or "판정하지 못한 변경이" in line)[:220])

    # 대조기가 터져도 원장은 남는다. 기록 장치의 실패로 run이 죽으면 안 된다
    import ledger as ledger_mod

    def boom(*_args, **_kwargs):
        raise KeyError("Alert")

    broken = ledger_mod.contract_diff({"a": 1}, {"a": 2},
                                      {"diff_pointers": boom, "compare_http": boom, "points_of": boom})
    check("대조기가 터지면 셈을 비우고 실패를 들고 나간다",
          broken["available"] and broken["error"].startswith("KeyError")
          and not broken["breaking"] and not broken["uncompared"],
          f"error={broken['error']!r} breaking={broken['breaking']} pointers={broken['pointers']}")

    import ledger as ledger_module  # noqa: F811 — 앞에서도 쓰지만 이 블록만 떼어 읽을 수 있게 둔다

    # 새 규칙 id와 새 키. 원장은 지점 단위로 두고 리포트가 원인으로 묶는다
    c_ledger = cause["ledger"]
    c_violations = c_ledger["violations"]
    differs = [v for v in c_violations if v["rule"] == "response.schema_differs"]
    body = next(v for v in c_violations if v["rule"] == "response.body_missing")
    check("원장은 지점 단위로 남는다",
          len(c_violations) == 5 and len(differs) == 3
          and sorted(v["point"] for v in differs) == ["createAlert:201", "getAlert:200", "listAlerts:200"]
          and len({v["id"] for v in differs}) == 3,
          f"위반 {len(c_violations)}건, 지점 {sorted(v['point'] for v in c_violations)}")
    check("라벨과 family와 접힌 잎을 들고 간다",
          all(v["label"] == "breaks_reader" and v["family"] == "differs"
              and len(v["evidence"]) == 3 and v["label_meaning"] for v in differs)
          and body["family"] == "missing" and body["label"] == "withholds_promised_response",
          f"differs: family={differs[0]['family']} label={differs[0]['label']} 잎={len(differs[0]['evidence'])}개; "
          f"body_missing: family={body['family']} label={body['label']}")
    check("새 규칙 id로도 closed_by의 두 갈래가 갈린다",
          all(v["closed_by"] == "contract" for v in differs) and body["closed_by"] == "code",
          "; ".join(f"{v['rule']}@{v['point']} → {v['closed_by']} ({v['closed_because']})"
                    for v in differs[:1] + [body]))

    c_report = (cause["run_dir"] / REPORT_NAME).read_text(encoding="utf-8")
    check("리포트 5절이 원인으로 묶는다",
          "그 5건은 원인 3개에서 나왔다" in c_report
          and "그중 1개가 여러 판정 지점에 걸쳐 3건을 만들었다" in c_report
          and "닿는 판정 지점은 3곳이다" in c_report,
          "; ".join(line for line in c_report.splitlines()
                    if "원인 2개" in line or "닿는 판정 지점은" in line)[:220])
    check("접힌 잎을 리포트에서 편다",
          "게이트가 한 건으로 접은 잎 3개다" in c_report
          and "- 없다: Alert.lastCheckedAt" in c_report
          and "보지 못한 자리는 3절에 따로 있다" in c_report,
          "; ".join(line for line in c_report.splitlines() if "접은 잎" in line)[:200])

    # 계약 변경의 종류
    c_change = c_ledger["contract_changes"][0]
    u_kind = unresolved["ledger"]["contract_changes"][0]
    check("약속을 줄인 변경과 표현만 바꾼 변경을 가른다",
          c_change["kind"] == "promise" and c_change["promise_lines"]
          and u_kind["kind"] == "representation" and not u_kind["promise_lines"],
          f"{c_change['id']}={c_change['kind']} {c_change['promise_lines']}; "
          f"{u_kind['id']}={u_kind['kind']} (판정 지점과 필수와 산문 슬롯이 그대로다)")
    check("리포트 2절이 약속 변경을 위로 올린다",
          "약속을 줄인 변경이 위로 온다" in c_report
          and "그중 1건이 계약의 약속을 줄였다" in c_report
          and "줄인 약속은 이렇다" in c_report,
          "; ".join(line for line in c_report.splitlines() if "약속을 줄였다" in line)[:200])

    # 게이트가 낼 수 있는 규칙 id 전부에 카드가 있는가. 없으면 why_it_matters가 조용히 detail로 떨어진다
    import importlib
    g1 = None
    for name in ("gates.g1", "pipeline.gates.g1"):
        try:
            g1 = importlib.import_module(name)
            break
        except ImportError:
            continue
    rules = ledger_module._load_rules() if g1 else {}
    cards = {c.get("id") for c in (rules.get("rules") or [])}
    missing_cards = sorted(set(getattr(g1, "RULE_IDS", ())) - cards) if g1 else ["g1 모듈을 찾지 못했다"]
    check("게이트가 낼 수 있는 규칙 id에 모두 카드가 있다", not missing_cards,
          f"RULE_IDS {len(getattr(g1, 'RULE_IDS', ()))}개, 카드 {len(cards)}개"
          + (f", 카드 없는 id {missing_cards}" if missing_cards else ""))

    # v0의 합격선. 판정했다와 위반이 없다를 갈라 적는다
    clean = code["ledger"]["pass_bar"]
    check("기계 판정이 깨끗한 iteration을 집어낸다",
          clean["clean_iterations"] == ["002"] and "001" in clean["blocked"],
          f"깨끗한 iteration={clean['clean_iterations']}, 걸린 것={list(clean['blocked'])}")

    bar = unjudged_run["ledger"]["pass_bar"]
    first = unjudged_run["ledger"]["iterations"][0]["machine_verdict"]
    second = unjudged_run["ledger"]["iterations"][1]["machine_verdict"]
    check("판정하지 못한 것을 위반 0건과 같은 칸에 넣지 않는다",
          not bar["clean_iterations"]
          and first["judged"] is False and first["no_violation"] is False
          and any("판정되지 않았다" in gap for gap in first["judged_gaps"])
          and any("위반 0건이라고 말할 수 없다" in gap for gap in first["violation_gaps"]),
          f"위반 목록은 비었는데 judged={first['judged']} no_violation={first['no_violation']}; "
          + "; ".join(first["judged_gaps"][:2] + first["violation_gaps"][:1]))
    check("재지 못한 축이 있으면 판정했다가 아니다",
          second["judged"] is False and second["no_violation"] is True
          and any("재지 못한 축이 있다" in gap for gap in second["judged_gaps"]),
          f"judged={second['judged']} no_violation={second['no_violation']} "
          + "; ".join(second["judged_gaps"]))

    u_report = (unjudged_run["run_dir"] / REPORT_NAME).read_text(encoding="utf-8")
    check("리포트 첫 줄이 깨끗한 iteration이 있었는지 말한다",
          "기계 판정이 깨끗한 iteration은 없" in u_report
          and "| 001 | REJECT | 아니다 | 아니다 |" in u_report
          and "합격선을 넘지 못한 자리마다 무엇에 걸렸는지 적는다" in u_report,
          "; ".join(line for line in u_report.splitlines()
                    if "깨끗한 iteration" in line)[:220])
    c_bar_report = (code["run_dir"] / REPORT_NAME).read_text(encoding="utf-8")
    check("깨끗한 run은 그 사실을 첫 줄에 적는다",
          "기계 판정이 깨끗한 iteration이 있다: 002" in c_bar_report
          and "v0의 합격선은 최종 판정 PASS가 아니라 이것이다" in c_bar_report,
          "; ".join(line for line in c_bar_report.splitlines() if "깨끗한 iteration" in line)[:200])

    # 라벨 어휘가 리포트의 순서표와 맞는가. 맞지 않으면 새 라벨이 조용히 바닥으로 간다
    import report as report_module
    vocab_yaml = {str(c.get("id")) for c in (ledger_module._load_rules().get("labels") or []) if c.get("id")}
    gate_vocab = set(getattr(g1, "LABELS", ())) if g1 else set()
    ordered = set(report_module.LABEL_ORDER)
    only_vocab = sorted((vocab_yaml | gate_vocab) - ordered)
    only_report = sorted(ordered - (vocab_yaml | gate_vocab))
    check("라벨 어휘와 리포트의 순서표가 맞는다",
          not only_vocab and not only_report and vocab_yaml == gate_vocab,
          f"카드 {len(vocab_yaml)}개, g1.LABELS {len(gate_vocab)}개, 순서표 {len(ordered)}개"
          + (f"; 순서표에 없는 라벨 {only_vocab}" if only_vocab else "")
          + (f"; 어휘에 없는 라벨 {only_report}" if only_report else "")
          + (f"; 카드와 g1이 어긋난다 {sorted(vocab_yaml ^ gate_vocab)}" if vocab_yaml != gate_vocab else ""))
    check("모르는 라벨은 바닥이 아니라 맨 위로 간다",
          report_module.label_rank("무엇인지_모르는_라벨") < report_module.label_rank("harmless_to_client")
          and report_module.label_rank("accepts_what_contract_rejects")
              < report_module.label_rank("undeclared_surface")
              < report_module.label_rank("harmless_to_client"),
          f"모르는 라벨={report_module.label_rank('무엇인지_모르는_라벨')}, "
          f"accepts={report_module.label_rank('accepts_what_contract_rejects')}, "
          f"undeclared={report_module.label_rank('undeclared_surface')}, "
          f"harmless={report_module.label_rank('harmless_to_client')}")

    # 새 라벨이 붙은 위반이 무해한 것 아래로 가지 않는다
    # 지점 단위 표만 본다. 2절의 "계약으로 닫힌 위반" 표도 `| v_`로 시작하므로 잘라서 센다.
    tail = c_report.split("판정은 지점 단위로 한다", 1)[-1]
    rows = [line for line in tail.splitlines() if line.startswith("| v_")]
    accepts_row = next((i for i, line in enumerate(rows) if "accepts_what_contract_rejects" in line), None)
    check("새 라벨이 붙은 위반이 라벨 순서대로 온다",
          accepts_row is not None
          and any("breaks_reader" in line for line in rows[:accepts_row]),
          f"지점 표에서 accepts_what_contract_rejects의 자리 {accepts_row}, 그 위의 라벨 "
          + "; ".join(line.split("|")[5].strip() for line in rows[:max(accepts_row or 0, 1)])
          + f" (모두 {len(rows)}줄)")

    # 「좁혔다」 가족이 기록으로 흐르고 가족 이름이 카드에서 온다
    narrowed = next((o for o in c_ledger["observations"] if o["kind"] == "gate:response.value_narrowed"), None)
    check("좁혔다 가족이 기록으로 흐른다",
          narrowed is not None and narrowed["family"] == "narrowed"
          and narrowed["label"] == "harmless_to_client" and narrowed["label_meaning"]
          and not any(v["rule"] == "response.value_narrowed" for v in c_violations),
          f"family={narrowed['family'] if narrowed else None} label={narrowed['label'] if narrowed else None}, "
          "위반 목록에는 없다" if narrowed else "기록이 없다")
    check("리포트가 무해한 라벨을 위반이 아니라 기록으로 설명한다",
          "위반에 붙는 라벨만 여기 온다" in c_report
          and "기록에 붙은 라벨의 뜻이다" in c_report
          and "| gate:response.value_narrowed | narrowed | harmless_to_client |" in c_report,
          "; ".join(line for line in c_report.splitlines()
                    if "위반에 붙는 라벨만" in line or "기록에 붙은 라벨" in line)[:200])

    # 겹친다고 말하려면 같은 좌표를 가리켜야 한다
    c_dis = c_ledger["disagreements"]
    check("좌표가 만나지 않으면 겹침으로 세지 않는다",
          not c_dis["machine_checkable_restatement"],
          f"관찰 규칙 1종에 약점 {len(FALSE_OVERLAP_CRITIQUE['weaknesses'])}개인 run에서 "
          f"겹침 {len(c_dis['machine_checkable_restatement'])}건")
    check("겹치지 않았다고 리포트가 적는다",
          "게이트가 세는 자리를 비평이 다시 말한 곳은 없다" in c_report
          and "같은 좌표를 가리키지 않으면 겹침이 아니므로" in c_report,
          "; ".join(line for line in c_report.splitlines() if "다시 말한 곳은 없다" in line)[:200])

    # 진짜 겹침은 그대로 잡고 겹친 좌표를 적는다
    real = contract["ledger"]["disagreements"]["machine_checkable_restatement"]
    check("같은 좌표를 가리키면 겹침으로 세고 그 좌표를 적는다",
          len(real) == 1 and real[0]["shared"] == "Alert.lastCheckedAt"
          and real[0]["rule"] == "response.field_missing",
          f"{real[0]['critique_id']} → {real[0]['rule']} @ {real[0]['shared']}" if real else "겹침이 없다")

    # 축 판정의 어긋남에 방향이 붙는다
    axis_001 = [a for a in c_dis["axis_disagreement"] if a["iteration"] == "001"]
    axis_002 = [a for a in c_dis["axis_disagreement"] if a["iteration"] == "002"]
    three = [a for a in axis_002 if a["three_way"]]
    check("축 판정의 어긋남에 방향이 붙는다",
          all(a["direction"] == "Critique가 더 엄하다" for a in axis_001 + axis_002)
          and all(a["gate_verdict"] == "REJECT" and not a["three_way"] for a in axis_001)
          and len(three) == 1 and three[0]["axis"] == "request_tolerance" and three[0]["eval_score"] == 5.0,
          f"001 {[(a['axis'], a['gate_verdict']) for a in axis_001]}; "
          f"002 셋이 갈린 자리 {[(a['axis'], a['eval_score'], a['gate_verdict']) for a in three]}")
    check("리포트가 셋이 갈린 자리를 적는다",
          "게이트는 위반을 찾지 못했고 Eval은 5.0를 줬는데 Critique만 high로 지적했다. 셋이 갈렸다" in c_report
          and "누가 더 엄한지가 읽는 사람에게 필요하다" in c_report,
          "; ".join(line for line in c_report.splitlines() if "셋이 갈렸다" in line)[:220])

    # 5. 리포트의 여섯 절
    for name, bundle in (("code", code), ("contract", contract), ("recurrence", recurrence),
                         ("drift", drift), ("nocommit", nocommit), ("unresolved", unresolved),
                         ("cause", cause), ("unjudged", unjudged_run)):
        text = (bundle["run_dir"] / REPORT_NAME).read_text(encoding="utf-8")
        headings = [line[3:].strip() for line in text.splitlines() if line.startswith("## ")]
        check(f"REPORT.md 여섯 절 ({name})", headings == SECTION_TITLES, " / ".join(headings))

    print("확인 결과")
    for ok, line in results:
        print(f"  [{'OK' if ok else 'FAIL'}] {line}")
    failed = [line for ok, line in results if not ok]
    print(f"\n{len(results) - len(failed)}/{len(results)} 통과")

    if args.print_report != "none":
        bundle = {"code": code, "contract": contract, "recurrence": recurrence,
                  "drift": drift, "nocommit": nocommit, "unresolved": unresolved,
                  "cause": cause, "unjudged": unjudged_run}[args.print_report]
        path = bundle["run_dir"] / REPORT_NAME
        print(f"\n===== {path} =====")
        print(path.read_text(encoding="utf-8"))

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
