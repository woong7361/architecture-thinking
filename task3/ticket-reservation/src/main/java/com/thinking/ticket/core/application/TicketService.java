package com.thinking.ticket.core.application;

import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.domain.PaymentFailedException;
import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.domain.UserNotFoundException;
import com.thinking.ticket.core.port.in.ReservationResult;
import com.thinking.ticket.core.port.in.ReserveTicketCommand;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;
import com.thinking.ticket.core.port.out.ChargePort;
import com.thinking.ticket.core.port.out.TicketRepository;
import com.thinking.ticket.core.port.out.UserRepository;

/* 티켓 예매 유스케이스의 조립자(orchestration) + Inbound Port 구현.
 * 예약 불변식·상태 전이는 Ticket이, 결제/저장 I/O는 포트가 책임진다 —
 * 서비스는 협력의 순서만 조정한다. Spring/JPA 타입을 전혀 import하지 않는다(Core 순수성).
 *
 * <p>알려진 quirk(현재 동작으로 박제): 없는 티켓 findById는 null -> NPE, 결제 후 저장 실패 시
 * 보상(취소) 없음. 이 원자성/보상 경계는 C-5에서 다룬다(여기선 안전망대로 보존). */
public class TicketService implements ReserveTicketUseCase {

    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final ChargePort chargePort;
    private final DiscountPolicy discountPolicy;

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo, ChargePort chargePort,
                         DiscountPolicy discountPolicy) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.chargePort = chargePort;
        this.discountPolicy = discountPolicy;
    }

    /* Inbound Port 진입점: Adapter가 만든 Command를 받아 유스케이스를 실행하고 결과 DTO로 답한다. */
    @Override
    public ReservationResult reserve(ReserveTicketCommand command) {
        boolean reserved = reserveTicket(command.userId(), command.ticketId(), command.paymentInfo());
        return new ReservationResult(reserved, command.ticketId(), command.userId());
    }

    /* 유스케이스 본문. 인수테스트(특성화 안전망)가 직접 호출하는 경계이기도 하다 —
     * 관찰 가능한 동작(예약 확정/결제 청구/거부 사유)을 그대로 유지한다. */
    public boolean reserveTicket(long userId, long ticketId, String paymentInfo) {
        User user = userRepo.findById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }

        Ticket ticket = ticketRepo.findById(ticketId);
        ticket.ensureReservable();

        int amount = discountPolicy.finalAmount(ticket.getPrice());
        boolean paymentSuccess = chargePort.charge(paymentInfo, amount);
        if (!paymentSuccess) {
            throw new PaymentFailedException();
        }

        ticket.assignTo(userId);
        ticketRepo.save(ticket);
        return true;
    }
}
