package com.thinking.tennis.kata;

// 획득 순서를 호출자가 넘긴 대로 둘지, 전역 순서로 바꿀지 고른다. 앞의 값은 데드락 재현용 비교군이다.
public enum LockOrder {
    AS_REQUESTED,
    GLOBAL
}
