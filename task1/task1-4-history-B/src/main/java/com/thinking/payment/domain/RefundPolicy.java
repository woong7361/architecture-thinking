package com.thinking.payment.domain;

import java.time.Instant;

public enum RefundPolicy {
    PRORATION,
    MANUAL;

    private static final long FREE_CANCELLATION_DAYS = 7;

    public long calculate(long amount, int totalDays, int remainingDays) {
        if (this == MANUAL) {
            throw new IllegalStateException("manual amount is required for MANUAL policy");
        }
        return calculateProrationAmount(amount, totalDays, remainingDays);
    }

    public long calculate(long manualAmount, long cancellableAmount) {
        if (this != MANUAL) {
            throw new IllegalStateException("manual amount is only supported by MANUAL policy");
        }
        return calculateManualAmount(manualAmount, cancellableAmount);
    }

    public long calculate(RefundCalculationRequest request) {
        if (this == MANUAL) {
            return calculateManualAmount(request.manualAmount(), request.cancellableAmount());
        }
        return calculateRefundAmount(request);
    }

    public static long calculateRefundAmount(RefundCalculationRequest request) {
        if (request.manualAmount() != null) {
            return calculateManualAmount(request.manualAmount(), request.cancellableAmount());
        }

        if (request.elapsedDays() <= FREE_CANCELLATION_DAYS) {
            return request.cancellableAmount();
        }

        long amount = calculateProrationAmount(request.amount(), request.totalDays(), request.remainingDays());
        validateWithinCancellableAmount(amount, request.cancellableAmount());
        return amount;
    }

    public static long calculateAmount(RefundCalculationRequest request) {
        return calculateRefundAmount(request);
    }

    public static long calculateRefundAmount(
            long amount,
            long cancellableAmount,
            int totalDays,
            int remainingDays,
            long elapsedDays,
            Number manualAmount
    ) {
        Long convertedManualAmount = manualAmount == null ? null : manualAmount.longValue();
        return calculateRefundAmount(new RefundCalculationRequest(
                amount,
                cancellableAmount,
                totalDays,
                remainingDays,
                convertedManualAmount,
                elapsedDays
        ));
    }

    public static long calculateRefundAmount(
            long amount,
            long cancellableAmount,
            int totalDays,
            int remainingDays,
            Instant paidAt,
            Instant requestedAt,
            Long manualAmount
    ) {
        return calculateRefundAmount(RefundCalculationRequest.of(
                amount,
                cancellableAmount,
                totalDays,
                remainingDays,
                manualAmount,
                paidAt,
                requestedAt
        ));
    }

    public static long calculateProrationAmount(long amount, int totalDays, int remainingDays) {
        validateProrationInput(amount, totalDays, remainingDays);

        if (remainingDays == totalDays) {
            return amount;
        }
        if (remainingDays == 0) {
            return 0;
        }

        long dailyAmount = amount / totalDays;
        return dailyAmount * remainingDays;
    }

    public static long calculateProratedAmount(long amount, int totalDays, int remainingDays) {
        return calculateProrationAmount(amount, totalDays, remainingDays);
    }

    public static long calculateProratedRefundAmount(long amount, int totalDays, int remainingDays) {
        return calculateProrationAmount(amount, totalDays, remainingDays);
    }

    public static long calculateManualAmount(Long manualAmount, long cancellableAmount) {
        if (manualAmount == null) {
            throw new IllegalArgumentException("manual amount is required");
        }
        if (manualAmount <= 0) {
            throw new RefundException("manual amount must be positive");
        }
        validateWithinCancellableAmount(manualAmount, cancellableAmount);
        return manualAmount;
    }

    public static long calculateManualAmount(long manualAmount, long cancellableAmount) {
        return calculateManualAmount(Long.valueOf(manualAmount), cancellableAmount);
    }

    public static RefundType determineRefundType(long refundAmount, long cancellableAmount) {
        if (refundAmount < 0) {
            throw new RefundException("refund amount must not be negative");
        }
        validateWithinCancellableAmount(refundAmount, cancellableAmount);
        return refundAmount == cancellableAmount ? RefundType.FULL : RefundType.PARTIAL;
    }

    public static long elapsedDays(Instant paidAt, Instant requestedAt) {
        return RefundCalculationRequest.elapsedDays(paidAt, requestedAt);
    }

    private static void validateProrationInput(long amount, int totalDays, int remainingDays) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (totalDays <= 0) {
            throw new IllegalArgumentException("total days must be positive");
        }
        if (remainingDays < 0 || remainingDays > totalDays) {
            throw new IllegalArgumentException("remaining days must be between 0 and total days");
        }
    }

    private static void validateWithinCancellableAmount(long refundAmount, long cancellableAmount) {
        if (cancellableAmount <= 0) {
            throw new IllegalArgumentException("cancellable amount must be positive");
        }
        if (refundAmount > cancellableAmount) {
            throw new RefundException("refund amount exceeds cancellable amount");
        }
    }
}
