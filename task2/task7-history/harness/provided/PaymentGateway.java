package com.thinking.ticket.provided;

/**
 * 새 결제사가 제공하는 결제 게이트웨이. 기존 인프라이며 수정할 수 없다.
 *
 * <p>실제 운영에서는 결제사 서버로 HTTP 요청을 보내지만, 이 실험에서는 메모리로 동작하는
 * 시뮬레이터다. 그래서 테스트가 결과를 통제하고({@link #approving}/{@link #declining})
 * 호출을 되읽는 통로가 함께 열려 있다.
 *
 * <p>{@link #authorize}는 승인되면 승인번호를 돌려주고, 거절되면 null을 돌려준다.
 */
public final class PaymentGateway {

    private final boolean outcome;
    private int authorizeCount;
    private String lastCardToken;
    private int lastAmount;

    private PaymentGateway(boolean outcome) {
        this.outcome = outcome;
    }

    /** 테스트용 생성 통로: 모든 승인 요청이 승인되는 결제사. */
    public static PaymentGateway approving() {
        return new PaymentGateway(true);
    }

    /** 테스트용 생성 통로: 모든 승인 요청이 거절되는 결제사. */
    public static PaymentGateway declining() {
        return new PaymentGateway(false);
    }

    /**
     * 승인을 요청한다.
     *
     * @return 승인되면 승인번호, 거절되면 null
     */
    public String authorize(int amount, String cardToken) {
        authorizeCount++;
        lastAmount = amount;
        lastCardToken = cardToken;
        return outcome ? "auth-" + authorizeCount : null;
    }

    /** 테스트가 호출 기록을 되읽는 통로. */
    public boolean wasAuthorized() {
        return authorizeCount > 0;
    }

    public int authorizeCount() {
        return authorizeCount;
    }

    public String lastCardToken() {
        return lastCardToken;
    }

    public int lastAmount() {
        return lastAmount;
    }
}
