package com.thinking.tennis.kata;

// 작업 도중 락을 하나 더 요청한 결과다. 이미 쥔 락보다 앞 순위를 요청하면 획득하지 않고 위반으로 돌려준다.
public enum EscalateResult {
    ACQUIRED,
    NOT_ACQUIRED,
    ORDER_VIOLATION
}
