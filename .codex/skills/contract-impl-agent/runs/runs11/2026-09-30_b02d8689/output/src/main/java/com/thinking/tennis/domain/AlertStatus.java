package com.thinking.tennis.domain;

/**
 * 신청의 상태다. 감시 중으로 시작해 나머지 셋 중 하나로 끝나며, 끝난 신청은 되살아나지 않는다.
 *
 * <p>계약의 {@code AlertStatus} 와 같은 이름과 같은 값을 쓴다. 이름을 다르게 두면 옮겨 적는 자리만 늘고
 * 그 자리에서 값이 어긋날 수 있다. 바깥으로 나가는 표현은 {@code api} 가 소유하므로 이 열거형은
 * 직렬화 기술을 모른다.
 */
public enum AlertStatus {

    /** 감시 중이다. 서버가 주기적으로 예약 상태를 확인한다. */
    WATCHING,
    /** 빈자리를 알리는 알림이 발송됐다. */
    NOTIFIED,
    /** 사용자가 해제했다. 확인과 발송을 멈춘다. */
    CANCELED,
    /** 만료 시각이 지났다. */
    EXPIRED;

    /** 끝난 상태인지 여부다. 끝난 신청은 되살아나지 않는다. */
    public boolean terminal() {
        return this != WATCHING;
    }
}
