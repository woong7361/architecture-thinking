# Original User Input

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


# Checked Context

# Project Context

사용자는 Strategy 구현을 `new`로 생성하는 코드가 다시 구체 구현 의존성을 만든다고 지적했다. 시스템에서 구체 객체 생성은 완전히 사라질 수 없으며, 핵심은 객체를 사용하는 비즈니스 코드와 객체를 조립하는 시작 경계를 분리하는 것이다.

현재처럼 정책이 애플리케이션 시작 시 하나로 정해지는 작은 Java kata에서는 수동 Composition Root가 가장 단순하다. 요청 데이터에 따라 구현을 선택해야 하면 미리 생성된 구현을 반환하는 Resolver가 필요할 수 있다. Spring 애플리케이션이라면 Bean 설정을 Composition Root로 사용할 수 있지만, 현재 kata에 프레임워크를 추가할 근거는 없다.
