# Original User Input

P는 어떤거때문에 분리하는거야? 

그리고 M은 필요한거야? 

D는 왜 필요한지 알겠는데


# Checked Context

# Decision

단순화한 Domain Entity/JPA Entity 분리 게이트에서 P가 나타내는 분리 편익과 M의 필요성을 다시 판단한다.

# Checked context

- `task3/assignments/taskC-1.md:46`: 핵심 문제는 Domain Policy가 외부 기술과 그 모델에 의존하여 테스트 비용과 변경 전파가 커지는 것이다.
- `task3/assignments/taskC-1.md:141`: 보호할 업무 규칙의 가치와 추가 경계·매핑 비용을 비교해야 한다.
- `task3/assignments/taskC-2.md:8`: 매핑 비용을 감수할 가치가 있는 경우를 본인 기준으로 세워야 한다.
- 직전 단순화안은 D를 보호할 업무 규칙, P를 충분한 영속성 압력, M을 Mapper 위험으로 정의했다.

# Constraints

- 사용자는 D의 필요성은 이해하고 있다.
- P가 어떤 문제 때문에 분리를 유발하는지 인과적으로 설명한다.
- M이 분리 필요성의 근거인지, 비용/방법 판단인지 구분한다.
- 기준이 불필요하게 복잡하면 축을 더 줄인다.
- 과제나 규칙 파일은 수정하지 않는다.

# Success criteria

1. P의 역할을 구체적 전후 예시로 설명한다.
2. P가 없을 때 발생하는 오판을 설명한다.
3. M이 필요한 범위와 불필요한 범위를 구분한다.
4. 가장 단순한 최종 게이트를 제안한다.
5. 대안과 trade-off를 짧게 비교한다.
