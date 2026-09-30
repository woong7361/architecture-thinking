package com.thinking.tennis.app;

import java.time.Instant;

/**
 * 멱등 키와 그 키로 처리한 2xx 응답을 보관하는 포트다.
 *
 * <p>사용자와 오퍼레이션 단위로 보관하고 보관 기간은 24시간이다. 기간이 지난 뒤 같은 키로 다시 오면
 * 재생이 아니라 새 요청으로 처리한다.
 *
 * <p>4xx와 5xx는 보관하지 않는다. 4xx는 요청을 고쳐야 풀리므로 같은 키로 다시 보내면 조건을 다시
 * 판정해야 하고, 5xx는 같은 키로 그대로 다시 보내 처리되게 해야 한다.
 */
public interface IdempotencyStore {

    /**
     * 이 키로 처리를 시작해도 되는지 묻는다.
     *
     * <p>처리를 시작할 수 있으면 그 자리를 잡아 둔다. 잡아 둔 자리는 {@link #complete} 로 응답을 채우거나
     * {@link #release} 로 놓아야 한다. 놓지 않으면 같은 키의 다음 요청이 계속 처리 중으로 보인다.
     */
    Claim claim(String userId, String operationId, String key, String fingerprint, Instant now);

    /** 2xx로 끝난 응답을 이 키에 붙여 보관한다. */
    void complete(String userId, String operationId, String key, int statusCode, AlertView view, Instant now);

    /** 2xx로 끝나지 않아 잡아 둔 자리를 놓는다. */
    void release(String userId, String operationId, String key);

    enum Outcome {
        /** 이 키로 처리를 시작했다. */
        STARTED,
        /** 같은 키의 앞선 요청이 아직 처리 중이고 그 요청의 내용이 이번 요청과 같다. */
        IN_PROGRESS,
        /**
         * 같은 키의 앞선 요청이 아직 처리 중인데 그 요청의 내용이 이번 요청과 다르다.
         *
         * <p>{@link #IN_PROGRESS} 와 가르는 이유는 기다린 뒤의 결과가 다르기 때문이다. 내용이 같으면
         * 앞선 요청의 응답을 재생으로 받지만, 다르면 앞선 요청이 2xx로 끝난 뒤 같은 키를 다시 쓴 것이
         * 되어 {@link #FINGERPRINT_MISMATCH} 로 거절된다. 물러나는 자리는 같아도 안내는 같을 수 없다.
         */
        IN_PROGRESS_OTHER_REQUEST,
        /** 같은 키로 처리한 응답이 있다. 그 응답을 그대로 다시 돌려준다. */
        REPLAY,
        /** 같은 키가 다른 내용의 요청에 이미 쓰였다. */
        FINGERPRINT_MISMATCH
    }

    /**
     * 자리를 물은 결과다.
     *
     * @param statusCode 재생할 응답의 상태 코드. 재생이 아니면 0이다
     * @param view       재생할 응답의 내용. 재생이 아니면 null이다
     */
    record Claim(Outcome outcome, int statusCode, AlertView view) {

        public static Claim started() {
            return new Claim(Outcome.STARTED, 0, null);
        }

        public static Claim inProgress(boolean sameRequest) {
            return new Claim(sameRequest ? Outcome.IN_PROGRESS : Outcome.IN_PROGRESS_OTHER_REQUEST, 0, null);
        }

        public static Claim replay(int statusCode, AlertView view) {
            return new Claim(Outcome.REPLAY, statusCode, view);
        }

        public static Claim fingerprintMismatch() {
            return new Claim(Outcome.FINGERPRINT_MISMATCH, 0, null);
        }
    }
}
