package com.qvenly.qv_ms_events.controller.event;

import com.qvenly.qv_ms_events.model.dto.request.event.CancelEventRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.CreateEventRequest;
import com.qvenly.qv_ms_events.model.dto.request.event.UpdateEventRequest;
import com.qvenly.qv_ms_events.model.dto.response.event.ApiResponse;
import com.qvenly.qv_ms_events.model.dto.response.event.AuditLogResponse;
import com.qvenly.qv_ms_events.model.dto.response.event.EventResponse;
import com.qvenly.qv_ms_events.service.event.AuditService;
import com.qvenly.qv_ms_events.service.event.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final AuditService auditService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("X-User-Id")    Long   userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evento creado exitosamente.", eventService.createEvent(request, userId, userEmail)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEvents(
            @RequestHeader("X-User-Id")    Long   userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        List<EventResponse> events;
        if ("ADMIN".equalsIgnoreCase(role)) {
            events = eventService.getAllEvents();
        } else {
            List<EventResponse> owned  = eventService.getMyEvents(userId);
            List<EventResponse> member = eventService.getEventsByMember(userEmail);
            events = new java.util.ArrayList<>(owned);
            member.stream()
                    .filter(m -> owned.stream().noneMatch(o -> o.getId().equals(m.getId())))
                    .forEach(events::add);
        }
        return ResponseEntity.ok(ApiResponse.success("Eventos obtenidos exitosamente.", events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Evento obtenido.", eventService.getEventById(id, userEmail, role)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Evento actualizado.", eventService.updateEvent(id, request, userEmail, role)));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Evento publicado.", eventService.publishEvent(id, userEmail, role)));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<ApiResponse<EventResponse>> startEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Evento iniciado.", eventService.startEvent(id, userEmail, role)));
    }

    @PatchMapping("/{id}/finish")
    public ResponseEntity<ApiResponse<EventResponse>> finishEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Evento finalizado.", eventService.finishEvent(id, userEmail, role)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EventResponse>> cancelEvent(
            @PathVariable Long id,
            @Valid @RequestBody CancelEventRequest request,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Evento cancelado.", eventService.cancelEvent(id, request, userEmail, role)));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAudit(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-Rol")        String role) {
        eventService.assertIsOrganizer(id, userEmail, role);
        return ResponseEntity.ok(ApiResponse.success("Auditoría obtenida.", auditService.getAuditLog(id)));
    }
}
