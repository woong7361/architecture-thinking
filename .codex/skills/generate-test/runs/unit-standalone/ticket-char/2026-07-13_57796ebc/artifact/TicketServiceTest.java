import com.thinking.ticket.PaymentApi;
import com.thinking.ticket.PaymentFailedException;
import com.thinking.ticket.Ticket;
import com.thinking.ticket.TicketAlreadyReservedException;
import com.thinking.ticket.TicketRepository;
import com.thinking.ticket.TicketService;
import com.thinking.ticket.User;
import com.thinking.ticket.UserNotFoundException;
import com.thinking.ticket.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final long USER_ID = 10L;
    private static final long TICKET_ID = 20L;
    private static final String PAYMENT_INFO = "card-token";
    private static final int TICKET_PRICE = 30_000;

    @Mock
    private TicketRepository ticketRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private PaymentApi paymentApi;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepo, userRepo, paymentApi);
    }

    @Test
    @DisplayName("유저 존재, 미예약 티켓, 결제 성공이면 티켓을 예약하고 저장한 뒤 true를 반환한다")
    void reserveTicket_reservesTicketAndReturnsTrue_whenUserExistsTicketIsOpenAndPaymentSucceeds() {
        User user = new User(USER_ID, "user");
        Ticket ticket = new Ticket(TICKET_ID, TICKET_PRICE);
        when(userRepo.findById(USER_ID)).thenReturn(user);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(ticket);
        when(paymentApi.charge(PAYMENT_INFO, TICKET_PRICE)).thenReturn(true);

        boolean result = ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO);

        assertThat(result).isTrue();
        assertThat(ticket.isReserved()).isTrue();
        assertThat(ticket.getUserId()).isEqualTo(USER_ID);
        verify(paymentApi).charge(PAYMENT_INFO, TICKET_PRICE);
        verify(ticketRepo).save(same(ticket));
    }

    @Test
    @DisplayName("결제 호출은 티켓 저장보다 먼저 일어난다")
    void reserveTicket_chargesBeforeSavingTicket_whenReservationSucceeds() {
        User user = new User(USER_ID, "user");
        Ticket ticket = new Ticket(TICKET_ID, TICKET_PRICE);
        when(userRepo.findById(USER_ID)).thenReturn(user);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(ticket);
        when(paymentApi.charge(PAYMENT_INFO, TICKET_PRICE)).thenReturn(true);

        ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO);

        InOrder inOrder = inOrder(paymentApi, ticketRepo);
        inOrder.verify(paymentApi).charge(PAYMENT_INFO, TICKET_PRICE);
        inOrder.verify(ticketRepo).save(same(ticket));
    }

    @Test
    @DisplayName("유저가 없으면 UserNotFoundException을 던지고 이후 협력자를 호출하지 않는다")
    void reserveTicket_throwsUserNotFoundExceptionAndStops_whenUserDoesNotExist() {
        when(userRepo.findById(USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepo).findById(USER_ID);
        verifyNoInteractions(ticketRepo, paymentApi);
    }

    @Test
    @DisplayName("이미 예약된 티켓이면 TicketAlreadyReservedException을 던지고 결제와 저장을 하지 않는다")
    void reserveTicket_throwsTicketAlreadyReservedExceptionAndStops_whenTicketIsAlreadyReserved() {
        User user = new User(USER_ID, "user");
        Ticket ticket = new Ticket(TICKET_ID, TICKET_PRICE);
        ticket.setReserved(true);
        when(userRepo.findById(USER_ID)).thenReturn(user);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(ticket);

        assertThatThrownBy(() -> ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO))
                .isInstanceOf(TicketAlreadyReservedException.class);

        verify(userRepo).findById(USER_ID);
        verify(ticketRepo).findById(TICKET_ID);
        verifyNoInteractions(paymentApi);
        verify(ticketRepo, never()).save(any());
        verifyNoMoreInteractions(userRepo, ticketRepo);
    }

    @Test
    @DisplayName("결제가 실패하면 PaymentFailedException을 던지고 티켓 상태 변경과 저장을 하지 않는다")
    void reserveTicket_throwsPaymentFailedExceptionAndDoesNotMutateOrSave_whenPaymentFails() {
        User user = new User(USER_ID, "user");
        Ticket ticket = new Ticket(TICKET_ID, TICKET_PRICE);
        when(userRepo.findById(USER_ID)).thenReturn(user);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(ticket);
        when(paymentApi.charge(PAYMENT_INFO, TICKET_PRICE)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO))
                .isInstanceOf(PaymentFailedException.class);

        assertThat(ticket.isReserved()).isFalse();
        assertThat(ticket.getUserId()).isEqualTo(0L);
        verify(paymentApi).charge(PAYMENT_INFO, TICKET_PRICE);
        verify(ticketRepo, never()).save(any());
    }

    @Test
    @DisplayName("티켓 조회가 null이면 null 체크 없이 NullPointerException을 던진다")
    void reserveTicket_throwsNullPointerException_whenTicketRepositoryReturnsNull() {
        User user = new User(USER_ID, "user");
        when(userRepo.findById(USER_ID)).thenReturn(user);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(null);

        assertThatThrownBy(() -> ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO))
                .isInstanceOf(NullPointerException.class);

        verify(userRepo).findById(USER_ID);
        verify(ticketRepo).findById(TICKET_ID);
        verifyNoInteractions(paymentApi);
        verify(ticketRepo, never()).save(any());
        verifyNoMoreInteractions(userRepo, ticketRepo);
    }

    @Test
    @DisplayName("결제 성공 후 저장이 실패하면 저장 예외가 그대로 전파되고 추가 보상 호출 없이 변경된 티켓이 남는다")
    void reserveTicket_propagatesSaveExceptionAfterSuccessfulCharge_withoutRollbackOrCompensation() {
        User user = new User(USER_ID, "user");
        Ticket ticket = new Ticket(TICKET_ID, TICKET_PRICE);
        RuntimeException saveFailure = new RuntimeException("save failed");
        when(userRepo.findById(USER_ID)).thenReturn(user);
        when(ticketRepo.findById(TICKET_ID)).thenReturn(ticket);
        when(paymentApi.charge(PAYMENT_INFO, TICKET_PRICE)).thenReturn(true);
        doThrow(saveFailure).when(ticketRepo).save(ticket);

        assertThatThrownBy(() -> ticketService.reserveTicket(USER_ID, TICKET_ID, PAYMENT_INFO))
                .isSameAs(saveFailure);

        assertThat(ticket.isReserved()).isTrue();
        assertThat(ticket.getUserId()).isEqualTo(USER_ID);

        InOrder inOrder = inOrder(paymentApi, ticketRepo);
        inOrder.verify(paymentApi).charge(PAYMENT_INFO, TICKET_PRICE);
        inOrder.verify(ticketRepo).save(same(ticket));
        verify(userRepo).findById(USER_ID);
        verify(ticketRepo).findById(TICKET_ID);
        verifyNoMoreInteractions(userRepo, ticketRepo, paymentApi);
    }
}
