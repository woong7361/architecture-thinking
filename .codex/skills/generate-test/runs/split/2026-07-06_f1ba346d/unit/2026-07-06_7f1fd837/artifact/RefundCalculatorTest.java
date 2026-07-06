import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator();

    @ParameterizedTest(name = "elapsedDays={4}, expectedAmount={7}, expectedType={8}")
    @CsvSource({
            "30000,0,2026-07-01T00:00:00Z,2026-07-01T00:00:00Z,0,30,30,30000,FULL",
            "30000,0,2026-07-01T00:00:00Z,2026-07-08T00:00:00Z,7,30,23,30000,FULL",
            "30000,0,2026-07-01T00:00:00Z,2026-07-09T00:00:00Z,8,30,22,22000,PARTIAL"
    })
    @DisplayName("MANUAL이 없으면 UTC 날짜 기준 경과일 7일까지 전액 환불이고 8일부터 PRORATION을 적용한다")
    void calculate_appliesFreeRefundUntilSevenUtcDates(
            long orderAmount,
            long alreadyRefundedAmount,
            Instant paidAt,
            Instant requestedAt,
            long expectedElapsedDays,
            int totalDays,
            int remainingDays,
            long expectedRefundAmount,
            RefundType expectedRefundType
    ) {
        Order order = new Order(orderAmount, alreadyRefundedAmount, OrderStatus.PAID);

        RefundCalculation result = calculator.calculate(order, RefundPolicy.PRORATION, Optional.empty(), paidAt, requestedAt, totalDays, remainingDays);

        assertThat(result.elapsedDays()).isEqualTo(expectedElapsedDays);
        assertThat(result.cancellableAmount()).isEqualTo(orderAmount - alreadyRefundedAmount);
        assertThat(result.amount()).isEqualTo(expectedRefundAmount);
        assertThat(result.type()).isEqualTo(expectedRefundType);
    }

    @ParameterizedTest(name = "paidAt={0}, requestedAt={1}, expectedElapsedDays={2}")
    @CsvSource({
            "2026-07-01T23:59:59Z,2026-07-08T00:00:00Z,7,30000,FULL",
            "2026-07-01T00:00:01Z,2026-07-09T23:59:59Z,8,22000,PARTIAL"
    })
    @DisplayName("7일 무료 경계는 UTC 시분초를 제거한 날짜 차이로 판정한다")
    void calculate_truncatesUtcTimeBeforeElapsedDays(
            Instant paidAt,
            Instant requestedAt,
            long expectedElapsedDays,
            long expectedRefundAmount,
            RefundType expectedRefundType
    ) {
        Order order = new Order(30_000L, 0L, OrderStatus.PAID);

        RefundCalculation result = calculator.calculate(order, RefundPolicy.PRORATION, Optional.empty(), paidAt, requestedAt, 30, 22);

        assertThat(result.elapsedDays()).isEqualTo(expectedElapsedDays);
        assertThat(result.amount()).isEqualTo(expectedRefundAmount);
        assertThat(result.type()).isEqualTo(expectedRefundType);
    }

    @Test
    @DisplayName("MANUAL 지정 금액은 7일 무료와 PRORATION보다 우선하고 PARTIAL 유형이 된다")
    void calculate_manualAmountOverridesFreeRefundAndProration() {
        Order order = new Order(30_000L, 0L, OrderStatus.PAID);

        RefundCalculation result = calculator.calculate(
                order,
                RefundPolicy.MANUAL,
                Optional.of(10_000L),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-03T00:00:00Z"),
                30,
                30
        );

        assertThat(result.amount()).isEqualTo(10_000L);
        assertThat(result.cancellableAmount()).isEqualTo(30_000L);
        assertThat(result.type()).isEqualTo(RefundType.PARTIAL);
    }

    @ParameterizedTest(name = "alreadyRefunded={0}, manualAmount={1}, expectedType={3}")
    @CsvSource({
            "0,30000,30000,FULL",
            "10000,20000,20000,FULL"
    })
    @DisplayName("MANUAL 지정 금액이 환불 가능 금액과 같으면 허용되고 FULL 유형이 된다")
    void calculate_allowsManualAmountEqualToCancellableAmount(
            long alreadyRefundedAmount,
            long manualAmount,
            long expectedCancellableAmount,
            RefundType expectedRefundType
    ) {
        Order order = new Order(30_000L, alreadyRefundedAmount, alreadyRefundedAmount == 0L ? OrderStatus.PAID : OrderStatus.PARTIALLY_REFUNDED);

        RefundCalculation result = calculator.calculate(
                order,
                RefundPolicy.MANUAL,
                Optional.of(manualAmount),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-03T00:00:00Z"),
                30,
                30
        );

        assertThat(result.amount()).isEqualTo(manualAmount);
        assertThat(result.cancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(result.type()).isEqualTo(expectedRefundType);
    }

    @ParameterizedTest(name = "orderAmount={0}, totalDays={1}, remainingDays={2}, unitPrice={3}, refundAmount={4}")
    @CsvSource({
            "30000,30,30,1000,30000,FULL",
            "30000,30,15,1000,15000,PARTIAL",
            "30000,30,1,1000,1000,PARTIAL",
            "30000,30,0,1000,0,PARTIAL",
            "10000,30,7,333,2331,PARTIAL"
    })
    @DisplayName("PRORATION은 정수 일할 단가와 잔여 일수로 환불 금액을 산출한다")
    void calculate_prorationUsesIntegerDailyUnitAndRemainingDays(
            long orderAmount,
            int totalDays,
            int remainingDays,
            long expectedDailyUnitAmount,
            long expectedRefundAmount,
            RefundType expectedRefundType
    ) {
        Order order = new Order(orderAmount, 0L, OrderStatus.PAID);

        RefundCalculation result = calculator.calculate(
                order,
                RefundPolicy.PRORATION,
                Optional.empty(),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                totalDays,
                remainingDays
        );

        assertThat(result.dailyUnitAmount()).isEqualTo(expectedDailyUnitAmount);
        assertThat(result.amount()).isEqualTo(expectedRefundAmount);
        assertThat(result.cancellableAmount()).isEqualTo(orderAmount);
        assertThat(result.type()).isEqualTo(expectedRefundType);
    }

    @ParameterizedTest(name = "remainingDays={0}, expectedAmount={1}, expectedType={3}")
    @CsvSource({
            "20,20000,20000,FULL",
            "19,19000,20000,PARTIAL"
    })
    @DisplayName("부분 환불 주문의 PRORATION 산출 금액은 환불 가능 금액과 비교해 FULL 또는 PARTIAL이 된다")
    void calculate_prorationForPartiallyRefundedOrderComparesWithCancellableAmount(
            int remainingDays,
            long expectedRefundAmount,
            long expectedCancellableAmount,
            RefundType expectedRefundType
    ) {
        Order order = new Order(30_000L, 10_000L, OrderStatus.PARTIALLY_REFUNDED);

        RefundCalculation result = calculator.calculate(
                order,
                RefundPolicy.PRORATION,
                Optional.empty(),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                30,
                remainingDays
        );

        assertThat(result.dailyUnitAmount()).isEqualTo(1_000L);
        assertThat(result.amount()).isEqualTo(expectedRefundAmount);
        assertThat(result.cancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(result.type()).isEqualTo(expectedRefundType);
    }

    @ParameterizedTest(name = "manualAmount={0}")
    @CsvSource({"0", "-1"})
    @DisplayName("MANUAL 지정 금액이 0 이하이면 도메인 오류가 된다")
    void calculate_rejectsNonPositiveManualAmount(long manualAmount) {
        Order order = new Order(30_000L, 0L, OrderStatus.PAID);

        assertThatThrownBy(() -> calculator.calculate(
                order,
                RefundPolicy.MANUAL,
                Optional.of(manualAmount),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                30,
                15
        ))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.INVALID_MANUAL_REFUND_AMOUNT);
    }

    @ParameterizedTest(name = "alreadyRefunded={0}, requestedManualAmount={1}")
    @CsvSource({
            "10000,20001",
            "10000,30000"
    })
    @DisplayName("MANUAL 지정 금액이 환불 가능 금액을 초과하면 도메인 오류가 된다")
    void calculate_rejectsManualAmountExceedingCancellableAmount(long alreadyRefundedAmount, long manualAmount) {
        Order order = new Order(30_000L, alreadyRefundedAmount, OrderStatus.PARTIALLY_REFUNDED);

        assertThatThrownBy(() -> calculator.calculate(
                order,
                RefundPolicy.MANUAL,
                Optional.of(manualAmount),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                30,
                15
        ))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.REFUND_AMOUNT_EXCEEDS_CANCELLABLE);
        assertThat(order.refundedAmount()).isEqualTo(alreadyRefundedAmount);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
    }

    @ParameterizedTest(name = "requestedAt={1}, remainingDays={3}, cancellableAmount={4}")
    @CsvSource({
            "2026-07-01T00:00:00Z,2026-07-04T00:00:00Z,30,20,20000",
            "2026-07-01T00:00:00Z,2026-07-09T00:00:00Z,30,25,20000"
    })
    @DisplayName("MANUAL이 없는 자동 산출 금액이 환불 가능 금액을 초과하면 도메인 오류가 된다")
    void calculate_rejectsAutomaticAmountExceedingCancellableAmount(
            Instant paidAt,
            Instant requestedAt,
            int totalDays,
            int remainingDays,
            long expectedCancellableAmount
    ) {
        Order order = new Order(30_000L, 10_000L, OrderStatus.PARTIALLY_REFUNDED);

        assertThat(order.cancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThatThrownBy(() -> calculator.calculate(
                order,
                RefundPolicy.PRORATION,
                Optional.empty(),
                paidAt,
                requestedAt,
                totalDays,
                remainingDays
        ))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.REFUND_AMOUNT_EXCEEDS_CANCELLABLE);
        assertThat(order.refundedAmount()).isEqualTo(10_000L);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
    }

    @ParameterizedTest(name = "totalDays={0}")
    @CsvSource({"0", "-1"})
    @DisplayName("PRORATION 총 구독 일수가 0 이하이면 도메인 오류가 된다")
    void calculate_rejectsInvalidSubscriptionPeriod(int totalDays) {
        Order order = new Order(30_000L, 0L, OrderStatus.PAID);

        assertThatThrownBy(() -> calculator.calculate(
                order,
                RefundPolicy.PRORATION,
                Optional.empty(),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                totalDays,
                0
        ))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.INVALID_SUBSCRIPTION_PERIOD);
    }

    @ParameterizedTest(name = "alreadyRefunded={1}, refundAmount={2}, expectedType={4}")
    @CsvSource({
            "PAID,0,30000,30000,FULL",
            "PAID,0,29999,30000,PARTIAL",
            "PARTIALLY_REFUNDED,10000,20000,20000,FULL",
            "PARTIALLY_REFUNDED,10000,19999,20000,PARTIAL"
    })
    @DisplayName("환불 유형은 확정된 환불금액과 환불 가능 금액을 비교해 결정한다")
    void determineType_comparesRefundAmountWithCancellableAmount(
            OrderStatus orderStatus,
            long alreadyRefundedAmount,
            long refundAmount,
            long expectedCancellableAmount,
            RefundType expectedRefundType
    ) {
        Order order = new Order(30_000L, alreadyRefundedAmount, orderStatus);

        RefundType type = calculator.determineType(order, refundAmount);

        assertThat(order.cancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(type).isEqualTo(expectedRefundType);
    }
}
