# 결론

`P`는 단순히 JPA 코드가 있다는 뜻이 아니다. `D`에서 찾은 업무 규칙이 JPA의 타입이나 실행 방식 때문에 테스트하기 어려워지거나, 영속성 변경이 업무 코드까지 전파되는지를 확인한다. 즉 `P`는 **분리했을 때 실제로 제거할 의존성 문제가 있는가**를 묻는다.

`M`은 분리 이유가 아니다. 분리했을 때 새로 생기는 Mapper 비용이다. 따라서 `D/P/M`을 같은 급의 세 게이트로 표현한 것은 복잡했다. 더 정확한 구조는 다음과 같다.

```text
분리 편익 = D AND P
분리 방법 = 매핑 비용 확인
```

M이라는 이름과 세부 점수표는 제거할 수 있다. 하지만 Task C-2가 매핑 비용을 감수할 가치가 있는지를 묻기 때문에 비용 확인 자체는 제거하면 안 된다.

## 비유

`D`가 금고에 넣을 귀중품인지 확인하는 것이라면, `P`는 현재 방에 도둑이 들어올 통로가 실제로 있는지 확인하는 것이다. 귀중품이 있어도 방이 이미 안전하고 JPA가 업무 규칙에 영향을 주지 않는다면 별도 금고를 설치할 이유가 약하다.

매핑 비용은 금고를 설치하는 공사비다. 도둑이 있는지를 판단하는 근거는 아니지만, 공사 규모가 건물을 뜯어야 할 정도라면 금고의 크기나 위치를 다시 설계해야 한다.

## P 때문에 분리하는 정확한 이유

P는 다음 두 피해 중 하나를 의미한다.

1. 테스트 격리 실패: 업무 규칙을 검증하려는데 Proxy, Lazy Loading, Persistence Context, EntityManager, DB가 필요하다.
2. 변경 격리 실패: JPA 연관관계, lifecycle, persistence용 field 또는 Entity 타입 변경이 Domain method나 Core Port까지 전파된다.

다음은 `D=true`지만 `P=false`인 경우다.

```java
@Entity
class Ticket {
    private TicketStatus status;

    public void reserve() {
        if (status == TicketStatus.SUSPENDED) {
            throw new CannotReserveSuspendedTicket();
        }
        status = TicketStatus.RESERVED;
    }
}
```

업무 규칙은 있지만 평범한 Java 객체로 생성해 테스트할 수 있고, 규칙이 Lazy association이나 JPA lifecycle을 사용하지 않는다. 이때 Domain Entity와 JPA Entity를 나누면 동일 필드와 Mapper만 늘어날 수 있으므로 통합을 유지한다.

반면 다음은 `D=true`, `P=true`다.

```java
@Entity
class Ticket {
    @OneToMany(fetch = FetchType.LAZY)
    private List<Reservation> reservations;

    public void reserve(UserId userId) {
        boolean alreadyReserved = reservations.stream()
                .anyMatch(it -> it.reservedBy(userId));
        if (alreadyReserved) {
            throw new AlreadyReserved();
        }
        reservations.add(Reservation.by(userId));
    }
}
```

예약 규칙이 JPA가 관리하는 Lazy collection을 직접 탐색한다. 테스트에서 일반 List를 사용할 때와 실제 Proxy collection을 사용할 때 실행 조건이 달라질 수 있고, 연관관계 fetch 전략 변경도 업무 method에 영향을 준다. 분리하면 Domain `Ticket`은 일반 collection으로 규칙을 실행하고 JPA Adapter만 Lazy Loading과 관계 매핑을 처리한다. 이것이 P 때문에 분리하는 이유다.

P를 빼고 `D=true`만으로 분리하면 첫 번째 예시까지 과잉 분리한다. 반대로 D를 빼고 P만 보면 보호할 업무 규칙이 없는 CRUD 모델에도 Domain Entity와 Mapper를 만들게 된다. 그래서 분리 편익은 `D AND P`다.

## M은 필요한가

### 분리 필요성을 판단할 때

필요하지 않다. M은 `왜 분리하는가`에 답하지 못한다. 매핑이 복잡하다는 사실은 분리 편익이 아니라 비용이기 때문이다.

### 최종 구현 방법을 정할 때

비용 확인은 필요하다. Task C-2의 질문 자체가 매핑 비용을 감수할 가치가 있는지 비교하라고 요구한다. 다만 별도의 `M=LOW/HIGH` 축과 상세 점수까지 유지할 필요는 없다.

다음 세 질문 중 두 개 이상이 참인지 확인하는 정도면 충분하다.

- DB 식별자를 가진 자식 graph를 왕복 변환해야 하는가?
- ID, Version, 삭제·순서·Diff를 Mapper가 보존해야 하는가?
- Bulk/Partial Update 또는 순환·다형·Legacy mapping이 있는가?

두 개 미만이면 전면 분리 비용을 감당할 수 있는 후보로 보고, 두 개 이상이면 현재 Aggregate를 그대로 양쪽에 복제하지 않는다. Aggregate를 줄이거나 command만 분리하고 query는 projection으로 둔다.

## 가장 단순한 최종 게이트

```text
1. D가 없는가?
   -> COMBINE

2. D는 있지만 P가 없는가?
   -> COMBINE

3. D와 P가 모두 있는가?
   -> SEPARATE CANDIDATE

4. 매핑 고위험 질문이 2개 이상인가?
   -> 전면 분리 대신 분리 범위를 줄여 재설계
   아니면 -> SEPARATE
```

따라서 결정에 필요한 핵심 기호는 `D`와 `P` 두 개다. 매핑 비용은 별도 기호로 모델링하지 않고 분리 후보가 된 뒤 실행 방법을 고르는 사후 체크로 내리는 것을 추천한다.

## 대안과 trade-off

| 방안 | 장점 | 단점 |
| --- | --- | --- |
| D만 사용 | 가장 단순함 | JPA와 무관하게 잘 동작하는 Rich Entity도 과잉 분리 |
| D/P/M을 동등한 축으로 유지 | 상세 진단 가능 | 분리 이유와 구현 비용이 섞여 복잡함 |
| D/P로 편익 판단 후 매핑 비용 사후 확인 | 원인과 비용이 분리되고 Task C-1, C-2를 모두 반영 | 사후 비용 질문은 여전히 사람이 경계를 해석해야 함 |

세 번째 방안을 추천한다.

## 근거와 한계

이 구조는 `task3/assignments/taskC-1.md:46`의 외부 기술 의존 문제를 P로 확인하고, `task3/assignments/taskC-1.md:141`과 `task3/assignments/taskC-2.md:8`의 매핑 비용 비교를 사후 체크로 둔다.

다만 P의 Lazy Loading 위험은 가정만으로 세면 안 된다. 업무 method가 mapped association을 실제로 탐색하는 코드, 실패 테스트, runtime observation처럼 확인 가능한 evidence가 있어야 한다. 단순 `@Entity` annotation 하나만으로 P=true가 되지는 않는다.
