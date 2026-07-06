import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderTest {

    @ParameterizedTest(name = "status={0}, alreadyRefunded={1}, requested={2}, result={4}")
    @CsvSource({
            "PAID,0,30000,30000,true,NONE",
            "PARTIALLY_REFUNDED,10000,20000,20000,true,NONE",
            "PAID,10000,20001,20000,false,REFUND_AMOUNT_EXCEEDS_CANCELLABLE",
            "REFUNDED,30000,1,0,false,ORDER_NOT_REFUNDABLE",
            "PENDING,0,1,30000,false,ORDER_NOT_REFUNDABLE",
            "FAILED,0,1,30000,false,ORDER_NOT_REFUNDABLE"
    })
    @DisplayName("환불 가능 여부는 주문 상태와 환불 가능 금액으로 검증한다")
    void validateRefundable_checksStatusAndCancellableAmount(
            OrderStatus status,
            long alreadyRefundedAmount,
            long requestedRefundAmount,
            long expectedCancellableAmount,
            boolean refundable,
            String expectedErrorCode
    ) {
        Order order = new Order(30_000L, alreadyRefundedAmount, status);

        assertThat(order.cancellableAmount()).isEqualTo(expectedCancellableAmount);

        if (refundable) {
            order.validateRefundable(requestedRefundAmount);
            assertThat(order.refundedAmount()).isEqualTo(alreadyRefundedAmount);
            assertThat(order.status()).isEqualTo(status);
            return;
        }

        assertThatThrownBy(() -> order.validateRefundable(requestedRefundAmount))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.valueOf(expectedErrorCode));
        assertThat(order.refundedAmount()).isEqualTo(alreadyRefundedAmount);
        assertThat(order.status()).isEqualTo(status);
    }

    @ParameterizedTest(name = "before={0}, alreadyRefunded={1}, refundAmount={2}, after={4}")
    @CsvSource({
            "PAID,0,30000,30000,REFUNDED",
            "PAID,0,10000,10000,PARTIALLY_REFUNDED",
            "PARTIALLY_REFUNDED,10000,20000,30000,REFUNDED",
            "PARTIALLY_REFUNDED,10000,19999,29999,PARTIALLY_REFUNDED"
    })
    @DisplayName("환불 성공 시 주문 상태는 누적 환불 금액으로 전이된다")
    void applyRefund_transitionsStatusByAccumulatedRefundAmount(
            OrderStatus beforeStatus,
            long alreadyRefundedAmount,
            long refundAmount,
            long expectedRefundedAmount,
            OrderStatus expectedStatus
    ) {
        Order order = new Order(30_000L, alreadyRefundedAmount, beforeStatus);

        order.applyRefund(refundAmount);

        assertThat(order.refundedAmount()).isEqualTo(expectedRefundedAmount);
        assertThat(order.status()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "status={0}, alreadyRefunded={1}, requested={2}")
    @CsvSource({
            "REFUNDED,30000,1",
            "PENDING,0,1",
            "FAILED,0,1"
    })
    @DisplayName("환불 불가 주문에 환불을 적용할 수 없다")
    void applyRefund_rejectsNonRefundableOrderStatus(OrderStatus status, long alreadyRefundedAmount, long refundAmount) {
        Order order = new Order(30_000L, alreadyRefundedAmount, status);

        assertThatThrownBy(() -> order.applyRefund(refundAmount))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.ORDER_NOT_REFUNDABLE);
        assertThat(order.refundedAmount()).isEqualTo(alreadyRefundedAmount);
        assertThat(order.status()).isEqualTo(status);
    }

    @ParameterizedTest(name = "alreadyRefunded={0}, requested={1}")
    @CsvSource({
            "10000,20001",
            "29999,2"
    })
    @DisplayName("환불 가능 금액을 초과해 주문에 환불을 적용할 수 없다")
    void applyRefund_rejectsAmountExceedingCancellableAmount(long alreadyRefundedAmount, long refundAmount) {
        Order order = new Order(30_000L, alreadyRefundedAmount, OrderStatus.PARTIALLY_REFUNDED);

        assertThatThrownBy(() -> order.applyRefund(refundAmount))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo(DomainErrorCode.REFUND_AMOUNT_EXCEEDS_CANCELLABLE);
        assertThat(order.refundedAmount()).isEqualTo(alreadyRefundedAmount);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
    }
}
