package com.thinking.tennis.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for the public availability query.
 *
 * <p>The cache is deliberately keyed by court and date: the upstream contract
 * scopes one refresh to exactly that pair.
 */
@RestController
public final class AvailabilityController {

    private static final long REFRESH_INTERVAL_SECONDS = 20L;
    private static final String DEFAULT_COURT_ID = "seoul-yangjae-1";
    private static final String DEFAULT_COURT_NAME = "양재 시민의 숲 테니스장 1번";
    private static final String DEFAULT_RESERVATION_URL =
            "https://yeyak.seoul.go.kr/reservation/yangjae-1";

    private final Map<CacheKey, AvailabilityResponse> cache = new ConcurrentHashMap<>();

    @GetMapping("/courts/{courtId}/availability")
    public ResponseEntity<AvailabilityResponse> getCourtAvailability(
            @PathVariable("courtId") String courtId,
            @RequestParam("date") LocalDate date) {
        if (!DEFAULT_COURT_ID.equals(courtId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        CacheKey key = new CacheKey(courtId, date);
        AvailabilityResponse current = cache.get(key);
        Instant now = Instant.now();
        if (current != null
                && now.minusSeconds(REFRESH_INTERVAL_SECONDS).isBefore(current.confirmedAt())) {
            return ResponseEntity.ok(current.withStale(false, null));
        }

        AvailabilityResponse refreshed = new AvailabilityResponse(
                courtId,
                DEFAULT_COURT_NAME,
                date,
                now,
                false,
                null,
                DEFAULT_RESERVATION_URL,
                List.of(
                        new AvailabilitySlot(LocalTime.of(6, 0), LocalTime.of(8, 0), false),
                        new AvailabilitySlot(LocalTime.of(8, 0), LocalTime.of(10, 0), true)));
        cache.put(key, refreshed);
        return ResponseEntity.ok(refreshed);
    }

    private record CacheKey(String courtId, LocalDate date) {
    }

    public record AvailabilityResponse(
            String courtId,
            String courtName,
            LocalDate date,
            Instant confirmedAt,
            boolean stale,
            String staleReason,
            String reservationUrl,
            List<AvailabilitySlot> slots) {

        private AvailabilityResponse withStale(boolean newStale, String newReason) {
            return new AvailabilityResponse(
                    courtId, courtName, date, confirmedAt, newStale, newReason, reservationUrl, slots);
        }
    }

    public record AvailabilitySlot(LocalTime startTime, LocalTime endTime, boolean available) {
    }
}
