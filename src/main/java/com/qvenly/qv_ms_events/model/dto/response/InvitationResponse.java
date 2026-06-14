package com.qvenly.qv_ms_events.model.dto.response;

import com.qvenly.qv_ms_events.model.enums.EventRole;
import com.qvenly.qv_ms_events.model.enums.InvitationStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InvitationResponse {
    private Long id;
    private Long eventId;
    private String invitedByEmail;
    private String invitedEmail;
    private EventRole eventRole;
    private InvitationStatus status;
    private String cancelReason;
    private String token;
    private LocalDateTime sentAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
}
