package com.thinking.ticket;

/* 빈약한 도메인 모델(Anemic): getter/setter 자루. 규칙은 하나도 스스로 지키지 않는다. */
public class Ticket {

    private long id;
    private int price;
    private boolean reserved;
    private long userId;

    public Ticket(long id, int price) {
        this.id = id;
        this.price = price;
        this.reserved = false;
    }

    /* 예약 가능 판단(불변식)을 스스로 책임진다 — 이미 예약된 티켓은 다시 예약될 수 없다. */
    public void ensureReservable() {
        if (reserved) {
            throw new TicketAlreadyReservedException();
        }
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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
