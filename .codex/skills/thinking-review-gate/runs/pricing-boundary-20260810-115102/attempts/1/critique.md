## 문제 지점

- [공통 `PricingContext` 생성 흐름] 초안은 “애플리케이션 계층이 외부 쿠폰을 검증한 뒤 공통 컨텍스트를 만든다”고 설명하지만, 실제 코드 예시는 `PricingContext` 생성자와 `ContextPricingService.finalAmount(context)`만 보여준다. 사용자가 묻는 핵심 중 하나가 “공통 pricingContext를 쓴다는 것도 어떻게 한다는 거냐”이므로, 외부 DTO 또는 쿠폰 코드가 어디서 검증되고 어떤 값으로 `PricingContext`에 들어가는지 코드 흐름이 조금 더 필요하다.

- [`ContextRateDiscountPolicy`의 기준 금액] `ContextRateDiscountPolicy`는 할인 적용 여부를 `context.basePrice()`로 판단하고 실제 할인은 `currentAmount`에 적용한다. 현재 가정한 순서에서는 첫 번째 정책이라 문제가 드러나지 않지만, 정책 목록을 동적으로 조립하거나 순서를 바꿀 수 있다고 설명한 구조에서는 “조건은 원래 기준가, 계산은 현재 금액”이라는 규칙이 의도인지 불명확하다. 이 부분은 요구사항에 따라 `basePrice` 기준인지 `currentAmount` 기준인지 명시해야 한다.

- [정책 순회 방식의 숨은 전제] 공통 컨텍스트 방식은 정책 목록 순회가 장점이라고 설명하지만, 할인 순서가 금액에 영향을 주는 경우 목록 순서가 도메인 규칙이 된다. 초안은 앞에서 “순서는 요구사항으로 확정해야 한다”고 말하지만, `ContextPricingService` 예시에서는 `List<ContextDiscountPolicy>`의 순서를 누가 보장하는지, DI 자동 주입 순서에 기대면 위험하다는 점이 충분히 드러나지 않는다.

- [현재 코드와의 연결] 입력에는 현재 `TicketService`가 `DiscountPolicy.finalAmount(ticket.getPrice())`만 호출하고, `DiscountPolicy`가 5만 원 이상 10% 할인 구체 클래스라는 맥락이 있다. 초안은 설계 예시는 충분하지만, 이 예시가 현재 구조에서 어떤 변경 방향을 뜻하는지 한두 문장으로 직접 연결하면 사용자가 기존 코드와 비교해 검증하기 더 쉽다.

## 확인 필요

- 5만 원 이상 10% 할인 조건이 최초 기준 가격 기준인지, 직전 할인까지 반영된 현재 금액 기준인지 확인해야 한다.
- 쿠폰이 없거나 유효하지 않거나 외부 쿠폰 서버 호출이 실패할 때 “할인 없음”으로 처리할지, 결제를 중단할지, 예외로 올릴지 확인해야 한다.
- 정책 목록을 동적으로 조립해야 하는 실제 요구가 있는지 확인해야 한다. 없다면 공통 `PricingContext`와 다형성 목록은 현재 단계에서 과할 수 있다.

## 수정 제안

- 공통 `PricingContext` 방식에 `PricingApplicationService` 또는 `PricingContextFactory` 수준의 짧은 예시를 추가해 `couponCode -> CouponPort.validate -> Coupon -> PricingContext` 흐름을 보여준다.
- `ContextRateDiscountPolicy` 옆에 “이 예시는 기준 가격으로 할인 자격을 판단하는 가정”이라고 명시하거나, 요구가 현재 금액 기준이라면 `currentAmount < THRESHOLD_AMOUNT`로 바꿔 예시의 전제를 분명히 한다.
- `ContextPricingService`의 `policies`는 도메인에서 정한 순서대로 주입되어야 하며, 순서가 바뀌면 결과가 달라질 수 있다는 주의점을 추가한다.
- 결론부에 현재 코드 기준으로는 `DiscountPolicy.finalAmount(price)`를 곧바로 거대한 `PricingContext` 계약으로 확장하기보다, 먼저 `PricingService`가 필요한 데이터와 좁은 규칙을 조합하는 방향이 자연스럽다는 연결 문장을 보강한다.

## 요약

초안은 두 접근의 차이, 예시 코드, trade-off를 전반적으로 잘 제시한다. 다만 사용자가 특히 궁금해한 공통 `PricingContext`의 생성 흐름이 코드로 덜 드러나고, 정책 순회 방식에서 할인 기준 금액과 정책 순서 보장이라는 숨은 전제가 남아 있다. 이 두 지점을 보강하면 답변의 검증 가능성이 더 좋아진다.