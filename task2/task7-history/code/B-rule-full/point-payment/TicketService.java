package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.PointApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

/**
 * 예매 유스케이스의 진입점.
 *
 * <p>규칙을 스스로 판단하지 않고, 그 규칙을 아는 쪽에게 시킨 뒤 바깥 세계와의 입출력(조회·결제·저장)만
 * 순서대로 엮는다. 티켓 예약 규칙이 바뀌면 {@link Ticket}이, 결제 수단이 늘면 {@link PaymentMethods}가,
 * 유스케이스의 흐름이 바뀌면 여기가 바뀐다.
 *
 * <p>흐름은 무엇으로 결제하든 같다 — 회원을 확인하고, 티켓을 그 회원 앞으로 예약하고, 값을 받아내고,
 * 받아낸 뒤에만 확정을 남긴다. 그래서 카드가 늘어 포인트가 되어도 이 순서는 손대지 않았다.
 */
public class TicketService {

    private final TicketStore tickets;
    private final UserStore users;
    private final PaymentMethods paymentMethods;

    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments, PointApi points) {
        this.tickets = tickets;
        this.users = users;
        this.paymentMethods = new PaymentMethods(payments, points);
    }

    /** 카드 결제. */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        return reserveTicket(userId, ticketId, PaymentMethods.CARD, paymentInfo);
    }

    /** {@code paymentMethod}는 "CARD" 또는 "POINT". "POINT"이면 {@code paymentInfo}는 쓰이지 않는다. */
    public boolean reserveTicket(long userId, long ticketId, String paymentMethod, String paymentInfo) {
        if (users.findById(userId) == null) {
            throw new UserNotFoundException("등록되지 않은 회원이다: " + userId);
        }

        Ticket ticket = Ticket.from(tickets.findById(ticketId));
        ticket.reserveFor(userId);

        PaymentMethod payment = paymentMethods.select(paymentMethod, userId, paymentInfo);
        payment.pay(ticket.price());

        tickets.save(ticket.toRecord());
        return true;
    }
}
