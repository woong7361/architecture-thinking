package com.thinking.ticket.adapter.in.web;

import com.thinking.ticket.core.domain.PaymentFailedException;
import com.thinking.ticket.core.domain.TicketAlreadyReservedException;
import com.thinking.ticket.core.domain.TicketNotFoundException;
import com.thinking.ticket.core.domain.TicketSuspendedException;
import com.thinking.ticket.core.domain.UserNotFoundException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/* 도메인 거부 사유를 HTTP 상태로 번역하는 Inbound Adapter의 일부.
 * (없는 티켓 -> NPE -> 500은 원본 quirk 보존을 위해 일부러 매핑하지 않는다.) */
@RestControllerAdvice
public class ReservationExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "등록되지 않은 회원");
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ProblemDetail handleTicketNotFound(TicketNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "존재하지 않는 티켓");
    }

    @ExceptionHandler({TicketSuspendedException.class, TicketAlreadyReservedException.class})
    public ProblemDetail handleNotReservable(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "예약할 수 없는 티켓: " + e.getClass().getSimpleName());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ProblemDetail handlePaymentFailed(PaymentFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, "결제 거절");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentReservation(OptimisticLockingFailureException e) {
        // 수평 확장(scale-out) 시 다른 인스턴스가 먼저 예약한 경우.
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "이미 다른 요청이 예약을 확정함");
    }
}
