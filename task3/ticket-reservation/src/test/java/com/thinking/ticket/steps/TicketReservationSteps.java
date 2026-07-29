package com.thinking.ticket.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.thinking.ticket.core.application.TicketService;
import com.thinking.ticket.core.domain.DiscountPolicy;
import com.thinking.ticket.core.domain.PaymentFailedException;
import com.thinking.ticket.core.domain.Ticket;
import com.thinking.ticket.core.domain.TicketAlreadyReservedException;
import com.thinking.ticket.core.domain.TicketSuspendedException;
import com.thinking.ticket.core.domain.User;
import com.thinking.ticket.core.domain.UserNotFoundException;
import com.thinking.ticket.support.InMemoryTicketRepository;
import com.thinking.ticket.support.InMemoryUserRepository;
import com.thinking.ticket.support.RecordingPaymentApi;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * reserveTicket 유스케이스 경계의 특성화 안전망 step 정의.
 * 도메인 언어 Given/When/Then을 fake 저장소 상태와 결제 double 기록으로 잇는다.
 * quirk의 구현 고도 사실(NPE 타입, 저장 실패 전파)은 여기(step)에서 구체로 단언한다 —
 * feature 문장은 도메인 언어로 유지하기 위해.
 *
 * <p>헥사고날 재배치 후에도 이 step은 Core를 in-memory 아웃바운드 어댑터에 끼워 그대로 검증한다 —
 * Spring 컨텍스트·DB 없이 돈다(단언 불변, import 경로만 core.* 로 갱신).
 */
public final class TicketReservationSteps {

    private final InMemoryUserRepository userRepo = new InMemoryUserRepository();
    private final InMemoryTicketRepository ticketRepo = new InMemoryTicketRepository();
    private RecordingPaymentApi paymentApi = RecordingPaymentApi.succeeding();

    private RuntimeException injectedSaveFailure;
    private Boolean result;
    private Throwable thrown;

    // --- Given ---

    @Given("회원 저장소와 티켓 저장소가 비어 있다")
    public void 저장소가_비어_있다() {
        // fake는 필드로 새로 생성되어 이미 비어 있음. 시나리오마다 새 step 인스턴스라 상태가 격리된다.
        this.result = null;
        this.thrown = null;
    }

    @Given("회원 {long}이 등록되어 있다")
    public void 회원이_등록되어_있다(long userId) {
        userRepo.save(new User(userId, "user-" + userId));
    }

    @Given("가격 {int}원짜리 미예약 티켓 {long}이 있다")
    public void 미예약_티켓이_있다(int price, long ticketId) {
        ticketRepo.seed(new Ticket(ticketId, price));
    }

    @Given("가격 {int}원짜리 이미 예약된 티켓 {long}이 있다")
    public void 이미_예약된_티켓이_있다(int price, long ticketId) {
        // 경계-클린 셋업: 내부 접근자(setter)나 도메인 메서드(assignTo)에 손대지 않고
        // 유스케이스(reserveTicket)로 '이미 예약됨' 상태를 만든다 — 내부 리팩토링에 안 깨진다.
        // 셋업 전용 회원·결제 double을 써서 When 단계의 결제 기록(paymentApi)을 오염시키지 않는다.
        ticketRepo.seed(new Ticket(ticketId, price));
        long setupUserId = -1L;
        userRepo.save(new User(setupUserId, "setup"));
        TicketService setup =
                new TicketService(ticketRepo, userRepo, RecordingPaymentApi.succeeding(), new DiscountPolicy());
        setup.reserveTicket(setupUserId, ticketId, "setup-token");
    }

    @Given("가격 {int}원짜리 판매 중지된 티켓 {long}이 있다")
    public void 판매_중지된_티켓이_있다(int price, long ticketId) {
        Ticket ticket = new Ticket(ticketId, price);
        ticket.suspend();
        ticketRepo.seed(ticket);
    }

    @Given("결제는 거절되는 상황이다")
    public void 결제는_거절된다() {
        this.paymentApi = RecordingPaymentApi.failing();
    }

    @Given("티켓 {long}은 저장소에 없다")
    public void 티켓이_저장소에_없다(long ticketId) {
        // 일부러 seed하지 않는다 — findById가 null을 반환해 원본 코드의 null 미검사 경로를 태운다.
    }

    @Given("티켓 저장이 실패하는 상황이다")
    public void 티켓_저장이_실패한다() {
        this.injectedSaveFailure = new RuntimeException("storage failure");
        ticketRepo.failSaveWith(injectedSaveFailure);
    }

    // --- When ---

    @When("회원 {long}이 카드정보 {string}으로 티켓 {long}을 예매하면")
    public void 예매하면(long userId, String paymentInfo, long ticketId) {
        TicketService service = new TicketService(ticketRepo, userRepo, paymentApi, new DiscountPolicy());
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
        Ticket stored = ticketRepo.findById(ticketId);
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
        Ticket stored = ticketRepo.findById(ticketId);
        assertThat(stored.isReserved()).isFalse();
    }

    @Then("{int}원 청구가 시도된다")
    public void 청구가_시도된다(int amount) {
        assertThat(paymentApi.wasCharged()).isTrue();
        assertThat(paymentApi.lastAmount()).isEqualTo(amount);
    }

    @Then("예매는 널 참조로 비정상 종료된다")
    public void 널_참조로_비정상_종료된다() {
        // quirk 박제: 없는 티켓에 도메인 예외가 아니라 NPE가 나는 것이 현재 동작.
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Then("예매는 저장 실패로 비정상 종료된다")
    public void 저장_실패로_비정상_종료된다() {
        // quirk 박제: 저장 실패 예외가 도메인 예외로 감싸이거나 보상되지 않고 그대로 전파된다.
        assertThat(thrown).isSameAs(injectedSaveFailure);
    }

    @Then("{int}원이 청구된 채로 남는다")
    public void 청구된_채로_남는다(int amount) {
        assertThat(paymentApi.wasCharged()).isTrue();
        assertThat(paymentApi.lastAmount()).isEqualTo(amount);
    }

    @Then("되돌리는 보상 결제 호출은 없다")
    public void 보상_호출은_없다() {
        // charge는 최초 1회뿐 — 저장 실패 후 결제를 되돌리는(취소/보상) 추가 호출이 없다.
        assertThat(paymentApi.chargeCount()).isEqualTo(1);
    }
}
