package com.qvenly.qv_ms_events.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qvenly.qv_ms_events.client.dto.UserPlanResponseDTO;
import com.qvenly.qv_ms_events.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class PlanServiceClient {

    private final WebClient webClient;

    public PlanServiceClient(@Value("${services.plans.url}") String plansUrl) {
        this.webClient = WebClient.builder().baseUrl(plansUrl).build();
    }

    public UserPlanResponseDTO getActivePlan(Long userId) {
        log.info("Consultando plan activo del usuario ID: {}", userId);
        try {
            PlanWrapper wrapper = webClient.get()
                    .uri("/api/user-plans/user/{userId}/active", userId)
                    .retrieve()
                    .bodyToMono(PlanWrapper.class)
                    .block();
            if (wrapper == null || wrapper.data() == null) {
                throw new BusinessException("El usuario no tiene un plan activo.", HttpStatus.FORBIDDEN);
            }
            return wrapper.data();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException("El usuario no tiene un plan activo. Adquiera un plan para crear eventos.", HttpStatus.FORBIDDEN);
            }
            log.error("Error al consultar plan activo del usuario {}: {}", userId, e.getMessage());
            throw new BusinessException("No se pudo verificar el plan del usuario.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error de conexión con qv-ms-plans: {}", e.getMessage());
            throw new BusinessException("Servicio de planes no disponible.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlanWrapper(boolean success, String message, UserPlanResponseDTO data) {}
}
