## 문제 지점

- [결과 enum 일관성] 초안 앞부분은 결과를 `COMBINE`, `SEPARATE`, `SELECTIVE_REDESIGN`, `NOT_EVALUABLE` 네 가지로 고정한다고 말하지만, 결정표와 출력 계약에는 `COMBINE_AS_RICH_JPA_ENTITY`가 추가되어 있다. 이는 내부 모순이며, caller가 결과 enum을 기계적으로 검증하기 어렵게 만든다.

- [결정적 count 기준] `Domain package`, `Domain class`, `Domain behavior`, `persistence-only attribute`, `같은 Domain 개념`, `독립적으로 일어난 변경` 같은 항목이 아직 프로젝트별 해석을 요구한다. 사용자가 요청한 “AI가 명시적으로 세거나 할 수 있는 기준”으로 쓰려면 파일 범위, 패키지 범위, AST/import 기준, 테스트 기준, git history 기준을 더 고정해야 한다.

- [inspection scope 정의 부족] 결정표에 `inspection_scope_is_incomplete`가 있지만, 어떤 파일과 근거를 확인해야 `complete`인지가 정의되어 있지 않다. `NOT_EVALUABLE`을 안정적으로 반환하려면 최소 검사 범위가 필요하다.

- [대안과 trade-off 부족] 초안은 추천 게이트 하나를 상세히 제시하지만, 다른 운영 방식과의 비교가 약하다. 예를 들어 `항상 통합 후 피해 발생 시 분리`, `불변식 기준 즉시 분리`, `증거 기반 게이트`를 비교하면 왜 현재 게이트가 적절한지 더 검증 가능해진다.

- [예시 evidence 형식 불일치] 출력 계약은 모든 evidence에 `file:line`과 rule ID가 필요하다고 하지만 예시 판정은 자연어 evidence만 사용한다. 예시가 가상 사례라면 “가상 예시”라고 명시하고, 실제 AI 출력 예시는 `file:line` 형태를 보여주는 편이 좋다.

## 확인 필요

- 결과 enum을 정말 네 가지로 고정할지, 아니면 `COMBINE_AS_RICH_JPA_ENTITY`를 `COMBINE`의 subtype으로 허용할지 확인해야 한다.

- 이 게이트가 실제 코드베이스에 적용될 때 최소 검사 범위가 무엇인지 확인해야 한다. 예시는 aggregate 파일, 관련 테스트, repository/adapter, mapper, architecture rule, 변경 이력 포함 여부다.

- git history를 필수 evidence로 볼지 선택 evidence로 볼지 확인해야 한다. history 접근이 불가능한 실행 환경에서는 해당 항목 때문에 불필요하게 `NOT_EVALUABLE`이 될 수 있다.

## 수정 제안

- `COMBINE_AS_RICH_JPA_ENTITY`를 제거하고 `COMBINE`의 reason으로 표현하거나, 처음부터 결과 enum을 다섯 가지로 수정하라. 기계 판정용 계약과 결정표의 enum은 반드시 동일해야 한다.

- `inspection_scope_is_incomplete`를 별도 섹션으로 정의하라. 예를 들어 “aggregate source, aggregate test, persistence adapter/entity mapping, architecture rule file을 모두 읽지 못하면 `NOT_EVALUABLE`”처럼 count 가능한 조건으로 바꾸는 것이 좋다.

- count 항목마다 “세는 방법”을 더 구체화하라. 예를 들어 import는 AST 또는 grep 기준, transition은 public method와 상태 변경 assignment 기준, persistence-only field는 annotation 또는 naming allowlist 기준처럼 정하면 AI와 스크립트가 같은 방식으로 셀 수 있다.

- 예시 판정에는 실제 출력 계약 형식에 맞춘 짧은 YAML 예시를 하나 포함하라. `evidence: ["src/.../Reservation.java:42 [I1]"]`처럼 보여주면 사용자가 적용 방식을 바로 검증할 수 있다.

- 추천 게이트를 확정하기 전에 2~3개 대안을 짧게 비교하라. 현재 답변의 결론은 유지하되, 왜 단일 불변식 기준이 과잉 분리를 만들 수 있고 왜 순수 피해 관찰 기준은 너무 늦을 수 있는지 trade-off를 드러내면 설득력이 높아진다.

## 요약

초안은 사용자의 요구인 “결정적이고 셀 수 있는 분리/통합 게이트”에 상당히 근접해 있다. 다만 결과 enum 모순, 검사 범위 정의 부족, 일부 count 항목의 해석 여지가 남아 있어 그대로 정책화하면 AI 실행 결과가 흔들릴 수 있다. enum을 정리하고 최소 검사 범위와 count 절차를 고정하면 검증 가능한 운영 게이트로 개선될 수 있다.