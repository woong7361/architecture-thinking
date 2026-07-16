package com.thinking.ticket;

/** 이미 티켓을 가진 회원이 추가 예매를 시도했다. */
public class TicketLimitExceededException extends RuntimeException {

    public TicketLimitExceededException(long userId) {
        super("회원은 티켓을 한 장만 가질 수 있습니다: " + userId);
    }
}
