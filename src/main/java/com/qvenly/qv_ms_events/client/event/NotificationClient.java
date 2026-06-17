package com.qvenly.qv_ms_events.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para qv-ms-notifications.
 * Llamadas fire-and-forget — si falla no detiene el flujo principal.
 */
@Slf4j
@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient(
            WebClient.Builder builder,
            @Value("${services.notifications.url}") String notificationsUrl) {
        this.webClient = builder.baseUrl(notificationsUrl).build();
    }

    /** Destinatario de una notificación */
    public record Recipient(Long userId, String email, String name) {}

    // ── RF52, RF53 — Invitación enviada ───────────────────────

    public void sendInvitationNotification(String invitedEmail, String invitedName,
                                           Long invitedUserId, String eventTitle,
                                           Long eventId, String eventRole,
                                           String token, String expiresAt,
                                           String eventDescription, String eventLocation,
                                           String eventType, String startDatetime,
                                           String endDatetime) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("invitedEmail",      invitedEmail);
            body.put("invitedName",       invitedName    != null ? invitedName    : "");
            body.put("invitedUserId",     invitedUserId  != null ? invitedUserId  : 0);
            body.put("eventTitle",        eventTitle);
            body.put("eventId",           eventId);
            body.put("eventRole",         eventRole);
            body.put("invitationToken",   token);
            body.put("expiresAt",         expiresAt      != null ? expiresAt      : "");
            body.put("eventDescription",  eventDescription != null ? eventDescription : "");
            body.put("eventLocation",     eventLocation    != null ? eventLocation    : "");
            body.put("eventType",         eventType        != null ? eventType        : "");
            body.put("startDatetime",     startDatetime    != null ? startDatetime    : "");
            body.put("endDatetime",       endDatetime      != null ? endDatetime      : "");
            post("/api/notifications/invitation-sent", body);
        } catch (Exception e) {
            log.warn("Error al enviar notificación de invitación: {}", e.getMessage());
        }
    }

    // ── RF42.1, RF39.1, RF59.1, RF60.1 — Notificaciones de evento ─

    public void sendEventNotification(String endpoint, String eventTitle,
                                      Long eventId, List<Recipient> recipients,
                                      String detail) {
        try {
            List<Map<String, Object>> recipientList = recipients.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", r.userId() != null ? r.userId() : 0);
                m.put("email",  r.email());
                m.put("name",   r.name()   != null ? r.name()   : "");
                return m;
            }).toList();

            Map<String, Object> body = new HashMap<>();
            body.put("type",       endpoint.replace("-", "_").toUpperCase());
            body.put("eventTitle", eventTitle);
            body.put("eventId",    eventId);
            body.put("recipients", recipientList);
            body.put("detail",     detail != null ? detail : "");

            post("/api/notifications/" + endpoint, body);
        } catch (Exception e) {
            log.warn("Error al enviar notificación {}: {}", endpoint, e.getMessage());
        }
    }

    // ── Helper interno ─────────────────────────────────────────

    private void post(String uri, Object body) {
        webClient.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        null,
                        err -> log.warn("Notificación fallida [{}]: {}", uri, err.getMessage())
                );
    }
}