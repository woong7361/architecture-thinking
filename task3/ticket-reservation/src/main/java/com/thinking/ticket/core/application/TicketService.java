package com.thinking.ticket.core.application;

import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.port.in.ReservationResult;
import com.thinking.ticket.core.port.in.ReserveTicketCommand;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;
import com.thinking.ticket.core.port.out.ChargePort;
import com.thinking.ticket.core.port.out.TicketRepository;
import com.thinking.ticket.core.port.out.UserRepository;

/* 출발선 자리표시자 — 유스케이스 구현은 파이프라인(L0)이 만든다.
 *
 * 클래스 이름과 생성자 시그니처는 남긴다. 심판이 이 표면을 참조하기 때문이다.
 * 이게 없으면 출발선에서 테스트 소스가 컴파일되지 않고, 그러면 멀쩡한 심판까지 전부 못 돌아
 * 게이트 자체가 켜지지 않는다. 출발선의 조건은 "빌드는 되고 실행만 실패"다. */
public class TicketService implements ReserveTicketUseCase {

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo, ChargePort chargePort,
                         DiscountPolicy discountPolicy) {
    }

    @Override
    public ReservationResult reserve(ReserveTicketCommand command) {
        throw new UnsupportedOperationException("유스케이스 미구현 — 출발선 자리표시자");
    }
}
