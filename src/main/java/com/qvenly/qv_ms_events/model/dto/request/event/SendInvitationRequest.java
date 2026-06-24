package com.qvenly.qv_ms_events.model.dto.request.event;

import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SendInvitationRequest {

    @NotBlank(message = "El email del invitado es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String invitedEmail;

    /**
     * Rol con el que se invita: ORGANIZER, STAFF o MEMBER.
     * Si es null, se asume MEMBER (caso por defecto / invitación genérica).
     * Los sub-roles de actividad (JUDGE, PARTICIPANT, ATTENDEE) NO son válidos aquí.
     */
    private EventRole eventRole;

    /** Fecha límite de aceptación. Si es null, se usa un valor por defecto (7 días). */
    private LocalDateTime expiresAt;
}