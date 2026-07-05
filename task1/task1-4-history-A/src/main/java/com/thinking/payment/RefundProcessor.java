package com.thinking.payment;

public final class RefundProcessor {

    public RefundReceipt refund(Order order, RefundRequest request) {
        validateRefundable(order);

        int refundableBeforeRequest = order.refundableAmount();
        int refundAmount = calculateRefundAmount(order, request);
        RefundType refundType = refundAmount == refundableBeforeRequest ? RefundType.FULL : RefundType.PARTIAL;

        order.applyRefund(refundAmount);

        return new RefundReceipt(refundAmount, refundType, order.status(), order.refundableAmount());
    }

    private void validateRefundable(Order order) {
        if (order.paymentPlatform() != PaymentPlatform.WEB) {
            throw new RefundRejectedException(
                RefundRejectionReason.WEB_ONLY,
                "웹 결제 주문만 이 경로로 환불할 수 있습니다: " + order.paymentPlatform());
        }
        if (order.status() != OrderStatus.PAID && order.status() != OrderStatus.PARTIALLY_REFUNDED) {
            throw new RefundRejectedException(
                RefundRejectionReason.NOT_REFUNDABLE,
                "환불할 수 없는 주문 상태입니다: " + order.status());
        }
    }

    private int calculateRefundAmount(Order order, RefundRequest request) {
        return switch (request.policy()) {
            case PRORATION -> RefundCalculator.calculate(order.amount(), order.totalDays(), request.elapsedDays());
            case MANUAL -> manualAmount(order, request.manualAmount());
        };
    }

    private int manualAmount(Order order, Integer manualAmount) {
        if (manualAmount == null || manualAmount <= 0) {
            throw new RefundRejectedException(
                RefundRejectionReason.INVALID_REFUND_AMOUNT,
                "수동 환불 금액은 0원보다 커야 합니다");
        }
        if (manualAmount > order.refundableAmount()) {
            throw new RefundRejectedException(
                RefundRejectionReason.REFUND_AMOUNT_EXCEEDED,
                "수동 환불 금액이 환불 가능 금액을 초과했습니다: amount=" + manualAmount
                    + ", refundable=" + order.refundableAmount());
        }
        return manualAmount;
    }
}
