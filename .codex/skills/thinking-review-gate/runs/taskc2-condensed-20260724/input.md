# Original User Input

수행 내용이 전부 들어가야해 

그리고 너무 길다 좀 압축하자


# Checked Context

# Decision

Task C-2 답안을 현재 분량의 절반 안팎으로 줄이면서 수행 내용과 앞선 사용자 요구를 모두 보존한다.

# Required coverage

1. Domain 모델과 JPA Entity를 통합하는 방식과 분리하는 방식의 정의
2. 두 방식의 현실적 장점과 비용
3. 실제 현장과 시스템 사이즈를 개인 기준으로 해석
4. 실제 분리 시 Domain, Port, Adapter, Mapper, JPA Entity의 배치와 흐름
5. Rich Domain Model과 연결된 통합 예시 및 분리 검토 예시
6. 매핑 비용을 감수할 개인 기준 D/P와 Mapper 비용 확인
7. 참고 자료

# Constraints

- D/P 정의는 한 번만 쓴다.
- 현장·사이즈는 독립 결정 규칙이 아니라 D/P 적용 문맥으로 쓴다.
- 모델 통합 여부와 Port/Adapter 사용 여부를 혼동하지 않는다.
- 단순 `@Entity` 또는 불변식 한 개만으로 분리하지 않는다.
- M은 별도 기호로 두지 않는다.
- 공개 사례의 범위를 과장하지 않는다.
- 앞선 논의를 보지 않은 독자도 이해할 수 있어야 한다.
- 최소 400자를 충족하되 반복 설명과 세부 구현 나열을 제거한다.

# Success criteria

1. Required coverage 7개를 모두 확인할 수 있다.
2. 기존 답안보다 문자 수가 40% 이상 줄어든다.
3. 주요 판단식 `D AND P`와 Mapper 사후 확인이 보존된다.
4. 통합과 분리 예시가 모두 남는다.
5. 문서 내부에 같은 정의나 결론의 반복이 없다.
