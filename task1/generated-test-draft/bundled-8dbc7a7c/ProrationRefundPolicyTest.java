import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProrationRefundPolicyTest {

    private RefundPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new RefundPolicy();
    }

    @ParameterizedTest(name = "{0}원 / {1}일 * {2}일 = {3}원")
    @CsvSource({
            "30000, 30, 30, 30000",
            "30000, 30, 1, 1000",
            "30000, 30, 0, 0",
            "10000, 30, 7, 2331"
    })
    @DisplayName("일할 계산은 정수 단가에 잔여 일수를 곱하고 소수점 이하는 절사한다")
    void proratesByIntegerDailyPrice(long paidAmount, int totalDays, int remainingDays, long expectedAmount) {
        long amount = policy.prorate(paidAmount, totalDays, remainingDays);

        assertThat(amount).isEqualTo(expectedAmount);
    }

    @ParameterizedTest(name = "totalDays={0}")
    @CsvSource({"0", "-1"})
    @DisplayName("총 구독 일수가 0 이하이면 일할 계산을 거절한다")
    void rejectsNonPositiveTotalDays(int totalDays) {
        assertThatThrownBy(() -> policy.prorate(30_000, totalDays, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
