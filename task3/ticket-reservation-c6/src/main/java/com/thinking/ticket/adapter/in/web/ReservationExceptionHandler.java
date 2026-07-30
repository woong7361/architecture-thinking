package com.thinking.ticket.adapter.in.web;

import com.thinking.ticket.core.domain.PaymentFailedException;
import com.thinking.ticket.core.domain.TicketAlreadyReservedException;
import com.thinking.ticket.core.domain.TicketNotFoundException;
import com.thinking.ticket.core.domain.TicketSuspendedException;
import com.thinking.ticket.core.domain.UserNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/* 도메인 거부 → HTTP 상태 코드 번역. 판단은 이미 Core에서 끝났고 여기서는 프로토콜 어휘로 옮기기만 한다 —
 * 이 클래스에 조건 분기로 업무 규칙이 들어오면 그 규칙은 어떤 도메인 테스트도 보지 못하는 자리에 놓인다.
 *
 * 거부 응답은 전부 RFC 7807 application/problem+json으로 낸다. 미디어 타입을 ResponseEntity에
 * 명시하는 이유: 이 타입이 '도메인이 요청을 거부했다'와 '그런 엔드포인트가 없다'를 가르는 신호라서,
 * 프레임워크 기본 동작에 맡겨 조용히 application/json으로 나가면 두 실패가 구분되지 않는다.
 *
 * 적용 범위를 예매 컨트롤러로 한정한다. 전역으로 걸면 이 어댑터와 무관한 진입점의 실패까지
 * 예매용 번역 규칙이 삼켜 버린다. */
@RestControllerAdvice(assignableTypes = ReservationController.class)
public class ReservationExceptionHandler {

    /* 자격이나 대상이 아예 없는 경우 — 요청이 가리키는 자원이 존재하지 않는다. */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "등록되지 않은 회원입니다.");
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTicketNotFound(TicketNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "존재하지 않는 티켓입니다.");
    }

    /* 자원은 있으나 지금 상태가 예약을 허락하지 않는 경우 — 상태 충돌이므로 409다.
     * 400으로 내면 '요청이 잘못됐다'가 되어, 다시 보내면 될 요청과 구분되지 않는다. */
    @ExceptionHandler(TicketAlreadyReservedException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyReserved(TicketAlreadyReservedException e) {
        return problem(HttpStatus.CONFLICT, "이미 예약된 티켓입니다.");
    }

    @ExceptionHandler(TicketSuspendedException.class)
    public ResponseEntity<ProblemDetail> handleSuspended(TicketSuspendedException e) {
        return problem(HttpStatus.CONFLICT, "판매가 중지된 티켓입니다.");
    }

    /* 인스턴스를 여러 개 띄우면 같은 티켓을 동시에 확정하려는 요청이 겹친다. 저장 계층이 그 경합을
     * 잠금 실패로 알려 오는 경로도 결국 '남이 먼저 가져갔다'와 같은 사실이므로 409로 모은다.
     * 500으로 새면 서버 결함처럼 보여, 정상적인 경합을 장애로 오인하게 된다.
     * (조건부 UPDATE로 잡히는 경합은 이미 TicketAlreadyReservedException으로 올라온다 —
     *  이 핸들러는 그 밖의 잠금 실패 표현까지 같은 자리로 끌어오는 몫이다.) */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleLockingFailure(OptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "다른 요청이 먼저 예약을 확정했습니다.");
    }

    /* 결제 거절은 요청도 상태도 아닌 '지불이 이루어지지 않았다'는 별개의 사실이라 402로 옮긴다. */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ProblemDetail> handlePaymentFailed(PaymentFailedException e) {
        return problem(HttpStatus.PAYMENT_REQUIRED, "결제가 거절되었습니다.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        /* type은 about:blank(기본값)로 둔다. 실제로 문서가 없는 URI를 적어 두면 규격을 지킨 것처럼
         * 보이지만 따라가도 아무것도 없는, 이름뿐인 계약이 된다. */
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
