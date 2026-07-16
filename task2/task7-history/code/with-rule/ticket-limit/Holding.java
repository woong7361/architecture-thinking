package com.thinking.ticket;

/**
 * 한 회원이 지금 몇 장을 가지고 있는지, 그 보유 현황.
 *
 * <p>"한 회원은 한 장만 가질 수 있다"는 규칙을 보유 수를 가진 이 객체가 스스로 지킨다.
 * 몇 장을 가졌는지 바깥에 내주고 바깥이 세어보게 두면, 그 규칙은 묻는 곳마다 흩어진다.
 * 그래서 수를 여는 접근자를 두지 않고, {@link #take}로 한 장을 가져가는 행위만 연다.
 *
 * <p>바뀔 이유는 하나다: 회원이 몇 장까지 가질 수 있는지가 바뀔 때.
 * 티켓이 이미 잡혔는지는 티켓 자신의 규칙이므로 여기서 다시 보지 않는다.
 */
public final class Holding {

    /** 한 회원이 가질 수 있는 최대 장수. */
    private static final int LIMIT = 1;

    private final long userId;
    private final int held;

    public Holding(long userId, int held) {
        this.userId = userId;
        this.held = held;
    }

    /**
     * 이 회원이 티켓 한 장을 가져간 결과를 돌려준다. 이미 한도만큼 가지고 있으면 거부한다.
     *
     * <p>한도를 먼저 보고 티켓을 잡는다. 한도에 걸린 회원의 요청은 티켓에 닿지도 않는다.
     */
    public Ticket take(Ticket ticket) {
        if (held >= LIMIT) {
            throw new TicketLimitExceededException(userId);
        }
        return ticket.reserveFor(userId);
    }
}
