package com.thinking.tennis.app.store;

import com.thinking.tennis.app.AlertRepository;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link AlertRepository} 의 인메모리 구현이다.
 *
 * <p>저장 순서를 함께 들고 있는 이유는 정렬이다. 계약은 최근에 신청한 것이 앞에 오라고 하는데 신청
 * 시각이 같은 두 신청의 앞뒤는 그것만으로 정해지지 않아, 저장된 순서를 뒷순위 기준으로 쓴다.
 */
@Component
public class InMemoryAlertRepository implements AlertRepository {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public synchronized Alert insertIfNotWatching(Alert candidate) {
        Optional<Alert> existing = findWatching(candidate.userId(), candidate.courtId(),
                candidate.date(), candidate.slot());
        if (existing.isPresent()) {
            return existing.get();
        }
        entries.put(candidate.alertId(), new Entry(sequence.incrementAndGet(), candidate));
        return candidate;
    }

    @Override
    public Optional<Alert> findById(String alertId) {
        Entry entry = alertId == null ? null : entries.get(alertId);
        return entry == null ? Optional.empty() : Optional.of(entry.alert());
    }

    @Override
    public List<Alert> findByUserNewestFirst(String userId, AlertStatus statusFilter) {
        List<Entry> owned = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (!entry.alert().ownedBy(userId)) {
                continue;
            }
            if (statusFilter != null && entry.alert().status() != statusFilter) {
                continue;
            }
            owned.add(entry);
        }
        owned.sort(NEWEST_FIRST);
        List<Alert> alerts = new ArrayList<>(owned.size());
        for (Entry entry : owned) {
            alerts.add(entry.alert());
        }
        return List.copyOf(alerts);
    }

    @Override
    public Optional<Alert> findWatching(String userId, String courtId, LocalDate date, TimeSlot slot) {
        for (Entry entry : entries.values()) {
            Alert alert = entry.alert();
            if (alert.ownedBy(userId) && alert.watches(courtId, date, slot)) {
                return Optional.of(alert);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Alert> findWatchingByCourtDay(String courtId, LocalDate date) {
        List<Alert> watching = new ArrayList<>();
        for (Entry entry : entries.values()) {
            Alert alert = entry.alert();
            if (alert.status() == AlertStatus.WATCHING
                    && alert.courtId().equals(courtId)
                    && alert.date().equals(date)) {
                watching.add(alert);
            }
        }
        return List.copyOf(watching);
    }

    @Override
    public List<Alert> findAllWatching() {
        List<Alert> watching = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (entry.alert().status() == AlertStatus.WATCHING) {
                watching.add(entry.alert());
            }
        }
        return List.copyOf(watching);
    }

    @Override
    public boolean compareAndSet(Alert expected, Alert updated) {
        Entry current = entries.get(expected.alertId());
        if (current == null || !current.alert().equals(expected)) {
            return false;
        }
        return entries.replace(expected.alertId(), current, new Entry(current.sequence(), updated));
    }

    private static final Comparator<Entry> NEWEST_FIRST = Comparator
            .comparing((Entry entry) -> entry.alert().createdAt())
            .thenComparingLong(Entry::sequence)
            .reversed();

    /**
     * 저장된 신청 하나다.
     *
     * @param sequence 저장된 순서. 신청 시각이 같을 때의 앞뒤를 정한다
     */
    private record Entry(long sequence, Alert alert) {
    }
}
