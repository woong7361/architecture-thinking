package com.thinking.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefundDesignPolicyTest {

    private static final Instant PAID_AT = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    @DisplayName("MANUAL 지정은 7일 이하 무료와 PRORATION보다 우선한다")
    void manualAmountOverridesFreePeriodAndProration() {
        RefundCalculationRequest request = RefundCalculationRequest.of(
                30000L,
                30000L,
                30,
                24,
                5000L,
                PAID_AT,
                Instant.parse("2026-07-07T23:59:59Z")
        );

        long amount = RefundPolicy.calculateRefundAmount(request);

        assertThat(amount).isEqualTo(5000L);
    }

    @ParameterizedTest(name = "paidAt={0}, requestedAt={1} -> elapsed={2}, amount={4}, type={5}")
    @CsvSource({
            "2026-07-01T00:00:00Z, 2026-07-01T23:59:59Z, 0, 30, 30000, FULL",
            "2026-07-01T00:00:00Z, 2026-07-08T23:59:59Z, 7, 23, 30000, FULL",
            "2026-07-01T00:00:00Z, 2026-07-09T00:00:00Z, 8, 22, 22000, PARTIAL"
    })
    @DisplayName("7일 이하 무료 정책 경계는 UTC 날짜 기준 경과일로 판단한다")
    void freeRefundBoundaryUsesUtcDateElapsedDays(
            String paidAt,
            String requestedAt,
            long expectedElapsedDays,
            int remainingDays,
            long expectedAmount,
            RefundType expectedType
    ) {
        RefundCalculationRequest request = RefundCalculationRequest.of(
                30000L,
                30000L,
                30,
                remainingDays,
                null,
                Instant.parse(paidAt),
                Instant.parse(requestedAt)
        );

        long amount = RefundPolicy.calculateRefundAmount(request);
        RefundType type = RefundPolicy.determineRefundType(amount, 30000L);

        assertThat(request.elapsedDays()).isEqualTo(expectedElapsedDays);
        assertThat(amount).isEqualTo(expectedAmount);
        assertThat(type).isEqualTo(expectedType);
    }

    @ParameterizedTest(name = "amount={0}, totalDays={1}, remainingDays={2} -> refund={3}")
    @CsvSource({
            "30000, 30, 30, 30000",
            "30000, 30, 15, 15000",
            "30000, 30, 1, 1000",
            "30000, 30, 0, 0",
            "10000, 30, 7, 2331"
    })
    @DisplayName("PRORATION은 일할 단가를 정수 나눗셈으로 구하고 잔여 일수에 곱한다")
    void prorationUsesIntegerDailyRateAndRemainingDays(
            long amount,
            int totalDays,
            int remainingDays,
            long expectedRefundAmount
    ) {
        long refundAmount = RefundPolicy.calculateProrationAmount(amount, totalDays, remainingDays);

        assertThat(refundAmount).isEqualTo(expectedRefundAmount);
    }

    @ParameterizedTest(name = "totalDays={0}")
    @CsvSource({"0", "-1"})
    @DisplayName("PRORATION은 총 구독 일수가 0 이하이면 거절한다")
    void prorationRejectsNonPositiveTotalDays(int totalDays) {
        assertThatThrownBy(() -> RefundPolicy.calculateProrationAmount(30000L, totalDays, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "canceled={0}, manual={1} -> cancellable={2}, refund={3}, type={4}")
    @CsvSource({
            "0, 30000, 30000, 30000, FULL",
            "0, 29999, 30000, 29999, PARTIAL",
            "10000, 1, 20000, 1, PARTIAL",
            "10000, 20000, 20000, 20000, FULL"
    })
    @DisplayName("MANUAL은 지정금액이 환불 가능 금액 이하이고 0원보다 크면 지정금액을 따른다")
    void manualAmountIsAcceptedWhenPositiveAndNotGreaterThanCancellableAmount(
            long canceledAmount,
            long manualAmount,
            long expectedCancellableAmount,
            long expectedRefundAmount,
            RefundType expectedType
    ) {
        Order order = order(30000L, canceledAmount, canceledAmount == 0 ? OrderStatus.PAID : OrderStatus.PARTIALLY_REFUNDED);

        long refundAmount = RefundPolicy.MANUAL.calculate(manualAmount, order.getCancellableAmount());
        RefundType type = order.determineRefundType(refundAmount);

        assertThat(order.getCancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(refundAmount).isEqualTo(expectedRefundAmount);
        assertThat(type).isEqualTo(expectedType);
    }

    @ParameterizedTest(name = "canceled={0}, manual={1}")
    @CsvSource({
            "0, 30001",
            "0, 0",
            "0, -1",
            "10000, 20001"
    })
    @DisplayName("MANUAL 지정금액이 허용 범위를 벗어나면 거절한다")
    void manualAmountRejectsOutOfRangeAmount(long canceledAmount, long manualAmount) {
        Order order = order(30000L, canceledAmount, canceledAmount == 0 ? OrderStatus.PAID : OrderStatus.PARTIALLY_REFUNDED);

        assertThatThrownBy(() -> RefundPolicy.MANUAL.calculate(manualAmount, order.getCancellableAmount()))
                .isInstanceOfAny(RefundException.class, IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}, refund={2} -> cancellable={3}, type={4}")
    @CsvSource({
            "PAID, 0, 30000, 30000, FULL",
            "PAID, 0, 29999, 30000, PARTIAL",
            "PARTIALLY_REFUNDED, 10000, 20000, 20000, FULL",
            "PARTIALLY_REFUNDED, 10000, 19999, 20000, PARTIAL"
    })
    @DisplayName("환불 유형은 환불금액과 환불 가능 금액의 비교로 결정한다")
    void refundTypeIsDecidedByComparingRefundAmountWithCancellableAmount(
            OrderStatus status,
            long canceledAmount,
            long refundAmount,
            long expectedCancellableAmount,
            RefundType expectedType
    ) {
        Order order = order(30000L, canceledAmount, status);

        RefundType type = order.determineRefundType(refundAmount);

        assertThat(order.getCancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(type).isEqualTo(expectedType);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}, refund={2} -> cancellable={3}")
    @CsvSource({
            "PAID, 0, 30000, 30000",
            "PAID, 0, 29999, 30000",
            "PARTIALLY_REFUNDED, 10000, 20000, 20000",
            "PARTIALLY_REFUNDED, 10000, 19999, 20000"
    })
    @DisplayName("환불 가능한 주문 상태에서는 환불금액이 환불 가능 금액 이하일 때 검증을 통과한다")
    void refundableOrderPassesValidationWhenRefundAmountDoesNotExceedCancellableAmount(
            OrderStatus status,
            long canceledAmount,
            long refundAmount,
            long expectedCancellableAmount
    ) {
        Order order = order(30000L, canceledAmount, status);

        assertThatCode(order::validateRefundable).doesNotThrowAnyException();
        assertThatCode(() -> order.determineRefundType(refundAmount)).doesNotThrowAnyException();
        assertThat(order.getCancellableAmount()).isEqualTo(expectedCancellableAmount);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}")
    @CsvSource({
            "REFUNDED, 30000",
            "PENDING, 0",
            "FAILED, 0"
    })
    @DisplayName("환불 불가 주문 상태에서는 환불 가능 검증을 거절한다")
    void nonRefundableOrderStatusRejectsRefundValidation(OrderStatus status, long canceledAmount) {
        Order order = order(30000L, canceledAmount, status);

        assertThatThrownBy(order::validateRefundable)
                .isInstanceOf(RefundException.class);
    }

    @Test
    @DisplayName("환불금액이 환불 가능 금액을 초과하면 환불 가능 검증을 거절한다")
    void refundAmountGreaterThanCancellableAmountIsRejected() {
        Order order = order(30000L, 10000L, OrderStatus.PARTIALLY_REFUNDED);

        assertThatThrownBy(() -> order.determineRefundType(20001L))
                .isInstanceOf(RefundException.class);
    }

    @ParameterizedTest(name = "before={0}, canceled={1}, refund={2} -> cumulative={3}, after={4}")
    @CsvSource({
            "PAID, 0, 30000, 30000, REFUNDED",
            "PAID, 0, 29999, 29999, PARTIALLY_REFUNDED",
            "PARTIALLY_REFUNDED, 10000, 20000, 30000, REFUNDED",
            "PARTIALLY_REFUNDED, 10000, 19999, 29999, PARTIALLY_REFUNDED"
    })
    @DisplayName("환불 적용 후 누적 환불액이 주문금액에 도달했는지에 따라 주문 상태를 전이한다")
    void applyingRefundChangesOrderStatusByCumulativeRefundAmount(
            OrderStatus beforeStatus,
            long canceledAmount,
            long refundAmount,
            long expectedCumulativeRefundAmount,
            OrderStatus expectedAfterStatus
    ) {
        Order order = order(30000L, canceledAmount, beforeStatus);

        order.applyRefund(refundAmount);

        assertThat(order.getCanceledAmount()).isEqualTo(expectedCumulativeRefundAmount);
        assertThat(order.getStatus()).isEqualTo(expectedAfterStatus);
    }

    @Test
    @DisplayName("환불 요청이 생성되면 환불 상태는 REQUESTED이다")
    void newRefundStartsWithRequestedStatus() {
        Refund refund = Refund.requested(15000L, 30000L);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.getType()).isEqualTo(RefundType.PARTIAL);
    }

    @ParameterizedTest(name = "event={0} -> status={1}")
    @CsvSource({
            "SUCCEEDED, SUCCEEDED",
            "FAILED, FAILED",
            "TIMED_OUT, TIMED_OUT"
    })
    @DisplayName("결제 취소 결과 도메인 사건에 따라 환불 상태를 전이한다")
    void paymentCancelResultEventChangesRefundStatus(String event, RefundStatus expectedStatus) {
        Refund refund = Refund.requested(15000L, RefundType.PARTIAL);

        switch (event) {
            case "SUCCEEDED" -> refund.succeed();
            case "FAILED" -> refund.fail();
            case "TIMED_OUT" -> refund.timeOut();
            default -> throw new IllegalArgumentException("unknown event: " + event);
        }

        assertThat(refund.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("종료 상태에 도달한 환불은 다른 종료 상태로 다시 전이할 수 없다")
    void terminalRefundStatusRejectsAnotherTerminalTransition() {
        Refund refund = Refund.requested(15000L, RefundType.PARTIAL);
        refund.succeed();

        assertThatThrownBy(refund::fail)
                .isInstanceOf(RefundException.class);
    }

    private static Order order(long amount, long canceledAmount, OrderStatus status) {
        return new Order("order-1", amount, canceledAmount, status);
    }
}
