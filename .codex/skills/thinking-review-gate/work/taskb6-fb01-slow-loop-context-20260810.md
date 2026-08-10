# 맥락

- 사용자는 Task B-6 첫 번째 피드백의 자기평가 순환 문제를 Goodhart의 법칙과 사람 개입 slow-loop의 필요성으로 요약했다.
- `PROBLEM.md:14-37`은 rubric 점수만 보고 생성기를 고치면 rubric이 최적화 목표가 된다고 기록하며, 해결 조건으로 사람 verdict가 붙은 동결 캘리브레이션 셋을 둔다.
- `.codex/skills/refactor-agent/pipeline/runner.py:203-216`은 Eval PASS만 최종 PASS 조건으로 사용한다. Critique 약점은 기록되지만 PASS를 막지 않는다.

# 판단 경계

- 사용자의 요약은 핵심적으로 맞다.
- 단, 사람 개입이 모든 run의 중간 승인을 뜻할 필요는 없다.
- fast-loop은 테스트, 스키마, 정적 규칙처럼 결정적으로 확인 가능한 신호를 처리할 수 있다.
- slow-loop은 proxy와 실제 품질의 정렬을 확인해야 하는 지점에서 필요하다. 예: Eval과 Critique 충돌, rubric 변경, 새로운 사례 유형, 표본 감사, 낮은 신뢰도.
- 사람의 핵심 역할은 모든 산출물을 직접 채점하는 운영자가 아니라 품질 기준과 calibration set의 소유자다.
