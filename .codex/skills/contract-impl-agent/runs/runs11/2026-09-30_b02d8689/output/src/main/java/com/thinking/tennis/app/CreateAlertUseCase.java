package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.Court;
import com.thinking.tennis.domain.TimeSlot;
import com.thinking.tennis.port.CourtAvailabilityPort.SlotAvailability;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 코트 하나, 날짜 하나, 시간대 하나로 알림을 신청한다. 신청은 감시 중으로 시작한다.
 *
 * <p>계약이 거절하라고 한 셋을 코트, 시간대, 만료 순으로 본다. 코트를 모르면 그 코트의 운영 시간대를
 * 물을 수 없고, 운영하지 않는 시간대에는 이용 시작 시각도 없어서 이 순서가 앞의 판정 없이는 뒤의
 * 판정을 할 수 없는 순서다.
 *
 * <p>같은 조건을 감시 중인 신청이 이미 있으면 새 신청을 만들지 않고 그 신청을 돌려준다. 멱등 키가 달라도
 * 그렇다. 이미 끝난 신청과 같은 조건이면 새 신청을 만든다. 끝난 신청은 되살아나지 않지만 같은 조건을
 * 다시 감시할 수는 있다.
 */
@Service
public class CreateAlertUseCase {

    /** 신청 조건이다. 시각과 날짜는 이미 파싱된 값으로 받는다. */
    public record Command(String userId, String courtId, LocalDate date, TimeSlot slot) {
    }

    /**
     * @param alert   신청 하나
     * @param created 이번 요청이 신청을 만들었는지 여부. 계약이 201과 200을 이것으로 가른다
     */
    public record Result(AlertView alert, boolean created) {
    }

    private final GetCourtAvailabilityUseCase availability;
    private final AlertStore alerts;
    private final AlertViewFactory views;
    private final Clock clock;

    public CreateAlertUseCase(GetCourtAvailabilityUseCase availability,
                              AlertStore alerts,
                              AlertViewFactory views,
                              Clock clock) {
        this.availability = availability;
        this.alerts = alerts;
        this.views = views;
        this.clock = clock;
    }

    public Result create(Command command) {
        Court court = availability.supportedCourt(command.courtId(), AlertOperationFailure.Origin.BODY);
        requireOperatingSlot(command);

        Instant expiresAt = startOfSlot(command.date(), command.slot());
        Instant now = clock.instant();
        if (!expiresAt.isAfter(now)) {
            throw new AlertOperationFailure.WindowClosed(expiresAt);
        }

        /*
         * 조건이 같은 두 요청이 동시에 들어와도 신청은 하나여야 한다. 찾기와 만들기가 갈라져 있으면
         * 둘 다 못 찾고 둘 다 만든다.
         */
        synchronized (alerts.conditionLock(command.userId(), command.courtId(), command.date(), command.slot())) {
            Optional<Alert> watching = alerts.findWatchingCondition(
                    command.userId(), command.courtId(), command.date(), command.slot());
            if (watching.isPresent()) {
                return new Result(views.of(watching.get()), false);
            }

            Alert alert = new Alert(UUID.randomUUID(), command.userId(), court, command.date(),
                    command.slot(), AlertStatus.WATCHING, now, expiresAt, null, alerts.nextSequence());
            alerts.save(alert);
            return new Result(views.of(alert), true);
        }
    }

    /**
     * 그 코트가 그 날짜에 운영하는 시간대인지 본다. 운영 시간대는 확인 결과에 실린 것이므로 저장된 결과가
     * 없으면 이 판정이 확인을 일으킨다.
     */
    private void requireOperatingSlot(Command command) {
        List<SlotAvailability> operating = operatingSlots(command);
        boolean supported = operating.stream().anyMatch(slot ->
                slot.startTime().equals(command.slot().startTime())
                        && slot.endTime().equals(command.slot().endTime()));
        if (!supported) {
            throw new AlertOperationFailure.UnsupportedSlot(describeOperating(command, operating));
        }
    }

    private List<SlotAvailability> operatingSlots(Command command) {
        try {
            return availability.availability(command.courtId(), command.date()).check().slots();
        } catch (AlertOperationFailure.UnsupportedCourt unsupported) {
            /* 이 오퍼레이션은 코트를 본문에서 받았으므로 경로에서 받은 실패로 나가면 안 된다. */
            throw new AlertOperationFailure.UnsupportedCourt(
                    command.courtId(), AlertOperationFailure.Origin.BODY);
        } catch (AlertOperationFailure.AvailabilityUnavailable unavailable) {
            throw new AlertOperationFailure.AvailabilityUndetermined(unavailable.reason());
        }
    }

    /** 만료 시각은 이용 시작 시각이다. 코트가 있는 지역의 날짜와 시각을 그 지역 시간대로 붙인다. */
    private static Instant startOfSlot(LocalDate date, TimeSlot slot) {
        return ZonedDateTime.of(date, slot.startTime(), AvailabilityPolicy.COURT_ZONE).toInstant();
    }

    private static String describeOperating(Command command, List<SlotAvailability> operating) {
        if (operating.isEmpty()) {
            return command.courtId() + "은 " + command.date() + "에 운영하는 시간대가 없습니다";
        }
        StringBuilder units = new StringBuilder();
        for (SlotAvailability slot : operating) {
            if (units.length() > 0) {
                units.append(", ");
            }
            units.append(slot.startTime()).append("~").append(slot.endTime());
        }
        return command.courtId() + "은 " + command.date() + "에 " + units + " 단위로 운영합니다";
    }
}
