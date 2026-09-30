package com.thinking.tennis.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
public class AlertRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64,
            pattern = "^[a-z0-9][a-z0-9-]*$")
    private String courtId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
    private String date;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private TimeSlotRequest slot;

    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public TimeSlotRequest getSlot() {
        return slot;
    }

    public void setSlot(TimeSlotRequest slot) {
        this.slot = slot;
    }

    @Schema(name = "TimeSlot", requiredProperties = {"startTime", "endTime"})
    public static class TimeSlotRequest {

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        private String startTime;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
        private String endTime;

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
