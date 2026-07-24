# 결론

자연 법칙처럼 모든 프로젝트에 맞는 숫자 하나는 없다. 하지만 이 프로젝트에서 AI가 같은 입력에 같은 판정을 내리도록 하는 결정적 운영 게이트는 만들 수 있다. 추천은 `불변식 수` 하나가 아니라 `보호할 Domain`, `관찰된 JPA 의존 피해`, `모델 변화`, `매핑 복잡도`를 순서대로 세는 4단계 게이트다.

판정 단위는 애플리케이션 전체가 아니라 Aggregate 하나다. 결과는 `COMBINE`, `SEPARATE`, `SELECTIVE_REDESIGN`, `NOT_EVALUABLE` 네 가지로 고정한다.

## 0. 아키텍처 강제 게이트

프로젝트에 Domain module의 persistence dependency를 금지하는 ArchUnit, module build rule, 문서화된 mandatory rule이 있으면 다른 점수를 세지 않고 `SEPARATE`다.

단순히 헥사고날을 선호한다는 문장만으로는 이 게이트를 통과시키지 않는다. 실제 강제 규칙을 `file:line`으로 제시해야 한다.

## 1. 보호할 Domain이 있는지 센다

다음 값을 센다.

- `I`: 복합 Domain invariant 개수
- `T`: guarded state transition 개수
- `A`: Aggregate Root가 child 변경을 독점하면 1, 아니면 0

### `I`에 포함하는 규칙

객체가 업무적으로 잘못된 상태가 되는 것을 막으며 다음 중 하나 이상에 의존하는 고유한 규칙만 센다.

- 현재 상태
- 둘 이상의 Domain 값
- child entity 또는 collection
- 과거 이력
- 업무 시간이나 순서

Null, 문자열 길이, 정규식, JSON 형식, DB FK/Unique 같은 입력·저장 제약은 세지 않는다. 같은 업무 규칙을 여러 `if`와 테스트가 표현해도 1개로 센다.

### `T`에 포함하는 행위

현재 상태를 바꾸며 업무 precondition을 검사하는 public Domain method만 센다. 단순 setter와 CRUD service method는 제외한다.

다음이면 보호할 Domain이 있다고 판정한다.

```text
R = (I >= 1) OR (T >= 2) OR (A == 1)
```

`R=false`면 `COMBINE`이다. 별도 Rich Domain Entity를 만들 보호 대상이 아직 없기 때문이다. API DTO나 query projection 분리는 이 판정과 별개로 허용한다.

중요하게도 `I>=1`은 분리 확정이 아니라 다음 게이트로 갈 자격이다.

## 2. JPA 의존 피해를 센다

### Hard evidence `H`

다음 증거의 개수를 센다.

1. Domain rule unit test가 Spring Context, EntityManager, 실제 DB 중 하나를 필요로 한다.
2. 같은 Domain method가 Lazy initialization, Proxy, Managed/Detached 상태 때문에 실행 실패하거나 다른 DB 접근을 요구한 테스트 또는 코드 경로가 있다.
3. 업무 규칙의 정합성이 JPA lifecycle callback, Dirty Checking, entity listener 중 하나에 의존한다.
4. Application 또는 Domain Port가 JPA Entity 타입을 입력이나 출력 계약으로 노출한다.

각 항목은 실제 코드나 테스트의 `file:line`이 있어야 1이다. 가능성만 있으면 0이 아니라 soft signal로 보낸다.

### Soft coupling `S`

다음 항목의 개수를 센다.

1. Domain package가 `jakarta.persistence`를 import한다.
2. Domain behavior가 JPA mapped association을 순회한다.
3. Persistence-only attribute나 relation이 두 개 이상 Domain class에 들어 있다. 예시는 version, FK navigation, audit field다.
4. 같은 Domain 개념을 구성하는 저장소나 외부 source가 두 개 이상이다.
5. Git history나 변경 기록에서 persistence 변경과 Domain 변경이 독립적으로 일어난 사례가 두 번 이상 확인된다.

단순 `@Entity` 하나는 `S=1`일 뿐이므로 분리를 확정하지 않는다.

## 3. 매핑 비용 `C`를 센다

다음 항목마다 1을 더한다.

1. child entity collection이 있다.
2. child의 DB-generated ID를 보존해야 한다.
3. `@Version`을 왕복 보존해야 한다.
4. 추가·수정·삭제·순서를 diff해야 하는 orphan 관계가 있다.
5. bulk 또는 partial update가 Aggregate 전체 로딩을 우회한다.
6. bidirectional, cyclic, polymorphic mapping 중 하나가 있다.

```text
C = 해당 항목 수
low mapping cost  = C <= 2
high mapping cost = C >= 3
```

`C>=3`은 통합하라는 뜻이 아니다. mapper가 위험하므로 Aggregate 경계 축소, write/read model 분리, 핵심 행위만 선택적으로 분리할 필요가 있다는 뜻이다.

## 4. 결정표

```text
if required_architecture_rule_exists:
    SEPARATE
elif inspection_scope_is_incomplete:
    NOT_EVALUABLE
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
    COMBINE_AS_RICH_JPA_ENTITY
```

`H>=1`은 이미 테스트나 런타임에서 의존 피해가 관찰됐다는 뜻이다. `H=0, S>=2`는 피해가 아직 재현되지는 않았지만 소스 의존과 모델 변화 신호가 겹쳤다는 뜻이다. 이 임계값은 v0.1 운영 기준이며 과거 프로젝트에 대입한 뒤 false positive와 false negative로 보정해야 한다.

## 예시 판정

### 단순 CRUD

```text
I=0, T=0, A=0
R=false
결론=COMBINE
```

필드 validation과 CRUD만 있다. JPA Entity를 별도 Domain Entity와 1:1로 복제할 이유가 없다.

### 불변식 하나가 있지만 JPA 피해는 없는 경우

```text
I=1: 특정 상태에서는 예약할 수 없다
T=1: reserve()
A=0
H=0
S=1: @Entity import만 있음
C=1
결론=COMBINE_AS_RICH_JPA_ENTITY
```

불변식 하나만으로는 분리하지 않는다. JPA Entity가 업무 메서드를 가지되 Domain test가 plain Java로 동작하고 JPA 실행 의미가 규칙에 개입하지 않는다면 통합이 더 싸다.

### 불변식과 실제 JPA 피해가 함께 있는 경우

```text
I=1
T=1
H=1: Domain rule test가 Lazy association 때문에 JPA integration test를 요구
S=2: persistence import + mapped association 순회
C=2
결론=SEPARATE
```

이 경우 mapper는 단순 중복이 아니라 Task C-1에서 지적한 의존성을 Adapter 밖으로 밀어내기 위한 비용이다.

### Rich Aggregate인데 매핑도 복잡한 경우

```text
I=3
T=4
A=1
H=1
C=5: child collections, generated IDs, version, orphan diff, partial update
결론=SELECTIVE_REDESIGN
```

전면 통합으로 돌아가지 않는다. Aggregate가 너무 큰지 확인하고, 핵심 command model만 분리하며 조회는 projection으로 우회하는 방안을 검토한다.

## AI 출력 계약

AI는 최종 판정 전에 다음 형식을 반드시 출력한다.

```yaml
scope: <aggregate or feature>
evidence_complete: true|false
R:
  invariants:
    count: 0
    evidence: []
  guarded_transitions:
    count: 0
    evidence: []
  aggregate_root_controls_children:
    value: false
    evidence: []
H:
  count: 0
  evidence: []
S:
  count: 0
  evidence: []
C:
  count: 0
  evidence: []
decision: COMBINE|COMBINE_AS_RICH_JPA_ENTITY|SEPARATE|SELECTIVE_REDESIGN|NOT_EVALUABLE
reason: <one sentence>
```

모든 evidence는 `file:line`과 rule ID를 가져야 한다. 파일 범위를 충분히 보지 못하면 없는 것으로 추정하지 않고 `evidence_complete=false`, `NOT_EVALUABLE`을 반환한다.

## 핵심 주의점

이 게이트는 반복 가능한 팀 정책 초안이지 객관적 업계 법칙이 아니다. 특히 `S>=2`, `C>=3` 임계값은 먼저 5~10개 과거 Aggregate에 적용해 사람 판정과 비교한 뒤 조정해야 한다. 규칙을 바로 자동화하기보다 첫 단계에서는 AI가 evidence report를 만들고 사람이 판정을 검토하는 방식이 안전하다.
