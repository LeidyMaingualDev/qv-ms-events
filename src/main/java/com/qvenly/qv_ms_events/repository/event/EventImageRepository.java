package com.qvenly.qv_ms_events.repository.event;

import com.qvenly.qv_ms_events.model.entity.event.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {

    List<EventImage> findByEventId(Long eventId);

    void deleteByIdAndEventId(Long id, Long eventId);

    EventImage findByEventIdAndIsCoverTrue(Long eventId);
}