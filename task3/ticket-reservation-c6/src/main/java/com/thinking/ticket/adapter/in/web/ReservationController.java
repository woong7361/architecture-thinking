package com.thinking.ticket.adapter.in.web;

import com.thinking.ticket.core.port.in.ReservationResult;
import com.thinking.ticket.core.port.in.ReserveTicketCommand;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* 인바운드 진입 어댑터. HTTP라는 바깥 표현을 Command로 번역해 Inbound Port를 호출하는 일만 한다.
 *
 * 주입받는 타입을 TicketService가 아니라 ReserveTicketUseCase로 둔다 — 구현 클래스 이름을 알게 되는
 * 순간 이 어댑터가 Core의 조립 방식에 묶이고, 인수테스트가 쓰는 구성과 갈라진다.
 *
 * 유스케이스를 건너뛰고 아웃바운드 포트를 직접 부르지 않는다. 그러면 업무 절차가 어댑터로 새어
 * 같은 규칙이 두 곳에 생긴다. 예외를 상태 코드로 옮기는 일도 여기 두지 않고 별도 핸들러에 모은다
 * (번역 규칙이 메서드마다 흩어지면 새 엔드포인트가 생길 때 조용히 빠진다). */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReserveTicketUseCase reserveTicketUseCase;

    public ReservationController(ReserveTicketUseCase reserveTicketUseCase) {
        this.reserveTicketUseCase = reserveTicketUseCase;
    }

    /* Gherkin의 When 한 문장과 1:1로 대응하는 포트 메서드를 이 엔드포인트 하나가 그대로 노출한다.
     * 성공 응답 본문은 유스케이스 결과를 그대로 직렬화한다 — 이 자리에서 필드를 다시 조립하면
     * 와이어 계약(reserved, ticketId, userId)이 포트 계약과 어긋날 여지가 생긴다. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReservationResult reserve(@RequestBody ReserveTicketRequest request) {
        return reserveTicketUseCase.reserve(
                new ReserveTicketCommand(request.userId(), request.ticketId(), request.paymentInfo()));
    }
}
