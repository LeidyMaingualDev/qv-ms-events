package com.qvenly.qv_ms_events.controller.event;

import com.qvenly.qv_ms_events.model.dto.response.event.ApiResponse;
import com.qvenly.qv_ms_events.model.dto.response.event.EventResponse;
import com.qvenly.qv_ms_events.service.event.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @GetMapping("/internal/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventInternal(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

        if (apiKey == null || !apiKey.equals(internalApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Llave interna inválida.", null));
        }

        return ResponseEntity.ok(ApiResponse.success("Evento obtenido.", eventService.getEventByIdInternal(id)));
    }
}