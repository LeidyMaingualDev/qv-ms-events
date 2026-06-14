package com.qvenly.qv_ms_events.model.dto.response;

import com.qvenly.qv_ms_events.model.enums.AuditActionType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private Long eventId;
    private AuditActionType actionType;
    private String performedByEmail;
    private String performedByRole;
    private String changeDetail;
    private LocalDateTime performedAt;
}
