import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

    @ParameterizedTest(name = "{0} 상태에서 {2}원 환불 가능")
    @MethodSource("refundableOrders")
    @DisplayName("PAID와 PARTIALLY_REFUNDED 주문은 환불 가능 금액 이하로 환불할 수 있다")
    void paidAndPartiallyRefundedOrdersAreRefundable(OrderStatus status, long canceledAmount, long refundAmount) {
        Order order = new Order(30_000L, canceledAmount, status);

        assertThatCode(() -> order.assertRefundable(refundAmount))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> refundableOrders() {
        return Stream.of(
                Arguments.of(OrderStatus.PAID, 0L, 30_000L),
                Arguments.of(OrderStatus.PAID, 0L, 15_000L),
                Arguments.of(OrderStatus.PARTIALLY_REFUNDED, 10_000L, 20_000L),
                Arguments.of(OrderStatus.PARTIALLY_REFUNDED, 10_000L, 19_999L)
        );
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"REFUNDED", "PENDING", "FAILED"})
    @DisplayName("REFUNDED, PENDING, FAILED 주문은 환불할 수 없다")
    void refundedPendingAndFailedOrdersAreNotRefundable(OrderStatus status) {
        Order order = new Order(30_000L, status == OrderStatus.REFUNDED ? 30_000L : 0L, status);

        assertThatThrownBy(() -> order.assertRefundable(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "amount={0}, canceledAmount={1}, refundAmount={2}")
    @CsvSource({
            "30000, 0, 30001",
            "30000, 10000, 20001"
    })
    @DisplayName("환불 금액은 환불 가능 금액을 초과할 수 없다")
    void refundAmountMustNotExceedCancellableAmount(long amount, long canceledAmount, long refundAmount) {
        Order order = new Order(amount, canceledAmount, canceledAmount == 0 ? OrderStatus.PAID : OrderStatus.PARTIALLY_REFUNDED);

        assertThatThrownBy(() -> order.assertRefundable(refundAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0}에서 {2}원 적용 후 {3}")
    @MethodSource("stateTransitionCases")
    @DisplayName("환불 적용 후 주문 상태는 누적 환불 금액으로 결정된다")
    void applyingRefundTransitionsOrderStatus(
            OrderStatus initialStatus,
            long canceledAmount,
            long refundAmount,
            OrderStatus expectedStatus,
            long expectedCanceledAmount
    ) {
        Order order = new Order(30_000L, canceledAmount, initialStatus);

        order.applyRefund(refundAmount);

        assertThat(order.status()).isEqualTo(expectedStatus);
        assertThat(order.canceledAmount()).isEqualTo(expectedCanceledAmount);
        assertThat(order.cancellableAmount()).isEqualTo(30_000L - expectedCanceledAmount);
    }

    static Stream<Arguments> stateTransitionCases() {
        return Stream.of(
                Arguments.of(OrderStatus.PAID, 0L, 30_000L, OrderStatus.REFUNDED, 30_000L),
                Arguments.of(OrderStatus.PAID, 0L, 29_999L, OrderStatus.PARTIALLY_REFUNDED, 29_999L),
                Arguments.of(OrderStatus.PARTIALLY_REFUNDED, 10_000L, 20_000L, OrderStatus.REFUNDED, 30_000L),
                Arguments.of(OrderStatus.PARTIALLY_REFUNDED, 10_000L, 19_999L, OrderStatus.PARTIALLY_REFUNDED, 29_999L)
        );
    }
}
