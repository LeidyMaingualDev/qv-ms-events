package com.qvenly.qv_ms_events.model.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UpdateEventRequest {
    private String title;
    private String description;
    private String location;
    private String eventType;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
