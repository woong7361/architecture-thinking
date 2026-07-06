import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

    @Test
    @DisplayName("환불 가능 금액은 주문 금액에서 이미 환불된 금액을 뺀 금액이다")
    void calculateCancellableAmount() {
        Order order = Order.partiallyRefunded("order-1", 30_000L, 10_000L);

        long cancellableAmount = order.cancellableAmount();

        assertThat(cancellableAmount).isEqualTo(20_000L);
    }

    @ParameterizedTest(name = "{0} 상태는 환불 불가")
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "FAILED", "REFUNDED"})
    @DisplayName("PAID와 PARTIALLY_REFUNDED가 아닌 주문은 환불할 수 없다")
    void rejectNonRefundableOrderStatus(OrderStatus status) {
        Order order = Order.of("order-1", 30_000L, 0L, status);

        assertThatThrownBy(() -> order.validateRefundable(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("환불 금액이 환불 가능 금액을 초과하면 거절한다")
    void rejectRefundAmountGreaterThanCancellableAmount() {
        Order order = Order.partiallyRefunded("order-1", 30_000L, 10_000L);

        assertThatThrownBy(() -> order.validateRefundable(20_001L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0}, 기존환불 {1}원, 추가환불 {2}원 -> {3}")
    @MethodSource("orderTransitionCases")
    @DisplayName("환불 적용 후 누적 환불 금액에 따라 주문 상태를 전이한다")
    void applyRefundAndTransitionStatus(
            OrderStatus beforeStatus,
            long canceledAmount,
            long refundAmount,
            OrderStatus expectedStatus,
            long expectedCanceledAmount
    ) {
        Order order = Order.of("order-1", 30_000L, canceledAmount, beforeStatus);

        order.applyRefund(refundAmount);

        assertThat(order.status()).isEqualTo(expectedStatus);
        assertThat(order.canceledAmount()).isEqualTo(expectedCanceledAmount);
    }

    static Stream<Arguments> orderTransitionCases() {
        return Stream.of(
                Arguments.of(OrderStatus.PAID, 0L, 30_000L, OrderStatus.REFUNDED, 30_000L),
                Arguments.of(OrderStatus.PAID, 0L, 15_000L, OrderStatus.PARTIALLY_REFUNDED, 15_000L),
                Arguments.of(OrderStatus.PARTIALLY_REFUNDED, 10_000L, 20_000L, OrderStatus.REFUNDED, 30_000L),
                Arguments.of(OrderStatus.PARTIALLY_REFUNDED, 10_000L, 15_000L, OrderStatus.PARTIALLY_REFUNDED, 25_000L)
        );
    }
}
