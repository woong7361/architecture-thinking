package com.thinking.ticket;

/**
 * 예매 요청을 받아 협력을 이끄는 창구.
 *
 * <p>여기 있는 것은 순서뿐이다. 등록 여부는 명부가, 이미 잡혔는지는 티켓 자신이,
 * 값이 치러졌는지는 치르는 자가 각자 안다 — 창구는 그중 어느 것도 대신 판단하지 않는다.
 * 카드인지 포인트인지는 더더욱 알지 못한다. 그래서 수단이 하나 늘어도 이 절차는 그대로다.
 *
 * <p>협력 전체를 봐야 나오는 판단 하나는 창구의 몫이다: 값을 치르지 못하면 잡은 티켓을
 * 남기지 않는다. 그 판단은 아래 순서로 드러나 있다.
 *
 * <p>바뀔 이유는 하나다: 예매 절차가 바뀔 때. 바깥과 어떻게 연결되는지는 알지 못한다.
 */
final class ReservationDesk {

    private final Tickets tickets;
    private final Members members;

    ReservationDesk(Tickets tickets, Members members) {
        this.tickets = tickets;
        this.members = members;
    }

    void reserve(long userId, long ticketId, Payer payer) {
        if (!members.isRegistered(userId)) {
            throw new UserNotFoundException(userId);
        }

        Ticket ticket = tickets.byId(ticketId);

        // 잡아본 뒤에 치른다. 이미 잡힌 티켓이면 여기서 거부되므로 값은 치러지지 않는다.
        Ticket reserved = ticket.reserveFor(userId);

        payer.pay(ticket.price());

        // 남겨야 확정이다. 값을 치르지 못해 여기까지 못 오면 티켓은 아무에게도 확정되지 않은 채로 남는다.
        tickets.save(reserved);
    }
}
