package com.thinking.ticket.support;

import com.thinking.ticket.PaymentApi;

/**
 * 외부 결제 API의 test double. 진짜 결제를 대신할 수 없으므로,
 * 성공/실패를 통제(configure)하고 charge 호출을 기록(record)한다.
 * net은 이 기록을 상태로 읽어 "결제가 시도됐나/안 됐나"를 단언한다(verify 대신).
 */
public final class RecordingPaymentApi implements PaymentApi {

    private final boolean outcome;
    private int chargeCount = 0;
    private String lastPaymentInfo;
    private int lastAmount;

    private RecordingPaymentApi(boolean outcome) {
        this.outcome = outcome;
    }

    public static RecordingPaymentApi succeeding() {
        return new RecordingPaymentApi(true);
    }

    public static RecordingPaymentApi failing() {
        return new RecordingPaymentApi(false);
    }

    @Override
    public boolean charge(String paymentInfo, int amount) {
        chargeCount++;
        lastPaymentInfo = paymentInfo;
        lastAmount = amount;
        return outcome;
    }

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
