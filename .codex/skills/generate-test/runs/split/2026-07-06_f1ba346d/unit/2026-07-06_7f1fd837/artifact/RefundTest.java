import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefundTest {

    @Test
    @DisplayName("환불은 REQUESTED 상태로 생성된다")
    void request_createsRequestedRefund() {
        Refund refund = Refund.request(10_000L, RefundType.PARTIAL);

        assertThat(refund.amount()).isEqualTo(10_000L);
        assertThat(refund.type()).isEqualTo(RefundType.PARTIAL);
        assertThat(refund.status()).isEqualTo(RefundStatus.REQUESTED);
    }

    @ParameterizedTest(name = "paymentResult={0}, refundStatus={1}")
    @CsvSource({
            "SUCCESS,SUCCEEDED",
            "REJECTED,FAILED",
            "UNKNOWN,TIMED_OUT"
    })
    @DisplayName("결제 취소 결과에 따라 환불 상태가 REQUESTED에서 최종 상태로 전이된다")
    void complete_transitionsFromRequestedByPaymentCancellationResult(
            PaymentCancellationResult paymentResult,
            RefundStatus expectedRefundStatus
    ) {
        Refund refund = Refund.request(10_000L, RefundType.PARTIAL);

        refund.complete(paymentResult);

        assertThat(refund.status()).isEqualTo(expectedRefundStatus);
    }
}
