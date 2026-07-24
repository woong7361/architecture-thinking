# 결론

있다. 이전 게이트는 의존성 문제를 보지만, 세부 신호를 너무 잘게 나눠 같은 업무 규칙을 `I`, `T`, `A`로 중복 집계할 수 있다. 반대로 Aggregate 밖의 Domain Service에 있는 교차 객체 규칙과 XML 매핑처럼 annotation import가 없는 영속성 결합은 놓칠 수 있다.

추천은 여섯 기호 `I/T/A/H/S/C`를 세 축 `D/P/M`으로 합치는 것이다.

- `D`: 보호할 업무 규칙이 있는가
- `P`: 그 규칙에 영속성 기술이 얼마나 압력을 주는가
- `M`: 분리할 때 Mapper가 얼마나 위험해지는가

비유하면 검문소 여섯 개를 세 개로 합치는 것이다. 먼저 보호할 화물이 있는지 보고, 외부 기술이 화물을 건드리는지 보고, 다른 차로 옮겨 싣는 비용이 큰지를 본다.

## 현재 게이트가 놓치거나 잘못 셀 수 있는 것

| 문제 | 기존 기준의 한계 | 단순화안의 처리 |
| --- | --- | --- |
| 같은 규칙 중복 집계 | 하나의 상태 전이 규칙이 `I`, `T`, `A`에 동시에 잡힌다 | 고유 업무 규칙을 `D`에서 한 번만 센다 |
| Aggregate 밖 업무 규칙 | Root와 owned child 중심이라 Domain Service의 교차 객체 정책을 놓친다 | 해당 feature가 책임지는 모든 업무 규칙을 `D`에 포함한다 |
| annotation 없는 결합 | XML 매핑, converter, superclass 매핑은 `jakarta.persistence` import 검색만으로 빠질 수 있다 | source뿐 아니라 ORM 설정과 converter를 `P` 검사 범위에 포함한다 |
| 통합 모델의 실제 피해와 잠재 결합 분산 | `H`와 `S`가 같은 원인의 실제/잠재 증거인데 별도 계산식이라 복잡하다 | 강한 증거와 약한 증거를 `P` 하나의 단계로 합친다 |
| 높은 매핑 비용의 원인 오판 | `C`가 높으면 분리 비용만 커 보이지만 Aggregate 경계가 너무 큰 증상일 수 있다 | `M=HIGH`면 통합이 아니라 `SELECTIVE_REDESIGN`으로 보낸다 |
| 선택 입력에 따른 판정 변화 | Git history를 봤는지에 따라 `S`가 바뀔 수 있다 | Git 이력은 설명 참고로만 쓰고 자동 판정에서 제외한다 |
| 자료가 없어서 계속 판정 불가 | 테스트·Mapper·아키텍처 문서가 없으면 `NOT_EVALUABLE`이 될 수 있다 | 업무 코드와 실제 영속성 매핑만 필수로 한다. 나머지는 있으면 검사한다 |

다만 코드만으로는 미래에 저장소가 추가될지, 조직이 Mapper를 감당할 수 있는지, 성능 병목이 실제로 있는지 확정할 수 없다. 이런 항목은 자동 점수에 넣지 않고 `context_warnings`로 남겨야 한다. 성능은 프로파일링이나 쿼리 측정이 있을 때만 판단 근거로 쓴다.

## 대안 비교

| 방안 | 장점 | 비용과 실패 가능성 |
| --- | --- | --- |
| 기존 `I/T/A/H/S/C` 유지 | 진단 세부 정보가 많다 | 중복 산정과 분기 수가 많아 AI와 사람이 해석하기 어렵다 |
| 불변식 유무만 사용 | 가장 단순하다 | Task C-1의 핵심인 외부 기술 의존과 테스트 피해를 판정하지 못한다 |
| `D/P/M` 3축 | 보호 가치, 의존성 피해, 매핑 비용을 각각 한 번만 판단한다 | 임계값은 팀 사례로 보정해야 하며 미래·조직 맥락은 별도 확인이 필요하다 |

Task C-1의 결론을 가장 적은 축으로 보존하는 세 번째 방안을 추천한다.

## 단순화한 결정 게이트

판정 단위는 Aggregate 하나다. Aggregate가 식별되지 않았다면 하나의 변경 목적을 가진 bounded feature를 사용한다. 결과는 네 개만 둔다.

```text
COMBINE
SEPARATE
SELECTIVE_REDESIGN
NOT_EVALUABLE
```

### 0. 최소 검사 범위

다음 두 가지가 확인되어야 한다.

1. 해당 feature의 업무 행위를 구현한 source
2. 그 feature가 저장되는 실제 mapping source 또는 설정

테스트, Mapper, architecture test는 존재하면 읽는다. 저장소 검색으로 없음을 확인했다면 누락으로 보지 않는다. 업무 source나 영속성 mapping의 위치를 찾지 못했을 때만 `NOT_EVALUABLE`이다.

실행되는 architecture test나 build rule이 해당 scope의 Domain 객체와 JPA Entity 분리를 직접 강제한다면 다른 계산보다 먼저 `SEPARATE`다. 단순히 Domain module의 특정 dependency만 금지하거나 문서가 분리를 선호하는 정도는 이 조건에 포함하지 않는다.

### 1. `D`: 보호할 업무 규칙

`D=true`는 해당 feature에 고유한 비자명 업무 규칙이 한 개 이상 있다는 뜻이다. 다음 중 하나를 만족하면서, 규칙을 어기면 업무적으로 잘못된 상태나 행위가 허용되는 경우만 센다.

- 기존 상태, 둘 이상의 업무 값, 자식/이력/시간을 사용해 잘못된 상태를 막는다.
- 상태 변경 전에 업무상 허용 조건을 검사한다.
- 둘 이상의 객체나 Aggregate 사이에서 반드시 지켜야 할 업무 정책을 검사한다.

Null, 문자열 길이, 정규식, 직렬화 형식, DB FK/Unique만 확인하는 것은 제외한다. 같은 규칙이 method, `if`, exception, test에 반복되어도 한 번만 센다. Root 내부 규칙인지 Domain Service의 교차 객체 규칙인지는 구분하지 않는다.

```text
D=false -> COMBINE
```

보호할 업무 규칙이 없으면 별도 Domain Entity를 만드는 비용을 정당화할 대상이 없기 때문이다.

### 2. `P`: 영속성 압력

`P`는 `NONE`, `WEAK`, `STRONG` 중 하나다.

다음 중 하나라도 실제 코드나 실패 테스트로 확인되면 `STRONG`이다.

1. 업무 규칙 실행이나 테스트에 EntityManager, DB 상태, proxy 초기화, persistence context가 필요하다.
2. 업무 정합성이 JPA lifecycle callback, Entity Listener, dirty checking에 의존한다.
3. Core Port 또는 Application 계약이 JPA Entity 타입을 입출력으로 노출한다.
4. 같은 업무 개념을 현재 두 개 이상의 concrete persistence/data-source model로 변환한다.

강한 증거가 없고 다음 약한 신호가 서로 다른 두 종류 이상이면 `WEAK`이다.

1. 업무 모델이 persistence annotation/type을 import하거나 ORM XML, mapped superclass, custom converter로 직접 매핑된다.
2. 업무 method가 mapped association을 순회한다.
3. 업무에 쓰이지 않는 persistence 전용 field/relation이 두 개 이상 있다.
4. 현재 코드나 확정 문서에 schema와 업무 모델의 독립 변경 또는 구조 불일치가 명시돼 있다.

그 외는 `NONE`이다. 단순 `@Entity` annotation 하나는 약한 신호 한 개라서 `P=NONE`으로 처리한다. Git history와 미래 계획은 자동 합계에 넣지 않는다.

### 3. `M`: Mapper 위험

다음 세 그룹 중 해당하는 그룹 수를 센다.

1. `identified graph`: DB identity를 가진 child entity collection이 있다.
2. `round-trip state`: generated child ID, version, orphan 삭제·순서·diff 중 하나를 왕복 보존해야 한다.
3. `nonstandard shape`: bulk/partial update, 양방향·순환·다형 매핑, custom converter, legacy schema mapping 중 하나가 있다.

```text
0~1개 그룹 -> M=LOW
2~3개 그룹 -> M=HIGH
```

그룹 안의 세부 항목을 여러 개 만족해도 한 그룹으로 센다. 이 방식은 Mapper의 같은 어려움을 여러 번 더하는 문제를 줄인다.

### 4. 최종 결정표

```text
if required source or persistence mapping was not found:
    NOT_EVALUABLE
elif enforced rule directly requires separate Domain and JPA models:
    SEPARATE
elif D == false:
    COMBINE
elif P == NONE:
    COMBINE
elif M == LOW:
    SEPARATE
else:
    SELECTIVE_REDESIGN
```

핵심은 `D AND P`다. 보호할 규칙이 있고 그 규칙에 영속성 압력이 관찰될 때만 매핑 비용을 검토한다. `M=HIGH`는 통합하라는 뜻이 아니다. 큰 Aggregate를 줄이거나, command model만 분리하고 query는 projection으로 처리하는 등 분리 범위를 다시 설계하라는 뜻이다.

## 예시

### 1. 단순 CRUD

필드 저장과 조회만 있고 업무상 잘못된 상태를 막는 규칙이 없다.

```text
D=false -> COMBINE
```

### 2. 불변식 한 개와 `@Entity`만 있음

상태 변경 규칙은 plain Java unit test에서 동작한다. 영속성 결합은 annotation 하나뿐이다.

```text
D=true, P=NONE, M=LOW -> COMBINE
```

불변식 하나만으로 분리하지 않고 Rich JPA Entity를 유지한다.

### 3. 규칙이 mapped collection을 탐색함

상태 변경 규칙이 있고, 같은 클래스가 persistence annotation을 사용하며 업무 method가 lazy mapped collection을 순회한다. 서로 다른 약한 신호 두 개다. 단순 필드 매핑이라 Mapper 위험은 낮다.

```text
D=true, P=WEAK, M=LOW -> SEPARATE
```

### 4. 실제 proxy 의존이 재현됨

업무 규칙 테스트가 proxy 초기화나 persistence context 없이는 실패하며, 변환 대상은 단순하다.

```text
D=true, P=STRONG, M=LOW -> SEPARATE
```

Task C-1에서 지적한 테스트 의존 피해가 실제로 확인된 경우다.

### 5. 교차 Aggregate 규칙과 복잡한 그래프

Domain Service에 두 Aggregate 사이의 업무 정책이 있고, Port가 JPA Entity를 노출한다. 자식 식별자와 version을 왕복하며 bulk update도 함께 사용한다.

```text
D=true, P=STRONG, M=HIGH -> SELECTIVE_REDESIGN
```

기존 기준이 놓칠 수 있던 Aggregate 밖 규칙은 `D`가 잡고, 높은 Mapper 위험은 경계 재설계로 보낸다.

## AI 출력 계약

긴 YAML 대신 다음만 출력하면 된다. 모든 자동 판정 신호에는 `file:line`과 규칙 또는 method 이름을 붙인다.

```yaml
scope: <aggregate-or-bounded-feature>
inspected: []
D:
  value: true|false
  rules: []
P:
  level: NONE|WEAK|STRONG
  signals: []
M:
  level: LOW|HIGH
  groups: []
decision: COMBINE|SEPARATE|SELECTIVE_REDESIGN|NOT_EVALUABLE
reason: <one sentence>
context_warnings: []
```

## 남는 한계

이 게이트도 다음은 자동으로 확정하지 못한다.

- 미래에 저장 기술이나 adapter가 추가될 가능성
- 팀이 Mapper를 유지할 역량과 납기 압박
- 아직 측정하지 않은 N+1, 메모리, 쿼리 성능 문제
- 업무 규칙의 금전적·규제적 중요도

AI는 이를 추정해 점수화하지 말고 사용자 제공 문서나 측정치가 없으면 `context_warnings`에 남긴다. 이 기준은 보편 법칙이 아니라 v0.2 팀 정책이다. 5~10개의 이미 결론 난 기능에 적용하여 사람 판단과 어긋난 사례를 모은 뒤 `P=WEAK`의 두 신호와 `M=HIGH`의 두 그룹 임계값만 조정하는 것이 적절하다.

## 참고한 프로젝트 근거

- `task3/assignments/taskC-1.md:46`
- `task3/assignments/taskC-1.md:141`
- `task3/assignments/taskC-2.md:8`
- `.codex/skills/thinking-review-gate/runs/taskc2-countable-gate-20260724/attempts/3/critique.md`
- `.codex/skills/thinking-review-gate/runs/taskc2-countable-gate-20260724/attempts/3/validation.json`
