package com.thinking.tennis.adapter;

import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link CourtAvailabilityPort} 의 인메모리 구현이다.
 *
 * <p><b>이 어댑터는 사람이 소유한다. 생성 코드는 이 파일을 고치지 않고 포트만 호출한다.</b>
 *
 * <p>외부 HTTP를 부르지 않는다. 지원 코트와 시간대를 코드 안의 고정 표로 두는 이유는 게이트가 iteration마다
 * 부팅해야 하고, 계약을 지켰는지 판정하는 데 실제 예약처가 필요하지 않기 때문이다. 네트워크를 쓰면 판정이
 * 예약처의 상태에 따라 흔들린다.
 *
 * <p>고정 표는 {@code putCourt}·{@code setSlots}·{@code setNextCheckFailure}·{@code setClock} 으로 바꿀 수
 * 있다. 테스트가 빈자리 유무나 확인 실패를 만들어야 하고, 그러려고 프로덕션 코드를 고치게 하면 안 된다.
 * 세터를 둔 것은 테스트를 위한 것이므로 유스케이스는 세터를 부르지 않는다.
 */
@Component
public class InMemoryCourtAvailabilityAdapter implements CourtAvailabilityPort {

    /** 지원 코트의 기본 표. 계약의 예시에 나오는 코트를 그대로 쓴다. */
    private static final List<SupportedCourt> DEFAULT_COURTS = List.of(
            new SupportedCourt("seoul-yangjae-1", "양재 시민의 숲 테니스장 1번",
                    "https://yeyak.seoul.go.kr/reservation/yangjae-1"),
            new SupportedCourt("seoul-yangjae-2", "양재 시민의 숲 테니스장 2번",
                    "https://yeyak.seoul.go.kr/reservation/yangjae-2"),
            new SupportedCourt("seoul-jangchung-1", "장충 테니스장 1번",
                    "https://yeyak.seoul.go.kr/reservation/jangchung-1"));

    /**
     * 코트가 운영하는 기본 시간대. 두 시간 단위로 06시부터 22시까지다.
     * 어느 시간대가 비어 있는지는 {@link #defaultAvailability(String, LocalDate, LocalTime)} 이 정한다.
     */
    private static final List<LocalTime> DEFAULT_SLOT_STARTS = List.of(
            LocalTime.of(6, 0), LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0),
            LocalTime.of(14, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(20, 0));

    private static final int SLOT_HOURS = 2;

    private final Map<String, SupportedCourt> courts = new LinkedHashMap<>();

    /** 코트·날짜마다 덮어쓴 시간대 표. 비어 있으면 기본 표를 쓴다. */
    private final Map<String, List<SlotAvailability>> overriddenSlots = new ConcurrentHashMap<>();

    /** null이 아니면 다음 확인이 이 이유로 실패한다. */
    private volatile UpstreamFailureReason nextCheckFailure;

    private volatile Clock clock = Clock.systemUTC();

    public InMemoryCourtAvailabilityAdapter() {
        reset();
    }

    @Override
    public Optional<SupportedCourt> findCourt(String courtId) {
        if (courtId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(courts.get(courtId));
    }

    @Override
    public CourtAvailabilityCheck checkAvailability(String courtId, LocalDate date)
            throws CheckFailed, CourtNotSupportedException {
        SupportedCourt court = findCourt(courtId)
                .orElseThrow(() -> new CourtNotSupportedException(courtId));

        /* 실패는 한 번만 일으킨다. 테스트가 실패 뒤의 복구까지 한 시나리오에서 보게 하려는 것이다. */
        UpstreamFailureReason failure = nextCheckFailure;
        if (failure != null) {
            nextCheckFailure = null;
            throw new CheckFailed(failure);
        }

        List<SlotAvailability> slots = overriddenSlots.get(key(courtId, date));
        if (slots == null) {
            slots = defaultSlots(courtId, date);
        }
        return new CourtAvailabilityCheck(court, date, Instant.now(clock), List.copyOf(slots));
    }

    /** 지원 코트를 더하거나 같은 식별자의 코트를 덮어쓴다. */
    public void putCourt(SupportedCourt court) {
        courts.put(court.courtId(), court);
    }

    /** 코트를 지원 목록에서 뺀다. 지원하지 않는 코트를 물었을 때의 경로를 만들 때 쓴다. */
    public void removeCourt(String courtId) {
        courts.remove(courtId);
    }

    /** 코트·날짜의 시간대 표를 덮어쓴다. 빈 목록을 주면 예약처가 그 날짜를 열지 않은 상태가 된다. */
    public void setSlots(String courtId, LocalDate date, List<SlotAvailability> slots) {
        overriddenSlots.put(key(courtId, date), List.copyOf(slots));
    }

    /** 코트·날짜의 시간대 표를 기본값으로 되돌린다. */
    public void clearSlots(String courtId, LocalDate date) {
        overriddenSlots.remove(key(courtId, date));
    }

    /** 다음 확인 한 번을 이 이유로 실패시킨다. null을 주면 예약된 실패를 취소한다. */
    public void setNextCheckFailure(UpstreamFailureReason reason) {
        this.nextCheckFailure = reason;
    }

    /** 확인 시각을 고정한다. 확인 간격과 신선도를 다루는 테스트가 시간을 손에 쥐게 한다. */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /** 표 전체를 처음 상태로 되돌린다. 테스트 사이에 상태가 새지 않게 한다. */
    public final void reset() {
        courts.clear();
        DEFAULT_COURTS.forEach(court -> courts.put(court.courtId(), court));
        overriddenSlots.clear();
        nextCheckFailure = null;
        clock = Clock.systemUTC();
    }

    private List<SlotAvailability> defaultSlots(String courtId, LocalDate date) {
        List<SlotAvailability> slots = new ArrayList<>(DEFAULT_SLOT_STARTS.size());
        for (LocalTime start : DEFAULT_SLOT_STARTS) {
            slots.add(new SlotAvailability(start, start.plusHours(SLOT_HOURS),
                    defaultAvailability(courtId, date, start)));
        }
        return slots;
    }

    /*
     * 기본 빈자리 여부를 코트·날짜·시작 시각에서 계산한다. 난수를 쓰지 않는 이유는 같은 입력에 같은 답이
     * 나와야 게이트와 테스트가 판정 장치로 쓸 수 있기 때문이다.
     */
    private boolean defaultAvailability(String courtId, LocalDate date, LocalTime start) {
        int seed = courtId.hashCode() + date.getDayOfYear() + start.getHour();
        return Math.floorMod(seed, 3) == 0;
    }

    private static String key(String courtId, LocalDate date) {
        return courtId + "@" + date;
    }
}
