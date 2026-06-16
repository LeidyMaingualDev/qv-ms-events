package com.qvenly.qv_ms_events.model.entity.event;

import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.InvitationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "invitation")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "invited_by_email", nullable = false, length = 100)
    private String invitedByEmail;

    @Column(name = "invited_email", nullable = false, length = 100)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_role", nullable = false, length = 20)
    private EventRole eventRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = sentAt.plusDays(7);
        }
    }
}
