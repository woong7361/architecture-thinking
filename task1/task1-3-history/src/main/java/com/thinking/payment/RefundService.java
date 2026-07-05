package com.thinking.payment;

public final class RefundService {

    private final PgClient pg;

    public RefundService(PgClient pg) {
        this.pg = pg;
    }

    public void cancel(Order order, Refund refund) {
        int refundAmount = refund.amountFor(order.amount());
        PgCancelResult result = pg.cancelPayment(order.paymentUuid(), refundAmount);

        switch (result) {
            case SUCCEEDED -> {
                refund.succeed();
                order.applyRefund(refundAmount);
            }
            case REJECTED -> refund.fail();
            case TIMED_OUT -> refund.timeOut();
        }
    }
}
