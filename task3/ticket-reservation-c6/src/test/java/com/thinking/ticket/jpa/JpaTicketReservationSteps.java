package com.thinking.ticket.jpa;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 실제 MySQL 구성의 스텝 정의. in-memory 구성과 같은 Feature 문장을 쓰지만, 아웃바운드가 fake가
 * 아니라 Inbound Port(Spring 빈) + 실제 MySQL이다. When은 Inbound Port를 호출하고,
 * 상태 단언은 실제 DB에서 읽는다.
 *
 * <p>상태 준비·확인은 <b>저장 스키마</b>로 직접 한다. 영속 어댑터의 자바 타입을 쓰지 않는 이유는 둘이다.
 * <ul>
 *   <li>포트의 쓰기 계약은 "아직 예약되지 않은 것만 예약"이라는 도메인 동작이라, "이미 예약된 상태"
 *       같은 임의 상태를 심판이 만들 수 없다. 포트만으로는 Given을 세울 수 없다.
 *   <li>심판이 어댑터 구현 타입에 결합하면, 그 어댑터가 아직 없는 상태에서 이 파일이 컴파일되지 않아
 *       멀쩡한 다른 심판까지 못 돌게 된다. 심판은 구성이 확정한 것(스키마)에만 결합한다.
 * </ul>
 * 따라서 테이블·컬럼 이름 {@code tickets(id, price, reserved, suspended, user_id)} ·
 * {@code users(id, name)} 이 이 구성의 계약이다.
 */
public class JpaTicketReservationSteps {

    @Autowired
    private ReserveTicketUseCase reserveTicket;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestChargePort payment;

    private ReservationResult result;
    private Throwable thrown;

    // --- Given ---

    @Given("회원 저장소와 티켓 저장소가 비어 있다")
    public void 저장소가_비어_있다() {
        // 싱글턴 컨텍스트라 시나리오 간 상태가 남으므로 매 시나리오마다 실제 DB와 결제 더블을 초기화한다.
        jdbc.update("delete from tickets");
        jdbc.update("delete from users");
        payment.reset();
        this.result = null;
        this.thrown = null;
    }

    @Given("회원 {long}이 등록되어 있다")
    public void 회원이_등록되어_있다(long userId) {
        jdbc.update("insert into users(id, name) values(?, ?)", userId, "user-" + userId);
    }

    @Given("가격 {int}원짜리 미예약 티켓 {long}이 있다")
    public void 미예약_티켓이_있다(int price, long ticketId) {
        티켓을_적재한다(ticketId, price, false, false, 0);
    }

    @Given("가격 {int}원짜리 이미 예약된 티켓 {long}이 있다")
    public void 이미_예약된_티켓이_있다(int price, long ticketId) {
        // DB에는 예약 완료 상태를 직접 적재한다(소유자 값은 이 시나리오 단언과 무관).
        티켓을_적재한다(ticketId, price, true, false, 0);
    }

    @Given("가격 {int}원짜리 판매 중지된 티켓 {long}이 있다")
    public void 판매_중지된_티켓이_있다(int price, long ticketId) {
        티켓을_적재한다(ticketId, price, false, true, 0);
    }

    @Given("결제는 거절되는 상황이다")
    public void 결제는_거절된다() {
        payment.decline();
    }

    @Given("티켓 {long}은 저장소에 없다")
    public void 티켓이_저장소에_없다(long ticketId) {
        // 일부러 적재하지 않는다 — 조회가 비어 도메인이 TicketNotFoundException을 던지는 경로를 태운다.
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
        assertThat(예약되었나(ticketId)).isTrue();
        assertThat(소유자(ticketId)).isEqualTo(userId);
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
        assertThat(예약되었나(ticketId)).isFalse();
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

    // --- 저장 스키마 접근 (이 구성이 확정한 계약) ---

    private void 티켓을_적재한다(long id, int price, boolean reserved, boolean suspended, long userId) {
        jdbc.update("insert into tickets(id, price, reserved, suspended, user_id) values(?, ?, ?, ?, ?)",
                id, price, reserved, suspended, userId);
    }

    private boolean 예약되었나(long ticketId) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("select reserved from tickets where id = ?", Boolean.class, ticketId));
    }

    private long 소유자(long ticketId) {
        Long userId = jdbc.queryForObject("select user_id from tickets where id = ?", Long.class, ticketId);
        return userId == null ? 0L : userId;
    }
}
