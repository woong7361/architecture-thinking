package com.thinking.ticket.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.thinking.ticket.adapter.out.persistence.TicketJpaEntity;
import com.thinking.ticket.adapter.out.persistence.TicketJpaRepository;
import com.thinking.ticket.adapter.out.persistence.UserJpaEntity;
import com.thinking.ticket.adapter.out.persistence.UserJpaRepository;
import com.thinking.ticket.core.domain.PaymentFailedException;
import com.thinking.ticket.core.domain.TicketAlreadyReservedException;
import com.thinking.ticket.core.domain.TicketNotFoundException;
import com.thinking.ticket.core.domain.TicketSuspendedException;
import com.thinking.ticket.core.domain.UserNotFoundException;
import com.thinking.ticket.core.port.in.ReservationResult;
import com.thinking.ticket.core.port.in.ReserveTicketCommand;
import com.thinking.ticket.core.port.in.ReserveTicketUseCase;
import com.thinking.ticket.jpa.TestPaymentConfig.TestChargePort;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 조합 B(JPA + 실제 MySQL)의 스텝 정의. 조합 A와 같은 Feature 문장을 쓰지만,
 * 아웃바운드가 in-memory fake가 아니라 Inbound Port(Spring 빈) + JPA 저장소 + 실제 MySQL이다.
 * When은 Inbound Port(ReserveTicketUseCase)를 호출하고, 상태 단언은 JPA 저장소에서 읽는다.
 * 단언은 조합 A와 동일한 관찰 가능 동작을 검증한다(어댑터 교체·Core 무수정 증명).
 */
public class JpaTicketReservationSteps {

    @Autowired
    private ReserveTicketUseCase reserveTicket;

    @Autowired
    private TicketJpaRepository ticketRepo;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private TestChargePort payment;

    private ReservationResult result;
    private Throwable thrown;

    // --- Given ---

    @Given("회원 저장소와 티켓 저장소가 비어 있다")
    public void 저장소가_비어_있다() {
        // 싱글턴 컨텍스트라 시나리오 간 상태가 남으므로 매 시나리오마다 실제 DB와 결제 더블을 초기화한다.
        ticketRepo.deleteAll();
        userRepo.deleteAll();
        payment.reset();
        this.result = null;
        this.thrown = null;
    }

    @Given("회원 {long}이 등록되어 있다")
    public void 회원이_등록되어_있다(long userId) {
        userRepo.save(new UserJpaEntity(userId, "user-" + userId));
    }

    @Given("가격 {int}원짜리 미예약 티켓 {long}이 있다")
    public void 미예약_티켓이_있다(int price, long ticketId) {
        ticketRepo.save(new TicketJpaEntity(ticketId, price, false, false, 0));
    }

    @Given("가격 {int}원짜리 이미 예약된 티켓 {long}이 있다")
    public void 이미_예약된_티켓이_있다(int price, long ticketId) {
        // DB에는 예약 완료 상태를 직접 적재한다(소유자 값은 이 시나리오 단언과 무관).
        ticketRepo.save(new TicketJpaEntity(ticketId, price, true, false, 0));
    }

    @Given("가격 {int}원짜리 판매 중지된 티켓 {long}이 있다")
    public void 판매_중지된_티켓이_있다(int price, long ticketId) {
        ticketRepo.save(new TicketJpaEntity(ticketId, price, false, true, 0));
    }

    @Given("결제는 거절되는 상황이다")
    public void 결제는_거절된다() {
        payment.decline();
    }

    @Given("티켓 {long}은 저장소에 없다")
    public void 티켓이_저장소에_없다(long ticketId) {
        // 일부러 적재하지 않는다 — findById가 비어 도메인이 TicketNotFoundException을 던지는 경로를 태운다.
    }

    // --- When ---

    @When("회원 {long}이 카드정보 {string}으로 티켓 {long}을 예매하면")
    public void 예매하면(long userId, String paymentInfo, long ticketId) {
        try {
            this.result = reserveTicket.reserve(new ReserveTicketCommand(userId, ticketId, paymentInfo));
        } catch (Throwable t) {
            this.thrown = t;
        }
    }

    // --- Then ---

    @Then("예매는 성공한다")
    public void 예매는_성공한다() {
        assertThat(thrown).isNull();
        assertThat(result).isNotNull();
        assertThat(result.reserved()).isTrue();
    }

    @Then("티켓 {long}은 회원 {long}에게 예약된다")
    public void 티켓이_회원에게_예약된다(long ticketId, long userId) {
        TicketJpaEntity stored = ticketRepo.findById(ticketId).orElseThrow();
        assertThat(stored.isReserved()).isTrue();
        assertThat(stored.getUserId()).isEqualTo(userId);
    }

    @Then("{int}원이 청구된다")
    public void 금액이_청구된다(int amount) {
        assertThat(payment.wasCharged()).isTrue();
        assertThat(payment.lastAmount()).isEqualTo(amount);
    }

    @Then("예매는 회원 없음으로 거부된다")
    public void 회원_없음으로_거부된다() {
        assertThat(thrown).isInstanceOf(UserNotFoundException.class);
    }

    @Then("결제는 청구되지 않는다")
    public void 결제는_청구되지_않는다() {
        assertThat(payment.wasCharged()).isFalse();
    }

    @Then("예매는 이미 예약됨으로 거부된다")
    public void 이미_예약됨으로_거부된다() {
        assertThat(thrown).isInstanceOf(TicketAlreadyReservedException.class);
    }

    @Then("예매는 판매 중지로 거부된다")
    public void 판매_중지로_거부된다() {
        assertThat(thrown).isInstanceOf(TicketSuspendedException.class);
    }

    @Then("예매는 결제 실패로 거부된다")
    public void 결제_실패로_거부된다() {
        assertThat(thrown).isInstanceOf(PaymentFailedException.class);
    }

    @Then("티켓 {long}은 예약되지 않는다")
    public void 티켓은_예약되지_않는다(long ticketId) {
        TicketJpaEntity stored = ticketRepo.findById(ticketId).orElseThrow();
        assertThat(stored.isReserved()).isFalse();
    }

    @Then("{int}원 청구가 시도된다")
    public void 청구가_시도된다(int amount) {
        assertThat(payment.wasCharged()).isTrue();
        assertThat(payment.lastAmount()).isEqualTo(amount);
    }

    @Then("예매는 티켓 없음으로 거부된다")
    public void 티켓_없음으로_거부된다() {
        assertThat(thrown).isInstanceOf(TicketNotFoundException.class);
    }
}
