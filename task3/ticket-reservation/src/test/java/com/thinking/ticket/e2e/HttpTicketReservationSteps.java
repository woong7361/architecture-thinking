package com.thinking.ticket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.thinking.ticket.jpa.TestPaymentConfig.TestChargePort;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 관통 구성의 스텝 정의. 같은 Feature 문장을 쓰지만 When이 Inbound Port를 직접 부르지 않고
 * <b>실제 HTTP 요청</b>을 보낸다 — 인바운드 진입 슬롯이 대역에서 실물로 바뀐 유일한 차이다.
 *
 * <p>거부 사유는 도메인 예외 타입이 아니라 <b>HTTP 상태 코드</b>로 판정한다. 이 구성에서 심판은
 * 프로토콜 바깥에 있으므로 예외 타입을 볼 수 없고, 봐서도 안 된다. 상태 코드 대응은 이 구성의 계약이다.
 * <ul>
 *   <li>없는 회원 · 없는 티켓 → 404
 *   <li>이미 예약됨 · 판매 중지 → 409
 *   <li>결제 거절 → 402
 * </ul>
 * 두 거부 사유가 같은 상태를 공유하지만, 시나리오는 Given과 결제 청구 여부 단언으로 구분된다.
 *
 * <p>거부 응답은 모두 RFC 7807 {@code application/problem+json} 이어야 한다 — 이것도 이 구성의 계약이다.
 *
 * <p>상태 준비·확인은 저장 스키마로 직접 한다. 이유는 실제 저장소 구성의 스텝과 같다 —
 * 심판은 파이프라인이 생성할 타입에 결합하지 않는다.
 */
public class HttpTicketReservationSteps {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestChargePort payment;

    private ResponseEntity<String> response;

    // --- Given ---

    @Given("회원 저장소와 티켓 저장소가 비어 있다")
    public void 저장소가_비어_있다() {
        jdbc.update("delete from tickets");
        jdbc.update("delete from users");
        payment.reset();
        this.response = null;
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
        // 일부러 적재하지 않는다.
    }

    // --- When ---

    @When("회원 {long}이 카드정보 {string}으로 티켓 {long}을 예매하면")
    public void 예매하면(long userId, String paymentInfo, long ticketId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("ticketId", ticketId);
        body.put("paymentInfo", paymentInfo);
        this.response = http.postForEntity("/api/reservations", body, String.class);
    }

    // --- Then ---

    @Then("예매는 성공한다")
    public void 예매는_성공한다() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"reserved\":true");
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
        거부_응답이다(HttpStatus.NOT_FOUND);
    }

    @Then("결제는 청구되지 않는다")
    public void 결제는_청구되지_않는다() {
        assertThat(payment.wasCharged()).isFalse();
    }

    @Then("예매는 이미 예약됨으로 거부된다")
    public void 이미_예약됨으로_거부된다() {
        거부_응답이다(HttpStatus.CONFLICT);
    }

    @Then("예매는 판매 중지로 거부된다")
    public void 판매_중지로_거부된다() {
        거부_응답이다(HttpStatus.CONFLICT);
    }

    @Then("예매는 결제 실패로 거부된다")
    public void 결제_실패로_거부된다() {
        거부_응답이다(HttpStatus.PAYMENT_REQUIRED);
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
        거부_응답이다(HttpStatus.NOT_FOUND);
    }

    /* 거부 응답 판정. 상태 코드만 보면 "도메인이 거부한 404"와 "엔드포인트가 아예 없어서 난 404"를
     * 구분하지 못한다 — 인바운드 어댑터가 없어도 그 시나리오가 통과해 버린다(공허한 초록불).
     * 그래서 응답이 problem+json 인지 함께 본다: 우리 어댑터가 번역한 거부만 이 타입으로 답한다. */
    private void 거부_응답이다(HttpStatus expected) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getHeaders().getContentType())
                .as("거부 응답은 RFC 7807 problem+json 이어야 한다")
                .isNotNull()
                .satisfies(type -> assertThat(type.toString()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
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
