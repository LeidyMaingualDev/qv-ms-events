package com.qvenly.qv_ms_events.repository.eventSurvey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.Survey;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyStatus;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    List<Survey> findByEventIdOrderByCreatedAtDesc(Long eventId);
 
    List<Survey> findByEventIdAndStatusOrderByCreatedAtDesc(Long eventId, SurveyStatus status);
 
    /**
     * Busca una encuesta por id cargando sus preguntas y roles destino en una sola query,
     * para evitar N+1 al armar la vista previa (RF130.3)
     */
       @Query("""
                SELECT s FROM Survey s
                LEFT JOIN FETCH s.questions
                WHERE s.id = :surveyId
                """)
        Optional<Survey> findByIdWithQuestionsAndRoles(@Param("surveyId") Long surveyId);

        
     /**
     * Encuestas publicadas cuya fecha límite ya venció — útil para un job de cierre automático
     */
    @Query("""
            SELECT s FROM Survey s
            WHERE s.status = 'PUBLISHED'
            AND s.deadline IS NOT NULL
            AND s.deadline < :now
            """)
    List<Survey> findExpiredSurveys(@Param("now") LocalDateTime now);


    @Query("""
        SELECT s FROM Survey s
        JOIN s.targetRoles tr
        WHERE s.event.id = :eventId
        AND s.status = 'PUBLISHED'
        AND tr.eventRole IN :roles
        AND (:userId IS NULL OR NOT EXISTS (
                SELECT r FROM SurveyResponse r
                WHERE r.survey = s AND r.userId = :userId
        ))
        """)
        List<Survey> findPendingForUser(
                @Param("eventId") Long eventId,
                @Param("roles") List<SurveyEventRole> roles,
                @Param("userId") Long userId);
}
