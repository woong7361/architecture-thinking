import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefundTypeTest {

    @ParameterizedTest(name = "refundAmount={0}, cancellableAmount={1}이면 {2}")
    @CsvSource({
            "30000, 30000, FULL",
            "29999, 30000, PARTIAL",
            "20000, 20000, FULL",
            "19999, 20000, PARTIAL"
    })
    @DisplayName("환불 금액이 환불 가능 금액과 같으면 FULL, 작으면 PARTIAL이다")
    void determinesRefundTypeByCancellableAmount(long refundAmount, long cancellableAmount, RefundType expectedType) {
        RefundType type = RefundType.from(refundAmount, cancellableAmount);

        assertThat(type).isEqualTo(expectedType);
    }
}
