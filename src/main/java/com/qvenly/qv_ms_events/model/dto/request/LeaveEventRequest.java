package com.qvenly.qv_ms_events.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveEventRequest {

    @NotBlank(message = "La justificación es obligatoria para abandonar el evento")
    private String reason;
}
