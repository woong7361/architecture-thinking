package com.thinking.ticket;

/**
 * 카드로 값을 치르는 자.
 *
 * <p>결제사는 승인 여부만 답한다. 그 답을 "예매 실패"로 볼지는 결제사가 정할 일이 아니라
 * 이쪽이 정한다 — 거절이 무엇을 뜻하는지는 카드로 치른다는 사실을 아는 여기서만 안다.
 *
 * <p>바깥과 주고받는 일은 결제사 역할이 하고, 여기 있는 것은 그 답을 읽는 판단뿐이다.
 */
final class CardPayer implements Payer {

    private final Payments payments;
    private final String paymentInfo;

    CardPayer(Payments payments, String paymentInfo) {
        this.payments = payments;
        this.paymentInfo = paymentInfo;
    }

    @Override
    public void pay(int amount) {
        if (!payments.charge(paymentInfo, amount)) {
            throw new PaymentFailedException(amount);
        }
    }
}
