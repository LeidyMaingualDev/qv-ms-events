package com.qvenly.qv_ms_events.model.dto.response.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventImageResponse {
    private Long id;
    private Long eventId;
    private String imageUrl;
    private LocalDateTime uploadedAt;
    private Boolean isCover;
}