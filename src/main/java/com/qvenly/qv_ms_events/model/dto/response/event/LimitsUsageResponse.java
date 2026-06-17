package com.qvenly.qv_ms_events.model.dto.response.event;

import lombok.Data;

@Data
public class LimitsUsageResponse {

    private String planName;
    private RoleUsage organizers;
    private RoleUsage participants;
    private RoleUsage judges;
    private RoleUsage attendees;
    private RoleUsage staff;
    private RoleUsage members;

    @Data
    public static class RoleUsage {
        private int current;
        private int max;
        private int remaining;
        private boolean full;

        public RoleUsage(int current, int max) {
            this.current   = current;
            this.max       = max;
            this.remaining = Math.max(0, max - current);
            this.full      = current >= max;
        }
    }
}