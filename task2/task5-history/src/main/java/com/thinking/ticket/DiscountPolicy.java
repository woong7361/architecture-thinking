package com.thinking.ticket;

/* 가격 정책: 임계 금액 이상이면 비율 할인. 규칙을 스스로 소유한다.
 * 할인 '종류'가 하나뿐(v=1)이라 인터페이스/전략으로 다형화하지 않는다(YAGNI).
 * 2번째 할인 종류가 확정되면 그때 인터페이스를 추출한다(Replace Conditional with Polymorphism). */
public class DiscountPolicy {

    private static final int THRESHOLD_AMOUNT = 50_000;
    private static final int DISCOUNT_PERCENT = 10;

    public int finalAmount(int basePrice) {
        if (basePrice >= THRESHOLD_AMOUNT) {
            return basePrice - (basePrice * DISCOUNT_PERCENT / 100);
        }
        return basePrice;
    }
}
