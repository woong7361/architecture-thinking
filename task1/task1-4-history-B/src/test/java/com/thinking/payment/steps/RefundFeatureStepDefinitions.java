package com.thinking.payment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.thinking.payment.domain.Order;
import com.thinking.payment.domain.OrderStatus;
import com.thinking.payment.domain.Refund;
import com.thinking.payment.domain.RefundCalculationRequest;
import com.thinking.payment.domain.RefundException;
import com.thinking.payment.domain.RefundPolicy;
import com.thinking.payment.domain.RefundType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public final class RefundFeatureStepDefinitions {

    private Order order;
    private int totalDays;
    private int elapsedDays;
    private Refund refund;
    private Throwable thrown;
    private boolean externalStorePayment;

    @Given("결제금액 {int}원, 구독 기간 {int}일인 결제 완료 주문이 있다")
    public void paidOrder(int amount, int totalDays) {
        this.totalDays = totalDays;
        this.elapsedDays = 0;
        this.refund = null;
        this.thrown = null;
        this.externalStorePayment = false;
        this.order = new Order(amount);
    }

    @Given("구독 시작 후 {int}일이 지났다")
    public void elapsedDays(int elapsedDays) {
        this.elapsedDays = elapsedDays;
    }

    @Given("이미 {int}원이 환불되어 부분 환불됨 상태인 주문이 있다")
    public void partiallyRefundedOrder(int refundedAmount) {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.refund = null;
        this.thrown = null;
        this.externalStorePayment = false;
        this.order = new Order(30000, refundedAmount, OrderStatus.PARTIALLY_REFUNDED);
    }

    @Given("이미 전액 환불되어 환불됨 상태인 주문이 있다")
    public void refundedOrder() {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.refund = null;
        this.thrown = null;
        this.externalStorePayment = false;
        this.order = new Order(30000, 30000, OrderStatus.REFUNDED);
    }

    @Given("결제 대기중인 주문이 있다")
    public void pendingOrder() {
        this.totalDays = 30;
        this.elapsedDays = 0;
        this.refund = null;
        this.thrown = null;
        this.externalStorePayment = false;
        this.order = new Order(30000, OrderStatus.PENDING);
    }

    @Given("애플 앱스토어로 결제된 주문이 있다")
    public void appStoreOrder() {
        paidOrder(30000, 30);
        this.externalStorePayment = true;
    }

    @Given("구글 플레이로 결제된 주문이 있다")
    public void googlePlayOrder() {
        paidOrder(30000, 30);
        this.externalStorePayment = true;
    }

    @When("일할 계산 정책으로 환불을 요청하면")
    public void requestProrationRefund() {
        execute(() -> {
            order.validateRefundable();
            long amount = RefundPolicy.PRORATION.calculate(new RefundCalculationRequest(
                    order.getAmount(),
                    order.getCancellableAmount(),
                    totalDays,
                    totalDays - elapsedDays,
                    null,
                    elapsedDays
            ));
            refund = Refund.requested(amount, order.getCancellableAmount());
            order.applyRefund(amount);
        });
    }

    @When("수동 정책으로 {int}원 환불을 요청하면")
    public void requestManualRefund(int amount) {
        execute(() -> {
            order.validateRefundable();
            long refundAmount = RefundPolicy.MANUAL.calculate(amount, order.getCancellableAmount());
            refund = Refund.requested(refundAmount, order.getCancellableAmount());
            order.applyRefund(refundAmount);
        });
    }

    @When("수동 정책으로 금액을 지정하지 않고 환불을 요청하면")
    public void requestManualRefundWithoutAmount() {
        execute(() -> {
            order.validateRefundable();
            long refundAmount = RefundPolicy.MANUAL.calculate(new RefundCalculationRequest(
                    order.getAmount(),
                    order.getCancellableAmount(),
                    totalDays,
                    totalDays - elapsedDays,
                    null,
                    elapsedDays
            ));
            refund = Refund.requested(refundAmount, order.getCancellableAmount());
            order.applyRefund(refundAmount);
        });
    }

    @Then("환불금액은 {int}원이다")
    public void refundAmountShouldBe(int expectedAmount) {
        assertThat(thrown).isNull();
        assertThat(refund.getAmount()).isEqualTo(expectedAmount);
    }

    @Then("환불 유형은 부분 환불이다")
    public void refundTypeShouldBePartial() {
        assertThat(thrown).isNull();
        assertThat(refund.getType()).isEqualTo(RefundType.PARTIAL);
    }

    @Then("환불 유형은 전액 환불이다")
    public void refundTypeShouldBeFull() {
        assertThat(thrown).isNull();
        assertThat(refund.getType()).isEqualTo(RefundType.FULL);
    }

    @Then("주문 상태는 부분 환불됨이다")
    public void orderStatusShouldBePartiallyRefunded() {
        assertThat(thrown).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
    }

    @Then("주문 상태는 환불됨이다")
    public void orderStatusShouldBeRefunded() {
        assertThat(thrown).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Then("환불 가능 금액은 {int}원이다")
    public void refundableAmountShouldBe(int expectedAmount) {
        assertThat(thrown).isNull();
        assertThat(order.getCancellableAmount()).isEqualTo(expectedAmount);
    }

    @Then("잘못된 구독 기간 오류가 발생한다")
    public void invalidSubscriptionPeriodErrorShouldBeRaised() {
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Then("환불 가능 금액 초과 오류가 발생한다")
    public void refundAmountExceededErrorShouldBeRaised() {
        assertThat(thrown).isInstanceOf(RefundException.class);
    }

    @Then("잘못된 환불 금액 오류가 발생한다")
    public void invalidRefundAmountErrorShouldBeRaised() {
        assertThat(thrown).isInstanceOfAny(RefundException.class, IllegalArgumentException.class);
    }

    @Then("환불 불가 오류가 발생한다")
    public void notRefundableErrorShouldBeRaised() {
        assertThat(thrown).isInstanceOf(RefundException.class);
    }

    @Then("웹 플랫폼 전용 환불 오류가 발생한다")
    public void webOnlyRefundErrorShouldBeRaised() {
        assertThat(thrown).isInstanceOf(RefundException.class);
        assertThat(externalStorePayment).isTrue();
    }

    private void execute(Runnable action) {
        this.refund = null;
        this.thrown = null;
        try {
            action.run();
        } catch (Throwable e) {
            this.thrown = e;
        }
    }
}
