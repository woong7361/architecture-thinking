package com.thinking.ticket.provided;

/**
 * 외부 결제사가 제공하는 결제 API. 기존 인프라이며 수정할 수 없다.
 *
 * <p>실제 운영에서는 결제사 서버로 HTTP 요청을 보내지만, 이 실험에서는 메모리로 동작하는
 * 시뮬레이터다. 그래서 테스트가 결과를 통제하고({@link #succeeding}/{@link #failing})
 * 호출을 되읽는 통로가 함께 열려 있다.
 *
 * <p>{@link #charge}는 청구에 성공하면 true, 결제사가 거절하면 false를 반환한다.
 */
public final class PaymentApi {

    private final boolean outcome;
    private int chargeCount;
    private String lastPaymentInfo;
    private int lastAmount;

    private PaymentApi(boolean outcome) {
        this.outcome = outcome;
    }

    /** 테스트용 생성 통로: 모든 청구가 승인되는 결제사. */
    public static PaymentApi succeeding() {
        return new PaymentApi(true);
    }

    /** 테스트용 생성 통로: 모든 청구가 거절되는 결제사. */
    public static PaymentApi failing() {
        return new PaymentApi(false);
    }

    public boolean charge(String paymentInfo, int amount) {
        chargeCount++;
        lastPaymentInfo = paymentInfo;
        lastAmount = amount;
        return outcome;
    }

    /** 테스트가 호출 기록을 되읽는 통로. */
    public boolean wasCharged() {
        return chargeCount > 0;
    }

    public int chargeCount() {
        return chargeCount;
    }

    public String lastPaymentInfo() {
        return lastPaymentInfo;
    }

    public int lastAmount() {
        return lastAmount;
    }
}
