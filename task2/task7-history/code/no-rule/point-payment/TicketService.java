package com.thinking.ticket;

import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.PointApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserRecord;
import com.thinking.ticket.provided.UserStore;

/**
 * 예매 유스케이스의 진입점.
 */
public class TicketService {

    private static final String CARD = "CARD";
    private static final String POINT = "POINT";

    private final TicketStore tickets;
    private final UserStore users;
    private final PaymentApi payments;
    private final PointApi points;

    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments, PointApi points) {
        this.tickets = tickets;
        this.users = users;
        this.payments = payments;
        this.points = points;
    }

    /**
     * 카드로 예매한다. 카드 결제만 있던 시절의 호출부가 그대로 쓰는 진입점이다.
     *
     * @return 예매에 성공하면 true. 실패는 예외로 알린다.
     * @throws UserNotFoundException 등록되지 않은 회원이다
     * @throws TicketAlreadyReservedException 이미 예매된 티켓이다
     * @throws PaymentFailedException 결제사가 청구를 거절했다
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        return reserveTicket(userId, ticketId, CARD, paymentInfo);
    }

    /**
     * 고른 결제 수단으로 값을 받고 회원에게 티켓을 확정한다.
     *
     * <p>거부 사유는 값을 받기 전에 모두 판단한다. 값을 받지 못하면 확정도 저장도 하지 않는다.
     *
     * @param paymentMethod {@code "CARD"} 또는 {@code "POINT"}
     * @param paymentInfo 카드정보. {@code "POINT"} 이면 쓰이지 않는다.
     * @return 예매에 성공하면 true. 실패는 예외로 알린다.
     * @throws UserNotFoundException 등록되지 않은 회원이다
     * @throws TicketAlreadyReservedException 이미 예매된 티켓이다
     * @throws PaymentFailedException 결제사가 청구를 거절했다
     * @throws InsufficientPointException 포인트 잔액이 티켓 가격보다 적다
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentMethod, String paymentInfo) {
        UserRecord user = users.findById(userId);
        if (user == null) {
            throw new UserNotFoundException("등록되지 않은 회원입니다: " + userId);
        }

        Ticket ticket = Ticket.from(tickets.findById(ticketId));
        ticket.requireReservable();

        paymentBy(paymentMethod, paymentInfo).pay(userId, ticket.price());

        ticket.reserveFor(userId);
        tickets.save(ticket.toRecord());
        return true;
    }

    private Payment paymentBy(String paymentMethod, String paymentInfo) {
        if (CARD.equals(paymentMethod)) {
            return new CardPayment(payments, paymentInfo);
        }
        if (POINT.equals(paymentMethod)) {
            return new PointPayment(points);
        }
        throw new IllegalArgumentException("알 수 없는 결제 수단입니다: " + paymentMethod);
    }
}
