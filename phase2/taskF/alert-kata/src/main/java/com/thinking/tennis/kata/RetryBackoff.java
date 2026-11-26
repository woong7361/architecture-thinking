package com.thinking.tennis.kata;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

// 재시도 사이의 대기를 정하는 네 방식. 식은 AWS 아키텍처 블로그의 백오프 시뮬레이터와 같다.
public enum RetryBackoff {
    NONE {
        @Override
        Duration waitBefore(int attemptNumber, Duration base, Duration cap) {
            return Duration.ZERO;
        }
    },
    // 고정 간격은 상한값을 쓴다. 지수 증가가 상한에 닿은 뒤와 같은 간격이다.
    FIXED {
        @Override
        Duration waitBefore(int attemptNumber, Duration base, Duration cap) {
            return cap;
        }
    },
    FIXED_JITTER {
        @Override
        Duration waitBefore(int attemptNumber, Duration base, Duration cap) {
            return Duration.ofNanos(uniform(cap.toNanos()));
        }
    },
    EXPONENTIAL {
        @Override
        Duration waitBefore(int attemptNumber, Duration base, Duration cap) {
            return exponential(attemptNumber, base, cap);
        }
    },
    EQUAL_JITTER {
        @Override
        Duration waitBefore(int attemptNumber, Duration base, Duration cap) {
            long half = exponential(attemptNumber, base, cap).toNanos() / 2;
            return Duration.ofNanos(half + uniform(half));
        }
    },
    FULL_JITTER {
        @Override
        Duration waitBefore(int attemptNumber, Duration base, Duration cap) {
            return Duration.ofNanos(uniform(exponential(attemptNumber, base, cap).toNanos()));
        }
    };

    abstract Duration waitBefore(int attemptNumber, Duration base, Duration cap);

    // min(cap, 2^attemptNumber * base). 곱셈을 상한에서 멈춰 넘침을 막는다.
    static Duration exponential(int attemptNumber, Duration base, Duration cap) {
        long capNanos = cap.toNanos();
        long waitNanos = base.toNanos();
        for (int doubled = 0; doubled < attemptNumber && waitNanos < capNanos; doubled++) {
            waitNanos *= 2;
        }
        return Duration.ofNanos(Math.min(waitNanos, capNanos));
    }

    private static long uniform(long boundNanos) {
        return boundNanos <= 0 ? 0 : ThreadLocalRandom.current().nextLong(boundNanos);
    }
}
