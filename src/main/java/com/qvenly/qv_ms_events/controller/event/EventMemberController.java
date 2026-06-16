package com.qvenly.qv_ms_events.controller.event;

import com.qvenly.qv_ms_events.model.dto.request.event.ChangeMemberRoleRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.LeaveEventRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.RemoveMemberRequest;
import com.qvenly.qv_ms_events.model.dto.response.event.ApiResponse;
import com.qvenly.qv_ms_events.model.dto.response.event.EventMemberResponse;
import com.qvenly.qv_ms_events.model.dto.response.event.LimitsUsageResponse;
import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.service.event.EventMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class EventMemberController {

    private final EventMemberService memberService;

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<EventMemberResponse>>> getMembers(
            @PathVariable Long eventId,
            @RequestParam(required = false) EventRole role,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String systemRole) {
        List<EventMemberResponse> members = (role != null)
                ? memberService.getMembersByRole(eventId, role)
                : memberService.getActiveMembers(eventId);
        return ResponseEntity.ok(ApiResponse.success("Miembros obtenidos.", members));
    }

    @PatchMapping("/members/{memberId}/role")
    public ResponseEntity<ApiResponse<EventMemberResponse>> changeMemberRole(
            @PathVariable Long eventId,
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String systemRole) {
        return ResponseEntity.ok(ApiResponse.success("Rol actualizado.",
                memberService.changeMemberRole(eventId, memberId, request, userEmail, systemRole)));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long eventId,
            @PathVariable Long memberId,
            @Valid @RequestBody RemoveMemberRequest request,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String systemRole) {
        memberService.removeMember(eventId, memberId, request, userEmail, systemRole);
        return ResponseEntity.ok(ApiResponse.success("Miembro eliminado."));
    }

    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leaveEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody LeaveEventRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        memberService.leaveEvent(eventId, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Has abandonado el evento."));
    }

    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<LimitsUsageResponse>> getLimits(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String systemRole) {
        return ResponseEntity.ok(ApiResponse.success("Límites obtenidos.", memberService.getLimitsUsage(eventId)));
    }
}
