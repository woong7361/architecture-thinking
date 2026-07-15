package com.thinking.ticket;

/* 예약 규칙을 스스로 지키는 도메인 모델. 상태 전이는 reserveBy(), 저장된 상태 복원은 of()로만 이루어진다. */
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

    private Ticket(long id, int price, boolean reserved, long userId) {
        this.id = id;
        this.price = price;
        this.reserved = reserved;
        this.userId = userId;
    }

    /* 저장소가 저장된 상태를 되살리는 통로. 도메인 전이가 아니다. */
    public static Ticket of(long id, int price, boolean reserved, long userId) {
        return new Ticket(id, price, reserved, userId);
    }

    public void ensureReservable() {
        if (reserved) {
            throw new TicketAlreadyReservedException();
        }
    }

    public void reserveBy(long userId) {
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
