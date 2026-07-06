import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefundTest {

    @ParameterizedTest(name = "{0} 결과이면 {1} 상태")
    @CsvSource({
            "SUCCEEDED, SUCCEEDED",
            "FAILED, FAILED",
            "TIMED_OUT, TIMED_OUT"
    })
    @DisplayName("REQUESTED 환불은 결제 취소 결과에 따라 종료 상태로 전이한다")
    void requestedRefundTransitionsByCancellationResult(RefundStatus cancellationResult, RefundStatus expectedStatus) {
        Refund refund = Refund.requested(30_000L, RefundType.FULL);

        refund.completeAs(cancellationResult);

        assertThat(refund.status()).isEqualTo(expectedStatus);
    }
}
