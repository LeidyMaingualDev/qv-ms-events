package com.qvenly.qv_ms_events.model.dto.request.event;

import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeMemberRoleRequest {

    @NotNull(message = "El nuevo rol es obligatorio")
    private EventRole newRole;
}
