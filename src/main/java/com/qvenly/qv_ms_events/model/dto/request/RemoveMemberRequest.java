package com.qvenly.qv_ms_events.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RemoveMemberRequest {

    @NotBlank(message = "El motivo de eliminación es obligatorio")
    private String reason;
}
