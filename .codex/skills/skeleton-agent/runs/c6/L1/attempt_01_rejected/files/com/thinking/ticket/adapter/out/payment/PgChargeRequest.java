package com.thinking.ticket.adapter.out.payment;

/* 결제사에 보내는 요청 본문. 도메인 개념이 아니라 이 어댑터와 결제사 사이의 전송 모양이라
 * 패키지 밖으로 내보내지 않는다. */
record PgChargeRequest(String paymentInfo, int amount) {
}
