package com.qvenly.qv_ms_events.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Cliente HTTP para qv-ms-auth.
 * Usado para resolver si un email corresponde a un usuario registrado.
 */
@Slf4j
@Component
public class AuthInternalClient {

    private final WebClient webClient;

    public AuthInternalClient(
            WebClient.Builder builder,
            @Value("${services.auth.url}") String authUrl) {
        this.webClient = builder.baseUrl(authUrl).build();
    }

    /**
     * Busca un usuario registrado por su email.
     * @return mapa con userId y name si existe, o null si no está registrado.
     */
    public Map<String, Object> findUserByEmail(String email) {
        try {
            return webClient.get()
                    .uri("/auth/internal/users/by-email?email={email}", email)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> (Map<String, Object>) response.get("data"))
                    .block();
        } catch (Exception e) {
            log.debug("Usuario no encontrado o error al consultar {}: {}", email, e.getMessage());
            return null;
        }
    }
}