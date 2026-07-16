package com.thinking.ticket.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.thinking.ticket.PaymentFailedException;
import com.thinking.ticket.TicketAlreadyReservedException;
import com.thinking.ticket.TicketService;
import com.thinking.ticket.UserNotFoundException;
import com.thinking.ticket.provided.PaymentApi;
import com.thinking.ticket.provided.TicketRecord;
import com.thinking.ticket.provided.TicketStore;
import com.thinking.ticket.provided.UserRecord;
import com.thinking.ticket.provided.UserStore;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * 예매 유스케이스 경계의 인수테스트 step 정의.
 *
 * <p>이 net은 기존 인프라(provided 패키지)와 계약된 진입점 하나에만 의존한다.
 * 구현이 클래스를 몇 개로 쪼갰는지, 인터페이스를 뒀는지, 로직을 어디에 뒀는지는 보지 않는다.
 */
public final class TicketReservationSteps {

    private final UserStore userStore = new UserStore();
    private final TicketStore ticketStore = new TicketStore();
    private PaymentApi paymentApi = PaymentApi.succeeding();

    private Boolean result;
    private Throwable thrown;

    // --- Given ---

    @Given("회원 저장소와 티켓 저장소가 비어 있다")
    public void 저장소가_비어_있다() {
        // 시뮬레이터는 필드로 새로 생성되어 이미 비어 있다. 시나리오마다 새 step 인스턴스라 상태가 격리된다.
        this.result = null;
        this.thrown = null;
    }

    @Given("회원 {long}이 등록되어 있다")
    public void 회원이_등록되어_있다(long userId) {
        userStore.seed(new UserRecord(userId, "user-" + userId));
    }

    @Given("가격 {int}원짜리 미예약 티켓 {long}이 있다")
    public void 미예약_티켓이_있다(int price, long ticketId) {
        ticketStore.seed(new TicketRecord(ticketId, price));
    }

    @Given("가격 {int}원짜리 이미 예약된 티켓 {long}이 있다")
    public void 이미_예약된_티켓이_있다(int price, long ticketId) {
        // 저장소에 이미 그런 행이 들어 있는 상황을 인프라 쪽에서 직접 만든다.
        // 구현 코드를 거치지 않으므로 어떤 구조로 짜였든 이 셋업은 그대로 성립한다.
        TicketRecord reserved = new TicketRecord(ticketId, price);
        reserved.setReserved(true);
        reserved.setUserId(-1L);
        ticketStore.seed(reserved);
    }

    @Given("결제는 거절되는 상황이다")
    public void 결제는_거절된다() {
        this.paymentApi = PaymentApi.failing();
    }

    // --- When ---

    @When("회원 {long}이 카드정보 {string}으로 티켓 {long}을 예매하면")
    public void 예매하면(long userId, String paymentInfo, long ticketId) {
        TicketService service = new TicketService(ticketStore, userStore, paymentApi);
        try {
            this.result = service.reserveTicket(userId, ticketId, paymentInfo);
        } catch (Throwable t) {
            this.thrown = t;
        }
    }

    // --- Then ---

    @Then("예매는 성공한다")
    public void 예매는_성공한다() {
        assertThat(thrown).isNull();
        assertThat(result).isTrue();
    }

    @Then("티켓 {long}은 회원 {long}에게 예약된다")
    public void 티켓이_회원에게_예약된다(long ticketId, long userId) {
        TicketRecord stored = ticketStore.findById(ticketId);
        assertThat(stored.isReserved()).isTrue();
        assertThat(stored.getUserId()).isEqualTo(userId);
    }

    @Then("{int}원이 청구된다")
    public void 금액이_청구된다(int amount) {
        assertThat(paymentApi.wasCharged()).isTrue();
        assertThat(paymentApi.lastAmount()).isEqualTo(amount);
    }

    @Then("예매는 회원 없음으로 거부된다")
    public void 회원_없음으로_거부된다() {
        assertThat(thrown).isInstanceOf(UserNotFoundException.class);
    }

    @Then("결제는 청구되지 않는다")
    public void 결제는_청구되지_않는다() {
        assertThat(paymentApi.wasCharged()).isFalse();
    }

    @Then("예매는 이미 예약됨으로 거부된다")
    public void 이미_예약됨으로_거부된다() {
        assertThat(thrown).isInstanceOf(TicketAlreadyReservedException.class);
    }

    @Then("예매는 결제 실패로 거부된다")
    public void 결제_실패로_거부된다() {
        assertThat(thrown).isInstanceOf(PaymentFailedException.class);
    }

    @Then("티켓 {long}은 예약되지 않는다")
    public void 티켓은_예약되지_않는다(long ticketId) {
        TicketRecord stored = ticketStore.findById(ticketId);
        assertThat(stored.isReserved()).isFalse();
    }

    @Then("{int}원 청구가 시도된다")
    public void 청구가_시도된다(int amount) {
        assertThat(paymentApi.wasCharged()).isTrue();
        assertThat(paymentApi.lastAmount()).isEqualTo(amount);
    }
}
