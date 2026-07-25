# 변경 초안

## 1. `AGENTS.md`에 추가할 규칙

`설명 요청 응답 형식` 앞에 다음 섹션을 추가한다.

```md
## 요약·리포트 작성 원칙

- 요약, 리포트, 과제 답안은 앞선 논의와 원문을 보지 않은 독자도 이해할 수 있게 작성한다.
- 분량을 줄이는 것보다 핵심 인과를 보존하는 것을 우선한다. 필요한 맥락, 구체적인 상황, 원인과 결과, 결론 순서로 독자가 사고의 흐름을 따라갈 수 있게 구성한다.
- 추상적인 주장이나 판단에는 코드, 시나리오, 전후 비교 중 가장 적합한 구체 예시를 최소 하나 포함한다. 예시는 장식이 아니라 주장을 어떻게 확인할 수 있는지 보여줘야 한다.
- 전문 용어는 독자가 이미 안다고 가정하지 않는다. 꼭 필요한 용어만 사용하고, 처음 등장할 때 문맥 안에서 뜻을 풀어 쓴다.
- 표나 다이어그램은 관계나 흐름을 글보다 분명하게 보여줄 때만 사용한다.
```

이 문구는 독자 관점, 인과 보존, 예시 의무를 일반 규칙으로 만들며 특정 과제나 도메인에 고정되지 않는다.

## 2. `taskC-1.md`에 추가할 예시

기존 Testability와 Flexibility 설명 다음에 아래 예시를 추가한다.

````md
### 예시: 주문 조회에서 JPA 엔티티가 전 계층에 공유된 경우

```java
@Entity
class Order {
    @OneToMany(fetch = FetchType.LAZY)
    private List<OrderItem> items;

    Money totalPrice() {
        return items.stream()
                .map(OrderItem::price)
                .reduce(Money.ZERO, Money::add);
    }
}
```

Repository가 반환한 `Order`를 Service가 사용하고 Controller가 그대로 응답한다고 하자. 단위 테스트에서는 `items`에 평범한 `List`를 넣으므로 `totalPrice()`가 통과한다. 그러나 실제 실행에서는 `items`가 Hibernate의 지연 로딩 컬렉션일 수 있다. 영속성 컨텍스트가 닫힌 뒤 Controller가 `items`에 접근하면 조회가 실패할 수 있고, 컨텍스트가 열려 있더라도 예상하지 못한 추가 쿼리가 실행될 수 있다. Mock을 사용한 테스트는 이 차이를 재현하지 못하므로 JPA를 포함한 통합 테스트가 추가로 필요해진다.

변경에도 같은 결합이 드러난다. API가 `Order`를 그대로 응답하고 있다면 테이블 관계를 바꾸려고 `items` 매핑을 수정한 일이 Repository 안에서 끝나지 않는다. Service의 계산 방식과 Controller의 응답 구조까지 영향을 받을 수 있다. 저장 기술의 세부 모델이 업무와 API의 공용 계약이 되었기 때문이다.

JPA가 없어도 원리는 같다. `OrderService -> JdbcOrderDao -> OrderRow` 구조에서 `OrderRow`를 Controller까지 공유하면, DB 컬럼에 맞춘 `statusCode` 변경이 Service와 API로 전파된다. 따라서 문제의 뿌리는 특정 ORM이 아니라 도메인 정책이 하위 기술의 모델을 직접 공유하는 의존 방향에 있다.
````

## 3. 최종 응답 초안

`taskC-1.md`에 JPA 누수와 일반 N-Tier 문제를 대비하는 예시를 추가했고, 루트 `AGENTS.md`에 요약·리포트의 독자 관점 작성 원칙을 일반 규칙으로 반영했다고 보고한다. 변경 파일과 검증 결과를 링크한다.
