package com.qvenly.qv_ms_events.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ActivitiesClient {

    private final WebClient webClient;

    public ActivitiesClient(
            WebClient.Builder builder,
            @Value("${services.activities.url}") String activitiesUrl) {
        this.webClient = builder.baseUrl(activitiesUrl).build();
    }

    /**
     * Devuelve los emails de miembros activos de un evento con los roles dados.
     * roles debe ser lista de strings: ["JUDGE","PARTICIPANT","ATTENDEE","STAFF"]
     */
    public List<String> getMemberEmailsByRoles(Long eventId, List<String> roles) {
        try {
            String rolesParam = String.join(",", roles);
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/activities/members-by-event")
                            .queryParam("eventId", eventId)
                            .queryParam("roles", rolesParam)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || response.get("data") == null) return List.of();

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            return data.stream()
                    .map(m -> (String) m.get("email"))
                    .filter(e -> e != null && !e.isBlank())
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("Error al consultar miembros de actividades para eventId={}: {}", eventId, e.getMessage());
            return List.of();
        }
    }


    public List<String> getMemberRolesByEmailAndEvent(String email, Long eventId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/activities/roles-by-member")
                            .queryParam("email", email)
                            .queryParam("eventId", eventId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || response.get("data") == null) return List.of();
            return (List<String>) response.get("data");
        } catch (Exception e) {
            log.warn("Error al consultar roles de {} en evento {}: {}", email, eventId, e.getMessage());
            return List.of();
        }
    }
}