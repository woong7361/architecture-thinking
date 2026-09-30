package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 신청을 들고 있는 인메모리 저장소다.
 *
 * <p>계약 적합성을 판정하는 데 실제 저장소가 필요하지 않고, 게이트가 iteration마다 부팅하므로 부팅이
 * 빠른 쪽을 택했다.
 */
@Component
public class AlertStore {

    private final Map<UUID, Alert> alerts = new ConcurrentHashMap<>();

    /** 신청 하나를 동시에 고치려는 요청을 가르는 자리다. */
    private final Map<UUID, ReentrantLock> updateLocks = new ConcurrentHashMap<>();

    /** 같은 사용자·조건으로 동시에 들어온 신청이 둘이 되지 않게 가르는 자리다. */
    private final Map<String, Object> conditionLocks = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong();

    public long nextSequence() {
        return sequence.incrementAndGet();
    }

    public void save(Alert alert) {
        alerts.put(alert.alertId(), alert);
    }

    /**
     * 요청한 사용자의 신청 하나를 찾는다. 남의 신청은 찾지 못한 것과 같게 다룬다. 존재 여부를 드러내지
     * 않으려면 없는 신청과 남의 신청이 이 자리에서 같은 모양이어야 한다.
     */
    public Optional<Alert> findOwned(UUID alertId, String userId) {
        return Optional.ofNullable(alerts.get(alertId))
                .filter(alert -> alert.userId().equals(userId));
    }

    /**
     * 요청한 사용자의 신청을 최근에 신청한 것부터 돌려준다.
     *
     * @param status null이면 끝난 신청까지 모두 돌려준다. 거르는 기준은 저장된 상태이므로 만료 시각이
     *               지났지만 아직 감시 중으로 저장된 신청은 감시 중으로 걸러도 함께 온다
     */
    public List<Alert> findOwnedBy(String userId, AlertStatus status) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> status == null || alert.status() == status)
                .sorted(Comparator.comparingLong(Alert::sequence).reversed())
                .toList();
    }

    /** 같은 조건을 감시 중인 신청이다. 끝난 신청은 같은 조건이어도 여기 걸리지 않는다. */
    public Optional<Alert> findWatchingCondition(String userId, String courtId, LocalDate date, TimeSlot slot) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(Alert::watching)
                .filter(alert -> alert.sameCondition(courtId, date, slot))
                .min(Comparator.comparingLong(Alert::sequence));
    }

    public List<Alert> watching() {
        return alerts.values().stream().filter(Alert::watching).toList();
    }

    ReentrantLock updateLock(UUID alertId) {
        return updateLocks.computeIfAbsent(alertId, ignored -> new ReentrantLock());
    }

    Object conditionLock(String userId, String courtId, LocalDate date, TimeSlot slot) {
        String key = userId + "|" + courtId + "|" + date + "|" + slot.startTime() + "-" + slot.endTime();
        return conditionLocks.computeIfAbsent(key, ignored -> new Object());
    }
}
