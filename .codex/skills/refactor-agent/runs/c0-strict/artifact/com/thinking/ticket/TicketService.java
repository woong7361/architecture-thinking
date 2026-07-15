package com.thinking.ticket;

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
        requireExistingUser(userId);

        Ticket ticket = ticketRepo.findById(ticketId);
        ticket.ensureReservable();

        boolean paymentSuccess = paymentApi.charge(paymentInfo, ticket.getPrice());
        if (!paymentSuccess) {
            throw new PaymentFailedException();
        }

        ticket.reserveBy(userId);
        ticketRepo.save(ticket);
        return true;
    }

    private void requireExistingUser(long userId) {
        User user = userRepo.findById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
    }
}
