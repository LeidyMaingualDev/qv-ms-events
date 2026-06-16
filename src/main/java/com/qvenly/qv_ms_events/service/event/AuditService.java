package com.qvenly.qv_ms_events.service.event;

import com.qvenly.qv_ms_events.model.dto.response.event.AuditLogResponse;
import com.qvenly.qv_ms_events.model.entity.event.EventAuditLog;
import com.qvenly.qv_ms_events.model.enums.event.AuditActionType;
import com.qvenly.qv_ms_events.repository.event.EventAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final EventAuditLogRepository repository;

    public void log(Long eventId, AuditActionType action,
                    String performedByEmail, String performedByRole, String detail) {
        EventAuditLog entry = new EventAuditLog();
        entry.setEventId(eventId);
        entry.setActionType(action);
        entry.setPerformedByEmail(performedByEmail);
        entry.setPerformedByRole(performedByRole);
        entry.setChangeDetail(detail);
        repository.save(entry);
    }

    public List<AuditLogResponse> getAuditLog(Long eventId) {
        return repository.findByEventIdOrderByPerformedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(EventAuditLog e) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(e.getId());
        r.setEventId(e.getEventId());
        r.setActionType(e.getActionType());
        r.setPerformedByEmail(e.getPerformedByEmail());
        r.setPerformedByRole(e.getPerformedByRole());
        r.setChangeDetail(e.getChangeDetail());
        r.setPerformedAt(e.getPerformedAt());
        return r;
    }
}
