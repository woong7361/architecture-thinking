분리할 수 있어. 다만 `new`와 의존성을 완전히 없애는 것이 아니라 **구체 객체를 사용하는 코드에서 애플리케이션을 조립하는 경계로 옮긴다.** 집 안의 전선을 없애는 것이 아니라 가전제품 안에서 배전반으로 모으는 것과 비슷하다.

다음처럼 `PricingService` 안에서 직접 생성하면 문제가 된다.

```java
final class PricingService {
    int finalAmount(int basePrice) {
        DiscountPolicy policy = new RateDiscountPolicy();
        return policy.finalAmount(basePrice);
    }
}
```

`PricingService`가 `RateDiscountPolicy`를 직접 알아서 구현을 교체하거나 테스트 대역을 넣기 어렵다.

반면 다음 구조에서는 비즈니스 코드가 인터페이스만 알고, 구체 구현은 바깥 조립 지점만 안다.

```text
ApplicationConfiguration -> RateDiscountPolicy
                         -> PricingService -> DiscountPolicy
```

## 1. 수동 Composition Root: 현재 kata에 추천

```java
interface DiscountPolicy {
    int finalAmount(int basePrice);
}

final class PricingService {
    private final DiscountPolicy discountPolicy;

    PricingService(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    int finalAmount(int basePrice) {
        return discountPolicy.finalAmount(basePrice);
    }
}
```

애플리케이션 시작점에서만 객체를 만든다.

```java
public final class ApplicationConfiguration {
    public PricingService pricingService() {
        DiscountPolicy policy = new RateDiscountPolicy();
        return new PricingService(policy);
    }
}
```

여기에는 여전히 `new RateDiscountPolicy()`가 있다. 하지만 구체 구현을 반드시 선택해야 하는 조립 경계이므로 의도적인 의존이다. `PricingService`는 구체 정책을 모른다.

얻는 것은 비즈니스 코드의 분리와 테스트 용이성이다. 비용은 작은 설정 클래스 하나가 생긴다는 점이다. 정책이 애플리케이션 시작 시 하나로 정해지는 현재 kata에 적합하다.

테스트에서는 원하는 구현을 바로 넣는다.

```java
DiscountPolicy noDiscount = basePrice -> basePrice;
PricingService service = new PricingService(noDiscount);
```

## 2. 요청마다 정책을 선택한다: Resolver 또는 Factory

할인 종류가 요청 데이터로 결정된다면 시작할 때 구현들을 생성하고 Resolver가 선택하게 할 수 있다.

```java
enum DiscountType {
    RATE, FIXED
}

final class DiscountPolicyResolver {
    private final java.util.Map<DiscountType, DiscountPolicy> policies;

    DiscountPolicyResolver(
            java.util.Map<DiscountType, DiscountPolicy> policies
    ) {
        this.policies = java.util.Map.copyOf(policies);
    }

    DiscountPolicy resolve(DiscountType type) {
        DiscountPolicy policy = policies.get(type);
        if (policy == null) {
            throw new IllegalArgumentException("unsupported discount type: " + type);
        }
        return policy;
    }
}
```

조립 지점은 다음과 같다.

```java
DiscountPolicyResolver resolver = new DiscountPolicyResolver(
        java.util.Map.of(
                DiscountType.RATE, new RateDiscountPolicy(),
                DiscountType.FIXED, new FixedDiscountPolicy()
        )
);
```

서비스는 요청에 맞는 전략 하나를 선택해 한 번 호출한다. Resolver는 매번 새 객체를 만들 필요가 없다.

얻는 것은 런타임 선택이다. 비용은 정책 종류와 선택 규칙이라는 개념이 추가된다. 요청마다 구현이 달라진다는 실제 요구가 있을 때만 적합하다. 단순히 `new`를 숨기기 위한 Factory라면 계층만 늘어난다.

## 3. Spring DI: Spring 애플리케이션일 때

```java
@org.springframework.context.annotation.Configuration
class PricingConfiguration {
    @org.springframework.context.annotation.Bean
    DiscountPolicy discountPolicy() {
        return new RateDiscountPolicy();
    }

    @org.springframework.context.annotation.Bean
    PricingService pricingService(DiscountPolicy discountPolicy) {
        return new PricingService(discountPolicy);
    }
}
```

Spring이 Bean 생성과 연결을 실행하지만 `new`와 구체 구현 의존이 사라진 것은 아니다. 설정 클래스와 컨테이너로 이동했을 뿐이다.

얻는 것은 애플리케이션 전체의 일관된 생명주기와 조립 관리다. 비용은 프레임워크 설정과 여러 구현이 있을 때의 선택 규칙이다. 이미 Spring을 사용하는 애플리케이션에 적합하며, 현재 kata에 Spring을 추가할 이유는 없다.

## 결론

`new`는 시스템 어딘가에는 반드시 존재한다. 판단 기준은 위치다.

- 도메인 서비스나 유스케이스 내부의 `new ConcretePolicy()`는 피한다.
- 애플리케이션 시작점의 `new ConcretePolicy()`는 허용한다.
- 요청마다 구현 선택이 달라질 때만 Resolver를 둔다.
- 이미 Spring을 사용한다면 Bean 설정을 조립 경계로 삼는다.

따라서 현재 문맥에서는 `ApplicationConfiguration` 같은 수동 Composition Root가 가장 적절하다. Factory나 Spring은 `new`를 없애기 위해 도입하는 것이 아니라, 런타임 선택이나 애플리케이션 규모라는 추가 문제가 생길 때 선택한다.
