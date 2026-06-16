package com.qvenly.qv_ms_events.model.dto.request.event;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelInvitationRequest {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String cancelReason;
}
