import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RefundTest {

    @Test
    @DisplayName("환불은 REQUESTED 상태로 생성된다")
    void createsRequestedRefund() {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);

        assertThat(refund.status()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.amount()).isEqualTo(15_000);
    }

    @Test
    @DisplayName("REQUESTED 환불은 결제 취소 성공 시 SUCCEEDED로 전이한다")
    void requestedRefundSucceeds() {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);

        refund.succeed();

        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("REQUESTED 환불은 결제 취소가 명확히 거부되면 FAILED로 전이한다")
    void requestedRefundFails() {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);

        refund.fail();

        assertThat(refund.status()).isEqualTo(RefundStatus.FAILED);
    }

    @Test
    @DisplayName("REQUESTED 환불은 결제 취소 결과가 불확실하면 TIMED_OUT으로 전이한다")
    void requestedRefundTimesOut() {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);

        refund.timeout();

        assertThat(refund.status()).isEqualTo(RefundStatus.TIMED_OUT);
    }

    @ParameterizedTest(name = "terminalStatus={0}")
    @EnumSource(value = RefundStatus.class, names = {"SUCCEEDED", "FAILED", "TIMED_OUT"})
    @DisplayName("종료 상태의 환불은 성공 상태로 다시 전이할 수 없다")
    void terminalRefundCannotSucceedAgain(RefundStatus terminalStatus) {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);
        moveTo(refund, terminalStatus);

        assertThatThrownBy(refund::succeed)
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "terminalStatus={0}")
    @EnumSource(value = RefundStatus.class, names = {"SUCCEEDED", "FAILED", "TIMED_OUT"})
    @DisplayName("종료 상태의 환불은 실패 상태로 다시 전이할 수 없다")
    void terminalRefundCannotFailAgain(RefundStatus terminalStatus) {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);
        moveTo(refund, terminalStatus);

        assertThatThrownBy(refund::fail)
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "terminalStatus={0}")
    @EnumSource(value = RefundStatus.class, names = {"SUCCEEDED", "FAILED", "TIMED_OUT"})
    @DisplayName("종료 상태의 환불은 타임아웃 상태로 다시 전이할 수 없다")
    void terminalRefundCannotTimeOutAgain(RefundStatus terminalStatus) {
        Refund refund = Refund.requested("refund-1", "order-1", 15_000);
        moveTo(refund, terminalStatus);

        assertThatThrownBy(refund::timeout)
                .isInstanceOf(IllegalStateException.class);
    }

    private void moveTo(Refund refund, RefundStatus status) {
        switch (status) {
            case SUCCEEDED -> refund.succeed();
            case FAILED -> refund.fail();
            case TIMED_OUT -> refund.timeout();
            case REQUESTED -> { }
        }
    }
}
