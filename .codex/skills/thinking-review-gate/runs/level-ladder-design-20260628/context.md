# Project Context

Checked sources:

- `AGENTS.md`: 프로젝트는 AI 작업 환경을 점진적으로 개선하는 맥락이며, 답변은 검증 가능한 형태로 마무리하고 확신 낮은 내용은 표시해야 한다.
- `PROBLEM.md`: `thinking-review-gate`의 verifier output과 hand-off 문제가 open 상태다. 핵심 쟁점은 file hand-off 사용 여부, critique/eval 분리 여부, 점수 노출이 main agent를 anchor할 가능성이다. 현재 임시 결정은 일반 verifier output에는 축별 점수를 노출하지 않고, debugging/review case 평가에서만 별도 형식으로 출력하는 방향이다.
- `ai-answer-accuracy-gate.md`: Level 1/2/3를 별도 skill 버전이 아니라 검토 강도로 두는 설계를 설명한다. Level 1은 기본 inline self-review, Level 2는 설계/추천/의사결정 등에 쓰는 critique review, Level 3는 review case/gate/runner/schema validation 등 재현 가능한 기록이 필요한 경우에만 쓴다고 정리한다. 정확성 보장이 아니라 사용자 검증 가능한 표면을 만드는 것이 목표라고 명시한다.
- `.codex/skills/thinking-review-gate/SKILL.md`: 현재 구현 지침은 모든 적용 답변에 Level 1 inline verifier review를 적용하고, 설계/추천/구현 방향 등은 Level 2, 사용자가 review case/file hand-off/eval/gate/runner/schema validation/재현 가능한 기록을 요청하면 Level 3로 올리도록 한다. Level 2는 sub-agent inline critique, Level 3는 file hand-off artifact와 `validation.json.gate_result` 기준으로 판단한다.
- `.codex/skills/thinking-review-gate/prompts/verifier.md`: Level 2 verifier는 점수와 pass/fail을 출력하지 않고 Markdown critique만 반환한다.
- `.codex/skills/thinking-review-gate/prompts/level3-critique.system.md` and `level3-eval.system.md`: Level 3는 critique와 eval을 분리한다. critique는 수정 제안 artifact, eval은 축별 점수와 이유 JSON만 만든다.
- `.codex/skills/thinking-review-gate/rubric.yaml`: Level 3 gate는 `evidence_count`, `evidence_quality`, `claim_coverage`, `uncertainty_boundary`, `consistency`, `alternatives_tradeoff` 축과 `min_score: 3.6`으로 평가한다.
- `thinking-review-gate-checklist.md`: MVP 체크리스트도 단일 skill 안에서 Level 1/2/3로 검토 강도를 나누는 방향을 전제로 한다. 다만 일부 체크리스트 항목은 현재 구현과 완전히 동기화되지 않은 듯하다.

Relevant constraints:

- 사용자가 규칙 파일 수정을 요청하지 않았으므로 설계 판단만 한다.
- `PROBLEM.md` 변경은 사용자 확인 없이 하지 않는다.
- 이 판단은 현재 로컬 문서 기준이며, 실제 장기 운영 데이터는 아직 충분하지 않을 수 있다.
