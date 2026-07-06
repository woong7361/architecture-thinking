import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RefundApplicationServiceTest {

    @Mock
    private PaymentCancellationGateway paymentCancellationGateway;

    private RefundApplicationService refundApplicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        refundApplicationService = new RefundApplicationService(new RefundCalculator(), paymentCancellationGateway);
    }

    @Test
    @DisplayName("7일 이하 무료 환불이 성공하면 전액 환불되고 주문과 환불 상태가 성공으로 전이된다")
    void requestRefund_freeRefundWithinSevenDaysSucceedsAsFullRefund() {
        Order order = new Order(30_000L, 0L, OrderStatus.PAID);
        RefundRequest request = RefundRequest.proration(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-04T00:00:00Z"),
                30,
                20
        );
        when(paymentCancellationGateway.cancel(order, 30_000L)).thenReturn(PaymentCancellationResult.SUCCESS);

        Refund refund = refundApplicationService.refund(order, request);

        assertThat(refund.amount()).isEqualTo(30_000L);
        assertThat(refund.type()).isEqualTo(RefundType.FULL);
        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(order.refundedAmount()).isEqualTo(30_000L);
        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
        verify(paymentCancellationGateway).cancel(order, 30_000L);
    }

    @ParameterizedTest(name = "remainingDays={2}, refundAmount={3}, refundType={4}, orderStatus={6}")
    @CsvSource({
            "10000,PARTIALLY_REFUNDED,20,20000,FULL,30000,REFUNDED",
            "10000,PARTIALLY_REFUNDED,19,19000,PARTIAL,29000,PARTIALLY_REFUNDED"
    })
    @DisplayName("부분 환불 주문의 PRORATION 정상 산출은 환불 가능 금액과 비교해 유형과 주문 상태를 결정한다")
    void requestRefund_prorationForPartiallyRefundedOrderSucceedsWithinCancellableAmount(
            long alreadyRefundedAmount,
            OrderStatus existingOrderStatus,
            int remainingDays,
            long expectedRefundAmount,
            RefundType expectedRefundType,
            long expectedOrderRefundedAmount,
            OrderStatus expectedOrderStatus
    ) {
        Order order = new Order(30_000L, alreadyRefundedAmount, existingOrderStatus);
        RefundRequest request = RefundRequest.proration(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                30,
                remainingDays
        );
        when(paymentCancellationGateway.cancel(order, expectedRefundAmount)).thenReturn(PaymentCancellationResult.SUCCESS);

        Refund refund = refundApplicationService.refund(order, request);

        assertThat(refund.amount()).isEqualTo(expectedRefundAmount);
        assertThat(refund.type()).isEqualTo(expectedRefundType);
        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(order.refundedAmount()).isEqualTo(expectedOrderRefundedAmount);
        assertThat(order.status()).isEqualTo(expectedOrderStatus);
        verify(paymentCancellationGateway).cancel(order, expectedRefundAmount);
    }

    @ParameterizedTest(name = "alreadyRefunded={0}, manualAmount={2}, refundType={3}, orderStatus={5}")
    @CsvSource({
            "0,PAID,30000,FULL,30000,REFUNDED",
            "10000,PARTIALLY_REFUNDED,20000,FULL,30000,REFUNDED"
    })
    @DisplayName("MANUAL 지정 금액이 환불 가능 금액과 같으면 전액 환불로 성공한다")
    void requestRefund_manualAmountEqualToCancellableAmountSucceedsAsFullRefund(
            long alreadyRefundedAmount,
            OrderStatus existingOrderStatus,
            long manualRefundAmount,
            RefundType expectedRefundType,
            long expectedOrderRefundedAmount,
            OrderStatus expectedOrderStatus
    ) {
        Order order = new Order(30_000L, alreadyRefundedAmount, existingOrderStatus);
        RefundRequest request = RefundRequest.manual(
                manualRefundAmount,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-03T00:00:00Z"),
                30,
                30
        );
        when(paymentCancellationGateway.cancel(order, manualRefundAmount)).thenReturn(PaymentCancellationResult.SUCCESS);

        Refund refund = refundApplicationService.refund(order, request);

        assertThat(refund.amount()).isEqualTo(manualRefundAmount);
        assertThat(refund.type()).isEqualTo(expectedRefundType);
        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(order.refundedAmount()).isEqualTo(expectedOrderRefundedAmount);
        assertThat(order.status()).isEqualTo(expectedOrderStatus);
        verify(paymentCancellationGateway).cancel(order, manualRefundAmount);
    }

    @ParameterizedTest(name = "paymentResult={3}, refundStatus={4}, orderStatus={6}")
    @CsvSource({
            "PAID,0,10000,SUCCESS,SUCCEEDED,10000,PARTIALLY_REFUNDED",
            "PAID,0,10000,REJECTED,FAILED,0,PAID",
            "PAID,0,10000,UNKNOWN,TIMED_OUT,0,PAID"
    })
    @DisplayName("결제 취소 결과에 따라 환불 상태가 바뀌고 성공한 경우에만 주문에 환불이 적용된다")
    void requestRefund_appliesOrderOnlyWhenPaymentCancellationSucceeded(
            OrderStatus existingOrderStatus,
            long existingRefundedAmount,
            long manualRefundAmount,
            PaymentCancellationResult paymentResult,
            RefundStatus expectedRefundStatus,
            long expectedOrderRefundedAmount,
            OrderStatus expectedOrderStatus
    ) {
        Order order = new Order(30_000L, existingRefundedAmount, existingOrderStatus);
        RefundRequest request = RefundRequest.manual(
                manualRefundAmount,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                30,
                20
        );
        when(paymentCancellationGateway.cancel(order, manualRefundAmount)).thenReturn(paymentResult);

        Refund refund = refundApplicationService.refund(order, request);

        assertThat(refund.amount()).isEqualTo(manualRefundAmount);
        assertThat(refund.type()).isEqualTo(RefundType.PARTIAL);
        assertThat(refund.status()).isEqualTo(expectedRefundStatus);
        assertThat(order.refundedAmount()).isEqualTo(expectedOrderRefundedAmount);
        assertThat(order.status()).isEqualTo(expectedOrderStatus);
        verify(paymentCancellationGateway).cancel(order, manualRefundAmount);
    }

    @ParameterizedTest(name = "manualAmount={0}")
    @CsvSource({"0", "-1"})
    @DisplayName("환불 산출이 도메인 오류이면 결제 취소를 호출하지 않고 주문은 변경되지 않는다")
    void requestRefund_doesNotCancelPaymentWhenCalculationRejected(long manualAmount) {
        Order order = new Order(30_000L, 0L, OrderStatus.PAID);
        RefundRequest request = RefundRequest.manual(
                manualAmount,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-10T00:00:00Z"),
                30,
                15
        );

        assertThatThrownBy(() -> refundApplicationService.refund(order, request))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.INVALID_MANUAL_REFUND_AMOUNT);
        assertThat(order.refundedAmount()).isEqualTo(0L);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        verifyNoInteractions(paymentCancellationGateway);
    }

    @ParameterizedTest(name = "requestedAt={1}, remainingDays={3}")
    @CsvSource({
            "2026-07-01T00:00:00Z,2026-07-04T00:00:00Z,30,20",
            "2026-07-01T00:00:00Z,2026-07-09T00:00:00Z,30,25"
    })
    @DisplayName("MANUAL이 없는 산출 금액이 환불 가능 금액을 초과하면 결제 취소를 호출하지 않고 주문은 변경되지 않는다")
    void requestRefund_doesNotCancelPaymentWhenAutomaticAmountExceedsCancellableAmount(
            Instant paidAt,
            Instant requestedAt,
            int totalDays,
            int remainingDays
    ) {
        Order order = new Order(30_000L, 10_000L, OrderStatus.PARTIALLY_REFUNDED);
        RefundRequest request = RefundRequest.proration(paidAt, requestedAt, totalDays, remainingDays);

        assertThat(order.cancellableAmount()).isEqualTo(20_000L);
        assertThatThrownBy(() -> refundApplicationService.refund(order, request))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.REFUND_AMOUNT_EXCEEDS_CANCELLABLE);
        assertThat(order.refundedAmount()).isEqualTo(10_000L);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
        verifyNoInteractions(paymentCancellationGateway);
    }
}
