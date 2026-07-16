package com.thinking.ticket;

/**
 * 예매하는 회원.
 *
 * <p>"한 회원은 티켓을 한 장만 가진다"는 규칙을, 가진 티켓 수를 아는 이 객체가 스스로 지킨다.
 * 창구가 대신 세어보고 판단하면 규칙이 회원 바깥으로 새어나간다.
 *
 * <p>그래서 가진 수를 묻는 통로를 열지 않고, {@link #claim}이라는 의미 있는 행위로만 티켓을 잡는다.
 * 잡을 수 있는지는 회원이, 잡힐 수 있는지는 티켓이 각자 판단한다.
 */
public final class Member {

    /** 한 회원이 가질 수 있는 티켓 수. */
    private static final int TICKET_LIMIT = 1;

    private final long id;
    private final int ticketsHeld;

    public Member(long id, int ticketsHeld) {
        this.id = id;
        this.ticketsHeld = ticketsHeld;
    }

    /**
     * 이 회원 앞으로 잡은 티켓을 돌려준다. 이미 한도만큼 가지고 있으면 거부한다.
     *
     * <p>한도를 먼저 보므로, 한도에 걸린 회원의 예매는 티켓을 건드리지도 청구하지도 않는다.
     */
    public Ticket claim(Ticket ticket) {
        if (ticketsHeld >= TICKET_LIMIT) {
            throw new TicketLimitExceededException(id);
        }
        return ticket.reserveFor(id);
    }
}
