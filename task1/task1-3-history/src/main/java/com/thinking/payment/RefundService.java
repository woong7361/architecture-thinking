package com.thinking.payment;

public final class RefundService {

    private final PgClient pg;

    public RefundService(PgClient pg) {
        this.pg = pg;
    }

    public void cancel(Order order, Refund refund) {
        int refundAmount = refund.amountFor(order.amount());
        PgCancelResult result = pg.cancelPayment(order.paymentUuid(), refundAmount);

        if (result.isSucceeded()) {
            refund.succeed();
            order.applyRefund(refundAmount);
        }
        if (result.isRejected()) {
            refund.fail();
        }
    }
}
