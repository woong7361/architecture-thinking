import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RefundPeriodCalculatorTest {

    private final RefundPeriodCalculator calculator = new RefundPeriodCalculator();

    @ParameterizedTest(name = "{0}부터 {1}까지는 {2}일 경과")
    @MethodSource("utcDateCases")
    @DisplayName("경과일은 결제 시각과 환불 요청 시각의 UTC 날짜 기준으로 계산한다")
    void calculateElapsedDaysByUtcDateOnly(String paidAt, String requestedAt, int expectedElapsedDays) {
        int elapsedDays = calculator.elapsedDays(Instant.parse(paidAt), Instant.parse(requestedAt));

        assertThat(elapsedDays).isEqualTo(expectedElapsedDays);
    }

    static Stream<Arguments> utcDateCases() {
        return Stream.of(
                Arguments.of("2026-07-01T23:30:00Z", "2026-07-08T00:10:00Z", 7),
                Arguments.of("2026-07-01T00:00:00Z", "2026-07-09T23:59:59Z", 8)
        );
    }
}
