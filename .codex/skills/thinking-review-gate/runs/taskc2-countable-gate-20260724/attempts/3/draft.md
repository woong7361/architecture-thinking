# 결론

모든 프로젝트에 객관적으로 참인 숫자 하나는 없다. 하지만 이 프로젝트에서 AI가 같은 코드에 같은 판정을 내리도록 하는 결정적 운영 게이트는 만들 수 있다. 추천은 `불변식 1개면 분리`가 아니라 `보호할 Domain`, `관찰된 JPA 실행 피해`, `정적 결합과 모델 변화`, `매핑 복잡도`를 순서대로 세는 게이트다.

근거는 `task3/assignments/taskC-1.md:46`의 `Domain Policy가 외부 기술과 그 모델을 향해 의존`한다는 결론과 `task3/assignments/taskC-1.md:141`의 `보호할 업무 규칙의 가치가 경계와 매핑 비용보다 큰가`라는 기준이다. Task C-2도 `task3/assignments/taskC-2.md:8`에서 항상 분리와 항상 통합을 피하고 본인 기준을 세우라고 요구한다.

판정 단위는 애플리케이션 전체가 아니라 Aggregate 하나다. Aggregate가 아직 식별되지 않은 단순 기능은 하나의 bounded feature를 임시 scope로 사용한다. 결과 enum은 다음 네 개만 사용한다.

```text
COMBINE
SEPARATE
SELECTIVE_REDESIGN
NOT_EVALUABLE
```

Rich JPA Entity로 통합한다는 설명은 `COMBINE`의 reason에만 기록하며 별도 enum으로 만들지 않는다.

## 운영 대안 비교

| 방식 | 장점 | 실패 가능성 |
| --- | --- | --- |
| 피해가 생길 때까지 항상 통합 | 초기 비용이 가장 작다 | 경계 누수 뒤 분리하면 migration 범위가 커진다 |
| 복합 불변식이 1개면 즉시 분리 | 규칙이 단순하고 AI 판정이 쉽다 | JPA와 무관하게 잘 테스트되는 Rich Entity까지 과잉 분리한다 |
| 증거 기반 단계 게이트 | Task C-1의 의존 원인과 C-2의 매핑 비용을 함께 본다 | 검사 범위가 필요하고 초기 임계값을 calibration해야 한다 |

세 번째 방식을 v0.1로 추천한다.

## 0. 최소 검사 범위

다음 네 범주를 모두 읽거나, 저장소 검색으로 해당 범주가 없음을 확인해야 `evidence_complete=true`다.

1. Aggregate Root와 Root가 필드로 소유한 Entity, Value Object의 source
2. 해당 Aggregate의 unit test와 integration test
3. 해당 Aggregate를 저장하는 JPA Entity, Repository, Adapter, Mapper
4. module build file, package rule, ArchUnit test, 아키텍처 문서

Git history는 선택 근거다. 접근하지 못해도 `NOT_EVALUABLE`로 만들지 않는다. 위 네 범주 중 하나라도 확인하지 못하면 다른 값을 0으로 추정하지 않고 다른 모든 게이트보다 먼저 `NOT_EVALUABLE`을 반환한다.

## 1. 아키텍처 강제 게이트

Domain module의 persistence dependency를 금지하는 mandatory rule이나 실행되는 architecture test가 발견되면 즉시 `SEPARATE`다. 선호 문장만으로는 부족하며 `file:line` 증거가 필요하다.

## 2. 보호할 Domain `R`

다음을 센다.

- `I`: 복합 Domain invariant 개수
- `T`: guarded state transition 개수
- `A`: Aggregate Root가 owned child 변경에 업무 precondition을 강제하면 1, 단순 소유만 하면 0

### `I`를 세는 규칙

업무적으로 잘못된 상태를 막으며 다음 중 하나 이상을 조건으로 사용하는 고유한 규칙만 센다.

- 현재 Domain 상태
- 둘 이상의 Domain 값
- owned child 또는 collection
- 과거 이력
- 업무 시간이나 순서

Null, 길이, 정규식, JSON 형식, DB FK/Unique는 제외한다. 같은 업무 규칙이 여러 `if`, exception, test에 반복되어도 1개다. AI는 규칙마다 Root method 또는 Domain test의 `file:line`을 하나 이상 제시한다.

### `T`를 세는 규칙

Root 또는 owned Entity의 public method 중 다음을 모두 만족하는 고유 method를 센다.

1. Aggregate state를 직접 변경하거나 Domain method에 위임한다.
2. 변경 전에 업무 precondition을 검사한다.

Setter와 Application Service의 CRUD orchestration은 제외한다.

```text
R = (I >= 1) OR (T >= 2) OR ((A == 1) AND (T >= 1))
```

`R=false`면 `COMBINE`이다. 별도 Rich Domain Entity를 둘 보호 대상이 아직 없기 때문이다. API DTO와 query projection은 별도로 분리할 수 있다. `I>=1`은 분리 확정이 아니라 다음 게이트 진입 조건이다.

## 3. 관찰된 JPA 실행 피해 `H`

다음 세 항목 중 실제 코드 또는 테스트로 확인된 개수를 센다.

1. Domain method 또는 invariant evaluation 경로 자체가 Spring Context, EntityManager, 실제 DB state, Proxy initialization 중 하나 없이는 실행될 수 없음이 code 또는 실패 test로 확인된다. 단순히 기존 test가 integration style로 작성됐다는 사실은 세지 않는다.
2. Domain method가 Lazy initialization, Proxy, Managed/Detached 상태 때문에 실패한 test 또는 exception handling path가 있다.
3. 업무 정합성이 JPA lifecycle callback, Entity Listener, Dirty Checking 중 하나가 실행되어야만 유지된다.

가상의 위험은 세지 않는다. 각 항목은 실패 test, integration test, production exception handling 또는 해당 의존을 직접 사용하는 code의 `file:line`이 있어야 1이다.

## 4. 정적 결합과 변화 신호 `S`

다음 항목의 개수를 센다.

1. Aggregate source가 `jakarta.persistence`를 import한다.
2. Domain behavior가 JPA mapped association field를 순회한다.
3. Domain/Application Port가 `@Entity` class를 parameter 또는 return type으로 노출한다.
4. `@Version`, FK navigation, audit field처럼 Domain behavior와 Domain test에서 사용되지 않고 mapping에만 쓰이는 field/relation이 두 개 이상이다.
5. 같은 Outbound Port를 구현하는 concrete persistence/data-source Adapter가 두 개 이상이다.
6. Git history를 확인했다면 persistence-only change와 domain-rule-only change가 각각 한 번 이상 독립 commit으로 확인된다.

Git을 확인하지 않았다면 6번은 `not_inspected`로 기록하고 합계에서 제외한다. `S>=2` 임계값은 바꾸지 않는다. Git 신호는 결정을 완화하기 위한 필수 점수가 아니라 이미 존재하는 정적 결합 증거를 강화하는 선택 보너스다. 단순 `@Entity` import 하나는 `S=1`이므로 분리를 확정하지 않는다.

## 5. 매핑 복잡도 `C`

다음 항목마다 1을 더한다.

1. owned child entity collection이 있다.
2. child의 DB-generated ID를 왕복 보존해야 한다.
3. `@Version`을 왕복 보존해야 한다.
4. 추가·수정·삭제·순서를 diff하는 orphan 관계가 있다.
5. bulk 또는 partial update가 Aggregate 전체 로딩을 우회한다.
6. bidirectional, cyclic, polymorphic mapping 중 하나가 있다.

```text
low mapping cost  = C <= 2
high mapping cost = C >= 3
```

`C>=3`은 통합 신호가 아니다. mapper 자체가 위험하므로 Aggregate 경계를 줄이거나 command model만 분리하고 query는 projection으로 우회하라는 신호다.

## 6. 결정표

```text
if evidence_complete == false:
    NOT_EVALUABLE
elif mandatory_architecture_rule_exists:
    SEPARATE
elif R == false:
    COMBINE
elif H >= 1 and C <= 2:
    SEPARATE
elif H >= 1 and C >= 3:
    SELECTIVE_REDESIGN
elif H == 0 and S >= 2 and C <= 2:
    SEPARATE
elif H == 0 and S >= 2 and C >= 3:
    SELECTIVE_REDESIGN
else:
    COMBINE
```

마지막 `COMBINE`은 불변식이 있어도 JPA 실행 피해가 없고 정적 결합 신호도 하나 이하이므로 Rich JPA Entity로 유지한다는 뜻이다.

신호의 역할은 동일 가중치 점수가 아니다. `H`는 재현된 실행 피해라 하나만 있어도 즉시 분리 쪽으로 간다. `S`는 잠재 결합이라 서로 다른 신호 두 개가 겹쳐야 분리를 검토한다. `C`는 분리 편익을 깎는 점수가 아니라 mapper 위험을 나타내며, 높으면 통합 대신 경계 재설계로 보낸다. 이 비대칭이 Task C-1의 원인, C-2의 비용을 반영한다.

## 예시 판정

### 단순 CRUD

```text
I=0, T=0, A=0 -> R=false -> COMBINE
```

### 불변식 하나와 Annotation만 있는 가상 예시

```text
I=1, T=1, A=0
H=0
S=1: @Entity import
C=1
decision=COMBINE
reason=Rich JPA Entity로도 규칙이 plain Java test에서 독립 실행됨
```

불변식 하나만으로 분리하지 않는다.

### 불변식과 JPA 실행 피해가 함께 있는 가상 예시

```yaml
scope: Ticket
evidence_complete: true
R:
  invariants:
    count: 1
    evidence:
      - "src/domain/Ticket.java:42 [I1: 특정 상태에서 변경 거부]"
  guarded_transitions:
    count: 1
    evidence:
      - "src/domain/Ticket.java:40 [T1: reserve]"
  aggregate_root_controls_children:
    value: false
    evidence: []
H:
  count: 1
  evidence:
    - "src/test/TicketJpaTest.java:31 [H1: Domain rule test requires EntityManager]"
S:
  count: 2
  evidence:
    - "src/domain/Ticket.java:3 [S1: persistence import]"
    - "src/domain/Ticket.java:47 [S2: mapped association traversal]"
C:
  count: 2
  evidence:
    - "src/persistence/TicketJpaEntity.java:28 [C1: child collection]"
    - "src/persistence/SeatJpaEntity.java:12 [C2: generated child ID]"
decision: SEPARATE
reason: "보호할 불변식에 실제 JPA 실행 피해가 있고 매핑 복잡도가 low다."
```

위 경로는 형식을 보여주기 위한 가상 evidence다.

### Rich Aggregate인데 매핑도 복잡한 경우

```text
R=true, H=1, C=5 -> SELECTIVE_REDESIGN
```

전면 통합으로 돌아가지 않는다. Aggregate가 과도하게 큰지 확인하고 핵심 command model만 분리하며 조회는 projection을 검토한다.

## AI 출력 계약

AI는 위 YAML 구조로 모든 count와 `file:line [rule_id]` evidence를 먼저 출력한 뒤 네 enum 중 하나만 반환한다. 필수 필드는 다음과 같다.

```yaml
scope: <aggregate-or-bounded-feature>
evidence_complete: true|false
inspected_sources: []
missing_sources: []
mandatory_architecture_rule:
  found: false
  evidence: []
R:
  I: {count: 0, evidence: []}
  T: {count: 0, evidence: []}
  A: {value: 0, evidence: []}
H: {count: 0, evidence: []}
S:
  count: 0
  git_history: inspected|not_inspected
  evidence: []
C: {count: 0, evidence: []}
decision: COMBINE|SEPARATE|SELECTIVE_REDESIGN|NOT_EVALUABLE
reason: <one sentence>
calibration_note: "v0.1 thresholds"
```

`evidence_complete=false`이면 `missing_sources`가 비어 있으면 안 된다. 이때 R/H/S/C는 확인된 값만 기록하되 판정에는 사용하지 않고 `NOT_EVALUABLE`을 반환한다.

이 게이트는 업계 법칙이 아니라 v0.1 팀 정책이다. 특히 `S>=2`, `C>=3`은 5~10개 과거 Aggregate에 적용해 사람 판정과 비교한 후 조정해야 한다. 자동 적용 전에는 AI가 evidence report를 만들고 사람이 판정을 확인하는 방식이 안전하다.
