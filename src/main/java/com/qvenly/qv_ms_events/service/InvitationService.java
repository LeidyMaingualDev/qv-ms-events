package com.qvenly.qv_ms_events.service;

import com.qvenly.qv_ms_events.client.NotificationClient;
import com.qvenly.qv_ms_events.exception.BusinessException;
import com.qvenly.qv_ms_events.model.dto.request.CancelInvitationRequest;
import com.qvenly.qv_ms_events.model.dto.request.SendInvitationRequest;
import com.qvenly.qv_ms_events.model.dto.response.InvitationResponse;
import com.qvenly.qv_ms_events.model.entity.Event;
import com.qvenly.qv_ms_events.model.entity.Invitation;
import com.qvenly.qv_ms_events.model.enums.AuditActionType;
import com.qvenly.qv_ms_events.model.enums.EventRole;
import com.qvenly.qv_ms_events.model.enums.EventStatus;
import com.qvenly.qv_ms_events.model.enums.InvitationStatus;
import com.qvenly.qv_ms_events.model.enums.MemberStatus;
import com.qvenly.qv_ms_events.repository.EventMemberRepository;
import com.qvenly.qv_ms_events.repository.InvitationRepository;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository  invitationRepository;
    private final EventMemberRepository memberRepository;
    private final EventService          eventService;
    private final EventMemberService    memberService;
    private final AuditService          auditService;
    private final NotificationClient notificationClient;

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
                eventId, req.getInvitedEmail(), req.getEventRole(), InvitationStatus.PENDING)) {
            throw new BusinessException(
                    String.format("Ya existe una invitación pendiente para %s como %s.",
                            req.getInvitedEmail(), req.getEventRole()), HttpStatus.CONFLICT);
        }
        if (memberRepository.findByEventIdAndUserEmailAndEventRole(
                eventId, req.getInvitedEmail(), req.getEventRole()).isPresent()) {
            throw new BusinessException("El usuario ya es miembro con el rol " + req.getEventRole(), HttpStatus.CONFLICT);
        }
        memberService.validateRoleLimit(eventId, req.getEventRole());

        Invitation inv = new Invitation();
        inv.setEventId(eventId);
        inv.setInvitedByEmail(performerEmail);
        inv.setInvitedEmail(req.getInvitedEmail());
        inv.setEventRole(req.getEventRole());
        inv.setToken(UUID.randomUUID().toString());
        inv.setStatus(InvitationStatus.PENDING);
        Invitation saved = invitationRepository.save(inv);

        auditService.log(eventId, AuditActionType.INVITATION_SENT, performerEmail,
                eventService.resolveRole(eventId, performerEmail, systemRole),
                String.format("Invitación enviada a %s para rol %s.", req.getInvitedEmail(), req.getEventRole()));

        // Notificar al invitado por correo + notificación interna si está registrado
        notificationClient.sendInvitationNotification(
                saved.getInvitedEmail(),
                null,                          // nombre: null hasta integrar con auth
                null,                          // userId: null si no está registrado
                event.getTitle(),
                eventId,
                saved.getEventRole().name(),
                saved.getToken(),
                saved.getExpiresAt() != null ? saved.getExpiresAt().toString() : null
        );

        return toResponse(saved);
    }

    @Transactional
    public BulkResult sendBulkFromExcel(Long eventId, MultipartFile file, EventRole defaultRole,
                                         String performerEmail, String systemRole) {
        Event event = eventService.findEventById(eventId);
        assertEventAcceptsInvitations(event);
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        List<InvitationResponse> sent   = new ArrayList<>();
        List<BulkResult.Failed>  failed = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String email = cell(row, 0);
                if (email == null || email.isBlank()) continue;
                String roleStr = cell(row, 1);
                EventRole role = defaultRole;
                if (roleStr != null && !roleStr.isBlank()) {
                    try { role = EventRole.valueOf(roleStr.trim().toUpperCase()); }
                    catch (IllegalArgumentException ex) {
                        failed.add(new BulkResult.Failed(email, "Rol inválido: " + roleStr)); continue;
                    }
                }
                try {
                    SendInvitationRequest req = new SendInvitationRequest();
                    req.setInvitedEmail(email.trim().toLowerCase());
                    req.setEventRole(role);
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
        auditService.log(eventId, AuditActionType.INVITATION_CANCELLED, performerEmail,
                eventService.resolveRole(eventId, performerEmail, systemRole),
                String.format("Invitación a %s cancelada. Motivo: %s", inv.getInvitedEmail(), req.getCancelReason()));
        return toResponse(updated);
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

    public List<InvitationResponse> getMyPendingInvitations(String userEmail) {
        return invitationRepository.findByInvitedEmailAndStatus(userEmail, InvitationStatus.PENDING)
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
        return r;
    }

    public record BulkResult(List<InvitationResponse> sent, List<Failed> failed) {
        public record Failed(String email, String reason) {}
    }
}
