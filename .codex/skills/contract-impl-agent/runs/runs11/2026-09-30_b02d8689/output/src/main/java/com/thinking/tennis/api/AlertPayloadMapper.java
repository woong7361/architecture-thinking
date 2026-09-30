package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.Alert;
import com.thinking.tennis.api.dto.AlertDelivery;
import com.thinking.tennis.api.dto.AlertList;
import com.thinking.tennis.api.dto.AlertStatus;
import com.thinking.tennis.api.dto.AvailabilitySlot;
import com.thinking.tennis.api.dto.CourtAvailability;
import com.thinking.tennis.api.dto.DeliveryStatus;
import com.thinking.tennis.api.dto.TimeSlot;
import com.thinking.tennis.api.dto.UpstreamFailureReason;
import com.thinking.tennis.app.AlertView;
import com.thinking.tennis.app.AvailabilityView;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import com.thinking.tennis.port.CourtAvailabilityPort.SlotAvailability;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 안쪽의 값을 계약이 적은 응답 모양으로 옮긴다.
 *
 * <p>시각을 {@code HH:mm} 으로 적는 자리가 이곳이다. 계약은 시간대의 시작과 끝을 분 단위까지만 적은
 * 문자열로 두었으므로, 초가 붙은 값이 나가지 않도록 형식을 여기서 고정한다.
 */
@Component
public class AlertPayloadMapper {

    private static final DateTimeFormatter HOUR_AND_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    public Alert toAlert(AlertView view) {
        com.thinking.tennis.domain.Alert alert = view.alert();
        return new Alert(
                alert.alertId(),
                alert.court().courtId(),
                alert.court().courtName(),
                alert.court().reservationUrl(),
                alert.date(),
                toTimeSlot(alert.slot()),
                AlertStatus.of(alert.status()),
                alert.createdAt(),
                alert.expiresAt(),
                view.lastCheckedAt(),
                view.checkDelayed(),
                toDelivery(alert.delivery()));
    }

    public AlertList toAlertList(List<AlertView> views) {
        return new AlertList(views.stream().map(this::toAlert).toList());
    }

    public CourtAvailability toCourtAvailability(AvailabilityView view) {
        CourtAvailabilityCheck check = view.check();
        return new CourtAvailability(
                check.court().courtId(),
                check.court().courtName(),
                check.date(),
                check.confirmedAt(),
                view.stale(),
                UpstreamFailureReason.of(view.staleReason()),
                check.court().reservationUrl(),
                check.slots().stream().map(AlertPayloadMapper::toAvailabilitySlot).toList());
    }

    private static AvailabilitySlot toAvailabilitySlot(SlotAvailability slot) {
        return new AvailabilitySlot(format(slot.startTime()), format(slot.endTime()), slot.available());
    }

    private static TimeSlot toTimeSlot(com.thinking.tennis.domain.TimeSlot slot) {
        return new TimeSlot(format(slot.startTime()), format(slot.endTime()));
    }

    private static AlertDelivery toDelivery(com.thinking.tennis.domain.AlertDelivery delivery) {
        if (delivery == null) {
            return null;
        }
        return new AlertDelivery(
                DeliveryStatus.of(delivery.status()),
                delivery.attemptCount(),
                delivery.lastAttemptAt(),
                delivery.failureReason());
    }

    private static String format(LocalTime time) {
        return HOUR_AND_MINUTE.format(time);
    }
}
