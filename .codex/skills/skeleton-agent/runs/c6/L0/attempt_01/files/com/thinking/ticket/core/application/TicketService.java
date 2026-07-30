package com.thinking.ticket.core.application;

import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.domain.PaymentFailedException;
import com.thinking.ticket.core.domain.TicketNotFoundException;
import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.domain.UserNotFoundException;
import com.thinking.ticket.core.port.in.ReservationResult;
import com.thinking.ticket.core.port.in.ReserveTicketCommand;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;
import com.thinking.ticket.core.port.out.ChargePort;
import com.thinking.ticket.core.port.out.TicketRepository;
import com.thinking.ticket.core.port.out.UserRepository;

/* 예매 유스케이스. Inbound Port를 구현하고 Outbound Port들의 협력 '순서'만 소유한다.
 * 예약 가능 판단과 상태 전이는 Ticket이, 금액 계산은 DiscountPolicy가 가진다 —
 * 여기서 조건 분기로 업무 규칙을 흉내내면 규칙이 두 곳에 생긴다.
 *
 * 생성자 주입만 쓰고 프레임워크 애노테이션을 붙이지 않는다. 조립은 config가,
 * 인수테스트에선 인메모리 대역을 직접 넣는 쪽이 담당한다(Core는 컨테이너 없이 실행된다). */
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

    @Override
    public ReservationResult reserve(ReserveTicketCommand command) {
        /* 회원 확인이 먼저다 — 자격 없는 요청은 티켓을 조회하기 전에 거절한다. */
        User user = userRepo.findById(command.userId());
        if (user == null) {
            throw new UserNotFoundException();
        }

        Ticket ticket = ticketRepo.findById(command.ticketId());
        if (ticket == null) {
            throw new TicketNotFoundException();
        }

        /* assignTo가 같은 불변식을 다시 검사하지만, 여기서 먼저 물어본다.
         * 결제를 먼저 하면 예약 불가 티켓에 대해 청구가 남고 되돌릴 방법이 없다. */
        ticket.ensureReservable();

        int amount = discountPolicy.finalAmount(ticket.getPrice());
        if (!chargePort.charge(command.paymentInfo(), amount)) {
            throw new PaymentFailedException();
        }

        /* 결제가 성공한 뒤에만 확정한다 — 확정과 저장 사이에 다른 판단을 끼우지 않는다. */
        ticket.assignTo(user.getId());
        ticketRepo.save(ticket);

        return new ReservationResult(true, ticket.getId(), user.getId());
    }
}
