package com.thinking.tennis.api;

import com.thinking.tennis.app.AvailabilityResult;
import com.thinking.tennis.domain.AlertSnapshot;
import com.thinking.tennis.domain.Availability;
import com.thinking.tennis.domain.Delivery;
import com.thinking.tennis.domain.TimeSlot;

import java.util.List;

final class ApiMapper {
    private ApiMapper() {
    }

    static AlertResponse alert(AlertSnapshot alert) {
        return new AlertResponse(
                alert.alertId(),
                alert.courtId(),
                alert.courtName(),
                alert.reservationUrl(),
                alert.date(),
                slot(alert.slot()),
                alert.status(),
                alert.createdAt(),
                alert.expiresAt(),
                alert.lastCheckedAt(),
                alert.checkDelayed(),
                delivery(alert.delivery())
        );
    }

    static AvailabilityResponse availability(AvailabilityResult result) {
        Availability availability = result.availability();
        List<AvailabilitySlotResponse> slots = availability.slots().stream()
                .map(slot -> new AvailabilitySlotResponse(
                        slot.startTime().toString(),
                        slot.endTime().toString(),
                        slot.available()
                ))
                .toList();
        return new AvailabilityResponse(
                availability.courtId(),
                availability.courtName(),
                availability.date(),
                availability.confirmedAt(),
                result.stale(),
                result.staleReason(),
                availability.reservationUrl(),
                slots
        );
    }

    private static TimeSlotResponse slot(TimeSlot slot) {
        return new TimeSlotResponse(slot.startTime().toString(), slot.endTime().toString());
    }

    private static DeliveryResponse delivery(Delivery delivery) {
        if (delivery == null) {
            return null;
        }
        return new DeliveryResponse(
                delivery.status(),
                delivery.attemptCount(),
                delivery.lastAttemptAt(),
                delivery.failureReason()
        );
    }
}
