package com.thinking.ticket.core.port.in;

/* Inbound Port: Core가 바깥에 제공하는 계약("reserve 유스케이스를 이렇게 호출하라").
 * Cucumber(인수테스트)와 HTTP(ReservationController)가 같은 계약에 의존하고,
 * Gherkin의 When 한 문장이 이 메서드 하나와 1:1로 대응한다. */
public interface ReserveTicketUseCase {

    ReservationResult reserve(ReserveTicketCommand command);
}
