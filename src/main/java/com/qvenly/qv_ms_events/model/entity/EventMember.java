package com.qvenly.qv_ms_events.model.entity;

import com.qvenly.qv_ms_events.model.enums.EventRole;
import com.qvenly.qv_ms_events.model.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
    name = "event_member",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_member_role",
        columnNames = {"event_id", "user_email", "event_role"}
    )
)
public class EventMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_role", nullable = false, length = 20)
    private EventRole eventRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "leave_reason", columnDefinition = "TEXT")
    private String leaveReason;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
