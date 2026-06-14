package com.qvenly.qv_ms_events.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "event_plan_snapshot")
public class EventPlanSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "max_events", nullable = false)
    private Integer maxEvents = 0;

    @Column(name = "max_organizers", nullable = false)
    private Integer maxOrganizers = 0;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 0;

    @Column(name = "max_judges", nullable = false)
    private Integer maxJudges = 0;

    @Column(name = "max_attendees", nullable = false)
    private Integer maxAttendees = 0;

    @Column(name = "max_staff", nullable = false)
    private Integer maxStaff = 0;

    @Column(name = "captured_at", updatable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }
}
