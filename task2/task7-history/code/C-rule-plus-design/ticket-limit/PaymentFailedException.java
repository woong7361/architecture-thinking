package com.thinking.ticket;

/**
 * 결제사가 청구를 거절했다.
 *
 * <p>청구는 이미 시도된 뒤다. 다만 저장은 아직이므로 티켓은 아무에게도 확정되지 않은 채로 남는다.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(long ticketId, int amount) {
        super("결제가 거절되었습니다. 티켓: " + ticketId + ", 금액: " + amount);
    }
}
