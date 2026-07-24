## 문제 지점

- [evidence_complete와 아키텍처 강제 게이트] 초안은 "필수 네 범주 중 하나라도 확인하지 못하면 `NOT_EVALUABLE`"이라고 말한 뒤, 결정표에서는 `mandatory_architecture_rule_exists`를 `evidence_complete == false`보다 먼저 평가한다. 이 순서라면 검사 범위가 불완전해도 아키텍처 규칙 하나만 발견하면 `SEPARATE`가 될 수 있어 입력 제약의 "Missing scope must produce `NOT_EVALUABLE`"과 충돌한다.

- [H1: Domain rule test가 Spring/JPA/DB 없이는 실행되지 않음] 이 항목은 실제 코드 의존성 때문에 plain unit test가 불가능한 경우와, 단순히 기존 테스트가 integration style로 작성된 경우를 구분하지 못한다. 입력 제약은 "source dependency"와 "demonstrated runtime/semantic dependency harm"을 구분하라고 했는데, 현재 H1은 테스트 작성 관례만으로도 피해로 집계될 가능성이 있다.

- [R의 `A == 1` 조건] "Aggregate Root가 owned child 변경을 독점하면 1"은 countable하지만, 그 자체가 보호할 업무 규칙인지 구조적 소유 관계인지 불명확하다. `A == 1`만으로 `R=true`가 되면 불변식이나 guarded transition이 없어도 다음 분리 게이트로 진입할 수 있다. 이 조건은 "단순 CRUD가 아닌 도메인 불변식"을 찾으려는 사용자 질문과 약하게 연결된다.

- [S의 optional Git history 항목] Git history를 확인하지 않으면 6번을 합계에서 제외한다고 했지만, `S >= 2` 임계값은 그대로 유지된다. 검사한 S 항목 수가 달라져도 같은 threshold를 쓰는 정책인지, Git history는 보너스 신호인지 명시가 필요하다. 현재 상태로도 동작은 가능하지만, AI가 같은 판정을 내리는 결정적 게이트로 쓰기에는 `not_inspected` 처리 방식이 덜 엄밀하다.

- [가중 신호와 즉시 결정 신호의 구분] 초안은 `H >= 1`, `S >= 2`, `C >= 3` 같은 숫자 조건을 제시하지만, 각 신호가 왜 같은 크기로 취급되는지에 대한 정책적 근거가 부족하다. "v0.1 팀 정책이며 calibration 필요"라고 한계는 표시했지만, 어떤 항목은 즉시 분리 신호이고 어떤 항목은 보조 신호인지 더 분명히 나누면 검증 가능성이 올라간다.

- [출력 계약의 불완전성] "AI는 위 YAML 구조로 모든 count와 `file:line [rule_id]` evidence를 먼저 출력"한다고 했지만, 실제 필수 필드 목록, `not_inspected` 표현, `evidence_complete=false`일 때 어떤 count를 생략하거나 보류할지까지는 고정하지 않는다. 사용자가 원한 "AI가 명시적으로 세거나 할 수 있는 기준"으로 운영하려면 출력 스키마가 조금 더 엄격해야 한다.

## 확인 필요

- mandatory architecture rule이 발견된 경우에도 네 범주 전체 검사를 요구할지, 아니면 이 경우만 예외적으로 즉시 `SEPARATE`를 허용할지 확인해야 한다.

- H1은 "테스트가 그렇게 작성되어 있음"이 아니라 "도메인 규칙 실행 경로가 Spring/JPA/DB에 실제로 의존함"일 때만 세는지 확인해야 한다.

- `A == 1`을 독립적인 보호 가치로 유지할지, 아니면 복합 불변식 또는 guarded transition이 있는 owned child에만 붙는 보조 신호로 낮출지 결정해야 한다.

- `S >= 2`, `C >= 3` 임계값은 초안이 말한 것처럼 과거 Aggregate 5~10개로 calibration할 전제인지 확인해야 한다.

## 수정 제안

- 결정표에서 `evidence_complete == false`를 최상단으로 올리거나, mandatory architecture rule만 예외라는 문장을 명시한다. 현재처럼 두 규칙이 충돌해 보이면 AI 판정이 흔들릴 수 있다.

- H1을 다음처럼 좁혀라: "plain unit test를 작성하지 않은 것"은 세지 않고, Domain method나 invariant evaluation이 Spring Context, EntityManager, DB state, proxy initialization 없이는 실행 불가능하다는 코드 또는 실패 테스트가 있을 때만 센다.

- `A`는 `R`의 독립 조건에서 빼거나, "owned child에 대한 업무 precondition을 Root가 강제하는 경우"처럼 불변식 또는 guarded transition과 연결된 조건으로 바꾸는 편이 낫다.

- S의 Git history 항목은 optional bonus로 유지할지, 필수 검사 항목으로 올릴지 정리하라. optional로 유지한다면 `not_inspected`일 때도 threshold가 변하지 않는다는 정책 이유를 한 문장 추가하라.

- 출력 계약에는 최소 YAML 필드를 고정하라. 예를 들어 `scope`, `evidence_complete`, `inspected_sources`, `R`, `H`, `S`, `C`, `decision`, `reason`, `calibration_note` 정도를 필수로 두고, `NOT_EVALUABLE`일 때는 `missing_sources`를 반드시 출력하게 하면 된다.

- 초안의 장점인 "불변식 1개는 분리 확정이 아니라 다음 게이트 진입 조건"은 유지하라. 이 부분은 사용자 질문의 예시를 과잉 규칙으로 만들지 않으면서 countable gate로 승격한 핵심이다.

## 요약

초안은 사용자 질문의 핵심인 "AI가 셀 수 있는 분리/통합 게이트"에 잘 맞고, 항상 분리와 항상 통합을 피하는 방향도 입력 맥락과 일치한다. 다만 `NOT_EVALUABLE` 우선순위, H1의 실제 피해 판정, `A == 1`의 의미, optional Git history 처리, 출력 스키마를 더 엄밀히 고정해야 결정적 운영 게이트로 쓰기 좋아진다.