package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 본인의 신청을 읽는다.
 *
 * <p>목록과 단건 모두 요청한 사용자의 것만 돌려준다. 남의 신청은 어떤 방법으로도 이 경로로 나오지
 * 않으며, 단건 조회에서 남의 신청을 물으면 존재 여부를 드러내지 않기 위해 없는 것과 같은 실패를 낸다.
 *
 * <p>조회는 확인을 일으키지 않는다. 확인을 일으키는 것은 예약 상태 조회뿐이라고 요구사항이 정했다.
 */
@Service
public class QueryAlertsUseCase {

    private final AlertStore alerts;
    private final AlertViewFactory views;

    public QueryAlertsUseCase(AlertStore alerts, AlertViewFactory views) {
        this.alerts = alerts;
        this.views = views;
    }

    /**
     * @param status null이면 끝난 신청까지 모두 돌려준다
     */
    public List<AlertView> list(String userId, AlertStatus status) {
        return views.of(alerts.findOwnedBy(userId, status));
    }

    public AlertView get(String userId, UUID alertId) {
        Alert alert = alerts.findOwned(alertId, userId)
                .orElseThrow(AlertOperationFailure.AlertNotFound::new);
        return views.of(alert);
    }
}
