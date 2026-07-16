package com.thinking.ticket;

/**
 * 한 회원이 가진 티켓.
 *
 * <p>"한 회원은 한 장만 가진다"가 제약하는 상태는 티켓 한 장이 아니라 그 회원의 보유분이다.
 * 티켓은 자기가 잡혔는지만 알 뿐 그 회원이 다른 데서 몇 장을 가졌는지 모르므로,
 * 이 규칙을 티켓에게도 창구에게도 맡길 수 없다. 그 상태를 가진 이 객체가 스스로 지킨다.
 *
 * <p>몇 장을 가졌는지를 밖으로 열지 않는다. 세어 보고 밖에서 판단하게 두면 규칙이 새어나간다.
 * 보유분은 {@link #take}라는 의미 있는 행위로만 늘어난다.
 */
public final class Holding {

    /** 1인 1매. */
    private static final int LIMIT = 1;

    private final long userId;
    private final int held;

    public Holding(long userId, int held) {
        this.userId = userId;
        this.held = held;
    }

    /**
     * 이 티켓을 회원 앞으로 받아 잡은 티켓을 돌려준다. 이미 한도만큼 가지고 있으면 거부한다.
     *
     * <p>한도는 보유분이, 그 티켓이 이미 잡혔는지는 티켓이 각자 본다. 여기서 대신 보지 않는다.
     */
    public Ticket take(Ticket ticket) {
        if (held >= LIMIT) {
            throw new TicketLimitExceededException(userId);
        }
        return ticket.reserveFor(userId);
    }
}
