맞아. **현재 Task B-5 문맥에서는 순회 구조가 불필요하다.** 직전의 `List<DiscountPolicy>` 예시는 여러 할인을 누적하고 목록을 동적으로 바꾼다는 요구가 있을 때의 파이프라인이었다. 그런 요구가 없다면 소스만 복잡해진다는 지적이 맞다.

다만 순회와 다형성은 대체 관계는 아니다.

- 순회는 여러 정책을 실행하는 제어 흐름이다.
- 다형성은 각 객체의 구체 타입을 검사하지 않고 같은 메시지를 보내는 호출 방식이다.

```java
for (DiscountPolicy policy : policies) {
    amount = policy.apply(amount, context);
}
```

이 코드에서도 `policy.apply`는 다형적 호출이다. 하지만 여러 정책을 실행해야 한다는 요구가 없다면 이 루프 자체가 필요 없다.

## 1. 요청마다 정책 하나만 선택한다: Strategy

비율 할인과 정액 할인 중 하나만 적용한다면 정책 하나를 주입하고 한 번 호출하면 된다.

```java
interface DiscountPolicy {
    int finalAmount(int basePrice);
}

final class RateDiscountPolicy implements DiscountPolicy {
    private static final int DISCOUNT_PERCENT = 10;

    @Override
    public int finalAmount(int basePrice) {
        return basePrice - basePrice * DISCOUNT_PERCENT / 100;
    }
}

final class FixedDiscountPolicy implements DiscountPolicy {
    private static final int DISCOUNT_AMOUNT = 5_000;

    @Override
    public int finalAmount(int basePrice) {
        return Math.max(0, basePrice - DISCOUNT_AMOUNT);
    }
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

조립할 때 필요한 구현 하나를 선택한다.

```java
PricingService pricingService =
        new PricingService(new RateDiscountPolicy());
```

이것이 전형적인 Strategy다. 얻는 것은 구현 교체와 타입 분기 제거다. 비용은 구현들이 같은 입력과 같은 결과 계약을 공유해야 한다는 점이다. 요청마다 정확히 하나의 정책만 적용할 때 추천한다.

## 2. 여러 규칙을 정해진 순서로 항상 적용한다: 명시적 조합

기본 할인, VIP 할인, 쿠폰 할인을 항상 같은 순서로 적용한다면 순회보다 호출 순서를 코드에 드러내는 편이 낫다.

```java
int finalAmount(PricingCommand command) {
    int amount = rateDiscount.apply(command.basePrice());
    amount = vipDiscount.apply(amount, command.grade());
    return couponDiscount.apply(amount, command.coupon());
}
```

얻는 것은 실행 순서와 규칙별 입력이 명확하다는 점이다. 비용은 규칙 추가 시 `PricingService`를 수정해야 한다는 점이다. 하지만 정책 조합이 고정돼 있다면 이 수정은 결함이 아니라 실제 도메인 흐름 변경을 코드에 반영하는 것이다.

## 3. 적용할 규칙과 순서가 런타임에 달라진다: 파이프라인

고객군, 캠페인 설정, 상품에 따라 적용할 규칙 목록이 매번 달라진다는 요구가 있을 때만 순회가 의미를 가진다.

```java
final class DiscountPipeline {
    private final java.util.List<DiscountPolicy> policies;

    DiscountPipeline(java.util.List<DiscountPolicy> policies) {
        this.policies = java.util.List.copyOf(policies);
    }

    int finalAmount(int basePrice) {
        int amount = basePrice;
        for (DiscountPolicy policy : policies) {
            amount = policy.finalAmount(amount);
        }
        return amount;
    }
}
```

이 경우 루프는 단순히 다형성을 보여주기 위한 장식이 아니라 런타임에 정해진 복수 규칙을 실행하는 요구를 표현한다. 얻는 것은 동적 조합 가능성이다. 비용은 실행 순서, 중복 적용, 규칙 간 상호작용을 별도로 통제해야 한다는 점이다. 이것은 Strategy 하나를 고르는 구조라기보다 규칙 파이프라인 또는 Chain에 가깝다.

## 현재 과제에 적용

현재는 비율 할인 구체 클래스 하나뿐이다. 따라서 다음 순서가 맞다.

1. 지금은 현재 구체 `DiscountPolicy`를 그대로 둔다.
2. 같은 입력을 받는 대체 할인 중 하나를 선택해야 할 때 Strategy 인터페이스를 도입한다.
3. 여러 할인을 고정 순서로 누적해야 한다면 `PricingService`가 명시적으로 조합한다.
4. 적용할 정책 목록과 순서가 실제로 동적일 때만 순회 기반 파이프라인을 검토한다.

판단표로 줄이면 다음과 같다.

| 한 요청에 적용하는 정책 | 구성 | 적합한 구조 |
|---|---|---|
| 정확히 하나 | 구현만 교체 | Strategy, 한 번 호출 |
| 여러 개 | 집합과 순서 고정 | 명시적 조합 |
| 여러 개 | 집합이나 순서가 동적 | 다형성 목록 순회 |

따라서 사용자의 반론은 현재 문맥에 대해서는 맞다. 직전 예시의 공통 `PricingContext`와 정책 순회는 동적 복수 할인이라는 요구가 확인된 뒤에만 정당화된다. 지금 도입하면 과설계다.
