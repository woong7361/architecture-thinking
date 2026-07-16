package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserRecord;
import com.thinking.ticket.provided.UserStore;

/**
 * 예매 유스케이스의 진입점.
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

    /**
     * 회원에게 티켓을 확정하고 티켓 가격을 청구한다.
     *
     * <p>거부 사유는 청구가 일어나기 전에 모두 판단한다. 청구가 거절되면 확정도 저장도 하지 않는다.
     *
     * @return 예매에 성공하면 true. 실패는 예외로 알린다.
     * @throws UserNotFoundException 등록되지 않은 회원이다
     * @throws TicketAlreadyReservedException 이미 예매된 티켓이다
     * @throws PaymentFailedException 결제사가 청구를 거절했다
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        UserRecord user = users.findById(userId);
        if (user == null) {
            throw new UserNotFoundException("등록되지 않은 회원입니다: " + userId);
        }

        Ticket ticket = Ticket.from(tickets.findById(ticketId));
        ticket.requireReservable();

        if (!payments.charge(paymentInfo, ticket.price())) {
            throw new PaymentFailedException("청구가 거절되었습니다: 티켓 " + ticketId);
        }

        ticket.reserveFor(userId);
        tickets.save(ticket.toRecord());
        return true;
    }
}
