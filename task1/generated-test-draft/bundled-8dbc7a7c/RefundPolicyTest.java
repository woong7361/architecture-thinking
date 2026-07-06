import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RefundPolicyTest {

    private RefundPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new RefundPolicy();
    }

    @Test
    @DisplayName("MANUAL 지정은 7일 이하 무료와 일할 계산보다 우선한다")
    void manualAmountTakesPrecedenceOverFreeWithdrawalAndProration() {
        RefundCalculationResult result = policy.calculate(baseRequest()
                .paidAt(Instant.parse("2026-07-01T00:00:00Z"))
                .requestedAt(Instant.parse("2026-07-04T00:00:00Z"))
                .remainingDays(27)
                .manualAmount(5_000)
                .build());

        assertThat(result.amount()).isEqualTo(5_000);
        assertThat(result.type()).isEqualTo(RefundType.PARTIAL);
    }

    @Test
    @DisplayName("부분 환불 이후 수동 지정 금액이 환불 가능 금액과 같으면 FULL로 결정한다")
    void manualAmountEqualToCancellableAmountIsFullAfterPartialRefund() {
        RefundCalculationResult result = policy.calculate(baseRequest()
                .alreadyRefundedAmount(10_000)
                .manualAmount(20_000)
                .build());

        assertThat(result.amount()).isEqualTo(20_000);
        assertThat(result.type()).isEqualTo(RefundType.FULL);
    }

    @ParameterizedTest(name = "manualAmount={0}, cancellableAmount={1}")
    @CsvSource({
            "0, 30000",
            "-1, 30000",
            "20001, 20000"
    })
    @DisplayName("수동 지정 금액은 0 이하이거나 환불 가능 금액을 초과할 수 없다")
    void rejectsInvalidManualAmount(long manualAmount, long cancellableAmount) {
        long alreadyRefundedAmount = 30_000 - cancellableAmount;

        assertThatThrownBy(() -> policy.calculate(baseRequest()
                .alreadyRefundedAmount(alreadyRefundedAmount)
                .manualAmount(manualAmount)
                .build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "경과일 {2}일이면 {6}로 {4}원 환불")
    @CsvSource({
            "2026-07-01T00:30:00Z, 2026-07-01T23:30:00Z, 0, 30, 30000, 0, FULL",
            "2026-07-01T23:30:00Z, 2026-07-08T00:30:00Z, 7, 23, 30000, 0, FULL",
            "2026-07-01T23:30:00Z, 2026-07-09T00:00:00Z, 8, 22, 22000, 0, PARTIAL"
    })
    @DisplayName("UTC 날짜 기준 경과일이 7일 이하면 전액, 8일부터 일할 계산을 적용한다")
    void freeWithdrawalBoundaryUsesUtcDate(String paidAt, String requestedAt, int elapsedDays, int remainingDays, long expectedAmount, long alreadyRefundedAmount, RefundType expectedType) {
        RefundCalculationResult result = policy.calculate(baseRequest()
                .alreadyRefundedAmount(alreadyRefundedAmount)
                .paidAt(Instant.parse(paidAt))
                .requestedAt(Instant.parse(requestedAt))
                .remainingDays(remainingDays)
                .build());

        assertThat(result.elapsedDays()).isEqualTo(elapsedDays);
        assertThat(result.amount()).isEqualTo(expectedAmount);
        assertThat(result.type()).isEqualTo(expectedType);
    }

    @Test
    @DisplayName("부분 환불 이후 7일 이하 무료 환불은 환불 가능 금액을 기준으로 FULL이 된다")
    void freeWithdrawalAfterPartialRefundUsesCancellableAmount() {
        RefundCalculationResult result = policy.calculate(baseRequest()
                .alreadyRefundedAmount(10_000)
                .paidAt(Instant.parse("2026-07-01T00:00:00Z"))
                .requestedAt(Instant.parse("2026-07-08T00:00:00Z"))
                .remainingDays(23)
                .build());

        assertThat(result.elapsedDays()).isEqualTo(7);
        assertThat(result.amount()).isEqualTo(20_000);
        assertThat(result.type()).isEqualTo(RefundType.FULL);
    }

    @Test
    @DisplayName("일할 계산 결과가 환불 가능 금액보다 작으면 PARTIAL로 결정한다")
    void prorationAmountLessThanCancellableAmountIsPartial() {
        RefundCalculationResult result = policy.calculate(baseRequest()
                .paidAt(Instant.parse("2026-07-01T00:00:00Z"))
                .requestedAt(Instant.parse("2026-07-09T00:00:00Z"))
                .remainingDays(22)
                .build());

        assertThat(result.amount()).isEqualTo(22_000);
        assertThat(result.type()).isEqualTo(RefundType.PARTIAL);
    }

    @Test
    @DisplayName("만료 후 일할 계산 결과는 0원으로 산출한다")
    void expiredSubscriptionProrationAmountIsZero() {
        RefundCalculationResult result = policy.calculate(baseRequest()
                .paidAt(Instant.parse("2026-07-01T00:00:00Z"))
                .requestedAt(Instant.parse("2026-07-31T00:00:00Z"))
                .remainingDays(0)
                .build());

        assertThat(result.elapsedDays()).isEqualTo(30);
        assertThat(result.amount()).isZero();
    }

    private RefundCalculationRequest.Builder baseRequest() {
        return RefundCalculationRequest.builder()
                .paidAmount(30_000)
                .alreadyRefundedAmount(0)
                .totalDays(30)
                .remainingDays(15)
                .paidAt(Instant.parse("2026-07-01T00:00:00Z"))
                .requestedAt(Instant.parse("2026-07-10T00:00:00Z"));
    }
}
