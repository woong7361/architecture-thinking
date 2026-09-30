package com.thinking.tennis.api;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Schema(name = "Alert", requiredProperties = {
        "alertId", "courtId", "courtName", "reservationUrl", "date", "slot",
        "status", "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
})
public class AlertResponse {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter INSTANT_FORMAT = DateTimeFormatter.ISO_INSTANT;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    private final String alertId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String courtId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    private final String courtName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
    private final String reservationUrl;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
    private final String date;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final TimeSlotResponse slot;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
    private final String createdAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
    private final String expiresAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "date-time")
    private final String lastCheckedAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean checkDelayed;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private final DeliveryResponse delivery;

    private AlertResponse(
            String alertId,
            String courtId,
            String courtName,
            String reservationUrl,
            String date,
            TimeSlotResponse slot,
            String status,
            String createdAt,
            String expiresAt,
            String lastCheckedAt,
            boolean checkDelayed,
            DeliveryResponse delivery) {
        this.alertId = alertId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.reservationUrl = reservationUrl;
        this.date = date;
        this.slot = slot;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastCheckedAt = lastCheckedAt;
        this.checkDelayed = checkDelayed;
        this.delivery = delivery;
    }

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.alertId().toString(),
                alert.courtId(),
                alert.courtName(),
                alert.reservationUrl(),
                alert.date().toString(),
                new TimeSlotResponse(
                        TIME_FORMAT.format(alert.startTime()),
                        TIME_FORMAT.format(alert.endTime())),
                alert.status().name(),
                INSTANT_FORMAT.format(alert.createdAt()),
                INSTANT_FORMAT.format(alert.expiresAt()),
                alert.lastCheckedAt() == null ? null : INSTANT_FORMAT.format(alert.lastCheckedAt()),
                alert.checkDelayed(),
                DeliveryResponse.from(alert.delivery()));
    }

    public String getAlertId() {
        return alertId;
    }

    public String getCourtId() {
        return courtId;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getReservationUrl() {
        return reservationUrl;
    }

    public String getDate() {
        return date;
    }

    public TimeSlotResponse getSlot() {
        return slot;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getLastCheckedAt() {
        return lastCheckedAt;
    }

    public boolean isCheckDelayed() {
        return checkDelayed;
    }

    public DeliveryResponse getDelivery() {
        return delivery;
    }

    @Schema(name = "TimeSlot", requiredProperties = {"startTime", "endTime"})
    public static class TimeSlotResponse {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        private final String startTime;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        private final String endTime;

        public TimeSlotResponse(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }
    }

    @Schema(name = "AlertDelivery", requiredProperties = {
            "status", "attemptCount", "lastAttemptAt", "failureReason"
    })
    public static class DeliveryResponse {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private final String status;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "4")
        private final int attemptCount;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "date-time")
        private final String lastAttemptAt;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        private final String failureReason;

        private DeliveryResponse(
                String status,
                int attemptCount,
                String lastAttemptAt,
                String failureReason) {
            this.status = status;
            this.attemptCount = attemptCount;
            this.lastAttemptAt = lastAttemptAt;
            this.failureReason = failureReason;
        }

        public static DeliveryResponse from(AlertDelivery delivery) {
            if (delivery == null) {
                return null;
            }
            return new DeliveryResponse(
                    delivery.status().name(),
                    delivery.attemptCount(),
                    delivery.lastAttemptAt() == null ? null : INSTANT_FORMAT.format(delivery.lastAttemptAt()),
                    delivery.failureReason());
        }

        public String getStatus() {
            return status;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public String getLastAttemptAt() {
            return lastAttemptAt;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }
}
