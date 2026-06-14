package com.qvenly.qv_ms_events.model.dto.request;

import com.qvenly.qv_ms_events.model.enums.EventRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendInvitationRequest {

    @NotBlank(message = "El email del invitado es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String invitedEmail;

    @NotNull(message = "El rol del invitado es obligatorio")
    private EventRole eventRole;
}
