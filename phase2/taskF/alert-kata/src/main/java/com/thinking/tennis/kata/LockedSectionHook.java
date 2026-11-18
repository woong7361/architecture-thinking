package com.thinking.tennis.kata;

public interface LockedSectionHook {
    LockedSectionHook NONE = new LockedSectionHook() {
    };

    default void entered(LockToken token) {
    }

    default void beforeExit(LockToken token, boolean ownedAtExit) {
    }

    default boolean skipRelease() {
        return false;
    }
}
