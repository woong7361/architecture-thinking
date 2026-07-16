package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

/**
 * 예매 유스케이스. 협력의 순서를 안다.
 *
 * <p>예약 규칙 자체는 {@link Ticket}이 가진다. 이 클래스가 지는 책임은 순서다 — 거절될 수 있는
 * 판단을 모두 청구 앞에 두어, 못 파는 자리에 돈부터 걷는 일이 없게 한다.
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
     * 회원이 티켓 하나를 골라 예매한다. 성공하면 티켓은 그 회원의 것으로 확정되어 저장되고,
     * 티켓 가격이 청구된 상태가 된다.
     *
     * @return 예매에 성공하면 true
     * @throws UserNotFoundException          등록되지 않은 회원이면 (청구 없음)
     * @throws TicketLimitExceededException   이미 티켓을 가진 회원이면 (청구 없음)
     * @throws TicketAlreadyReservedException 이미 예약된 티켓이면 (청구 없음)
     * @throws PaymentFailedException         결제사가 청구를 거절하면 (티켓은 확정되지 않음)
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        if (users.findById(userId) == null) {
            throw new UserNotFoundException(userId);
        }

        // 요청자 자격에 대한 판단이라 회원 확인 옆에 온다. 대상을 보기도 전에 갈린다.
        TicketHolding.of(userId, tickets.countByUserId(userId)).claimAnother();

        Ticket ticket = Ticket.from(tickets.findById(ticketId));
        ticket.reserve(userId);

        // 거절될 수 있는 판단은 여기서 끝났다. 이제부터 처음으로 돈이 움직인다.
        if (!payments.charge(paymentInfo, ticket.price())) {
            throw new PaymentFailedException(ticketId, ticket.price());
        }
        tickets.save(ticket.toRecord());

        return true;
    }
}
