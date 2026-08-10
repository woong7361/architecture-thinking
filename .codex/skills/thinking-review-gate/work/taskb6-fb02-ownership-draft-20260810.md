# 두 번째 피드백의 최종 판정

결정은 다음과 같다.

> `Ticket.of()` 실패의 발원지는 Critique다. Gen은 이를 막지 못하고 증폭한 2차 책임이 있다. 재발 방지를 위한 실제 수정 대상은 Critique→Gen 전달 계약이다.

하나만 고르라면 Critique 문제로 분류한다. 그러나 Critique prompt 한 줄을 보강하는 것으로 닫으면 같은 부류의 실패가 반복될 가능성이 높다.

## 왜 Critique가 최초 원인인가

Critique prompt는 이미 다음을 요구한다.

- 실제 코드에서 확인한다.
- 추측하지 않는다.
- "그럴 수 있다"가 아니라 "여기 이 코드가"를 근거로 지적한다.

그런데 1차 Critique는 입력에 persistence adapter가 없는데도 저장소가 setter로 상태를 복원할 가능성을 high 위험으로 판정했다. 가능성을 발견한 것 자체는 유용하지만, 확인된 weakness로 출력한 것은 Critique 계약 위반이다.

따라서 최초 잘못된 신호를 만든 단계는 Critique다.

## Gen에도 책임이 있는 이유

Diagnose prompt도 진단 근거는 code뿐이며 지어내지 말라고 한다. `REVISION_FEEDBACK`은 이전 단계의 지적이지 새로운 evidence가 아니다.

2차 Diagnose는 다음을 확인했어야 한다.

1. 입력 코드에 실제 복원 구현이 있는가.
2. setter 호출처가 남아 있는가.
3. 실제 호출처가 없으면 해당 위험을 DEFER해야 하는가.

하지만 Critique의 지적을 재검증하지 않고 `Ticket.of()`라는 GO 제안으로 바꿨다. 따라서 Gen은 잘못된 입력을 차단하지 못한 방어 실패가 있다.

다만 Implement는 책임 대상이 아니다. Implement는 확정된 GO proposal을 코드로 옮기는 역할이므로, proposal의 근거까지 재심사하게 하면 역할이 중복된다.

## 근본 문제는 전달 계약이다

현재 Critique schema에는 다음 필드만 있다.

```text
severity, axis, where, suggestion
```

그래서 두 종류의 출력이 같은 weakness 배열에 들어간다.

- 코드에서 확인된 결함
- 코드에는 없지만 존재할 수 있는 위험 가설

runner는 이 배열을 그대로 Diagnose의 `REVISION_FEEDBACK`으로 넘긴다. Diagnose 입장에서는 어느 것이 확인된 사실이고 어느 것이 추가 확인이 필요한 추정인지 구분할 수 없다.

따라서 수정은 세 층으로 해야 한다.

### 1. Critique 출력 분리

```text
confirmed_weaknesses
risk_hypotheses
```

확인된 weakness에는 입력 코드에서 추적 가능한 evidence anchor를 요구한다. 입력 밖 가능성은 weakness가 아니라 추가 문맥이 필요한 hypothesis로 출력한다.

### 2. Gen의 재검증 규칙

`REVISION_FEEDBACK` 자체를 근거로 취급하지 않는다. 입력 코드에서 다시 확인된 항목만 GO나 REMOVE로 승격한다. 확인할 수 없는 가설은 DEFER하거나 slow-loop로 보낸다.

### 3. Handoff gate

다음 규칙을 둔다.

```text
confirmed + evidence 있음 → refine 가능
hypothesis / evidence 없음 → 코드 생성 금지, 문맥 확장 또는 사람 확인
```

이 규칙은 persistence나 factory 같은 특정 사례가 아니라 모든 외부 구현 추정, 숨은 호출처 추정, 미래 요구 추정에 적용되는 도메인 중립 규칙이어야 한다.

## 선택지와 트레이드오프

1. Critique prompt만 강화한다.
   - 비용은 가장 작다.
   - 이미 추측 금지 규칙이 있는데도 실패했으므로 재발 방지력이 약하다.
2. Gen에게 모든 피드백 재검증 책임을 준다.
   - 잘못된 Critique를 막을 수 있다.
   - Gen prompt가 복잡해지고 Critique의 역할 오류가 가려진다.
3. Critique 출력과 handoff를 구조화하고 Gen이 확인된 항목만 반영한다. 추천한다.
   - 원인과 방어를 함께 고치고 provenance를 보존한다.
   - schema, runner, prompt를 함께 바꿔야 하므로 구현 비용이 가장 크다.

## 최종 분류

- 문제 이름: `Critique의 미확인 가설이 refine에서 구현 요구로 승격되는 문제`
- 주 책임: Critique
- 보조 책임: Diagnose/Gen refine
- 근본 수정 대상: Critique output schema + runner handoff + Diagnose refine rule
- 처리: 수정 필요 문제로 확정
- 이번 단계: 결정만 확정하고 파일은 변경하지 않는다.

이 결정으로 두 번째 피드백은 닫을 수 있다. 다음은 FB-B6-03의 "한 번에 기법 하나만 적용하는 파울러의 리듬을 게이트로 강제할 것인가"를 검토하면 된다.
