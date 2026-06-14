package com.qvenly.qv_ms_events.client.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserPlanResponseDTO {

    private Long idUserPlan;
    private Long userId;
    private PlanDTO plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean renewal;
    private LocalDateTime createdAt;

    public boolean isActive() {
        return "active".equalsIgnoreCase(status)
                && endDate != null
                && !endDate.isBefore(LocalDate.now());
    }

    @Data
    public static class PlanDTO {
        private Integer idPlan;
        private String name;
        private Integer maxEvents;
        private Integer maxOrganizers;
        private Integer maxParticipants;
        private Integer maxJudges;
        private Integer maxAttendees;
        private Integer maxStaff;
    }
}
