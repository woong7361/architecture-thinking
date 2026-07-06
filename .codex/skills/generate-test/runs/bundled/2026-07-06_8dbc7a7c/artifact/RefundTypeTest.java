import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RefundTypeTest {

    private final RefundTypeResolver resolver = new RefundTypeResolver();

    @ParameterizedTest(name = "환불금액 {0}원 / 환불가능금액 {1}원 -> {2}")
    @MethodSource("refundTypeCases")
    @DisplayName("확정된 환불 금액과 환불 가능 금액으로 환불 유형을 결정한다")
    void resolveRefundType(long refundAmount, long cancellableAmount, RefundType expectedType) {
        RefundType refundType = resolver.resolve(refundAmount, cancellableAmount);

        assertThat(refundType).isEqualTo(expectedType);
    }

    static Stream<Arguments> refundTypeCases() {
        return Stream.of(
                Arguments.of(30_000L, 30_000L, RefundType.FULL),
                Arguments.of(29_999L, 30_000L, RefundType.PARTIAL),
                Arguments.of(0L, 30_000L, RefundType.PARTIAL)
        );
    }
}
