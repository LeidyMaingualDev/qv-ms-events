package com.qvenly.qv_ms_events.repository.event;

import com.qvenly.qv_ms_events.model.entity.event.EventPlanSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventPlanSnapshotRepository extends JpaRepository<EventPlanSnapshot, Long> {

    Optional<EventPlanSnapshot> findByEventId(Long eventId);
}
