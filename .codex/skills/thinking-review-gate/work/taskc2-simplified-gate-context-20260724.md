# Decision

Task C-2의 Domain Entity와 JPA Entity 분리/통합 게이트가 놓치는 사례를 찾아내고, Task C-1의 의존성 결론을 보존하면서 AI가 명시적으로 판정할 수 있는 더 단순한 기준으로 합친다.

# Checked project context

- `task3/assignments/taskC-1.md:46`: 핵심 문제는 파일 수가 아니라 Domain Policy가 외부 기술과 그 모델을 향해 의존하여 테스트 비용과 변경 전파가 커지는 것이다.
- `task3/assignments/taskC-1.md:141`: 보호할 업무 규칙의 가치가 경계와 매핑 비용보다 큰지가 판단 기준이다.
- `task3/assignments/taskC-2.md:8`: 항상 분리/항상 통합이 아닌 본인 기준을 요구한다.
- 이전 countable gate attempt 3은 `R(I/T/A)`, `H`, `S`, `C` 여섯 기호와 네 결과 enum을 사용했다.
- 이전 critique는 Git 선택 입력이 결정에 영향을 주는 문제, architecture rule 범위의 모호함, 과도한 evidence completeness, I/T 중복, A의 모호함, 높은 C가 COMBINE으로 끝날 때 warning 부재를 지적했다.
- 이전 validation은 pass였지만 임계값은 보편 법칙이 아니라 calibration이 필요한 팀 정책이라고 평가했다.

# Constraints

- Task C-1의 주원인인 외부 기술 의존과 그로 인한 테스트/변경 전파를 중심에 둔다.
- 단순 불변식 하나만으로 분리를 확정하지 않는다.
- 동일 코드와 동일 검사 범위에는 같은 판정이 나오도록 선택적 Git history를 자동 판정에서 제외한다.
- 없는 테스트, Mapper, 아키텍처 문서 때문에 자동으로 NOT_EVALUABLE이 되지 않게 한다.
- AI가 코드에서 확인 가능한 신호와 사용자에게 별도 입력받아야 하는 경영/미래 맥락을 구분한다.
- 공용 판단 규칙은 특정 도메인 명사에 고정하지 않는다.
- 설계만 제안하며 과제나 규칙 파일은 수정하지 않는다.

# Success criteria

1. 기존 게이트가 못 잡는 사례를 구체적으로 밝힌다.
2. 중복되는 축을 합쳐 3개 이하의 핵심 축으로 단순화한다.
3. 각 축은 AI가 증거와 함께 판정할 수 있어야 한다.
4. 결정표와 최소 4개의 예시를 제공한다.
5. 자동 판정으로 잡지 못하는 한계를 분명히 표시한다.
6. 대안과 trade-off를 비교하고 추천안을 밝힌다.
