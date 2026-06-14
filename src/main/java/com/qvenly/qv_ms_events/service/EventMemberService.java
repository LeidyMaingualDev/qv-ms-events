package com.qvenly.qv_ms_events.service;

import com.qvenly.qv_ms_events.client.NotificationClient;
import com.qvenly.qv_ms_events.exception.BusinessException;
import com.qvenly.qv_ms_events.model.dto.request.ChangeMemberRoleRequest;
import com.qvenly.qv_ms_events.model.dto.request.LeaveEventRequest;
import com.qvenly.qv_ms_events.model.dto.request.RemoveMemberRequest;
import com.qvenly.qv_ms_events.model.dto.response.EventMemberResponse;
import com.qvenly.qv_ms_events.model.dto.response.LimitsUsageResponse;
import com.qvenly.qv_ms_events.model.entity.EventMember;
import com.qvenly.qv_ms_events.model.entity.EventPlanSnapshot;
import com.qvenly.qv_ms_events.model.enums.AuditActionType;
import com.qvenly.qv_ms_events.model.enums.EventRole;
import com.qvenly.qv_ms_events.model.enums.MemberStatus;
import com.qvenly.qv_ms_events.repository.EventMemberRepository;
import com.qvenly.qv_ms_events.repository.EventPlanSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventMemberService {

    private final EventMemberRepository       memberRepository;
    private final EventPlanSnapshotRepository snapshotRepository;
    private final EventService                eventService;
    private final AuditService                auditService;
    private final NotificationClient notificationClient;

    public List<EventMemberResponse> getActiveMembers(Long eventId) {
        return memberRepository.findByEventIdAndStatus(eventId, MemberStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    public List<EventMemberResponse> getMembersByRole(Long eventId, EventRole role) {
        return memberRepository.findByEventIdAndEventRoleAndStatus(eventId, role, MemberStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventMemberResponse changeMemberRole(Long eventId, Long memberId,
                                                 ChangeMemberRoleRequest req,
                                                 String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);
        EventMember member = findActiveMember(memberId, eventId);

        if (member.getEventRole() == EventRole.ORGANIZER) {
            long count = memberRepository.countActiveByEventAndRole(eventId, EventRole.ORGANIZER);
            if (count <= 1) {
                throw new BusinessException("No se puede cambiar el rol del único organizador.", HttpStatus.CONFLICT);
            }
        }
        validateRoleLimit(eventId, req.getNewRole());
        if (member.getEventRole() == req.getNewRole()) {
            throw new BusinessException("El miembro ya tiene el rol " + req.getNewRole(), HttpStatus.CONFLICT);
        }
        String prevRole = member.getEventRole().name();
        member.setEventRole(req.getNewRole());
        EventMember updated = memberRepository.save(member);
        auditService.log(eventId, AuditActionType.MEMBER_ROLE_CHANGED, performerEmail,
                eventService.resolveRole(eventId, performerEmail, systemRole),
                String.format("Miembro %s: %s → %s", member.getUserEmail(), prevRole, req.getNewRole()));
        notificationClient.sendEventNotification(
                "member-role-changed",
                eventService.findEventById(eventId).getTitle(),
                eventId,
                List.of(new NotificationClient.Recipient(null, member.getUserEmail(), null)),
                req.getNewRole().name()
        );
        return toResponse(updated);
    }

    @Transactional
    public void removeMember(Long eventId, Long memberId, RemoveMemberRequest req,
                              String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);
        EventMember member = findActiveMember(memberId, eventId);
        if (member.getEventRole() == EventRole.ORGANIZER) {
            long count = memberRepository.countActiveByEventAndRole(eventId, EventRole.ORGANIZER);
            if (count <= 1) {
                throw new BusinessException("No se puede eliminar al único organizador.", HttpStatus.CONFLICT);
            }
        }
        member.setStatus(MemberStatus.REMOVED);
        member.setLeaveReason(req.getReason());
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);
        auditService.log(eventId, AuditActionType.MEMBER_REMOVED, performerEmail,
                eventService.resolveRole(eventId, performerEmail, systemRole),
                String.format("Miembro %s (%s) eliminado. Motivo: %s",
                        member.getUserEmail(), member.getEventRole(), req.getReason()));
        notificationClient.sendEventNotification(
                "member-removed",
                eventService.findEventById(eventId).getTitle(),
                eventId,
                List.of(new NotificationClient.Recipient(null, member.getUserEmail(), null)),
                req.getReason()
        );
    }

    @Transactional
    public void leaveEvent(Long eventId, LeaveEventRequest req, String userEmail) {
        EventMember member = memberRepository.findByEventIdAndStatus(eventId, MemberStatus.ACTIVE)
                .stream().filter(m -> m.getUserEmail().equalsIgnoreCase(userEmail)).findFirst()
                .orElseThrow(() -> new BusinessException("No eres miembro activo de este evento.", HttpStatus.FORBIDDEN));

        if (member.getEventRole() == EventRole.ORGANIZER) {
            long count = memberRepository.countActiveByEventAndRole(eventId, EventRole.ORGANIZER);
            if (count <= 1) {
                throw new BusinessException(
                        "No puedes abandonar el evento siendo el único organizador.", HttpStatus.CONFLICT);
            }
        }
        member.setStatus(MemberStatus.LEFT);
        member.setLeaveReason(req.getReason());
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);
        auditService.log(eventId, AuditActionType.MEMBER_LEFT, userEmail,
                member.getEventRole().name(), "Abandono voluntario. Motivo: " + req.getReason());
    }

    public LimitsUsageResponse getLimitsUsage(Long eventId) {
        EventPlanSnapshot snap = snapshotRepository.findByEventId(eventId)
                .orElseThrow(() -> new EntityNotFoundException("No hay snapshot de plan para el evento: " + eventId));
        LimitsUsageResponse r = new LimitsUsageResponse();
        r.setPlanName(snap.getPlanName());
        r.setOrganizers(new LimitsUsageResponse.RoleUsage(
                (int) memberRepository.countActiveByEventAndRole(eventId, EventRole.ORGANIZER), snap.getMaxOrganizers()));
        r.setParticipants(new LimitsUsageResponse.RoleUsage(
                (int) memberRepository.countActiveByEventAndRole(eventId, EventRole.PARTICIPANT), snap.getMaxParticipants()));
        r.setJudges(new LimitsUsageResponse.RoleUsage(
                (int) memberRepository.countActiveByEventAndRole(eventId, EventRole.JUDGE), snap.getMaxJudges()));
        r.setAttendees(new LimitsUsageResponse.RoleUsage(
                (int) memberRepository.countActiveByEventAndRole(eventId, EventRole.ATTENDEE), snap.getMaxAttendees()));
        r.setStaff(new LimitsUsageResponse.RoleUsage(
                (int) memberRepository.countActiveByEventAndRole(eventId, EventRole.STAFF), snap.getMaxStaff()));
        return r;
    }

    public void validateRoleLimit(Long eventId, EventRole role) {
        EventPlanSnapshot snap = snapshotRepository.findByEventId(eventId)
                .orElseThrow(() -> new EntityNotFoundException("No hay snapshot de plan para el evento: " + eventId));
        long current = memberRepository.countActiveByEventAndRole(eventId, role);
        int max = switch (role) {
            case ORGANIZER   -> snap.getMaxOrganizers();
            case PARTICIPANT -> snap.getMaxParticipants();
            case JUDGE       -> snap.getMaxJudges();
            case ATTENDEE    -> snap.getMaxAttendees();
            case STAFF       -> snap.getMaxStaff();
        };
        if (current >= max) {
            throw new BusinessException(
                    String.format("Se alcanzó el límite de %d %s(s) para este evento.", max, role.name().toLowerCase()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Transactional
    public EventMember addMember(Long eventId, Long userId, String userEmail, EventRole role) {
        if (memberRepository.findByEventIdAndUserEmailAndEventRole(eventId, userEmail, role).isPresent()) {
            throw new BusinessException("El usuario ya es miembro con el rol " + role, HttpStatus.CONFLICT);
        }
        EventMember m = new EventMember();
        m.setEventId(eventId); m.setUserId(userId); m.setUserEmail(userEmail);
        m.setEventRole(role); m.setStatus(MemberStatus.ACTIVE);
        EventMember saved = memberRepository.save(m);
        auditService.log(eventId, AuditActionType.MEMBER_ADDED, userEmail, role.name(),
                String.format("Miembro %s agregado con rol %s.", userEmail, role));
        return saved;
    }

    private EventMember findActiveMember(Long memberId, Long eventId) {
        EventMember m = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Miembro no encontrado: " + memberId));
        if (!m.getEventId().equals(eventId)) {
            throw new BusinessException("El miembro no pertenece a este evento.", HttpStatus.BAD_REQUEST);
        }
        if (m.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException("El miembro ya no está activo.", HttpStatus.CONFLICT);
        }
        return m;
    }

    public EventMemberResponse toResponse(EventMember m) {
        EventMemberResponse r = new EventMemberResponse();
        r.setId(m.getId()); r.setEventId(m.getEventId()); r.setUserId(m.getUserId());
        r.setUserEmail(m.getUserEmail()); r.setEventRole(m.getEventRole()); r.setStatus(m.getStatus());
        r.setLeaveReason(m.getLeaveReason()); r.setJoinedAt(m.getJoinedAt()); r.setLeftAt(m.getLeftAt());
        return r;
    }
}
