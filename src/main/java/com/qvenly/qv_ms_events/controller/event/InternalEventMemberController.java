package com.qvenly.qv_ms_events.controller.event;

import com.qvenly.qv_ms_events.model.dto.response.event.ApiResponse;
import com.qvenly.qv_ms_events.service.event.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class InternalEventMemberController {

    private final EventService eventService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    /**
     * Devuelve el rol de evento (ORGANIZER/STAFF/MEMBER) de una persona en un evento,
     * o null en data si no es miembro activo. Uso interno entre microservicios.
     */
    @GetMapping("/internal/{eventId}/members/role")
    public ResponseEntity<ApiResponse<String>> getMemberRole(
            @PathVariable Long eventId,
            @RequestParam String email,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

        if (apiKey == null || !apiKey.equals(internalApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Llave interna inválida.", null));
        }
        String role = eventService.getActiveMemberRole(eventId, email);
        return ResponseEntity.ok(ApiResponse.success("Rol obtenido.", role));
    }
}