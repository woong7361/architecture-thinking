package com.thinking.ticket;

import com.thinking.ticket.infra.CardPayments;
import com.thinking.ticket.infra.MemberPoints;
import com.thinking.ticket.infra.RegisteredMembers;
import com.thinking.ticket.infra.StoredTickets;
import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.PointApi;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserStore;

/**
 * 계약이 고정한 진입점. 바깥이 건네는 기존 인프라를 역할에 맞춰 조립하고 창구에 넘긴다.
 *
 * <p>정책은 여기 없다. 바뀔 이유는 하나다: 바깥과 연결되는 방식이 바뀔 때.
 * 이 클래스만 provided를 알고, 창구 아래로는 내려보내지 않는다.
 *
 * <p>"CARD"/"POINT"라는 문자열은 이 진입점의 어휘이지 예매 정책의 어휘가 아니다. 그래서
 * 그 문자열을 치르는 자로 바꾸는 일이 여기 있고, 창구 아래로는 문자열이 내려가지 않는다.
 *
 * <p>성공을 true로 돌려주는 것도 이 경계의 번역이다. 실패는 모두 예외로 나가므로
 * 창구는 성공/실패를 boolean으로 다루지 않는다.
 */
public class TicketService {

    private static final String CARD = "CARD";
    private static final String POINT = "POINT";

    private final ReservationDesk desk;
    private final Payments payments;
    private final Points points;

    public TicketService(TicketStore tickets, UserStore users, PaymentApi payments, PointApi points) {
        this.desk = new ReservationDesk(
                new StoredTickets(tickets),
                new RegisteredMembers(users));
        this.payments = new CardPayments(payments);
        this.points = new MemberPoints(points);
    }

    /** 카드 결제로 예매한다. 성공이면 true, 실패는 예외로 알린다. */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        return reserveTicket(userId, ticketId, CARD, paymentInfo);
    }

    /**
     * 지정한 수단으로 예매한다. 성공이면 true, 실패는 예외로 알린다.
     *
     * @param paymentMethod "CARD" 또는 "POINT". "POINT"이면 paymentInfo는 쓰이지 않는다.
     */
    public boolean reserveTicket(long userId, long ticketId, String paymentMethod, String paymentInfo) {
        desk.reserve(userId, ticketId, payerFor(paymentMethod, userId, paymentInfo));
        return true;
    }

    private Payer payerFor(String paymentMethod, long userId, String paymentInfo) {
        if (CARD.equals(paymentMethod)) {
            return new CardPayer(payments, paymentInfo);
        }
        if (POINT.equals(paymentMethod)) {
            return new PointPayer(points, userId);
        }
        throw new IllegalArgumentException("알 수 없는 결제 수단입니다: " + paymentMethod);
    }
}
