package com.thinking.ticket;

/**
 * 예매 대상인 티켓.
 *
 * <p>"이미 잡힌 티켓은 다시 잡을 수 없다"는 규칙을 예약 상태를 가진 이 객체가 스스로 지킨다.
 * 그래서 상태를 여는 setter를 두지 않고, {@link #reserveFor}라는 의미 있는 행위로만 잡힌다.
 * 잡는 것과 값을 아는 것 외에 이 객체가 하는 일은 없다 — 청구 같은 바깥 입출력은 섞지 않는다.
 */
public final class Ticket {

    private final long id;
    private final int price;

    /** 아무도 잡지 않았으면 null. */
    private final Long reservedBy;

    public Ticket(long id, int price, Long reservedBy) {
        this.id = id;
        this.price = price;
        this.reservedBy = reservedBy;
    }

    /**
     * 이 티켓을 그 회원 앞으로 잡은 티켓을 돌려준다. 이미 잡혀 있으면 거부한다.
     *
     * <p>원본을 바꾸지 않고 새 티켓을 돌려주므로, 부르는 쪽이 남기지 않기로 하면
     * 잡은 사실 자체가 없던 일이 된다.
     */
    public Ticket reserveFor(long userId) {
        if (reservedBy != null) {
            throw new TicketAlreadyReservedException(id);
        }
        return new Ticket(id, price, userId);
    }

    public long id() {
        return id;
    }

    public int price() {
        return price;
    }

    public Long reservedBy() {
        return reservedBy;
    }
}
