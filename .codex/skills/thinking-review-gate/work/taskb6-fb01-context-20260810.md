# 대상

- `task2/assignments/taskB-6.md`의 FB-B6-01만 상세히 분석한다.
- 원래 주장: B-5가 완성도와 규율은 더 낫지만, 파이프라인은 worktree 게이트로 행위 보존을 기계 검증하므로 사람보다 객관적이라고 썼다.
- 피드백: worktree 객관성은 행위 보존에만 성립하며, 설계 개선 판정은 LLM Eval이므로 LLM 생성물을 LLM이 채점하는 순환이 있지 않느냐고 질문했다.

# 확인한 사실

1. `.codex/skills/refactor-agent/pipeline/behavior_gate.py:32-64`
   - baseline worktree에 변경을 적용하고 지정 테스트 명령의 exit code와 결과로 GREEN, RED, COMPILE_FAIL을 판정한다.
   - 이 판정은 실행한 테스트가 관찰하는 행위에 한정된다.
2. `.codex/skills/refactor-agent/pipeline/runner.py:75-89`
   - LLM이 축별 점수를 만들고, Python 코드는 그 점수의 가중합과 임계값 통과 여부를 결정적으로 계산한다.
   - 합산은 결정적이지만 입력 점수는 LLM 판단이다.
3. `.codex/skills/refactor-agent/pipeline/runner.py:114-133`
   - Critique와 Eval은 분리된 호출이지만 같은 client를 사용한다. 서로의 출력은 보지 않는다.
   - 이는 직접적인 정보 누출을 줄이지만 공유 모델 편향까지 없애지는 않는다.
4. `.codex/skills/refactor-agent/pipeline/runner.py:203-216`
   - 최종 PASS 조건은 Eval의 `score["passed"]`다.
   - Critique 약점은 final에 기록되지만 PASS를 막지 않는다.
   - Eval 실패 시 숫자 대신 약한 축 이름과 Critique 약점을 다음 Diagnose에 전달한다.
5. `.codex/skills/refactor-agent/runs/c0-strict/final.json`
   - 행위 게이트는 6개 시나리오 GREEN이다.
   - Eval은 weighted_total 4.0으로 passed=true다.
   - 같은 final에 high behavior_risk, medium over_engineering 등 Critique 약점 네 건이 남아 있다.
6. `PROBLEM.md:14-37`
   - rubric 점수만 보고 생성기를 고치면 rubric이 최적화 목표가 되는 Goodhart 순환을 열린 문제로 기록했다.
   - 해결 후보는 사람 verdict가 붙은 동결 캘리브레이션 셋이다.

# 해석 경계

- LLM이 생성하고 LLM이 평가한다고 해서 Eval이 자동으로 무가치하거나 논리적으로 무효가 되는 것은 아니다. 별도 세션과 고정 rubric은 내부 품질 검사로 유용하다.
- 그러나 같은 모델 계열의 상관된 오류가 남고 외부 정답과 비교하지 않으므로, Eval은 설계가 객관적으로 더 좋아졌다는 독립 증거가 아니다.
- 한 번의 평가에서는 자기평가 편향 문제이고, Eval 피드백으로 Gen을 반복 수정하는 장기 루프에서는 rubric을 목표로 최적화하는 Goodhart 위험이 더 분명해진다.
- Critique high 지적 자체도 LLM 출력이므로 ground truth는 아니다. 하지만 파이프라인이 Critique를 위험 탐지 단계로 채택했다면, 그 high 위험을 무시하고 PASS하는 것은 내부 판정 정책의 불일치다.
