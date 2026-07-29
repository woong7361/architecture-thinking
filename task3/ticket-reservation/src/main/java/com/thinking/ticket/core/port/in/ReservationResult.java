package com.thinking.ticket.core.port.in;

/* Inbound Port 출력 DTO. 성공 여부와 예약 결과 요약만 담는다(HTTP 상태·JSON은 Adapter가 결정). */
public record ReservationResult(boolean reserved, long ticketId, long userId) {
}
