package com.thinking.payment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public record RefundCalculationRequest(
        long amount,
        long cancellableAmount,
        int totalDays,
        int remainingDays,
        Long manualAmount,
        long elapsedDays
) {
    public RefundCalculationRequest {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (cancellableAmount <= 0 || cancellableAmount > amount) {
            throw new IllegalArgumentException("cancellable amount must be between 1 and amount");
        }
        if (totalDays <= 0) {
            throw new IllegalArgumentException("total days must be positive");
        }
        if (remainingDays < 0 || remainingDays > totalDays) {
            throw new IllegalArgumentException("remaining days must be between 0 and total days");
        }
        if (elapsedDays < 0) {
            throw new IllegalArgumentException("elapsed days must not be negative");
        }
    }

    public static RefundCalculationRequest of(
            long amount,
            long cancellableAmount,
            int totalDays,
            int remainingDays,
            Long manualAmount,
            long elapsedDays
    ) {
        return new RefundCalculationRequest(
                amount,
                cancellableAmount,
                totalDays,
                remainingDays,
                manualAmount,
                elapsedDays
        );
    }

    public static RefundCalculationRequest of(
            long amount,
            long cancellableAmount,
            int totalDays,
            int remainingDays,
            Long manualAmount,
            Instant paidAt,
            Instant requestedAt
    ) {
        return of(
                amount,
                cancellableAmount,
                totalDays,
                remainingDays,
                manualAmount,
                elapsedDays(paidAt, requestedAt)
        );
    }

    public static long elapsedDays(Instant paidAt, Instant requestedAt) {
        LocalDate paidDate = paidAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate requestedDate = requestedAt.atZone(ZoneOffset.UTC).toLocalDate();
        long elapsedDays = ChronoUnit.DAYS.between(paidDate, requestedDate);
        if (elapsedDays < 0) {
            throw new IllegalArgumentException("requestedAt must not be before paidAt");
        }
        return elapsedDays;
    }
}
