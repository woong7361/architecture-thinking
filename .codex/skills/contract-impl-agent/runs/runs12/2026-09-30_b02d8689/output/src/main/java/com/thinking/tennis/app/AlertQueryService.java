package com.thinking.tennis.app;

import com.thinking.tennis.app.error.AlertNotFoundException;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 신청을 읽는 유스케이스다.
 *
 * <p>모든 조회는 사용자 본인의 신청으로 한정한다. 남의 신청은 어떤 방법으로도 목록에 나오지 않고,
 * 단건으로 물으면 없는 것과 같은 답을 받는다.
 */
@Service
public class AlertQueryService {

    private final AlertRepository alerts;
    private final AlertViewFactory views;

    public AlertQueryService(AlertRepository alerts, AlertViewFactory views) {
        this.alerts = alerts;
        this.views = views;
    }

    /**
     * 본인의 신청 목록이다. 최근에 신청한 것이 앞에 온다.
     *
     * <p>거르는 기준은 저장된 상태다. 만료 시각이 지났지만 아직 감시 중으로 저장된 신청은 감시 중으로
     * 걸러도 함께 온다. 만료로 옮기는 일은 서버 작업이 하므로 조회가 그 전이를 앞당기지 않는다.
     *
     * @param statusFilter null이면 끝난 신청까지 모두 돌려준다
     */
    public List<AlertView> list(String userId, AlertStatus statusFilter) {
        List<Alert> found = alerts.findByUserNewestFirst(userId, statusFilter);
        List<AlertView> result = new ArrayList<>(found.size());
        for (Alert alert : found) {
            result.add(views.of(alert));
        }
        return List.copyOf(result);
    }

    /**
     * 본인의 신청 하나다.
     *
     * @throws AlertNotFoundException 없는 신청이거나 남의 신청이다
     */
    public AlertView get(String userId, String alertId) {
        return alerts.findById(alertId)
                .filter(alert -> alert.ownedBy(userId))
                .map(views::of)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }
}
