package com.thinking.ticket.adapter.in.web;

import com.thinking.ticket.core.port.in.ReservationResult;
import com.thinking.ticket.core.port.in.ReserveTicketCommand;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* Inbound Adapter(HTTP). 요청 JSON을 Command로 바꿔 Inbound Port를 호출한다 —
 * Cucumber Step과 동일한 계약(ReserveTicketUseCase)을 재사용한다(C-6 walking skeleton). */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReserveTicketUseCase reserveTicket;

    public ReservationController(ReserveTicketUseCase reserveTicket) {
        this.reserveTicket = reserveTicket;
    }

    @PostMapping
    public ReservationResult reserve(@RequestBody ReservationRequest request) {
        ReserveTicketCommand command =
                new ReserveTicketCommand(request.userId(), request.ticketId(), request.paymentInfo());
        return reserveTicket.reserve(command);
    }

    public record ReservationRequest(long userId, long ticketId, String paymentInfo) {
    }
}
