맞아. 더 정확히 말하면 **무엇이 공통이고 무엇이 서로 독립적으로 변하는지를 보고 경계를 정한다**는 뜻이야. 이름이 모두 "할인"이라는 사실만으로 같은 인터페이스에 넣는 것은 충분하지 않다.

아래 예시는 설명을 위해 다음 순서를 가정한다.

1. 5만 원 이상이면 10% 할인
2. VIP면 할인 후 금액에서 추가 5% 할인
3. 검증된 쿠폰 금액 차감

실제 구현에서는 이 순서와 중복 적용 가능 여부를 요구사항으로 먼저 확정해야 한다.

## 1. 가격 계산 서비스가 좁은 규칙을 조합하는 방식

```java
enum CustomerGrade {
    NORMAL, VIP
}

record Coupon(int discountAmount) {
    Coupon {
        if (discountAmount < 0) {
            throw new IllegalArgumentException("discountAmount must not be negative");
        }
    }

    static Coupon none() {
        return new Coupon(0);
    }
}

record PricingCommand(int basePrice, CustomerGrade grade, String couponCode) {
}

interface CouponPort {
    Coupon validate(String couponCode);
}
```

`CouponPort` 구현체가 외부 서버 응답을 검증하고 도메인 값인 `Coupon`으로 변환한다. 외부 서버 DTO는 가격 규칙까지 들어오지 않는다.

각 규칙은 실제로 필요한 입력만 받는다.

```java
final class RateDiscount {
    private static final int THRESHOLD_AMOUNT = 50_000;
    private static final int DISCOUNT_PERCENT = 10;

    int apply(int amount) {
        if (amount < THRESHOLD_AMOUNT) {
            return amount;
        }
        return amount - amount * DISCOUNT_PERCENT / 100;
    }
}

final class VipDiscount {
    private static final int DISCOUNT_PERCENT = 5;

    int apply(int amount, CustomerGrade grade) {
        if (grade != CustomerGrade.VIP) {
            return amount;
        }
        return amount - amount * DISCOUNT_PERCENT / 100;
    }
}

final class CouponDiscount {
    int apply(int amount, Coupon coupon) {
        return Math.max(0, amount - coupon.discountAmount());
    }
}
```

`PricingService`가 외부 협력과 규칙 적용 순서를 조정한다.

```java
final class PricingService {
    private final CouponPort couponPort;
    private final RateDiscount rateDiscount;
    private final VipDiscount vipDiscount;
    private final CouponDiscount couponDiscount;

    PricingService(
            CouponPort couponPort,
            RateDiscount rateDiscount,
            VipDiscount vipDiscount,
            CouponDiscount couponDiscount
    ) {
        this.couponPort = couponPort;
        this.rateDiscount = rateDiscount;
        this.vipDiscount = vipDiscount;
        this.couponDiscount = couponDiscount;
    }

    int finalAmount(PricingCommand command) {
        int amount = rateDiscount.apply(command.basePrice());
        amount = vipDiscount.apply(amount, command.grade());
        return couponDiscount.apply(amount, resolveCoupon(command.couponCode()));
    }

    private Coupon resolveCoupon(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return Coupon.none();
        }
        return couponPort.validate(couponCode);
    }
}
```

흐름은 다음과 같다.

```text
TicketService
  -> PricingService
       -> CouponPort             외부 I/O
       -> RateDiscount           가격만 사용
       -> VipDiscount            가격 + 등급 사용
       -> CouponDiscount         가격 + 검증된 쿠폰 사용
  -> ChargePort
```

이 구조에서는 `PricingCommand`가 여러 입력을 갖지만, 이는 가격 계산 유스케이스 전체가 실제로 필요로 하는 입력이다. 각 도메인 규칙에 모든 입력을 강제로 전달하지는 않는다.

장점은 규칙별 입력과 책임이 선명하다는 점이다. 비용은 `PricingService`가 적용 순서와 조합 정책을 명시적으로 알아야 한다는 점이다. 규칙들의 입력과 외부 협력이 서로 많이 다를 때 추천한다.

## 2. 공통 PricingContext로 정책을 순회하는 방식

먼저 애플리케이션 계층이 외부 쿠폰을 검증한 뒤 공통 컨텍스트를 만든다.

```java
record PricingContext(
        int basePrice,
        CustomerGrade grade,
        Coupon coupon
) {
}

interface ContextDiscountPolicy {
    int apply(int currentAmount, PricingContext context);
}
```

모든 정책은 같은 계약을 구현한다.

```java
final class ContextRateDiscountPolicy implements ContextDiscountPolicy {
    private static final int THRESHOLD_AMOUNT = 50_000;
    private static final int DISCOUNT_PERCENT = 10;

    @Override
    public int apply(int currentAmount, PricingContext context) {
        if (context.basePrice() < THRESHOLD_AMOUNT) {
            return currentAmount;
        }
        return currentAmount - currentAmount * DISCOUNT_PERCENT / 100;
    }
}

final class ContextVipDiscountPolicy implements ContextDiscountPolicy {
    private static final int DISCOUNT_PERCENT = 5;

    @Override
    public int apply(int currentAmount, PricingContext context) {
        if (context.grade() != CustomerGrade.VIP) {
            return currentAmount;
        }
        return currentAmount - currentAmount * DISCOUNT_PERCENT / 100;
    }
}

final class ContextCouponDiscountPolicy implements ContextDiscountPolicy {
    @Override
    public int apply(int currentAmount, PricingContext context) {
        return Math.max(0, currentAmount - context.coupon().discountAmount());
    }
}
```

가격 계산기는 정책의 구체 종류를 모른 채 목록을 순회한다.

```java
final class ContextPricingService {
    private final java.util.List<ContextDiscountPolicy> policies;

    ContextPricingService(java.util.List<ContextDiscountPolicy> policies) {
        this.policies = java.util.List.copyOf(policies);
    }

    int finalAmount(PricingContext context) {
        int amount = context.basePrice();
        for (ContextDiscountPolicy policy : policies) {
            amount = policy.apply(amount, context);
        }
        return amount;
    }
}
```

흐름은 다음과 같다.

```text
PricingContext 생성
  -> ContextPricingService
       -> ContextDiscountPolicy 1
       -> ContextDiscountPolicy 2
       -> ContextDiscountPolicy 3
```

장점은 정책 목록을 설정으로 조립하거나 새로운 정책을 같은 방식으로 추가하기 쉽다는 점이다. 비용은 비율 정책이 등급과 쿠폰을, VIP 정책이 쿠폰을 사용하지 않으면서도 같은 컨텍스트를 받는다는 점이다.

다음 조건이면 공통 컨텍스트가 비교적 자연스럽다.

- 정책 대부분이 같은 사실들을 실제로 사용한다.
- 정책을 목록으로 동적 조립해야 한다.
- 컨텍스트는 이미 확보된 불변 데이터이며 생성 자체가 불필요한 외부 호출을 유발하지 않는다.

반대로 대부분의 정책이 컨텍스트 필드 일부만 사용하거나, 컨텍스트를 만들기 위해 모든 요청에서 VIP 조회와 쿠폰 서버 호출을 해야 한다면 경계를 잘못 잡았을 가능성이 크다.

## 경계를 잡는 기준

공통화는 한 번에 하나만 있는 것이 아니다.

| 경계 | 공통화하는 것 | 예시 |
|---|---|---|
| 유스케이스 입력 | 가격 계산 전체가 필요로 하는 요청 데이터 | `PricingCommand` |
| 외부 연동 | 쿠폰 검증이라는 I/O 역할 | `CouponPort` |
| 개별 규칙 | 한 규칙이 실제 사용하는 값 | `VipDiscount.apply(amount, grade)` |
| 다형성 계약 | 모든 구현이 공유하는 안정된 입력과 결과 | `ContextDiscountPolicy` |

따라서 사용자의 결론이 맞다. 다만 “공통화할 수 있는 것은 전부 한 경계로 묶는다”가 아니라, **같은 이유로 변하고 같은 계약을 지키는 것끼리 경계를 만든다**가 더 정확하다.

현재 FB-B5-01처럼 정책별 입력과 외부 협력이 크게 다른 가정에서는 첫 번째 방식이 더 안전하다. 공통 `PricingContext` 방식은 동적 정책 목록이라는 실제 필요가 생기고 공통 데이터 비율이 충분히 높을 때 선택하는 편이 낫다.
