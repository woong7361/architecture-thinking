package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason;

import java.time.Instant;

/**
 * 유스케이스가 처리하지 못한 이유다.
 *
 * <p>여기에는 상태 코드도 에러 코드도 없다. 실패를 상태 코드로 옮기는 판단은 계약이 정한 대로 인바운드
 * 어댑터가 하고, 유스케이스는 무엇이 안 됐는지만 말한다. 포트가 확인 실패를 {@code CheckFailed} 로
 * 표현한 것과 같은 이유다.
 *
 * <p>실패 종류를 한 파일에 중첩해 둔 것은 포트가 자기 예외를 인터페이스 안에 둔 것과 같은 결이다.
 * 매핑하는 자리에서 한눈에 목록을 볼 수 있어야 계약이 선언한 실패와 대조하기 쉽다.
 */
public abstract class AlertOperationFailure extends RuntimeException {

    protected AlertOperationFailure(String message) {
        super(message);
    }

    /**
     * 같은 실패를 어디서 받았는지다. 계약은 지원하지 않는 코트를 경로에서 받았으면 404, 본문에서 받았으면
     * 422로 내보내라고 하므로 실패를 내는 자리가 이것을 함께 말해야 한다.
     */
    public enum Origin {
        PATH,
        BODY
    }

    /** 서비스가 지원하지 않는 코트다. */
    public static final class UnsupportedCourt extends AlertOperationFailure {

        private final String courtId;
        private final Origin origin;

        public UnsupportedCourt(String courtId, Origin origin) {
            super("지원하지 않는 코트: " + courtId);
            this.courtId = courtId;
            this.origin = origin;
        }

        public String courtId() {
            return courtId;
        }

        public Origin origin() {
            return origin;
        }
    }

    /** 코트는 지원하지만 그 코트가 그 날짜에 운영하지 않는 시간대다. */
    public static final class UnsupportedSlot extends AlertOperationFailure {

        private final String description;

        public UnsupportedSlot(String description) {
            super(description);
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    /** 만료 시각이 이미 지나 감시할 수 없는 조건이다. */
    public static final class WindowClosed extends AlertOperationFailure {

        private final Instant expiresAt;

        public WindowClosed(Instant expiresAt) {
            super("이용 시작 시각이 지나 감시할 수 없습니다: " + expiresAt);
            this.expiresAt = expiresAt;
        }

        public Instant expiresAt() {
            return expiresAt;
        }
    }

    /** 그런 신청이 없거나 요청한 사용자의 것이 아니다. 두 경우를 구분해 알리지 않는다. */
    public static final class AlertNotFound extends AlertOperationFailure {

        public AlertNotFound() {
            super("신청을 찾을 수 없습니다");
        }
    }

    /** 외부 예약처 확인이 실패했고 저장된 마지막 성공 결과도 없다. */
    public static final class AvailabilityUnavailable extends AlertOperationFailure {

        private final UpstreamFailureReason reason;

        public AvailabilityUnavailable(UpstreamFailureReason reason) {
            super("예약 상태를 확인하지 못했습니다: " + reason);
            this.reason = reason;
        }

        public UpstreamFailureReason reason() {
            return reason;
        }
    }

    /**
     * 신청을 받을 수 있는 조건인지 판단할 근거를 얻지 못했다.
     *
     * <p>그 코트가 그 날짜에 운영하는 시간대를 모르면 신청을 받아도 되는지 말할 수 없다. 계약은 신청
     * 오퍼레이션에 예약처 실패를 알리는 상태 코드를 두지 않았으므로 확인 실패와는 다른 실패로 둔다.
     */
    public static final class AvailabilityUndetermined extends AlertOperationFailure {

        private final UpstreamFailureReason reason;

        public AvailabilityUndetermined(UpstreamFailureReason reason) {
            super("운영 시간대를 확인하지 못해 신청 조건을 판정할 수 없습니다: " + reason);
            this.reason = reason;
        }

        public UpstreamFailureReason reason() {
            return reason;
        }
    }

    /** 같은 자원을 동시에 고치려다 경합에 밀렸다. */
    public static final class ConcurrentUpdate extends AlertOperationFailure {

        public ConcurrentUpdate(String message) {
            super(message);
        }
    }
}
