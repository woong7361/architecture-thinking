# Decision

Task C-2의 제출물을 완성한다. 앞선 리서치와 Task C-1 의존성 결론, 방금 확정한 D/P 및 매핑 비용 사후 확인 기준을 하나의 독립적으로 이해 가능한 답안으로 작성한다.

# Checked context

- `task3/assignments/taskC-2.md`는 분리와 통합의 현실적 장점·비용, 매핑 비용을 감수할 개인 기준을 최소 400자로 요구한다.
- `task3/assignments/taskC-1.md:46`은 핵심 문제를 Domain Policy가 외부 기술과 그 모델에 의존하여 테스트 비용과 변경 전파가 커지는 것으로 정리한다.
- `task3/assignments/taskC-1.md:141`은 보호할 업무 규칙의 가치와 경계·매핑 비용을 비교해야 한다고 결론낸다.
- 이전 research run은 Jakarta Persistence, Spring PetClinic, Buckpal, Microsoft DDD guide, Netflix Studio Workflows, Shopify, Allegro를 조사했다.
- 이전 재검토에서 DB/Domain 모델 차이는 1차 원인이 아니라 의존성 문제의 증폭 신호라고 바로잡았다.
- 직전 논의에서 D는 보호할 업무 규칙, P는 업무 규칙이 JPA 때문에 테스트·변경 격리를 잃는 증거로 확정했다.
- M은 분리 이유가 아니므로 독립 축에서 제거하되, Task C-2 요구상 매핑 비용 확인은 마지막 구현 범위 판단으로 남긴다.

# Constraints

- 앞선 논의를 보지 않은 독자도 이해할 수 있어야 한다.
- 괄호식 부연을 피하고 필요한 설명은 문장으로 푼다.
- 실제 사례의 기술 범위와 한계를 밝힌다. Netflix와 Shopify를 JPA 사례로 오인시키지 않는다.
- 규모는 트래픽이나 회사 크기 하나가 아니라 변경 표면, 데이터 소스, 팀 경계, 규칙 복잡도로 설명한다.
- `불변식 1개면 무조건 분리`로 결론내지 않는다.
- 통합 Rich JPA Entity 예시와 JPA 실행 의미에 결합된 예시를 모두 포함한다.
- Mapping이 단순 값 변환인지 DB 상태 판단인지로 비용을 확인한다.
- 참고 링크를 명시한다.
- 검토가 통과한 뒤에만 과제 파일을 수정한다.

# Success criteria

1. 분리와 통합의 정의, 장점, 비용이 대칭적으로 설명된다.
2. 실제 사용 사례와 시스템 사이즈 기준이 포함된다.
3. Task C-1의 의존성 문제와 Task C-2의 매핑 비용이 인과적으로 연결된다.
4. D/P 기준과 두 코드 예시가 포함된다.
5. Mapper가 단순 변환이 아닐 때 전면 분리 대신 범위를 재설계하는 기준이 포함된다.
6. 제출물 체크박스를 완료 처리할 수 있는 최종 답안이다.
