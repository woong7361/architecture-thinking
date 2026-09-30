package com.thinking.tennis.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * 응답에 싣는 경로 참조를 만든다.
 *
 * <p>계약의 서버 주소가 주 버전 접두사를 들고 있으므로 오퍼레이션의 경로에는 그것을 다시 넣지 않는다.
 * 그런데 {@code Location} 과 {@code instance} 는 클라이언트가 그대로 따라갈 참조이고 계약은 그 예시를
 * 접두사까지 붙은 모양으로 적었다. 그래서 매핑 경로에는 넣지 않고 만들어 내보내는 참조에만 붙인다.
 */
public final class ApiUris {

    /** 계약의 서버 주소가 가진 주 버전 접두사다. */
    public static final String VERSION_PREFIX = "/v1";

    public static String alertLocation(UUID alertId) {
        return VERSION_PREFIX + "/alerts/" + alertId;
    }

    /** 실패가 난 요청의 경로다. */
    public static String instance(HttpServletRequest request) {
        return VERSION_PREFIX + request.getRequestURI();
    }

    private ApiUris() {
    }
}
