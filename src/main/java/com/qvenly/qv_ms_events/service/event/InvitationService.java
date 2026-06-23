package com.qvenly.qv_ms_events.service.event;

import com.qvenly.qv_ms_events.client.event.AuthInternalClient;
import com.qvenly.qv_ms_events.client.event.NotificationClient;
import com.qvenly.qv_ms_events.exception.BusinessException;
import com.qvenly.qv_ms_events.model.dto.request.event.CancelInvitationRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.SendInvitationRequest;
import com.qvenly.qv_ms_events.model.dto.response.event.InvitationResponse;
import com.qvenly.qv_ms_events.model.entity.event.Event;
import com.qvenly.qv_ms_events.model.entity.event.Invitation;
import com.qvenly.qv_ms_events.model.enums.event.AuditActionType;
import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.EventStatus;
import com.qvenly.qv_ms_events.model.enums.event.InvitationStatus;
import com.qvenly.qv_ms_events.repository.event.EventImageRepository;
import com.qvenly.qv_ms_events.repository.event.EventMemberRepository;
import com.qvenly.qv_ms_events.repository.event.InvitationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository  invitationRepository;
    private final EventMemberRepository memberRepository;
    private final EventImageRepository imageRepository;
    private final EventService eventService;
    private final EventMemberService memberService;
    private final AuditService auditService;
    private final NotificationClient notificationClient;
    private final AuthInternalClient authInternalClient;

    @Transactional
    public InvitationResponse sendInvitation(Long eventId, SendInvitationRequest req,
                                             String performerEmail, String systemRole) {
        Event event = eventService.findEventById(eventId);
        assertEventAcceptsInvitations(event);
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        if (req.getInvitedEmail().equalsIgnoreCase(performerEmail)) {
            throw new BusinessException("No puedes invitarte a ti mismo.", HttpStatus.BAD_REQUEST);
        }
        if (invitationRepository.existsByEventIdAndInvitedEmailAndEventRoleAndStatus(
                eventId, req.getInvitedEmail(), EventRole.MEMBER, InvitationStatus.PENDING)) {
            throw new BusinessException(
                    String.format("Ya existe una invitación pendiente para %s.", req.getInvitedEmail()),
                    HttpStatus.CONFLICT);
        }
        if (memberRepository.findByEventIdAndUserEmailAndEventRole(
                eventId, req.getInvitedEmail(), EventRole.MEMBER).isPresent()) {
            throw new BusinessException("El usuario ya es miembro de este evento.", HttpStatus.CONFLICT);
        }
        memberService.validateRoleLimit(eventId, EventRole.MEMBER);

        Invitation inv = new Invitation();
        inv.setEventId(eventId);
        inv.setInvitedByEmail(performerEmail);
        inv.setInvitedEmail(req.getInvitedEmail());
        inv.setEventRole(EventRole.MEMBER);
        inv.setToken(UUID.randomUUID().toString());
        inv.setStatus(InvitationStatus.PENDING);
        inv.setExpiresAt(req.getExpiresAt() != null ? req.getExpiresAt() : LocalDateTime.now().plusDays(7));
        Invitation saved = invitationRepository.save(inv);

        auditService.log(eventId, AuditActionType.INVITATION_SENT, performerEmail,
                eventService.resolveRole(eventId, performerEmail, systemRole),
                String.format("Invitación enviada a %s.", req.getInvitedEmail()));

        Map<String, Object> registeredUser = authInternalClient.findUserByEmail(saved.getInvitedEmail());
        Long invitedUserId = registeredUser != null && registeredUser.get("userId") != null
                ? Long.valueOf(registeredUser.get("userId").toString()) : null;
        String invitedName = registeredUser != null ? (String) registeredUser.get("name") : null;

        notificationClient.sendInvitationNotification(
                saved.getInvitedEmail(),
                invitedName,
                invitedUserId,
                event.getTitle(),
                eventId,
                saved.getEventRole().name(),
                saved.getToken(),
                saved.getExpiresAt() != null ? saved.getExpiresAt().toString() : null,
                event.getDescription(),
                event.getLocation(),
                event.getEventType(),
                event.getStartDatetime().toString(),
                event.getEndDatetime().toString()
        );

        return toResponse(saved);
    }

    @Transactional
    public BulkResult sendBulkFromExcel(Long eventId, MultipartFile file, LocalDateTime expiresAt,
                                        String performerEmail, String systemRole) {
        Event event = eventService.findEventById(eventId);
        assertEventAcceptsInvitations(event);
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        List<InvitationResponse> sent   = new ArrayList<>();
        List<BulkResult.Failed>  failed = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String email = cell(row, 0);
                if (email == null || email.isBlank()) continue;
                String normalizedEmail = email.trim().toLowerCase();

                if (!seenInFile.add(normalizedEmail)) {
                    failed.add(new BulkResult.Failed(email, "Correo repetido en el archivo."));
                    continue;
                }

                try {
                    SendInvitationRequest req = new SendInvitationRequest();
                    req.setInvitedEmail(normalizedEmail);
                    req.setExpiresAt(expiresAt);
                    sent.add(sendInvitation(eventId, req, performerEmail, systemRole));
                } catch (BusinessException e) {
                    failed.add(new BulkResult.Failed(email, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo Excel.", HttpStatus.BAD_REQUEST);
        }
        return new BulkResult(sent, failed);
    }

    @Transactional
    public InvitationResponse cancelInvitation(Long eventId, Long invitationId,
                                               CancelInvitationRequest req,
                                               String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);
        Invitation inv = findById(invitationId);
        if (!inv.getEventId().equals(eventId)) {
            throw new BusinessException("La invitación no pertenece a este evento.", HttpStatus.BAD_REQUEST);
        }
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Solo se pueden cancelar invitaciones PENDING.", HttpStatus.CONFLICT);
        }
        inv.setStatus(InvitationStatus.CANCELLED);
        inv.setCancelReason(req.getCancelReason());
        inv.setRespondedAt(LocalDateTime.now());
        Invitation updated = invitationRepository.save(inv);

        Event event = eventService.findEventById(eventId);
        auditService.log(eventId, AuditActionType.INVITATION_CANCELLED, performerEmail,
                eventService.resolveRole(eventId, performerEmail, systemRole),
                String.format("Invitación a %s cancelada. Motivo: %s", inv.getInvitedEmail(), req.getCancelReason()));

        Map<String, Object> registeredUser = authInternalClient.findUserByEmail(inv.getInvitedEmail());
        Long invitedUserId = registeredUser != null && registeredUser.get("userId") != null
                ? Long.valueOf(registeredUser.get("userId").toString()) : null;
        String invitedName = registeredUser != null ? (String) registeredUser.get("name") : null;

        notificationClient.sendEventNotification(
                "invitation-cancelled",
                event.getTitle(),
                eventId,
                List.of(new NotificationClient.Recipient(invitedUserId, inv.getInvitedEmail(), invitedName)),
                req.getCancelReason()
        );

        return toResponse(updated);
    }

    public InvitationResponse findByToken(String token) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Enlace inválido o no encontrado.", HttpStatus.NOT_FOUND));
        return toResponse(inv);
    }

    @Transactional
    public InvitationResponse acceptInvitation(String token, Long userId, String userEmail) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Enlace inválido o no encontrado.", HttpStatus.NOT_FOUND));
        if (!inv.getInvitedEmail().equalsIgnoreCase(userEmail)) {
            throw new BusinessException("Esta invitación no está dirigida a tu cuenta.", HttpStatus.FORBIDDEN);
        }
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Esta invitación ya fue " + inv.getStatus().name().toLowerCase() + ".", HttpStatus.CONFLICT);
        }
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            inv.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(inv);
            throw new BusinessException("Esta invitación ha expirado.", HttpStatus.GONE);
        }
        Event event = eventService.findEventById(inv.getEventId());
        assertEventAcceptsInvitations(event);
        memberService.validateRoleLimit(inv.getEventId(), inv.getEventRole());
        memberService.addMember(inv.getEventId(), userId, userEmail, inv.getEventRole());
        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setRespondedAt(LocalDateTime.now());
        return toResponse(invitationRepository.save(inv));
    }

    public List<InvitationResponse> getInvitations(Long eventId) {
        return invitationRepository.findByEventIdOrderBySentAtDesc(eventId).stream().map(this::toResponse).toList();
    }

    public List<InvitationResponse> getInvitationsByStatus(Long eventId, InvitationStatus status) {
        return invitationRepository.findByEventIdAndStatus(eventId, status).stream().map(this::toResponse).toList();
    }

    public List<InvitationResponse> getAllMyInvitations(String userEmail) {
        return invitationRepository.findByInvitedEmailOrderBySentAtDesc(userEmail)
                .stream().map(this::toResponse).toList();
    }

    public List<InvitationResponse> getMyInvitationsByStatus(String userEmail, InvitationStatus status) {
        return invitationRepository.findByInvitedEmailAndStatus(userEmail, status)
                .stream().map(this::toResponse).toList();
    }

    private void assertEventAcceptsInvitations(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessException("No se pueden gestionar invitaciones en un evento " + event.getStatus(), HttpStatus.CONFLICT);
        }
    }

    private Invitation findById(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invitación no encontrada: " + id));
    }

    private String cell(Row row, int col) {
        try {
            var c = row.getCell(col);
            if (c == null) return null;
            return switch (c.getCellType()) {
                case STRING  -> c.getStringCellValue().trim();
                case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
                default -> null;
            };
        } catch (Exception e) { return null; }
    }

    public InvitationResponse toResponse(Invitation i) {
        InvitationResponse r = new InvitationResponse();
        r.setId(i.getId()); r.setEventId(i.getEventId());
        r.setInvitedByEmail(i.getInvitedByEmail()); r.setInvitedEmail(i.getInvitedEmail());
        r.setEventRole(i.getEventRole()); r.setStatus(i.getStatus());
        r.setCancelReason(i.getCancelReason()); r.setSentAt(i.getSentAt());
        r.setToken(i.getToken());
        r.setExpiresAt(i.getExpiresAt()); r.setRespondedAt(i.getRespondedAt());

        Event event = eventService.findEventById(i.getEventId());
        r.setEventTitle(event.getTitle());
        r.setEventDescription(event.getDescription());
        r.setEventLocation(event.getLocation());
        r.setEventType(event.getEventType());
        r.setEventStartDatetime(event.getStartDatetime());
        r.setEventEndDatetime(event.getEndDatetime());

        var cover = imageRepository.findByEventIdAndIsCoverTrue(i.getEventId());
        if (cover != null) {
            r.setEventCoverImageUrl(cover.getImageUrl());
        }

        return r;
    }

    public record BulkResult(List<InvitationResponse> sent, List<Failed> failed) {
        public record Failed(String email, String reason) {}
    }
}
