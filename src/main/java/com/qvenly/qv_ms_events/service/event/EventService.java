package com.qvenly.qv_ms_events.service.event;

import com.qvenly.qv_ms_events.client.event.NotificationClient;
import com.qvenly.qv_ms_events.client.event.PlanServiceClient;
import com.qvenly.qv_ms_events.client.dto.event.UserPlanResponseDTO;
import com.qvenly.qv_ms_events.exception.BusinessException;
import com.qvenly.qv_ms_events.model.dto.request.event.CancelEventRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.CreateEventRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.UpdateEventRequest;
import com.qvenly.qv_ms_events.model.dto.response.event.EventResponse;
import com.qvenly.qv_ms_events.model.entity.event.Event;
import com.qvenly.qv_ms_events.model.entity.event.EventMember;
import com.qvenly.qv_ms_events.model.entity.event.EventPlanSnapshot;
import com.qvenly.qv_ms_events.model.enums.event.AuditActionType;
import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.EventStatus;
import com.qvenly.qv_ms_events.model.enums.event.MemberStatus;
import com.qvenly.qv_ms_events.repository.event.EventMemberRepository;
import com.qvenly.qv_ms_events.repository.event.EventPlanSnapshotRepository;
import com.qvenly.qv_ms_events.repository.event.EventRepository;
import com.qvenly.qv_ms_events.repository.event.EventImageRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository             eventRepository;
    private final EventMemberRepository       memberRepository;
    private final EventPlanSnapshotRepository snapshotRepository;
    private final PlanServiceClient           planClient;
    private final AuditService auditService;
    private final NotificationClient notificationClient;
    private final EventImageRepository imageRepository;

    @Transactional
    public EventResponse createEvent(CreateEventRequest req, Long ownerUserId, String ownerEmail) {
        if (!req.getEndDatetime().isAfter(req.getStartDatetime())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio.", HttpStatus.BAD_REQUEST);
        }
        UserPlanResponseDTO userPlan = planClient.getActivePlan(ownerUserId);
        if (!userPlan.isActive()) {
            throw new BusinessException("El plan ha vencido. Renueve su plan para crear eventos.", HttpStatus.FORBIDDEN);
        }
        long currentCount = eventRepository.countActiveEventsByOwner(ownerUserId);
        int maxEvents = userPlan.getPlan().getMaxEvents();
        if (currentCount >= maxEvents) {
            throw new BusinessException(
                    String.format("Alcanzó el límite de %d evento(s) de su plan '%s'.", maxEvents, userPlan.getPlan().getName()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Event event = new Event();
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setLocation(req.getLocation());
        event.setEventType(req.getEventType());
        event.setStartDatetime(req.getStartDatetime());
        event.setEndDatetime(req.getEndDatetime());
        event.setStatus(EventStatus.DRAFT);
        event.setOwnerUserId(ownerUserId);
        event.setOwnerEmail(ownerEmail);
        Event saved = eventRepository.save(event);

        EventPlanSnapshot snapshot = buildSnapshot(saved.getId(), userPlan);
        EventPlanSnapshot savedSnapshot = snapshotRepository.save(snapshot);
        saved.setPlanSnapshotId(savedSnapshot.getId());
        eventRepository.save(saved);

        EventMember ownerMember = new EventMember();
        ownerMember.setEventId(saved.getId());
        ownerMember.setUserId(ownerUserId);
        ownerMember.setUserEmail(ownerEmail);
        ownerMember.setEventRole(EventRole.ORGANIZER);
        ownerMember.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(ownerMember);

        auditService.log(saved.getId(), AuditActionType.EVENT_CREATED, ownerEmail, "ORGANIZER",
                String.format("Evento '%s' creado. Plan: %s.", saved.getTitle(), userPlan.getPlan().getName()));
        log.info("Evento creado: id={}, owner={}", saved.getId(), ownerEmail);
        return toResponse(saved, savedSnapshot);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest req, String performerEmail, String systemRole) {
        Event event = findEventById(eventId);
        assertEditable(event);
        assertIsOrganizer(eventId, performerEmail, systemRole);
        String before = String.format("título='%s'", event.getTitle());
        if (req.getTitle()         != null) event.setTitle(req.getTitle());
        if (req.getDescription()   != null) event.setDescription(req.getDescription());
        if (req.getLocation()      != null) event.setLocation(req.getLocation());
        if (req.getEventType()     != null) event.setEventType(req.getEventType());
        if (req.getStartDatetime() != null) event.setStartDatetime(req.getStartDatetime());
        if (req.getEndDatetime()   != null) event.setEndDatetime(req.getEndDatetime());
        if (event.getEndDatetime() != null && event.getStartDatetime() != null
                && !event.getEndDatetime().isAfter(event.getStartDatetime())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la de inicio.", HttpStatus.BAD_REQUEST);
        }
        Event updated = eventRepository.save(event);
        auditService.log(eventId, AuditActionType.EVENT_EDITED, performerEmail,
                resolveRole(eventId, performerEmail, systemRole),
                String.format("Antes: %s | Después: título='%s'", before, updated.getTitle()));
        List<EventMember> activeMembers = memberRepository.findByEventIdAndStatus(eventId, MemberStatus.ACTIVE);
        notificationClient.sendEventNotification(
                "event-updated",
                updated.getTitle(),
                eventId,
                activeMembers.stream().map(m -> new NotificationClient.Recipient(
                        null, m.getUserEmail(), null)).toList(),
                String.format("Antes: %s | Después: título='%s'", before, updated.getTitle())
        );
        return toResponse(updated, snapshotRepository.findByEventId(eventId).orElse(null));
    }

    @Transactional
    public EventResponse publishEvent(Long eventId, String performerEmail, String systemRole) {
        Event event = findEventById(eventId);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("Solo se puede publicar un evento en estado DRAFT.", HttpStatus.CONFLICT);
        }
        assertIsOrganizer(eventId, performerEmail, systemRole);
        event.setStatus(EventStatus.PUBLISHED);
        Event updated = eventRepository.save(event);
        auditService.log(eventId, AuditActionType.EVENT_PUBLISHED, performerEmail,
                resolveRole(eventId, performerEmail, systemRole), "Evento publicado.");
        return toResponse(updated, snapshotRepository.findByEventId(eventId).orElse(null));
    }

    @Transactional
    public EventResponse startEvent(Long eventId, String performerEmail, String systemRole) {
        Event event = findEventById(eventId);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException("Solo se puede iniciar un evento PUBLISHED.", HttpStatus.CONFLICT);
        }
        assertIsOrganizer(eventId, performerEmail, systemRole);
        event.setStatus(EventStatus.IN_PROGRESS);
        Event updated = eventRepository.save(event);
        auditService.log(eventId, AuditActionType.EVENT_STARTED, performerEmail,
                resolveRole(eventId, performerEmail, systemRole), "Evento iniciado.");
        return toResponse(updated, snapshotRepository.findByEventId(eventId).orElse(null));
    }

    @Transactional
    public EventResponse finishEvent(Long eventId, String performerEmail, String systemRole) {
        Event event = findEventById(eventId);
        if (event.getStatus() != EventStatus.IN_PROGRESS) {
            throw new BusinessException("Solo se puede finalizar un evento IN_PROGRESS.", HttpStatus.CONFLICT);
        }
        assertIsOrganizer(eventId, performerEmail, systemRole);
        event.setStatus(EventStatus.FINISHED);
        Event updated = eventRepository.save(event);
        auditService.log(eventId, AuditActionType.EVENT_FINISHED, performerEmail,
                resolveRole(eventId, performerEmail, systemRole), "Evento finalizado.");
        return toResponse(updated, snapshotRepository.findByEventId(eventId).orElse(null));
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId, CancelEventRequest req, String performerEmail, String systemRole) {
        Event event = findEventById(eventId);
        assertEditable(event);
        assertIsOrganizer(eventId, performerEmail, systemRole);
        event.setStatus(EventStatus.CANCELLED);
        event.setCancelReason(req.getCancelReason());
        Event updated = eventRepository.save(event);
        auditService.log(eventId, AuditActionType.EVENT_CANCELLED, performerEmail,
                resolveRole(eventId, performerEmail, systemRole), "Motivo: " + req.getCancelReason());

        List<EventMember> activeMembers = memberRepository.findByEventIdAndStatus(eventId, MemberStatus.ACTIVE);
        notificationClient.sendEventNotification(
                "event-cancelled",
                updated.getTitle(),
                eventId,
                activeMembers.stream().map(m -> new NotificationClient.Recipient(
                        null, m.getUserEmail(), null)).toList(),
                req.getCancelReason()
        );

        return toResponse(updated, snapshotRepository.findByEventId(eventId).orElse(null));
    }

    public List<EventResponse> getMyEvents(Long ownerUserId) {
        return eventRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)
                .stream().map(e -> toResponse(e, snapshotRepository.findByEventId(e.getId()).orElse(null))).toList();
    }

    public List<EventResponse> getEventsByMember(String userEmail) {
        return eventRepository.findEventsByMemberEmail(userEmail)
                .stream().map(e -> toResponse(e, snapshotRepository.findByEventId(e.getId()).orElse(null))).toList();
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream().map(e -> toResponse(e, snapshotRepository.findByEventId(e.getId()).orElse(null))).toList();
    }

    public EventResponse getEventById(Long eventId, String requesterEmail, String systemRole) {
        Event event = findEventById(eventId);
        boolean isAdmin  = "ADMIN".equalsIgnoreCase(systemRole);
        boolean isMember = memberRepository.existsByEventIdAndUserEmailAndStatus(eventId, requesterEmail, MemberStatus.ACTIVE);
        if (!isAdmin && !isMember) {
            throw new BusinessException("No tienes acceso a este evento.", HttpStatus.FORBIDDEN);
        }
        return toResponse(event, snapshotRepository.findByEventId(eventId).orElse(null));
    }

    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado: " + eventId));
    }

    public void assertIsOrganizer(Long eventId, String userEmail, String systemRole) {
        if ("ADMIN".equalsIgnoreCase(systemRole)) return;
        boolean isOrg = memberRepository.existsByEventIdAndUserEmailAndEventRoleAndStatus(
                eventId, userEmail, EventRole.ORGANIZER, MemberStatus.ACTIVE);
        if (!isOrg) {
            throw new BusinessException("Solo los organizadores pueden realizar esta acción.", HttpStatus.FORBIDDEN);
        }
    }

    private void assertEditable(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessException("No se puede modificar un evento en estado " + event.getStatus(), HttpStatus.CONFLICT);
        }
    }

    public String resolveRole(Long eventId, String userEmail, String systemRole) {
        if ("ADMIN".equalsIgnoreCase(systemRole)) return "ADMIN";
        return memberRepository.findByEventIdAndUserEmailAndEventRole(eventId, userEmail, EventRole.ORGANIZER)
                .map(m -> m.getEventRole().name()).orElse("ORGANIZER");
    }

    private EventPlanSnapshot buildSnapshot(Long eventId, UserPlanResponseDTO up) {
        UserPlanResponseDTO.PlanDTO p = up.getPlan();
        EventPlanSnapshot s = new EventPlanSnapshot();
        s.setEventId(eventId); s.setPlanId(p.getIdPlan()); s.setPlanName(p.getName());
        s.setMaxEvents(p.getMaxEvents()); s.setMaxOrganizers(p.getMaxOrganizers());
        s.setMaxParticipants(p.getMaxParticipants()); s.setMaxJudges(p.getMaxJudges());
        s.setMaxAttendees(p.getMaxAttendees()); s.setMaxStaff(p.getMaxStaff());
        return s;
    }

    public EventResponse toResponse(Event e, EventPlanSnapshot snap) {
        EventResponse r = new EventResponse();
        r.setId(e.getId()); r.setTitle(e.getTitle()); r.setDescription(e.getDescription());
        r.setLocation(e.getLocation()); r.setEventType(e.getEventType());
        r.setStartDatetime(e.getStartDatetime()); r.setEndDatetime(e.getEndDatetime());
        r.setStatus(e.getStatus()); r.setCancelReason(e.getCancelReason());
        r.setOwnerUserId(e.getOwnerUserId()); r.setOwnerEmail(e.getOwnerEmail());
        r.setCreatedAt(e.getCreatedAt()); r.setUpdatedAt(e.getUpdatedAt());
        if (snap != null) {
            EventResponse.PlanLimitsResponse lim = new EventResponse.PlanLimitsResponse();
            lim.setPlanName(snap.getPlanName()); lim.setMaxEvents(snap.getMaxEvents());
            lim.setMaxOrganizers(snap.getMaxOrganizers()); lim.setMaxParticipants(snap.getMaxParticipants());
            lim.setMaxJudges(snap.getMaxJudges()); lim.setMaxAttendees(snap.getMaxAttendees());
            lim.setMaxStaff(snap.getMaxStaff());
            r.setPlanLimits(lim);
        }
        var cover = imageRepository.findByEventIdAndIsCoverTrue(e.getId());
        if (cover != null) {
            r.setCoverImageUrl(cover.getImageUrl());
        }
        return r;
    }
}
