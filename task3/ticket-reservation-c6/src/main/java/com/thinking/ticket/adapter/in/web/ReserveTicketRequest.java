package com.thinking.ticket.adapter.in.web;

/* HTTP 요청 본문의 표현. Inbound Port의 ReserveTicketCommand와 필드가 같더라도 별도로 둔다 —
 * 겸용하면 JSON 와이어 포맷의 사정(필드 추가·이름 변경·역직렬화 규약)이 Core의 계약을 규정하게 된다.
 * 이 클래스는 이 어댑터 밖으로 나가지 않는다. */
public record ReserveTicketRequest(long userId, long ticketId, String paymentInfo) {
}
