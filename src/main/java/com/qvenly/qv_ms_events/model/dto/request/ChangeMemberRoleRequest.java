package com.qvenly.qv_ms_events.model.dto.request;

import com.qvenly.qv_ms_events.model.enums.EventRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeMemberRoleRequest {

    @NotNull(message = "El nuevo rol es obligatorio")
    private EventRole newRole;
}
