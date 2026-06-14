package com.qvenly.qv_ms_events.repository;

import com.qvenly.qv_ms_events.model.entity.EventAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, Long> {

    List<EventAuditLog> findByEventIdOrderByPerformedAtDesc(Long eventId);
}
