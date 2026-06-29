package com.qvenly.qv_ms_events.controller.eventSurvey;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qvenly.qv_ms_events.model.dto.request.eventSurvey.SurveyCancelRequestDTO;
import com.qvenly.qv_ms_events.model.dto.request.eventSurvey.SurveyRequestDTO;
import com.qvenly.qv_ms_events.model.dto.request.eventSurvey.SurveyResponseRequestDTO;
import com.qvenly.qv_ms_events.model.dto.response.event.ApiResponse;
import com.qvenly.qv_ms_events.model.dto.response.eventSurvey.SurveyResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.eventSurvey.SurveyResultsResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.eventSurvey.SurveySummaryResponseDTO;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;
import com.qvenly.qv_ms_events.service.eventSurvey.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class SurveyController {

        private final SurveyService surveyService;
        
        // RF130: Crear encuesta
        @PostMapping("/{eventId}/surveys")
        public ResponseEntity<ApiResponse<SurveyResponseDTO>> createSurvey(
                @PathVariable Long eventId,
                @RequestBody SurveyRequestDTO request,
                @RequestHeader("X-User-Id")    Long   userId,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Encuesta creada exitosamente.",
                                surveyService.createSurvey(eventId, request, userId, userEmail, role)));
        }
 
        // RF130.3: Listar encuestas del evento
        @GetMapping("/{eventId}/surveys")
        public ResponseEntity<ApiResponse<List<SurveySummaryResponseDTO>>> getSurveys(
                @PathVariable Long eventId,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                return ResponseEntity.ok(ApiResponse.success("Encuestas obtenidas.",
                        surveyService.getSurveysByEvent(eventId, userEmail, role)));
        }
 
        // RF130.3: Vista previa / detalle completo de una encuesta
        @GetMapping("/{eventId}/surveys/{surveyId}")
        public ResponseEntity<ApiResponse<SurveyResponseDTO>> getSurveyDetail(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                return ResponseEntity.ok(ApiResponse.success("Encuesta obtenida.",
                        surveyService.getSurveyDetail(surveyId)));
        }
 
        // RF131: Publicar encuesta
        @PatchMapping("/{eventId}/surveys/{surveyId}/publish")
        public ResponseEntity<ApiResponse<SurveyResponseDTO>> publishSurvey(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                return ResponseEntity.ok(ApiResponse.success("Encuesta publicada.",
                        surveyService.publishSurvey(surveyId, userEmail, role)));
        }
 
        // RF132: Cancelar encuesta
        @PatchMapping("/{eventId}/surveys/{surveyId}/cancel")
        public ResponseEntity<ApiResponse<SurveyResponseDTO>> cancelSurvey(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestBody SurveyCancelRequestDTO request,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                return ResponseEntity.ok(ApiResponse.success("Encuesta cancelada.",
                        surveyService.cancelSurvey(surveyId, request, userEmail, role)));
        }
 
        // RF133/RF134/RF135: Responder encuesta
        @PostMapping("/{eventId}/surveys/{surveyId}/responses")
        public ResponseEntity<ApiResponse<Void>> submitResponse(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestBody SurveyResponseRequestDTO request,
                @RequestHeader("X-User-Id")    Long   userId,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                surveyService.submitResponse(surveyId, request, userId, userEmail);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Respuesta enviada exitosamente.", null));
        }
 
        // RF136/RF137: Ver resultados (con filtro opcional por rol)
        @GetMapping("/{eventId}/surveys/{surveyId}/results")
        public ResponseEntity<ApiResponse<SurveyResultsResponseDTO>> getResults(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestParam(required = false) SurveyEventRole roleFilter,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
                return ResponseEntity.ok(ApiResponse.success("Resultados obtenidos.",
                        surveyService.getResults(surveyId, roleFilter, userEmail, role)));
        }

        // Encuestas pendientes para el usuario en un evento
        @GetMapping("/{eventId}/surveys/pending")
        public ResponseEntity<ApiResponse<List<SurveySummaryResponseDTO>>> getPendingSurveys(
                @PathVariable Long eventId,
                @RequestHeader("X-User-Id")    Long   userId,
                @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(ApiResponse.success("Encuestas pendientes.",
                surveyService.getPendingSurveys(eventId, userId, userEmail)));
        }

        //Editar encuesta en DRAFT
        @PutMapping("/{eventId}/surveys/{surveyId}")
        public ResponseEntity<ApiResponse<SurveyResponseDTO>> updateSurvey(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestBody SurveyRequestDTO request,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
        return ResponseEntity.ok(ApiResponse.success("Encuesta actualizada.",
                surveyService.updateSurvey(surveyId, request, userEmail, role)));
        }

        // Eliminar encuesta (solo DRAFT, CANCELLED o CLOSED)
        @DeleteMapping("/{eventId}/surveys/{surveyId}")
        public ResponseEntity<ApiResponse<Void>> deleteSurvey(
                @PathVariable Long eventId,
                @PathVariable Long surveyId,
                @RequestHeader("X-User-Email") String userEmail,
                @RequestHeader("X-Rol")        String role) {
        surveyService.deleteSurvey(surveyId, userEmail, role);
        return ResponseEntity.ok(ApiResponse.success("Encuesta eliminada.", null));
        }
}
