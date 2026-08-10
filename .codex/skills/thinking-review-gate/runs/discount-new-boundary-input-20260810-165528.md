# Original User Input

그렇게 하는거 좋다 근데 그러면 new가 의존성이 생기잖아 이것도 분리할 방법은 없나?

# Confirmed Context

- 직전 예시는 `new PricingService(new RateDiscountPolicy())`처럼 조립 지점에서 구체 정책을 생성했다.
- 현재 `TicketService`는 생성자로 저장소, 결제 포트, 구체 `DiscountPolicy`를 주입받는다.
- 현재 과제는 Spring 없이 동작하는 작은 Java kata다.
- 할인 구현을 런타임에 선택해야 한다는 요구는 아직 확인되지 않았다.

# Constraints

- 현재 코드를 수정하지 않는다.
- `new` 자체와 구체 구현 의존을 구분한다.
- 수동 Composition Root, 런타임 Factory 또는 Resolver, Spring DI를 비교한다.
- 단순한 현재 kata에 적합한 방식을 추천한다.
