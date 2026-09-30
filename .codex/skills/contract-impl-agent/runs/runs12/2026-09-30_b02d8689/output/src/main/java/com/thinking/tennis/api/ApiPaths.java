package com.thinking.tennis.api;

/**
 * 밖에서 보이는 경로를 만든다.
 *
 * <p>주 버전 접두사는 서버 주소에 있으므로 각 오퍼레이션의 경로에 다시 넣지 않는다. 그래서 컨트롤러가
 * 받는 경로에는 접두사가 없다. 그러나 응답에 실어 보내는 위치와 실패가 난 요청의 경로는 클라이언트가
 * 그대로 다시 부를 수 있어야 하므로 접두사를 붙여 만든다. 계약의 예시도 붙은 모양이다.
 */
final class ApiPaths {

    /** 주 버전 접두사다. 서버 주소의 경로와 같다. */
    private static final String VERSION_PREFIX = "/v1";

    static String alertLocation(String alertId) {
        return VERSION_PREFIX + "/alerts/" + alertId;
    }

    static String instance(String requestUri) {
        return VERSION_PREFIX + requestUri;
    }

    private ApiPaths() {
    }
}
