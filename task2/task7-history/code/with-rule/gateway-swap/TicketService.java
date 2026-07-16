package com.thinking.ticket;

import com.thinking.ticket.infra.CardPayments;
import com.thinking.ticket.infra.RegisteredMembers;
import com.thinking.ticket.infra.StoredTickets;
import com.thinking.ticket.provided.PaymentGateway;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

/**
 * 계약이 고정한 진입점. 바깥이 건네는 기존 인프라를 역할에 맞춰 조립하고 창구에 넘긴다.
 *
 * <p>정책은 여기 없다. 바뀔 이유는 하나다: 바깥과 연결되는 방식이 바뀔 때.
 * 이 클래스만 provided를 알고, 창구 아래로는 내려보내지 않는다.
 *
 * <p>성공을 true로 돌려주는 것도 이 경계의 번역이다. 실패는 모두 예외로 나가므로
 * 창구는 성공/실패를 boolean으로 다루지 않는다.
 */
public class TicketService {

    private final ReservationDesk desk;

    public TicketService(TicketStore tickets, UserStore users, PaymentGateway payments) {
        this.desk = new ReservationDesk(
                new StoredTickets(tickets),
                new RegisteredMembers(users),
                new CardPayments(payments));
    }

    /** 예매 성공이면 true. 실패는 예외로 알린다. */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        desk.reserve(userId, ticketId, paymentInfo);
        return true;
    }
}
