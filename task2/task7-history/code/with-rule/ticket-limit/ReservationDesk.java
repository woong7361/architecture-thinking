package com.thinking.ticket;

/**
 * 예매 요청을 받아 협력을 이끄는 창구.
 *
 * <p>여기 있는 것은 순서뿐이다. 등록 여부는 명부가, 이미 잡혔는지는 티켓 자신이,
 * 한 장을 더 가질 수 있는지는 그 회원의 보유 현황이, 승인 여부는 결제사가 각자 안다 —
 * 창구는 그중 어느 것도 대신 판단하지 않는다.
 * 다만 "거절이면 예매 실패"처럼 협력 전체를 봐야 나오는 판단은 창구가 한다.
 *
 * <p>바뀔 이유는 하나다: 예매 절차가 바뀔 때. 바깥과 어떻게 연결되는지는 알지 못한다.
 */
final class ReservationDesk {

    private final Tickets tickets;
    private final Members members;
    private final Payments payments;

    ReservationDesk(Tickets tickets, Members members, Payments payments) {
        this.tickets = tickets;
        this.members = members;
        this.payments = payments;
    }

    void reserve(long userId, long ticketId, String paymentInfo) {
        if (!members.isRegistered(userId)) {
            throw new UserNotFoundException(userId);
        }

        Ticket ticket = tickets.byId(ticketId);

        // 잡아본 뒤에 청구한다. 한도에 걸렸거나 이미 잡힌 티켓이면 여기서 거부되므로
        // 청구는 시도조차 되지 않는다.
        Ticket reserved = tickets.holdingOf(userId).take(ticket);

        if (!payments.charge(paymentInfo, ticket.price())) {
            throw new PaymentFailedException(ticketId, ticket.price());
        }

        // 남겨야 확정이다. 청구가 거절돼 여기까지 못 오면 티켓은 아무에게도 확정되지 않은 채로 남는다.
        tickets.save(reserved);
    }
}
