# Original User Input

collector는 언제 돌아가는거야? 여러번 돌아도 하나의 파일로 만드는거야? 
axis 축과, critique의 문제점도 수집하는거야?
병합된건을 감지하는걸 고려한거야?

proposal은 자동으로 루프를 도는거야? 아니면 manual하게 도는거야?

# Checked Context

- .codex/skills/thinking-review-gate/SKILL.md: Level 2 artifact는 runs/{run_id}/input.md와 attempts/{n}/draft.md, critique.md, eval.json, validation.json을 남긴다. Original User Input은 사용자 원문 그대로 저장해야 한다.
- .codex/skills/thinking-review-gate/scripts/validate.py: validation.json에는 scores, score_reasons, weak_axes, weighted_score, gate_result가 들어간다.
- .codex/skills/thinking-review-gate/scripts/critique.py: run 생성, attempt 추가, validate, cleanup은 있지만 collector/proposal 구현은 아직 없다.
- 현재 설계 질문은 collector 실행 시점, report 산출 방식, 수집 대상, 병합 감지, proposal 실행 방식을 정하는 것이다.
