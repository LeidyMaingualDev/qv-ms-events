package com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUserDetailResponseDTO {
    
    private Long eventId;
    private String eventName;
    private Long staff;
    private Long assistants;
    private Long judges;
    private Long participants;
}
