package com.thinking.payment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.thinking.payment.Order;
import com.thinking.payment.OrderStatus;
import com.thinking.payment.PaymentPlatform;
import com.thinking.payment.RefundProcessor;
import com.thinking.payment.RefundReceipt;
import com.thinking.payment.RefundRejectedException;
import com.thinking.payment.RefundRejectionReason;
import com.thinking.payment.RefundRequest;
import com.thinking.payment.RefundType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public final class RefundStepDefinitions {

    private static final String PAYMENT_UUID = "acceptance-payment-1";

    private final RefundProcessor refundProcessor = new RefundProcessor();

    private Order order;
    private int totalDays;
    private int elapsedDays;
    private RefundReceipt receipt;
    private Throwable thrown;

    @Given("결제금액 {int}원, 구독 기간 {int}일인 결제 완료 주문이 있다")
    public void paidOrder(int amount, int totalDays) {
        this.totalDays = totalDays;
        this.elapsedDays = 0;
        this.receipt = null;
        this.thrown = null;
        this.order = Order.paid(PAYMENT_UUID, amount, totalDays);
    }

    @Given("구독 시작 후 {int}일이 지났다")
    public void elapsedDays(int elapsedDays) {
        this.elapsedDays = elapsedDays;
    }

    @Given("이미 {int}원이 환불되어 부분 환불됨 상태인 주문이 있다")
    public void partiallyRefundedOrder(int refundedAmount) {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.receipt = null;
        this.thrown = null;
        this.order = Order.partiallyRefunded(PAYMENT_UUID, 30000, totalDays, refundedAmount);
    }

    @Given("이미 전액 환불되어 환불됨 상태인 주문이 있다")
    public void refundedOrder() {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.receipt = null;
        this.thrown = null;
        this.order = Order.refunded(PAYMENT_UUID, 30000, totalDays);
    }

    @Given("결제 대기중인 주문이 있다")
    public void pendingOrder() {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.receipt = null;
        this.thrown = null;
        this.order = Order.pending(PAYMENT_UUID, 30000, totalDays);
    }

    @Given("애플 앱스토어로 결제된 주문이 있다")
    public void appStoreOrder() {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.receipt = null;
        this.thrown = null;
        this.order = Order.paidOn(PAYMENT_UUID, 30000, totalDays, PaymentPlatform.APPLE_APP_STORE);
    }

    @Given("구글 플레이로 결제된 주문이 있다")
    public void googlePlayOrder() {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.receipt = null;
        this.thrown = null;
        this.order = Order.paidOn(PAYMENT_UUID, 30000, totalDays, PaymentPlatform.GOOGLE_PLAY);
    }

    @When("일할 계산 정책으로 환불을 요청하면")
    public void requestProrationRefund() {
        execute(() -> receipt = refundProcessor.refund(order, RefundRequest.proration(elapsedDays)));
    }

    @When("수동 정책으로 {int}원 환불을 요청하면")
    public void requestManualRefund(int amount) {
        execute(() -> receipt = refundProcessor.refund(order, RefundRequest.manual(amount)));
    }

    @When("수동 정책으로 금액을 지정하지 않고 환불을 요청하면")
    public void requestManualRefundWithoutAmount() {
        execute(() -> receipt = refundProcessor.refund(order, RefundRequest.manualWithoutAmount()));
    }

    @Then("환불금액은 {int}원이다")
    public void refundAmountShouldBe(int expectedAmount) {
        assertThat(thrown).isNull();
        assertThat(receipt.amount()).isEqualTo(expectedAmount);
    }

    @Then("환불 유형은 부분 환불이다")
    public void refundTypeShouldBePartial() {
        assertThat(thrown).isNull();
        assertThat(receipt.type()).isEqualTo(RefundType.PARTIAL);
    }

    @Then("환불 유형은 전액 환불이다")
    public void refundTypeShouldBeFull() {
        assertThat(thrown).isNull();
        assertThat(receipt.type()).isEqualTo(RefundType.FULL);
    }

    @Then("주문 상태는 부분 환불됨이다")
    public void orderStatusShouldBePartiallyRefunded() {
        assertThat(thrown).isNull();
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
    }

    @Then("주문 상태는 환불됨이다")
    public void orderStatusShouldBeRefunded() {
        assertThat(thrown).isNull();
        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Then("환불 가능 금액은 {int}원이다")
    public void refundableAmountShouldBe(int expectedAmount) {
        assertThat(thrown).isNull();
        assertThat(order.refundableAmount()).isEqualTo(expectedAmount);
    }

    @Then("잘못된 구독 기간 오류가 발생한다")
    public void invalidSubscriptionPeriodErrorShouldBeRaised() {
        assertRejected(RefundRejectionReason.INVALID_SUBSCRIPTION_PERIOD);
    }

    @Then("환불 가능 금액 초과 오류가 발생한다")
    public void refundAmountExceededErrorShouldBeRaised() {
        assertRejected(RefundRejectionReason.REFUND_AMOUNT_EXCEEDED);
    }

    @Then("잘못된 환불 금액 오류가 발생한다")
    public void invalidRefundAmountErrorShouldBeRaised() {
        assertRejected(RefundRejectionReason.INVALID_REFUND_AMOUNT);
    }

    @Then("환불 불가 오류가 발생한다")
    public void notRefundableErrorShouldBeRaised() {
        assertRejected(RefundRejectionReason.NOT_REFUNDABLE);
    }

    @Then("웹 플랫폼 전용 환불 오류가 발생한다")
    public void webOnlyRefundErrorShouldBeRaised() {
        assertRejected(RefundRejectionReason.WEB_ONLY);
    }

    private void execute(Runnable action) {
        this.receipt = null;
        this.thrown = null;
        try {
            action.run();
        } catch (Throwable e) {
            this.thrown = e;
        }
    }

    private void assertRejected(RefundRejectionReason expectedReason) {
        assertThat(thrown).isInstanceOf(RefundRejectedException.class);
        assertThat(((RefundRejectedException) thrown).reason()).isEqualTo(expectedReason);
    }
}
