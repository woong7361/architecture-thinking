package com.thinking.ticket.core.domain;

/* 존재하지 않는 티켓을 예매하려 할 때의 도메인 예외.
 * (원본은 null 미검사로 NPE가 났으나, 방어 없는 비정상 종료 대신 도메인 거부로 바꿈 — B-1) */
public class TicketNotFoundException extends RuntimeException {
}
