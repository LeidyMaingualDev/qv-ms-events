package com.qvenly.qv_ms_events.model.dto.response.event;

import com.qvenly.qv_ms_events.model.enums.event.EventStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private String coverImageUrl;
    private String eventType;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private EventStatus status;
    private String cancelReason;
    private Long ownerUserId;
    private String ownerEmail;
    private PlanLimitsResponse planLimits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class PlanLimitsResponse {
        private String planName;
        private Integer maxEvents;
        private Integer maxOrganizers;
        private Integer maxParticipants;
        private Integer maxJudges;
        private Integer maxAttendees;
        private Integer maxStaff;
    }
}
