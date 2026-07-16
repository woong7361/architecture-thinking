package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

/**
 * 예매 유스케이스의 진입점.
 *
 * <p>규칙을 스스로 판단하지 않고, 그 규칙을 아는 쪽에게 시킨 뒤 바깥 세계와의 입출력(조회·청구·저장)만
 * 순서대로 엮는다. 티켓 예약 규칙이 바뀌면 {@link Ticket}이 바뀌고, 유스케이스의 흐름이 바뀌면 여기가 바뀐다.
 */
public class TicketService {

    private final TicketStore tickets;
    private final UserStore users;
    private final PaymentApi payments;

    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments) {
        this.tickets = tickets;
        this.users = users;
        this.payments = payments;
    }

    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        if (users.findById(userId) == null) {
            throw new UserNotFoundException("등록되지 않은 회원이다: " + userId);
        }

        Ticket ticket = Ticket.from(tickets.findById(ticketId));
        ticket.reserveFor(userId);

        if (!payments.charge(paymentInfo, ticket.price())) {
            throw new PaymentFailedException("청구가 거절되었다. 티켓: " + ticketId);
        }

        tickets.save(ticket.toRecord());
        return true;
    }
}
