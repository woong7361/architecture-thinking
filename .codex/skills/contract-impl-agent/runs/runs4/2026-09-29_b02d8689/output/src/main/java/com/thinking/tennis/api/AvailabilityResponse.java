package com.thinking.tennis.api;

import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(name = "CourtAvailability", requiredProperties = {
        "courtId", "courtName", "date", "confirmedAt", "stale",
        "staleReason", "reservationUrl", "slots"
})
public class AvailabilityResponse {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String courtId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    private final String courtName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
    private final String date;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
    private final String confirmedAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean stale;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private final String staleReason;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
    private final String reservationUrl;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<AvailabilitySlotResponse> slots;

    private AvailabilityResponse(
            String courtId,
            String courtName,
            String date,
            String confirmedAt,
            boolean stale,
            String staleReason,
            String reservationUrl,
            List<AvailabilitySlotResponse> slots) {
        this.courtId = courtId;
        this.courtName = courtName;
        this.date = date;
        this.confirmedAt = confirmedAt;
        this.stale = stale;
        this.staleReason = staleReason;
        this.reservationUrl = reservationUrl;
        this.slots = slots;
    }

    public static AvailabilityResponse from(AvailabilitySnapshot snapshot) {
        return new AvailabilityResponse(
                snapshot.court().courtId(),
                snapshot.court().courtName(),
                snapshot.date().toString(),
                snapshot.confirmedAt().toString(),
                snapshot.stale(),
                snapshot.staleReason() == null ? null : snapshot.staleReason().name(),
                snapshot.court().reservationUrl(),
                snapshot.slots().stream().map(AvailabilitySlotResponse::from).toList());
    }

    public String getCourtId() {
        return courtId;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getDate() {
        return date;
    }

    public String getConfirmedAt() {
        return confirmedAt;
    }

    public boolean isStale() {
        return stale;
    }

    public String getStaleReason() {
        return staleReason;
    }

    public String getReservationUrl() {
        return reservationUrl;
    }

    public List<AvailabilitySlotResponse> getSlots() {
        return slots;
    }

    @Schema(name = "AvailabilitySlot", requiredProperties = {"startTime", "endTime", "available"})
    public static class AvailabilitySlotResponse {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        private final String startTime;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        private final String endTime;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private final boolean available;

        private AvailabilitySlotResponse(String startTime, String endTime, boolean available) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = available;
        }

        public static AvailabilitySlotResponse from(CourtAvailabilityPort.SlotAvailability slot) {
            return new AvailabilitySlotResponse(
                    TIME_FORMAT.format(slot.startTime()),
                    TIME_FORMAT.format(slot.endTime()),
                    slot.available());
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public boolean isAvailable() {
            return available;
        }
    }
}
