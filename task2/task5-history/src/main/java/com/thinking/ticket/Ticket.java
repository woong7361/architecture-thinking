package com.thinking.ticket;

/* 풍부한 도메인 모델(Rich): 예약 불변식과 상태 전이를 스스로 소유한다.
 * public setter가 없어 ensureReservable()/assignTo()를 통해서만 상태가 바뀐다. */
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

    /* 예약 상태 전이를 스스로 책임진다 — 불변식을 자기방어하며 소유자를 기록한다. */
    public void assignTo(long userId) {
        ensureReservable();
        this.reserved = true;
        this.userId = userId;
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

    public long getUserId() {
        return userId;
    }
}
