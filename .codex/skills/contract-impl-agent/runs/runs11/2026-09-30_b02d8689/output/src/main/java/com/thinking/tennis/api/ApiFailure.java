package com.thinking.tennis.api;

/**
 * 인바운드 어댑터에서만 판정되는 실패다.
 *
 * <p>요청의 형식, 사용자 식별, 멱등 키 규약은 HTTP 요청을 읽는 자리에서만 볼 수 있다. 유스케이스에
 * 올려 보내면 안쪽이 헤더와 토큰을 알아야 하고, 그러면 같은 유스케이스를 다른 경로로 부를 때 쓸 수 없는
 * 값을 요구하게 된다.
 */
public abstract class ApiFailure extends RuntimeException {

    protected ApiFailure(String message) {
        super(message);
    }

    /** 요청의 형식이나 값이 스키마를 만족하지 않는다. */
    public static final class ValidationFailed extends ApiFailure {

        public ValidationFailed(String detail) {
            super(detail);
        }
    }

    /** 토큰이 없거나 유효하지 않아 사용자를 식별할 수 없다. */
    public static final class Unauthenticated extends ApiFailure {

        public Unauthenticated() {
            super("토큰이 없거나 유효하지 않습니다");
        }
    }

    /** 같은 멱등 키가 다른 내용의 요청에 이미 쓰였다. */
    public static final class IdempotencyKeyReused extends ApiFailure {

        public IdempotencyKeyReused() {
            super("같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다");
        }
    }

    /**
     * 같은 멱등 키의 앞선 요청이 아직 처리 중이다.
     *
     * <p>기다렸다 같은 키로 다시 보내면 그 요청의 결과를 재생으로 받는다. 계약은 이 경우를 경합에 밀린
     * 것과 같은 코드로 알리라고 했다. 클라이언트의 대응이 같기 때문이다.
     */
    public static final class RequestInProgress extends ApiFailure {

        public RequestInProgress() {
            super("같은 Idempotency-Key의 앞선 요청이 아직 처리 중입니다");
        }
    }
}
