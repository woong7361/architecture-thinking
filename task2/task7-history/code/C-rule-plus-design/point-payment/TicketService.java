package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.PointApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

/**
 * 예매 유스케이스. 협력의 순서를 안다.
 *
 * <p>예약 규칙 자체는 {@link Ticket}이, 결제수단 선택은 {@link PaymentMethods}가 가진다. 이 클래스가
 * 지는 책임은 순서다 — 거절될 수 있는 판단을 모두 청구 앞에 두어, 못 파는 자리에 돈부터 걷는 일이 없게 한다.
 *
 * <p>결제수단이 늘어도 이 클래스는 열리지 않는다. 여기엔 "CARD면 …, POINT면 …"이 없다.
 * 담당자를 고르는 일은 {@link PaymentMethods}에, 청구하는 법은 각 {@link ChargePort} 구현에 있다.
 */
public class TicketService {

    private final TicketStore tickets;
    private final UserStore users;
    private final PaymentMethods paymentMethods;

    /**
     * 계약이 못박은 조립 지점이다. 벤더 API를 구체 타입으로 받게 되어 있으므로, 그것을 결제 담당으로
     * 엮는 일은 이 자리에서 끝내고 유스케이스 본문은 역할({@link ChargePort})만 상대하게 한다.
     */
    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments, PointApi points) {
        this.tickets = tickets;
        this.users = users;
        this.paymentMethods = new PaymentMethods(payments, points);
    }

    /**
     * 회원이 티켓 하나를 골라 카드로 예매한다.
     *
     * <p>기존 호출부가 그대로 살아 있는 진입점이다. 카드 예매는 결제수단이 카드인 예매일 뿐이므로
     * 흐름을 복제하지 않고 아래 진입점에 그대로 넘긴다.
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        return reserveTicket(userId, ticketId, PaymentMethods.CARD, paymentInfo);
    }

    /**
     * 회원이 티켓 하나를 골라 지정한 결제수단으로 예매한다. 성공하면 티켓은 그 회원의 것으로 확정되어
     * 저장되고, 티켓 가격이 그 수단으로 청구된 상태가 된다.
     *
     * @param paymentMethod {@code "CARD"} 또는 {@code "POINT"}
     * @param paymentInfo   카드정보. {@code "POINT"}면 쓰이지 않는다.
     * @return 예매에 성공하면 true
     * @throws UserNotFoundException          등록되지 않은 회원이면 (청구 없음)
     * @throws TicketAlreadyReservedException 이미 예약된 티켓이면 (청구 없음)
     * @throws PaymentFailedException         결제사가 청구를 거절하면 (티켓은 확정되지 않음)
     * @throws InsufficientPointException     포인트가 모자라면 (차감 없음, 티켓은 확정되지 않음)
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentMethod, String paymentInfo) {
        if (users.findById(userId) == null) {
            throw new UserNotFoundException(userId);
        }

        Ticket ticket = Ticket.from(tickets.findById(ticketId));
        ticket.reserve(userId);

        // 거절될 수 있는 판단은 여기서 끝났다. 이제부터 처음으로 돈이 움직인다.
        ChargePort payment = paymentMethods.select(paymentMethod, userId, paymentInfo);
        payment.charge(ticket.price());
        tickets.save(ticket.toRecord());

        return true;
    }
}
