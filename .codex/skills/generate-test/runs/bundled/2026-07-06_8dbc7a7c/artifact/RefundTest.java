import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RefundTest {

    @Test
    @DisplayName("환불 요청이 생성되면 상태는 REQUESTED이다")
    void createRequestedRefund() {
        Refund refund = Refund.requested("order-1", 15_000L, RefundType.PARTIAL);

        assertThat(refund.status()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.amount()).isEqualTo(15_000L);
        assertThat(refund.type()).isEqualTo(RefundType.PARTIAL);
    }

    @ParameterizedTest(name = "{0} 처리 후 {1}")
    @MethodSource("refundTransitionCases")
    @DisplayName("REQUESTED 환불은 결제 취소 결과에 따라 최종 상태로 전이한다")
    void transitionFromRequested(CancellationOutcome outcome, RefundStatus expectedStatus) {
        Refund refund = Refund.requested("order-1", 15_000L, RefundType.PARTIAL);

        refund.markBy(outcome);

        assertThat(refund.status()).isEqualTo(expectedStatus);
    }

    static Stream<Arguments> refundTransitionCases() {
        return Stream.of(
                Arguments.of(CancellationOutcome.SUCCEEDED, RefundStatus.SUCCEEDED),
                Arguments.of(CancellationOutcome.REJECTED, RefundStatus.FAILED),
                Arguments.of(CancellationOutcome.UNKNOWN, RefundStatus.TIMED_OUT)
        );
    }
}
