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
