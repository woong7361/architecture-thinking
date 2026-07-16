package com.thinking.ticket;

/**
 * 한 회원이 지금 가지고 있는 티켓들.
 *
 * <p>"한 회원은 한 장만 가진다"는 규칙은 그 회원이 몇 장을 가졌는지를 아는 쪽만 지킬 수 있다.
 * 티켓 한 장은 자기가 잡혔는지만 알 뿐 남의 보유분을 모르고, 창구가 대신 세어 검사하면
 * 규칙이 상태 밖으로 샌다. 그래서 보유분을 가진 이 객체가 스스로 지킨다.
 *
 * <p>보유분이 느는 유일한 통로가 {@link #take}이므로, 규칙을 건너뛰고 한 장을 더 잡을 길이 없다.
 * 몇 장까지인지는 이 객체 안에만 있다.
 */
public final class Holdings {

    private static final int LIMIT = 1;

    private final long userId;
    private final int held;

    public Holdings(long userId, int held) {
        this.userId = userId;
        this.held = held;
    }

    /**
     * 이 회원 앞으로 티켓 한 장을 더 잡아 돌려준다. 이미 한도를 채웠으면 거부한다.
     *
     * <p>그 티켓이 잡을 수 있는 것인지는 티켓 자신이 판단한다 — 여기서 대신 보지 않는다.
     */
    public Ticket take(Ticket ticket) {
        if (held >= LIMIT) {
            throw new TicketLimitExceededException(userId);
        }
        return ticket.reserveFor(userId);
    }
}
