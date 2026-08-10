# Original User Input

실패인데 critique의 문제인지 gen의 문제인지 확인을 통해 수정해야할 문제로 결정하고 다음으로 넘어가자


# Checked Context

# 결정 대상

- `Ticket.of()` 과설계 실패의 책임을 Critique 문제, Gen 문제, 파이프라인 전달 계약 문제로 구분한다.
- 구현 수정은 아직 하지 않고, 수정해야 할 문제의 소유권과 우선순위만 확정한다.

# 확인된 계약과 위반

1. `pipeline/prompts/critique_refactor.md`
   - "실제 코드에서 확인한다 — 추측 아님"
   - "실제 refactored_code를 근거로 지적한다. '그럴 수 있다'가 아니라 '여기 이 코드가'"
   - 1차 Critique는 입력에 없는 persistence 구현이 setter로 DB 상태를 복원할 가능성을 high behavior_risk로 제기했다. 따라서 Critique가 자신의 명시적 계약을 위반한 것이 최초 원인이다.
2. `pipeline/prompts/diagnose_refactor.md`
   - "진단 근거는 code에서만. 지어내지 마라."
   - refine에서는 이전 제안의 타당한 부분을 유지하고 지적된 것만 고치라고 한다.
   - 2차 Diagnose는 Critique 지적이 code로 확인됐는지 다시 검증하지 않고 `Ticket.of()` 제안으로 확정했다. 따라서 Gen의 방어 실패도 있다.
3. `pipeline/schemas/critique_output.schema.json`
   - weakness에는 severity, axis, where, suggestion만 있다.
   - evidence anchor, confirmed/hypothesis 상태, 추가 문맥 필요 여부를 표현할 필드가 없다.
4. `pipeline/runner.py`
   - Critique weaknesses를 그대로 `REVISION_FEEDBACK`으로 Diagnose에 전달한다.
   - 확인된 결함과 확인되지 않은 위험 가설을 구분하는 validation/gate가 없다.
5. Implement
   - Diagnose의 GO/REMOVE proposal을 충실히 구현하는 역할이다. 잘못된 proposal의 근거를 재판정할 책임이 없으므로 이번 원인의 소유자가 아니다.

# 판정

- 최초 결함 발원: Critique.
- 2차 방어 실패: Gen/Diagnose refine.
- 재발을 막을 근본 수정 위치: Critique→Gen handoff contract와 validator.
- 하나만 우선 고르면 Critique 문제로 분류한다. 하지만 Critique prompt에 이미 추측 금지 규칙이 있으므로 문구 추가만으로는 부족하다.

# 일반화된 수정 방향

- 공용 규칙을 특정 도메인 사례에 anchor하지 않는다.
- Critique의 확인된 weakness와 증거가 부족한 risk hypothesis를 별도 출력으로 분리한다.
- weakness에는 입력에서 추적 가능한 evidence anchor를 요구한다.
- Diagnose는 REVISION_FEEDBACK 자체를 evidence로 취급하지 않고, code에서 재확인된 항목만 GO/REMOVE로 승격한다.
- 확인할 수 없는 위험은 코드 생성으로 넘기지 않고 DEFER 또는 slow-loop/context expansion으로 보낸다.
