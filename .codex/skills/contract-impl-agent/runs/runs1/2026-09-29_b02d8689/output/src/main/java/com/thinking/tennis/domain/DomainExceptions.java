package com.thinking.tennis.domain;

public final class DomainExceptions {

    private DomainExceptions() {
    }

    public static class AlertNotFound extends RuntimeException {
    }

    public static class CourtNotSupported extends RuntimeException {

        private final String courtId;

        public CourtNotSupported(String courtId) {
            this.courtId = courtId;
        }

        public String courtId() {
            return courtId;
        }
    }

    public static class SlotNotSupported extends RuntimeException {
    }

    public static class AlertWindowClosed extends RuntimeException {
    }

    public static class IdempotencyKeyReused extends RuntimeException {
    }

    public static class ConcurrentUpdateConflict extends RuntimeException {
    }

    public static class StorageTimeout extends RuntimeException {
    }
}
