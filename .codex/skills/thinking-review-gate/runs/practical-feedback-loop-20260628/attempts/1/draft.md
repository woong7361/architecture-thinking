실전에서 쓰려면 두 단계를 분리해서 추가하는 것이 좋다. 첫 단계는 collector이고, 두 번째 단계는 느린 개선 루프다. collector는 판정 기준을 바꾸지 않고 기록을 모아 관찰 가능하게 만드는 장치다. 느린 개선 루프는 그 관찰 결과를 기준 변경 후보로 바꾸되, 사람이 승인하기 전에는 SKILL.md, rubric.yaml, prompt, validate.py를 직접 바꾸지 않는 장치다.

1단계 collector는 scripts/collect.py 하나로 시작한다. 입력은 .codex/skills/thinking-review-gate/runs 아래의 manifest.json, input.md, attempts/*/validation.json, eval.json, critique.md다. 출력은 reports/summary.json과 reports/trend.md로 둔다. summary.json은 기계가 다시 읽을 수 있는 집계이고, trend.md는 사람이 회고할 수 있는 리포트다. 최소 집계 항목은 전체 run 수, pass/fail 수, 평균 weighted_score, 축별 평균 점수, weak_axes 빈도, fail run 목록, 반복되는 score_reasons 키워드, attempt별 개선 여부다.

실전용 collector의 기본 명령은 다음처럼 둔다. python scripts/collect.py --runs-dir runs --output-dir reports --since-days 14. 기본은 읽기 전용이고 기존 report를 덮어쓸 때만 --overwrite를 요구한다. schema가 깨진 validation.json이나 누락된 artifact는 실패시키기보다 skipped_artifacts에 기록한다. 그래야 오래된 실험 run 때문에 전체 회고가 멈추지 않는다.

2단계 느린 개선 루프는 scripts/propose_improvements.py로 둔다. 이 스크립트는 collector의 summary.json과 trend.md를 읽고, 바로 파일을 수정하지 않고 proposals/{date}/proposal.md와 proposal.json을 만든다. proposal.md에는 반복 문제, 근거 run 링크, 제안 변경 대상, 예상 효과, 위험, 회귀 테스트 케이스를 적는다. 예를 들어 uncertainty_boundary가 반복적으로 약하면 verifier prompt에 불확실성 표시 예시를 추가하자는 제안이 나올 수 있다. alternatives_tradeoff가 반복적으로 약하면 SKILL.md 체크리스트나 rubric 설명을 바꾸자는 제안이 나올 수 있다.

느린 개선 루프의 핵심 안전장치는 네 가지다. 첫째, 자동 수정 금지. 둘째, 변경 후보는 patch가 아니라 proposal로 먼저 남긴다. 셋째, 사용자가 승인한 proposal만 apply 단계로 간다. 넷째, 기준 변경 뒤에는 대표 run 세트로 회귀 확인을 한다. 회귀 세트는 pass였던 좋은 답변, fail이었다가 수정된 답변, 경계선 점수 답변을 각각 포함한다.

구현 순서는 collector부터다. 먼저 collect.py로 summary.json과 trend.md를 만들고, 그 다음 propose_improvements.py를 붙인다. 마지막에 apply_proposal.py나 수동 patch 흐름을 붙인다. 처음부터 자동 patch까지 만들면 편해 보이지만 기준 안정성이 흔들릴 수 있다. 반대로 collector만 만들면 안전하지만 101번째 검사가 더 똑똑해지는 효과는 약하다. 그래서 추천은 collector + proposal까지만 자동화하고, apply는 사람 승인 기반으로 두는 것이다.

완성 기준은 명확해야 한다. collector는 샘플 run 1개와 깨진 artifact 1개가 있어도 report를 생성해야 한다. proposal은 최소 3개 이상의 run 또는 같은 weak_axis가 2회 이상 반복될 때만 기준 변경 후보를 내야 한다. 기준 변경은 승인 전에는 파일을 수정하지 않아야 한다. 이렇게 만들면 thinking-review-gate는 개별 답변 검사기에서, 기록을 통해 기준 개선 후보를 축적하는 실전형 품질 운영 루프로 넘어간다.
