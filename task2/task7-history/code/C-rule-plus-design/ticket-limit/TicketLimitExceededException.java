package com.thinking.ticket;

/**
 * 이미 티켓을 가진 회원이 추가 예매를 시도했다.
 *
 * <p>{@link TicketHolding#claimAnother()}가 자기 불변식을 지키며 스스로 던진다.
 */
public class TicketLimitExceededException extends RuntimeException {

    public TicketLimitExceededException(long userId, int limit) {
        super("회원 " + userId + "은(는) 티켓을 " + limit + "장까지만 가질 수 있습니다");
    }
}
