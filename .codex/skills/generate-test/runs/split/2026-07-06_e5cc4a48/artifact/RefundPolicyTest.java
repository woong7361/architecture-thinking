import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundPolicyTest {

    private static final Instant PAID_AT = Instant.parse("2026-07-01T00:00:00Z");

    private RefundCalculator calculator;
    private RefundApplicationService refundService;

    @Mock
    private PaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        calculator = new RefundCalculator();
        // PG 결제 취소는 외부 API이므로 Mock 처리한다.
        refundService = new RefundApplicationService(calculator, paymentGateway);
    }

    @Test
    @DisplayName("MANUAL 지정은 7일 이하 무료와 PRORATION보다 우선해 지정금액을 따른다")
    void manualAmountOverridesFreePeriodAndProration() {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.manual(
                order,
                5000L,
                PAID_AT,
                Instant.parse("2026-07-07T23:59:59Z"),
                30,
                24
        );

        RefundQuote quote = calculator.calculate(command);

        assertThat(quote.amount()).isEqualTo(5000L);
        assertThat(quote.type()).isEqualTo(RefundType.PARTIAL);
    }

    @ParameterizedTest(name = "elapsedDays={2}, remainingDays={3} -> amount={4}, type={5}")
    @CsvSource({
            "2026-07-01T00:00:00Z, 2026-07-01T23:59:59Z, 0, 30, 30000, FULL",
            "2026-07-01T00:00:00Z, 2026-07-08T23:59:59Z, 7, 23, 30000, FULL",
            "2026-07-01T00:00:00Z, 2026-07-09T00:00:00Z, 8, 22, 22000, PARTIAL"
    })
    @DisplayName("7일 이하 무료 정책의 경계는 UTC 날짜 기준 경과일로 판단한다")
    void freeRefundBoundaryUsesUtcDateElapsedDays(
            String paidAt,
            String requestedAt,
            long expectedElapsedDays,
            int remainingDays,
            long expectedAmount,
            RefundType expectedType
    ) {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.policy(
                order,
                Instant.parse(paidAt),
                Instant.parse(requestedAt),
                30,
                remainingDays
        );

        RefundQuote quote = calculator.calculate(command);

        assertThat(quote.elapsedDays()).isEqualTo(expectedElapsedDays);
        assertThat(quote.amount()).isEqualTo(expectedAmount);
        assertThat(quote.type()).isEqualTo(expectedType);
    }

    @ParameterizedTest(name = "amount={0}, totalDays={1}, remainingDays={2} -> refund={3}, type={4}")
    @CsvSource({
            "30000, 30, 30, 30000, FULL",
            "30000, 30, 15, 15000, PARTIAL",
            "30000, 30, 1, 1000, PARTIAL",
            "30000, 30, 0, 0, PARTIAL",
            "10000, 30, 7, 2331, PARTIAL"
    })
    @DisplayName("PRORATION은 일할 단가를 정수 나눗셈으로 구하고 잔여 일수에 곱한다")
    void prorationUsesIntegerDailyRateAndRemainingDays(
            long orderAmount,
            int totalDays,
            int remainingDays,
            long expectedAmount,
            RefundType expectedType
    ) {
        Order order = paidOrder(orderAmount, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.proration(order, totalDays, remainingDays);

        RefundQuote quote = calculator.calculate(command);

        assertThat(quote.amount()).isEqualTo(expectedAmount);
        assertThat(quote.type()).isEqualTo(expectedType);
    }

    @ParameterizedTest(name = "totalDays={0}")
    @CsvSource({"0", "-1"})
    @DisplayName("PRORATION은 총 구독 일수가 0 이하이면 잘못된 구독 기간으로 거절한다")
    void prorationRejectsNonPositiveTotalDays(int totalDays) {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.proration(order, totalDays, 0);

        assertThatThrownBy(() -> calculator.calculate(command))
                .isInstanceOf(InvalidSubscriptionPeriodException.class);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}, manual={2} -> cancellable={3}, amount={4}, type={5}")
    @CsvSource({
            "PAID, 0, 30000, 30000, 30000, FULL",
            "PAID, 0, 29999, 30000, 29999, PARTIAL",
            "PARTIALLY_REFUNDED, 10000, 1, 20000, 1, PARTIAL",
            "PARTIALLY_REFUNDED, 10000, 20000, 20000, 20000, FULL"
    })
    @DisplayName("MANUAL은 지정금액이 환불 가능 금액 이하이고 0원보다 크면 지정금액을 따른다")
    void manualAmountIsAcceptedWhenPositiveAndNotGreaterThanCancellableAmount(
            OrderStatus orderStatus,
            long canceledAmount,
            long manualAmount,
            long expectedCancellableAmount,
            long expectedRefundAmount,
            RefundType expectedRefundType
    ) {
        Order order = paidOrder(30000L, canceledAmount, orderStatus);
        RefundCommand command = RefundCommand.manual(
                order,
                manualAmount,
                PAID_AT,
                Instant.parse("2026-07-20T00:00:00Z"),
                30,
                11
        );

        RefundQuote quote = calculator.calculate(command);

        assertThat(order.cancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(quote.amount()).isEqualTo(expectedRefundAmount);
        assertThat(quote.type()).isEqualTo(expectedRefundType);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}, manual={2}")
    @CsvSource({
            "PAID, 0, 30001",
            "PAID, 0, 0",
            "PAID, 0, -1",
            "PARTIALLY_REFUNDED, 10000, 20001"
    })
    @DisplayName("MANUAL 지정금액이 허용 범위를 벗어나면 거절한다")
    void manualAmountRejectsOutOfRangeAmount(OrderStatus orderStatus, long canceledAmount, long manualAmount) {
        Order order = paidOrder(30000L, canceledAmount, orderStatus);
        RefundCommand command = RefundCommand.manual(order, manualAmount, PAID_AT, Instant.parse("2026-07-20T00:00:00Z"), 30, 11);

        assertThatThrownBy(() -> calculator.calculate(command))
                .isInstanceOf(InvalidManualRefundAmountException.class);
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
            OrderStatus orderStatus,
            long canceledAmount,
            long refundAmount,
            long expectedCancellableAmount,
            RefundType expectedType
    ) {
        Order order = paidOrder(30000L, canceledAmount, orderStatus);

        RefundType refundType = calculator.decideRefundType(order, refundAmount);

        assertThat(order.cancellableAmount()).isEqualTo(expectedCancellableAmount);
        assertThat(refundType).isEqualTo(expectedType);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}, refund={2} -> cancellable={3}")
    @CsvSource({
            "PAID, 0, 30000, 30000",
            "PAID, 0, 29999, 30000",
            "PARTIALLY_REFUNDED, 10000, 20000, 20000",
            "PARTIALLY_REFUNDED, 10000, 19999, 20000"
    })
    @DisplayName("PAID와 PARTIALLY_REFUNDED 주문은 환불금액이 환불 가능 금액 이하일 때 검증을 통과한다")
    void refundableOrderPassesValidationWhenRefundAmountDoesNotExceedCancellableAmount(
            OrderStatus orderStatus,
            long canceledAmount,
            long refundAmount,
            long expectedCancellableAmount
    ) {
        Order order = paidOrder(30000L, canceledAmount, orderStatus);

        order.validateRefundable(refundAmount);

        assertThat(order.cancellableAmount()).isEqualTo(expectedCancellableAmount);
    }

    @ParameterizedTest(name = "status={0}, canceled={1}")
    @CsvSource({
            "REFUNDED, 30000",
            "PENDING, 0",
            "FAILED, 0"
    })
    @DisplayName("REFUNDED, PENDING, FAILED 주문은 환불 가능 검증을 거절한다")
    void nonRefundableOrderStatusRejectsRefundValidation(OrderStatus orderStatus, long canceledAmount) {
        Order order = paidOrder(30000L, canceledAmount, orderStatus);

        assertThatThrownBy(() -> order.validateRefundable(1000L))
                .isInstanceOf(RefundNotAllowedException.class);
    }

    @Test
    @DisplayName("환불금액이 환불 가능 금액을 초과하면 환불 가능 검증을 거절한다")
    void refundAmountGreaterThanCancellableAmountIsRejected() {
        Order order = paidOrder(30000L, 10000L, OrderStatus.PARTIALLY_REFUNDED);

        assertThatThrownBy(() -> order.validateRefundable(20001L))
                .isInstanceOf(RefundAmountExceededException.class);
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
        Order order = paidOrder(30000L, canceledAmount, beforeStatus);

        order.applyRefund(refundAmount);

        assertThat(order.canceledAmount()).isEqualTo(expectedCumulativeRefundAmount);
        assertThat(order.status()).isEqualTo(expectedAfterStatus);
    }

    @Test
    @DisplayName("환불 요청이 생성되면 환불 상태는 REQUESTED이다")
    void newRefundStartsWithRequestedStatus() {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);

        Refund refund = Refund.request(order, 15000L, RefundType.PARTIAL);

        assertThat(refund.status()).isEqualTo(RefundStatus.REQUESTED);
    }

    @ParameterizedTest(name = "event={0} -> status={1}, manualRequired={2}")
    @MethodSource("paymentCancelResultEvents")
    @DisplayName("결제 취소 결과 도메인 사건에 따라 환불 상태를 전이한다")
    void paymentCancelResultEventChangesRefundStatus(
            PaymentCancelResult result,
            RefundStatus expectedStatus,
            boolean expectedManualHandlingRequired
    ) {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        Refund refund = Refund.request(order, 15000L, RefundType.PARTIAL);

        refund.applyPaymentCancelResult(result);

        assertThat(refund.status()).isEqualTo(expectedStatus);
        assertThat(refund.requiresManualHandling()).isEqualTo(expectedManualHandlingRequired);
    }

    @Test
    @DisplayName("PG 취소 성공이면 환불은 SUCCEEDED가 되고 주문에는 환불금액이 적용된다")
    void refundServiceMarksRefundSucceededAndAppliesAmountWhenPaymentCancelSucceeds() {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.manual(order, 30000L, PAID_AT, Instant.parse("2026-07-20T00:00:00Z"), 30, 11);
        when(paymentGateway.cancel(order.paymentKey(), 30000L)).thenReturn(PaymentCancelResult.succeeded());

        Refund refund = refundService.refund(command);

        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.canceledAmount()).isEqualTo(30000L);
        verify(paymentGateway).cancel(order.paymentKey(), 30000L);
        verifyNoMoreInteractions(paymentGateway);
    }

    @Test
    @DisplayName("PG가 명확히 거부하면 환불은 FAILED가 되고 주문 환불액은 증가하지 않는다")
    void refundServiceMarksRefundFailedAndDoesNotApplyAmountWhenPaymentCancelIsRejected() {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.manual(order, 15000L, PAID_AT, Instant.parse("2026-07-20T00:00:00Z"), 30, 11);
        when(paymentGateway.cancel(order.paymentKey(), 15000L)).thenReturn(PaymentCancelResult.rejected());

        Refund refund = refundService.refund(command);

        assertThat(refund.status()).isEqualTo(RefundStatus.FAILED);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.canceledAmount()).isEqualTo(0L);
        verify(paymentGateway).cancel(order.paymentKey(), 15000L);
        verifyNoMoreInteractions(paymentGateway);
    }

    @Test
    @DisplayName("PG 결과가 불확실하면 환불은 TIMED_OUT이 되고 관리자 수동 처리가 필요하다")
    void refundServiceMarksRefundTimedOutAndRequiresManualHandlingWhenPaymentCancelIsUncertain() {
        Order order = paidOrder(30000L, 0L, OrderStatus.PAID);
        RefundCommand command = RefundCommand.manual(order, 15000L, PAID_AT, Instant.parse("2026-07-20T00:00:00Z"), 30, 11);
        when(paymentGateway.cancel(order.paymentKey(), 15000L)).thenReturn(PaymentCancelResult.uncertain());

        Refund refund = refundService.refund(command);

        assertThat(refund.status()).isEqualTo(RefundStatus.TIMED_OUT);
        assertThat(refund.requiresManualHandling()).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.canceledAmount()).isEqualTo(0L);
        verify(paymentGateway).cancel(order.paymentKey(), 15000L);
        verifyNoMoreInteractions(paymentGateway);
    }

    private static Stream<Arguments> paymentCancelResultEvents() {
        return Stream.of(
                Arguments.of(PaymentCancelResult.succeeded(), RefundStatus.SUCCEEDED, false),
                Arguments.of(PaymentCancelResult.rejected(), RefundStatus.FAILED, false),
                Arguments.of(PaymentCancelResult.uncertain(), RefundStatus.TIMED_OUT, true)
        );
    }

    private static Order paidOrder(long amount, long canceledAmount, OrderStatus status) {
        return Order.restore("order-1", "payment-key-1", amount, canceledAmount, status, PAID_AT);
    }
}
