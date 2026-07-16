package com.thinking.ticket;

/**
 * 자기가 티켓을 몇 장까지 가질 수 있는지 아는 회원.
 *
 * <p>"한 회원은 티켓을 한 장만 가질 수 있다"는 불변식은 보유 수를 가진 이 객체 안에서 강제한다.
 * 바깥이 보유 수를 꺼내 세어보고 판단하면(Ask) 그 규칙은 묻는 곳마다 흩어진다.
 *
 * <p>{@link Ticket}과 마찬가지로 저장소에서 읽은 값을 복원해 다루는 스냅샷이다. 보유의 기록은
 * 이 객체가 아니라 티켓의 저장으로 남으므로, 보유 수는 여기서 바뀌지 않는다.
 */
final class TicketHolder {

    /** 한 회원이 가질 수 있는 티켓 수. */
    private static final int LIMIT = 1;

    private final long userId;
    private final int held;

    private TicketHolder(long userId, int held) {
        this.userId = userId;
        this.held = held;
    }

    static TicketHolder of(long userId, int held) {
        return new TicketHolder(userId, held);
    }

    /** 티켓을 한 장 더 가져간다. 제한을 넘으면 거부한다. */
    void takeOneMore() {
        if (held >= LIMIT) {
            throw new TicketLimitExceededException(
                    "이미 티켓을 가진 회원이다: " + userId + " (보유 " + held + "장, 제한 " + LIMIT + "장)");
        }
    }
}
