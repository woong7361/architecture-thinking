package com.thinking.ticket;

/* 티켓 예매 유스케이스의 조립자(orchestration).
 * 예약 불변식·상태 전이는 Ticket이, 결제/저장 I/O는 포트가 책임진다 —
 * 서비스는 협력의 순서만 조정한다. (원자성/보상 경계는 롤백 인프라 부재로 미구현: quirk)
 */
public class TicketService {

    private final TicketRepository ticketRepo; // (DB 의존)
    private final UserRepository userRepo;     // (DB 의존)
    private final ChargePort chargePort;       // (외부 결제 포트 의존)

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo, ChargePort chargePort) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.chargePort = chargePort;
    }

    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        User user = userRepo.findById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }

        Ticket ticket = ticketRepo.findById(ticketId);
        ticket.ensureReservable();

        boolean paymentSuccess = chargePort.charge(paymentInfo, ticket.getPrice());
        if (!paymentSuccess) {
            throw new PaymentFailedException();
        }

        ticket.assignTo(userId);
        ticketRepo.save(ticket);
        return true;
    }
}
