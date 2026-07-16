package com.thinking.ticket.provided;

/**
 * 티켓 저장소가 다루는 영속 레코드. 기존 인프라이며 수정할 수 없다.
 *
 * <p>DB의 한 행을 그대로 옮긴 구조라 상태와 접근자만 갖는다.
 */
public final class TicketRecord {

    private final long id;
    private final int price;
    private boolean reserved;
    private Long userId;

    public TicketRecord(long id, int price) {
        this.id = id;
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
