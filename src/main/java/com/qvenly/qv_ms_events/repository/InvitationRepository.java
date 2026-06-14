package com.qvenly.qv_ms_events.repository;

import com.qvenly.qv_ms_events.model.entity.Invitation;
import com.qvenly.qv_ms_events.model.enums.EventRole;
import com.qvenly.qv_ms_events.model.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByEventIdOrderBySentAtDesc(Long eventId);

    Optional<Invitation> findByToken(String token);

    boolean existsByEventIdAndInvitedEmailAndEventRoleAndStatus(
            Long eventId, String invitedEmail, EventRole role, InvitationStatus status);

    List<Invitation> findByInvitedEmailAndStatus(String invitedEmail, InvitationStatus status);

    List<Invitation> findByEventIdAndStatus(Long eventId, InvitationStatus status);
}
