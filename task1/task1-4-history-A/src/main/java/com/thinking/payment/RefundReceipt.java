package com.thinking.payment;

public record RefundReceipt(
    int amount,
    RefundType type,
    OrderStatus orderStatus,
    int refundableAmount
) {
}
