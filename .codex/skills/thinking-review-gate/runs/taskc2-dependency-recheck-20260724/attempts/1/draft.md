# 재검토 결론

사용자의 지적이 맞다. 이전 문장은 완전히 틀린 기준은 아니지만 Task C-1의 핵심 원인과 C-2의 보조 신호를 뒤집어 말했다. `DB 모델과 업무 모델이 얼마나 다르게 변하는가`는 분리 효용을 키우는 중요한 신호지만, 1차 판단 기준은 아니다.

Task C-1의 인과는 다음과 같다.

```text
Domain Policy가 JPA와 그 실행 의미를 향해 의존
    -> 업무 테스트에 Persistence Context, Proxy, Lazy Loading이 개입
    -> JPA/스키마 변경이 업무 코드로 전파
    -> Core가 Port를 소유하고 JPA Adapter가 바깥에서 구현하도록 의존 방향을 역전
```

따라서 Task C-2에서 mapper 비용을 지불하는 이유도 단순히 두 모델의 필드 모양이 달라서가 아니다. Domain Model과 JPA Entity를 분리하면 `Domain -> JPA` 의존을 끊고, JPA를 아는 책임과 런타임 생명주기를 persistence adapter 안으로 밀어낼 수 있다.

수정된 핵심 기준은 다음과 같다.

> 보호할 업무 규칙을 JPA의 타입과 실행 의미로부터 분리해 줄일 수 있는 테스트 비용과 변경 전파 비용이, mapper와 왕복 검증 비용보다 큰가?

여기에 `DB 모델과 업무 모델의 모양 및 변경 이유가 다른가`가 두 번째 판단 축으로 붙는다. 모양이 다르면 같은 의존성으로 인해 발생하는 변경 전파가 더 자주 나타나므로 분리 편익이 커진다.

## 왜 기존 기준만으로 부족했는가

### 모델 모양이 같아도 의존성 문제는 생긴다

`Ticket`과 `tickets` 테이블 필드가 1:1이라고 하자. 다음처럼 예약 가능 좌석을 계산한다면 표면적인 모양은 같다.

```java
@Entity
class Ticket {
    @OneToMany(fetch = FetchType.LAZY)
    private List<Seat> seats;

    public int availableSeatCount() {
        return (int) seats.stream()
                .filter(Seat::isAvailable)
                .count();
    }
}
```

그러나 `availableSeatCount()`의 실행 가능 여부와 쿼리 비용이 열린 Persistence Context와 Lazy Loading에 좌우된다. plain Java List를 넣은 테스트는 통과해도 실제 환경에서는 추가 쿼리나 초기화 실패가 생길 수 있다. 이 경우 모델 필드가 같더라도 업무 행위가 JPA 실행 의미에 의존하므로 Task C-1의 문제가 남아 있다.

분리하면 Domain `Ticket`은 완성된 `List<Seat>`만 받아 규칙을 실행하고, 어떤 조회로 그 상태를 구성할지는 JPA Adapter가 책임진다. 대신 Adapter가 Aggregate를 완전하게 복원했다는 계약과 mapper 테스트를 유지해야 한다.

### 모델 모양이 달라도 별도 Domain Entity가 필요하지 않을 수 있다

반대로 단순 상품 카탈로그가 legacy column과 API의 필드명이 다르더라도 행위가 거의 없는 CRUD라면 차이는 mapper나 projection DTO 하나로 흡수할 수 있다. 별도의 Rich Domain Entity까지 만들면 동일 데이터를 여러 클래스로 복사하는 비용만 늘 수 있다.

따라서 `모양이 다르다`는 분리의 충분조건이 아니며, `모양이 같다`는 통합의 충분조건도 아니다.

## Task C-1과 연결한 판단 순서

1. 보호할 업무 규칙이 있는가?
   - 상태 전이, 불변식, 사업적으로 중요한 계산이 거의 없으면 분리 편익이 작다.
2. 그 규칙이 JPA에 소스 또는 실행 의미로 의존하는가?
   - `jakarta.persistence` 타입과 Annotation, Proxy, Lazy Loading, managed/detached state, dirty checking을 전제로 하는지 본다.
3. 그 의존 때문에 실제 테스트 비용이나 변경 전파가 발생하는가?
   - Domain 테스트에 Spring/JPA가 필요하거나, fetch와 연관관계 변경이 업무 코드까지 흔드는지 본다.
4. DB와 Domain의 모양 또는 변경 이유가 다른가?
   - legacy code, 정규화된 관계, 여러 data source가 있으면 분리 편익이 더 커진다.
5. 위 편익이 mapper 비용보다 큰가?
   - ID, version, 자식 관계, 삭제, 부분 로딩을 왕복 보존하는 비용과 비교한다.

## 세 선택지

- 통합: 단순 CRUD이고 업무 규칙이 적으며 JPA 실행 의미가 Domain 판단에 거의 들어오지 않을 때 가장 경제적이다. 엄격한 Hexagonal 의존 규율은 일부 양보하는 의식적인 shortcut이다.
- 선택적 분리: 핵심 Aggregate만 JPA Entity와 나누고 기준정보나 단순 조회 모델은 통합하거나 projection을 사용한다. 현재 과제 문맥에 가장 적합한 기본안이다.
- 전면 분리: 여러 Aggregate가 모두 rich하고, legacy schema나 여러 저장소가 있으며, 팀이 경계를 독립적으로 소유할 때 검토한다. 그렇지 않으면 mapper와 테스트 비용이 빠르게 늘어난다.

## 고쳐 쓸 최종 문장

기존 문장보다 다음이 Task C-1과 일관된다.

> Domain 모델과 JPA Entity를 분리할지는 먼저 보호할 업무 규칙이 JPA의 타입과 실행 의미에 의존하면서 테스트 비용과 변경 전파를 만들고 있는지로 판단한다. DB 모델과 업무 모델의 모양과 변경 이유가 다르면 이 분리 편익은 더 커진다. 이 편익이 ID, version, 관계를 보존하는 mapper와 검증 비용보다 클 때 분리한다.

즉 의존성 문제가 원인이고, 모델 차이는 그 문제가 얼마나 자주 변경으로 드러날지를 보여주는 증폭 신호이며, 매핑 비용은 해결책의 대가다.
