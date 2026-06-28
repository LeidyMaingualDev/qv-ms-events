package com.qvenly.qv_ms_events.repository.eventSurvey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyResponse;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long>{

    List<SurveyResponse> findBySurveyId(Long surveyId);
 
    List<SurveyResponse> findBySurveyIdAndRespondentRole(Long surveyId, SurveyEventRole role);
 
    long countBySurveyId(Long surveyId);
 
    long countBySurveyIdAndRespondentRole(Long surveyId, SurveyEventRole role);
 
    /**
     * Verifica si un usuario ya respondió la encuesta — evita respuestas duplicadas (RF133)
     */
    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);
 
    /**
     * Carga las respuestas con sus answers en una sola query para armar resultados (RF136)
     */
    @Query("""
            SELECT DISTINCT r FROM SurveyResponse r
            LEFT JOIN FETCH r.answers a
            LEFT JOIN FETCH a.question
            LEFT JOIN FETCH a.selectedOption
            WHERE r.survey.id = :surveyId
            """)
    List<SurveyResponse> findBySurveyIdWithAnswers(@Param("surveyId") Long surveyId);
 
    /**
     * Igual que el anterior pero filtrado por rol (RF137)
     */
    @Query("""
            SELECT DISTINCT r FROM SurveyResponse r
            LEFT JOIN FETCH r.answers a
            LEFT JOIN FETCH a.question
            LEFT JOIN FETCH a.selectedOption
            WHERE r.survey.id = :surveyId
            AND r.respondentRole = :role
            """)
    List<SurveyResponse> findBySurveyIdAndRoleWithAnswers(
            @Param("surveyId") Long surveyId,
            @Param("role") SurveyEventRole role
    );
}
