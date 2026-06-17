package com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventByOrganizerResponseDTO {
    
    private Long organizerId;
    private String organizerEmail;
    private Long numberEvents;
}
