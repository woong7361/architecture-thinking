package com.thinking.ticket;

/**
 * 티켓 보관소에게 기대하는 역할.
 *
 * <p>변경 축: 티켓이 어디에 담기는지(지금은 사내 티켓 저장소)는 정책 바깥에서 바뀐다.
 * 정책이 통제하지 못하는 저장 레코드에 직접 매달리지 않도록 방향을 뒤집는다.
 *
 * <p>너비: 예매가 필요로 하는 것은 한 장을 불러오는 것, 한 회원의 보유분을 불러오는 것,
 * 확정된 한 장을 남기는 것뿐이다. 몇 장을 가졌는지 세는 일은 보관소가 알아서 하고,
 * 정책에는 보유분 자체가 온다 — 세는 방법이 새어나오면 규칙이 정책 쪽으로 흘러나온다.
 *
 * <p>티켓이 보관소에 아예 없는 경우의 동작은 이번 범위에서 정해지지 않았다.
 */
public interface Tickets {

    Ticket byId(long ticketId);

    Holdings holdingsOf(long userId);

    void save(Ticket ticket);
}
