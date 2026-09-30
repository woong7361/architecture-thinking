package com.thinking.tennis.domain;

/**
 * 알림 한 건의 발송 결과다.
 */
public enum DeliveryStatus {

    /** 빈자리를 확인해 발송 대기에 올랐고 아직 보내지 않았다. */
    PENDING,

    /** 전송 서비스가 알림을 접수했다. */
    SENT,

    /** 재시도를 모두 쓰고도 실패해 더 보내지 않는다. */
    FAILED
}
