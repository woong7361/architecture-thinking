package com.thinking.tennis.app.error;

/**
 * 서비스가 지원하지 않는 코트다.
 *
 * <p>계약은 같은 원인을 받은 자리에 따라 다른 상태 코드로 내보내라고 한다. 경로에서 받았으면 404,
 * 본문에서 받았으면 422다. 그 구분은 요청을 받은 자리가 아는 것이라 여기서 {@link Source} 로 들고 가고,
 * 상태 코드로 옮기는 일은 인바운드 어댑터가 한다.
 */
public class UnsupportedCourtException extends RuntimeException {

    private final String courtId;
    private final Source source;

    public UnsupportedCourtException(String courtId, Source source) {
        super("지원하지 않는 코트: " + courtId);
        this.courtId = courtId;
        this.source = source;
    }

    public String courtId() {
        return courtId;
    }

    public Source source() {
        return source;
    }

    /** 코트 식별자를 받은 자리다. */
    public enum Source {
        PATH,
        BODY
    }
}
