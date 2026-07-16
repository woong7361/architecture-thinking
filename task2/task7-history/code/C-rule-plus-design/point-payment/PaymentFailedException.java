package com.thinking.ticket;

/**
 * 결제사가 청구를 거절했다.
 *
 * <p>청구는 이미 시도된 뒤다. 다만 저장은 아직이므로 티켓은 아무에게도 확정되지 않은 채로 남는다.
 *
 * <p>티켓을 담지 않는다. 이 실패를 아는 것은 {@link CardPaymentAdapter}이고, 결제 담당은 금액만 알 뿐
 * 그 돈이 어느 티켓 값인지 모른다 — 알 필요도 없다.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(int amount) {
        super("결제가 거절되었습니다. 금액: " + amount);
    }
}
