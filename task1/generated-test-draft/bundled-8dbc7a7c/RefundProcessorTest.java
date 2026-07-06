import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundProcessorTest {

    private PaymentCancellationGateway paymentGateway;
    private RefundProcessor processor;

    @BeforeEach
    void setUp() {
        paymentGateway = mock(PaymentCancellationGateway.class);
        processor = new RefundProcessor(paymentGateway);
    }

    @Test
    @DisplayName("결제 취소가 성공하면 환불은 SUCCEEDED가 되고 주문에는 환불 금액이 적용된다")
    void succeedsRefundAndAppliesOrderWhenPaymentCancellationApproved() {
        Order order = Order.paid("order-1", "payment-1", 30_000);
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);
        when(paymentGateway.cancel("payment-1", 15_000)).thenReturn(PaymentCancellationResult.approved());

        processor.cancelPayment(order, refund);

        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
        assertThat(order.cancellableAmount()).isEqualTo(15_000);
        verify(paymentGateway).cancel("payment-1", 15_000);
    }

    @Test
    @DisplayName("결제 취소가 명확히 거부되면 환불은 FAILED가 되고 주문 상태는 변경되지 않는다")
    void marksRefundFailedWhenPaymentCancellationRejected() {
        Order order = Order.paid("order-1", "payment-1", 30_000);
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);
        when(paymentGateway.cancel("payment-1", 15_000)).thenReturn(PaymentCancellationResult.rejected());

        processor.cancelPayment(order, refund);

        assertThat(refund.status()).isEqualTo(RefundStatus.FAILED);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.cancellableAmount()).isEqualTo(30_000);
        verify(paymentGateway).cancel("payment-1", 15_000);
    }

    @Test
    @DisplayName("결제 취소 결과가 불확실하면 환불은 TIMED_OUT이 되고 주문 상태는 변경되지 않는다")
    void marksRefundTimedOutWhenPaymentCancellationUncertain() {
        Order order = Order.paid("order-1", "payment-1", 30_000);
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);
        when(paymentGateway.cancel("payment-1", 15_000)).thenReturn(PaymentCancellationResult.uncertain());

        processor.cancelPayment(order, refund);

        assertThat(refund.status()).isEqualTo(RefundStatus.TIMED_OUT);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.cancellableAmount()).isEqualTo(30_000);
        verify(paymentGateway).cancel("payment-1", 15_000);
    }
}
