## 문제 지점

- [Git history 신호의 결정성] `S6`을 "선택 보너스"로 두면서도 `S` 합계에 포함하고 있어, 같은 코드라도 Git history를 확인한 실행과 확인하지 않은 실행의 판정이 달라질 수 있다. 사용자는 AI가 명시적으로 세는 결정적 게이트를 요청했으므로, 선택 입력이 최종 decision을 바꾸는 구조는 약점이다.

- [mandatory architecture rule의 의미] `Domain module의 persistence dependency를 금지하는 mandatory rule`을 발견하면 즉시 `SEPARATE`라고 했는데, 이 규칙이 "Domain과 JPA Entity 분리"를 강제하는지, 아니면 단순히 모듈 의존 방향만 금지하는지 애매하다. 예를 들어 persistence 모듈 안의 JPA Entity와 domain 모듈의 Domain 객체 분리는 강제할 수 있지만, Rich JPA Entity를 어떤 모듈에 두는 정책인지까지 확인해야 한다.

- [evidence_complete의 과도한 선행 실패 가능성] 네 범주 중 하나라도 확인하지 못하면 즉시 `NOT_EVALUABLE`이라고 했는데, "저장소 검색으로 해당 범주가 없음을 확인"하는 기준이 충분히 구체적이지 않다. 테스트가 없는 작은 기능, 아키텍처 문서가 없는 프로젝트, Mapper가 아직 없는 통합 모델에서도 계속 `NOT_EVALUABLE`이 나올 수 있다.

- [I, T 중복 산정 경계] 하나의 public method가 업무 precondition을 검사하며 상태 전이를 수행하고, 그 precondition이 곧 `I`의 복합 불변식일 때 `I=1`, `T=1`로 동시에 세는 것이 의도인지 명시되어 있지 않다. 중복 산정을 허용하는지, 서로 다른 보호 신호로 보는지 설명이 필요하다.

- [A의 countability] `Aggregate Root가 owned child 변경에 업무 precondition을 강제하면 1`이라는 기준은 유용하지만, "Root가 강제한다"의 확인 지점이 모호하다. child collection 변경 메서드를 Root만 노출하는 경우, child setter를 막은 경우, package-private 생성자를 쓰는 경우 등을 어떻게 판정할지 보강이 필요하다.

- [C의 해석과 decision 연결] `C>=3`은 통합 신호가 아니라고 잘 설명했지만, `R=true, H=0, S<2, C>=3`이면 최종 decision은 `COMBINE`이다. 이 경우에도 매핑 복잡도가 이미 큰데 통합 유지가 맞는지, 아니면 "분리할 보호 이유는 부족하므로 COMBINE이지만 mapping risk는 별도 warning"이라고 출력해야 하는지 명확히 해야 한다.

## 확인 필요

- Git history를 최종 판정 입력으로 허용할지, 아니면 참고 evidence로만 출력하고 decision에는 반영하지 않을지 확인해야 한다.

- mandatory architecture rule이 어떤 형태일 때 즉시 `SEPARATE`인지 확인해야 한다. 특히 "Domain module persistence dependency 금지"와 "Domain 객체와 JPA Entity 분리 강제"를 구분해야 한다.

- `evidence_complete`의 필수 범위가 실제 프로젝트 규모에 비해 너무 엄격하지 않은지 확인해야 한다. 없는 범주를 "확인됨"으로 처리하는 검색 기준도 필요하다.

- `I`, `T`, `A`, `H`, `S`, `C`를 세는 최소 evidence 형식이 `file:line` 하나로 충분한지, test 이름이나 method 이름까지 요구할지 확인해야 한다.

## 수정 제안

- `S6`은 최종 `S.count`에서 제외하고 `history_signal` 같은 별도 참고 필드로 분리하는 편이 좋다. 또는 Git history 확인을 필수 검사 범위로 올려서 같은 입력이면 항상 같은 decision이 나오게 해야 한다.

- mandatory architecture rule은 다음처럼 더 좁혀야 한다: "실행되는 architecture test 또는 build rule이 domain layer의 persistence annotation, JPA Entity type, repository implementation 의존을 금지하고, 그 정책이 해당 scope에 적용된다." 단순 선호 문서나 일반 모듈 규칙은 즉시 분리 근거에서 제외하는 것이 안전하다.

- `evidence_complete`는 "필수로 존재해야 하는 범주"와 "없음을 확인하면 충족되는 범주"를 나누어 적어야 한다. 예를 들어 Aggregate source는 필수이고, Mapper는 `rg`로 없음이 확인되면 inspected로 처리할 수 있다고 명시하면 AI가 0을 날조하지 않으면서도 불필요한 `NOT_EVALUABLE`을 줄일 수 있다.

- `I`와 `T`의 중복 허용 여부를 명시해야 한다. 추천은 `I`는 보호해야 할 규칙 수, `T`는 규칙이 걸린 상태 변경 진입점 수로 서로 다른 축이라고 선언하고, 같은 코드 위치가 두 축의 evidence로 재사용될 수 있다고 적는 것이다.

- 최종 YAML 계약에 `warnings` 또는 `notes`를 추가하는 것을 검토할 만하다. 예를 들어 `decision=COMBINE`이어도 `C>=3`이면 "분리는 권하지 않지만 매핑 위험은 별도 관리 필요" 같은 보조 판단을 남길 수 있다.

## 요약

초안은 사용자의 요구에 맞게 countable gate, enum, evidence contract, 예시를 잘 제시한다. 다만 Git history 선택 입력이 decision을 바꿀 수 있고, mandatory architecture rule과 evidence completeness의 판정 경계가 아직 모호하다. 이 두 지점을 정리하면 "AI가 같은 코드에 같은 판정을 내리는 v0.1 팀 정책"이라는 주장에 더 잘 맞는다.