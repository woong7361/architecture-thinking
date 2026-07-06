import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator();

    @Test
    @DisplayName("MANUAL 지정 금액은 7일 무료와 일할 계산보다 우선한다")
    void manualAmountOverridesFreeCancellationAndProration() {
        RefundCalculationRequest request = RefundCalculationRequest.builder()
                .paidAmount(30_000L)
                .canceledAmount(0L)
                .totalDays(30)
                .remainingDays(30)
                .elapsedDays(0)
                .manualAmount(10_000L)
                .build();

        long refundAmount = calculator.calculate(request);

        assertThat(refundAmount).isEqualTo(10_000L);
    }

    @ParameterizedTest(name = "경과일 {0}일이면 전액 환불")
    @MethodSource("freeCancellationBoundaryCases")
    @DisplayName("MANUAL이 없고 경과일이 7일 이하이면 환불 가능 금액 전액을 환불한다")
    void freeCancellationWithinSevenDays(int elapsedDays, long canceledAmount, long expectedAmount) {
        RefundCalculationRequest request = RefundCalculationRequest.builder()
                .paidAmount(30_000L)
                .canceledAmount(canceledAmount)
                .totalDays(30)
                .remainingDays(15)
                .elapsedDays(elapsedDays)
                .manualAmount(null)
                .build();

        long refundAmount = calculator.calculate(request);

        assertThat(refundAmount).isEqualTo(expectedAmount);
    }

    static Stream<Arguments> freeCancellationBoundaryCases() {
        return Stream.of(
                Arguments.of(0, 0L, 30_000L),
                Arguments.of(7, 0L, 30_000L),
                Arguments.of(7, 5_000L, 25_000L)
        );
    }

    @ParameterizedTest(name = "총 {0}일 중 잔여 {1}일이면 {2}원")
    @MethodSource("prorationCases")
    @DisplayName("경과일이 8일 이상이면 일할 계산을 적용한다")
    void prorationAfterFreeCancellationPeriod(int totalDays, int remainingDays, long paidAmount, long expectedAmount) {
        RefundCalculationRequest request = RefundCalculationRequest.builder()
                .paidAmount(paidAmount)
                .canceledAmount(0L)
                .totalDays(totalDays)
                .remainingDays(remainingDays)
                .elapsedDays(8)
                .manualAmount(null)
                .build();

        long refundAmount = calculator.calculate(request);

        assertThat(refundAmount).isEqualTo(expectedAmount);
    }

    static Stream<Arguments> prorationCases() {
        return Stream.of(
                Arguments.of(30, 30, 30_000L, 30_000L),
                Arguments.of(30, 15, 30_000L, 15_000L),
                Arguments.of(30, 1, 30_000L, 1_000L),
                Arguments.of(30, 0, 30_000L, 0L),
                Arguments.of(30, 7, 10_000L, 2_331L)
        );
    }

    @ParameterizedTest(name = "totalDays={0}")
    @MethodSource("invalidTotalDays")
    @DisplayName("총 구독 일수가 0 이하이면 일할 계산을 거절한다")
    void rejectInvalidTotalDays(int totalDays) {
        RefundCalculationRequest request = RefundCalculationRequest.builder()
                .paidAmount(30_000L)
                .canceledAmount(0L)
                .totalDays(totalDays)
                .remainingDays(0)
                .elapsedDays(8)
                .manualAmount(null)
                .build();

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> invalidTotalDays() {
        return Stream.of(Arguments.of(0), Arguments.of(-1));
    }

    @ParameterizedTest(name = "manualAmount={0}")
    @MethodSource("invalidManualAmounts")
    @DisplayName("수동 지정 금액은 0 이하일 수 없다")
    void rejectNonPositiveManualAmount(long manualAmount) {
        RefundCalculationRequest request = RefundCalculationRequest.builder()
                .paidAmount(30_000L)
                .canceledAmount(0L)
                .totalDays(30)
                .remainingDays(30)
                .elapsedDays(0)
                .manualAmount(manualAmount)
                .build();

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> invalidManualAmounts() {
        return Stream.of(Arguments.of(0L), Arguments.of(-1L));
    }

    @Test
    @DisplayName("수동 지정 금액이 환불 가능 금액을 초과하면 거절한다")
    void rejectManualAmountGreaterThanCancellableAmount() {
        RefundCalculationRequest request = RefundCalculationRequest.builder()
                .paidAmount(30_000L)
                .canceledAmount(10_000L)
                .totalDays(30)
                .remainingDays(30)
                .elapsedDays(0)
                .manualAmount(20_001L)
                .build();

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
