package com.thinking.ticket.core.port.in;

/* Inbound Port 입력 DTO. Inbound Adapter(HTTP/Cucumber)는 자신의 표현(JSON, Gherkin 값)을
 * 이 Command로 바꾼 뒤 Port를 호출한다 — Core는 HTTP/Gherkin을 모른다. */
public record ReserveTicketCommand(long userId, long ticketId, String paymentInfo) {
}
