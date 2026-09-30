package com.thinking.tennis.domain;

/**
 * 신청의 상태. 감시 중으로 시작해 나머지 셋 중 하나로 끝나며, 끝난 신청은 되살아나지 않는다.
 */
public enum AlertStatus {

    /**
     * 감시 중이다. 예약 상태 조회가 일으킨 확인 결과로 빈자리를 판정한다.
     *
     * <p>감시 중인 신청만 보고 예약처를 도는 주기 작업은 없다. 그 코트·날짜를 아무도 조회하지 않으면
     * 확인도 일어나지 않으며, 그 상태는 확인 지연으로 드러난다.
     */
    WATCHING,

    /** 빈자리를 알리는 알림이 발송됐다. */
    NOTIFIED,

    /** 사용자가 해제했다. 확인과 발송을 멈춘다. */
    CANCELED,

    /** 만료 시각이 지났다. */
    EXPIRED;

    /** 끝난 상태인지 여부다. 끝난 신청은 다시 감시 중으로 돌아가지 않는다. */
    public boolean isTerminal() {
        return this != WATCHING;
    }
}
