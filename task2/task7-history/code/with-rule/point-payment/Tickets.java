package com.thinking.ticket;

/**
 * 티켓 보관소에게 기대하는 역할.
 *
 * <p>변경 축: 티켓이 어디에 담기는지(지금은 사내 티켓 저장소)는 정책 바깥에서 바뀐다.
 * 정책이 통제하지 못하는 저장 레코드에 직접 매달리지 않도록 방향을 뒤집는다.
 *
 * <p>너비: 예매가 필요로 하는 것은 한 장을 불러오는 것과 확정된 한 장을 남기는 것뿐이다.
 *
 * <p>티켓이 보관소에 아예 없는 경우의 동작은 이번 범위에서 정해지지 않았다.
 */
public interface Tickets {

    Ticket byId(long ticketId);

    void save(Ticket ticket);
}
