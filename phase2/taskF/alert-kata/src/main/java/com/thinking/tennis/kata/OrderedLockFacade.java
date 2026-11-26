package com.thinking.tennis.kata;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

// 락 획득의 단일 창구다. 호출자는 필요한 락과 할 일만 넘기고, 획득 순서는 이 클래스만 정한다.
public final class OrderedLockFacade {
    private final RedisSpinLock lock;
    private final LockOrder order;
    private final Duration budget;

    public OrderedLockFacade(RedisSpinLock lock, LockOrder order, Duration budget) {
        this.lock = lock;
        this.order = order;
        this.budget = budget;
    }

    public LockedOutcome inOrder(List<LockRequest> requests, LockedWork work) throws InterruptedException {
        List<LockRequest> planned = new ArrayList<>(new LinkedHashSet<>(requests));
        if (order == LockOrder.GLOBAL) {
            planned.sort(LockRequest.GLOBAL_ORDER);
        }

        Acquisition acquisition = new Acquisition(System.nanoTime() + budget.toNanos());
        LockedOutcome.Status status = LockedOutcome.Status.COMPLETED;
        try {
            for (LockRequest request : planned) {
                if (!acquisition.acquire(request)) {
                    status = LockedOutcome.Status.NOT_ACQUIRED;
                    break;
                }
            }
            if (status == LockedOutcome.Status.COMPLETED) {
                work.run(acquisition);
                if (acquisition.violated) {
                    status = LockedOutcome.Status.ORDER_VIOLATION;
                }
            }
        } finally {
            acquisition.releaseAll();
        }
        return acquisition.outcome(status);
    }

    // 한 번의 창구 통과 동안 보유한 락과 그 기록을 들고 있다.
    private final class Acquisition implements LockSession {
        private final long deadlineNanos;
        private final List<LockRequest> heldRequests = new ArrayList<>();
        private final List<LockToken> heldTokens = new ArrayList<>();
        private final List<LockStep> steps = new ArrayList<>();
        private boolean violated;

        private Acquisition(long deadlineNanos) {
            this.deadlineNanos = deadlineNanos;
        }

        @Override
        public EscalateResult escalate(LockRequest request) throws InterruptedException {
            if (heldRequests.contains(request)) {
                return EscalateResult.ACQUIRED;
            }
            if (order == LockOrder.GLOBAL && !isAfterEveryHeldLock(request)) {
                violated = true;
                return EscalateResult.ORDER_VIOLATION;
            }
            return acquire(request) ? EscalateResult.ACQUIRED : EscalateResult.NOT_ACQUIRED;
        }

        private boolean isAfterEveryHeldLock(LockRequest request) {
            return heldRequests.stream().allMatch(held -> LockRequest.GLOBAL_ORDER.compare(held, request) < 0);
        }

        private boolean acquire(LockRequest request) throws InterruptedException {
            long started = System.nanoTime();
            Optional<LockToken> token = lock.acquire(request.key(), remainingBudget());
            double waitMillis = (System.nanoTime() - started) / 1_000_000.0;
            boolean allPreviousHeld = heldTokens.stream().allMatch(lock::heldBy);

            steps.add(new LockStep(request, waitMillis, token.isPresent(), allPreviousHeld, null));
            if (token.isEmpty()) {
                return false;
            }
            heldRequests.add(request);
            heldTokens.add(token.get());
            return true;
        }

        private Duration remainingBudget() {
            long remaining = deadlineNanos - System.nanoTime();
            return remaining <= 0 ? Duration.ZERO : Duration.ofNanos(remaining);
        }

        // 해제 하나가 실패해도 나머지를 전부 해제한다.
        private void releaseAll() {
            for (int index = heldTokens.size() - 1; index >= 0; index--) {
                LockRequest request = heldRequests.get(index);
                try {
                    recordRelease(request, lock.release(heldTokens.get(index)));
                } catch (RuntimeException failed) {   // 해제 호출이 실패한 경우도 소유권을 확인하지 못한 것으로 적는다
                    recordRelease(request, ReleaseResult.NOT_RELEASED);
                }
            }
            heldTokens.clear();
            heldRequests.clear();
        }

        private void recordRelease(LockRequest request, ReleaseResult result) {
            for (int index = steps.size() - 1; index >= 0; index--) {
                LockStep step = steps.get(index);
                if (step.request().equals(request) && step.acquired() && step.releaseResult() == null) {
                    steps.set(index, new LockStep(step.request(), step.waitMillis(), true,
                            step.allPreviousHeldAtAcquire(), result));
                    return;
                }
            }
        }

        private LockedOutcome outcome(LockedOutcome.Status status) {
            return new LockedOutcome(status, List.copyOf(steps));
        }
    }
}
