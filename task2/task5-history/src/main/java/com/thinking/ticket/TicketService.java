package com.thinking.ticket;

/* [리팩토링 대상: 티켓 예매 서비스 — B-2 kata 원문]
 * 하나의 메소드에 모든 로직이 절차적으로 구현되어 있다.
 * B-5의 안전망(특성화 테스트)은 이 "현재 동작"을 있는 그대로 고정한다(quirk 포함).
 */
public class TicketService {

    private final TicketRepository ticketRepo; // (DB 의존)
    private final UserRepository userRepo;     // (DB 의존)
    private final PaymentApi paymentApi;       // (외부 API 의존)

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo, PaymentApi paymentApi) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.paymentApi = paymentApi;
    }

    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        // 1. 유저 조회 (DB)
        User user = userRepo.findById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        // 2. 티켓 조회 (DB)
        Ticket ticket = ticketRepo.findById(ticketId);
        ticket.ensureReservable();
        // 3. 결제 시도 (외부 API)
        boolean paymentSuccess = paymentApi.charge(paymentInfo, ticket.getPrice());
        if (!paymentSuccess) {
            throw new PaymentFailedException();
        }
        // 4. 티켓 상태 변경 (DB)
        ticket.assignTo(userId);
        ticketRepo.save(ticket);
        return true;
    }
}
