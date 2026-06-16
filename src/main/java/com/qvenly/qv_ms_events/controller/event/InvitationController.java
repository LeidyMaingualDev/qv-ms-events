package com.qvenly.qv_ms_events.controller.event;

import com.qvenly.qv_ms_events.model.dto.request.event.CancelInvitationRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.SendInvitationRequest;
import com.qvenly.qv_ms_events.model.dto.response.event.ApiResponse;
import com.qvenly.qv_ms_events.model.dto.response.event.InvitationResponse;
import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.InvitationStatus;
import com.qvenly.qv_ms_events.service.event.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/api/events/{eventId}/invitations")
    public ResponseEntity<ApiResponse<InvitationResponse>> sendInvitation(
            @PathVariable Long eventId,
            @Valid @RequestBody SendInvitationRequest request,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invitación enviada.", invitationService.sendInvitation(eventId, request, userEmail, role)));
    }

    @PostMapping("/api/events/{eventId}/invitations/bulk")
    public ResponseEntity<ApiResponse<InvitationService.BulkResult>> sendBulk(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "defaultRole", defaultValue = "ATTENDEE") EventRole defaultRole,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        InvitationService.BulkResult result = invitationService.sendBulkFromExcel(eventId, file, defaultRole, userEmail, role);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Proceso completado: %d enviadas, %d fallidas.", result.sent().size(), result.failed().size()), result));
    }

    @PatchMapping("/api/events/{eventId}/invitations/{invitationId}/cancel")
    public ResponseEntity<ApiResponse<InvitationResponse>> cancelInvitation(
            @PathVariable Long eventId,
            @PathVariable Long invitationId,
            @Valid @RequestBody CancelInvitationRequest request,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Invitación cancelada.",
                invitationService.cancelInvitation(eventId, invitationId, request, userEmail, role)));
    }

    @GetMapping("/api/events/{eventId}/invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getInvitations(
            @PathVariable Long eventId,
            @RequestParam(required = false) InvitationStatus status,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        List<InvitationResponse> invitations = (status != null)
                ? invitationService.getInvitationsByStatus(eventId, status)
                : invitationService.getInvitations(eventId);
        return ResponseEntity.ok(ApiResponse.success("Invitaciones obtenidas.", invitations));
    }

    @PostMapping("/api/invitations/accept/{token}")
    public ResponseEntity<ApiResponse<InvitationResponse>> acceptInvitation(
            @PathVariable String token,
            @RequestHeader("X-User-Id")    Long   userId,
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Invitación aceptada.",
                invitationService.acceptInvitation(token, userId, userEmail)));
    }

    @GetMapping("/api/invitations/my")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getMyInvitations(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Invitaciones pendientes.",
                invitationService.getMyPendingInvitations(userEmail)));
    }
}
