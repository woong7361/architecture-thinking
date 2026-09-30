from __future__ import annotations

import argparse
import json
import sys
import tempfile
from pathlib import Path

sys.dont_write_bytecode = True

from runner import (
    AGENT_CRITIQUE,
    AGENT_EVAL,
    AGENT_GEN,
    AGENT_REFINE,
    EVAL_MODEL_SCHEMA,
    GEN_PROMPT_PATH,
    PROJECT_DIR,
    PROVIDER_CLAUDE,
    PROVIDER_CODEX,
    RUBRIC_PATH,
    STATUS_ERROR,
    STATUS_PASS,
    STATUS_REJECT,
    STATUS_SKIPPED,
    ProgressReporter,
    RunContext,
    add_common_arguments,
    build_critique,
    build_draft,
    build_eval,
    build_refine_request,
    copy_input,
    display_model,
    ensure_pass,
    gate_verdict,
    changed_paths,
    collect_code_files,
    commit_iteration,
    lock_contract_versions,
    detect_generator_commits,
    head_commit,
    import_pipeline_module,
    init_output,
    compute_decision_risk,
    is_unmeasured_threshold_error,
    load_gate_rules,
    load_json,
    load_ledger_writer,
    draft_prose_slots,
    load_prose_slots,
    load_report_writer,
    relative_to_run,
    normalize_and_log,
    now_iso,
    load_rubric_optional,
    prepare_contract_copies,
    raise_on_gate_error,
    resolve_agent_models,
    tag_pass,
    rubric_axes,
    run_gate_stage,
    unmeasured_axes,
    verify_contract_hashes,
    write_json,
)
from stages.critique import critique
from stages.evaluator import evaluate
from stages.generator import generate
from stages.refine import refine
from stages.scripts.claude_client import _parse_json
from stages.scripts.llm_client import SANDBOX_READ_ONLY, LLMClient, create_client
from validate import check_schema_pairs, validate_file, write_result


STAGE_GEN = "gen"
STAGE_GATE = "gate"
STAGE_CRITIQUE = "critique"
STAGE_EVAL = "eval"
STAGE_REFINE = "refine"
# 산출물만 읽어 원장과 리포트를 만드는 단계. LLM을 부르지 않는다.
STAGE_RECORD = "record"
STAGES = [STAGE_GEN, STAGE_GATE, STAGE_CRITIQUE, STAGE_EVAL, STAGE_REFINE, STAGE_RECORD]

# 루브릭이 필요한 단계. critique는 축의 이름과 뜻을, eval은 스케일과 하한을 받아야 한다.
# refine은 루브릭을 보지 않고 runner가 계산한 약한 축만 받으므로 없어도 돌아간다.
STAGES_NEEDING_RUBRIC = {STAGE_CRITIQUE, STAGE_EVAL, STAGE_RECORD}


def prepare_run_dir(
    context: RunContext,
    input_path: Path,
    input_data: dict,
    origin: list[Path],
    skeleton_dir: Path,
    progress: ProgressReporter,
) -> dict:
    """run 폴더를 단계 하나만 돌릴 수 있는 상태로 만든다. 이미 있으면 그대로 쓴다."""
    context.run_dir.mkdir(parents=True, exist_ok=True)
    context.iter_dir.mkdir(parents=True, exist_ok=True)
    copy_input(input_path, context.copied_input_path)
    verify_contract_hashes(input_data, progress)
    lineage = prepare_contract_copies(context, input_data, origin)
    init_output(context, input_data, skeleton_dir, progress)
    return lineage


# 원문을 다시 흘릴 때 기록에 남는 모델 이름. 원장을 읽는 사람이 새 호출이 아님을 알아야 한다.
REPLAY_MODEL = "raw-replay"


class RawReplayClient(LLMClient):
    """저장된 응답 원문을 그 단계의 산출로 흘린다. LLM을 부르지 않는다.

    진단 도구가 저장한 것을 스스로 읽을 수 없으면 저장의 값이 반쯤 버려진다. 모델이 실제로 낸 응답이
    손에 있는데 다시 부르는 것은 돈만 드는 것이 아니라 정직하지도 않다 — 두 번째 호출은 다른 응답이고,
    첫 번째가 문턱을 넘었는지 확인하는 일과 다른 일이 된다.
    """

    def __init__(self, raw: Path) -> None:
        self.raw = Path(raw)

    def run_prompt(self, system, user, output_schema, output_path, model=None, work_dir=None,
                   sandbox=SANDBOX_READ_ONLY, raw_path=None, on_note=None):
        text = self.raw.read_bytes().decode("utf-8", errors="replace")
        if on_note is not None:
            on_note(f"원문 재생: {self.raw} ({len(text)}자). LLM을 부르지 않았다.")
        data, repairs = _parse_json(text)
        for repair in repairs:
            if on_note is not None:
                on_note(f"응답의 문법 흠을 고쳐 통과시켰다: {repair} (원문은 그대로 남아 있다)")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        return None


def runner_first_commit(context) -> str:
    """작업 저장소의 루트 커밋. 스켈레톤과 기준선의 스냅샷이고 생성 코드 diff의 기준이다."""
    from runner import git

    return git(["rev-list", "--max-parents=0", "HEAD"], context.output_dir).strip().splitlines()[0]


def code_payload(context, draft: dict, progress) -> dict:
    """비평·채점에 실어 보낼 코드. 읽기를 모델의 도구 성공에 맡기지 않는다."""
    payload = collect_code_files(
        output_dir=context.output_dir,
        first_commit=runner_first_commit(context),
        index_paths=[entry["path"] for entry in draft.get("files", [])],
    )
    progress.line(
        f"code payload files={len(payload['files'])} bytes={payload['total_bytes']} "
        f"omitted={len(payload['omitted'])}"
    )
    return payload


def baseline_contract_path(context: RunContext, input_data: dict) -> Path:
    first = input_data["brief"]["contracts"][0]["path"]
    return context.baseline_dir / Path(first).name


def require_file(path: Path, stage: str, what: str) -> Path:
    if not path.exists():
        raise FileNotFoundError(
            f"--stage {stage} needs {what} but it is missing: {path}\n"
            "앞 단계를 먼저 돌려라."
        )
    return path


def stage_gen(context, args, input_data, client, models, prose_slots, progress) -> Path:
    head_before = head_commit(context.output_dir)
    with tempfile.TemporaryDirectory(prefix="contract-impl-gen-") as temp_dir:
        temp_output = Path(temp_dir) / "gen-output.json"
        with progress.step(f"gen model={display_model(models[AGENT_GEN], args.provider)}", live=True):
            token_usage = generate(
                input_path=context.copied_input_path,
                output_path=temp_output,
                client=client,
                model=models[AGENT_GEN],
                gen_prompt_path=GEN_PROMPT_PATH,
                prose_slots=prose_slots,
                work_dir=context.output_dir,
                raw_path=context.iter_dir / "gen.raw.txt",
                on_note=progress.line,
            )
        normalize_and_log(temp_output, "gen_output", "gen", progress, work_dir=context.output_dir)
        result = validate_file(
            temp_output,
            artifact="gen_output",
            immutable_paths=input_data["brief"]["implementation_contract"]["immutable_paths"],
            work_dir=context.output_dir,
            changed_paths=changed_paths(context.output_dir),
        )
        progress.validation("gen_output_validate", result)
        ensure_pass(result)
        gen_output = load_json(temp_output)

    for hash_value in detect_generator_commits(context.output_dir, head_before):
        progress.line(f"generator_commit {hash_value[:10]} — 커밋은 runner의 일이다")
    commit = commit_iteration(context.output_dir, context.iteration, AGENT_GEN)
    progress.line(f"commit={commit[:10]}")
    draft = build_draft(
        input_data=input_data,
        stage_output=gen_output,
        iteration=context.iteration,
        model_name=display_model(models[AGENT_GEN], args.provider),
        token_usage=token_usage,
        source_stage=AGENT_GEN,
        commit=commit,
    )
    write_json(context.draft_path, draft, overwrite=args.overwrite)
    lock_contract_versions(context.output_dir)
    return context.draft_path


def stage_gate(context, args, input_data, progress) -> Path:
    require_file(context.draft_path, STAGE_GATE, "the draft of this iteration")
    draft = load_json(context.draft_path)
    draft_result = validate_file(
        context.draft_path,
        artifact="draft",
        expected_brief_hash=context.brief_hash,
        expected_iteration=context.iteration,
        immutable_paths=input_data["brief"]["implementation_contract"]["immutable_paths"],
        work_dir=context.output_dir,
    )
    progress.validation("draft_validate", draft_result)
    ensure_pass(draft_result, context.draft_validation_path)

    gate_result = run_gate_stage(
        context=context,
        draft=draft,
        input_data=input_data,
        rules=load_gate_rules(args.rules, args.allow_missing_gates, progress),
        java_home=args.java_home,
        allow_missing_gates=args.allow_missing_gates,
        progress=progress,
    )
    write_json(context.gate_path, gate_result, overwrite=True)
    raise_on_gate_error(gate_result, context.gate_path)
    progress.line(
        f"gate {gate_verdict(gate_result)} g0={gate_result['g0']} g1={gate_result['g1']} "
        f"changes={gate_result['changes']} violations={len(gate_result['violations'])} "
        f"observations={len(gate_result['observations'])}"
    )
    return context.gate_path


def stage_critique(context, args, input_data, client, models, rubric, prose_slots, progress) -> Path:
    require_file(context.draft_path, STAGE_CRITIQUE, "the draft of this iteration")
    draft = load_json(context.draft_path)
    observations = []
    if context.gate_path.exists():
        observations = load_json(context.gate_path).get("observations", [])
    else:
        progress.line(f"gate observations {STATUS_SKIPPED} reason=no_gate_artifact")

    with tempfile.TemporaryDirectory(prefix="contract-impl-critique-") as temp_dir:
        temp_output = Path(temp_dir) / "critique.json"
        with progress.step(f"critique model={display_model(models[AGENT_CRITIQUE], args.provider)}", live=True):
            token_usage = critique(
                input_path=context.copied_input_path,
                draft_path=context.draft_path,
                output_path=temp_output,
                client=client,
                model=models[AGENT_CRITIQUE],
                prose_slots=prose_slots,
                decisions=draft.get("decisions", []),
                contract_changes=draft.get("contract_changes", []),
                observations=observations,
                axes=rubric_axes(rubric),
                work_dir=context.output_dir,
                code_files=code_payload(context, draft, progress),
                raw_path=context.iter_dir / "critique.raw.txt",
                on_note=progress.line,
            )
        normalize_and_log(temp_output, "critique_output", "critique", progress)
        result = validate_file(temp_output, artifact="critique_output")
        progress.validation("critique_output_validate", result)
        ensure_pass(result)
        critique_output = load_json(temp_output)

    artifact = build_critique(
        critique_output=critique_output,
        iteration=context.iteration,
        model_name=display_model(models[AGENT_CRITIQUE], args.provider),
        token_usage=token_usage,
    )
    write_json(context.critique_path, artifact, overwrite=args.overwrite)
    critique_result = validate_file(
        context.critique_path,
        artifact="critique",
        expected_brief_hash=context.brief_hash,
        expected_iteration=context.iteration,
        rubric=rubric,
    )
    progress.validation("critique_validate", critique_result)
    ensure_pass(critique_result, context.critique_validation_path)
    return context.critique_path


def stage_eval(context, args, input_data, client, models, rubric, prose_slots, progress) -> Path:
    require_file(context.draft_path, STAGE_EVAL, "the draft of this iteration")
    draft = load_json(context.draft_path)
    with tempfile.TemporaryDirectory(prefix="contract-impl-eval-") as temp_dir:
        temp_output = Path(temp_dir) / "eval.json"
        with progress.step(f"eval model={display_model(models[AGENT_EVAL], args.provider)}", live=True):
            token_usage = evaluate(
                input_path=context.copied_input_path,
                draft_path=context.draft_path,
                rubric=rubric,
                output_path=temp_output,
                client=client,
                model=models[AGENT_EVAL],
                eval_output_schema=EVAL_MODEL_SCHEMA,
                prose_slots=prose_slots,
                decisions=draft.get("decisions", []),
                contract_changes=draft.get("contract_changes", []),
                work_dir=context.output_dir,
                code_files=code_payload(context, draft, progress),
                raw_path=context.iter_dir / "eval.raw.txt",
                on_note=progress.line,
            )
        normalize_and_log(temp_output, "eval_output", "eval", progress)
        result = validate_file(temp_output, artifact="eval_output")
        progress.validation("eval_output_validate", result)
        ensure_pass(result)
        eval_output = load_json(temp_output)

    artifact = build_eval(
        eval_output=eval_output,
        iteration=context.iteration,
        model_name=display_model(models[AGENT_EVAL], args.provider),
        token_usage=token_usage,
    )
    write_json(context.eval_path, artifact, overwrite=args.overwrite)
    eval_result = validate_file(
        context.eval_path,
        artifact="eval",
        expected_brief_hash=context.brief_hash,
        expected_iteration=context.iteration,
        rubric=rubric,
    )
    unmeasured = unmeasured_axes(artifact, rubric)
    if unmeasured:
        progress.line(f"eval 측정 불가 {unmeasured} — 이 채점은 판정에 쓰이지 않는다")
    if eval_result["status"] != STATUS_PASS:
        write_result(eval_result, context.eval_validation_path)
        progress.validation("eval_validate", eval_result)
    return context.eval_path


def stage_refine(context, args, input_data, client, models, rubric, progress) -> Path:
    require_file(context.draft_path, STAGE_REFINE, "the draft of this iteration")
    head_before = head_commit(context.output_dir)
    draft = load_json(context.draft_path)
    gate_result = load_json(context.gate_path) if context.gate_path.exists() else {
        "g0": STATUS_SKIPPED,
        "g1": STATUS_SKIPPED,
        "changes": STATUS_SKIPPED,
        "violations": [],
        "observations": [],
    }
    critique_artifact = load_json(context.critique_path) if context.critique_path.exists() else None
    eval_artifact = load_json(context.eval_path) if context.eval_path.exists() else None

    to_iteration = f"{int(context.iteration) + 1:03d}"
    refine_request = build_refine_request(
        input_data=input_data,
        draft=draft,
        critique_artifact=critique_artifact,
        eval_artifact=eval_artifact,
        gate_result=gate_result,
        rubric=rubric,
        rules=load_gate_rules(args.rules, True, progress),
        to_iteration=to_iteration,
    )
    write_json(context.refine_request_path, refine_request, overwrite=True)

    next_context = context.with_iteration(to_iteration)
    next_context.iter_dir.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="contract-impl-refine-") as temp_dir:
        temp_output = Path(temp_dir) / "refine-output.json"
        with progress.step(
            f"refine {context.iteration}->{to_iteration} model={display_model(models[AGENT_REFINE], args.provider)}",
            live=True,
        ):
            token_usage = refine(
                input_path=context.copied_input_path,
                draft_path=context.draft_path,
                critique_path=context.critique_path if critique_artifact is not None else None,
                refine_request=refine_request,
                output_path=temp_output,
                client=client,
                model=models[AGENT_REFINE],
                work_dir=context.output_dir,
                raw_path=context.iter_dir / "refine.raw.txt",
                on_note=progress.line,
            )
        normalize_and_log(temp_output, "refine_output", "refine", progress, work_dir=context.output_dir)
        result = validate_file(
            temp_output,
            artifact="refine_output",
            immutable_paths=input_data["brief"]["implementation_contract"]["immutable_paths"],
            work_dir=context.output_dir,
            changed_paths=changed_paths(context.output_dir),
        )
        progress.validation("refine_output_validate", result)
        ensure_pass(result)
        refine_output = load_json(temp_output)

    for hash_value in detect_generator_commits(context.output_dir, head_before):
        progress.line(f"generator_commit {hash_value[:10]} — 커밋은 runner의 일이다")
    refined_commit = commit_iteration(context.output_dir, to_iteration, AGENT_REFINE)
    progress.line(f"commit={refined_commit[:10]}")
    refined = build_draft(
        input_data=input_data,
        stage_output=refine_output,
        iteration=to_iteration,
        model_name=display_model(models[AGENT_REFINE], args.provider),
        token_usage=token_usage,
        source_stage=AGENT_REFINE,
        commit=refined_commit,
    )
    write_json(next_context.draft_path, refined, overwrite=args.overwrite)
    lock_contract_versions(context.output_dir)
    return next_context.draft_path


def stage_record(context, args, input_data, rubric, progress) -> Path:
    """run 폴더에 있는 iteration 산출물로 원장과 리포트를 만든다. LLM을 부르지 않는다.

    단계별로 되살린 run에는 원장과 리포트가 없다. 그런데 그 리포트가 이 하네스의 제출물이므로, 없으면
    여기까지 온 것이 파일 몇 개로만 남는다. 그래서 디스크에 있는 것만 읽어 다시 만든다.

    **없는 것을 꾸미지 않는다.** `eval.json`이 없으면 `eval`을 `None`으로 넘겨 원장이 "쟀는지 말할 수 없다"로
    적게 한다. 빠진 것을 빈 값으로 채우면 리포트가 거짓말을 한다. 돌리지 않은 iteration은 아예 넣지 않는다 —
    돌리지 않은 것과 실패한 것은 다르다.
    """
    update_ledger = load_ledger_writer()
    write_report = load_report_writer()
    if update_ledger is None and write_report is None:
        raise FileNotFoundError("ledger.py와 report.py가 없다. 원장과 리포트를 만들 수 없다.")

    # `record`는 정의상 디스크의 산출물에서 원장을 **처음부터** 만드는 일이다. 원장을 갱신하는 쪽은 그것을
    # 입력으로 읽어 이어 쓰므로, 남겨 두면 다시 만드는 것이 아니라 두 번 세는 것이 된다. 판정 장치를 고친 뒤
    # 옛 run의 리포트를 다시 내는 길도 거기서 막힌다 — 옛 원장에 옛 판정이 들어 있다.
    ledger_path = context.run_dir / ledger_file_name()
    if ledger_path.exists():
        if not args.overwrite:
            raise FileExistsError(
                f"원장이 이미 있다: {ledger_path}\n"
                "record는 원장을 처음부터 만든다. 덮어쓸 뜻이면 --overwrite를 붙여라. "
                "조용히 이어 쓰면 두 번 센 리포트가 나오고 읽는 사람은 그것을 알 수 없다."
            )
        ledger_path.unlink()
        progress.line(f"기존 원장을 버렸다: {ledger_path.name} (record는 처음부터 만든다)")

    immutable = input_data["brief"]["implementation_contract"]["immutable_paths"]
    ledger: dict | None = None
    payloads: list[dict] = []

    for iter_dir in sorted(path for path in context.run_dir.glob("iter_*") if path.is_dir()):
        iteration = iter_dir.name.removeprefix("iter_")
        at = context.with_iteration(iteration)
        if not at.draft_path.exists():
            progress.line(f"iter {iteration} 건너뜀 — draft.json이 없다(돌리지 않은 iteration)")
            continue

        draft = load_json(at.draft_path)
        gate = load_json(at.gate_path) if at.gate_path.exists() else {}
        critique_artifact = load_json(at.critique_path) if at.critique_path.exists() else None
        eval_artifact = load_json(at.eval_path) if at.eval_path.exists() else None

        unmeasured = unmeasured_axes(eval_artifact or {}, rubric)
        rubric_errors: list[str] = []
        if eval_artifact is not None:
            result = validate_file(
                at.eval_path,
                artifact="eval",
                expected_brief_hash=context.brief_hash,
                expected_iteration=iteration,
                rubric=rubric,
            )
            rubric_errors = [
                error
                for error in result.get("errors", [])
                if not is_unmeasured_threshold_error(str(error), unmeasured)
            ]
        gate_errors = [
            f"gate:{v.get('rule')}@{v.get('point')}: {v.get('detail', '')}"
            for v in gate.get("violations", [])
            if isinstance(v, dict)
        ]
        # 채점이 판정에 쓰일 수 있는가. 산출이 없거나 측정 불가가 있으면 아니다.
        eval_judges = eval_artifact is not None and not unmeasured
        passed = gate_verdict(gate) == STATUS_PASS and not rubric_errors and eval_judges

        failures: list[dict] = []
        for name in ("gen", "critique", "eval", "refine"):
            marker = iter_dir / f"{name}.transport-failure.json"
            if marker.exists():
                failures.append(load_json(marker))

        payload = {
            "iteration": iteration,
            "status": STATUS_PASS if passed else STATUS_REJECT,
            "draft": relative_to_run(at.draft_path, context.run_dir),
            "gate": gate,
            "critique": critique_artifact,
            "eval": eval_artifact,
            "decision_risk": compute_decision_risk(
                draft.get("decisions", []), draft.get("contract_changes", [])
            ),
            "build_attempts": gate.get("build_attempts", []),
            "normalizations": [],
            "transport_failures": failures,
            "eval_judges": eval_judges,
            "unmeasured_axes": unmeasured,
            "commit": draft.get("commit"),
            "output": relative_to_run(context.output_dir, context.run_dir),
            "generator_commits": [],
            "gate_errors": gate_errors,
            "rubric_errors": rubric_errors,
            "skipped_reason": None if eval_artifact is not None else "eval_missing",
            # 이 산출은 한 번의 run에서 연속으로 나온 것이 아니다. 원장을 읽는 사람이 알아야 한다.
            "replayed": True,
            "recorded_by": "run_stage --stage record",
        }
        payloads.append(payload)
        progress.line(
            f"iter {iteration} 기록 — gate={gate_verdict(gate)} eval={'있다' if eval_artifact else '없다'} "
            f"unmeasured={len(unmeasured)} status={payload['status']}"
        )
        if update_ledger is not None:
            ledger = update_ledger(run_dir=context.run_dir, iteration=iteration, payload=payload)

    if not payloads:
        raise FileNotFoundError(f"기록할 iteration이 없다: {context.run_dir}")

    result = {**payloads[-1], "iteration": payloads[-1]["iteration"]}
    if write_report is not None:
        write_report(run_dir=context.run_dir, ledger=ledger or {}, result=result)

    # 원장의 형식은 다른 조각이 소유하므로 모르는 키를 싣지 않는다. 그래서 되살린 사실은 이 자리에 적는다.
    # 이 산출이 한 번의 run에서 연속으로 나온 것이 아님을 원장을 읽는 사람이 알아야 한다.
    write_json(
        context.run_dir / "record.json",
        {
            "recorded_at": now_iso(),
            "recorded_by": "run_stage --stage record",
            "continuous_run": False,
            "note": (
                "이 원장과 리포트는 한 번의 run이 끝나며 나온 것이 아니다. run 폴더에 남은 iteration 산출물을 "
                "읽어 다시 만들었고 그 과정에서 LLM을 부르지 않았다. 산출물이 없는 iteration은 넣지 않았다 — "
                "돌리지 않은 것과 실패한 것은 다르다."
            ),
            "iterations": [
                {
                    "iteration": payload["iteration"],
                    "status": payload["status"],
                    "eval_present": payload["eval"] is not None,
                    "critique_present": payload["critique"] is not None,
                    "unmeasured_axes": payload["unmeasured_axes"],
                    "stage_models": stage_models_of(context, payload["iteration"]),
                }
                for payload in payloads
            ],
        },
        overwrite=True,
    )
    progress.line(
        f"iteration {len(payloads)}개를 기록했다 — record.json에 되살린 run이라는 사실을 남겼다 "
        f"(immutable_paths {len(immutable)}개)"
    )
    return context.run_dir / "REPORT.md"


def ledger_file_name() -> str:
    """원장 파일 이름. 이름은 원장을 소유한 조각이 정하므로 그쪽에서 읽고, 없으면 관례를 쓴다."""
    module, _ = import_pipeline_module("ledger")
    return str(getattr(module, "LEDGER_NAME", "run_ledger.json")) if module is not None else "run_ledger.json"


def stage_models_of(context: RunContext, iteration: str) -> dict[str, str]:
    """그 iteration의 산출물이 어떤 모델에서 나왔는지. `raw-replay`면 원문을 다시 흘린 것이다."""
    at = context.with_iteration(iteration)
    models: dict[str, str] = {}
    for name, path in (("draft", at.draft_path), ("critique", at.critique_path), ("eval", at.eval_path)):
        if path.exists():
            try:
                models[name] = str(load_json(path).get("model", ""))
            except (OSError, ValueError):
                continue
    return models


def main() -> int:
    parser = argparse.ArgumentParser(
        description="단계 하나만 돌린다. 앞 단계의 산출물을 run 폴더에서 읽어 쓰고 결과 경로를 stdout에 찍는다."
    )
    parser.add_argument("--stage", required=True, choices=STAGES)
    parser.add_argument("--input", required=True, type=Path, help="input.schema.json을 만족하는 input JSON.")
    parser.add_argument("--run-dir", required=True, type=Path, help="이 run의 폴더. 없으면 만든다.")
    parser.add_argument("--iteration", default="001")
    parser.add_argument(
        "--from-raw",
        type=Path,
        help="저장된 응답 원문을 그 단계의 응답으로 쓴다. LLM을 부르지 않고 파싱·정규화·검증·산출까지 지나간다.",
    )
    add_common_arguments(parser)
    args = parser.parse_args()

    if len(args.iteration) != 3 or not args.iteration.isdigit():
        raise ValueError("--iteration must use a 3-digit value such as 001")

    progress = ProgressReporter()
    input_path = args.input.resolve()
    input_result = validate_file(input_path, artifact="input")
    progress.validation("input_validate", input_result)
    ensure_pass(input_result)
    input_data = load_json(input_path)

    if args.stage != STAGE_GATE:
        pair_result = check_schema_pairs()
        progress.validation("schema_pairs", pair_result)
        ensure_pass(pair_result)

    context = RunContext.at(
        brief_hash=input_data["brief_hash"],
        iteration=args.iteration,
        run_dir=args.run_dir,
    )
    prepare_run_dir(context, input_path, input_data, args.origin or [], args.skeleton_dir, progress)

    rubric = load_rubric_optional((args.rubric or RUBRIC_PATH).resolve())
    if rubric is None and args.stage in STAGES_NEEDING_RUBRIC:
        raise FileNotFoundError(
            f"rubric not found: {args.rubric or RUBRIC_PATH}\n"
            f"--stage {args.stage} 는 축 집합이 필요하다. 루브릭은 다른 조각이 소유한다."
        )

    prose_slots: list[dict] = []
    if args.stage == STAGE_GEN:
        prose_slots, source = load_prose_slots(baseline_contract_path(context, input_data), args.prose_slots)
        progress.line(f"prose_slots count={len(prose_slots)} source={source}")
    elif args.stage in (STAGE_CRITIQUE, STAGE_EVAL):
        # 비평과 채점은 초안이 서 있는 계약 판본으로 본다. 초안이 없으면 각 단계가 그 사실로 멈춘다.
        draft = load_json(context.draft_path) if context.draft_path.exists() else {}
        prose_slots, source = draft_prose_slots(
            context, draft, baseline_contract_path(context, input_data), args.prose_slots
        )
        progress.line(f"prose_slots count={len(prose_slots)} source={source}")

    models = resolve_agent_models(args)
    client = None
    if args.stage not in (STAGE_GATE, STAGE_RECORD):
        if args.from_raw is not None:
            # 원문 재생이므로 모델 이름을 기록에 그렇게 남긴다.
            models = {agent: REPLAY_MODEL for agent in models}
            client = RawReplayClient(args.from_raw)
            progress.line(f"from-raw={args.from_raw} — LLM을 부르지 않고 저장된 원문을 흘린다")
        else:
            client = create_client(
                provider=args.provider,
                project_dir=PROJECT_DIR,
                timeout_seconds=args.timeout_seconds,
                codex_bin=args.codex_bin,
            )

    progress.line(f"stage {args.stage} start run_dir={context.run_dir} iteration={context.iteration}")
    if args.stage == STAGE_GEN:
        output = stage_gen(context, args, input_data, client, models, prose_slots, progress)
    elif args.stage == STAGE_GATE:
        output = stage_gate(context, args, input_data, progress)
    elif args.stage == STAGE_CRITIQUE:
        output = stage_critique(context, args, input_data, client, models, rubric, prose_slots, progress)
    elif args.stage == STAGE_EVAL:
        output = stage_eval(context, args, input_data, client, models, rubric, prose_slots, progress)
    elif args.stage == STAGE_RECORD:
        output = stage_record(context, args, input_data, rubric, progress)
    else:
        output = stage_refine(context, args, input_data, client, models, rubric, progress)

    status = STATUS_PASS
    payload: dict = {}
    if args.stage == STAGE_GATE:
        gate_result = load_json(output)
        status = gate_verdict(gate_result)
        payload = {
            "g0": gate_result["g0"],
            "g1": gate_result["g1"],
            "changes": gate_result["changes"],
            "allow_missing_gates": gate_result["allow_missing_gates"],
            "violations": len(gate_result["violations"]),
            "observations": len(gate_result["observations"]),
        }
    print(
        json.dumps(
            {
                "status": status,
                "stage": args.stage,
                "iteration": context.iteration,
                "run_dir": str(context.run_dir),
                "output": str(output),
                **payload,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0 if status in (STATUS_PASS, STATUS_REJECT) else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
