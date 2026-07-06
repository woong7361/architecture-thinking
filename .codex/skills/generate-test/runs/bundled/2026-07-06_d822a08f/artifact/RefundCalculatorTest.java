import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class RefundCalculatorTest {

    private RefundCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RefundCalculator();
    }

    @Test
    @DisplayName("MANUAL 지정은 7일 이하 무료 환불보다 우선한다")
    void manualAmountOverridesFreeCancellationPeriod() {
        RefundCalculation result = calculator.calculate(command(
                30_000L,
                0L,
                30,
                30,
                "2026-07-06T23:59:59Z",
                "2026-07-06T00:00:00Z",
                OptionalLong.of(10_000L)));

        assertThat(result.amount()).isEqualTo(10_000L);
        assertThat(result.type()).isEqualTo(RefundType.PARTIAL);
    }

    @Test
    @DisplayName("MANUAL 지정은 8일 이후 PRORATION보다 우선한다")
    void manualAmountOverridesProrationAfterFreeCancellationPeriod() {
        RefundCalculation result = calculator.calculate(command(
                30_000L,
                10_000L,
                30,
                15,
                "2026-06-28T10:15:30Z",
                "2026-07-06T01:02:03Z",
                OptionalLong.of(20_000L)));

        assertThat(result.amount()).isEqualTo(20_000L);
        assertThat(result.type()).isEqualTo(RefundType.FULL);
    }

    @ParameterizedTest(name = "manualAmount={2}원은 거절된다")
    @CsvSource({
            "30000, 0, 0",
            "30000, 0, -1",
            "30000, 10000, 20001"
    })
    @DisplayName("MANUAL 지정 금액은 0원 초과이고 환불 가능 금액 이하여야 한다")
    void manualAmountMustBePositiveAndNotExceedCancellableAmount(long amount, long canceledAmount, long manualAmount) {
        assertThatThrownBy(() -> calculator.calculate(command(
                amount,
                canceledAmount,
                30,
                15,
                "2026-06-28T00:00:00Z",
                "2026-07-06T00:00:00Z",
                OptionalLong.of(manualAmount))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "paidAt={0}이면 전액 환불")
    @CsvSource({
            "2026-07-06T23:59:59Z",
            "2026-06-29T00:00:00Z"
    })
    @DisplayName("MANUAL이 없고 UTC 날짜 기준 경과일이 7일 이하이면 전액 환불한다")
    void freeCancellationUsesUtcDateBoundary(String paidAt) {
        RefundCalculation result = calculator.calculate(command(
                30_000L,
                0L,
                30,
                15,
                paidAt,
                "2026-07-06T00:00:00Z",
                OptionalLong.empty()));

        assertThat(result.amount()).isEqualTo(30_000L);
        assertThat(result.type()).isEqualTo(RefundType.FULL);
    }

    @Test
    @DisplayName("UTC 날짜 기준 경과일이 8일이면 PRORATION을 적용한다")
    void appliesProrationWhenElapsedDaysIsEightByUtcDate() {
        RefundCalculation result = calculator.calculate(command(
                30_000L,
                0L,
                30,
                15,
                "2026-06-28T23:59:59Z",
                "2026-07-06T00:00:00Z",
                OptionalLong.empty()));

        assertThat(result.amount()).isEqualTo(15_000L);
        assertThat(result.type()).isEqualTo(RefundType.PARTIAL);
    }

    @ParameterizedTest(name = "{0}원 / {1}일 * {2}일 = {3}원")
    @MethodSource("prorationCases")
    @DisplayName("PRORATION은 정수 나눗셈 단가에 잔여 일수를 곱한다")
    void proratesByIntegerDailyPrice(long amount, int totalDays, int remainingDays, long expectedAmount) {
        RefundCalculation result = calculator.calculateProration(amount, totalDays, remainingDays);

        assertThat(result.amount()).isEqualTo(expectedAmount);
    }

    static Stream<Arguments> prorationCases() {
        return Stream.of(
                Arguments.of(30_000L, 30, 30, 30_000L),
                Arguments.of(30_000L, 30, 15, 15_000L),
                Arguments.of(30_000L, 30, 1, 1_000L),
                Arguments.of(30_000L, 30, 0, 0L),
                Arguments.of(10_000L, 30, 7, 2_331L)
        );
    }

    @ParameterizedTest(name = "totalDays={0}이면 예외")
    @CsvSource({"0", "-1"})
    @DisplayName("총 구독 일수가 0 이하이면 PRORATION을 계산할 수 없다")
    void totalDaysMustBePositive(int totalDays) {
        assertThatThrownBy(() -> calculator.calculateProration(30_000L, totalDays, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RefundCalculationCommand command(
            long amount,
            long canceledAmount,
            int totalDays,
            int remainingDays,
            String paidAt,
            String requestedAt,
            OptionalLong manualAmount
    ) {
        return new RefundCalculationCommand(
                amount,
                canceledAmount,
                totalDays,
                remainingDays,
                Instant.parse(paidAt),
                Instant.parse(requestedAt),
                manualAmount
        );
    }
}
