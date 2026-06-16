package com.qvenly.qv_ms_events.model.dto.response.event;

import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.MemberStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventMemberResponse {
    private Long id;
    private Long eventId;
    private Long userId;
    private String userEmail;
    private EventRole eventRole;
    private MemberStatus status;
    private String leaveReason;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
