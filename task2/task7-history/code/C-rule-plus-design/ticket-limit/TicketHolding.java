package com.thinking.ticket;

/**
 * 보유 한도 불변식의 주인 — 한 회원이 가진 티켓 현황.
 *
 * <p>"한 회원은 티켓을 한 장만 가질 수 있다"는 규칙이 필요로 하는 정보는 <b>그 회원이 지금 몇 장을
 * 가졌나</b>이다. {@link Ticket} 한 장은 자기 예약 상태만 알 뿐 그 수를 모른다 — 티켓을 가로지르는
 * 사실이라 어느 티켓도 정보 전문가가 아니다. 그래서 그 수를 소유하는 객체가 따로 태어나 규칙을 진다.
 *
 * <p>수를 세는 일은 저장소가, 그 수로 <b>판단</b>하는 일은 이 객체가 한다. 서비스가
 * {@code if (count >= LIMIT) throw ...}로 직접 세어보고 판단하면 그 규칙은 묻는 곳마다 흩어진다.
 *
 * <p>I/O는 모른다. {@link Ticket}과 마찬가지로 순수한 판단만 품는다.
 */
final class TicketHolding {

    /** 한 회원이 가질 수 있는 티켓 수. */
    private static final int LIMIT = 1;

    private final long userId;
    private int held;

    private TicketHolding(long userId, int held) {
        this.userId = userId;
        this.held = held;
    }

    static TicketHolding of(long userId, int held) {
        return new TicketHolding(userId, held);
    }

    /**
     * 이 회원 앞으로 티켓 한 장을 더 잡는다.
     *
     * @throws TicketLimitExceededException 이미 한도만큼 가지고 있으면
     */
    void claimAnother() {
        if (held >= LIMIT) {
            throw new TicketLimitExceededException(userId, LIMIT);
        }
        held++;
    }
}
