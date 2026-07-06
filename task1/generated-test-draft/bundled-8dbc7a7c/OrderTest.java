import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderTest {

    @ParameterizedTest(name = "{0} 상태에서 환불 가능 {2}원 중 {3}원 환불 가능")
    @CsvSource({
            "PAID, 0, 30000, 15000",
            "PAID, 0, 30000, 30000",
            "PARTIALLY_REFUNDED, 15000, 15000, 10000",
            "PARTIALLY_REFUNDED, 15000, 15000, 15000"
    })
    @DisplayName("PAID와 PARTIALLY_REFUNDED 주문은 환불 가능 금액 이하로 환불할 수 있다")
    void allowsRefundForRefundableStates(OrderStatus status, long alreadyRefundedAmount, long cancellableAmount, long refundAmount) {
        Order order = order(status, 30_000, alreadyRefundedAmount);

        assertThatCode(() -> order.validateRefundable(refundAmount))
                .doesNotThrowAnyException();
        assertThat(order.status()).isEqualTo(status);
        assertThat(order.cancellableAmount()).isEqualTo(cancellableAmount);
    }

    @ParameterizedTest(name = "{0} 상태, 환불 가능 {2}원, 요청 {3}원")
    @CsvSource({
            "REFUNDED, 30000, 0, 1",
            "PENDING, 0, 30000, 1000",
            "FAILED, 0, 30000, 1000",
            "PAID, 0, 30000, 30001",
            "PARTIALLY_REFUNDED, 15000, 15000, 15001"
    })
    @DisplayName("환불 불가 상태이거나 환불 가능 금액을 초과하면 상태와 환불 가능 금액을 변경하지 않고 거절한다")
    void rejectsNonRefundableStateOrAmountExceeded(OrderStatus status, long alreadyRefundedAmount, long cancellableAmount, long refundAmount) {
        Order order = order(status, 30_000, alreadyRefundedAmount);

        assertThatThrownBy(() -> order.validateRefundable(refundAmount))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.status()).isEqualTo(status);
        assertThat(order.cancellableAmount()).isEqualTo(cancellableAmount);
    }

    @Test
    @DisplayName("PAID 주문이 전액 환불되면 REFUNDED로 전이한다")
    void paidOrderBecomesRefundedWhenFullyRefunded() {
        Order order = Order.paid("order-1", "payment-1", 30_000);

        order.applyRefund(30_000);

        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.cancellableAmount()).isZero();
    }

    @Test
    @DisplayName("PAID 주문이 부분 환불되면 PARTIALLY_REFUNDED로 전이한다")
    void paidOrderBecomesPartiallyRefundedWhenPartialRefunded() {
        Order order = Order.paid("order-1", "payment-1", 30_000);

        order.applyRefund(15_000);

        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
        assertThat(order.cancellableAmount()).isEqualTo(15_000);
    }

    @Test
    @DisplayName("부분 환불 주문이 추가 환불로 전액 도달하면 REFUNDED로 전이한다")
    void partiallyRefundedOrderBecomesRefundedWhenCumulativeRefundReachesAmount() {
        Order order = Order.partiallyRefunded("order-1", "payment-1", 30_000, 15_000);

        order.applyRefund(15_000);

        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.cancellableAmount()).isZero();
    }

    @Test
    @DisplayName("부분 환불 주문이 추가 환불 후에도 잔액이 있으면 PARTIALLY_REFUNDED를 유지한다")
    void partiallyRefundedOrderRemainsPartiallyRefundedWhenCumulativeRefundIsLessThanAmount() {
        Order order = Order.partiallyRefunded("order-1", "payment-1", 30_000, 10_000);

        order.applyRefund(15_000);

        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
        assertThat(order.cancellableAmount()).isEqualTo(5_000);
    }

    private Order order(OrderStatus status, long amount, long alreadyRefundedAmount) {
        return switch (status) {
            case PAID -> Order.paid("order-1", "payment-1", amount);
            case PARTIALLY_REFUNDED -> Order.partiallyRefunded("order-1", "payment-1", amount, alreadyRefundedAmount);
            case REFUNDED -> Order.refunded("order-1", "payment-1", amount);
            case PENDING -> Order.pending("order-1", amount);
            case FAILED -> Order.failed("order-1", amount);
        };
    }
}
